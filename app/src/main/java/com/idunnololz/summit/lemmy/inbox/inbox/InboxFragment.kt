package com.idunnololz.summit.lemmy.inbox.inbox

import android.content.Context
import android.graphics.Typeface
import android.os.Bundle
import android.text.SpannableStringBuilder
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.HapticFeedbackConstantsCompat
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.LifecycleOwner
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.Adapter
import androidx.recyclerview.widget.RecyclerView.OnScrollListener
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import arrow.core.Either
import com.discord.panels.OverlappingPanelsLayout
import com.discord.panels.PanelState
import com.idunnololz.summit.R
import com.idunnololz.summit.account.info.AccountInfoManager
import com.idunnololz.summit.account.loadProfileImageOrDefault
import com.idunnololz.summit.accountUi.AccountsAndSettingsDialogFragment
import com.idunnololz.summit.accountUi.PreAuthDialogFragment
import com.idunnololz.summit.alert.OldAlertDialogFragment
import com.idunnololz.summit.api.NotAuthenticatedException
import com.idunnololz.summit.avatar.AvatarHelper
import com.idunnololz.summit.databinding.FragmentInboxBinding
import com.idunnololz.summit.databinding.InboxListItemBinding
import com.idunnololz.summit.databinding.InboxListLoaderItemBinding
import com.idunnololz.summit.databinding.ItemConversationBinding
import com.idunnololz.summit.databinding.ItemInboxHeaderBinding
import com.idunnololz.summit.databinding.ItemInboxWarningBinding
import com.idunnololz.summit.drafts.DraftData
import com.idunnololz.summit.error.ErrorDialogFragment
import com.idunnololz.summit.lemmy.LemmyTextHelper
import com.idunnololz.summit.lemmy.PageRef
import com.idunnololz.summit.lemmy.appendNameWithInstance
import com.idunnololz.summit.lemmy.comment.AddOrEditCommentFragment
import com.idunnololz.summit.lemmy.comment.AddOrEditCommentFragmentArgs
import com.idunnololz.summit.lemmy.inbox.InboxItem
import com.idunnololz.summit.lemmy.inbox.InboxSwipeToActionCallback
import com.idunnololz.summit.lemmy.inbox.InboxTabbedFragment
import com.idunnololz.summit.lemmy.inbox.PageType
import com.idunnololz.summit.lemmy.inbox.ReportItem
import com.idunnololz.summit.lemmy.inbox.conversation.Conversation
import com.idunnololz.summit.lemmy.inbox.conversation.ConversationsManager
import com.idunnololz.summit.lemmy.inbox.conversation.NewConversation
import com.idunnololz.summit.lemmy.personPicker.PersonPickerDialogFragment
import com.idunnololz.summit.lemmy.postAndCommentView.PostAndCommentViewBuilder
import com.idunnololz.summit.lemmy.utils.actions.MoreActionsHelper
import com.idunnololz.summit.lemmy.utils.actions.installOnActionResultHandler
import com.idunnololz.summit.lemmy.utils.addEllipsizeToSpannedOnLayout
import com.idunnololz.summit.lemmy.utils.setup
import com.idunnololz.summit.links.LinkContext
import com.idunnololz.summit.links.onLinkClick
import com.idunnololz.summit.preferences.Preferences
import com.idunnololz.summit.preview.VideoType
import com.idunnololz.summit.util.AnimationsHelper
import com.idunnololz.summit.util.BaseFragment
import com.idunnololz.summit.util.BottomMenu
import com.idunnololz.summit.util.PrettyPrintStyles
import com.idunnololz.summit.util.PrettyPrintUtils
import com.idunnololz.summit.util.StatefulData
import com.idunnololz.summit.util.dateStringToPretty
import com.idunnololz.summit.util.ext.getColorCompat
import com.idunnololz.summit.util.ext.performHapticFeedbackCompat
import com.idunnololz.summit.util.ext.setup
import com.idunnololz.summit.util.ext.showAllowingStateLoss
import com.idunnololz.summit.util.getParcelableCompat
import com.idunnololz.summit.util.insetViewAutomaticallyByPadding
import com.idunnololz.summit.util.insetViewExceptBottomAutomaticallyByMargins
import com.idunnololz.summit.util.recyclerView.AdapterHelper
import com.idunnololz.summit.util.setupForFragment
import com.idunnololz.summit.util.showMoreLinkOptions
import com.idunnololz.summit.util.tsToShortDate
import com.idunnololz.summit.video.VideoState
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class InboxFragment :
    BaseFragment<FragmentInboxBinding>(),
    OldAlertDialogFragment.AlertDialogFragmentListener {

    companion object {
        private val TAG = "InboxFragment"
    }

    private val args by navArgs<InboxFragmentArgs>()

    val viewModel: InboxViewModel by activityViewModels()

    @Inject
    lateinit var moreActionsHelper: MoreActionsHelper

    @Inject
    lateinit var postAndCommentViewBuilder: PostAndCommentViewBuilder

    @Inject
    lateinit var accountInfoManager: AccountInfoManager

    @Inject
    lateinit var preferences: Preferences

    @Inject
    lateinit var avatarHelper: AvatarHelper

    @Inject
    lateinit var animationsHelper: AnimationsHelper

    private var lastPageType: PageType? = null

    private var adapter: InboxItemAdapter? = null

    private val paneOnBackPressHandler = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            if (!isBindingAvailable()) return

            binding.paneLayout.closePanels()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (savedInstanceState == null) {
            requireMainActivity().apply {
                setupForFragment<InboxTabbedFragment>()
            }
        }

        childFragmentManager.setFragmentResultListener(
            PersonPickerDialogFragment.REQUEST_KEY,
            this,
        ) { key, bundle ->
            val result = bundle.getParcelableCompat<PersonPickerDialogFragment.Result>(
                PersonPickerDialogFragment.REQUEST_KEY_RESULT,
            )
            val accountId = viewModel.currentAccount.value?.id
            if (result != null && accountId != null) {
                (parentFragment as? InboxTabbedFragment)?.openConversation(
                    accountId = accountId,
                    conversation = Either.Right(
                        NewConversation(
                            result.personId,
                            result.personRef.instance,
                            result.personRef.name,
                            result.icon,
                        ),
                    ),
                    instance = viewModel.instance,
                )
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        super.onCreateView(inflater, container, savedInstanceState)

        setBinding(FragmentInboxBinding.inflate(inflater, container, false))

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val context = requireContext()
        val parentFragment = parentFragment as InboxTabbedFragment

        requireMainActivity().apply {
            setSupportActionBar(binding.toolbar)
            supportActionBar?.setDisplayShowHomeEnabled(true)
            supportActionBar?.setDisplayHomeAsUpEnabled(false)

            insetViewAutomaticallyByPaddingAndNavUi(
                lifecycleOwner = viewLifecycleOwner,
                rootView = binding.coordinatorLayoutContainer,
                applyTopInset = false,
            )
            insetViewAutomaticallyByPadding(viewLifecycleOwner, binding.startPane)
            insetViewExceptBottomAutomaticallyByMargins(viewLifecycleOwner, binding.toolbar)
        }

        viewModel.pageType.observe(viewLifecycleOwner) {
            it ?: return@observe

            Log.d(TAG, "Page type changed")

            getMainActivity()?.supportActionBar?.title = it.getName(context)
            binding.itemHighlighter.updateLayoutParams<ConstraintLayout.LayoutParams> {
                when (it) {
                    PageType.Unread -> {
                        topToTop = R.id.unread
                        bottomToBottom = R.id.unread
                    }

                    PageType.All -> {
                        topToTop = R.id.all
                        bottomToBottom = R.id.all
                    }

                    PageType.Replies -> {
                        topToTop = R.id.replies
                        bottomToBottom = R.id.replies
                    }

                    PageType.Mentions -> {
                        topToTop = R.id.mentions
                        bottomToBottom = R.id.mentions
                    }

                    PageType.Messages -> {
                        topToTop = R.id.messages
                        bottomToBottom = R.id.messages
                    }

                    PageType.Reports -> {
                        topToTop = R.id.reports
                        bottomToBottom = R.id.reports
                    }

                    PageType.Conversation -> error("unreachable")
                }
            }

            when (it) {
                PageType.Messages -> {
                    binding.fab.setImageResource(R.drawable.baseline_edit_24)
                    binding.fab.setOnClickListener {
                        PersonPickerDialogFragment.show(childFragmentManager)
                    }
                }

                PageType.Conversation -> error("unreachable")
                else -> {
                    binding.fab.setImageResource(R.drawable.baseline_done_all_24)
                    binding.fab.setOnClickListener {
                        viewModel.markAllAsRead()
                    }
                }
            }

            if (lastPageType != null && lastPageType != it) {
                binding.recyclerView.postDelayed(
                    {
                        if (isBindingAvailable()) {
                            binding.recyclerView.scrollToPosition(0)
                            binding.appBar.setExpanded(true)
                        }
                    },
                    100
                )
            }

            lastPageType = it
        }

        viewModel.inboxUpdate.observe(viewLifecycleOwner) {
            Log.d(TAG, "", RuntimeException())
            onUpdate()
        }
        viewModel.currentFullAccount.observe(viewLifecycleOwner) {
            if (it?.accountInfo?.miscAccountInfo?.modCommunityIds?.isNotEmpty() == true) {
                setModReportsVisibility(isVisible = true)
            } else {
                setModReportsVisibility(isVisible = false)
            }
        }

        val adapter = this.adapter
            ?: createAdapter()
        this.adapter = adapter

        binding.loadingView.setOnRefreshClickListener {
            if (accountInfoManager.currentFullAccount.value == null) {
                parentFragment.showLogin()
            } else {
                refresh(force = true)
            }
        }

        binding.swipeRefreshLayout.setOnRefreshListener {
            refresh(force = true)
        }

        binding.recyclerView.setHasFixedSize(true)
        binding.recyclerView.setup(animationsHelper)
        binding.recyclerView.layoutManager = LinearLayoutManager(context)
        binding.recyclerView.adapter = adapter
        binding.recyclerView.addOnScrollListener(
            object : OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    super.onScrolled(recyclerView, dx, dy)

                    val layoutManager = binding.recyclerView.layoutManager as? LinearLayoutManager
                        ?: return

                    if (layoutManager.findLastVisibleItemPosition() == adapter.itemCount - 1) {
                        if (!viewModel.inboxUpdate.isLoading && adapter.hasMore()) {
                            viewModel.fetchNextPage()
                        }
                    }
                }
            },
        )
        ItemTouchHelper(
            InboxSwipeToActionCallback(
                context,
                context.getColorCompat(R.color.style_green),
                R.drawable.baseline_check_24,
                binding.recyclerView,
            ) { viewHolder, _ ->
                val inboxItem = adapter.getItemAt(viewHolder.absoluteAdapterPosition)
                if (inboxItem != null) {
                    viewModel.markAsRead(
                        inboxItem = inboxItem,
                        read = true,
                    )
                }
            },
        ).attachToRecyclerView(binding.recyclerView)

        fun updatePaneBackPressHandler() {
            if (binding.paneLayout.getSelectedPanel() != OverlappingPanelsLayout.Panel.CENTER) {
                paneOnBackPressHandler.remove()
                requireMainActivity().onBackPressedDispatcher.addCallback(paneOnBackPressHandler)
            } else {
                paneOnBackPressHandler.remove()
            }
        }

        binding.paneLayout.setEndPanelLockState(OverlappingPanelsLayout.LockState.CLOSE)
        binding.paneLayout
            .registerStartPanelStateListeners(
                object : OverlappingPanelsLayout.PanelStateListener {
                    override fun onPanelStateChange(panelState: PanelState) {
                        when (panelState) {
                            PanelState.Closed -> {
                                getMainActivity()?.setNavUiOpenPercent(0f)

                                updatePaneBackPressHandler()
                            }
                            is PanelState.Closing -> {
                                getMainActivity()?.setNavUiOpenPercent(panelState.progress)
                            }
                            PanelState.Opened -> {
                                getMainActivity()?.setNavUiOpenPercent(100f)
                                updatePaneBackPressHandler()
                            }
                            is PanelState.Opening -> {
                                getMainActivity()?.setNavUiOpenPercent(panelState.progress)
                            }
                        }
                    }
                },
            )

        binding.toolbar.setNavigationIcon(R.drawable.baseline_menu_24)
        binding.toolbar.setNavigationOnClickListener {
            binding.paneLayout.openStartPanel()
        }

        binding.accountImageView.setOnClickListener {
            AccountsAndSettingsDialogFragment.newInstance()
                .showAllowingStateLoss(childFragmentManager, "AccountsDialogFragment")
        }

        with(binding) {
            unread.setOnClickListener {
                viewModel.setPageType(PageType.Unread)
                paneLayout.closePanels()
            }
            all.setOnClickListener {
                viewModel.setPageType(PageType.All)
                paneLayout.closePanels()
            }
            replies.setOnClickListener {
                viewModel.setPageType(PageType.Replies)
                paneLayout.closePanels()
            }
            mentions.setOnClickListener {
                viewModel.setPageType(PageType.Mentions)
                paneLayout.closePanels()
            }
            messages.setOnClickListener {
                viewModel.setPageType(PageType.Messages)
                paneLayout.closePanels()
            }
            reports.setOnClickListener {
                viewModel.setPageType(PageType.Reports)
                paneLayout.closePanels()
            }
        }

        viewModel.currentAccountView.observe(viewLifecycleOwner) {
            it.loadProfileImageOrDefault(binding.accountImageView)
        }
        viewModel.currentAccount.observe(viewLifecycleOwner) {
            adapter.accountId = it?.id
        }

        viewModel.markAsReadResult.observe(viewLifecycleOwner) {
            when (it) {
                is StatefulData.Error ->
                    ErrorDialogFragment.show(
                        message = getString(R.string.error_unable_to_mark_message_as_read),
                        error = it.error,
                        fm = childFragmentManager,
                    )
                is StatefulData.Loading -> {}
                is StatefulData.NotStarted -> {}
                is StatefulData.Success -> {}
            }
        }

        if (args.pageType == PageType.Reports) {
            binding.fab.visibility = View.GONE
        }
        binding.fab.setup(preferences)

        installOnActionResultHandler(
            moreActionsHelper = moreActionsHelper,
            snackbarContainer = binding.coordinatorLayout,
        )
    }

    private fun setModReportsVisibility(isVisible: Boolean) {
        if (!isBindingAvailable()) return

        with(binding) {
            val visibility = if (isVisible) View.VISIBLE else View.GONE
            divider.visibility = visibility
            reports.visibility = visibility
        }
    }

    private fun markAsRead(inboxItem: InboxItem, read: Boolean) {
        viewModel.markAsRead(inboxItem, read)
    }

    private fun createAdapter(): InboxItemAdapter {
        return InboxItemAdapter(
            context = requireContext(),
            accountId = viewModel.currentAccount.value?.id,
            postAndCommentViewBuilder = postAndCommentViewBuilder,
            instance = viewModel.instance,
            lifecycleOwner = viewLifecycleOwner,
            avatarHelper = avatarHelper,
            onImageClick = { url ->
                getMainActivity()?.openImage(
                    sharedElement = null,
                    appBar = binding.appBar,
                    title = null,
                    url = url,
                    mimeType = null
                )
            },
            onVideoClick = { url, videoType, state ->
                getMainActivity()?.openVideo(url, videoType, state)
            },
            onMarkAsRead = { view, inboxItem, read ->
                markAsRead(inboxItem, read)

                if (preferences.hapticsOnActions) {
                    view.performHapticFeedbackCompat(HapticFeedbackConstantsCompat.CONFIRM)
                }
            },
            onPageClick = {
                getMainActivity()?.launchPage(it)
            },
            onMessageClick = {
                val accountId = viewModel.currentAccount.value?.id ?: return@InboxItemAdapter
                if (!it.isRead && it !is ReportItem) {
                    markAsRead(it, read = true)
                }
                (parentFragment as? InboxTabbedFragment)?.openMessage(
                    accountId = accountId,
                    item = it,
                    instance = viewModel.instance,
                )
            },
            onConversationClick = {
                val accountId = viewModel.currentAccount.value?.id ?: return@InboxItemAdapter
                (parentFragment as? InboxTabbedFragment)?.openConversation(
                    accountId = accountId,
                    conversation = Either.Left(it),
                    instance = viewModel.instance,
                )
            },
            onAddCommentClick = { view, inboxItem ->
                if (accountInfoManager.currentFullAccount.value == null) {
                    PreAuthDialogFragment.newInstance(R.id.action_add_comment)
                        .show(childFragmentManager, "asdf")
                    return@InboxItemAdapter
                }

                AddOrEditCommentFragment().apply {
                    arguments =
                        AddOrEditCommentFragmentArgs(
                            instance = viewModel.instance,
                            commentView = null,
                            postView = null,
                            editCommentView = null,
                            inboxItem = inboxItem,
                        ).toBundle()
                }.show(childFragmentManager, "asdf")

                if (preferences.hapticsOnActions) {
                    view.performHapticFeedbackCompat(HapticFeedbackConstantsCompat.CONFIRM)
                }
            },
            onOverflowMenuClick = {
                getMainActivity()?.showBottomMenu(
                    BottomMenu(requireContext()).apply {
                        setTitle(R.string.message_actions)
                        addItem(io.noties.markwon.R.id.none, R.string.no_options)
                    },
                )
            },
            onSignInRequired = {
                PreAuthDialogFragment.newInstance()
                    .show(childFragmentManager, "asdf")
            },
            onInstanceMismatch = { accountInstance, apiInstance ->
                OldAlertDialogFragment.Builder()
                    .setTitle(R.string.error_account_instance_mismatch_title)
                    .setMessage(
                        getString(
                            R.string.error_account_instance_mismatch,
                            accountInstance,
                            apiInstance,
                        ),
                    )
                    .createAndShow(childFragmentManager, "aa")
            },
            onLinkClick = { url, text, linkType ->
                onLinkClick(url, text, linkType)
            },
            onLinkLongClick = { url, text ->
                getMainActivity()?.showMoreLinkOptions(url, text)
            },
        ).apply {
            stateRestorationPolicy = Adapter.StateRestorationPolicy.PREVENT_WHEN_EMPTY
        }
    }

    fun refresh(force: Boolean) {
        viewModel.refresh(force = force)
    }

    private fun onUpdate() {
        Log.d(TAG, "onUpdate: ${viewModel.inboxUpdate.value}")
        when (val data = viewModel.inboxUpdate.value) {
            is StatefulData.Error -> {
                binding.swipeRefreshLayout.isRefreshing = false
                if (data.error is NotAuthenticatedException) {
                    binding.loadingView.showErrorWithRetry(
                        getString(R.string.please_login_to_view_your_inbox),
                        getString(R.string.login),
                    )
                } else {
                    binding.loadingView.showDefaultErrorMessageFor(data.error)
                }
            }
            is StatefulData.Loading -> {
                if (!binding.swipeRefreshLayout.isRefreshing) {
                    binding.loadingView.showProgressBar()
                }
            }
            is StatefulData.NotStarted -> {}
            is StatefulData.Success -> {
                val inboxUpdate = data.data
                val inboxData = inboxUpdate.inboxModel
                val itemCount = inboxData.items.size
                val adapter = (binding.recyclerView.adapter as? InboxItemAdapter)

                if (data.data.isLoading) {
                    binding.loadingView.showProgressBar()
                } else if (itemCount == 0) {
                    binding.loadingView.showErrorWithRetry(
                        getString(R.string.there_doesnt_seem_to_be_anything_here),
                        getString(R.string.refresh),
                    )
                    binding.swipeRefreshLayout.isRefreshing = false
                } else {
                    binding.loadingView.hideAll()
                    binding.swipeRefreshLayout.isRefreshing = false
                }

                adapter?.setData(inboxData) {
                    if (inboxUpdate.scrollToTop) {
                        Log.d(TAG, "Scrolling back to top")
                        binding.recyclerView.scrollToPosition(0)
                    }
                }

                if (viewModel.pageType.value == PageType.All ||
                    viewModel.pageType.value == PageType.Unread
                ) {
                    viewModel.lastInboxUnreadLoadTimeMs.value = System.currentTimeMillis()
                }
            }
        }
    }

    override fun onPause() {
        super.onPause()

        binding.paneLayout.isEnabled = false
    }

    override fun onResume() {
        binding.paneLayout.isEnabled = true

        super.onResume()

//        viewModel.fetchInbox(0, force = true)
    }

    private class InboxItemAdapter(
        private val context: Context,
        var accountId: Long?,
        private val postAndCommentViewBuilder: PostAndCommentViewBuilder,
        private val instance: String,
        private val lifecycleOwner: LifecycleOwner,
        private val avatarHelper: AvatarHelper,
        private val onImageClick: (String) -> Unit,
        private val onVideoClick: (
            url: String,
            videoType: VideoType,
            videoState: VideoState?,
        ) -> Unit,
        private val onMarkAsRead: (View, InboxItem, Boolean) -> Unit,
        private val onPageClick: (PageRef) -> Unit,
        private val onMessageClick: (InboxItem) -> Unit,
        private val onConversationClick: (Conversation) -> Unit,
        private val onAddCommentClick: (View, InboxItem) -> Unit,
        private val onOverflowMenuClick: (InboxItem) -> Unit,
        private val onSignInRequired: () -> Unit,
        private val onInstanceMismatch: (String, String) -> Unit,
        private val onLinkClick: (url: String, text: String, linkContext: LinkContext) -> Unit,
        private val onLinkLongClick: (String, String) -> Unit,
    ) : RecyclerView.Adapter<ViewHolder>() {

        private sealed interface Item {

            data object HeaderItem : Item

            data class TooManyMessagesWarningItem(
                val earliestMessageTs: Long,
            ) : Item

            data class InboxListItem(
                val inboxItem: InboxItem,
            ) : Item

            data class ConversationItem(
                val conversation: Conversation,
                val draftMessage: DraftData.MessageDraftData?,
            ) : Item

            data class LoaderItem(
                val state: StatefulData<Unit>,
            ) : Item
        }

        private var inboxModel: InboxModel = InboxModel()

        private val adapterHelper = AdapterHelper<Item>(
            areItemsTheSame = { old, new ->
                old::class == new::class && when (old) {
                    is Item.HeaderItem -> true
                    is Item.InboxListItem ->
                        old.inboxItem.id == (new as Item.InboxListItem).inboxItem.id
                    is Item.ConversationItem ->
                        old.conversation.id == (new as Item.ConversationItem).conversation.id
                    is Item.LoaderItem -> true
                    is Item.TooManyMessagesWarningItem -> true
                }
            },
        ).apply {
            addItemType(Item.HeaderItem::class, ItemInboxHeaderBinding::inflate) { item, b, _ -> }
            addItemType(
                Item.TooManyMessagesWarningItem::class,
                ItemInboxWarningBinding::inflate,
            ) { item, b, h ->
                b.message.text = context.getString(
                    R.string.warn_too_many_messages,
                    PrettyPrintUtils.defaultDecimalFormat.format(
                        ConversationsManager.CONVERSATION_MAX_MESSAGE_REFRESH_LIMIT,
                    ),
                    tsToShortDate(item.earliestMessageTs),
                )
            }
            addItemType(Item.InboxListItem::class, InboxListItemBinding::inflate) { item, b, _ ->
                postAndCommentViewBuilder.bindMessage(
                    b = b,
                    instance = instance,
                    accountId = accountId,
                    viewLifecycleOwner = lifecycleOwner,
                    item = item.inboxItem,
                    onImageClick = onImageClick,
                    onVideoClick = onVideoClick,
                    onMarkAsRead = onMarkAsRead,
                    onPageClick = onPageClick,
                    onMessageClick = onMessageClick,
                    onAddCommentClick = onAddCommentClick,
                    onOverflowMenuClick = onOverflowMenuClick,
                    onSignInRequired = onSignInRequired,
                    onInstanceMismatch = onInstanceMismatch,
                    onLinkClick = onLinkClick,
                    onLinkLongClick = onLinkLongClick,
                )
            }
            addItemType(
                Item.ConversationItem::class,
                ItemConversationBinding::inflate,
            ) { item, b, _ ->
                val conversation = item.conversation
                val draftContent = item.draftMessage?.content?.let {
                    "(${context.getString(R.string.draft)}) $it"
                }

                if (conversation.isRead && draftContent == null) {
                    b.title.setTypeface(b.title.typeface, Typeface.NORMAL)
                    b.content.setTypeface(b.title.typeface, Typeface.NORMAL)
                    b.title.alpha = 0.6f
                    b.content.alpha = 0.6f
                } else {
                    b.title.setTypeface(b.title.typeface, Typeface.BOLD)
                    b.content.setTypeface(b.title.typeface, Typeface.BOLD)
                    b.title.alpha = 1f
                    b.content.alpha = 1f
                }

                avatarHelper.loadAvatar(
                    imageView = b.icon,
                    imageUrl = conversation.iconUrl,
                    personName = conversation.personName ?: "name",
                    personId = conversation.personId,
                    personInstance = conversation.personInstance ?: "instance",
                )
                b.title.text = SpannableStringBuilder().apply {
                    appendNameWithInstance(
                        context = context,
                        name = conversation.personName ?: "",
                        instance = conversation.personInstance,
                    )
                }

                LemmyTextHelper.bindText(
                    textView = b.content,
                    text = draftContent
                        ?: conversation.content
                        ?: "",
                    instance = instance,
                    onImageClick = {
                        onImageClick(it)
                    },
                    onVideoClick = { url ->
                        onVideoClick(url, VideoType.Unknown, null)
                    },
                    onPageClick = onPageClick,
                    onLinkClick = onLinkClick,
                    onLinkLongClick = onLinkLongClick,
                    showMediaAsLinks = true,
                )
                b.content.addEllipsizeToSpannedOnLayout()

                b.ts.text = dateStringToPretty(
                    context = context,
                    ts = conversation.ts,
                    style = PrettyPrintStyles.SHORT_DYNAMIC,
                )
                b.root.setOnClickListener {
                    onConversationClick(conversation)
                }
                b.root.setTag(R.id.swipe_enabled, false)
            }
            addItemType(Item.LoaderItem::class, InboxListLoaderItemBinding::inflate) { item, b, _ ->
                when (val state = item.state) {
                    is StatefulData.Error -> {
                        b.loadingView.showDefaultErrorMessageFor(state.error)
                    }
                    is StatefulData.Loading ->
                        b.loadingView.showProgressBar()
                    is StatefulData.NotStarted ->
                        b.loadingView.showProgressBar()
                    is StatefulData.Success ->
                        b.loadingView.showErrorText(R.string.no_more_items)
                }
                b.root.setTag(R.id.swipe_enabled, false)
            }
        }

        override fun getItemViewType(position: Int): Int = adapterHelper.getItemViewType(position)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
            adapterHelper.onCreateViewHolder(parent, viewType)

        override fun getItemCount(): Int = adapterHelper.itemCount

        override fun onBindViewHolder(holder: ViewHolder, position: Int) =
            adapterHelper.onBindViewHolder(holder, position)

        private fun refreshItems(cb: (() -> Unit)? = null) {
            val newItems = mutableListOf<Item>()

            newItems.add(Item.HeaderItem)

            val earliestMessageTs = inboxModel.earliestMessageTs
            if (earliestMessageTs != null) {
                newItems.add(Item.TooManyMessagesWarningItem(earliestMessageTs))
            }

            inboxModel.items.map { item ->
                when (item) {
                    is InboxListItem.ConversationItem -> {
                        newItems.add(
                            Item.ConversationItem(
                                item.conversation,
                                item.draftMessage,
                            ),
                        )
                    }
                    is InboxListItem.RegularInboxItem -> {
                        newItems.add(Item.InboxListItem(item.item))
                    }
                }
            }

            if (inboxModel.items.isNotEmpty()) {
                if (inboxModel.hasMore) {
                    newItems.add(Item.LoaderItem(StatefulData.NotStarted()))
                } else {
                    newItems.add(Item.LoaderItem(StatefulData.Success(Unit)))
                }
            }

            adapterHelper.setItems(newItems, this, cb)
        }

        fun hasMore(): Boolean = inboxModel.hasMore

        fun isEmpty(): Boolean = inboxModel.items.isEmpty()

        fun getItemAt(position: Int): InboxItem? =
            when (val item = adapterHelper.items.getOrNull(position)) {
                is Item.HeaderItem -> null
                is Item.InboxListItem -> item.inboxItem
                is Item.ConversationItem -> null
                is Item.LoaderItem -> null
                null -> null
                is Item.TooManyMessagesWarningItem -> null
            }

        fun setData(inboxModel: InboxModel, cb: (() -> Unit)? = null) {
            Log.d(TAG, "Data set! hasMore: ${inboxModel.hasMore}")
            this.inboxModel = inboxModel
            refreshItems(cb)
        }
    }

    override fun onPositiveClick(dialog: OldAlertDialogFragment, tag: String?) {
    }

    override fun onNegativeClick(dialog: OldAlertDialogFragment, tag: String?) {
    }
}
