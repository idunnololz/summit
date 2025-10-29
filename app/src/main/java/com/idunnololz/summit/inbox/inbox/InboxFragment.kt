package com.idunnololz.summit.inbox.inbox

import android.content.Context
import android.graphics.Typeface
import android.os.Bundle
import android.text.SpannableStringBuilder
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.core.view.HapticFeedbackConstantsCompat
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
import com.idunnololz.summit.alert.launchAlertDialog
import com.idunnololz.summit.api.NotAuthenticatedException
import com.idunnololz.summit.avatar.AvatarHelper
import com.idunnololz.summit.databinding.FragmentInboxBinding
import com.idunnololz.summit.databinding.InboxListItemBinding
import com.idunnololz.summit.databinding.InboxListLoaderItemBinding
import com.idunnololz.summit.databinding.InboxListRegistrationApplicationItemBinding
import com.idunnololz.summit.databinding.ItemConversationBinding
import com.idunnololz.summit.databinding.ItemInboxHeaderBinding
import com.idunnololz.summit.databinding.ItemInboxWarningBinding
import com.idunnololz.summit.drafts.DraftData
import com.idunnololz.summit.error.ErrorDialogFragment
import com.idunnololz.summit.inbox.InboxItem
import com.idunnololz.summit.inbox.InboxItem.RegistrationApplicationInboxItem
import com.idunnololz.summit.inbox.InboxSwipeToActionCallback
import com.idunnololz.summit.inbox.InboxTabbedFragment
import com.idunnololz.summit.inbox.PageType
import com.idunnololz.summit.inbox.ReportItem
import com.idunnololz.summit.inbox.conversation.Conversation
import com.idunnololz.summit.inbox.conversation.ConversationsManager
import com.idunnololz.summit.inbox.conversation.NewConversation
import com.idunnololz.summit.lemmy.LemmyTextHelper
import com.idunnololz.summit.lemmy.PageRef
import com.idunnololz.summit.lemmy.appendNameWithInstance
import com.idunnololz.summit.lemmy.comment.AddOrEditCommentFragment
import com.idunnololz.summit.lemmy.comment.AddOrEditCommentFragmentArgs
import com.idunnololz.summit.lemmy.personPicker.PersonPickerDialogFragment
import com.idunnololz.summit.lemmy.postAndCommentView.PostAndCommentViewBuilder
import com.idunnololz.summit.lemmy.utils.actions.MoreActionsHelper
import com.idunnololz.summit.lemmy.utils.actions.installOnActionResultHandler
import com.idunnololz.summit.lemmy.utils.addEllipsizeToSpannedOnLayout
import com.idunnololz.summit.lemmy.utils.setup
import com.idunnololz.summit.links.LinkContext
import com.idunnololz.summit.links.onLinkClick
import com.idunnololz.summit.preferences.InboxFabActionId
import com.idunnololz.summit.preferences.InboxLayoutId
import com.idunnololz.summit.preferences.Preferences
import com.idunnololz.summit.preview.VideoType
import com.idunnololz.summit.reason.ReasonDialogFragment
import com.idunnololz.summit.util.AnimationsHelper
import com.idunnololz.summit.util.BaseFragment
import com.idunnololz.summit.util.BottomMenu
import com.idunnololz.summit.util.PrettyPrintStyles
import com.idunnololz.summit.util.PrettyPrintUtils
import com.idunnololz.summit.util.StatefulData
import com.idunnololz.summit.util.ext.getColorCompat
import com.idunnololz.summit.util.ext.getDimen
import com.idunnololz.summit.util.ext.performHapticFeedbackCompat
import com.idunnololz.summit.util.ext.setup
import com.idunnololz.summit.util.ext.showAllowingStateLoss
import com.idunnololz.summit.util.getParcelableCompat
import com.idunnololz.summit.util.insetViewAutomaticallyByMargins
import com.idunnololz.summit.util.insetViewAutomaticallyByPadding
import com.idunnololz.summit.util.insetViewExceptBottomAutomaticallyByMargins
import com.idunnololz.summit.util.recyclerView.AdapterHelper
import com.idunnololz.summit.util.setupForFragment
import com.idunnololz.summit.util.setupToolbar
import com.idunnololz.summit.util.showMoreLinkOptions
import com.idunnololz.summit.util.tsToConcise
import com.idunnololz.summit.util.tsToShortDate
import com.idunnololz.summit.video.VideoState
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class InboxFragment : BaseFragment<FragmentInboxBinding>() {

  companion object {
    private const val TAG = "InboxFragment"
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

  @Inject
  lateinit var lemmyTextHelper: LemmyTextHelper

  @Inject
  lateinit var inboxPaneControllerFactory: InboxPaneController.Factory

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
      requireSummitActivity().apply {
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
              personId = result.personRef.id,
              personInstance = result.personRef.instance,
              personName = result.personRef.name,
              personAvatar = result.icon,
            ),
          ),
          instance = viewModel.instance,
        )
      }
    }

    childFragmentManager.setFragmentResultListener(
      ReasonDialogFragment.REQUEST_KEY,
      this,
    ) { key, bundle ->
      val result = bundle.getParcelableCompat<ReasonDialogFragment.Result>(
        ReasonDialogFragment.RESULT_KEY,
      )
      if (result != null) {
        if (result.isOk) {
          viewModel.approveRegistrationApplication(
            applicationId = viewModel.lastApplicationId.value ?: return@setFragmentResultListener,
            approve = false,
            denyReason = result.reason,
          )
        }
      }
    }

    childFragmentManager.setFragmentResultListener(
      AddOrEditCommentFragment.REQUEST_KEY,
      this,
    ) { key, bundle ->
      bundle.getParcelableCompat<AddOrEditCommentFragment.Result>(
        AddOrEditCommentFragment.REQUEST_KEY_RESULT,
      )?.let { result ->
        val lastInboxItemSelected = result.inboxItem
        if (result.wasCommentSent && lastInboxItemSelected != null) {
          viewModel.markAsRead(inboxItem = lastInboxItemSelected, read = true)
        }
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

    requireSummitActivity().apply {
      setupToolbar(binding.toolbar, "")

      insetViewAutomaticallyByPaddingAndNavUi(
        lifecycleOwner = viewLifecycleOwner,
        rootView = binding.coordinatorLayoutContainer,
        applyTopInset = false,
      )
      insetViewAutomaticallyByPadding(viewLifecycleOwner, binding.startPanel.root)
      insetViewExceptBottomAutomaticallyByMargins(viewLifecycleOwner, binding.toolbar)
      insetViewAutomaticallyByMargins(
        viewLifecycleOwner,
        binding.newItemsProgressBar,
        context.getDimen(R.dimen.padding),
      )
    }

    viewModel.pageType.observe(viewLifecycleOwner) {
      it ?: return@observe

      Log.d(TAG, "Page type changed")

      binding.toolbar.title = it.getName(context)

      when (it) {
        PageType.Messages -> {
          binding.fab.visibility = View.VISIBLE
          binding.fab.setImageResource(R.drawable.baseline_edit_24)
          binding.fab.setOnClickListener {
            PersonPickerDialogFragment.show(childFragmentManager)
          }
        }
        PageType.Reports -> {
          binding.fab.visibility = View.GONE
        }
        PageType.Applications -> {
          binding.fab.visibility = View.GONE
        }

        PageType.Conversation -> error("unreachable")
        else -> {
          when (preferences.inboxFabAction) {
            InboxFabActionId.MARK_ALL_AS_READ -> {
              binding.fab.visibility = View.VISIBLE
              binding.fab.setImageResource(R.drawable.baseline_done_all_24)
              binding.fab.setOnClickListener {
                viewModel.markAllAsRead()
              }
            }
            else -> {
              binding.fab.visibility = View.GONE
            }
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
          100,
        )
      }

      lastPageType = it
    }

    viewModel.inboxUpdate.observe(viewLifecycleOwner) {
      onUpdate()
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
        requireSummitActivity().onBackPressedDispatcher.addCallback(paneOnBackPressHandler)
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
                getMainActivity()?.setNavUiOpenPercent(1f, force = true)

                updatePaneBackPressHandler()
              }
              is PanelState.Closing -> {
                getMainActivity()?.setNavUiOpenPercent(1f - panelState.progress, force = true)
              }
              PanelState.Opened -> {
                getMainActivity()?.setNavUiOpenPercent(0f, force = true)
                updatePaneBackPressHandler()
              }
              is PanelState.Opening -> {
                getMainActivity()?.setNavUiOpenPercent(1f - panelState.progress, force = true)
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
      inboxPaneControllerFactory.create(
        viewModel = viewModel,
        binding = binding.startPanel,
        viewLifecycleOwner = viewLifecycleOwner,
        onCategoryClickListener = {
          when (it) {
            InboxPaneController.InboxCategory.Unread -> {
              viewModel.setPageType(PageType.Unread)
              paneLayout.closePanels()
            }
            InboxPaneController.InboxCategory.All -> {
              viewModel.setPageType(PageType.All)
              paneLayout.closePanels()
            }
            InboxPaneController.InboxCategory.Replies -> {
              viewModel.setPageType(PageType.Replies)
              paneLayout.closePanels()
            }
            InboxPaneController.InboxCategory.Mentions -> {
              viewModel.setPageType(PageType.Mentions)
              paneLayout.closePanels()
            }
            InboxPaneController.InboxCategory.Messages -> {
              viewModel.setPageType(PageType.Messages)
              paneLayout.closePanels()
            }
            InboxPaneController.InboxCategory.Reports -> {
              viewModel.setPageType(PageType.Reports)
              paneLayout.closePanels()
            }
            InboxPaneController.InboxCategory.Applications -> {
              viewModel.setPageType(PageType.Applications)
              paneLayout.closePanels()
            }
          }
        },
      )
    }

    viewModel.currentAccountView.observe(viewLifecycleOwner) {
      it.loadProfileImageOrDefault(binding.accountImageView)
    }
    viewModel.currentAccount.observe(viewLifecycleOwner) {
      adapter.accountId = it?.id
    }

    viewModel.markAsReadResult.observe(viewLifecycleOwner) {
      when (it) {
        is StatefulData.Error -> {
          ErrorDialogFragment.show(
            message = getString(R.string.error_unable_to_mark_message_as_read),
            error = it.error,
            fm = childFragmentManager,
          )
          viewModel.markAsReadResult.postIdle()
        }
        is StatefulData.Loading -> {}
        is StatefulData.NotStarted -> {}
        is StatefulData.Success -> {}
      }
    }
    viewModel.approveRegistrationApplicationResult.observe(viewLifecycleOwner) {
      when (it) {
        is StatefulData.Error ->
          ErrorDialogFragment.show(
            message = getString(R.string.error_unable_to_approve_decline_registration_application),
            error = it.error,
            fm = childFragmentManager,
          )
        is StatefulData.Loading -> {}
        is StatefulData.NotStarted -> {}
        is StatefulData.Success -> {}
      }
    }
    viewModel.fetchNewInboxItems.observe(viewLifecycleOwner) {
      when (it) {
        is StatefulData.Error -> {
          binding.newItemsProgressBar.visibility = View.GONE
        }
        is StatefulData.Loading -> {
          binding.newItemsProgressBar.visibility = View.VISIBLE
        }
        is StatefulData.NotStarted<*> -> {
          binding.newItemsProgressBar.visibility = View.GONE
        }
        is StatefulData.Success<*> -> {
          binding.newItemsProgressBar.visibility = View.GONE
        }
      }
    }

    binding.fab.setup(preferences)

    installOnActionResultHandler(
      context = context,
      moreActionsHelper = moreActionsHelper,
      snackbarContainer = binding.coordinatorLayout,
    )
  }

  private fun createAdapter(): InboxItemAdapter {
    return InboxItemAdapter(
      context = requireContext(),
      accountId = viewModel.currentAccount.value?.id,
      postAndCommentViewBuilder = postAndCommentViewBuilder,
      instance = viewModel.instance,
      lifecycleOwner = viewLifecycleOwner,
      avatarHelper = avatarHelper,
      lemmyTextHelper = lemmyTextHelper,
      preferences = preferences,
      onImageClick = { url ->
        getMainActivity()?.openImage(
          sharedElement = null,
          appBar = binding.appBar,
          title = null,
          url = url,
          mimeType = null,
        )
      },
      onVideoClick = { url, videoType, state ->
        getMainActivity()?.openVideo(url, videoType, state)
      },
      onMarkAsRead = { view, inboxItem, read ->
        viewModel.markAsRead(inboxItem, read)

        if (preferences.hapticsOnActions) {
          view.performHapticFeedbackCompat(HapticFeedbackConstantsCompat.CONFIRM)
        }
      },
      onPageClick = { url, pageRef ->
        getMainActivity()?.launchPage(pageRef, url = url)
      },
      onMessageClick = {
        val accountId = viewModel.currentAccount.value?.id ?: return@InboxItemAdapter
        if (!it.isRead && it !is ReportItem && preferences.inboxAutoMarkAsRead) {
          viewModel.markAsRead(it, read = true)
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
            addItem(R.id.none, R.string.no_options)
          },
        )
      },
      onSignInRequired = {
        PreAuthDialogFragment.newInstance()
          .show(childFragmentManager, "asdf")
      },
      onInstanceMismatch = { accountInstance, apiInstance ->
        launchAlertDialog("error_instance_mismatch") {
          titleResId = R.string.error_account_instance_mismatch_title
          message = getString(
            R.string.error_account_instance_mismatch,
            accountInstance,
            apiInstance,
          )
        }
      },
      onLinkClick = { url, text, linkType ->
        onLinkClick(url, text, linkType)
      },
      onLinkLongClick = { url, text ->
        getMainActivity()?.showMoreLinkOptions(url, text)
      },
      onApproveClick = { id ->
        viewModel.approveRegistrationApplication(
          applicationId = id,
          approve = true,
          denyReason = null,
        )
      },
      onDeclineClick = { id ->
        viewModel.lastApplicationId.value = id
        ReasonDialogFragment.show(
          childFragmentManager,
          getString(R.string.decline_reason),
          getString(R.string.decline),
        )
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

    if (System.currentTimeMillis() - viewModel.lastFetchRequestTs >= 5_000) {
      Log.d(TAG, "Screen resumed! Checking if there are new items!")
      viewModel.fetchNewInboxItems()
    } else {
      Log.d(TAG, "Screen was shown too recently. Waiting a while before checking for new items!")
    }
  }

  fun openDefaultPage() {
    viewModel.setPageType(PageType.Unread)
  }

  private class InboxItemAdapter(
    private val context: Context,
    var accountId: Long?,
    private val postAndCommentViewBuilder: PostAndCommentViewBuilder,
    private val instance: String,
    private val lifecycleOwner: LifecycleOwner,
    private val avatarHelper: AvatarHelper,
    private val lemmyTextHelper: LemmyTextHelper,
    private val preferences: Preferences,
    private val onImageClick: (String) -> Unit,
    private val onVideoClick: (
      url: String,
      videoType: VideoType,
      videoState: VideoState?,
    ) -> Unit,
    private val onMarkAsRead: (View, InboxItem, Boolean) -> Unit,
    private val onPageClick: (url: String, PageRef) -> Unit,
    private val onMessageClick: (InboxItem) -> Unit,
    private val onConversationClick: (Conversation) -> Unit,
    private val onAddCommentClick: (View, InboxItem) -> Unit,
    private val onOverflowMenuClick: (InboxItem) -> Unit,
    private val onSignInRequired: () -> Unit,
    private val onInstanceMismatch: (String, String) -> Unit,
    private val onLinkClick: (url: String, text: String, linkContext: LinkContext) -> Unit,
    private val onLinkLongClick: (String, String) -> Unit,
    private val onApproveClick: (applicationId: Int) -> Unit,
    private val onDeclineClick: (applicationId: Int) -> Unit,
  ) : Adapter<ViewHolder>() {

    private sealed interface Item {

      data object HeaderItem : Item

      data class TooManyMessagesWarningItem(
        val earliestMessageTs: Long,
      ) : Item

      sealed interface InboxListItem : Item {
        val inboxItem: InboxItem
      }
      data class CompactInboxListItem(
        override val inboxItem: InboxItem,
      ) : InboxListItem
      data class FullInboxListItem(
        override val inboxItem: InboxItem,
      ) : InboxListItem

      data class RegistrationApplicationItem(
        val registrationApplication: RegistrationApplicationInboxItem,
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
        old::class == new::class &&
          when (old) {
            is Item.HeaderItem -> true
            is Item.RegistrationApplicationItem ->
              old.registrationApplication.id ==
                (new as Item.RegistrationApplicationItem).registrationApplication.id
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
      addItemType(Item.CompactInboxListItem::class, InboxListItemBinding::inflate) { item, b, _ ->
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
      addItemType(Item.FullInboxListItem::class, InboxListItemBinding::inflate) { item, b, _ ->
        b.content.maxLines = Integer.MAX_VALUE
        b.content.isSingleLine = false

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
        clazz = Item.RegistrationApplicationItem::class,
        inflateFn = InboxListRegistrationApplicationItemBinding::inflate,
      ) { item, b, _ ->
        postAndCommentViewBuilder.bindRegistrationApplication(
          b = b,
          instance = instance,
          item = item.registrationApplication,
          onImageClick = onImageClick,
          onVideoClick = onVideoClick,
          onPageClick = onPageClick,
          onLinkClick = onLinkClick,
          onLinkLongClick = onLinkLongClick,
          onApproveClick = onApproveClick,
          onDeclineClick = onDeclineClick,
        )
      }
      addItemType(
        clazz = Item.ConversationItem::class,
        inflateFn = ItemConversationBinding::inflate,
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

        lemmyTextHelper.bindText(
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

        b.ts.text = tsToConcise(
          context = context,
          ts = conversation.ts,
          style = PrettyPrintStyles.SHORT_DYNAMIC,
        )
        b.root.setOnClickListener {
          onConversationClick(conversation)
        }
        b.root.setTag(R.id.swipe_enabled, false)
      }
      addItemType(
        clazz = Item.LoaderItem::class,
        inflateFn = InboxListLoaderItemBinding::inflate,
      ) { item, b, _ ->
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
        is Item.RegistrationApplicationItem -> item.registrationApplication
      }

    fun setData(inboxModel: InboxModel, cb: (() -> Unit)? = null) {
      Log.d(TAG, "Data set! hasMore: ${inboxModel.hasMore}")
      this.inboxModel = inboxModel
      refreshItems(cb)
    }

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
          is InboxListItem.RegistrationApplicationInboxItem -> {
            newItems.add(
              Item.RegistrationApplicationItem(
                item.item,
              ),
            )
          }
          is InboxListItem.RegularInboxItem -> {
            when (preferences.inboxLayout) {
              InboxLayoutId.COMPACT ->
                newItems.add(Item.CompactInboxListItem(item.item))
              InboxLayoutId.FULL ->
                newItems.add(Item.FullInboxListItem(item.item))
            }
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
  }
}
