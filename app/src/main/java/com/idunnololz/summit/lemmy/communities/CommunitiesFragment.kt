package com.idunnololz.summit.lemmy.communities

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.Adapter
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import coil3.load
import com.idunnololz.summit.R
import com.idunnololz.summit.api.dto.CommunityView
import com.idunnololz.summit.api.utils.instance
import com.idunnololz.summit.avatar.AvatarHelper
import com.idunnololz.summit.databinding.CommunitiesLoadItemBinding
import com.idunnololz.summit.databinding.CommunityDetailsItemBinding
import com.idunnololz.summit.databinding.EmptyItemBinding
import com.idunnololz.summit.databinding.FragmentCommunitiesBinding
import com.idunnololz.summit.lemmy.LemmyTextHelper
import com.idunnololz.summit.lemmy.LemmyUtils
import com.idunnololz.summit.lemmy.PageRef
import com.idunnololz.summit.lemmy.search.Item
import com.idunnololz.summit.lemmy.toCommunityRef
import com.idunnololz.summit.lemmy.utils.ListEngine
import com.idunnololz.summit.offline.OfflineManager
import com.idunnololz.summit.util.AnimationsHelper
import com.idunnololz.summit.util.BaseFragment
import com.idunnololz.summit.util.BottomMenu
import com.idunnololz.summit.util.StatefulData
import com.idunnololz.summit.util.TextMeasurementUtils
import com.idunnololz.summit.util.ext.navigateSafe
import com.idunnololz.summit.util.ext.setup
import com.idunnololz.summit.util.insetViewExceptBottomAutomaticallyByMargins
import com.idunnololz.summit.util.insetViewExceptTopAutomaticallyByPadding
import com.idunnololz.summit.util.recyclerView.AdapterHelper
import com.idunnololz.summit.util.setupForFragment
import com.idunnololz.summit.util.setupToolbar
import com.idunnololz.summit.util.shimmer.newShimmerDrawable16to9
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class CommunitiesFragment : BaseFragment<FragmentCommunitiesBinding>() {

    private val args by navArgs<CommunitiesFragmentArgs>()

    private val viewModel: CommunitiesViewModel by viewModels()

    @Inject
    lateinit var offlineManager: OfflineManager

    @Inject
    lateinit var animationsHelper: AnimationsHelper

    @Inject
    lateinit var avatarHelper: AvatarHelper

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

            setupToolbar(binding.toolbar, getString(R.string.communities))

            navBarController.updatePaddingForNavBar(binding.contentContainer)
        }

        viewModel.setCommunitiesInstance(args.instance)
        if (viewModel.communitiesData.isNotStarted) {
            viewModel.fetchCommunities(0)
        }

        runAfterLayout {
            setupView()
        }
    }

    private fun setupView() {
        val context = context ?: return

        with(binding) {
            val params = TextMeasurementUtils.TextMeasurementParams.Builder
                .from(descriptionMeasurementObject).build()

            val adapter = CommunitiesEngineAdapter(
                context = context,
                rootView = root,
                offlineManager = offlineManager,
                instance = viewModel.apiInstance,
                params = params,
                avatarHelper = avatarHelper,
                onPageClick = {
                    getMainActivity()?.launchPage(it)
                },
                onLoadPageClick = {
                    viewModel.fetchCommunities(it)
                },
                showMoreOptionsMenu = { communityView ->
                    val bottomMenu = BottomMenu(requireContext()).apply {
                        setTitle(R.string.community_actions)

                        addItemWithIcon(
                            id = R.id.community_info,
                            title = R.string.community_info,
                            icon = R.drawable.ic_community_24,
                        )

                        setOnMenuItemClickListener {
                            when (it.id) {
                                R.id.community_info -> {
                                    val direction = CommunitiesFragmentDirections
                                        .actionCommunitiesFragmentToCommunityInfoFragment(
                                            communityView.community.toCommunityRef(),
                                        )
                                    findNavController().navigateSafe(direction)
                                }
                            }
                        }
                    }

                    getMainActivity()?.showBottomMenu(bottomMenu, expandFully = false)
                },
            )
            val layoutManager = LinearLayoutManager(context)

            fun fetchPageIfLoadItem(position: Int) {
                (adapter.items.getOrNull(position) as? ListEngine.Item.LoadItem)
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
            recyclerView.setup(animationsHelper)
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
        private val params: TextMeasurementUtils.TextMeasurementParams,
        private val avatarHelper: AvatarHelper,
        private val onPageClick: (PageRef) -> Unit,
        private val onLoadPageClick: (Int) -> Unit,
        private val showMoreOptionsMenu: (CommunityView) -> Unit,
    ) : Adapter<ViewHolder>() {

        var items: List<ListEngine.Item<CommunityView>> = listOf()
            set(value) {
                field = value

                updateItems()
            }

        private val adapterHelper = AdapterHelper<ListEngine.Item<CommunityView>>(
            areItemsTheSame = { old, new ->
                old::class == new::class && when (old) {
                    is ListEngine.Item.DataItem -> {
                        old.data.community.id ==
                            (new as ListEngine.Item.DataItem).data.community.id
                    }
                    is ListEngine.Item.ErrorItem ->
                        old.pageIndex == (new as ListEngine.Item.ErrorItem).pageIndex
                    is ListEngine.Item.LoadItem ->
                        old.pageIndex == (new as ListEngine.Item.LoadItem).pageIndex
                    is ListEngine.Item.EmptyItem -> true
                }
            },
        ).apply {
            addItemType(
                clazz = ListEngine.Item.EmptyItem<CommunityView>()::class,
                inflateFn = EmptyItemBinding::inflate,
            ) { item, b, h ->
            }
            addItemType(
                clazz = ListEngine.Item.DataItem<CommunityView>(null)::class,
                inflateFn = CommunityDetailsItemBinding::inflate,
            ) { item, b, h ->
                val community = item.data

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
                    b.descriptionFade.visibility = View.GONE
                } else {
                    b.description.visibility = View.VISIBLE

                    val lineCount = TextMeasurementUtils.getTextLines(
                        LemmyTextHelper.getSpannable(context, community.community.description),
                        params,
                    ).size

                    if (lineCount > 4) {
                        b.descriptionFade.visibility = View.VISIBLE
                    } else {
                        b.descriptionFade.visibility = View.GONE
                    }

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
                    b.banner.load("file:///android_asset/banner_placeholder.svg")
                } else {
                    b.banner.load(newShimmerDrawable16to9(context))
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

                avatarHelper.loadCommunityIcon(b.icon, community.community)

                h.itemView.setOnClickListener {
                    onPageClick(community.community.toCommunityRef())
                }
                h.itemView.setOnLongClickListener {
                    showMoreOptionsMenu(item.data)
                    true
                }
                b.moreButton.setOnClickListener {
                    showMoreOptionsMenu(item.data)
                }
            }
            addItemType(
                clazz = ListEngine.Item.LoadItem<CommunityView>()::class,
                inflateFn = CommunitiesLoadItemBinding::inflate,
            ) { item, b, h ->
                b.loadingView.showProgressBar()
            }
            addItemType(
                clazz = ListEngine.Item.ErrorItem<CommunityView>()::class,
                inflateFn = CommunitiesLoadItemBinding::inflate,
            ) { item, b, h ->
                b.loadingView.showDefaultErrorMessageFor(item.error)
                b.loadingView.setOnRefreshClickListener {
                    onLoadPageClick(item.pageIndex)
                }
            }
        }

        override fun getItemViewType(position: Int): Int = adapterHelper.getItemViewType(position)

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
