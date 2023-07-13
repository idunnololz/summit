package com.idunnololz.summit.lemmy.community

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.MarginLayoutParams
import androidx.activity.OnBackPressedCallback
import androidx.core.content.ContextCompat
import androidx.core.view.doOnPreDraw
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.viewModels
import androidx.navigation.findNavController
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.Adapter
import com.google.android.material.snackbar.Snackbar
import com.idunnololz.summit.R
import com.idunnololz.summit.account_ui.AccountsAndSettingsDialogFragment
import com.idunnololz.summit.api.dto.PostView
import com.idunnololz.summit.account_ui.PreAuthDialogFragment
import com.idunnololz.summit.account_ui.SignInNavigator
import com.idunnololz.summit.alert.AlertDialogFragment
import com.idunnololz.summit.api.utils.getUniqueKey
import com.idunnololz.summit.api.utils.instance
import com.idunnololz.summit.databinding.AutoLoadItemBinding
import com.idunnololz.summit.databinding.FragmentCommunityBinding
import com.idunnololz.summit.databinding.ListingItemCardBinding
import com.idunnololz.summit.databinding.ListingItemCompactBinding
import com.idunnololz.summit.databinding.ListingItemFullBinding
import com.idunnololz.summit.databinding.ListingItemListBinding
import com.idunnololz.summit.databinding.LoadingViewItemBinding
import com.idunnololz.summit.databinding.MainFooterItemBinding
import com.idunnololz.summit.databinding.PostListEndItemBinding
import com.idunnololz.summit.history.HistoryManager
import com.idunnololz.summit.history.HistorySaveReason
import com.idunnololz.summit.lemmy.CommunityRef
import com.idunnololz.summit.lemmy.CommunitySortOrder
import com.idunnololz.summit.lemmy.CommunityViewState
import com.idunnololz.summit.lemmy.MoreActionsViewModel
import com.idunnololz.summit.lemmy.PageRef
import com.idunnololz.summit.lemmy.PostRef
import com.idunnololz.summit.lemmy.actions.LemmySwipeActionCallback
import com.idunnololz.summit.lemmy.comment.AddOrEditCommentFragment
import com.idunnololz.summit.lemmy.comment.AddOrEditCommentFragmentArgs
import com.idunnololz.summit.lemmy.createOrEditPost.CreateOrEditPostFragment
import com.idunnololz.summit.lemmy.createOrEditPost.CreateOrEditPostFragmentArgs
import com.idunnololz.summit.lemmy.getShortDesc
import com.idunnololz.summit.lemmy.idToSortOrder
import com.idunnololz.summit.lemmy.post.PostFragment
import com.idunnololz.summit.lemmy.postListView.ListingItemViewHolder
import com.idunnololz.summit.lemmy.postListView.PostListViewBuilder
import com.idunnololz.summit.main.LemmyAppBarController
import com.idunnololz.summit.main.MainFragment
import com.idunnololz.summit.offline.OfflineManager
import com.idunnololz.summit.preferences.Preferences
import com.idunnololz.summit.preview.VideoType
import com.idunnololz.summit.user.UserCommunitiesManager
import com.idunnololz.summit.util.BaseFragment
import com.idunnololz.summit.util.BottomMenu
import com.idunnololz.summit.util.CustomDividerItemDecoration
import com.idunnololz.summit.util.SharedElementTransition
import com.idunnololz.summit.util.StatefulData
import com.idunnololz.summit.util.Utils
import com.idunnololz.summit.util.VerticalSpaceItemDecoration
import com.idunnololz.summit.util.ext.getColorCompat
import com.idunnololz.summit.util.ext.getDimen
import com.idunnololz.summit.util.ext.getDrawableCompat
import com.idunnololz.summit.util.ext.navigateSafe
import com.idunnololz.summit.util.ext.showAllowingStateLoss
import com.idunnololz.summit.util.getParcelableCompat
import com.idunnololz.summit.util.recyclerView.ViewBindingViewHolder
import com.idunnololz.summit.util.recyclerView.getBinding
import com.idunnololz.summit.video.VideoState
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class CommunityFragment : BaseFragment<FragmentCommunityBinding>(), SignInNavigator,
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
                when(menuItem.id) {
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
            addItemWithIcon(R.id.layout_list, R.string.list, R.drawable.baseline_list_24)
            addItemWithIcon(R.id.layout_compact, R.string.compact, R.drawable.baseline_list_24)
            addItemWithIcon(R.id.layout_card, R.string.card, R.drawable.baseline_article_24)
            addItemWithIcon(R.id.layout_full, R.string.full, R.drawable.baseline_view_day_24)
            setTitle(R.string.layout)

            setOnMenuItemClickListener { menuItem ->
                when(menuItem.id) {
                    R.id.layout_compact ->
                        preferences.setPostsLayout(CommunityLayout.Compact)
                    R.id.layout_list ->
                        preferences.setPostsLayout(CommunityLayout.List)
                    R.id.layout_card ->
                        preferences.setPostsLayout(CommunityLayout.Card)
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
                            getString(R.string.error_account_instance_mismatch,
                                accountInstance,
                                apiInstance)
                        )
                        .setNegativeButton(R.string.go_to_account_instance)
                        .createAndShow(childFragmentManager, "onInstanceMismatch")
                },
                onImageClick = { sharedElementView, url ->
                    getMainActivity()?.openImage(
                        sharedElement = sharedElementView,
                        appBar = binding.customAppBar,
                        title = null,
                        url = url,
                        mimeType = null
                    )
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
                        videoState ->

                    viewPagerController?.openPost(
                        instance = instance,
                        id =  id,
                        reveal = reveal,
                        post = post,
                        jumpToComments = jumpToComments,
                        currentCommunity = currentCommunity,
                        videoState = videoState,
                    )
                },
                onShowMoreActions = {
                    showMoreOptionsFor(it)
                },
                onPostRead = { postView ->
                    viewModel.onPostRead(postView)
                },
                onLoadPage = {
                    viewModel.fetchPage(it)
                }
            ).apply {
                stateRestorationPolicy = Adapter.StateRestorationPolicy.PREVENT_WHEN_EMPTY
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
            this
        ) { _, bundle ->
            val result = bundle.getParcelableCompat<PostView>(
                CreateOrEditPostFragment.REQUEST_KEY_RESULT)

            if (result != null) {
                viewModel.fetchCurrentPage(force = true)
                viewPagerController?.openPost(
                    instance = result.instance,
                    id =  result.post.id,
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
        savedInstanceState: Bundle?
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

        requireMainActivity().apply {
            insetViewExceptBottomAutomaticallyByMargins(viewLifecycleOwner, binding.customActionBar)

            binding.customAppBar.addOnOffsetChangedListener { _, verticalOffset ->
                val percentShown = -verticalOffset.toFloat() / binding.customAppBar.height

                bottomNavViewOffset.value =
                    (percentShown * getBottomNavHeight()).toInt()

                isCustomAppBarExpandedPercent = 1f - percentShown

                updateFabState()
            }
            lemmyAppBarController.setup(
                communitySelectedListener = { controller, communityRef ->
                    val action = CommunityFragmentDirections.actionSubredditFragmentSwitchSubreddit(
                        communityRef = communityRef
                    )
                    findNavController().navigate(action)
                    Utils.hideKeyboard(activity)
                    controller.hide()
                },
                onAccountClick = {
                    AccountsAndSettingsDialogFragment.newInstance()
                        .showAllowingStateLoss(childFragmentManager, "AccountsDialogFragment")
                })
        }

        view.doOnPreDraw {
            adapter?.contentMaxWidth = binding.recyclerView.measuredWidth
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
        val view = binding.root
        checkNotNull(view.findNavController())

        val mainActivity = requireMainActivity()
        mainActivity.apply {
            insetsChangedLiveData.observe(viewLifecycleOwner) {
                binding.fab.updateLayoutParams<MarginLayoutParams> {
                    this.bottomMargin = getBottomNavHeight() + getDimen(R.dimen.padding)
                }
            }
        }

        mainActivity.insetViewExceptTopAutomaticallyByPaddingAndNavUi(
            viewLifecycleOwner, binding.recyclerView, binding.customActionBar.height)

        val context = requireContext()

        (parentFragment?.parentFragment as? MainFragment)?.updateCommunityInfoPane(
            requireNotNull(viewModel.currentCommunityRef.value)
        )

        binding.swipeRefreshLayout.setOnRefreshListener {
            shouldScrollToTopAfterFresh = true
            viewModel.fetchCurrentPage(true, resetHideRead = true)
            binding.recyclerView.scrollToPosition(0)
        }
        binding.loadingView.setOnRefreshClickListener {
            viewModel.fetchCurrentPage(true)
        }

        val layoutManager = LinearLayoutManager(context)
        binding.recyclerView.adapter = adapter
        binding.recyclerView.setHasFixedSize(true)
        binding.recyclerView.layoutManager = layoutManager

        if (preferences.markPostsAsReadOnScroll) {
            binding.recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    super.onScrolled(recyclerView, dx, dy)

                    val range = layoutManager.findFirstCompletelyVisibleItemPosition()..
                            layoutManager.findLastCompletelyVisibleItemPosition()

                    range.forEach {
                        adapter?.seenItemPositions?.add(it)
                    }
                }
            })
        }

        updateDecorator(binding.recyclerView)

        binding.fastScroller.setRecyclerView(binding.recyclerView)

        fun fetchPageIfLoadItem(position: Int) {
            (adapter?.items?.getOrNull(position) as? Item.AutoLoadItem)
                ?.pageToLoad
                ?.let {
                    viewModel.fetchPage(it)
                }
        }

        binding.recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
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
        })

        binding.rootView.post {
            scheduleStartPostponedTransition(binding.rootView)
        }

        if (preferences.useGestureActions) {
            ItemTouchHelper(LemmySwipeActionCallback(
                context,
                binding.recyclerView,
                listOf(
                    LemmySwipeActionCallback.SwipeAction(
                        R.id.swipe_action_upvote,
                        context.getDrawableCompat(R.drawable.baseline_arrow_upward_24)!!.mutate(),
                        context.getColorCompat(R.color.style_red)
                    ),
                    LemmySwipeActionCallback.SwipeAction(
                        R.id.swipe_action_bookmark,
                        context.getDrawableCompat(R.drawable.baseline_bookmark_add_24)!!.mutate(),
                        context.getColorCompat(R.color.style_amber)
                    ),
                    LemmySwipeActionCallback.SwipeAction(
                        R.id.swipe_action_reply,
                        context.getDrawableCompat(R.drawable.baseline_reply_24)!!.mutate(),
                        context.getColorCompat(R.color.style_blue)
                    )
                ),
                onActionSelected = { action, vh ->
                    val postView = vh.itemView.getTag(R.id.post_view) as? PostView
                        ?: return@LemmySwipeActionCallback

                    when (action.id) {
                        R.id.swipe_action_upvote -> {
                            actionsViewModel.upvote(postView)
                        }
                        R.id.swipe_action_bookmark -> {
                            getMainActivity()?.showSnackbar(R.string.coming_soon)
                        }
                        R.id.swipe_action_reply -> {

                            viewPagerController?.openPost(
                                instance = viewModel.accountInstance,
                                id =  postView.post.id,
                                reveal = false,
                                post = postView,
                                jumpToComments = false,
                                currentCommunity = viewModel.currentCommunityRef.value,
                                videoState = null,
                            )

                            AddOrEditCommentFragment().apply {
                                arguments = AddOrEditCommentFragmentArgs(
                                    viewModel.accountInstance, null, postView, null,
                                ).toBundle()
                            }.show(childFragmentManager, "asdf")
                        }
                    }
                }
            )).attachToRecyclerView(binding.recyclerView)
        }

        viewModel.loadedPostsData.observe(viewLifecycleOwner) a@{
            when (it) {
                is StatefulData.Error -> {
                    binding.swipeRefreshLayout.isRefreshing = false

                    if (viewModel.infinity) {
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
                                    biggestPageIndex
                                )
                                if (pagePosition.isAtBottom) {
                                    (binding.recyclerView.layoutManager as LinearLayoutManager)
                                        .scrollToPositionWithOffset(adapter.itemCount - 1, 0)
                                } else if (pagePosition.itemIndex != 0 || pagePosition.offset != 0) {
                                    (binding.recyclerView.layoutManager as LinearLayoutManager)
                                        .scrollToPositionWithOffset(
                                            pagePosition.itemIndex,
                                            pagePosition.offset
                                        )
                                } else {
                                    binding.recyclerView.scrollToPosition(0)
                                    binding.customAppBar.setExpanded(true)
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

    override fun onResume() {
        super.onResume()

        runOnReady {
            val customAppBarController = lemmyAppBarController

            viewModel.currentCommunityRef.observe(viewLifecycleOwner) {
                val currentDefaultPage = preferences.getDefaultPage()
                customAppBarController.setCommunity(it, it == currentDefaultPage)

                updateFabState()
            }
            if (viewModel.infinity) {
                customAppBarController.setPageIndexInfinity()
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

            onSelectedLayoutChanged()
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
                shortDesc = viewState.getShortDesc(context)
            )
        }
    }

    override fun onDestroyView() {
        Log.d(TAG, "onDestroyView()")
        super.onDestroyView()
    }

    private fun restoreState(state: CommunityViewState?, reload: Boolean) {
        viewModel.restoreFromState(state ?: return)
        if (reload)
            viewModel.fetchCurrentPage()
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
            CommunityLayout.Card ->
                _layoutSelectorMenu.setChecked(R.id.layout_card)
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
                icon = R.drawable.ic_subreddit_default
            )
            addItemWithIcon(
                id = R.id.my_communities,
                title = R.string.my_communities,
                icon = R.drawable.baseline_subscriptions_24
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
                        icon = R.drawable.baseline_bookmark_remove_24
                    )
                } else {
                    addItemWithIcon(
                        id = R.id.toggle_bookmark,
                        title = R.string.bookmark_community,
                        icon = R.drawable.baseline_bookmark_add_24
                    )
                }

                addItemWithIcon(R.id.set_as_default, R.string.set_as_home_page, R.drawable.baseline_home_24)
            }

            setOnMenuItemClickListener {
                when(it.id) {
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
                                viewModel.getSharedLinkForCurrentPage()
                            )
                            type = "text/plain"
                        }

                        val shareIntent = Intent.createChooser(sendIntent, null)
                        startActivity(shareIntent)
                    }
                    R.id.hide_read -> {
                        val anchors = mutableSetOf<Int>()
                        val range = (binding.recyclerView.layoutManager as? LinearLayoutManager)?.let {
                            it.findFirstCompletelyVisibleItemPosition()..
                                    (adapter?.items?.size ?: it.findLastVisibleItemPosition())
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
                            Snackbar.LENGTH_LONG
                        ).show()
                    }
                    R.id.layout ->  {
                        getMainActivity()?.showBottomMenu(getLayoutMenu())
                    }

                    R.id.community_info -> {
                        (parentFragment?.parentFragment as? MainFragment)?.expandEndPane()
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
                                viewModel.postListEngine.getCommunityIcon())
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
                updateDecorator(binding.recyclerView)
            }
        }

        if (didUiConfigChange) {
            postListViewBuilder.postUiConfig = newPostUiConfig
        }

        if (didLayoutChange || didUiConfigChange) {
            adapter?.layout = preferences.getPostsLayout()
        }
    }

    private fun updateDecorator(recyclerView: RecyclerView) {
        while (binding.recyclerView.itemDecorationCount != 0) {
            binding.recyclerView.removeItemDecorationAt(
                binding.recyclerView.itemDecorationCount - 1)
        }
        if (preferences.getPostsLayout().usesDividers()) {
            binding.recyclerView.addItemDecoration(
                CustomDividerItemDecoration(
                    recyclerView.context,
                    DividerItemDecoration.VERTICAL
                ).apply {
                    setDrawable(
                        checkNotNull(
                            ContextCompat.getDrawable(
                                context,
                                R.drawable.vertical_divider
                            )
                        )
                    )
                }
            )
        } else {
            binding.recyclerView.addItemDecoration(
                VerticalSpaceItemDecoration(
                    recyclerView.context.getDimen(R.dimen.padding),
                    false
                )
            )
        }
    }

    @SuppressLint("ResourceType")
    private fun showMoreOptionsFor(postView: PostView) {
        if (!isBindingAvailable()) {
            return
        }

        val context = requireContext()

        val bottomMenu = BottomMenu(context).apply {
            setTitle(R.string.more_actions)
            addItemWithIcon(
                R.id.block_community,
                getString(R.string.block_this_community_format, postView.community.name),
                R.drawable.baseline_block_24
            )
            addItemWithIcon(
                R.id.block_user,
                getString(R.string.block_this_user, postView.creator.name),
                R.drawable.baseline_person_off_24
            )

            setOnMenuItemClickListener {
                when (it.id) {
                    R.id.block_community -> {
                        actionsViewModel.blockCommunity(postView.community.id)
                    }
                    R.id.block_user -> {
                        actionsViewModel.blockPerson(postView.creator.id)
                    }
                }
            }
        }

        getMainActivity()?.showBottomMenu(bottomMenu)
    }

    private inner class ListingItemAdapter(
        private val postListViewBuilder: PostListViewBuilder,
        private val context: Context,
        private var postListEngine: PostListEngine,
        private val onNextClick: () -> Unit,
        private val onPrevClick: () -> Unit,
        private val onSignInRequired: () -> Unit,
        private val onInstanceMismatch: (String, String) -> Unit,
        private val onImageClick: (View?, String) -> Unit,
        private val onVideoClick: (String, VideoType, VideoState?) -> Unit,
        private val onPageClick: (PageRef) -> Unit,
        private val onItemClick: (
            instance: String,
            id: Int,
            currentCommunity: CommunityRef?,
            post: PostView,
            jumpToComments: Boolean,
            reveal: Boolean,
            videoState: VideoState?
        ) -> Unit,
        private val onShowMoreActions: (PostView) -> Unit,
        private val onPostRead: (PostView) -> Unit,
        private val onLoadPage: (Int) -> Unit,
    ) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

        private val inflater = LayoutInflater.from(context)

        var items: List<Item> = listOf()
            private set

        /**
         * Set of items that is hidden by default but is reveals (ie. nsfw or spoiler tagged)
         */
        private var revealedItems = mutableSetOf<String>()

        var layout: CommunityLayout = CommunityLayout.List
            set(value) {
                field = value

                notifyDataSetChanged()
            }

        var contentMaxWidth: Int = 0

        var seenItemPositions = mutableSetOf<Int>()

        override fun getItemViewType(position: Int): Int = when (items[position]) {
            is Item.PostItem -> when (layout) {
                CommunityLayout.Compact -> R.layout.listing_item_compact
                CommunityLayout.List -> R.layout.listing_item_list
                CommunityLayout.Card -> R.layout.listing_item_card
                CommunityLayout.Full -> R.layout.listing_item_full
            }
            is Item.FooterItem -> R.layout.main_footer_item
            is Item.AutoLoadItem -> R.layout.auto_load_item
            Item.EndItem -> R.layout.post_list_end_item
            is Item.ErrorItem -> R.layout.loading_view_item
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            val v = inflater.inflate(viewType, parent, false)
            return when (viewType) {
                R.layout.listing_item_compact ->
                    ListingItemViewHolder.fromBinding(ListingItemCompactBinding.bind(v))
                R.layout.listing_item_list ->
                    ListingItemViewHolder.fromBinding(ListingItemListBinding.bind(v))
                R.layout.listing_item_card ->
                    ListingItemViewHolder.fromBinding(ListingItemCardBinding.bind(v))
                R.layout.listing_item_full ->
                    ListingItemViewHolder.fromBinding(ListingItemFullBinding.bind(v))
                R.layout.main_footer_item -> ViewBindingViewHolder(MainFooterItemBinding.bind(v))
                R.layout.auto_load_item ->
                    ViewBindingViewHolder(AutoLoadItemBinding.bind(v))
                R.layout.post_list_end_item ->
                    ViewBindingViewHolder(PostListEndItemBinding.bind(v))
                R.layout.loading_view_item ->
                    ViewBindingViewHolder(LoadingViewItemBinding.bind(v))
                else -> throw RuntimeException("Unknown view type: $viewType")
            }
        }

        override fun onBindViewHolder(
            holder: RecyclerView.ViewHolder,
            position: Int,
            payloads: MutableList<Any>
        ) {
            when (val item = items[position]) {
                is Item.PostItem -> {
                    if (payloads.isEmpty()) {
                        super.onBindViewHolder(holder, position, payloads)
                    } else {
                        val h: ListingItemViewHolder = holder as ListingItemViewHolder
                        val isRevealed = revealedItems.contains(item.postView.getUniqueKey())
                        val isActionsExpanded = item.isActionExpanded
                        val isExpanded = item.isExpanded

                        h.root.setTag(R.id.post_view, item.postView)

                        postListViewBuilder.bind(
                            holder = h,
                            container = binding.recyclerView,
                            postView = item.postView,
                            instance = item.instance,
                            isRevealed = isRevealed,
                            contentMaxWidth = contentMaxWidth,
                            viewLifecycleOwner = viewLifecycleOwner,
                            isExpanded = isExpanded,
                            isActionsExpanded = isActionsExpanded,
                            updateContent = false,
                            highlight = item.highlight,
                            highlightForever = item.highlightForever,
                            onRevealContentClickedFn = {
                                revealedItems.add(item.postView.getUniqueKey())
                                notifyItemChanged(h.absoluteAdapterPosition)
                            },
                            onImageClick = onImageClick,
                            onVideoClick = onVideoClick,
                            onPageClick = onPageClick,
                            onItemClick = onItemClick,
                            onShowMoreOptions = onShowMoreActions,
                            toggleItem = this::toggleItem,
                            toggleActions = this::toggleActions,
                            onSignInRequired = onSignInRequired,
                            onInstanceMismatch = onInstanceMismatch,
                            onHighlightComplete = {
                                  postListEngine.clearHighlight()
                            },
                        )
                    }
                }
                else -> {
                    super.onBindViewHolder(holder, position, payloads)
                }
            }
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            when (val item = items[position]) {
                is Item.FooterItem -> {
                    val b = holder.getBinding<MainFooterItemBinding>()
                    if (item.hasMore) {
                        b.nextButton.visibility = View.VISIBLE
                        b.nextButton.setOnClickListener {
                            if (preferences.markPostsAsReadOnScroll) {
                                seenItemPositions.forEach {
                                    (items.getOrNull(it) as? Item.PostItem)?.let {
                                        onPostRead(it.postView)
                                    }
                                }
                            }
                            onNextClick()
                        }
                    } else {
                        b.nextButton.visibility = View.INVISIBLE
                    }
                    if (viewModel.currentPageIndex.value == 0) {
                        b.prevButton.visibility = View.INVISIBLE
                    } else {
                        b.prevButton.visibility = View.VISIBLE
                        b.prevButton.setOnClickListener {
                            onPrevClick()
                        }
                    }
                }
                is Item.PostItem -> {
                    val h: ListingItemViewHolder = holder as ListingItemViewHolder
                    val isRevealed = revealedItems.contains(item.postView.getUniqueKey())
                    val isActionsExpanded = item.isActionExpanded
                    val isExpanded = item.isExpanded

                    h.root.setTag(R.id.post_view, item.postView)
                    h.root.setTag(R.id.swipeable, true)

                    postListViewBuilder.bind(
                        holder = h,
                        container = binding.recyclerView,
                        postView = item.postView,
                        instance = item.instance,
                        isRevealed = isRevealed,
                        contentMaxWidth = contentMaxWidth,
                        viewLifecycleOwner = viewLifecycleOwner,
                        isExpanded = isExpanded,
                        isActionsExpanded = isActionsExpanded,
                        updateContent = true,
                        highlight = item.highlight,
                        highlightForever = item.highlightForever,
                        onRevealContentClickedFn = {
                            revealedItems.add(item.postView.getUniqueKey())
                            notifyItemChanged(h.absoluteAdapterPosition)
                        },
                        onImageClick = onImageClick,
                        onShowMoreOptions = onShowMoreActions,
                        onVideoClick = onVideoClick,
                        onPageClick = onPageClick,
                        onItemClick = onItemClick,
                        toggleItem = this::toggleItem,
                        toggleActions = this::toggleActions,
                        onSignInRequired = onSignInRequired,
                        onInstanceMismatch = onInstanceMismatch,
                        onHighlightComplete = {
                            postListEngine.clearHighlight()
                        }
                    )
                }

                is Item.AutoLoadItem -> {
                    val b = holder.getBinding<AutoLoadItemBinding>()
                    b.loadingView.showProgressBar()
                }

                Item.EndItem -> {}
                is Item.ErrorItem -> {
                    val b = holder.getBinding<LoadingViewItemBinding>()
                    b.loadingView.showDefaultErrorMessageFor(item.error)
                    b.loadingView.setOnRefreshClickListener {
                        onLoadPage(item.pageToLoad)
                    }
                }
            }
        }

        override fun onViewRecycled(holder: RecyclerView.ViewHolder) {
            val position = holder.absoluteAdapterPosition
            super.onViewRecycled(holder)

            if (holder is ListingItemViewHolder) {
                postListViewBuilder.recycle(
                    holder
                )

                if (preferences.markPostsAsReadOnScroll) {
                    val postView = holder.root.getTag(R.id.post_view) as? PostView
                    if (postView != null && seenItemPositions.contains(position)) {
                        Log.d("HAHA", "Marking post ${position} - ${postView.post.name} as read")
                        onPostRead(postView)
                    } else {
                        Log.d("HAHA", "${position} recycled but not read")
                    }
                }
            }
        }

        private fun toggleItem(postView: PostView) {
            val isExpanded = postListEngine.toggleItem(postView)

            if (isExpanded) {
                onPostRead(postView)
            }

            postListEngine.createItems()
            refreshItems(animate = true)
        }

        private fun toggleActions(postView: PostView) {
            postListEngine.toggleActions(postView)
            postListEngine.createItems()
            refreshItems(animate = true)
        }

        override fun getItemCount(): Int = items.size

        fun onItemsChanged(
            animate: Boolean = true
        ) {
            refreshItems(animate)
        }

        fun highlightPost(postToHighlight: PostRef) {
            val index = postListEngine.highlight(postToHighlight)

            if (index >= 0) {
                postListEngine.createItems()
                refreshItems(animate = false)
            }
        }

        fun highlightPostForever(postToHighlight: PostRef) {
            val index = postListEngine.highlightForever(postToHighlight)

            if (index >= 0) {
                postListEngine.createItems()
                refreshItems(animate = false)
            }
        }

        fun refreshItems(animate: Boolean) {
            val newItems = postListEngine.items
            val oldItems = items

            val diff = DiffUtil.calculateDiff(object : DiffUtil.Callback() {
                override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                    val oldItem = oldItems[oldItemPosition]
                    val newItem = newItems[newItemPosition]

                    return oldItem::class == newItem::class && when (oldItem) {
                        is Item.FooterItem -> true
                        is Item.PostItem ->
                            oldItem.postView.getUniqueKey() ==
                                    (newItem as Item.PostItem).postView.getUniqueKey()
                        is Item.AutoLoadItem ->
                            oldItem.pageToLoad ==
                                    (newItem as Item.AutoLoadItem).pageToLoad

                        Item.EndItem -> true
                        is Item.ErrorItem ->
                            oldItem.pageToLoad ==
                                    (newItem as Item.ErrorItem).pageToLoad
                    }
                }

                override fun getOldListSize(): Int = oldItems.size

                override fun getNewListSize(): Int = newItems.size

                override fun areContentsTheSame(
                    oldItemPosition: Int,
                    newItemPosition: Int
                ): Boolean {
                    val oldItem = oldItems[oldItemPosition]
                    val newItem = newItems[newItemPosition]

                    return oldItem == newItem
                }

                override fun getChangePayload(oldItemPosition: Int, newItemPosition: Int): Any? {
                    return if (animate) {
                        null
                    } else {
                        Unit
                    }
                }
            })
            this.items = newItems
            diff.dispatchUpdatesTo(this)
        }
    }
}
