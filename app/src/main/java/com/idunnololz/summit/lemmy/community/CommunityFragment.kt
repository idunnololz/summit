package com.idunnololz.summit.lemmy.community

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.core.view.doOnPreDraw
import androidx.fragment.app.viewModels
import androidx.navigation.findNavController
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.Adapter
import com.google.android.material.snackbar.Snackbar
import com.idunnololz.summit.R
import com.idunnololz.summit.accountUi.AccountsAndSettingsDialogFragment
import com.idunnololz.summit.accountUi.PreAuthDialogFragment
import com.idunnololz.summit.accountUi.SignInNavigator
import com.idunnololz.summit.alert.AlertDialogFragment
import com.idunnololz.summit.api.dto.PostView
import com.idunnololz.summit.api.utils.instance
import com.idunnololz.summit.databinding.FragmentCommunityBinding
import com.idunnololz.summit.history.HistoryManager
import com.idunnololz.summit.history.HistorySaveReason
import com.idunnololz.summit.lemmy.CommunityRef
import com.idunnololz.summit.lemmy.CommunitySortOrder
import com.idunnololz.summit.lemmy.CommunityViewState
import com.idunnololz.summit.lemmy.ContentTypeFilterTooAggressiveException
import com.idunnololz.summit.lemmy.FilterTooAggressiveException
import com.idunnololz.summit.lemmy.LoadNsfwCommunityWhenNsfwDisabled
import com.idunnololz.summit.lemmy.MoreActionsViewModel
import com.idunnololz.summit.lemmy.actions.LemmySwipeActionCallback
import com.idunnololz.summit.lemmy.comment.AddOrEditCommentFragment
import com.idunnololz.summit.lemmy.comment.AddOrEditCommentFragmentArgs
import com.idunnololz.summit.lemmy.createOrEditPost.CreateOrEditPostFragment
import com.idunnololz.summit.lemmy.createOrEditPost.CreateOrEditPostFragmentArgs
import com.idunnololz.summit.lemmy.getShortDesc
import com.idunnololz.summit.lemmy.idToSortOrder
import com.idunnololz.summit.lemmy.post.PostFragment
import com.idunnololz.summit.lemmy.postListView.PostListViewBuilder
import com.idunnololz.summit.lemmy.postListView.showMorePostOptions
import com.idunnololz.summit.lemmy.utils.getPostSwipeActions
import com.idunnololz.summit.lemmy.utils.installOnActionResultHandler
import com.idunnololz.summit.lemmy.utils.setupDecoratorsForPostList
import com.idunnololz.summit.main.LemmyAppBarController
import com.idunnololz.summit.main.MainFragment
import com.idunnololz.summit.offline.OfflineManager
import com.idunnololz.summit.preferences.PostGestureAction
import com.idunnololz.summit.preferences.Preferences
import com.idunnololz.summit.user.UserCommunitiesManager
import com.idunnololz.summit.util.BaseFragment
import com.idunnololz.summit.util.BottomMenu
import com.idunnololz.summit.util.SharedElementTransition
import com.idunnololz.summit.util.StatefulData
import com.idunnololz.summit.util.Utils
import com.idunnololz.summit.util.ext.getDimen
import com.idunnololz.summit.util.ext.navigateSafe
import com.idunnololz.summit.util.ext.showAllowingStateLoss
import com.idunnololz.summit.util.getParcelableCompat
import com.idunnololz.summit.util.showBottomMenuForLink
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class CommunityFragment :
    BaseFragment<FragmentCommunityBinding>(),
    SignInNavigator,
    AlertDialogFragment.AlertDialogFragmentListener {

    companion object {
        private const val TAG = "CommunityFragment"
    }

    private val args: CommunityFragmentArgs by navArgs()

    private val viewModel: CommunityViewModel by viewModels()
    val actionsViewModel: MoreActionsViewModel by viewModels()

    private var adapter: ListingItemAdapter? = null

    private var shouldScrollToTopAfterFresh = false

    @Inject
    lateinit var historyManager: HistoryManager

    @Inject
    lateinit var offlineManager: OfflineManager

    @Inject
    lateinit var userCommunitiesManager: UserCommunitiesManager

    @Inject
    lateinit var postListViewBuilder: PostListViewBuilder

    private var viewPagerController: ViewPagerController? = null

    private var isCustomAppBarExpandedPercent = 0f

    private lateinit var lemmyAppBarController: LemmyAppBarController

    private var swipeActionCallback: LemmySwipeActionCallback? = null
    private var itemTouchHelper: ItemTouchHelper? = null

    private val onBackPressedHandler = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            if (!viewModel.infinity && viewModel.currentPageIndex.value != 0) {
                viewModel.fetchPrevPage()
            } else {
                isEnabled = false
                requireActivity().onBackPressed()
            }
        }
    }

    private val _sortByMenu: BottomMenu by lazy {
        BottomMenu(requireContext()).apply {
            addItem(R.id.sort_order_active, R.string.sort_order_active)
            addItem(R.id.sort_order_hot, R.string.sort_order_hot)
            addItem(R.id.sort_order_top, R.string.sort_order_top, R.drawable.baseline_chevron_right_24)
            addItem(R.id.sort_order_new, R.string.sort_order_new)
            addItem(R.id.sort_order_old, R.string.sort_order_old)
            addItem(R.id.sort_order_most_comments, R.string.sort_order_most_comments)
            addItem(R.id.sort_order_new_comments, R.string.sort_order_new_comments)
            setTitle(R.string.sort_by)

            setOnMenuItemClickListener { menuItem ->
                when (menuItem.id) {
                    R.id.sort_order_top ->
                        getMainActivity()?.showBottomMenu(getSortByTopMenu())
                    else ->
                        idToSortOrder(menuItem.id)?.let {
                            viewModel.setSortOrder(it)
                        }
                }
            }
        }
    }

    private val _sortByTopMenu: BottomMenu by lazy {
        BottomMenu(requireContext()).apply {
            addItem(R.id.sort_order_top_last_hour, R.string.time_frame_last_hour)
            addItem(R.id.sort_order_top_last_six_hour, getString(R.string.time_frame_last_hours_format, "6"))
            addItem(R.id.sort_order_top_last_twelve_hour, getString(R.string.time_frame_last_hours_format, "12"))
            addItem(R.id.sort_order_top_day, R.string.time_frame_today)
            addItem(R.id.sort_order_top_week, R.string.time_frame_this_week)
            addItem(R.id.sort_order_top_month, R.string.time_frame_this_month)
            addItem(R.id.sort_order_top_last_three_month, getString(R.string.time_frame_last_months_format, "3"))
            addItem(R.id.sort_order_top_last_six_month, getString(R.string.time_frame_last_months_format, "6"))
            addItem(R.id.sort_order_top_last_nine_month, getString(R.string.time_frame_last_months_format, "9"))
            addItem(R.id.sort_order_top_year, R.string.time_frame_this_year)
            addItem(R.id.sort_order_top_all_time, R.string.time_frame_all_time)
            setTitle(R.string.sort_by_top)

            setOnMenuItemClickListener { menuItem ->
                idToSortOrder(menuItem.id)?.let {
                    viewModel.setSortOrder(it)
                }
            }
        }
    }

    private val _layoutSelectorMenu: BottomMenu by lazy {
        BottomMenu(requireContext()).apply {
            addItemWithIcon(R.id.layout_list, R.string.list, R.drawable.baseline_view_list_24)
            addItemWithIcon(R.id.layout_large_list, R.string.large_list, R.drawable.baseline_view_list_24)
            addItemWithIcon(R.id.layout_compact, R.string.compact, R.drawable.baseline_list_24)
            addItemWithIcon(R.id.layout_card, R.string.card, R.drawable.baseline_article_24)
            addItemWithIcon(R.id.layout_card2, R.string.card2, R.drawable.baseline_article_24)
            addItemWithIcon(R.id.layout_card3, R.string.card3, R.drawable.baseline_article_24)
            addItemWithIcon(R.id.layout_full, R.string.full, R.drawable.baseline_view_day_24)
            setTitle(R.string.layout)

            setOnMenuItemClickListener { menuItem ->
                when (menuItem.id) {
                    R.id.layout_compact ->
                        preferences.setPostsLayout(CommunityLayout.Compact)
                    R.id.layout_list ->
                        preferences.setPostsLayout(CommunityLayout.List)
                    R.id.layout_large_list ->
                        preferences.setPostsLayout(CommunityLayout.LargeList)
                    R.id.layout_card ->
                        preferences.setPostsLayout(CommunityLayout.Card)
                    R.id.layout_card2 ->
                        preferences.setPostsLayout(CommunityLayout.Card2)
                    R.id.layout_card3 ->
                        preferences.setPostsLayout(CommunityLayout.Card3)
                    R.id.layout_full ->
                        preferences.setPostsLayout(CommunityLayout.Full)
                }
                onSelectedLayoutChanged()
            }
        }
    }

    @Inject
    lateinit var preferences: Preferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewModel.setTag(parentFragment?.tag)

        if (savedInstanceState == null) {
            requireMainActivity().apply {
                setupForFragment<CommunityFragment>()
            }
        }

        val context = requireContext()
        if (adapter == null) {
            adapter = ListingItemAdapter(
                postListViewBuilder = postListViewBuilder,
                context = context,
                viewModel.postListEngine,
                onNextClick = {
                    viewModel.fetchNextPage(clearPagePosition = true)
                },
                onPrevClick = {
                    viewModel.fetchPrevPage()
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
                        .setNegativeButton(R.string.go_to_account_instance)
                        .createAndShow(childFragmentManager, "onInstanceMismatch")
                },
                onImageClick = { postView, sharedElementView, url ->
                    getMainActivity()?.openImage(
                        sharedElement = sharedElementView,
                        appBar = binding.customAppBar.root,
                        title = postView.post.name,
                        url = url,
                        mimeType = null,
                    )
                    viewModel.onPostRead(postView)
                },
                onVideoClick = { url, videoType, state ->
                    getMainActivity()?.openVideo(url, videoType, state)
                },
                onPageClick = {
                    getMainActivity()?.launchPage(it)
                },
                onItemClick = {
                        instance,
                        id,
                        currentCommunity,
                        post,
                        jumpToComments,
                        reveal,
                        videoState, ->

                    viewPagerController?.openPost(
                        instance = instance,
                        id = id,
                        reveal = reveal,
                        post = post,
                        jumpToComments = jumpToComments,
                        currentCommunity = currentCommunity,
                        videoState = videoState,
                    )
                },
                onShowMoreActions = {
                    showMorePostOptions(viewModel.apiInstance, it, actionsViewModel)
                },
                onPostRead = { postView ->
                    viewModel.onPostRead(postView)
                },
                onLoadPage = {
                    viewModel.fetchPage(it)
                },
                onLinkLongClick = { url, text ->
                    getMainActivity()?.showBottomMenuForLink(url, text)
                },
            ).apply {
                stateRestorationPolicy = Adapter.StateRestorationPolicy.PREVENT_WHEN_EMPTY

                updateWithPreferences(preferences)
            }
            onSelectedLayoutChanged()
        }

        viewModel.changeCommunity(args.communityRef)

        if (savedInstanceState != null) {
            restoreState(CommunityViewState.restoreFromBundle(savedInstanceState), reload = false)
        }

        sharedElementEnterTransition = SharedElementTransition()
        sharedElementReturnTransition = SharedElementTransition()

        requireActivity().supportFragmentManager.setFragmentResultListener(
            CreateOrEditPostFragment.REQUEST_KEY,
            this,
        ) { _, bundle ->
            val result = bundle.getParcelableCompat<PostView>(
                CreateOrEditPostFragment.REQUEST_KEY_RESULT,
            )

            if (result != null) {
                viewModel.fetchCurrentPage(force = true)
                viewPagerController?.openPost(
                    instance = result.instance,
                    id = result.post.id,
                    reveal = false,
                    post = result,
                    jumpToComments = false,
                    currentCommunity = args.communityRef,
                    videoState = null,
                )
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        postponeEnterTransition()

        setBinding(FragmentCommunityBinding.inflate(inflater, container, false))

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.updateInfinity()
        binding.loadingView.hideAll()

        lemmyAppBarController = LemmyAppBarController(requireMainActivity(), binding.customAppBar)

        viewModel.defaultCommunity.observe(viewLifecycleOwner) {
            lemmyAppBarController.setDefaultCommunity(it)
        }
        viewModel.currentAccount.observe(viewLifecycleOwner) {
            lemmyAppBarController.onAccountChanged(it)
        }
        viewModel.sortOrder.observe(viewLifecycleOwner) {
            lemmyAppBarController.setSortOrder(it)
        }

        installOnActionResultHandler(
            actionsViewModel = actionsViewModel,
            snackbarContainer = binding.fabSnackbarCoordinatorLayout,
        )

        requireMainActivity().apply {
            insetViewExceptBottomAutomaticallyByMargins(viewLifecycleOwner, binding.customAppBar.customActionBar)

            binding.customAppBar.root.addOnOffsetChangedListener { _, verticalOffset ->
                val percentShown = -verticalOffset.toFloat() / binding.customAppBar.root.height

                bottomNavViewOffset.value =
                    (percentShown * getBottomNavHeight()).toInt()

                isCustomAppBarExpandedPercent = 1f - percentShown

                updateFabState()
            }
            lemmyAppBarController.setup(
                communitySelectedListener = { controller, communityRef ->
                    val action = CommunityFragmentDirections.actionSubredditFragmentSwitchSubreddit(
                        communityRef = communityRef,
                        tab = args.tab,
                    )
                    findNavController().navigate(action)
                    Utils.hideKeyboard(activity)
                    controller.hide()
                },
                onAccountClick = {
                    AccountsAndSettingsDialogFragment.newInstance()
                        .showAllowingStateLoss(childFragmentManager, "AccountsDialogFragment")
                },
                onSortOrderClick = {
                    getMainActivity()?.showBottomMenu(getSortByMenu())
                },
            )
        }

        view.doOnPreDraw {
            adapter?.contentMaxWidth = binding.recyclerView.measuredWidth
            adapter?.contentPreferredHeight = binding.recyclerView.measuredHeight
        }

        runOnReady {
            onReady()
        }

        binding.fab.setOnClickListener a@{
            showOverflowMenu()
        }

        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, onBackPressedHandler)

        viewPagerController = ViewPagerController(
            this,
            binding.viewPager,
            childFragmentManager,
            viewModel,
        ) {
            if (it == 0) {
                val lastSelectedPost = viewModel.lastSelectedPost
                if (lastSelectedPost != null) {
                    // We came from a post...
                    adapter?.highlightPost(lastSelectedPost)
                    viewModel.lastSelectedPost = null
                }
            } else {
                val lastSelectedPost = viewModel.lastSelectedPost
                if (lastSelectedPost != null) {
                    adapter?.highlightPostForever(lastSelectedPost)
                }
            }
        }.apply {
            init()
        }
        binding.viewPager.disableLeftSwipe = true
    }

    fun closePost(postFragment: PostFragment) {
        viewPagerController?.closePost(postFragment)
    }

    fun isPristineFirstPage(): Boolean {
        if (!isBindingAvailable()) {
            return false
        }

        val position = (binding.recyclerView.layoutManager as? LinearLayoutManager)
            ?.findFirstCompletelyVisibleItemPosition()

        return position == 0 && viewModel.currentPageIndex.value == 0
    }

    fun onPostUpdated() {
        viewModel.fetchCurrentPage(force = true)
    }

    private fun onReady() {
        if (!isBindingAvailable()) return

        val view = binding.root
        val context = requireContext()

        checkNotNull(view.findNavController())

        requireMainActivity().apply {
            insetViewExceptTopAutomaticallyByPaddingAndNavUi(
                viewLifecycleOwner,
                binding.fabSnackbarCoordinatorLayout,
            )
            insetViewExceptTopAutomaticallyByPaddingAndNavUi(
                viewLifecycleOwner,
                binding.recyclerView,
                context.getDimen(R.dimen.footer_spacer_height),
            )
        }

        (parentFragment?.parentFragment as? MainFragment)?.updateCommunityInfoPane(
            requireNotNull(viewModel.currentCommunityRef.value),
        )

        binding.swipeRefreshLayout.setOnRefreshListener {
            shouldScrollToTopAfterFresh = true
            viewModel.fetchCurrentPage(true, resetHideRead = true, clearPages = true)
            binding.recyclerView.scrollToPosition(0)
        }
        binding.loadingView.setOnRefreshClickListener {
            viewModel.fetchCurrentPage(true)
        }

        val layoutManager = LinearLayoutManager(context)
        adapter?.viewLifecycleOwner = viewLifecycleOwner
        binding.recyclerView.adapter = adapter
        binding.recyclerView.setHasFixedSize(true)
        binding.recyclerView.layoutManager = layoutManager

        if (preferences.markPostsAsReadOnScroll) {
            binding.recyclerView.addOnScrollListener(
                object : RecyclerView.OnScrollListener() {
                    override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                        super.onScrolled(recyclerView, dx, dy)

                        val range = layoutManager.findFirstCompletelyVisibleItemPosition()..layoutManager.findLastCompletelyVisibleItemPosition()

                        range.forEach {
                            adapter?.seenItemPositions?.add(it)
                        }
                    }
                },
            )
        }

        updateDecoratorAndGestureHandler(binding.recyclerView)

        binding.fastScroller.setRecyclerView(binding.recyclerView)

        fun fetchPageIfLoadItem(position: Int) {
            (adapter?.items?.getOrNull(position) as? Item.AutoLoadItem)
                ?.pageToLoad
                ?.let {
                    viewModel.fetchPage(it)
                }
        }

        binding.recyclerView.addOnScrollListener(
            object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    super.onScrolled(recyclerView, dx, dy)

                    val adapter = adapter ?: return
                    val firstPos = layoutManager.findFirstVisibleItemPosition()
                    val lastPos = layoutManager.findLastVisibleItemPosition()
                    (adapter.items.getOrNull(firstPos) as? Item.PostItem)
                        ?.pageIndex
                        ?.let { pageIndex ->
                            if (firstPos != 0 && lastPos == adapter.itemCount - 1) {
                                // firstPos != 0 - ensures that the page is scrollable even
                                viewModel.setPagePositionAtBottom(pageIndex)
                            } else {
                                val firstView = layoutManager.findViewByPosition(firstPos)
                                viewModel.setPagePosition(pageIndex, firstPos, firstView?.top ?: 0)
                            }
                        }

                    if (viewModel.infinity) {
                        fetchPageIfLoadItem(firstPos)
                        fetchPageIfLoadItem(firstPos - 1)
                        fetchPageIfLoadItem(lastPos)
                        fetchPageIfLoadItem(lastPos + 1)
                    }

                    viewModel.postListEngine.updateViewingPosition(firstPos, lastPos)
                }
            },
        )

        binding.rootView.post {
            if (!isBindingAvailable()) return@post
            scheduleStartPostponedTransition(binding.rootView)
        }

        viewModel.loadedPostsData.observe(viewLifecycleOwner) a@{
            when (it) {
                is StatefulData.Error -> {
                    binding.swipeRefreshLayout.isRefreshing = false

                    if (it.error is LoadNsfwCommunityWhenNsfwDisabled) {
                        binding.recyclerView.visibility = View.GONE
                        binding.loadingView.showErrorText(R.string.error_cannot_load_nsfw_community_when_nsfw_posts_are_hidden)
                    } else if (it.error is FilterTooAggressiveException) {
                        binding.recyclerView.visibility = View.GONE
                        binding.loadingView.showErrorText(R.string.error_filter_too_aggressive)
                    } else if (it.error is ContentTypeFilterTooAggressiveException) {
                        binding.recyclerView.visibility = View.GONE
                        binding.loadingView.showErrorText(R.string.error_content_type_filter_too_aggressive)
                    } else if (viewModel.infinity) {
                        binding.loadingView.hideAll()
                        adapter?.onItemsChanged()
                    } else {
                        binding.recyclerView.visibility = View.GONE
                        binding.loadingView.showDefaultErrorMessageFor(it.error)
                    }
                }
                is StatefulData.Loading -> {
                    binding.loadingView.showProgressBar()
                }
                is StatefulData.NotStarted -> {}
                is StatefulData.Success -> {
                    val adapter = adapter ?: return@a

                    if (it.data.isReadPostUpdate) {
                        adapter.onItemsChanged(animate = false)
                    } else {
                        adapter.seenItemPositions.clear()
                        adapter.onItemsChanged()

                        if (shouldScrollToTopAfterFresh) {
                            shouldScrollToTopAfterFresh = false
                            binding.recyclerView.scrollToPosition(0)
                        } else {
                            val biggestPageIndex = viewModel.postListEngine.biggestPageIndex
                            if (biggestPageIndex != null && !viewModel.infinity) {
                                val pagePosition = viewModel.getPagePosition(
                                    biggestPageIndex,
                                )
                                if (pagePosition.isAtBottom) {
                                    (binding.recyclerView.layoutManager as LinearLayoutManager)
                                        .scrollToPositionWithOffset(adapter.itemCount - 1, 0)
                                } else if (pagePosition.itemIndex != 0 || pagePosition.offset != 0) {
                                    (binding.recyclerView.layoutManager as LinearLayoutManager)
                                        .scrollToPositionWithOffset(
                                            pagePosition.itemIndex,
                                            pagePosition.offset,
                                        )
                                } else {
                                    binding.recyclerView.scrollToPosition(0)
                                    binding.customAppBar.root.setExpanded(true)
                                }
                            }
                        }

                        binding.swipeRefreshLayout.isRefreshing = false
                        binding.recyclerView.visibility = View.VISIBLE
                        binding.loadingView.hideAll()

                        if (viewModel.postListEngine.items.isEmpty()) {
                            binding.loadingView.showErrorText(R.string.no_posts)
                        }
                    }
                }
            }
        }

        actionsViewModel.blockCommunityResult.observe(viewLifecycleOwner) {
            if (it is StatefulData.Success) {
                actionsViewModel.blockCommunityResult.setIdle()
                viewModel.onBlockSettingsChanged()
            }
        }
        actionsViewModel.blockPersonResult.observe(viewLifecycleOwner) {
            if (it is StatefulData.Success) {
                actionsViewModel.blockPersonResult.setIdle()
                viewModel.onBlockSettingsChanged()
            }
        }

        if (adapter?.items.isNullOrEmpty()) {
            viewModel.fetchInitialPage()
        }

        // try to restore state...
        viewPagerController?.onPageSelected()
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
                    val postView = vh.itemView.getTag(R.id.post_view) as? PostView
                        ?: return@LemmySwipeActionCallback

                    when (action.id) {
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
                            viewPagerController?.openPost(
                                instance = viewModel.apiInstance,
                                id = postView.post.id,
                                reveal = false,
                                post = postView,
                                jumpToComments = false,
                                currentCommunity = viewModel.currentCommunityRef.value,
                                videoState = null,
                            )

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
                            viewModel.hidePost(postView.post.id)
                        }

                        PostGestureAction.MarkAsRead -> {
                            viewModel.onPostRead(postView, delayMs = 250)
                        }
                    }
                },
                preferences.postGestureSize,
            )
            itemTouchHelper = ItemTouchHelper(requireNotNull(swipeActionCallback))
        }

        swipeActionCallback?.updatePostSwipeActions()

        itemTouchHelper?.attachToRecyclerView(binding.recyclerView)
    }

    private fun LemmySwipeActionCallback.updatePostSwipeActions() {
        if (!isBindingAvailable()) return
        val context = requireContext()
        this.actions = preferences.getPostSwipeActions(context)
    }

    override fun onResume() {
        super.onResume()

        runOnReady {
            val customAppBarController = lemmyAppBarController

            viewModel.currentCommunityRef.observe(viewLifecycleOwner) {
                customAppBarController.setCommunity(it)

                val tab = args.tab
                if (tab != null) {
                    viewModel.updateTab(tab, it)
                }
            }

            customAppBarController.setIsInfinity(viewModel.infinity)
            if (viewModel.infinity) {
                onBackPressedHandler.isEnabled = false
            } else {
                viewModel.currentPageIndex.observe(viewLifecycleOwner) { currentPageIndex ->
                    Log.d(TAG, "Current page: $currentPageIndex")
                    customAppBarController.setPageIndex(currentPageIndex) { pageIndex ->
                        viewModel.fetchPage(pageIndex)
                    }

                    onBackPressedHandler.isEnabled = currentPageIndex != 0
                }
            }

            if (binding.viewPager.currentItem == 0) {
                getMainActivity()?.setNavUiOpenness(0f)
            }

            adapter?.updateWithPreferences(preferences)

            onSelectedLayoutChanged()

            viewModel.recheckPreferences()
        }

        runAfterLayout {
            adapter?.contentMaxWidth = binding.recyclerView.width
        }
    }

    private fun updateFabState() {
        if (isCustomAppBarExpandedPercent > 0.8f) {
            binding.fab.show()
        } else {
            binding.fab.hide()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        viewModel.createState()?.writeToBundle(outState)
        super.onSaveInstanceState(outState)
    }

    override fun onPause() {
        updateHistory()

        super.onPause()
    }

    private fun updateHistory() {
        if (!isBindingAvailable()) return

        val context = requireContext()

        val viewState = viewModel.createState()
        if (viewState != null) {
            historyManager.recordCommunityState(
                tabId = 0,
                saveReason = HistorySaveReason.LEAVE_SCREEN,
                state = viewState,
                shortDesc = viewState.getShortDesc(context),
            )
        }
    }

    override fun onDestroyView() {
        Log.d(TAG, "onDestroyView()")

        itemTouchHelper?.attachToRecyclerView(null) // detach the itemTouchHelper

        super.onDestroyView()
    }

    private fun restoreState(state: CommunityViewState?, reload: Boolean) {
        viewModel.restoreFromState(state ?: return)
        if (reload) {
            viewModel.fetchCurrentPage()
        }
    }

    private fun getSortByMenu(): BottomMenu {
        when (viewModel.getCurrentSortOrder()) {
            CommunitySortOrder.Active -> _sortByMenu.setChecked(R.id.sort_order_active)
            CommunitySortOrder.Hot -> _sortByMenu.setChecked(R.id.sort_order_hot)
            CommunitySortOrder.New -> _sortByMenu.setChecked(R.id.sort_order_new)
            is CommunitySortOrder.TopOrder -> _sortByMenu.setChecked(R.id.sort_order_top)
            CommunitySortOrder.MostComments -> _sortByMenu.setChecked(R.id.sort_order_most_comments)
            CommunitySortOrder.NewComments -> _sortByMenu.setChecked(R.id.sort_order_new_comments)
            CommunitySortOrder.Old -> _sortByMenu.setChecked(R.id.sort_order_old)
        }

        return _sortByMenu
    }
    private fun getSortByTopMenu(): BottomMenu {
        when (val order = viewModel.getCurrentSortOrder()) {
            is CommunitySortOrder.TopOrder -> {
                when (order.timeFrame) {
                    CommunitySortOrder.TimeFrame.Today ->
                        _sortByTopMenu.setChecked(R.id.sort_order_top_day)
                    CommunitySortOrder.TimeFrame.ThisWeek ->
                        _sortByTopMenu.setChecked(R.id.sort_order_top_week)
                    CommunitySortOrder.TimeFrame.ThisMonth ->
                        _sortByTopMenu.setChecked(R.id.sort_order_top_month)
                    CommunitySortOrder.TimeFrame.ThisYear ->
                        _sortByTopMenu.setChecked(R.id.sort_order_top_year)
                    CommunitySortOrder.TimeFrame.AllTime ->
                        _sortByTopMenu.setChecked(R.id.sort_order_top_all_time)
                    CommunitySortOrder.TimeFrame.LastHour ->
                        _sortByTopMenu.setChecked(R.id.sort_order_top_last_hour)
                    CommunitySortOrder.TimeFrame.LastSixHour ->
                        _sortByTopMenu.setChecked(R.id.sort_order_top_last_six_hour)
                    CommunitySortOrder.TimeFrame.LastTwelveHour ->
                        _sortByTopMenu.setChecked(R.id.sort_order_top_last_twelve_hour)
                    CommunitySortOrder.TimeFrame.LastThreeMonth ->
                        _sortByTopMenu.setChecked(R.id.sort_order_top_last_three_month)
                    CommunitySortOrder.TimeFrame.LastSixMonth ->
                        _sortByTopMenu.setChecked(R.id.sort_order_top_last_six_month)
                    CommunitySortOrder.TimeFrame.LastNineMonth ->
                        _sortByTopMenu.setChecked(R.id.sort_order_top_last_nine_month)
                }
            }
            else -> {}
        }

        return _sortByTopMenu
    }
    private fun getLayoutMenu(): BottomMenu {
        when (preferences.getPostsLayout()) {
            CommunityLayout.Compact ->
                _layoutSelectorMenu.setChecked(R.id.layout_compact)
            CommunityLayout.List ->
                _layoutSelectorMenu.setChecked(R.id.layout_list)
            CommunityLayout.LargeList ->
                _layoutSelectorMenu.setChecked(R.id.layout_large_list)
            CommunityLayout.Card ->
                _layoutSelectorMenu.setChecked(R.id.layout_card)
            CommunityLayout.Card2 ->
                _layoutSelectorMenu.setChecked(R.id.layout_card2)
            CommunityLayout.Card3 ->
                _layoutSelectorMenu.setChecked(R.id.layout_card3)
            CommunityLayout.Full ->
                _layoutSelectorMenu.setChecked(R.id.layout_full)
        }

        return _layoutSelectorMenu
    }

    private fun showOverflowMenu() {
        val context = context ?: return

        val currentCommunityRef = requireNotNull(viewModel.currentCommunityRef.value)
        val currentDefaultPage = preferences.getDefaultPage()
        val isBookmarked = userCommunitiesManager.isCommunityBookmarked(currentCommunityRef)
        val isCurrentPageDefault = currentCommunityRef == currentDefaultPage

        val bottomMenu = BottomMenu(context).apply {
            val currentCommunity = viewModel.currentCommunityRef.value
            var communityName: String? = null
            when (currentCommunity) {
                is CommunityRef.All -> {}
                is CommunityRef.CommunityRefByName -> {
                    communityName = currentCommunity.name
                    addItemWithIcon(R.id.create_post, R.string.create_post, R.drawable.baseline_add_24)
                }
                is CommunityRef.Local -> {}
                is CommunityRef.Subscribed -> {}
                null -> {}
            }

            addItemWithIcon(R.id.share, R.string.share, R.drawable.baseline_share_24)
            addItemWithIcon(R.id.hide_read, R.string.hide_read, R.drawable.baseline_clear_all_24)

            addItemWithIcon(R.id.sort, R.string.sort, R.drawable.baseline_sort_24)
            addItemWithIcon(R.id.layout, R.string.layout, R.drawable.baseline_view_comfy_24)
            addItemWithIcon(
                id = R.id.community_info,
                title = R.string.community_info,
                icon = R.drawable.ic_subreddit_default,
            )
            addItemWithIcon(
                id = R.id.my_communities,
                title = R.string.my_communities,
                icon = R.drawable.baseline_subscriptions_24,
            )
            addItemWithIcon(
                id = R.id.browse_communities,
                title = R.string.browse_communities,
                icon = R.drawable.baseline_dashboard_24,
            )
            addItemWithIcon(
                id = R.id.settings,
                title = R.string.settings,
                icon = R.drawable.baseline_settings_24,
            )

            if (isCurrentPageDefault) {
            } else {
                if (isBookmarked) {
                    addItemWithIcon(
                        id = R.id.toggle_bookmark,
                        title = R.string.remove_bookmark,
                        icon = R.drawable.baseline_bookmark_remove_24,
                    )
                } else {
                    addItemWithIcon(
                        id = R.id.toggle_bookmark,
                        title = R.string.bookmark_community,
                        icon = R.drawable.baseline_bookmark_add_24,
                    )
                }

                addItemWithIcon(R.id.set_as_default, R.string.set_as_home_page, R.drawable.baseline_home_24)
            }

            setOnMenuItemClickListener { menuItem ->
                when (menuItem.id) {
                    R.id.create_post -> {
                        CreateOrEditPostFragment()
                            .apply {
                                arguments = CreateOrEditPostFragmentArgs(
                                    instance = viewModel.communityInstance,
                                    communityName = communityName,
                                ).toBundle()
                            }
                            .showAllowingStateLoss(childFragmentManager, "CreateOrEditPostFragment")
                    }
                    R.id.share -> {
                        val sendIntent: Intent = Intent().apply {
                            action = Intent.ACTION_SEND
                            putExtra(
                                Intent.EXTRA_TEXT,
                                viewModel.getSharedLinkForCurrentPage(),
                            )
                            type = "text/plain"
                        }

                        val shareIntent = Intent.createChooser(sendIntent, null)
                        startActivity(shareIntent)
                    }
                    R.id.hide_read -> {
                        val anchors = mutableSetOf<Int>()
                        val range = (binding.recyclerView.layoutManager as? LinearLayoutManager)?.let {
                            it.findFirstCompletelyVisibleItemPosition()..(adapter?.items?.size ?: it.findLastVisibleItemPosition())
                        }
                        range?.mapNotNullTo(anchors) {
                            (adapter?.items?.getOrNull(it) as? Item.PostItem)?.postView?.post?.id
                        }
                        viewModel.onHideRead(anchors)
                    }

                    R.id.sort -> {
                        getMainActivity()?.showBottomMenu(getSortByMenu())
                    }

                    R.id.set_as_default -> {
                        viewModel.setDefaultPage(currentCommunityRef)

                        Snackbar.make(
                            requireMainActivity().getSnackbarContainer(),
                            R.string.home_page_set,
                            Snackbar.LENGTH_LONG,
                        ).show()
                    }
                    R.id.layout -> {
                        getMainActivity()?.showBottomMenu(getLayoutMenu())
                    }

                    R.id.community_info -> {
                        getMainActivity()?.showCommunityInfo(currentCommunityRef)
                    }

                    R.id.my_communities -> {
                        (parentFragment?.parentFragment as? MainFragment)?.expandStartPane()
                    }

                    R.id.toggle_bookmark -> {
                        if (isBookmarked) {
                            userCommunitiesManager.removeCommunity(currentCommunityRef)
                        } else {
                            userCommunitiesManager.addUserCommunity(
                                currentCommunityRef,
                                viewModel.postListEngine.getCommunityIcon(),
                            )
                        }
                    }
                    R.id.browse_communities -> {
                        lemmyAppBarController.showCommunitySelector()
                    }
                    R.id.settings -> {
                        requireMainActivity().openSettings()
                    }
                }
            }
        }

        getMainActivity()?.showBottomMenu(bottomMenu, expandFully = false)
    }

    override fun navigateToSignInScreen() {
        val direction = CommunityFragmentDirections.actionCommunityFragmentToLogin()
        findNavController().navigateSafe(direction)
    }

    override fun proceedAnyways(tag: Int) {}

    override fun onPositiveClick(dialog: AlertDialogFragment, tag: String?) {
    }

    override fun onNegativeClick(dialog: AlertDialogFragment, tag: String?) {
        if (tag == "onInstanceMismatch") {
            dialog.dismiss()
            viewModel.resetToAccountInstance()
        }
    }

    private fun onSelectedLayoutChanged() {
        val newPostUiConfig = preferences.getPostInListUiConfig()
        val didUiConfigChange = postListViewBuilder.postUiConfig != newPostUiConfig
        val didLayoutChange = adapter?.layout != preferences.getPostsLayout()

        if (didLayoutChange) {
            if (isBindingAvailable()) {
                updateDecoratorAndGestureHandler(binding.recyclerView)
            }
        }

        if (didUiConfigChange) {
            postListViewBuilder.postUiConfig = newPostUiConfig
        }

        if (didLayoutChange || didUiConfigChange) {
            adapter?.layout = preferences.getPostsLayout()
        }
    }

    private fun updateDecoratorAndGestureHandler(recyclerView: RecyclerView) {
        // We need to set the gesture and decorator together
        // Setting the decorator removes the gesture item decorator
        // So we need to add it back right after

        recyclerView.setupDecoratorsForPostList(preferences)

        // Detach before attaching or else attach will no-op
        itemTouchHelper?.attachToRecyclerView(null)
        attachGestureHandlerToRecyclerViewIfNeeded()
    }
}
