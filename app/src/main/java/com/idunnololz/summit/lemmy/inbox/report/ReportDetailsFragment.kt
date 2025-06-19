package com.idunnololz.summit.lemmy.inbox.report

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.doOnLayout
import androidx.core.view.updatePadding
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
import com.idunnololz.summit.databinding.FragmentReportDetailsBinding
import com.idunnololz.summit.lemmy.CommentRef
import com.idunnololz.summit.lemmy.LemmyTextHelper
import com.idunnololz.summit.lemmy.PersonRef
import com.idunnololz.summit.lemmy.PostRef
import com.idunnololz.summit.lemmy.comment.AddOrEditCommentFragment
import com.idunnololz.summit.lemmy.inbox.InboxItem
import com.idunnololz.summit.lemmy.inbox.InboxTabbedFragment
import com.idunnololz.summit.lemmy.inbox.ReportItem
import com.idunnololz.summit.lemmy.inbox.message.ContextFetcher
import com.idunnololz.summit.lemmy.post.OldThreadLinesDecoration
import com.idunnololz.summit.lemmy.post.PostAdapter
import com.idunnololz.summit.lemmy.post.PostViewModel
import com.idunnololz.summit.lemmy.postAndCommentView.PostAndCommentViewBuilder
import com.idunnololz.summit.lemmy.postAndCommentView.createCommentActionHandler
import com.idunnololz.summit.lemmy.postListView.showMorePostOptions
import com.idunnololz.summit.lemmy.utils.actions.MoreActionsHelper
import com.idunnololz.summit.lemmy.utils.showMoreVideoOptions
import com.idunnololz.summit.links.onLinkClick
import com.idunnololz.summit.preferences.Preferences
import com.idunnololz.summit.util.AnimationsHelper
import com.idunnololz.summit.util.BaseFragment
import com.idunnololz.summit.util.StatefulData
import com.idunnololz.summit.util.ext.getColorCompat
import com.idunnololz.summit.util.ext.getColorFromAttribute
import com.idunnololz.summit.util.ext.setup
import com.idunnololz.summit.util.insetViewExceptBottomAutomaticallyByMargins
import com.idunnololz.summit.util.insetViewExceptTopAutomaticallyByPadding
import com.idunnololz.summit.util.setupToolbar
import com.idunnololz.summit.util.showMoreLinkOptions
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.max

@AndroidEntryPoint
class ReportDetailsFragment : BaseFragment<FragmentReportDetailsBinding>() {

  private val args: ReportDetailsFragmentArgs by navArgs()
  private val viewModel: ReportDetailsViewModel by viewModels()

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

    setBinding(FragmentReportDetailsBinding.inflate(inflater, container, false))

    return binding.root
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)

    val inboxItem = args.reportItem
    val reportItem = args.reportItem as ReportItem

    with(binding) {
      requireSummitActivity().apply {
        insetViewExceptBottomAutomaticallyByMargins(viewLifecycleOwner, toolbar)
        insetViewExceptTopAutomaticallyByPadding(viewLifecycleOwner, mainContainer)
        insetViewExceptTopAutomaticallyByPadding(viewLifecycleOwner, fabContainer)
      }

      val title = when (reportItem) {
        is InboxItem.ReportCommentInboxItem -> {
          getString(R.string.report_on_comment)
        }
        is InboxItem.ReportMessageInboxItem -> {
          getString(R.string.report_on_private_message)
        }
        is InboxItem.ReportPostInboxItem -> {
          getString(R.string.report_on_post)
        }
      }

      setupToolbar(toolbar, title)
      toolbar.setNavigationOnClickListener {
        (parentFragment as? InboxTabbedFragment)?.closeMessage()
      }

      author.text = getString(R.string.from_format, inboxItem.authorName)
      author.setOnClickListener {
        getMainActivity()?.launchPage(
          PersonRef.PersonRefByName(
            name = inboxItem.authorName,
            instance = inboxItem.authorInstance,
          ),
        )
      }

      content.text = inboxItem.content

      fun goToPost() {
        when (reportItem) {
          is InboxItem.ReportCommentInboxItem -> {
            getMainActivity()?.launchPage(
              CommentRef(viewModel.apiInstance, reportItem.commentId),
            )
          }
          is InboxItem.ReportMessageInboxItem -> TODO()
          is InboxItem.ReportPostInboxItem -> {
            getMainActivity()?.launchPage(
              PostRef(viewModel.apiInstance, reportItem.reportedPostId),
            )
          }
        }
      }

      binding.goToPost.setOnClickListener {
        goToPost()
      }
      binding.openContextButton.setOnClickListener {
        goToPost()
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

      loadContext(reportItem)
    }
  }


  private fun loadContext(reportItem: ReportItem, force: Boolean = false) {
    when (val inboxItem = reportItem) {
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
          instance = viewModel.apiInstance,
          accountId = null,
          revealAll = false,
          useFooter = false,
          isEmbedded = true,
          videoState = null,
          autoCollapseCommentThreshold = -1f,
          lemmyTextHelper = lemmyTextHelper,
          onRefreshClickCb = {
            loadContext(args.reportItem, force = true)
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
              instance = viewModel.apiInstance,
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
              instance = viewModel.apiInstance,
              accountId = null,
              postView = postView,
              moreActionsHelper = moreActionsHelper,
              fragmentManager = childFragmentManager,
            )
          },
          onCommentActionClick = { commentView, _, actionId ->
            createCommentActionHandler(
              apiInstance = viewModel.apiInstance,
              commentView = commentView,
              moreActionsHelper = moreActionsHelper,
              fragmentManager = childFragmentManager,
            )(actionId)
          },
          onFetchComments = {
            val postId = when (val inboxItem = args.reportItem) {
              is InboxItem.ReportMessageInboxItem -> TODO()
              is InboxItem.ReportCommentInboxItem -> inboxItem.postId
              is InboxItem.ReportPostInboxItem -> inboxItem.reportedPostId
            }

            getMainActivity()?.launchPage(
              PostRef(viewModel.apiInstance, postId),
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
          postView = PostViewModel.ListView.PostListView(data.post),
          commentTree = listOfNotNull(data.commentTree),
          newlyPostedCommentId = null,
          selectedCommentId = null,
          isSingleComment = false,
          isNativePost = true,
          accountInstance = viewModel.apiInstance,
          isCommentsLoaded = true,
          commentPath = null,
          crossPosts = listOf(),
        ),
      )

      val reportItem = args.reportItem

      adapter.highlightTintColor = context.getColorCompat(R.color.style_red)
      when (reportItem) {
        is InboxItem.ReportCommentInboxItem -> {
          val commentId = reportItem.commentId
          if (commentId != null) {
            adapter.highlightCommentForever(commentId)
          }
        }
        is InboxItem.ReportMessageInboxItem -> {

        }
        is InboxItem.ReportPostInboxItem -> {
          adapter.highlightPostForever = true
        }
      }
    }
  }
}