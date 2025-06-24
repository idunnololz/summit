package com.idunnololz.summit.lemmy.post

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.BackEventCompat
import androidx.activity.OnBackPressedCallback
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.view.doOnPreDraw
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePadding
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearSmoothScroller
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.SmoothScroller
import arrow.core.Either
import com.google.android.material.snackbar.Snackbar
import com.idunnololz.summit.R
import com.idunnololz.summit.account.Account
import com.idunnololz.summit.account.AccountManager
import com.idunnololz.summit.account.loadProfileImageOrDefault
import com.idunnololz.summit.accountUi.AccountsAndSettingsDialogFragment
import com.idunnololz.summit.accountUi.PreAuthDialogFragment
import com.idunnololz.summit.accountUi.SignInNavigator
import com.idunnololz.summit.alert.OldAlertDialogFragment
import com.idunnololz.summit.alert.launchAlertDialog
import com.idunnololz.summit.api.AccountInstanceMismatchException
import com.idunnololz.summit.api.dto.CommentView
import com.idunnololz.summit.api.dto.PostView
import com.idunnololz.summit.api.utils.getUrl
import com.idunnololz.summit.databinding.FragmentPostBinding
import com.idunnololz.summit.databinding.ScreenshotModeAppBarBinding
import com.idunnololz.summit.history.HistoryFragment
import com.idunnololz.summit.history.HistoryManager
import com.idunnololz.summit.history.HistorySaveReason
import com.idunnololz.summit.lemmy.CommentRef
import com.idunnololz.summit.lemmy.CommentsSortOrder
import com.idunnololz.summit.lemmy.LemmyTextHelper
import com.idunnololz.summit.lemmy.PostRef
import com.idunnololz.summit.lemmy.actions.LemmySwipeActionCallback
import com.idunnololz.summit.lemmy.comment.AddOrEditCommentFragment
import com.idunnololz.summit.lemmy.community.CommunityFragment
import com.idunnololz.summit.lemmy.createOrEditPost.CreateOrEditPostFragment
import com.idunnololz.summit.lemmy.fastAccountSwitcher.FastAccountSwitcherDialogFragment
import com.idunnololz.summit.lemmy.getLocalizedName
import com.idunnololz.summit.lemmy.idToCommentsSortOrder
import com.idunnololz.summit.lemmy.person.PersonTabbedFragment
import com.idunnololz.summit.lemmy.post.PostViewModel.Companion.HIGHLIGHT_COMMENT_MS
import com.idunnololz.summit.lemmy.postAndCommentView.PostAndCommentViewBuilder
import com.idunnololz.summit.lemmy.postAndCommentView.createCommentActionHandler
import com.idunnololz.summit.lemmy.postAndCommentView.setupForPostAndComments
import com.idunnololz.summit.lemmy.postListView.createPostActionHandler
import com.idunnololz.summit.lemmy.postListView.showMorePostOptions
import com.idunnololz.summit.lemmy.screenshotMode.ScreenshotModeDialogFragment
import com.idunnololz.summit.lemmy.search.SearchTabbedFragment
import com.idunnololz.summit.lemmy.toCommunityRef
import com.idunnololz.summit.lemmy.userTags.UserTagsManager
import com.idunnololz.summit.lemmy.utils.actions.MoreActionsHelper
import com.idunnololz.summit.lemmy.utils.actions.MoreActionsHelperManager
import com.idunnololz.summit.lemmy.utils.actions.installOnActionResultHandler
import com.idunnololz.summit.lemmy.utils.getCommentSwipeActions
import com.idunnololz.summit.lemmy.utils.getPostSwipeActions
import com.idunnololz.summit.lemmy.utils.setup
import com.idunnololz.summit.lemmy.utils.showMoreVideoOptions
import com.idunnololz.summit.links.onLinkClick
import com.idunnololz.summit.offline.OfflineManager
import com.idunnololz.summit.preferences.CommentGestureAction
import com.idunnololz.summit.preferences.PostFabQuickActions
import com.idunnololz.summit.preferences.PostGestureAction
import com.idunnololz.summit.preferences.Preferences
import com.idunnololz.summit.saved.FilteredPostsAndCommentsTabbedFragment
import com.idunnololz.summit.util.AnimationsHelper
import com.idunnololz.summit.util.BaseDialogFragment.Companion.gestureInterpolator
import com.idunnololz.summit.util.BaseFragment
import com.idunnololz.summit.util.BottomMenu
import com.idunnololz.summit.util.KeyPressRegistrationManager
import com.idunnololz.summit.util.PrettyPrintUtils
import com.idunnololz.summit.util.SharedElementTransition
import com.idunnololz.summit.util.StatefulData
import com.idunnololz.summit.util.Utils
import com.idunnololz.summit.util.ext.focusAndShowKeyboard
import com.idunnololz.summit.util.ext.getDimen
import com.idunnololz.summit.util.ext.getDrawableCompat
import com.idunnololz.summit.util.ext.navigateSafe
import com.idunnololz.summit.util.ext.setup
import com.idunnololz.summit.util.ext.showAllowingStateLoss
import com.idunnololz.summit.util.getParcelableCompat
import com.idunnololz.summit.util.insetViewAutomaticallyByPadding
import com.idunnololz.summit.util.insetViewExceptBottomAutomaticallyByMargins
import com.idunnololz.summit.util.insetViewExceptBottomAutomaticallyByPadding
import com.idunnololz.summit.util.insetViewExceptTopAutomaticallyByPadding
import com.idunnololz.summit.util.setupForFragment
import com.idunnololz.summit.util.showMoreLinkOptions
import com.idunnololz.summit.util.toErrorMessage
import dagger.hilt.android.AndroidEntryPoint
import java.io.IOException
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@AndroidEntryPoint
class PostFragment :
  BaseFragment<FragmentPostBinding>(),
  OldAlertDialogFragment.AlertDialogFragmentListener,
  SignInNavigator {

  companion object {
    private val TAG = PostFragment::class.java.canonicalName

    private const val CONFIRM_DELETE_COMMENT_TAG = "CONFIRM_DELETE_COMMENT_TAG"
    private const val EXTRA_COMMENT_ID = "EXTRA_COMMENT_ID"
  }

  private val args: PostFragmentArgs by navArgs()

  private val viewModel: PostViewModel by viewModels()

  private var adapter: PostAdapter? = null

  private var hasConsumedJumpToComments: Boolean = false

  @Inject
  lateinit var moreActionsHelperManager: MoreActionsHelperManager

  @Inject
  lateinit var historyManager: HistoryManager

  @Inject
  lateinit var offlineManager: OfflineManager

  @Inject
  lateinit var accountManager: AccountManager

  @Inject
  lateinit var postAndCommentViewBuilder: PostAndCommentViewBuilder

  @Inject
  lateinit var animationsHelper: AnimationsHelper

  @Inject
  lateinit var userTagsManager: UserTagsManager

  @Inject
  lateinit var lemmyTextHelper: LemmyTextHelper

  lateinit var moreActionsHelper: MoreActionsHelper

  lateinit var preferences: Preferences

  private var swipeActionCallback: LemmySwipeActionCallback? = null
  private var itemTouchHelper: ItemTouchHelper? = null
  private var commentNavViewController: CommentNavViewController? = null
  private var smoothScroller: SmoothScroller? = null
  private var layoutManager: LinearLayoutManager? = null

  private val scrollOffsetTop
    get() = (
      (requireSummitActivity().insets.value?.topInset ?: 0) +
        Utils.convertDpToPixel(56f)
      ).toInt()

  private val _sortByMenu: BottomMenu by lazy {
    BottomMenu(requireContext()).apply {
      addItem(R.id.sort_order_hot, R.string.sort_order_hot)
      addItem(R.id.sort_order_top, R.string.sort_order_top)
      addItem(R.id.sort_order_controversial, R.string.sort_order_controversial)
      addItem(R.id.sort_order_new, R.string.sort_order_new)
      addItem(R.id.sort_order_old, R.string.sort_order_old)
      setTitle(R.string.comments_sort_order)

      setOnMenuItemClickListener { menuItem ->
        viewModel.setCommentsSortOrder(
          idToCommentsSortOrder(menuItem.id) ?: CommentsSortOrder.Hot,
        )
      }
    }
  }

  private val findInPageBackPressHandler = object : OnBackPressedCallback(false) {
    override fun handleOnBackPressed() {
      viewModel.findInPageVisible.value = false
    }
  }
  private val screenshotModeBackPressHandler = object : OnBackPressedCallback(false) {
    override fun handleOnBackPressed() {
      viewModel.screenshotMode.value = false
    }
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    val accountId = accountId
    if (accountId != null) {
      viewModel.forceAccount(accountId)
      moreActionsHelper = moreActionsHelperManager.getAccountInstance(accountId)
    } else {
      moreActionsHelper = moreActionsHelperManager.getDefaultInstance()
    }

    preferences = viewModel.preferences

    if (savedInstanceState == null) {
      viewModel.updatePostViewIfNeeded(args.post)
      viewModel.updateOriginalPostOrCommentRef(args.postOrCommentRef())
      viewModel.updatePostOrCommentRef(args.postOrCommentRef())
    }

    sharedElementEnterTransition = SharedElementTransition()
    sharedElementReturnTransition = SharedElementTransition()

    childFragmentManager.setFragmentResultListener(
      CreateOrEditPostFragment.REQUEST_KEY,
      this,
    ) { _, bundle ->
      val result = bundle.getParcelableCompat<PostView>(
        CreateOrEditPostFragment.REQUEST_KEY_RESULT,
      )

      if (result != null) {
        viewModel.fetchPostData(force = true)
        (parentFragment as? CommunityFragment)?.updatePost(result.post.id, accountId)
      }
    }

    childFragmentManager.setFragmentResultListener(
      FastAccountSwitcherDialogFragment.REQUEST_KEY,
      this,
    ) { _, bundle ->
      val result = bundle.getParcelableCompat<Account>(
        FastAccountSwitcherDialogFragment.RESULT_ACCOUNT,
      )

      if (result != null) {
        viewModel.switchAccount(result)
      }
    }

    childFragmentManager.setFragmentResultListener(
      AccountsAndSettingsDialogFragment.REQUEST_KEY,
      this,
    ) { _, bundle ->
      val result = bundle.getParcelableCompat<Account>(
        AccountsAndSettingsDialogFragment.REQUEST_RESULT,
      )
      val hasResult = bundle.containsKey(AccountsAndSettingsDialogFragment.REQUEST_RESULT)

      if (hasResult) {
        if (result != null) {
          viewModel.switchAccount(result)
        } else {
          viewModel.switchAccount(result)
        }
      }
    }

    childFragmentManager.setFragmentResultListener(
      ScreenshotModeDialogFragment.REQUEST_KEY,
      this,
    ) { _, bundle ->
      val result = bundle.getParcelableCompat<ScreenshotModeDialogFragment.Result>(
        ScreenshotModeDialogFragment.REQUEST_KEY_RESULT,
      )

      if (result != null) {
        viewModel.screenshotMode.postValue(false)

        try {
          val uri = result.uri
          val mimeType = result.mimeType

          val snackbarMsg = getString(R.string.screenshot_saved, uri.toString())
          Snackbar.make(binding.root, snackbarMsg, Snackbar.LENGTH_LONG).setAction(
            R.string.view,
          ) {
            if (!isAdded) {
              return@setAction
            }

            Utils.safeLaunchExternalIntentWithErrorDialog(
              requireContext(),
              parentFragmentManager,
              Intent(Intent.ACTION_VIEW).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                  Intent.FLAG_GRANT_READ_URI_PERMISSION
                setDataAndType(uri, mimeType)
              },
            )
          }.show()
        } catch (e: IOException) { /* do nothing */ }
      }
    }

    if (!args.isSinglePage) {
      val predictiveBackMargin = resources.getDimensionPixelSize(
        R.dimen.predictive_back_margin,
      )
      var initialTouchY = -1f

      requireSummitActivity().onBackPressedDispatcher
        .addCallback(
          this,
          object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
              goBack()
            }

            override fun handleOnBackProgressed(backEvent: BackEventCompat) {
              val background = binding.root
              val progress = gestureInterpolator.getInterpolation(backEvent.progress)

              if (initialTouchY < 0f) {
                initialTouchY = backEvent.touchY
              }
              // See the motion spec about the calculations below.
              // https://developer.android.com/design/ui/mobile/guides/patterns/predictive-back#motion-specs

              // Shift horizontally.
              val maxTranslationX = predictiveBackMargin
              background.translationX = progress * maxTranslationX
            }

            override fun handleOnBackCancelled() {
              val background = binding.root
              initialTouchY = -1f
              background.run {
                translationX = 0f
              }
            }
          },
        )
    } else {
      // do things if this is a single page
    }

    requireSummitActivity().onBackPressedDispatcher.apply {
      addCallback(
        this@PostFragment,
        findInPageBackPressHandler,
      )
      addCallback(
        this@PostFragment,
        screenshotModeBackPressHandler,
      )
    }
  }

  private fun goBack() {
    when (val fragment = requireParentFragment()) {
      is CommunityFragment -> {
        fragment.closePost(this@PostFragment)
      }
      is PersonTabbedFragment -> {
        fragment.closePost(this@PostFragment)
      }
      is FilteredPostsAndCommentsTabbedFragment -> {
        fragment.closePost(this@PostFragment)
      }
      is SearchTabbedFragment -> {
        fragment.closePost(this@PostFragment)
      }
      is HistoryFragment -> {
        fragment.closePost(this@PostFragment)
      }
      is PostTabbedFragment -> {
        fragment.closePost(this@PostFragment)
      }
    }
  }

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?,
  ): View {
    setBinding(FragmentPostBinding.inflate(inflater, container, false))

    return binding.root
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)

    with(binding) {
      requireSummitActivity().apply {
        insetViewExceptBottomAutomaticallyByPadding(
          viewLifecycleOwner,
          binding.findInPageToolbar,
        )
      }

      fun onMoreClick() {
        val data = viewModel.postData.valueOrNull
        val postView = data?.postView?.post ?: args.post

        if (postView != null) {
          showMorePostOptions(
            accountId = accountId,
            postView = postView,
            moreActionsHelper = moreActionsHelper,
            fragmentManager = childFragmentManager,
            isPostMenu = true,
            onSortOrderClick = {
              getMainActivity()?.showBottomMenu(getSortByMenu())
            },
            onRefreshClick = {
              swipeRefreshLayout.isRefreshing = true
              viewModel.fetchPostData(force = true)
            },
            onFindInPageClick = {
              viewModel.findInPageVisible.value = true
            },
            onScreenshotClick = {
              viewModel.screenshotMode.value = true
            },
          )
        }
      }

      val context = requireContext()
      if (adapter == null) {
        adapter = PostAdapter(
          postAndCommentViewBuilder = postAndCommentViewBuilder,
          context = context,
          lifecycleOwner = this@PostFragment,
          instance = getInstance(),
          accountId = accountId,
          revealAll = args.reveal,
          useFooter = false,
          isEmbedded = false,
          videoState = args.videoState,
          autoCollapseCommentThreshold = preferences.autoCollapseCommentThreshold,
          lemmyTextHelper = lemmyTextHelper,
          onRefreshClickCb = {
            forceRefresh()
          },
          onSignInRequired = {
            PreAuthDialogFragment.newInstance()
              .show(childFragmentManager, "asdf")
          },
          onInstanceMismatch = { accountInstance, apiInstance ->
            onInstanceMismatch(accountInstance, apiInstance)
          },
          onAddCommentClick = { postOrComment ->
            onAddCommentClick(postOrComment)
          },
          onImageClick = { postOrCommentView, imageView, url ->
            getMainActivity()?.openImage(
              sharedElement = imageView,
              appBar = binding.appBar,
              title = postOrCommentView?.fold(
                {
                  it.post.name
                },
                {
                  null
                },
              ),
              url = url,
              mimeType = null,
            )
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
          onPostActionClick = { postView, itemId, actionId ->
            createPostActionHandler(
              accountId = accountId,
              postView = postView,
              moreActionsHelper = moreActionsHelper,
              fragmentManager = childFragmentManager,
              onScreenshotClick = {
                getAdapter()?.selectItemForScreenshot(itemId)
                viewModel.screenshotMode.value = true
              },
            )(actionId)
          },
          onCommentActionClick = { commentView, itemId, actionId ->
            createCommentActionHandler(
              commentView = commentView,
              moreActionsHelper = moreActionsHelper,
              fragmentManager = childFragmentManager,
              onLoadComment = {
                viewModel.updatePostOrCommentRef(Either.Right(CommentRef(getInstance(), it)))
                viewModel.fetchPostData()
              },
              onScreenshotClick = {
                getAdapter()?.selectItemForScreenshot(itemId)
                viewModel.screenshotMode.value = true
              },
            )(actionId)
          },
          onFetchComments = {
            viewModel.fetchMoreComments(it)
          },
          onLoadPost = { post, force ->
            viewModel.updatePostOrCommentRef(Either.Left(PostRef(getInstance(), post)))
            viewModel.fetchPostData(force = force)
          },
          onLoadCommentPath = {
            viewModel.fetchCommentPath(getInstance(), it)
          },
          onLinkClick = { url, text, linkType ->
            onLinkClick(url, text, linkType)
          },
          onLinkLongClick = { url, text ->
            getMainActivity()?.showMoreLinkOptions(url, text)
          },
          switchToNativeInstance = {
            viewModel.switchToNativeInstance()
          },
          onCrossPostsClick = a@{
            val activity = getSummitActivity() ?: return@a
            val crossPosts = viewModel.postData.valueOrNull?.crossPosts ?: return@a
            val bottomMenu = BottomMenu(activity)
              .apply {
                val idToChoice = mutableMapOf<Int, PostView>()

                setTitle(R.string.cross_posted_to)

                crossPosts.withIndex().forEach { (index, crossPost) ->
                  idToChoice[index] = crossPost

                  addRawItem(
                    BottomMenu.MenuItem.ActionItem(
                      id = index,
                      title = crossPost.community.toCommunityRef().fullName,
                      description = buildString {
                        append(
                          context.resources.getQuantityString(
                            R.plurals.point_count_format,
                            crossPost.counts.score,
                            PrettyPrintUtils.defaultDecimalFormat.format(crossPost.counts.score),
                          ),
                        )
                        append(", ")
                        append(
                          context.resources.getQuantityString(
                            R.plurals.comments_format,
                            crossPost.counts.comments,
                            PrettyPrintUtils.defaultDecimalFormat.format(crossPost.counts.comments),
                          ),
                        )
                      },
                      icon = BottomMenu.MenuIcon.CommunityIcon(
                        crossPost.community.toCommunityRef(), crossPost.community.icon,
                      ),
                    ),
                  )
                }

                setOnMenuItemClickListener {
                  val crossPost = requireNotNull(idToChoice[it.id])

                  getMainActivity()?.launchPage(PostRef(viewModel.apiInstance, crossPost.post.id))
                }
              }
            activity.showBottomMenu(bottomMenu)
          },
        ).apply {
          stateRestorationPolicy =
            RecyclerView.Adapter.StateRestorationPolicy.PREVENT_WHEN_EMPTY
        }
      }

      installOnActionResultHandler(
        context = context,
        moreActionsHelper = moreActionsHelper,
        snackbarContainer = binding.fabSnackbarCoordinatorLayout,
        onPostUpdated = { postId, accountId ->
          viewModel.fetchPostData(force = true)
          (parentFragment as? CommunityFragment)?.updatePost(
            postId = postId,
            accountId = accountId,
          )
        },
        onCommentUpdated = {
          viewModel.fetchMoreComments(it, 1, true)
        },
      )

      fab.setup(preferences)
      if (preferences.commentsNavigationFab) {
        fab.setImageResource(R.drawable.outline_navigation_24)
        fab.show()
        fab.setOnClickListener {
          viewModel.toggleCommentNavControls()
        }
      } else {
        fab.setImageResource(R.drawable.baseline_more_horiz_24)
        fab.show()
        fab.setOnClickListener {
          onMoreClick()
        }
      }

      when (preferences.postFabQuickAction) {
        PostFabQuickActions.NONE -> {}
        PostFabQuickActions.ADD_COMMENT -> {
          fab.setOnLongClickListener {
            val postView = viewModel.postData.valueOrNull?.postView?.post
            if (postView != null) {
              onAddCommentClick(Either.Left(postView))
            }
            true
          }
        }
        PostFabQuickActions.COLLAPSE_ALL_COMMENTS -> {
          fab.setOnLongClickListener {
            if (viewModel.maxDepth == 1) {
              viewModel.maxDepth = null
            } else {
              viewModel.maxDepth = 1
            }
            forceRefresh()
            true
          }
        }
        PostFabQuickActions.SCROLL_TO_TOP -> {
          fab.setOnLongClickListener {
            appBar.setExpanded(true, true)
            recyclerView.smoothScrollToPosition(0)
            true
          }
        }
      }

      commentNavViewController = CommentNavViewController(
        binding.fabSnackbarCoordinatorLayout,
        preferences,
      )
      smoothScroller = object : LinearSmoothScroller(context) {
        override fun getVerticalSnapPreference(): Int {
          return SNAP_TO_START
        }

        override fun calculateDtToFit(
          viewStart: Int,
          viewEnd: Int,
          boxStart: Int,
          boxEnd: Int,
          snapPreference: Int,
        ): Int {
          return boxStart - viewStart + (getMainActivity()?.insets?.value?.topInset ?: 0)
        }
      }
      viewModel.commentNavControlsState.observe(viewLifecycleOwner) {
        if (!preferences.commentsNavigationFab) {
          return@observe
        }

        if (it != null) {
          binding.fab.setImageDrawable(
            context.getDrawableCompat(R.drawable.baseline_close_24),
          )
          commentNavViewController?.show(
            it,
            onNextClick = {
              goToNextComment()
            },
            onPrevClick = {
              goToPreviousComment()
            },
            onMoreClick = {
              val data = viewModel.postData.valueOrNull
              val postView = data?.postView?.post ?: args.post

              if (postView != null) {
                onMoreClick()
              }
            },
          )
        } else {
          commentNavViewController?.hide()
          binding.fab.setImageDrawable(
            context.getDrawableCompat(R.drawable.outline_navigation_24),
          )
        }
      }

      if (preferences.useVolumeButtonNavigation) {
        requireSummitActivity().keyPressRegistrationManager.register(
          viewLifecycleOwner,
          object : KeyPressRegistrationManager.OnKeyPressHandler {
            override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
              if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
                goToNextComment()

                return true
              } else if (keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
                goToPreviousComment()

                return true
              }

              return false
            }
          },
        )
      }

      viewModel.currentAccountView.observe(viewLifecycleOwner) {
        it.loadProfileImageOrDefault(binding.accountImageView)
      }
      binding.accountImageView.setOnClickListener {
        if (accountId != null) {
          launchAlertDialog("account_switching_not_allowed") {
            messageResId = R.string.account_switching_disabled_desc
          }
          return@setOnClickListener
        }

        AccountsAndSettingsDialogFragment.newInstance(dontSwitchAccount = true)
          .showAllowingStateLoss(childFragmentManager, "AccountsDialogFragment")
      }
      viewModel.switchAccountState.observe(viewLifecycleOwner) {
        when (it) {
          is StatefulData.Error -> {
            binding.loadingViewFullscreen.visibility = View.GONE
            binding.loadingView2.hideAll()

            Snackbar
              .make(
                binding.coordinatorLayout,
                getString(
                  R.string.error_switch_instance_failed_format,
                  it.error.toErrorMessage(context),
                ),
                Snackbar.LENGTH_LONG,
              )
              .show()
          }
          is StatefulData.Loading -> {
            binding.loadingViewFullscreen.visibility = View.VISIBLE
            binding.loadingView2.showProgressBarWithMessage(it.statusDesc)
          }
          is StatefulData.NotStarted -> {
            binding.loadingViewFullscreen.visibility = View.GONE
            binding.loadingView2.hideAll()
          }
          is StatefulData.Success -> {
            binding.loadingViewFullscreen.visibility = View.GONE
            binding.loadingView2.hideAll()
          }
        }
      }
      viewModel.onPostOrCommentRefChange.observe(viewLifecycleOwner) {
        adapter?.instance = getInstance()
//                moreActionsHelper.setPageInstance(getInstance())
      }

      binding.searchEditText.addTextChangedListener {
        viewModel.setFindInPageQuery(it?.toString() ?: "")
      }
      binding.searchEditText.setOnKeyListener { _, actionId, keyEvent ->
        if (keyEvent.action == KeyEvent.ACTION_DOWN && actionId == KeyEvent.KEYCODE_ENTER) {
          Utils.hideKeyboard(activity)
          true
        } else {
          false
        }
      }
      viewModel.findInPageVisible.observe(viewLifecycleOwner) { showFindInPage ->
        if (showFindInPage) {
          binding.findInPageToolbar.visibility = View.VISIBLE
          binding.searchEditText.focusAndShowKeyboard()
        } else {
          binding.findInPageToolbar.visibility = View.GONE
          viewModel.findInPageQuery.value = ""
          Utils.hideKeyboard(activity)
        }

        findInPageBackPressHandler.isEnabled = showFindInPage
      }
      viewModel.findInPageQuery.observe(viewLifecycleOwner) {
        adapter?.setQuery(it) {
          viewModel.queryMatchHelper.setMatches(it)
        }
      }
      viewModel.queryMatchHelper.currentQueryMatch.observe(viewLifecycleOwner) { match ->
        if (viewModel.queryMatchHelper.matchCount == 0) {
          binding.foundCount.text = "0 / 0"
        } else {
          match ?: return@observe

          highlightMatch(match)
          adapter?.currentMatch = match
          binding.foundCount.text = "${match.matchIndex + 1} / " +
            "${viewModel.queryMatchHelper.matchCount}"
        }
      }
      viewModel.screenshotMode.observe(viewLifecycleOwner) { showScreenshotMode ->
        updateScreenshotMode(showScreenshotMode)
        screenshotModeBackPressHandler.isEnabled = showScreenshotMode
      }

      nextResult.setOnClickListener {
        viewModel.queryMatchHelper.nextMatch()
        Utils.hideKeyboard(activity)
      }
      prevResult.setOnClickListener {
        viewModel.queryMatchHelper.prevMatch()
        Utils.hideKeyboard(activity)
      }
      clear.setOnClickListener {
        viewModel.findInPageVisible.value = false
      }

      runAfterLayout {
        if (!isBindingAvailable()) return@runAfterLayout

        adapter?.setContentMaxSize(
          binding.recyclerView.width,
          binding.recyclerView.height,
        )

        setup()
      }

      viewLifecycleOwner.lifecycleScope.launch {
        userTagsManager.onChangedFlow.collect {
          adapter?.notifyDataSetChanged()
        }
      }
    }
  }

  private fun onAddCommentClick(postOrComment: Either<PostView, CommentView>) {
    if (accountManager.currentAccount.value == null) {
      PreAuthDialogFragment.newInstance(R.id.action_add_comment)
        .show(childFragmentManager, "asdf")
      return
    }

    AddOrEditCommentFragment.showReplyDialog(
      instance = getInstance(),
      postOrCommentView = postOrComment,
      fragmentManager = childFragmentManager,
      accountId = accountId,
    )
  }

  private fun onInstanceMismatch(accountInstance: String, apiInstance: String) {
    OldAlertDialogFragment.Builder()
      .setTitle(R.string.error_account_instance_mismatch_title)
      .setMessage(
        getString(
          R.string.error_account_instance_mismatch,
          accountInstance,
          apiInstance,
        ),
      )
      .setPositiveButton(R.string.change_instance)
      .setNegativeButton(R.string.cancel)
      .createAndShow(childFragmentManager, "instance_mismatch")
  }

  private fun updateScreenshotMode(screenshotMode: Boolean?) {
    screenshotMode ?: return
    if (!isBindingAvailable()) {
      return
    }

    val context = requireContext()

    adapter?.screenshotMode = screenshotMode

    if (screenshotMode) {
      ScreenshotModeAppBarBinding
        .inflate(LayoutInflater.from(context), binding.root, false)
        .apply {
          root.updateLayoutParams<CoordinatorLayout.LayoutParams> {
            this.gravity = Gravity.BOTTOM
          }
          binding.root.addView(root)
          root.updatePadding(
            left = 0,
            top = 0,
            right = 0,
            bottom = getMainActivity()?.insets?.value?.bottomInset ?: 0,
          )

          screenshotModeAppBar.setOnMenuItemClickListener {
            when (it.itemId) {
              R.id.cancel -> {
                viewModel.screenshotMode.postValue(false)
              }
            }

            true
          }
          screenshotModeFab.setOnClickListener {
            ScreenshotModeDialogFragment.show(childFragmentManager)
          }
        }
      binding.fab.hide()
    } else {
      binding.root.removeView(binding.root.findViewById(R.id.screenshot_mode_container))
      binding.fab.show()
    }
  }

  private fun highlightMatch(match: QueryMatchHelper.QueryResult) {
    layoutManager?.scrollToPositionWithOffset(match.itemIndex, scrollOffsetTop)
  }

  private fun goToNextComment() {
    (binding.recyclerView.layoutManager as? LinearLayoutManager)?.let {
      var curPos = it.findFirstCompletelyVisibleItemPosition()

      if (curPos == -1) {
        curPos = it.findFirstVisibleItemPosition()
      }

      val pos = adapter
        ?.getNextTopLevelCommentPosition(curPos)
      if (pos != null) {
        smoothScroller?.targetPosition = pos
        it.startSmoothScroll(smoothScroller)
      }
    }
  }

  private fun goToPreviousComment() {
    (binding.recyclerView.layoutManager as? LinearLayoutManager)?.let {
      var curPos = it.findFirstCompletelyVisibleItemPosition()

      if (curPos == -1) {
        curPos = it.findFirstVisibleItemPosition()
      }

      val pos = adapter
        ?.getPrevTopLevelCommentPosition(curPos)
      if (pos != null) {
        smoothScroller?.targetPosition = pos
        it.startSmoothScroll(smoothScroller)
      }
    }
  }

  private fun getInstance() = viewModel.postOrCommentRef?.fold(
    { it.instance },
    { it.instance },
  ) ?: args.instance

  override fun onDestroyView() {
    Log.d(TAG, "onDestroyView()")

    itemTouchHelper?.attachToRecyclerView(null) // detach the itemTouchHelper
    itemTouchHelper = null

    commentNavViewController = null
    swipeActionCallback = null
    binding.recyclerView.adapter = null
    binding.recyclerView.layoutManager = null

    super.onDestroyView()
  }

  private fun setup() {
    if (!isBindingAvailable()) {
      return
    }

    val adapter = adapter ?: return

    val context = requireContext()

    requireSummitActivity().apply {
      insetViewExceptTopAutomaticallyByPadding(
        lifecycleOwner = viewLifecycleOwner,
        rootView = binding.recyclerView,
        additionalPaddingBottom = context.getDimen(R.dimen.footer_spacer_height),
      )
      insetViewExceptBottomAutomaticallyByMargins(viewLifecycleOwner, binding.toolbar)
      insetViewAutomaticallyByPadding(
        viewLifecycleOwner,
        binding.fabSnackbarCoordinatorLayout,
      )
    }

    with(binding.toolbar) {
      setNavigationIcon(
        com.google.android.material.R.drawable.ic_arrow_back_black_24,
      )
      setNavigationOnClickListener {
        if (args.isSinglePage) {
          findNavController().navigateUp()
        } else {
          goBack()
        }
      }
    }

    updateTitle(
      viewModel.commentsSortOrderLiveData.value?.getLocalizedName(context) ?: "",
    )
    binding.subtitle.visibility = View.GONE

    binding.title.setOnClickListener {
      getMainActivity()?.showBottomMenu(getSortByMenu())
    }

    val swipeRefreshLayout = binding.swipeRefreshLayout

    swipeRefreshLayout.setOnRefreshListener {
      forceRefresh()
    }
    binding.loadingView.setOnRefreshClickListener {
      forceRefresh()
    }
    binding.loadingView2.setOnRefreshClickListener {
      forceRefresh()
    }

    args.post?.let { post ->
      adapter.setStartingData(
        PostViewModel.PostData(
          postView = PostViewModel.ListView.PostListView(post),
          commentTree = listOf(),
          crossPosts = listOf(),
          newlyPostedCommentId = null,
          selectedCommentId = null,
          isSingleComment = false,
          isNativePost = viewModel.isNativePost(),
          accountInstance = viewModel.currentAccountView.value?.account?.instance,
          isCommentsLoaded = false,
          commentPath = null,
          wasUpdateForced = false,
        ),
      )
      onMainListingItemRetrieved(post)
    } ?: binding.loadingView.showProgressBar()

    viewModel.postData.observe(viewLifecycleOwner) {
      when (it) {
        is StatefulData.Error -> {
          binding.swipeRefreshLayout.isRefreshing = false
          binding.loadingView.hideAll()

          adapter.error = it.error
        }
        is StatefulData.Loading -> {
          if (!adapter.hasStartingData()) {
            binding.loadingView.showProgressBar()
          }
        }
        is StatefulData.NotStarted -> {}
        is StatefulData.Success -> {
          binding.swipeRefreshLayout.isRefreshing = false
          binding.loadingView.hideAll()
          adapter.setData(it.data)
          onMainListingItemRetrieved(it.data.postView.post)

          val newlyPostedCommentId = it.data.newlyPostedCommentId
          if (newlyPostedCommentId != null) {
            val pos = adapter.getPositionOfComment(newlyPostedCommentId)

            viewModel.resetNewlyPostedComment()

            binding.recyclerView.post {
              if (!isBindingAvailable()) {
                return@post
              }

              if (pos >= 0) {
                (binding.recyclerView.layoutManager as? LinearLayoutManager)
                  ?.scrollToPositionWithOffset(
                    pos,
                    scrollOffsetTop,
                  )
                adapter.highlightComment(newlyPostedCommentId)

                lifecycleScope.launch {
                  withContext(Dispatchers.IO) {
                    delay(HIGHLIGHT_COMMENT_MS)
                  }

                  withContext(Dispatchers.Main) {
                    adapter.clearHighlightComment()
                  }
                }
              }
            }
          }
        }
      }
    }

    if (viewModel.postData.valueOrNull == null) {
      lifecycleScope.launch(Dispatchers.Default) {
        if (preferences.delayWhenLoadingData) {
          delay(400)
        }

        withContext(Dispatchers.Main) {
          viewModel.fetchPostData(switchToNativeInstance = args.switchToNativeInstance)
        }
      }
    }

    args.post?.getUrl(getInstance())?.let { url ->
      historyManager.recordVisit(
        jsonUrl = url,
        saveReason = HistorySaveReason.LOADING,
        post = args.post,
      )
    }

    layoutManager = LinearLayoutManager(context)
    binding.recyclerView.setup(animationsHelper)
    binding.recyclerView.adapter = adapter
    binding.recyclerView.setHasFixedSize(true)
    binding.recyclerView.layoutManager = layoutManager
    binding.recyclerView.setupForPostAndComments(preferences)
    binding.fastScroller.setRecyclerView(binding.recyclerView)

    if (!hasConsumedJumpToComments && args.jumpToComments) {
      Log.d("TEST", "jumping to comments")
      hasConsumedJumpToComments = true
      (binding.recyclerView.layoutManager as LinearLayoutManager)
        .scrollToPositionWithOffset(1, scrollOffsetTop)
    }

    viewModel.commentsSortOrderLiveData.observe(viewLifecycleOwner) {
      updateTitle(viewModel.commentsSortOrderLiveData.value?.getLocalizedName(context) ?: "")
    }

    binding.root.doOnPreDraw {
      adapter.setContentMaxSize(
        binding.recyclerView.width,
        binding.recyclerView.height,
      )
    }
  }

  private fun updateTitle(titleText: String) {
    binding.toolbar.title = titleText
    binding.title.text = titleText
  }

  private fun attachGestureHandlerToRecyclerViewIfNeeded() {
    if (!isBindingAvailable()) return
    if (!preferences.useGestureActions) return

    val context = requireContext()

    if (itemTouchHelper == null) {
      swipeActionCallback = LemmySwipeActionCallback(
        context = context,
        recyclerView = binding.recyclerView,
        onActionSelected = { action, vh ->
          val postView = vh.itemView.tag as? PostView
          if (postView != null) {
            performPostAction(action.id, postView)
            return@LemmySwipeActionCallback
          }

          val commentView = vh.itemView.getTag(R.id.comment_view) as? CommentView
            ?: return@LemmySwipeActionCallback

          var result: Result<Unit>? = null

          when (action.id) {
            CommentGestureAction.Upvote -> {
              result = moreActionsHelper.vote(commentView, dir = 1, toggle = true, accountId = accountId)
            }

            CommentGestureAction.Downvote -> {
              result = moreActionsHelper.vote(commentView, dir = -1, toggle = true, accountId = accountId)
            }

            CommentGestureAction.Bookmark -> {
              moreActionsHelper.saveComment(commentView.comment.id, true)
            }

            CommentGestureAction.Reply -> {
              AddOrEditCommentFragment.showReplyDialog(
                instance = getInstance(),
                postOrCommentView = Either.Right(commentView),
                fragmentManager = childFragmentManager,
                accountId = accountId,
              )
            }

            CommentGestureAction.CollapseOrExpand -> {
              adapter?.toggleSection(vh.bindingAdapterPosition)
            }
          }

          val error = result?.exceptionOrNull()
          if (error is AccountInstanceMismatchException) {
            onInstanceMismatch(error.accountInstance, error.apiInstance)
          }
        },
        gestureSize = preferences.commentGestureSize,
        hapticsEnabled = preferences.hapticsEnabled,
        swipeDirection = preferences.gestureSwipeDirection,
      )
      itemTouchHelper = ItemTouchHelper(requireNotNull(swipeActionCallback))
    }

    swipeActionCallback?.apply {
      actions = preferences.getCommentSwipeActions(context)
      gestureSize = preferences.commentGestureSize
      postOnlyActions = preferences.getPostSwipeActions(context)
      postOnlyGestureSize = preferences.postGestureSize
      hapticsEnabled = preferences.hapticsEnabled
      swipeDirection = preferences.gestureSwipeDirection

      updateCommentSwipeActions()
    }

    itemTouchHelper?.attachToRecyclerView(null)
    itemTouchHelper?.attachToRecyclerView(binding.recyclerView)
  }

  private fun LemmySwipeActionCallback.updateCommentSwipeActions() {
    if (!isBindingAvailable()) return
    val context = requireContext()
    this.actions = preferences.getCommentSwipeActions(context)
  }

  private fun performPostAction(id: Int, postView: PostView) {
    when (id) {
      PostGestureAction.Upvote -> {
        moreActionsHelper.vote(postView, 1, toggle = true, accountId = accountId)
      }

      PostGestureAction.Downvote -> {
        moreActionsHelper.vote(postView, -1, toggle = true, accountId = accountId)
      }

      PostGestureAction.Bookmark -> {
        moreActionsHelper.savePost(postView.post.id, save = true)
      }

      PostGestureAction.Reply -> {
        if (accountManager.currentAccount.value == null) {
          PreAuthDialogFragment.newInstance(R.id.action_add_comment)
            .show(childFragmentManager, "asdf")
          return
        }

        AddOrEditCommentFragment.showReplyDialog(
          instance = viewModel.apiInstance,
          postOrCommentView = Either.Left(postView),
          fragmentManager = childFragmentManager,
          accountId = accountId,
        )
      }

      PostGestureAction.Hide -> {
        moreActionsHelper.hidePost(postView.post.id, postView)
      }

      PostGestureAction.MarkAsRead -> {
        moreActionsHelper.onPostRead(
          postView = postView,
          delayMs = 250,
          accountId = accountId,
          read = true,
        )
      }
    }
  }

  override fun onResume() {
    super.onResume()

    if (!args.isPreview) {
      setupForFragment<PostFragment>()
    }

    if (args.isSinglePage) {
      requireSummitActivity().apply {
        if (!navBarController.useNavigationRail) {
          hideNavBar()
        }
        lockUiOpenness = true
      }
    }

    runAfterLayout {
      adapter?.setContentMaxSize(
        binding.recyclerView.width,
        binding.recyclerView.height,
      )

      attachGestureHandlerToRecyclerViewIfNeeded()
    }

    postAndCommentViewBuilder.onPreferencesChanged()
  }

  override fun onPause() {
    if (args.isSinglePage) {
      requireSummitActivity().apply {
        lockUiOpenness = false
      }
    }

    super.onPause()
  }

  override fun navigateToSignInScreen() {
    if (args.isSinglePage) {
      val direction = PostFragmentDirections.actionGlobalLogin()
      findNavController().navigateSafe(direction)
    } else {
      (parentFragment as? SignInNavigator)?.navigateToSignInScreen()
    }
  }

  override fun proceedAnyways(tag: Int) {
    when (tag) {
      R.id.action_add_comment -> {
        val postView = viewModel.postData.valueOrNull?.postView ?: return

        AddOrEditCommentFragment.showReplyDialog(
          instance = getInstance(),
          postOrCommentView = Either.Left(postView.post),
          fragmentManager = childFragmentManager,
          accountId = accountId,
        )
      }
      R.id.action_edit_comment -> {
        val postView = viewModel.postData.valueOrNull?.postView ?: return

        AddOrEditCommentFragment.showReplyDialog(
          instance = getInstance(),
          postOrCommentView = Either.Left(postView.post),
          fragmentManager = childFragmentManager,
          accountId = accountId,
        )
      }
    }
  }

  private fun forceRefresh() {
    viewModel.fetchPostData(force = true)
  }

  private fun onMainListingItemRetrieved(post: PostView) {
    post.getUrl(getInstance()).let { url ->
      historyManager.recordVisit(
        jsonUrl = url,
        saveReason = HistorySaveReason.LOADED,
        post = post,
      )
    }
  }

  private fun getSortByMenu(): BottomMenu {
    when (viewModel.commentsSortOrderLiveData.value) {
      CommentsSortOrder.Hot -> _sortByMenu.setChecked(R.id.sort_order_hot)
      CommentsSortOrder.Top -> _sortByMenu.setChecked(R.id.sort_order_top)
      CommentsSortOrder.New -> _sortByMenu.setChecked(R.id.sort_order_new)
      CommentsSortOrder.Old -> _sortByMenu.setChecked(R.id.sort_order_old)
      CommentsSortOrder.Controversial -> _sortByMenu.setChecked(R.id.sort_order_controversial)
      else -> {}
    }

    return _sortByMenu
  }

  override fun onPositiveClick(dialog: OldAlertDialogFragment, tag: String?) {
    when (tag) {
      CONFIRM_DELETE_COMMENT_TAG -> {
        val commentId = dialog.getExtra(EXTRA_COMMENT_ID)
        if (commentId != null) {
          viewModel.deleteComment(
            accountId = accountId,
            postRef = PostRef(getInstance(), args.id),
            commentId = commentId.toInt(),
          )
        }
      }
      "instance_mismatch" -> {
        viewModel.switchToNativeInstance()
      }
    }
  }

  override fun onNegativeClick(dialog: OldAlertDialogFragment, tag: String?) {
    // do nothing
  }

  private fun PostFragmentArgs.postOrCommentRef() = if (this.id > 0) {
    Either.Left(PostRef(this.instance, this.id))
  } else {
    Either.Right(CommentRef(this.instance, this.commentId))
  }

  fun getAdapter() = adapter

  private val accountId
    get() =
      if (args.accountId != 0L) {
        args.accountId
      } else {
        null
      }
}
