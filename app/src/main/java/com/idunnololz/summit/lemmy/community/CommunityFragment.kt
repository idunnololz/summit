package com.idunnololz.summit.lemmy.community

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.viewModels
import androidx.navigation.findNavController
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.Adapter
import androidx.recyclerview.widget.RecyclerView.SCROLL_STATE_IDLE
import arrow.core.Either
import com.discord.panels.OverlappingPanelsLayout
import com.google.android.material.snackbar.Snackbar
import com.idunnololz.summit.R
import com.idunnololz.summit.account.info.AccountInfoManager
import com.idunnololz.summit.accountUi.AccountsAndSettingsDialogFragment
import com.idunnololz.summit.accountUi.PreAuthDialogFragment
import com.idunnololz.summit.accountUi.SignInNavigator
import com.idunnololz.summit.alert.AlertDialogFragment
import com.idunnololz.summit.api.dto.PostId
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
import com.idunnololz.summit.lemmy.MultiCommunityException
import com.idunnololz.summit.lemmy.actions.LemmySwipeActionCallback
import com.idunnololz.summit.lemmy.comment.AddOrEditCommentFragment
import com.idunnololz.summit.lemmy.createOrEditPost.CreateOrEditPostFragment
import com.idunnololz.summit.lemmy.createOrEditPost.CreateOrEditPostFragmentArgs
import com.idunnololz.summit.lemmy.getShortDesc
import com.idunnololz.summit.lemmy.idToSortOrder
import com.idunnololz.summit.lemmy.instancePicker.InstancePickerDialogFragment
import com.idunnololz.summit.lemmy.multicommunity.MultiCommunityEditorDialogFragment
import com.idunnololz.summit.lemmy.post.PostFragment
import com.idunnololz.summit.lemmy.postListView.PostListViewBuilder
import com.idunnololz.summit.lemmy.postListView.showMorePostOptions
import com.idunnololz.summit.lemmy.utils.getPostSwipeActions
import com.idunnololz.summit.lemmy.utils.installOnActionResultHandler
import com.idunnololz.summit.lemmy.utils.setup
import com.idunnololz.summit.lemmy.utils.setupDecoratorsForPostList
import com.idunnololz.summit.lemmy.utils.showMoreVideoOptions
import com.idunnololz.summit.links.onLinkClick
import com.idunnololz.summit.main.LemmyAppBarController
import com.idunnololz.summit.main.MainFragment
import com.idunnololz.summit.offline.OfflineManager
import com.idunnololz.summit.preferences.PostGestureAction
import com.idunnololz.summit.preferences.Preferences
import com.idunnololz.summit.preferences.perCommunity.PerCommunityPreferences
import com.idunnololz.summit.settings.navigation.NavBarDestinations
import com.idunnololz.summit.user.UserCommunitiesManager
import com.idunnololz.summit.util.BaseFragment
import com.idunnololz.summit.util.BottomMenu
import com.idunnololz.summit.util.SharedElementTransition
import com.idunnololz.summit.util.StatefulData
import com.idunnololz.summit.util.Utils
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

    @Inject
    lateinit var historyManager: HistoryManager

    @Inject
    lateinit var offlineManager: OfflineManager

    @Inject
    lateinit var userCommunitiesManager: UserCommunitiesManager

    @Inject
    lateinit var postListViewBuilder: PostListViewBuilder

    @Inject
    lateinit var accountInfoManager: AccountInfoManager

    @Inject
    lateinit var perCommunityPreferences: PerCommunityPreferences

    lateinit var preferences: Preferences

    private var slidingPaneController: SlidingPaneController? = null

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
            addItem(R.id.sort_order_controversial, R.string.sort_order_controversial)
            addItem(R.id.sort_order_scaled, R.string.sort_order_scaled)
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
        makeLayoutSelectorMenu {
            preferences.setPostsLayout(it)
            onSelectedLayoutChanged()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        preferences = viewModel.preferences

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
                onVideoLongClickListener = { url ->
                    showMoreVideoOptions(url, actionsViewModel)
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

                    slidingPaneController?.openPost(
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
                    showMorePostOptions(
                        instance = viewModel.apiInstance,
                        postView = it,
                        actionsViewModel = actionsViewModel,
                        fragmentManager = childFragmentManager,
                    )
                },
                onPostRead = { postView ->
                    viewModel.onPostRead(postView)
                },
                onLoadPage = {
                    viewModel.fetchPage(it)
                },
                onLinkClick = { url, text, linkType ->
                    onLinkClick(url, text, linkType)
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

        with(childFragmentManager) {
            setFragmentResultListener(
                CreateOrEditPostFragment.REQUEST_KEY,
                this@CommunityFragment,
            ) { _, bundle ->
                val result = bundle.getParcelableCompat<PostView>(
                    CreateOrEditPostFragment.REQUEST_KEY_RESULT,
                )

                if (result != null) {
                    viewModel.fetchCurrentPage(force = true)
                    slidingPaneController?.openPost(
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

            setFragmentResultListener(
                InstancePickerDialogFragment.REQUEST_KEY,
                this@CommunityFragment,
            ) { _, bundle ->
                val result = bundle.getParcelableCompat<InstancePickerDialogFragment.Result>(
                    InstancePickerDialogFragment.REQUEST_KEY_RESULT,
                )

                if (result != null) {
                    viewModel.changeGuestAccountInstance(result.instance)
                }
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

        viewModel.updatePreferences()
        binding.loadingView.hideAll()

        lemmyAppBarController = LemmyAppBarController(
            mainActivity = requireMainActivity(),
            binding = binding.customAppBar,
            accountInfoManager = accountInfoManager,
        )

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
            onPostUpdated = {
                updatePost(it)
            },
        )

        requireMainActivity().apply {
            insetViewExceptBottomAutomaticallyByMargins(
                lifecycleOwner = viewLifecycleOwner,
                view = binding.customAppBar.customActionBar,
            )

            if (navBarController.useNavigationRail) {
                navBarController.updatePaddingForNavBar(binding.coordinatorLayout)
            }
            binding.customAppBar.root.addOnOffsetChangedListener { _, verticalOffset ->
                if (viewModel.lockBottomBar) {
                    return@addOnOffsetChangedListener
                }

                val percentShown = -verticalOffset.toFloat() / binding.customAppBar.root.height

                if (!navBarController.useNavigationRail) {
                    navBarController.navBarOffsetPercent.value = percentShown
                }

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
                onChangeInstanceClick = {
                    InstancePickerDialogFragment.show(childFragmentManager)
                },
            )
        }

        runAfterLayout {
            if (!isBindingAvailable()) return@runAfterLayout

            adapter?.contentMaxWidth = binding.recyclerView.measuredWidth
            adapter?.contentPreferredHeight = binding.recyclerView.measuredHeight
        }

        runOnReady {
            onReady()
        }

        binding.fab.setup(preferences)
        binding.fab.setOnClickListener a@{
            showOverflowMenu()
        }

        requireActivity().onBackPressedDispatcher
            .addCallback(viewLifecycleOwner, onBackPressedHandler)

        slidingPaneController = SlidingPaneController(
            fragment = this,
            slidingPaneLayout = binding.slidingPaneLayout,
            childFragmentManager = childFragmentManager,
            viewModel = viewModel,
            globalLayoutMode = preferences.globalLayoutMode,
            retainClosedPosts = preferences.retainLastPost,
        ).apply {
            onPageSelectedListener = { isOpen ->
                if (!isOpen) {
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

                if (isOpen) {
                    requireMainActivity().apply {
                        setupForFragment<PostFragment>()
                        if (isSlideable) {
                            navBarController.hideNavBar(true)
                            setNavUiOpenPercent(1f)
                            lockUiOpenness = true
                        }
                    }
                    if (isSlideable) {
                        val mainFragment = parentFragment?.parentFragment as? MainFragment
                        mainFragment?.setStartPanelLockState(OverlappingPanelsLayout.LockState.CLOSE)
                    }
                } else {
                    requireMainActivity().apply {
                        setupForFragment<CommunityFragment>()
                        lockUiOpenness = false
                        if (isSlideable) {
                            setNavUiOpenPercent(0f)
                        }
                    }
                    if (isSlideable) {
                        val mainFragment = parentFragment?.parentFragment as? MainFragment
                        if (!lockPanes) {
                            mainFragment?.setStartPanelLockState(OverlappingPanelsLayout.LockState.UNLOCKED)
                        }
                    }
                }
            }

            init()
        }
    }

    fun closePost(postFragment: PostFragment) {
        slidingPaneController?.closePost(postFragment)
    }

    fun isPristineFirstPage(): Boolean {
        if (!isBindingAvailable()) {
            return false
        }

        val position = (binding.recyclerView.layoutManager as? LinearLayoutManager)
            ?.findFirstCompletelyVisibleItemPosition()

        return position == 0 && viewModel.currentPageIndex.value == 0
    }

    fun updatePost(postId: PostId) {
        viewModel.updatePost(postId)
    }

    private fun onReady() {
        if (!isBindingAvailable()) return

        val view = binding.root
        val context = requireContext()

        checkNotNull(view.findNavController()) { "NavController was null!" }

        requireMainActivity().apply {
            insetViewExceptTopAutomaticallyByPaddingAndNavUi(
                viewLifecycleOwner,
                binding.fabSnackbarCoordinatorLayout,
            )
            insetViewExceptTopAutomaticallyByPaddingAndNavUi(
                viewLifecycleOwner,
                binding.recyclerView,
            )
            insetViewStartAndEndByPadding(
                viewLifecycleOwner,
                binding.fastScroller,
            )
        }

        binding.swipeRefreshLayout.setOnRefreshListener {
            viewModel.fetchCurrentPage(
                force = true,
                resetHideRead = true,
                clearPages = true,
                scrollToTop = true,
            )
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
                override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                    super.onScrollStateChanged(recyclerView, newState)

                    val firstPos = layoutManager.findFirstVisibleItemPosition()
                    val lastPos = layoutManager.findLastVisibleItemPosition()
                    if (newState == SCROLL_STATE_IDLE) {
                        fetchPageIfLoadItem(firstPos)
                        fetchPageIfLoadItem(firstPos - 1)
                        fetchPageIfLoadItem(lastPos)
                        fetchPageIfLoadItem(lastPos + 1)
                    }
                }

                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    super.onScrolled(recyclerView, dx, dy)

                    val adapter = adapter ?: return
                    val firstPos = layoutManager.findFirstVisibleItemPosition()
                    val lastPos = layoutManager.findLastVisibleItemPosition()
                    (adapter.items.getOrNull(firstPos) as? Item.VisiblePostItem)
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
                    if (!binding.swipeRefreshLayout.isRefreshing) {
                        binding.loadingView.showProgressBar()
                    }
                }
                is StatefulData.NotStarted -> {}
                is StatefulData.Success -> {
                    binding.loadingView.hideAll()

                    val adapter = adapter ?: return@a

                    if (it.data.isReadPostUpdate) {
                        adapter.onItemsChanged(animate = false)
                    } else {
                        adapter.seenItemPositions.clear()
                        adapter.onItemsChanged()

                        if (it.data.scrollToTop) {
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

                        if (viewModel.postListEngine.items.isEmpty()) {
                            binding.loadingView.showErrorText(R.string.no_posts)
                        }
                    }
                }
            }
        }

        actionsViewModel.blockCommunityResult.observe(viewLifecycleOwner) {
            when (it) {
                is StatefulData.Error -> {
                    Snackbar.make(
                        requireMainActivity().getSnackbarContainer(),
                        R.string.error_unable_to_block_community,
                        Snackbar.LENGTH_LONG,
                    ).show()
                }
                is StatefulData.Loading -> {}
                is StatefulData.NotStarted -> {}
                is StatefulData.Success -> {
                    actionsViewModel.blockCommunityResult.setIdle()
                    viewModel.onBlockSettingsChanged()
                }
            }
        }
        actionsViewModel.blockPersonResult.observe(viewLifecycleOwner) {
            when (it) {
                is StatefulData.Error -> {
                    Snackbar.make(
                        requireMainActivity().getSnackbarContainer(),
                        R.string.error_unable_to_block_person,
                        Snackbar.LENGTH_LONG,
                    ).show()
                }
                is StatefulData.Loading -> {}
                is StatefulData.NotStarted -> {}
                is StatefulData.Success -> {
                    actionsViewModel.blockPersonResult.setIdle()
                    viewModel.onBlockSettingsChanged()
                }
            }
        }
        actionsViewModel.blockInstanceResult.observe(viewLifecycleOwner) {
            when (it) {
                is StatefulData.Error -> {
                    Snackbar.make(
                        requireMainActivity().getSnackbarContainer(),
                        R.string.error_unable_to_block_instance,
                        Snackbar.LENGTH_LONG,
                    ).show()
                }
                is StatefulData.Loading -> {}
                is StatefulData.NotStarted -> {}
                is StatefulData.Success -> {
                    actionsViewModel.blockInstanceResult.setIdle()
                    viewModel.onBlockSettingsChanged()
                }
            }
        }

        if (adapter?.items.isNullOrEmpty()) {
            viewModel.fetchInitialPage()
        }

        runAfterLayout {
            // try to restore state...
            // SlidingPaneLayout needs 1 layout pass in order to be in the right state. Otherwise
            // it will always think there is enough room even if there isn't.
            slidingPaneController?.callPageSelected()
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
                            slidingPaneController?.openPost(
                                instance = viewModel.apiInstance,
                                id = postView.post.id,
                                reveal = false,
                                post = postView,
                                jumpToComments = false,
                                currentCommunity = viewModel.currentCommunityRef.value,
                                videoState = null,
                            )

                            AddOrEditCommentFragment.showReplyDialog(
                                viewModel.apiInstance,
                                Either.Left(postView),
                                childFragmentManager,
                            )
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
            itemTouchHelper = ItemTouchHelper(
                requireNotNull(swipeActionCallback) {
                    "swipeActionCallback is null!"
                },
            )
        }

        swipeActionCallback?.updatePostSwipeActions()

        itemTouchHelper?.attachToRecyclerView(binding.recyclerView)
    }

    private fun LemmySwipeActionCallback.updatePostSwipeActions() {
        if (!isBindingAvailable()) return
        val context = requireContext()
        this.gestureSize = preferences.postGestureSize
        this.actions = preferences.getPostSwipeActions(context)
    }

    override fun onResume() {
        super.onResume()

        viewModel.changeCommunity(args.communityRef)

        runOnReady {
            val customAppBarController = lemmyAppBarController

            viewModel.currentCommunityRef.observe(viewLifecycleOwner) {
                customAppBarController.setCommunity(it)

                val tab = args.tab
                if (tab != null) {
                    viewModel.updateTab(tab, it)
                }

                // Apply per-community settings
                onSelectedLayoutChanged()
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

            if (!binding.slidingPaneLayout.isOpaque) {
                getMainActivity()?.setNavUiOpenPercent(0f)
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
            CommunitySortOrder.Controversial -> _sortByMenu.setChecked(R.id.sort_order_controversial)
            CommunitySortOrder.Scaled -> _sortByMenu.setChecked(R.id.sort_order_scaled)
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

        val currentCommunityRef = requireNotNull(viewModel.currentCommunityRef.value) {
            "currentCommunityRef is null!"
        }
        val currentDefaultPage = preferences.getDefaultPage()
        val isBookmarked = userCommunitiesManager.isCommunityBookmarked(currentCommunityRef)
        val isCurrentPageDefault = currentCommunityRef == currentDefaultPage

        val bottomMenu = BottomMenu(context).apply {
            addItemWithIcon(R.id.create_post, R.string.create_post, R.drawable.baseline_add_24)

            addItemWithIcon(R.id.ca_share, R.string.share, R.drawable.baseline_share_24)
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
                id = R.id.create_multi_community,
                title = R.string.create_multi_community,
                icon = R.drawable.baseline_dynamic_feed_24,
            )
            addItemWithIcon(
                id = R.id.settings,
                title = R.string.settings,
                icon = R.drawable.baseline_settings_24,
            )

            if (!isCurrentPageDefault) {
                addItemWithIcon(
                    id = R.id.set_as_default,
                    title = R.string.set_as_home_page,
                    icon = R.drawable.baseline_home_24,
                )
            }

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

            val mainFragment = parentFragment?.parentFragment as? MainFragment

            if (mainFragment != null) {
                addDivider()
                addItemWithIcon(
                    id = R.id.back_to_the_beginning,
                    title = R.string.back_to_the_beginning,
                    icon = R.drawable.baseline_arrow_upward_24,
                )
            }

            addDivider()
            addItemWithIcon(
                id = R.id.per_community_settings,
                title = getString(
                    R.string.per_community_settings_format,
                    currentCommunityRef.getName(context),
                ),
                icon = R.drawable.ic_community_24,
            )

            if (getMainActivity()?.useBottomNavBar == false) {
                addDivider()
                val navBarDestinations = preferences.navBarConfig.navBarDestinations
                for (dest in navBarDestinations) {
                    when (dest) {
                        NavBarDestinations.Home -> {
//                            addItemWithIcon(
//                                R.id.mainFragment,
//                                getString(R.string.home),
//                                R.drawable.baseline_home_24,
//                            )
                        }
                        NavBarDestinations.Saved -> {
                            addItemWithIcon(
                                R.id.savedFragment,
                                getString(R.string.saved),
                                R.drawable.baseline_bookmark_24,
                            )
                        }
                        NavBarDestinations.Search -> {
                            addItemWithIcon(
                                R.id.searchFragment,
                                getString(R.string.search),
                                R.drawable.baseline_search_24,
                            )
                        }
                        NavBarDestinations.History -> {
                            addItemWithIcon(
                                R.id.historyFragment,
                                getString(R.string.history),
                                R.drawable.baseline_history_24,
                            )
                        }
                        NavBarDestinations.Inbox -> {
                            addItemWithIcon(
                                R.id.inboxTabbedFragment,
                                getString(R.string.inbox),
                                R.drawable.baseline_inbox_24,
                            )
                        }
                        NavBarDestinations.Profile -> {
                            addItemWithIcon(
                                R.id.personTabbedFragment2,
                                getString(R.string.user_profile),
                                R.drawable.outline_account_circle_24,
                            )
                        }
                        NavBarDestinations.None -> {
                        }
                    }
                }
            }

            setOnMenuItemClickListener { menuItem ->
                when (menuItem.id) {
                    R.id.create_post -> {
                        val currentCommunity = viewModel.currentCommunityRef.value
                        var communityName: String? = null
                        when (currentCommunity) {
                            is CommunityRef.All -> {}
                            is CommunityRef.CommunityRefByName -> {
                                communityName = currentCommunity.name
                            }
                            is CommunityRef.Local -> {}
                            is CommunityRef.Subscribed -> {}
                            is CommunityRef.MultiCommunity -> {}
                            is CommunityRef.ModeratedCommunities -> {}
                            null -> {}
                        }

                        CreateOrEditPostFragment()
                            .apply {
                                arguments = CreateOrEditPostFragmentArgs(
                                    instance = viewModel.communityInstance,
                                    communityName = communityName,
                                ).toBundle()
                            }
                            .showAllowingStateLoss(childFragmentManager, "CreateOrEditPostFragment")
                    }
                    R.id.ca_share -> {
                        try {
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
                        } catch (e: MultiCommunityException) {
                            AlertDialogFragment.Builder()
                                .setMessage(R.string.error_cannot_share_multi_community)
                                .createAndShow(childFragmentManager, "sdafx")
                        }
                    }
                    R.id.hide_read -> {
                        val anchors = mutableSetOf<Int>()
                        val range = (binding.recyclerView.layoutManager as? LinearLayoutManager)?.let {
                            it.findFirstCompletelyVisibleItemPosition()..(adapter?.items?.size ?: it.findLastVisibleItemPosition())
                        }
                        range?.mapNotNullTo(anchors) {
                            (adapter?.items?.getOrNull(it) as? Item.VisiblePostItem)?.postView?.post?.id
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
                                when (currentCommunityRef) {
                                    is CommunityRef.CommunityRefByName ->
                                        viewModel.postListEngine.getCommunityIcon()
                                    is CommunityRef.All,
                                    is CommunityRef.Local,
                                    is CommunityRef.ModeratedCommunities,
                                    is CommunityRef.MultiCommunity,
                                    is CommunityRef.Subscribed,
                                    -> null
                                },
                            )
                        }
                    }
                    R.id.browse_communities -> {
                        lemmyAppBarController.showCommunitySelector()
                    }
                    R.id.settings -> {
                        requireMainActivity().openSettings()
                    }
                    R.id.create_multi_community -> {
                        MultiCommunityEditorDialogFragment.show(
                            childFragmentManager,
                            CommunityRef.MultiCommunity(
                                getString(R.string.default_multi_community_name),
                                null,
                                listOf(),
                            ),
                        )
                    }
                    R.id.mainFragment -> {
                        getMainActivity()?.navigateTopLevel(menuItem.id)
                    }
                    R.id.savedFragment -> {
                        getMainActivity()?.navigateTopLevel(menuItem.id)
                    }
                    R.id.searchFragment -> {
                        getMainActivity()?.navigateTopLevel(menuItem.id)
                    }
                    R.id.historyFragment -> {
                        getMainActivity()?.navigateTopLevel(menuItem.id)
                    }
                    R.id.inboxTabbedFragment -> {
                        getMainActivity()?.navigateTopLevel(menuItem.id)
                    }
                    R.id.back_to_the_beginning -> {
                        binding.recyclerView.scrollToPosition(0)
                    }
                    R.id.per_community_settings -> {
                        showPerCommunitySettings(currentCommunityRef)
                    }
                }
            }
        }

        getMainActivity()?.showBottomMenu(bottomMenu, expandFully = false)
    }

    private fun showPerCommunitySettings(currentCommunityRef: CommunityRef) {
        val context = context ?: return

        val bottomMenu = BottomMenu(context).apply {
            setTitle(
                getString(
                    R.string.per_community_settings_format,
                    currentCommunityRef.getName(context),
                ),
            )
            addItemWithIcon(R.id.layout, R.string.layout, R.drawable.baseline_view_comfy_24)
            addItemWithIcon(R.id.reset_settings, R.string.reset_settings, R.drawable.baseline_reset_wrench_24)

            setOnMenuItemClickListener { menuItem ->
                when (menuItem.id) {
                    R.id.layout -> {
                        val layoutMenu = makeLayoutSelectorMenu {
                            val communityConfig = perCommunityPreferences.getCommunityConfig(
                                currentCommunityRef,
                            ) ?: PerCommunityPreferences.CommunityConfig(currentCommunityRef)
                            val updatedConfig = communityConfig.copy(layout = it)

                            perCommunityPreferences.setCommunityConfig(
                                currentCommunityRef,
                                updatedConfig,
                            )
                            viewModel.basePreferences.usePerCommunitySettings = true
                            onSelectedLayoutChanged()
                        }

                        getMainActivity()?.showBottomMenu(layoutMenu, expandFully = false)
                    }
                    R.id.reset_settings -> {
                        perCommunityPreferences.setCommunityConfig(currentCommunityRef, null)
                        onSelectedLayoutChanged()
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
        val currentLayout = currentLayout
        val newPostUiConfig = preferences.getPostInListUiConfig(currentLayout)
        val didUiConfigChange = postListViewBuilder.postUiConfig != newPostUiConfig
        val didLayoutChange = adapter?.layout != currentLayout

        if (didLayoutChange) {
            if (isBindingAvailable()) {
                updateDecoratorAndGestureHandler(binding.recyclerView)
            }
        }

        if (didUiConfigChange) {
            postListViewBuilder.postUiConfig = newPostUiConfig
        }

        if (didLayoutChange || didUiConfigChange) {
            adapter?.layout = currentLayout

            // Need to manually call this in case the layout didn't change
            adapter?.notifyDataSetChanged()
        }

        postListViewBuilder.onPostUiConfigUpdated()
    }

    private fun updateDecoratorAndGestureHandler(recyclerView: RecyclerView) {
        // We need to set the gesture and decorator together
        // Setting the decorator removes the gesture item decorator
        // So we need to add it back right after

        recyclerView.setupDecoratorsForPostList(currentLayout)

        // Detach before attaching or else attach will no-op
        itemTouchHelper?.attachToRecyclerView(null)
        attachGestureHandlerToRecyclerViewIfNeeded()
    }

    private fun makeLayoutSelectorMenu(
        onLayoutSelected: (CommunityLayout) -> Unit,
    ): BottomMenu =
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
                        onLayoutSelected(CommunityLayout.Compact)
                    R.id.layout_list ->
                        onLayoutSelected(CommunityLayout.List)
                    R.id.layout_large_list ->
                        onLayoutSelected(CommunityLayout.LargeList)
                    R.id.layout_card ->
                        onLayoutSelected(CommunityLayout.Card)
                    R.id.layout_card2 ->
                        onLayoutSelected(CommunityLayout.Card2)
                    R.id.layout_card3 ->
                        onLayoutSelected(CommunityLayout.Card3)
                    R.id.layout_full ->
                        onLayoutSelected(CommunityLayout.Full)
                }
            }
        }

    private val currentLayout: CommunityLayout
        get() {
            if (!preferences.usePerCommunitySettings) {
                return preferences.getPostsLayout()
            }

            val currentCommunityRef = viewModel.currentCommunityRef.value ?: args.communityRef
            return currentCommunityRef?.let {
                perCommunityPreferences.getCommunityConfig(it)
            }?.layout ?: preferences.getPostsLayout()
        }
}
