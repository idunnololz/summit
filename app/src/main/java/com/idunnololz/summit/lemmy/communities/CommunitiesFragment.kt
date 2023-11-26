package com.idunnololz.summit.lemmy.communities

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.updatePadding
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.Adapter
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import coil.load
import com.idunnololz.summit.R
import com.idunnololz.summit.api.utils.instance
import com.idunnololz.summit.databinding.CommunitiesLoadItemBinding
import com.idunnololz.summit.databinding.CommunityDetailsItemBinding
import com.idunnololz.summit.databinding.EmptyItemBinding
import com.idunnololz.summit.databinding.FragmentCommunitiesBinding
import com.idunnololz.summit.lemmy.LemmyTextHelper
import com.idunnololz.summit.lemmy.LemmyUtils
import com.idunnololz.summit.lemmy.PageRef
import com.idunnololz.summit.lemmy.search.Item
import com.idunnololz.summit.lemmy.toCommunityRef
import com.idunnololz.summit.offline.OfflineManager
import com.idunnololz.summit.util.BaseFragment
import com.idunnololz.summit.util.StatefulData
import com.idunnololz.summit.util.ext.getColorFromAttribute
import com.idunnololz.summit.util.ext.getDrawableCompat
import com.idunnololz.summit.util.ext.tint
import com.idunnololz.summit.util.recyclerView.AdapterHelper
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class CommunitiesFragment : BaseFragment<FragmentCommunitiesBinding>() {

    private val args by navArgs<CommunitiesFragmentArgs>()

    private val viewModel: CommunitiesViewModel by viewModels()

    @Inject
    lateinit var offlineManager: OfflineManager

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        super.onCreateView(inflater, container, savedInstanceState)

        requireMainActivity().apply {
            setupForFragment<CommunitiesFragment>()
        }

        setBinding(FragmentCommunitiesBinding.inflate(inflater, container, false))

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val context = requireContext()

        requireMainActivity().apply {
            insetViewExceptTopAutomaticallyByPadding(viewLifecycleOwner, binding.recyclerView)
            insetViewExceptBottomAutomaticallyByMargins(viewLifecycleOwner, binding.toolbar)

            setSupportActionBar(binding.toolbar)

            supportActionBar?.setDisplayShowHomeEnabled(true)
            supportActionBar?.setDisplayHomeAsUpEnabled(true)
            supportActionBar?.title = getString(R.string.communities)

            binding.contentContainer.updatePadding(bottom = getBottomNavHeight())
        }

        viewModel.setCommunitiesInstance(args.instance)
        if (viewModel.communitiesData.isNotStarted) {
            viewModel.fetchCommunities(0)
        }

        with(binding) {
            val adapter = CommunitiesEngineAdapter(
                context = context,
                rootView = root,
                offlineManager = offlineManager,
                instance = viewModel.apiInstance,
                onPageClick = {
                    getMainActivity()?.launchPage(it)
                },
                onLoadPageClick = {
                    viewModel.fetchCommunities(it)
                },
            )
            val layoutManager = LinearLayoutManager(context)

            fun fetchPageIfLoadItem(position: Int) {
                (adapter.items.getOrNull(position) as? CommunitiesEngine.Item.LoadItem)
                    ?.pageIndex
                    ?.let {
                        viewModel.fetchCommunities(it)
                    }
            }

            fun checkIfFetchNeeded() {
                val firstPos = layoutManager.findFirstVisibleItemPosition()
                val lastPos = layoutManager.findLastVisibleItemPosition()

                fetchPageIfLoadItem(firstPos)
                fetchPageIfLoadItem(firstPos - 1)
                fetchPageIfLoadItem(lastPos)
                fetchPageIfLoadItem(lastPos + 1)
            }

            recyclerView.setHasFixedSize(true)
            recyclerView.layoutManager = layoutManager
            recyclerView.adapter = adapter
            recyclerView.addOnScrollListener(
                object : RecyclerView.OnScrollListener() {
                    override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                        super.onScrolled(recyclerView, dx, dy)

                        checkIfFetchNeeded()
                    }
                },
            )

            swipeRefreshLayout.setOnRefreshListener {
                viewModel.reset()
            }

            fastScroller.setRecyclerView(recyclerView)

            viewModel.communitiesData.observe(viewLifecycleOwner) {
                when (it) {
                    is StatefulData.Error -> {
                        swipeRefreshLayout.isRefreshing = false
                        loadingView.showDefaultErrorMessageFor(it.error)
                    }
                    is StatefulData.Loading -> {
                        loadingView.showProgressBar()
                    }
                    is StatefulData.NotStarted -> {}
                    is StatefulData.Success -> {
                        swipeRefreshLayout.isRefreshing = false
                        loadingView.hideAll()

                        adapter.items = it.data.data
                    }
                }
            }
        }
    }

    private class CommunitiesEngineAdapter(
        private val context: Context,
        private val rootView: View,
        private val offlineManager: OfflineManager,
        private val instance: String,
        private val onPageClick: (PageRef) -> Unit,
        private val onLoadPageClick: (Int) -> Unit,
    ) : Adapter<ViewHolder>() {

        var items: List<CommunitiesEngine.Item> = listOf()
            set(value) {
                field = value

                updateItems()
            }

        private val adapterHelper = AdapterHelper<CommunitiesEngine.Item>(
            areItemsTheSame = { old, new ->
                old::class == new::class && when (old) {
                    is CommunitiesEngine.Item.CommunityItem -> {
                        old.community.community.id ==
                            (new as CommunitiesEngine.Item.CommunityItem).community.community.id
                    }
                    CommunitiesEngine.Item.EmptyItem -> true
                    is CommunitiesEngine.Item.ErrorItem ->
                        old.pageIndex == (new as CommunitiesEngine.Item.ErrorItem).pageIndex
                    is CommunitiesEngine.Item.LoadItem ->
                        old.pageIndex == (new as CommunitiesEngine.Item.LoadItem).pageIndex
                }
            },
        ).apply {
            addItemType(
                clazz = CommunitiesEngine.Item.EmptyItem::class,
                inflateFn = EmptyItemBinding::inflate,
            ) { item, b, h ->
            }
            addItemType(
                clazz = CommunitiesEngine.Item.CommunityItem::class,
                inflateFn = CommunityDetailsItemBinding::inflate,
            ) { item, b, h ->
                val community = item.community

                b.overtext.text = "c/${community.community.name}@${community.community.instance}"
                b.title.text = community.community.title

                val mauString =
                    LemmyUtils.abbrevNumber(community.counts.users_active_month.toLong())
                val subsString =
                    LemmyUtils.abbrevNumber(community.counts.subscribers.toLong())

                @Suppress("SetTextI18n")
                b.stats.text =
                    "${context.getString(R.string.subscribers_format, subsString)}" +
                    " ${context.getString(R.string.mau_format, mauString)}"

                if (community.community.description == null) {
                    b.description.visibility = View.GONE
                } else {
                    b.description.visibility = View.VISIBLE

                    LemmyTextHelper.bindText(
                        textView = b.description,
                        text = community.community.description,
                        instance = instance,
                        highlight = null,
                        onImageClick = {},
                        onVideoClick = {},
                        onPageClick = {},
                        onLinkClick = { _, _, _ -> },
                        onLinkLongClick = { _, _ -> },
                    )
                }

                if (community.community.banner.isNullOrBlank()) {
                    b.banner.visibility = View.GONE
                } else {
                    b.banner.visibility = View.VISIBLE

                    b.banner.load(R.drawable.thumbnail_placeholder_16_9)
                    offlineManager.fetchImageWithError(
                        rootView,
                        community.community.banner,
                        {
                            b.banner.load(it)
                        },
                        {
                            b.banner.visibility = View.GONE
                        },
                    )
                }

                if (community.community.icon == null) {
                    context.getDrawableCompat(R.drawable.ic_community_24)
                        ?.tint(
                            context.getColorFromAttribute(
                                androidx.appcompat.R.attr.colorControlNormal,
                            ),
                        )
                        ?.let {
                            b.icon.setImageDrawable(it)
                        }
                        ?: run {
                            b.icon.setImageDrawable(null)
                        }
                } else {
                    offlineManager.fetchImage(rootView, community.community.icon) {
                        b.icon.load(it)
                    }
                }

                h.itemView.setOnClickListener {
                    onPageClick(community.community.toCommunityRef())
                }
            }
            addItemType(
                clazz = CommunitiesEngine.Item.LoadItem::class,
                inflateFn = CommunitiesLoadItemBinding::inflate,
            ) { item, b, h ->
                b.loadingView.showProgressBar()
            }
            addItemType(
                clazz = CommunitiesEngine.Item.ErrorItem::class,
                inflateFn = CommunitiesLoadItemBinding::inflate,
            ) { item, b, h ->
                b.loadingView.showDefaultErrorMessageFor(item.error)
                b.loadingView.setOnRefreshClickListener {
                    onLoadPageClick(item.pageIndex)
                }
            }
        }

        override fun getItemViewType(position: Int): Int =
            adapterHelper.getItemViewType(position)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
            adapterHelper.onCreateViewHolder(parent, viewType)

        override fun getItemCount(): Int = adapterHelper.itemCount

        override fun onBindViewHolder(holder: ViewHolder, position: Int) =
            adapterHelper.onBindViewHolder(holder, position)

        fun updateItems() {
            adapterHelper.setItems(items, this)
        }
    }
}
