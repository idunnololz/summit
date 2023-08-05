package com.idunnololz.summit.lemmy.post

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.core.view.doOnPreDraw
import androidx.fragment.app.setFragmentResultListener
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
import com.idunnololz.summit.R
import com.idunnololz.summit.account.AccountManager
import com.idunnololz.summit.accountUi.PreAuthDialogFragment
import com.idunnololz.summit.accountUi.SignInNavigator
import com.idunnololz.summit.alert.AlertDialogFragment
import com.idunnololz.summit.api.ClientApiException
import com.idunnololz.summit.api.dto.CommentView
import com.idunnololz.summit.api.dto.PostView
import com.idunnololz.summit.api.utils.getUrl
import com.idunnololz.summit.databinding.FragmentPostBinding
import com.idunnololz.summit.error.ErrorDialogFragment
import com.idunnololz.summit.history.HistoryManager
import com.idunnololz.summit.history.HistorySaveReason
import com.idunnololz.summit.lemmy.CommentRef
import com.idunnololz.summit.lemmy.CommentsSortOrder
import com.idunnololz.summit.lemmy.MoreActionsViewModel
import com.idunnololz.summit.lemmy.PostRef
import com.idunnololz.summit.lemmy.actions.LemmySwipeActionCallback
import com.idunnololz.summit.lemmy.comment.AddOrEditCommentFragment
import com.idunnololz.summit.lemmy.comment.AddOrEditCommentFragmentArgs
import com.idunnololz.summit.lemmy.community.CommunityFragment
import com.idunnololz.summit.lemmy.createOrEditPost.CreateOrEditPostFragment
import com.idunnololz.summit.lemmy.getLocalizedName
import com.idunnololz.summit.lemmy.idToCommentsSortOrder
import com.idunnololz.summit.lemmy.person.PersonTabbedFragment
import com.idunnololz.summit.lemmy.post.PostViewModel.Companion.HIGHLIGHT_COMMENT_MS
import com.idunnololz.summit.lemmy.postAndCommentView.PostAndCommentViewBuilder
import com.idunnololz.summit.lemmy.postAndCommentView.setupForPostAndComments
import com.idunnololz.summit.lemmy.postAndCommentView.showMoreCommentOptions
import com.idunnololz.summit.lemmy.postListView.showMorePostOptions
import com.idunnololz.summit.lemmy.search.SearchTabbedFragment
import com.idunnololz.summit.lemmy.utils.getCommentSwipeActions
import com.idunnololz.summit.lemmy.utils.getPostSwipeActions
import com.idunnololz.summit.lemmy.utils.installOnActionResultHandler
import com.idunnololz.summit.offline.OfflineManager
import com.idunnololz.summit.preferences.CommentGestureAction
import com.idunnololz.summit.preferences.PostGestureAction
import com.idunnololz.summit.preferences.Preferences
import com.idunnololz.summit.saved.SavedTabbedFragment
import com.idunnololz.summit.util.BaseFragment
import com.idunnololz.summit.util.BottomMenu
import com.idunnololz.summit.util.LinkUtils
import com.idunnololz.summit.util.PreferenceUtil
import com.idunnololz.summit.util.SharedElementTransition
import com.idunnololz.summit.util.StatefulData
import com.idunnololz.summit.util.Utils
import com.idunnololz.summit.util.ext.getDimen
import com.idunnololz.summit.util.ext.getDrawableCompat
import com.idunnololz.summit.util.ext.navigateSafe
import com.idunnololz.summit.util.getParcelableCompat
import com.idunnololz.summit.util.showBottomMenuForLink
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@AndroidEntryPoint
class PostFragment :
    BaseFragment<FragmentPostBinding>(),
    AlertDialogFragment.AlertDialogFragmentListener,
    SignInNavigator {

    companion object {
        private val TAG = PostFragment::class.java.canonicalName

        private const val CONFIRM_DELETE_COMMENT_TAG = "CONFIRM_DELETE_COMMENT_TAG"
        private const val EXTRA_COMMENT_ID = "EXTRA_COMMENT_ID"
    }

    private val args: PostFragmentArgs by navArgs()

    private val viewModel: PostViewModel by viewModels()
    private val actionsViewModel: MoreActionsViewModel by viewModels()

    private var adapter: PostsAdapter? = null

    private var hasConsumedJumpToComments: Boolean = false

    @Inject
    lateinit var historyManager: HistoryManager

    @Inject
    lateinit var offlineManager: OfflineManager

    @Inject
    lateinit var accountManager: AccountManager

    @Inject
    lateinit var postAndCommentViewBuilder: PostAndCommentViewBuilder

    @Inject
    lateinit var preferences: Preferences

    private var swipeActionCallback: LemmySwipeActionCallback? = null
    private var itemTouchHelper: ItemTouchHelper? = null
    private var commentNavViewController: CommentNavViewController? = null

    private val _sortByMenu: BottomMenu by lazy {
        BottomMenu(requireContext()).apply {
            addItem(R.id.sort_order_hot, R.string.sort_order_hot)
            addItem(R.id.sort_order_top, R.string.sort_order_top)
            addItem(R.id.sort_order_new, R.string.sort_order_new)
            addItem(R.id.sort_order_old, R.string.sort_order_old)
            setTitle(R.string.sort_by)

            setOnMenuItemClickListener { menuItem ->
                viewModel.setCommentsSortOrder(
                    idToCommentsSortOrder(menuItem.id) ?: CommentsSortOrder.Hot,
                )
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val context = requireContext()

        PreferenceUtil.preferences.edit().remove(PreferenceUtil.KEY_COMMENTS_NAVIGATION_FAB_OFF_Y).apply()

        sharedElementEnterTransition = SharedElementTransition()
        sharedElementReturnTransition = SharedElementTransition()

        setFragmentResultListener(AddOrEditCommentFragment.REQUEST_KEY) { _, bundle ->
//            val result = bundle.getParcelableCompat<AddOrEditCommentFragment.Result>(AddOrEditCommentFragment.REQUEST_KEY_RESULT)
//
//            if (result != null) {
//                viewModel.fetchPostData(args.instance, args.id)
//            }
        }

        requireActivity().supportFragmentManager.setFragmentResultListener(
            CreateOrEditPostFragment.REQUEST_KEY,
            this,
        ) { _, bundle ->
            val result = bundle.getParcelableCompat<PostView>(
                CreateOrEditPostFragment.REQUEST_KEY_RESULT,
            )

            if (result != null) {
                viewModel.fetchPostData(args.postOrCommentRef(), force = true)
                (parentFragment as? CommunityFragment)?.onPostUpdated()
            }
        }

        if (!args.isSinglePage) {
            requireMainActivity().onBackPressedDispatcher
                .addCallback(
                    this,
                    object : OnBackPressedCallback(true) {
                        override fun handleOnBackPressed() {
                            goBack()
                        }
                    },
                )
        } else {
            // do things if this is a single page
        }

        actionsViewModel.setPageInstance(args.instance)
    }

    private fun goBack() {
        when (val fragment = requireParentFragment()) {
            is CommunityFragment -> {
                fragment.closePost(this@PostFragment)
            }
            is PersonTabbedFragment -> {
                fragment.closePost(this@PostFragment)
            }
            is SavedTabbedFragment -> {
                fragment.closePost(this@PostFragment)
            }
            is SearchTabbedFragment -> {
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

        val context = requireContext()
        if (adapter == null) {
            adapter = PostsAdapter(
                postAndCommentViewBuilder = postAndCommentViewBuilder,
                context,
                binding.recyclerView,
                this,
                args.instance,
                args.reveal,
                useFooter = false,
                accountManager.currentAccount.value?.id,
                args.videoState,
                onRefreshClickCb = {
                    forceRefresh()
                },
                onSignInRequired = {
                    PreAuthDialogFragment.newInstance()
                        .show(childFragmentManager, "asdf")
                },
                onInstanceMismatch = { accountInstance, apiInstance ->
                    AlertDialogFragment.Builder()
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
                    if (accountManager.currentAccount.value == null) {
                        PreAuthDialogFragment.newInstance(R.id.action_add_comment)
                            .show(childFragmentManager, "asdf")
                        return@PostsAdapter
                    }

                    AddOrEditCommentFragment().apply {
                        arguments = postOrComment.fold(
                            {
                                AddOrEditCommentFragmentArgs(
                                    args.instance,
                                    null,
                                    it,
                                    null,
                                )
                            },
                            {
                                AddOrEditCommentFragmentArgs(
                                    args.instance,
                                    it,
                                    null,
                                    null,
                                )
                            },
                        ).toBundle()
                    }.show(childFragmentManager, "asdf")
                },
                onImageClick = { postOrCommentView, view, url ->
                    getMainActivity()?.openImage(
                        sharedElement = view,
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
                onPageClick = {
                    getMainActivity()?.launchPage(it)
                },
                onPostMoreClick = { postView ->
                    actionsViewModel.let {
                        showMorePostOptions(viewModel.apiInstance, postView, it, childFragmentManager)
                    }
                },
                onCommentMoreClick = { commentView ->
                    actionsViewModel.let {
                        showMoreCommentOptions(viewModel.apiInstance, commentView, it, childFragmentManager)
                    }
                },
                onFetchComments = {
                    viewModel.fetchMoreComments(it)
                },
                onLoadPost = {
                    viewModel.fetchPostData(Either.Left(PostRef(args.instance, it)))
                },
                onLinkLongClick = { url, text ->
                    getMainActivity()?.showBottomMenuForLink(url, text)
                },
            ).apply {
                stateRestorationPolicy =
                    RecyclerView.Adapter.StateRestorationPolicy.PREVENT_WHEN_EMPTY
            }
        }

        installOnActionResultHandler(actionsViewModel, binding.coordinatorLayout)

        runAfterLayout {
            if (!isBindingAvailable()) return@runAfterLayout

            adapter?.contentMaxWidth = binding.recyclerView.width

            setup()
        }

        if (preferences.commentsNavigationFab) {
            binding.fab.setImageResource(R.drawable.outline_navigation_24)
            binding.fab.show()
            binding.fab.setOnClickListener {
                viewModel.toggleCommentNavControls()
            }
        } else {
            binding.fab.setImageResource(R.drawable.baseline_more_horiz_24)
            binding.fab.show()
            binding.fab.setOnClickListener {
                val data = viewModel.postData.valueOrNull
                val postView = data?.postView?.post ?: args.post

                if (postView != null) {
                    showMorePostOptions(viewModel.apiInstance, postView, actionsViewModel, childFragmentManager)
                }
            }
        }

        commentNavViewController = CommentNavViewController(
            binding.fabSnackbarCoordinatorLayout,
            preferences,
        )
        val smoothScroller: SmoothScroller = object : LinearSmoothScroller(context) {
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
                return boxStart - viewStart + (getMainActivity()?.lastInsets?.topInset ?: 0)
            }
        }
        viewModel.commentNavControlsState.observe(viewLifecycleOwner) {
            if (!preferences.commentsNavigationFab) {
                return@observe
            }

            if (it != null) {
                binding.fab.setImageDrawable(context.getDrawableCompat(R.drawable.baseline_close_24))
                commentNavViewController?.show(
                    it,
                    onNextClick = {
                        (binding.recyclerView.layoutManager as? LinearLayoutManager)?.let {
                            var curPos = it.findFirstCompletelyVisibleItemPosition()

                            if (curPos == -1) {
                                curPos = it.findFirstVisibleItemPosition()
                            }

                            val pos = adapter
                                ?.getNextTopLevelCommentPosition(curPos)
                            if (pos != null) {
                                smoothScroller.targetPosition = pos
                                it.startSmoothScroll(smoothScroller)
                            }
                        }
                    },
                    onPrevClick = {
                        (binding.recyclerView.layoutManager as? LinearLayoutManager)?.let {
                            var curPos = it.findFirstCompletelyVisibleItemPosition()

                            if (curPos == -1) {
                                curPos = it.findFirstVisibleItemPosition()
                            }

                            val pos = adapter
                                ?.getPrevTopLevelCommentPosition(curPos)
                            if (pos != null) {
                                smoothScroller.targetPosition = pos
                                it.startSmoothScroll(smoothScroller)
                            }
                        }
                    },
                    onMoreClick = {
                        val data = viewModel.postData.valueOrNull
                        val postView = data?.postView?.post ?: args.post

                        if (postView != null) {
                            showMorePostOptions(viewModel.apiInstance, postView, actionsViewModel, childFragmentManager)
                        }
                    },
                )
            } else {
                commentNavViewController?.hide()
                binding.fab.setImageDrawable(context.getDrawableCompat(R.drawable.outline_navigation_24))
            }
        }
    }

    override fun onDestroyView() {
        Log.d(TAG, "onDestroyView()")

        itemTouchHelper?.attachToRecyclerView(null) // detach the itemTouchHelper

        super.onDestroyView()
    }

    private fun setup() {
        if (!isBindingAvailable()) {
            return
        }

        val adapter = adapter ?: return

        val context = requireContext()

        requireMainActivity().apply {
            insetViewExceptTopAutomaticallyByPadding(
                lifecycleOwner = viewLifecycleOwner,
                rootView = binding.recyclerView,
                additionalPaddingBottom = context.getDimen(R.dimen.footer_spacer_height),
            )
            insetViewExceptBottomAutomaticallyByMargins(viewLifecycleOwner, binding.toolbar)
            insetViewAutomaticallyByPadding(viewLifecycleOwner, binding.fabSnackbarCoordinatorLayout)
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
            title = viewModel.commentsSortOrderLiveData.value?.getLocalizedName(context) ?: ""

            inflateMenu(R.menu.menu_fragment_post)
            setOnMenuItemClickListener {
                when (it.itemId) {
                    R.id.share -> {
                        val sendIntent: Intent = Intent().apply {
                            action = Intent.ACTION_SEND
                            putExtra(
                                Intent.EXTRA_TEXT,
                                LinkUtils.postIdToLink(viewModel.apiInstance, args.id),
                            )
                            type = "text/plain"
                        }

                        val shareIntent = Intent.createChooser(sendIntent, null)
                        startActivity(shareIntent)
                        true
                    }

                    R.id.sort_comments_by -> {
                        getMainActivity()?.showBottomMenu(getSortByMenu())
                        true
                    }

                    R.id.refresh -> {
                        viewModel.fetchPostData(args.postOrCommentRef(), force = true)
                        true
                    }

                    else -> false
                }
            }
        }

        val swipeRefreshLayout = binding.swipeRefreshLayout

        swipeRefreshLayout.setOnRefreshListener {
            forceRefresh()
        }
        binding.loadingView.setOnRefreshClickListener {
            forceRefresh()
        }

        args.post?.let { post ->
            adapter.setStartingData(
                PostViewModel.PostData(
                    PostViewModel.ListView.PostListView(post),
                    listOf(),
                    null,
                    null,
                    false,
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
                                        (requireMainActivity().lastInsets.topInset + Utils.convertDpToPixel(56f)).toInt(),
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

        actionsViewModel.deletePostResult.observe(viewLifecycleOwner) {
            when (it) {
                is StatefulData.Error -> {
                    if (it.error is ClientApiException && it.error.errorCode == 404) {
                        viewModel.fetchPostData(args.postOrCommentRef(), force = true)
                        (parentFragment as? CommunityFragment)?.onPostUpdated()
                    } else {
                        ErrorDialogFragment.show(
                            getString(R.string.error_unable_to_delete_post),
                            it.error,
                            childFragmentManager,
                        )
                    }
                }
                is StatefulData.Loading -> {}
                is StatefulData.NotStarted -> {}
                is StatefulData.Success -> {
                    viewModel.fetchPostData(args.postOrCommentRef(), force = true)
                    (parentFragment as? CommunityFragment)?.onPostUpdated()
                }
            }
        }

        if (viewModel.postData.valueOrNull == null) {
            lifecycleScope.launch(Dispatchers.Default) {
                delay(400)

                withContext(Dispatchers.Main) {
                    viewModel.fetchPostData(args.postOrCommentRef())
                }
            }
        }

        args.post?.getUrl(args.instance)?.let { url ->
            historyManager.recordVisit(
                jsonUrl = url,
                saveReason = HistorySaveReason.LOADING,
                post = args.post,
            )
        }

        binding.recyclerView.adapter = adapter
        binding.recyclerView.setHasFixedSize(true)
        binding.recyclerView.layoutManager = LinearLayoutManager(context)
        binding.recyclerView.setupForPostAndComments(preferences)
        binding.fastScroller.setRecyclerView(binding.recyclerView)

        if (!hasConsumedJumpToComments && args.jumpToComments) {
            hasConsumedJumpToComments = true
            (binding.recyclerView.layoutManager as LinearLayoutManager)
                .scrollToPositionWithOffset(1, (Utils.convertDpToPixel(48f)).toInt())
        }

        viewModel.commentsSortOrderLiveData.observe(viewLifecycleOwner) {
            binding.toolbar.title =
                viewModel.commentsSortOrderLiveData.value?.getLocalizedName(context) ?: ""
        }

        binding.root.doOnPreDraw {
            adapter.contentMaxWidth = binding.recyclerView.width
        }
    }

    private fun attachGestureHandlerToRecyclerViewIfNeeded() {
        if (!isBindingAvailable()) return
        if (!preferences.useGestureActions) return

        val context = requireContext()

        if (itemTouchHelper == null) {
            swipeActionCallback = LemmySwipeActionCallback(
                context,
                binding.recyclerView,
                onActionSelected = { action, vh ->
                    val postView = vh.itemView.tag as? PostView
                    if (postView != null) {
                        performPostAction(action.id, postView)
                        return@LemmySwipeActionCallback
                    }

                    val commentView = vh.itemView.getTag(R.id.comment_view) as? CommentView
                        ?: return@LemmySwipeActionCallback

                    when (action.id) {
                        CommentGestureAction.Upvote -> {
                            actionsViewModel.vote(commentView, dir = 1, toggle = true)
                        }

                        CommentGestureAction.Downvote -> {
                            actionsViewModel.vote(commentView, dir = -1, toggle = true)
                        }

                        CommentGestureAction.Bookmark -> {
                            actionsViewModel.saveComment(commentView.comment.id, true)
                        }

                        CommentGestureAction.Reply -> {
                            AddOrEditCommentFragment().apply {
                                arguments = AddOrEditCommentFragmentArgs(
                                    args.instance,
                                    commentView,
                                    null,
                                    null,
                                ).toBundle()
                            }.show(childFragmentManager, "asdf")
                        }

                        CommentGestureAction.CollapseOrExpand -> {
                            adapter?.toggleSection(vh.bindingAdapterPosition)
                        }
                    }
                },
                preferences.commentGestureSize,
            )
            itemTouchHelper = ItemTouchHelper(requireNotNull(swipeActionCallback))
        }

        swipeActionCallback?.apply {
            actions = preferences.getCommentSwipeActions(context)
            gestureSize = preferences.commentGestureSize
            postOnlyActions = preferences.getPostSwipeActions(context)
            postOnlyGestureSize = preferences.postGestureSize

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
                actionsViewModel.vote(postView, 1, toggle = true)
            }

            PostGestureAction.Downvote -> {
                actionsViewModel.vote(postView, -1, toggle = true)
            }

            PostGestureAction.Bookmark -> {
                actionsViewModel.savePost(postView.post.id, save = true)
            }

            PostGestureAction.Reply -> {
                if (accountManager.currentAccount.value == null) {
                    PreAuthDialogFragment.newInstance(R.id.action_add_comment)
                        .show(childFragmentManager, "asdf")
                    return
                }

                AddOrEditCommentFragment().apply {
                    arguments = AddOrEditCommentFragmentArgs(
                        viewModel.apiInstance,
                        null,
                        postView,
                        null,
                    ).toBundle()
                }.show(childFragmentManager, "asdf")
            }

            PostGestureAction.Hide -> {
                actionsViewModel.hidePost(postView.post.id)
            }

            PostGestureAction.MarkAsRead -> {
                actionsViewModel.onPostRead(postView, delayMs = 250)
            }
        }
    }

    override fun onResume() {
        super.onResume()

        if (args.isSinglePage) {
            requireMainActivity().apply {
                setupForFragment<PostFragment>()
                hideBottomNav(animate = true)
                lockUiOpenness = true
            }
        }

        runAfterLayout {
            adapter?.contentMaxWidth = binding.recyclerView.width

            attachGestureHandlerToRecyclerViewIfNeeded()
        }
    }

    override fun onPause() {
        if (args.isSinglePage) {
            requireMainActivity().apply {
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
                AddOrEditCommentFragment().apply {
                    arguments =
                        AddOrEditCommentFragmentArgs(
                            args.instance,
                            null,
                            postView.post,
                            null,
                        )
                            .toBundle()
                }.show(childFragmentManager, "asdf")
            }
            R.id.action_edit_comment -> {
                val postView = viewModel.postData.valueOrNull?.postView ?: return
                AddOrEditCommentFragment().apply {
                    arguments =
                        AddOrEditCommentFragmentArgs(
                            args.instance,
                            null,
                            postView.post,
                            null,
                        )
                            .toBundle()
                }.show(childFragmentManager, "asdf")
            }
        }
    }

    private fun forceRefresh() {
        viewModel.fetchPostData(args.postOrCommentRef(), force = true)
    }

    private fun onMainListingItemRetrieved(post: PostView) {
        post.getUrl(args.instance).let { url ->
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
            else -> {}
        }

        return _sortByMenu
    }

    override fun onPositiveClick(dialog: AlertDialogFragment, tag: String?) {
        when (tag) {
            CONFIRM_DELETE_COMMENT_TAG -> {
                val commentId = dialog.getExtra(EXTRA_COMMENT_ID)
                if (commentId != null) {
                    viewModel.deleteComment(PostRef(args.instance, args.id), commentId.toInt())
                }
            }
        }
    }

    override fun onNegativeClick(dialog: AlertDialogFragment, tag: String?) {
        // do nothing
    }

    private fun PostFragmentArgs.postOrCommentRef() =
        if (this.id > 0) {
            Either.Left(PostRef(this.instance, this.id))
        } else {
            Either.Right(CommentRef(this.instance, this.commentId))
        }
}
