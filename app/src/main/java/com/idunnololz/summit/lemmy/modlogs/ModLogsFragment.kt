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
import com.idunnololz.summit.lemmy.LemmyTextHelper
import com.idunnololz.summit.lemmy.PageRef
import com.idunnololz.summit.lemmy.appendSeparator
import com.idunnololz.summit.lemmy.communities.CommunitiesFragmentDirections
import com.idunnololz.summit.lemmy.toCommunityRef
import com.idunnololz.summit.lemmy.utils.ListEngine
import com.idunnololz.summit.offline.OfflineManager
import com.idunnololz.summit.util.BaseFragment
import com.idunnololz.summit.util.BottomMenu
import com.idunnololz.summit.util.LinkUtils
import com.idunnololz.summit.util.StatefulData
import com.idunnololz.summit.util.TextMeasurementUtils
import com.idunnololz.summit.util.Utils
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
                availableWidth = binding.recyclerView.width,
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
        private val availableWidth: Int,
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

        private val summaryCharLength: Int

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

                var description: String = modEvent::class.simpleName ?: ""

                when (modEvent) {
                    is ModEvent.AdminPurgeCommentViewEvent -> {
                        description = context.getString(
                            R.string.purged_comment_format,
                            "[${modEvent.event.post.name}](${LinkUtils.getLinkForPost(instance, modEvent.event.post.id)})",
                        )
                    }
                    is ModEvent.AdminPurgeCommunityViewEvent -> {
                        description = context.getString(
                            R.string.purged_community,
                        )
                    }
                    is ModEvent.AdminPurgePersonViewEvent -> {
                        description = context.getString(
                            R.string.purged_person,
                        )
                    }
                    is ModEvent.AdminPurgePostViewEvent -> {
                        description = context.getString(
                            R.string.purged_post_format,
                            "[${modEvent.event.community.name}](${LinkUtils.getLinkForComment(instance, modEvent.event.community.id)})",
                        )
                    }
                    is ModEvent.ModAddCommunityViewEvent -> {
                        description = context.getString(
                            R.string.added_moderator_for_community_format,
                            "[${modEvent.event.modded_person.display_name}](${LinkUtils.getLinkForPerson(instance, modEvent.event.modded_person.name)})",
                            "[${modEvent.event.community.name}](${LinkUtils.getLinkForCommunity(instance, modEvent.event.community.name)})",
                        )
                    }
                    is ModEvent.ModAddViewEvent -> {
                        description = context.getString(
                            R.string.added_moderator_for_site_format,
                            "[${modEvent.event.modded_person.display_name}](${LinkUtils.getLinkForPerson(instance, modEvent.event.modded_person.name)})",
                            "[${instance}](${LinkUtils.getLinkForInstance(instance)})",
                        )
                    }
                    is ModEvent.ModBanFromCommunityViewEvent -> {
                        description = context.getString(
                            R.string.banned_person_from_community_format,
                            "[${modEvent.event.banned_person.display_name}](${LinkUtils.getLinkForPerson(instance, modEvent.event.banned_person.name)})",
                            "[${modEvent.event.community.name}](${LinkUtils.getLinkForCommunity(instance, modEvent.event.community.name)})",
                        )
                    }
                    is ModEvent.ModBanViewEvent -> {
                        description = context.getString(
                            R.string.banned_person_from_site_format,
                            "[${modEvent.event.banned_person.display_name}](${LinkUtils.getLinkForPerson(instance, modEvent.event.banned_person.name)})",
                            "[${instance}](${LinkUtils.getLinkForInstance(instance)})",
                        )
                    }
                    is ModEvent.ModFeaturePostViewEvent -> {
                        description = context.getString(
                            R.string.featured_post_in_community_format,
                            "[${modEvent.event.post.name}](${LinkUtils.getLinkForPost(instance, modEvent.event.post.id)})",
                            "[${modEvent.event.community.name}](${LinkUtils.getLinkForComment(instance, modEvent.event.community.id)})",
                        )
                    }
                    is ModEvent.ModHideCommunityViewEvent -> {
                        description = context.getString(
                            R.string.hidden_community_format,
                            "[${modEvent.event.community.name}](${LinkUtils.getLinkForComment(instance, modEvent.event.community.id)})",
                        )
                    }
                    is ModEvent.ModLockPostViewEvent -> {
                        description = context.getString(
                            R.string.locked_post_format,
                            "[${modEvent.event.post.name}](${LinkUtils.getLinkForPost(instance, modEvent.event.post.id)})",
                            "[${modEvent.event.community.name}](${LinkUtils.getLinkForComment(instance, modEvent.event.community.id)})",
                        )
                    }
                    is ModEvent.ModRemoveCommentViewEvent -> {
                        description = context.getString(
                            R.string.removed_comment_format,
                            "[${modEvent.event.comment.content}](${LinkUtils.getLinkForComment(instance, modEvent.event.comment.id)})",
                            "[${modEvent.event.post.name}](${LinkUtils.getLinkForPost(instance, modEvent.event.post.id)})",
                        )
                    }
                    is ModEvent.ModRemoveCommunityViewEvent -> {
                        description = context.getString(
                            R.string.removed_community_format,
                            "[${modEvent.event.community.name}](${LinkUtils.getLinkForCommunity(instance, modEvent.event.community.name)})",
                            "[${instance}](${LinkUtils.getLinkForInstance(instance)})",
                        )
                    }
                    is ModEvent.ModRemovePostViewEvent -> {
                        description = context.getString(
                            R.string.removed_community_format,
                            "[${modEvent.event.community.name}](${LinkUtils.getLinkForCommunity(instance, modEvent.event.community.name)})",
                            "[${instance}](${LinkUtils.getLinkForInstance(instance)})",
                        )
                    }
                    is ModEvent.ModTransferCommunityViewEvent -> {
                        description = context.getString(
                            R.string.transferred_ownership_of_community_format,
                            "[${modEvent.event.community.name}](${LinkUtils.getLinkForCommunity(instance, modEvent.event.community.name)})",
                            "[${modEvent.event.modded_person.display_name}](${LinkUtils.getLinkForPerson(instance, modEvent.event.modded_person.name)})",
                        )
                    }
                }

                b.overtext.text = SpannableStringBuilder().apply {
                    append(dateStringToPretty(context, modEvent.ts))
                    appendSeparator()
                    append(context.getString(
                        R.string.mod_action_format, modEvent.actionType.toString()))
                }
                LemmyTextHelper.bindText(
                    textView = b.title,
                    text = description,
                    instance = instance,
                    onImageClick = {},
                    onVideoClick = {},
                    onPageClick = {},
                    onLinkClick = { _, _, _ -> },
                    onLinkLongClick = { _, _ -> },
                )
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

        init {
            val widthDp = Utils.convertPixelsToDp(availableWidth.toFloat())

            summaryCharLength = when {
                widthDp < 400f -> {
                    60
                }
                widthDp < 600f -> {
                    80
                }
                else -> {
                    100
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