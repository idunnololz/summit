package com.idunnololz.summit.lemmy.inbox.message

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.HapticFeedbackConstantsCompat
import androidx.core.view.doOnLayout
import androidx.core.view.updatePadding
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.idunnololz.summit.R
import com.idunnololz.summit.account.AccountActionsManager
import com.idunnololz.summit.account.AccountManager
import com.idunnololz.summit.accountUi.PreAuthDialogFragment
import com.idunnololz.summit.alert.OldAlertDialogFragment
import com.idunnololz.summit.alert.OldAlertDialogFragment.*
import com.idunnololz.summit.databinding.FragmentMessageBinding
import com.idunnololz.summit.lemmy.CommentRef
import com.idunnololz.summit.lemmy.LemmyTextHelper
import com.idunnololz.summit.lemmy.PersonRef
import com.idunnololz.summit.lemmy.PostRef
import com.idunnololz.summit.lemmy.comment.AddOrEditCommentFragment
import com.idunnololz.summit.lemmy.comment.AddOrEditCommentFragmentArgs
import com.idunnololz.summit.lemmy.inbox.CommentBackedItem
import com.idunnololz.summit.lemmy.inbox.InboxItem
import com.idunnololz.summit.lemmy.inbox.InboxTabbedFragment
import com.idunnololz.summit.lemmy.inbox.ReportItem
import com.idunnololz.summit.lemmy.inbox.inbox.InboxViewModel
import com.idunnololz.summit.lemmy.post.OldThreadLinesDecoration
import com.idunnololz.summit.lemmy.post.PostAdapter
import com.idunnololz.summit.lemmy.post.PostViewModel
import com.idunnololz.summit.lemmy.postAndCommentView.PostAndCommentViewBuilder
import com.idunnololz.summit.lemmy.postAndCommentView.createCommentActionHandler
import com.idunnololz.summit.lemmy.postListView.showMorePostOptions
import com.idunnololz.summit.lemmy.toPostHeaderInfo
import com.idunnololz.summit.lemmy.utils.VotableRef
import com.idunnololz.summit.lemmy.utils.actions.MoreActionsHelper
import com.idunnololz.summit.lemmy.utils.actions.installOnActionResultHandler
import com.idunnololz.summit.lemmy.utils.bind
import com.idunnololz.summit.lemmy.utils.showMoreVideoOptions
import com.idunnololz.summit.links.onLinkClick
import com.idunnololz.summit.preferences.Preferences
import com.idunnololz.summit.preview.VideoType
import com.idunnololz.summit.util.AnimationsHelper
import com.idunnololz.summit.util.BaseFragment
import com.idunnololz.summit.util.StatefulData
import com.idunnololz.summit.util.ext.getColorFromAttribute
import com.idunnololz.summit.util.ext.performHapticFeedbackCompat
import com.idunnololz.summit.util.ext.setup
import com.idunnololz.summit.util.insetViewExceptBottomAutomaticallyByMargins
import com.idunnololz.summit.util.insetViewExceptTopAutomaticallyByPadding
import com.idunnololz.summit.util.showMoreLinkOptions
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlin.math.max
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MessageFragment : BaseFragment<FragmentMessageBinding>() {

  companion object {
    private const val TAG = "MessageFragment"
  }

  private val args by navArgs<MessageFragmentArgs>()

  private val viewModel: MessageViewModel by viewModels()
  private val inboxViewModel: InboxViewModel by activityViewModels()

  @Inject
  lateinit var moreActionsHelper: MoreActionsHelper

  @Inject
  lateinit var postAndCommentViewBuilder: PostAndCommentViewBuilder

  @Inject
  lateinit var accountActionsManager: AccountActionsManager

  @Inject
  lateinit var accountManager: AccountManager

  @Inject
  lateinit var preferences: Preferences

  @Inject
  lateinit var animationsHelper: AnimationsHelper

  @Inject
  lateinit var lemmyTextHelper: LemmyTextHelper

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?,
  ): View {
    super.onCreateView(inflater, container, savedInstanceState)

    setBinding(FragmentMessageBinding.inflate(inflater, container, false))

    return binding.root
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)

    val context = requireContext()

    requireSummitActivity().apply {
      insetViewExceptBottomAutomaticallyByMargins(viewLifecycleOwner, binding.toolbar)
      insetViewExceptTopAutomaticallyByPadding(viewLifecycleOwner, binding.mainContainer)
      insetViewExceptTopAutomaticallyByPadding(viewLifecycleOwner, binding.bottomAppBar)

      insets.observe(viewLifecycleOwner) { insets ->
        binding.dummyAppBar.updatePadding(bottom = insets.bottomInset * 2)
      }
    }

    binding.toolbar.setNavigationIcon(
      R.drawable.baseline_arrow_back_24,
    )
    binding.toolbar.setNavigationIconTint(
      context.getColorFromAttribute(androidx.appcompat.R.attr.colorControlNormal),
    )

    binding.toolbar.setNavigationOnClickListener {
      (parentFragment as? InboxTabbedFragment)?.closeMessage()
    }

    val inboxItem = args.inboxItem

    when (inboxItem) {
      is InboxItem.MentionInboxItem,
      is InboxItem.MessageInboxItem,
      is InboxItem.ReplyInboxItem,
      -> {
        lemmyTextHelper.bindText(
          binding.title,
          inboxItem.title,
          args.instance,
          onImageClick = { url ->
            getMainActivity()?.openImage(null, binding.appBar, null, url, null)
          },
          onVideoClick = { url ->
            getMainActivity()?.openVideo(url, VideoType.Unknown, null)
          },
          onPageClick = {
            getMainActivity()?.launchPage(it)
          },
          onLinkClick = { url, text, linkType ->
            onLinkClick(url, text, linkType)
          },
          onLinkLongClick = { url, text ->
            getMainActivity()?.showMoreLinkOptions(url, text)
          },
        )
      }
      is InboxItem.ReportCommentInboxItem -> {
        binding.title.setText(R.string.report_on_comment)
      }
      is InboxItem.ReportMessageInboxItem -> {
        binding.title.setText(R.string.report)
      }
      is InboxItem.ReportPostInboxItem -> {
        binding.title.setText(R.string.report_on_post)
      }
      is InboxItem.RegistrationApplicationInboxItem -> {
        binding.title.setText(R.string.registration_application)
      }
    }
    lemmyTextHelper.bindText(
      binding.content,
      inboxItem.content,
      args.instance,
      onImageClick = { url ->
        getMainActivity()?.openImage(null, binding.appBar, null, url, null)
      },
      onVideoClick = { url ->
        getMainActivity()?.openVideo(url, VideoType.Unknown, null)
      },
      onPageClick = {
        getMainActivity()?.launchPage(it)
      },
      onLinkClick = { url, text, linkType ->
        onLinkClick(url, text, linkType)
      },
      onLinkLongClick = { url, text ->
        getMainActivity()?.showMoreLinkOptions(url, text)
      },
    )

    binding.author.text = getString(R.string.from_format, args.inboxItem.authorName)
    binding.author.setOnClickListener {
      getMainActivity()?.launchPage(
        PersonRef.PersonRefByName(
          name = args.inboxItem.authorName,
          instance = args.inboxItem.authorInstance,
        ),
      )
    }

    viewModel.commentContext.observe(viewLifecycleOwner) {
      when (it) {
        is StatefulData.Error -> {
          binding.contextLoadingView.showDefaultErrorMessageFor(it.error)
        }
        is StatefulData.Loading -> {
          binding.contextLoadingView.showProgressBar()
        }
        is StatefulData.NotStarted -> {}
        is StatefulData.Success -> {
          binding.contextLoadingView.hideAll()
          loadRecyclerView(it.data)
        }
        null -> {}
      }
    }

    binding.bottomAppBar.setOnMenuItemClickListener {
      when (it.itemId) {
        R.id.mark_as_unread -> {
          inboxViewModel.markAsRead(args.inboxItem, read = false)
          (parentFragment as? InboxTabbedFragment)?.closeMessage()

          if (preferences.hapticsOnActions) {
            binding.bottomAppBar.performHapticFeedbackCompat(
              HapticFeedbackConstantsCompat.CONFIRM,
            )
          }

          true
        }
        R.id.upvote -> {
          val commentId = (args.inboxItem as CommentBackedItem).commentId
          accountActionsManager.vote(args.instance, VotableRef.CommentRef(commentId), 1)

          if (preferences.hapticsOnActions) {
            binding.bottomAppBar.performHapticFeedbackCompat(
              HapticFeedbackConstantsCompat.CONFIRM,
            )
          }

          true
        }
        R.id.downvote -> {
          val commentId = (args.inboxItem as CommentBackedItem).commentId
          accountActionsManager.vote(args.instance, VotableRef.CommentRef(commentId), -1)

          if (preferences.hapticsOnActions) {
            binding.bottomAppBar.performHapticFeedbackCompat(
              HapticFeedbackConstantsCompat.CONFIRM,
            )
          }

          true
        }
        else -> {
          false
        }
      }
    }

    binding.bottomAppBar.menu.apply {
      if (inboxItem is ReportItem) {
        findItem(R.id.upvote).isVisible = false
        findItem(R.id.downvote).isVisible = false
      }
    }

    if (inboxItem is ReportItem) {
      binding.fab.setImageResource(R.drawable.baseline_check_24)
      binding.fab.setOnClickListener {
        inboxViewModel.markAsRead(inboxItem, true)
        (parentFragment as? InboxTabbedFragment)?.closeMessage()

        if (preferences.hapticsOnActions) {
          binding.bottomAppBar.performHapticFeedbackCompat(
            HapticFeedbackConstantsCompat.CONFIRM,
          )
        }
      }
    } else {
      binding.fab.setImageResource(R.drawable.baseline_reply_24)
      binding.fab.setOnClickListener {
        if (accountManager.currentAccount.value == null) {
          PreAuthDialogFragment.newInstance(R.id.action_add_comment)
            .show(childFragmentManager, "asdf")
          return@setOnClickListener
        }

        AddOrEditCommentFragment().apply {
          arguments =
            AddOrEditCommentFragmentArgs(
              instance = args.instance,
              commentView = null,
              postView = null,
              editCommentView = null,
              inboxItem = inboxItem,
            ).toBundle()
        }.show(childFragmentManager, "asdf")

        if (preferences.hapticsOnActions) {
          binding.bottomAppBar.performHapticFeedbackCompat(
            HapticFeedbackConstantsCompat.CONFIRM,
          )
        }
      }
    }

    val upvoteColor = preferences.upvoteColor
    val downvoteColor = preferences.downvoteColor
    val normalTextColor = ContextCompat.getColor(context, R.color.colorText)
    when (inboxItem) {
      is CommentBackedItem -> {
        postAndCommentViewBuilder.voteUiHandler.bind(
          lifecycleOwner = viewLifecycleOwner,
          instance = args.instance,
          inboxItem = inboxItem,
          upVoteView = null,
          downVoteView = null,
          scoreView = binding.score,
          upvoteCount = null,
          downvoteCount = null,
          accountId = null,
          onUpdate = { vote, _, _, _ ->
            if (vote > 0) {
              binding.score.setTextColor(upvoteColor)
            } else if (vote == 0) {
              binding.score.setTextColor(normalTextColor)
            } else {
              binding.score.setTextColor(downvoteColor)
            }
          },
          onSignInRequired = {
            PreAuthDialogFragment.newInstance()
              .show(childFragmentManager, "asdf")
          },
          onInstanceMismatch = { accountInstance, apiInstance ->
            Builder()
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
        )

        fun goToPost() {
          getMainActivity()?.launchPage(
            CommentRef(args.instance, inboxItem.commentId),
          )
        }

        binding.goToPost.setOnClickListener {
          goToPost()
        }
        binding.openContextButton.setOnClickListener {
          goToPost()
        }
      }
      is InboxItem.MessageInboxItem -> {
        binding.score.visibility = View.GONE
        binding.bottomAppBar.menu.findItem(R.id.upvote).isEnabled = false
        binding.bottomAppBar.menu.findItem(R.id.downvote).isEnabled = false
      }

      is InboxItem.ReportCommentInboxItem,
      is InboxItem.ReportPostInboxItem,
      is InboxItem.ReportMessageInboxItem,
      -> {
        binding.score.visibility = View.GONE
        binding.bottomAppBar.menu.findItem(R.id.upvote).isEnabled = false
        binding.bottomAppBar.menu.findItem(R.id.downvote).isEnabled = false
      }
      is InboxItem.RegistrationApplicationInboxItem -> {
        binding.score.visibility = View.GONE
        binding.bottomAppBar.menu.findItem(R.id.upvote).isEnabled = false
        binding.bottomAppBar.menu.findItem(R.id.downvote).isEnabled = false
      }

      is InboxItem.MentionInboxItem,
      is InboxItem.ReplyInboxItem,
      -> error("THIS SHOULDNT HAPPEN!!!")
    }

    binding.contextCard.setOnClickListener {
      if (viewModel.isContextShowing) {
        viewModel.isContextShowing = false
      } else {
        viewModel.isContextShowing = true
        loadContext()
      }
      updateContextState()
    }

    installOnActionResultHandler(
      context = context,
      moreActionsHelper = moreActionsHelper,
      snackbarContainer = binding.root,
    )

    if (inboxItem is ReportItem) {
      viewModel.isContextShowing = true
      loadContext()
    }

    updateContextState()
  }

  override fun onResume() {
    super.onResume()

    if (preferences.leftHandMode) {
      binding.bottomAppBar.layoutDirection = View.LAYOUT_DIRECTION_RTL
      binding.fabContainer.layoutDirection = View.LAYOUT_DIRECTION_RTL
    } else {
      binding.bottomAppBar.layoutDirection = View.LAYOUT_DIRECTION_INHERIT
    }
  }

  private fun updateContextState() {
    with(binding) {
      if (args.inboxItem !is CommentBackedItem &&
        args.inboxItem !is InboxItem.ReportPostInboxItem &&
        args.inboxItem !is InboxItem.ReportCommentInboxItem
      ) {
        contextCard.visibility = View.GONE
      } else if (viewModel.isContextShowing) {
        indicator.setImageResource(R.drawable.baseline_expand_less_18)
        contextContent.visibility = View.VISIBLE
      } else {
        indicator.setImageResource(R.drawable.baseline_expand_more_18)
        contextContent.visibility = View.GONE
      }
    }
  }

  private fun loadContext(force: Boolean = false) {
    when (val inboxItem = args.inboxItem) {
      is InboxItem.MentionInboxItem -> {
        Log.d(TAG, "Context: " + inboxItem.commentPath)
        viewModel.fetchCommentContext(inboxItem.postId, inboxItem.commentPath, force)
      }
      is InboxItem.MessageInboxItem -> {}
      is InboxItem.ReplyInboxItem -> {
        Log.d(TAG, "Context: " + inboxItem.commentPath)
        viewModel.fetchCommentContext(inboxItem.postId, inboxItem.commentPath, force)
      }
      is InboxItem.ReportMessageInboxItem -> {
        TODO()
      }
      is InboxItem.ReportCommentInboxItem -> {
        viewModel.fetchCommentContext(
          inboxItem.postId,
          inboxItem.reportedCommentPath,
          force,
        )
      }
      is InboxItem.ReportPostInboxItem -> {
        viewModel.fetchCommentContext(inboxItem.reportedPostId, null, force)
      }
      is InboxItem.RegistrationApplicationInboxItem -> TODO()
    }
  }

  private fun loadRecyclerView(data: ContextFetcher.CommentContext) {
    if (!isBindingAvailable()) return

    val context = requireContext()

    with(binding) {
      var adapter = recyclerView.adapter as? PostAdapter

      if (adapter == null) {
        adapter = PostAdapter(
          postAndCommentViewBuilder = postAndCommentViewBuilder,
          context = context,
          lifecycleOwner = viewLifecycleOwner,
          instance = args.instance,
          accountId = null,
          revealAll = false,
          useFooter = false,
          isEmbedded = true,
          videoState = null,
          autoCollapseCommentThreshold = preferences.autoCollapseCommentThreshold,
          lemmyTextHelper = lemmyTextHelper,
          onRefreshClickCb = {
            loadContext(force = true)
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
          onAddCommentClick = { postOrComment ->
            if (viewModel.accountManager.currentAccount.value == null) {
              PreAuthDialogFragment.newInstance(R.id.action_add_comment)
                .show(childFragmentManager, "asdf")
              return@PostAdapter
            }

            AddOrEditCommentFragment.showReplyDialog(
              instance = args.instance,
              postOrCommentView = postOrComment,
              fragmentManager = childFragmentManager,
              accountId = null,
            )
          },
          onImageClick = { _, view, url ->
            getMainActivity()?.openImage(view, binding.appBar, null, url, null)
          },
          onVideoClick = { url, videoType, state ->
            getMainActivity()?.openVideo(url, videoType, state)
          },
          onVideoLongClickListener = { url ->
            showMoreVideoOptions(
              url = url,
              originalUrl = url,
              moreActionsHelper = moreActionsHelper,
              fragmentManager = childFragmentManager,
            )
          },
          onPageClick = {
            getMainActivity()?.launchPage(it)
          },
          onPostActionClick = { postView, _, actionId ->
            showMorePostOptions(
              accountId = null,
              postView = postView,
              moreActionsHelper = moreActionsHelper,
              fragmentManager = childFragmentManager,
            )
          },
          onCommentActionClick = { commentView, _, actionId ->
            createCommentActionHandler(
              commentView = commentView,
              moreActionsHelper = moreActionsHelper,
              fragmentManager = childFragmentManager,
            )(actionId)
          },
          onFetchComments = {
            val postId = when (val inboxItem = args.inboxItem) {
              is InboxItem.MentionInboxItem -> inboxItem.postId
              is InboxItem.MessageInboxItem -> return@PostAdapter
              is InboxItem.ReplyInboxItem -> inboxItem.postId
              is InboxItem.ReportMessageInboxItem -> TODO()
              is InboxItem.ReportCommentInboxItem -> inboxItem.postId
              is InboxItem.ReportPostInboxItem -> inboxItem.reportedPostId
              is InboxItem.RegistrationApplicationInboxItem -> TODO()
            }

            getMainActivity()?.launchPage(
              PostRef(args.instance, postId),
            )
          },
          onLoadPost = { _, _ -> },
          onLoadCommentPath = {},
          onLinkClick = { url, text, linkType ->
            onLinkClick(url, text, linkType)
          },
          onLinkLongClick = { url, text ->
            getMainActivity()?.showMoreLinkOptions(url, text)
          },
          switchToNativeInstance = {},
          onCrossPostsClick = {},
        ).apply {
          stateRestorationPolicy = RecyclerView.Adapter.StateRestorationPolicy.PREVENT_WHEN_EMPTY
        }
        recyclerView.adapter = adapter
        recyclerView.doOnLayout {
          recyclerView.post {
            adapter.setContentMaxSize(
              recyclerView.measuredWidth,
              recyclerView.measuredHeight,
            )
          }
        }
        recyclerView.setup(animationsHelper)
        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.addItemDecoration(
          OldThreadLinesDecoration(
            context,
            postAndCommentViewBuilder.hideCommentActions,
          ),
        )
      }

      adapter.setData(
        PostViewModel.PostData(
          postListView = PostViewModel.ListView.PostListView(
            post = data.post,
            postHeaderInfo = data.post.toPostHeaderInfo(context),
          ),
          commentTree = listOfNotNull(data.commentTree),
          newlyPostedCommentId = null,
          selectedCommentId = null,
          isSingleComment = false,
          isNativePost = true,
          accountInstance = viewModel.apiInstance,
          isCommentsLoaded = true,
          commentPath = null,
          crossPosts = listOf(),
          wasUpdateForced = false,
        ),
      )

      val commentId = args.inboxItem.commentId
      if (commentId != null) {
        adapter.highlightCommentForever(commentId)

        viewLifecycleOwner.lifecycleScope.launch {
          var lastScrollY = 0
          var startMonitorTs = 0L
          val mainActivity = requireSummitActivity()
          while (true) {
            delay(100)

            val position = adapter.getPositionOfComment(commentId)
            val vh = recyclerView.findViewHolderForAdapterPosition(position)
            val itemView = vh?.itemView
              ?: continue

            if (itemView.height == 0) {
              continue
            }

            if (startMonitorTs == 0L) {
              startMonitorTs = System.currentTimeMillis()
              binding.appBar.setExpanded(false)
            }

            val top = getTopRelativeToView(itemView, binding.mainContainer)
            val viewportHeight = mainContainer.height -
              mainContainer.paddingTop -
              mainContainer.paddingBottom
            val scrollY = top -
              max(
                (viewportHeight - itemView.height) / 2,
                mainActivity.insets.value?.topInset ?: 0,
              )

            if (scrollY != lastScrollY) {
              // In cases where there are images in the post/comment list, the layout
              // can be unstable. Monitor view positions for about 1s before we
              // give up.
              binding.mainContainer.smoothScrollTo(0, scrollY)
              lastScrollY = scrollY
            }

            if (System.currentTimeMillis() - startMonitorTs > 1_500) {
              binding.root.invalidate()
              break
            }
          }
        }
      }
    }
  }

  private fun getTopRelativeToView(childView: View, parentView: View): Int {
    var top = 0
    var curView = childView

    while (curView != parentView) {
      val parent = curView.parent as? View
        ?: return 0

      top += curView.top
      curView = parent
    }

    return top
  }
}
