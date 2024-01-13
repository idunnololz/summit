package com.idunnololz.summit.lemmy.modlogs

import android.content.Context
import android.graphics.RectF
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.URLSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.view.updatePadding
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.idunnololz.summit.R
import com.idunnololz.summit.api.utils.instance
import com.idunnololz.summit.databinding.CommunitiesLoadItemBinding
import com.idunnololz.summit.databinding.EmptyItemBinding
import com.idunnololz.summit.databinding.FragmentModLogsBinding
import com.idunnololz.summit.databinding.ModEventItemBinding
import com.idunnololz.summit.lemmy.LemmyTextHelper
import com.idunnololz.summit.lemmy.LinkResolver
import com.idunnololz.summit.lemmy.PageRef
import com.idunnololz.summit.lemmy.appendSeparator
import com.idunnololz.summit.lemmy.utils.ListEngine
import com.idunnololz.summit.links.LinkType
import com.idunnololz.summit.links.onLinkClick
import com.idunnololz.summit.offline.OfflineManager
import com.idunnololz.summit.util.BaseFragment
import com.idunnololz.summit.util.CustomLinkMovementMethod
import com.idunnololz.summit.util.DefaultLinkLongClickListener
import com.idunnololz.summit.util.LinkUtils
import com.idunnololz.summit.util.StatefulData
import com.idunnololz.summit.util.TextMeasurementUtils
import com.idunnololz.summit.util.Utils
import com.idunnololz.summit.util.dateStringToPretty
import com.idunnololz.summit.util.escapeMarkdown
import com.idunnololz.summit.util.recyclerView.AdapterHelper
import com.idunnololz.summit.util.showBottomMenuForLink
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
                instance = args.instance,
                availableWidth = binding.recyclerView.width,
                params = params,
                onPageClick = {
                    getMainActivity()?.launchPage(it)
                },
                onLoadPageClick = {
                    viewModel.fetchModLogs(it)
                },
                onLinkClick = { url: String, text: String, linkType: LinkType ->
                    onLinkClick(url, text, linkType)
                },
                onLinkLongClick = { url: String, text: String ->
                    getMainActivity()?.showBottomMenuForLink(url, text)
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

                        adapter.setItems(it.data.data) {
                            if (it.data.resetScrollPosition) {
                                layoutManager.scrollToPosition(0)
                            }
                        }
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
        private val onLinkClick: (url: String, text: String, linkType: LinkType) -> Unit,
        private val onLinkLongClick: (url: String, text: String) -> Unit,
    ) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

        var items: List<ListEngine.Item<ModEvent>> = listOf()
            private set

        private val summaryCharLength: Int

        init {
            val widthDp = Utils.convertPixelsToDp(availableWidth.toFloat())

            summaryCharLength = when {
                widthDp < 400f -> {
                    40
                }
                widthDp < 600f -> {
                    60
                }
                else -> {
                    80
                }
            }
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

                var description: String = modEvent::class.simpleName ?: ""
                var reason: String? = null

                when (modEvent) {
                    is ModEvent.AdminPurgeCommentViewEvent -> {
                        b.icon.setImageResource(R.drawable.baseline_delete_24)

                        description = context.getString(
                            R.string.purged_comment_format,
                            "[${modEvent.event.post.name.summarize()}](${LinkUtils.getLinkForPost(instance, modEvent.event.post.id)})",
                        )
                        reason = modEvent.event.admin_purge_comment.reason
                    }
                    is ModEvent.AdminPurgeCommunityViewEvent -> {
                        b.icon.setImageResource(R.drawable.baseline_delete_24)

                        description = context.getString(
                            R.string.purged_community,
                        )
                        reason = modEvent.event.admin_purge_community.reason
                    }
                    is ModEvent.AdminPurgePersonViewEvent -> {
                        b.icon.setImageResource(R.drawable.baseline_delete_24)

                        description = context.getString(
                            R.string.purged_person,
                        )
                        reason = modEvent.event.admin_purge_person.reason
                    }
                    is ModEvent.AdminPurgePostViewEvent -> {
                        b.icon.setImageResource(R.drawable.baseline_delete_24)

                        description = context.getString(
                            R.string.purged_post_format,
                            "[${modEvent.event.community.name}](${
                            LinkUtils.getLinkForCommunity(
                                modEvent.event.community.instance,
                                modEvent.event.community.name,
                            )})",
                        )
                        reason = modEvent.event.admin_purge_post.reason
                    }
                    is ModEvent.ModAddCommunityViewEvent -> {
                        if (modEvent.event.mod_add_community.removed) {
                            b.icon.setImageResource(R.drawable.outline_remove_moderator_24)

                            description = context.getString(
                                R.string.removed_moderator_for_community_format,
                                "[${modEvent.event.modded_person.name}](${LinkUtils.getLinkForPerson(instance, modEvent.event.modded_person.name)})",
                                "[${modEvent.event.community.name}](${
                                LinkUtils.getLinkForCommunity(
                                    modEvent.event.community.instance,
                                    modEvent.event.community.name,
                                )})",
                            )
                        } else {
                            b.icon.setImageResource(R.drawable.outline_add_moderator_24)

                            description = context.getString(
                                R.string.added_moderator_for_community_format,
                                "[${modEvent.event.modded_person.name}](${LinkUtils.getLinkForPerson(instance, modEvent.event.modded_person.name)})",
                                "[${modEvent.event.community.name}](${
                                LinkUtils.getLinkForCommunity(
                                    modEvent.event.community.instance,
                                    modEvent.event.community.name,
                                )})",
                            )
                        }
                    }
                    is ModEvent.ModAddViewEvent -> {
                        if (modEvent.event.mod_add.removed) {
                            b.icon.setImageResource(R.drawable.outline_remove_moderator_24)

                            description = context.getString(
                                R.string.removed_moderator_for_site_format,
                                "[${modEvent.event.modded_person.name}](${
                                LinkUtils.getLinkForPerson(
                                    instance,
                                    modEvent.event.modded_person.name,
                                )
                                })",
                                "[$instance](${LinkUtils.getLinkForInstance(instance)})",
                            )
                        } else {
                            b.icon.setImageResource(R.drawable.outline_add_moderator_24)

                            description = context.getString(
                                R.string.added_moderator_for_site_format,
                                "[${modEvent.event.modded_person.name}](${
                                LinkUtils.getLinkForPerson(
                                    instance,
                                    modEvent.event.modded_person.name,
                                )
                                })",
                                "[$instance](${LinkUtils.getLinkForInstance(instance)})",
                            )
                        }
                    }
                    is ModEvent.ModBanFromCommunityViewEvent -> {
                        if (modEvent.event.mod_ban_from_community.banned) {
                            b.icon.setImageResource(R.drawable.outline_person_remove_24)

                            description = context.getString(
                                R.string.banned_person_from_community_format,
                                "[${modEvent.event.banned_person.name}](${
                                LinkUtils.getLinkForPerson(
                                    instance,
                                    modEvent.event.banned_person.name,
                                )
                                })",
                                "[${modEvent.event.community.name}](${
                                LinkUtils.getLinkForCommunity(
                                    modEvent.event.community.instance,
                                    modEvent.event.community.name,
                                )
                                })",
                            )
                        } else {
                            b.icon.setImageResource(R.drawable.baseline_person_add_alt_24)

                            description = context.getString(
                                R.string.unbanned_person_from_community_format,
                                "[${modEvent.event.banned_person.name}](${
                                LinkUtils.getLinkForPerson(
                                    instance,
                                    modEvent.event.banned_person.name,
                                )
                                })",
                                "[${modEvent.event.community.name}](${
                                LinkUtils.getLinkForCommunity(
                                    modEvent.event.community.instance,
                                    modEvent.event.community.name,
                                )
                                })",
                            )
                        }
                        reason = modEvent.event.mod_ban_from_community.reason
                    }
                    is ModEvent.ModBanViewEvent -> {
                        if (modEvent.event.mod_ban.banned) {
                            b.icon.setImageResource(R.drawable.outline_person_remove_24)

                            description = context.getString(
                                R.string.banned_person_from_site_format,
                                "[${modEvent.event.banned_person.name}](${
                                LinkUtils.getLinkForPerson(
                                    instance,
                                    modEvent.event.banned_person.name,
                                )
                                })",
                                "[$instance](${LinkUtils.getLinkForInstance(instance)})",
                            )
                        } else {
                            b.icon.setImageResource(R.drawable.baseline_person_add_alt_24)

                            description = context.getString(
                                R.string.unbanned_person_from_site_format,
                                "[${modEvent.event.banned_person.name}](${
                                LinkUtils.getLinkForPerson(
                                    instance,
                                    modEvent.event.banned_person.name,
                                )
                                })",
                                "[$instance](${LinkUtils.getLinkForInstance(instance)})",
                            )
                        }
                        reason = modEvent.event.mod_ban.reason
                    }
                    is ModEvent.ModFeaturePostViewEvent -> {
                        if (modEvent.event.mod_feature_post.featured) {
                            b.icon.setImageResource(R.drawable.baseline_push_pin_24)

                            description = context.getString(
                                R.string.featured_post_in_community_format,
                                "[${modEvent.event.post.name.summarize()}](${
                                LinkUtils.getLinkForPost(
                                    instance,
                                    modEvent.event.post.id,
                                )
                                })",
                                "[${modEvent.event.community.name}](${
                                LinkUtils.getLinkForCommunity(
                                    modEvent.event.community.instance,
                                    modEvent.event.community.name,
                                )
                                })",
                            )
                        } else {
                            b.icon.setImageResource(R.drawable.ic_unpin_24)

                            description = context.getString(
                                R.string.unfeatured_post_in_community_format,
                                "[${modEvent.event.post.name.summarize()}](${
                                LinkUtils.getLinkForPost(
                                    instance,
                                    modEvent.event.post.id,
                                )
                                })",
                                "[${modEvent.event.community.name}](${
                                LinkUtils.getLinkForCommunity(
                                    modEvent.event.community.instance,
                                    modEvent.event.community.name,
                                )
                                })",
                            )
                        }
                    }
                    is ModEvent.ModHideCommunityViewEvent -> {
                        if (modEvent.event.mod_hide_community.hidden) {
                            b.icon.setImageResource(R.drawable.baseline_hide_24)

                            description = context.getString(
                                R.string.hidden_community_format,
                                "[${modEvent.event.community.name}](${
                                LinkUtils.getLinkForCommunity(
                                    modEvent.event.community.instance,
                                    modEvent.event.community.name,
                                )
                                })",
                            )
                        } else {
                            b.icon.setImageResource(R.drawable.baseline_expand_content_24)

                            description = context.getString(
                                R.string.unhidden_community_format,
                                "[${modEvent.event.community.name}](${
                                LinkUtils.getLinkForCommunity(
                                    modEvent.event.community.instance,
                                    modEvent.event.community.name,
                                )
                                })",
                            )
                        }
                        reason = modEvent.event.mod_hide_community.reason
                    }
                    is ModEvent.ModLockPostViewEvent -> {
                        if (modEvent.event.mod_lock_post.locked) {
                            b.icon.setImageResource(R.drawable.outline_lock_24)

                            description = context.getString(
                                R.string.locked_post_format,
                                "[${modEvent.event.post.name.summarize()}](${
                                LinkUtils.getLinkForPost(
                                    instance,
                                    modEvent.event.post.id,
                                )
                                })",
                                "[${modEvent.event.community.name}](${
                                LinkUtils.getLinkForCommunity(
                                    modEvent.event.community.instance,
                                    modEvent.event.community.name,
                                )
                                })",
                            )
                        } else {
                            b.icon.setImageResource(R.drawable.baseline_lock_open_24)

                            description = context.getString(
                                R.string.unlocked_post_format,
                                "[${modEvent.event.post.name.summarize()}](${
                                LinkUtils.getLinkForPost(
                                    instance,
                                    modEvent.event.post.id,
                                )
                                })",
                                "[${modEvent.event.community.name}](${
                                LinkUtils.getLinkForCommunity(
                                    modEvent.event.community.instance,
                                    modEvent.event.community.name,
                                )
                                })",
                            )
                        }
                    }
                    is ModEvent.ModRemoveCommentViewEvent -> {
                        if (modEvent.event.mod_remove_comment.removed) {
                            b.icon.setImageResource(R.drawable.baseline_remove_circle_outline_24)

                            description = context.getString(
                                R.string.removed_comment_format,
                                "[${modEvent.event.comment.content.summarize()}](${
                                LinkUtils.getLinkForComment(
                                    instance,
                                    modEvent.event.comment.id,
                                )
                                })",
                                "[${modEvent.event.post.name.summarize()}](${
                                LinkUtils.getLinkForPost(
                                    instance,
                                    modEvent.event.post.id,
                                )
                                })",
                            )
                        } else {
                            b.icon.setImageResource(R.drawable.baseline_undo_24)

                            description = context.getString(
                                R.string.unremoved_comment_format,
                                "[${modEvent.event.comment.content.summarize()}](${
                                LinkUtils.getLinkForComment(
                                    instance,
                                    modEvent.event.comment.id,
                                )
                                })",
                                "[${modEvent.event.post.name.summarize()}](${
                                LinkUtils.getLinkForPost(
                                    instance,
                                    modEvent.event.post.id,
                                )
                                })",
                            )
                        }
                        reason = modEvent.event.mod_remove_comment.reason
                    }
                    is ModEvent.ModRemoveCommunityViewEvent -> {
                        if (modEvent.event.mod_remove_community.removed) {
                            b.icon.setImageResource(R.drawable.baseline_remove_circle_outline_24)

                            description = context.getString(
                                R.string.removed_community_format,
                                "[${modEvent.event.community.name}](${
                                LinkUtils.getLinkForCommunity(
                                    modEvent.event.community.instance,
                                    modEvent.event.community.name,
                                )
                                })",
                                "[$instance](${LinkUtils.getLinkForInstance(instance)})",
                            )
                        } else {
                            b.icon.setImageResource(R.drawable.baseline_undo_24)

                            description = context.getString(
                                R.string.unremoved_community_format,
                                "[${modEvent.event.community.name}](${
                                LinkUtils.getLinkForCommunity(
                                    modEvent.event.community.instance,
                                    modEvent.event.community.name,
                                )
                                })",
                                "[$instance](${LinkUtils.getLinkForInstance(instance)})",
                            )
                        }
                        reason = modEvent.event.mod_remove_community.reason
                    }
                    is ModEvent.ModRemovePostViewEvent -> {
                        if (modEvent.event.mod_remove_post.removed) {
                            b.icon.setImageResource(R.drawable.baseline_remove_circle_outline_24)

                            description = context.getString(
                                R.string.removed_post_format,
                                "[${modEvent.event.post.name.summarize()}](${
                                LinkUtils.getLinkForPost(
                                    instance,
                                    modEvent.event.post.id,
                                )
                                })",
                                "[${modEvent.event.community.name}](${
                                LinkUtils.getLinkForCommunity(
                                    modEvent.event.community.instance,
                                    modEvent.event.community.name,
                                )
                                })",
                            )
                        } else {
                            b.icon.setImageResource(R.drawable.baseline_undo_24)

                            description = context.getString(
                                R.string.unremoved_post_format,
                                "[${modEvent.event.post.name.summarize()}](${
                                LinkUtils.getLinkForPost(
                                    instance,
                                    modEvent.event.post.id,
                                )
                                })",
                                "[${modEvent.event.community.name}](${
                                LinkUtils.getLinkForCommunity(
                                    modEvent.event.community.instance,
                                    modEvent.event.community.name,
                                )
                                })",
                            )
                        }
                        reason = modEvent.event.mod_remove_post.reason
                    }
                    is ModEvent.ModTransferCommunityViewEvent -> {
                        b.icon.setImageResource(R.drawable.baseline_swap_horiz_24)

                        description = context.getString(
                            R.string.transferred_ownership_of_community_format,
                            "[${modEvent.event.community.name}](${
                            LinkUtils.getLinkForCommunity(
                                modEvent.event.community.instance,
                                modEvent.event.community.name,
                            )})",
                            "[${modEvent.event.modded_person.name}](${LinkUtils.getLinkForPerson(instance, modEvent.event.modded_person.name)})",
                        )
                    }
                }

                b.overtext.text = SpannableStringBuilder().apply {
                    append(dateStringToPretty(context, modEvent.ts))
                    appendSeparator()
                    append(
                        context.getString(
                            R.string.mod_action_format,
                            modEvent.actionType.toString(),
                        ),
                    )

                    val agent = modEvent.agent
                    if (agent != null) {
                        appendSeparator()
                        val s = length
                        append(agent.name)
                        val e = length
                        setSpan(
                            URLSpan(LinkUtils.getLinkForPerson(instance, agent.name)),
                            s,
                            e,
                            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE,
                        )
                    }
                }
                b.overtext.movementMethod = CustomLinkMovementMethod().apply {
                    onLinkClickListener = object : CustomLinkMovementMethod.OnLinkClickListener {
                        override fun onClick(
                            textView: TextView,
                            url: String,
                            text: String,
                            rect: RectF,
                        ): Boolean {
                            val pageRef = LinkResolver.parseUrl(url, instance)

                            if (pageRef != null) {
                                onPageClick(pageRef)
                            } else {
                                onLinkClick(url, text, LinkType.Text)
                            }
                            return true
                        }
                    }
                    onLinkLongClickListener = DefaultLinkLongClickListener(
                        b.root.context,
                        onLinkLongClick,
                    )
                }
                LemmyTextHelper.bindText(
                    textView = b.title,
                    text = description,
                    instance = instance,
                    onImageClick = {},
                    onVideoClick = {},
                    onPageClick = {
                        onPageClick(it)
                    },
                    onLinkClick = { url, text, linkType ->
                        onLinkClick(url, text, linkType)
                    },
                    onLinkLongClick = { url, text ->
                        onLinkLongClick(url, text)
                    },
                )
                if (reason == null) {
                    b.body.visibility = View.GONE
                } else {
                    b.body.visibility = View.VISIBLE
                    b.body.text = context.getString(R.string.reason_format, reason)
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

        fun updateItems(cb: () -> Unit) {
            adapterHelper.setItems(items, this, cb)
        }

        fun setItems(newItems: List<ListEngine.Item<ModEvent>>, cb: () -> Unit) {
            items = newItems

            updateItems(cb)
        }

        private fun String.summarize() =
            if (this.length > summaryCharLength + 3) {
                this.take(summaryCharLength) + "…"
            } else {
                this
            }.escapeMarkdown()
    }
}
