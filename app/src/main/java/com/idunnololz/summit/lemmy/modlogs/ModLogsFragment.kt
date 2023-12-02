package com.idunnololz.summit.lemmy.modlogs

import android.content.Context
import android.os.Bundle
import android.text.SpannableStringBuilder
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.updatePadding
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.idunnololz.summit.R
import com.idunnololz.summit.api.dto.CommunityView
import com.idunnololz.summit.databinding.CommunitiesLoadItemBinding
import com.idunnololz.summit.databinding.EmptyItemBinding
import com.idunnololz.summit.databinding.FragmentModLogsBinding
import com.idunnololz.summit.databinding.ModEventItemBinding
import com.idunnololz.summit.lemmy.LemmyHeaderHelper
import com.idunnololz.summit.lemmy.PageRef
import com.idunnololz.summit.lemmy.appendSeparator
import com.idunnololz.summit.lemmy.communities.CommunitiesFragmentDirections
import com.idunnololz.summit.lemmy.toCommunityRef
import com.idunnololz.summit.lemmy.utils.ListEngine
import com.idunnololz.summit.offline.OfflineManager
import com.idunnololz.summit.util.BaseFragment
import com.idunnololz.summit.util.BottomMenu
import com.idunnololz.summit.util.StatefulData
import com.idunnololz.summit.util.TextMeasurementUtils
import com.idunnololz.summit.util.dateStringToPretty
import com.idunnololz.summit.util.ext.navigateSafe
import com.idunnololz.summit.util.recyclerView.AdapterHelper
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class ModLogsFragment : BaseFragment<FragmentModLogsBinding>() {

    private val args by navArgs<ModLogsFragmentArgs>()

    private val viewModel: ModLogsViewModel by viewModels()

    @Inject
    lateinit var offlineManager: OfflineManager

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        super.onCreateView(inflater, container, savedInstanceState)

        requireMainActivity().apply {
            setupForFragment<ModLogsFragment>()
        }

        setBinding(FragmentModLogsBinding.inflate(inflater, container, false))

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
            supportActionBar?.title = getString(R.string.mod_logs)

            binding.contentContainer.updatePadding(bottom = getBottomNavHeight())
        }

        viewModel.setArguments(args.instance, args.communityRef)
        if (viewModel.modLogData.isNotStarted) {
            viewModel.fetchModLogs(0)
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

            val adapter = ModEventsAdapter(
                context = context,
                rootView = root,
                offlineManager = offlineManager,
                instance = viewModel.apiInstance,
                params = params,
                onPageClick = {
                    getMainActivity()?.launchPage(it)
                },
                onLoadPageClick = {
                    viewModel.fetchModLogs(it)
                },
                showMoreOptionsMenu = { communityView ->
                    val bottomMenu = BottomMenu(requireContext()).apply {
                        setTitle(R.string.community_actions)

                        addItemWithIcon(
                            id = R.id.community_info,
                            title = R.string.community_info,
                            icon = R.drawable.ic_community_24
                        )

                        setOnMenuItemClickListener {
                            when (it.id) {
                                R.id.community_info -> {
                                    val direction = CommunitiesFragmentDirections
                                        .actionCommunitiesFragmentToCommunityInfoFragment(
                                            communityView.community.toCommunityRef()
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
                        viewModel.fetchModLogs(it)
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

            viewModel.modLogData.observe(viewLifecycleOwner) {
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

    private class ModEventsAdapter(
        private val context: Context,
        private val rootView: View,
        private val offlineManager: OfflineManager,
        private val instance: String,
        private val params: TextMeasurementUtils.TextMeasurementParams,
        private val onPageClick: (PageRef) -> Unit,
        private val onLoadPageClick: (Int) -> Unit,
        private val showMoreOptionsMenu: (CommunityView) -> Unit,
    ) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

        var items: List<ListEngine.Item<ModEvent>> = listOf()
            set(value) {
                field = value

                updateItems()
            }

        private val adapterHelper = AdapterHelper<ListEngine.Item<ModEvent>>(
            areItemsTheSame = { old, new ->
                old::class == new::class && when (old) {
                    is ListEngine.Item.DataItem -> {
                        old.data.id ==
                            (new as ListEngine.Item.DataItem).data.id
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
                clazz = ListEngine.Item.EmptyItem<ModEvent>()::class,
                inflateFn = EmptyItemBinding::inflate,
            ) { item, b, h ->
            }
            addItemType(
                clazz = ListEngine.Item.DataItem<ModEvent>(null)::class,
                inflateFn = ModEventItemBinding::inflate,
            ) { item, b, h ->
                val modEvent = item.data

                val timestampString: String

                when (modEvent) {
                    is ModEvent.AdminPurgeCommentViewEvent -> {
                        timestampString = modEvent.event.admin_purge_comment.when_
                    }
                    is ModEvent.AdminPurgeCommunityViewEvent -> {
                        timestampString = modEvent.event.admin_purge_community.when_
                    }
                    is ModEvent.AdminPurgePersonViewEvent -> {
                        timestampString = modEvent.event.admin_purge_person.when_
                    }
                    is ModEvent.AdminPurgePostViewEvent -> {
                        timestampString = modEvent.event.admin_purge_post.when_
                    }
                    is ModEvent.ModAddCommunityViewEvent -> {
                        timestampString = modEvent.event.mod_add_community.when_
                    }
                    is ModEvent.ModAddViewEvent -> {
                        timestampString = modEvent.event.mod_add.when_
                    }
                    is ModEvent.ModBanFromCommunityViewEvent -> {
                        timestampString = modEvent.event.mod_ban_from_community.when_
                    }
                    is ModEvent.ModBanViewEvent -> {
                        timestampString = modEvent.event.mod_ban.when_
                    }
                    is ModEvent.ModFeaturePostViewEvent -> {
                        timestampString = modEvent.event.mod_feature_post.when_
                    }
                    is ModEvent.ModHideCommunityViewEvent -> {
                        timestampString = modEvent.event.mod_hide_community.when_
                    }
                    is ModEvent.ModLockPostViewEvent -> {
                        timestampString = modEvent.event.mod_lock_post.when_
                    }
                    is ModEvent.ModRemoveCommentViewEvent -> {
                        timestampString = modEvent.event.mod_remove_comment.when_
                    }
                    is ModEvent.ModRemoveCommunityViewEvent -> {
                        timestampString = modEvent.event.mod_remove_community.when_
                    }
                    is ModEvent.ModRemovePostViewEvent -> {
                        timestampString = modEvent.event.mod_remove_post.when_
                    }
                    is ModEvent.ModTransferCommunityViewEvent -> {
                        timestampString = modEvent.event.mod_transfer_community.when_
                    }
                }

                b.overtext.text = SpannableStringBuilder().apply {
                    append(dateStringToPretty(context, modEvent.ts))
                    appendSeparator()
                    append(modEvent.actionType.toString())
                }
            }
            addItemType(
                clazz = ListEngine.Item.LoadItem<ModEvent>()::class,
                inflateFn = CommunitiesLoadItemBinding::inflate,
            ) { item, b, h ->
                b.loadingView.showProgressBar()
            }
            addItemType(
                clazz = ListEngine.Item.ErrorItem<ModEvent>()::class,
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

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder =
            adapterHelper.onCreateViewHolder(parent, viewType)

        override fun getItemCount(): Int = adapterHelper.itemCount

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) =
            adapterHelper.onBindViewHolder(holder, position)

        fun updateItems() {
            adapterHelper.setItems(items, this)
        }
    }
}