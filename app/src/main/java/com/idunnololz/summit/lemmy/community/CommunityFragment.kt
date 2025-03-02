package com.idunnololz.summit.lemmy.community

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
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
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.idunnololz.summit.R
import com.idunnololz.summit.account.AccountManager
import com.idunnololz.summit.account.info.AccountInfoManager
import com.idunnololz.summit.account.info.isCommunityBlocked
import com.idunnololz.summit.accountUi.AccountsAndSettingsDialogFragment
import com.idunnololz.summit.accountUi.PreAuthDialogFragment
import com.idunnololz.summit.accountUi.SignInNavigator
import com.idunnololz.summit.alert.OldAlertDialogFragment
import com.idunnololz.summit.api.LemmyApiClient
import com.idunnololz.summit.api.dto.PostId
import com.idunnololz.summit.api.dto.PostView
import com.idunnololz.summit.api.utils.instance
import com.idunnololz.summit.avatar.AvatarHelper
import com.idunnololz.summit.databinding.FragmentCommunityBinding
import com.idunnololz.summit.goTo.GoToDialogFragment
import com.idunnololz.summit.history.HistoryManager
import com.idunnololz.summit.history.HistorySaveReason
import com.idunnololz.summit.lemmy.CommunityRef
import com.idunnololz.summit.lemmy.CommunitySortOrder
import com.idunnololz.summit.lemmy.CommunityViewState
import com.idunnololz.summit.lemmy.ContentTypeFilterTooAggressiveException
import com.idunnololz.summit.lemmy.FilterTooAggressiveException
import com.idunnololz.summit.lemmy.LoadNsfwCommunityWhenNsfwDisabled
import com.idunnololz.summit.lemmy.MultiCommunityException
import com.idunnololz.summit.lemmy.actions.LemmySwipeActionCallback
import com.idunnololz.summit.lemmy.comment.AddOrEditCommentFragment
import com.idunnololz.summit.lemmy.communityInfo.CommunityInfoViewModel
import com.idunnololz.summit.lemmy.createOrEditPost.CreateOrEditPostFragment
import com.idunnololz.summit.lemmy.createOrEditPost.CreateOrEditPostFragmentArgs
import com.idunnololz.summit.lemmy.getShortDesc
import com.idunnololz.summit.lemmy.idToSortOrder
import com.idunnololz.summit.lemmy.instancePicker.InstancePickerDialogFragment
import com.idunnololz.summit.lemmy.multicommunity.FetchedPost
import com.idunnololz.summit.lemmy.multicommunity.MultiCommunityEditorDialogFragment
import com.idunnololz.summit.lemmy.multicommunity.accountId
import com.idunnololz.summit.lemmy.multicommunity.instance
import com.idunnololz.summit.lemmy.post.PostFragment
import com.idunnololz.summit.lemmy.postListView.PostListViewBuilder
import com.idunnololz.summit.lemmy.postListView.showMorePostOptions
import com.idunnololz.summit.lemmy.toApiSortOrder
import com.idunnololz.summit.lemmy.toCommunityRef
import com.idunnololz.summit.lemmy.toId
import com.idunnololz.summit.lemmy.toUrl
import com.idunnololz.summit.lemmy.userTags.UserTagsManager
import com.idunnololz.summit.lemmy.utils.actions.MoreActionsHelper
import com.idunnololz.summit.lemmy.utils.actions.installOnActionResultHandler
import com.idunnololz.summit.lemmy.utils.getPostSwipeActions
import com.idunnololz.summit.lemmy.utils.setup
import com.idunnololz.summit.lemmy.utils.setupDecoratorsForPostList
import com.idunnololz.summit.lemmy.utils.showHelpAndFeedbackOptions
import com.idunnololz.summit.lemmy.utils.showMoreVideoOptions
import com.idunnololz.summit.links.onLinkClick
import com.idunnololz.summit.main.LemmyAppBarController
import com.idunnololz.summit.main.MainFragment
import com.idunnololz.summit.nsfwMode.NsfwModeManager
import com.idunnololz.summit.offline.OfflineManager
import com.idunnololz.summit.offline.dialog.MakeOfflineDialogFragment
import com.idunnololz.summit.preferences.HomeFabQuickActionIds
import com.idunnololz.summit.preferences.PostGestureAction
import com.idunnololz.summit.preferences.Preferences
import com.idunnololz.summit.preferences.perCommunity.PerCommunityPreferences
import com.idunnololz.summit.settings.navigation.NavBarDestinations
import com.idunnololz.summit.user.UserCommunitiesManager
import com.idunnololz.summit.util.AnimationsHelper
import com.idunnololz.summit.util.BaseFragment
import com.idunnololz.summit.util.BottomMenu
import com.idunnololz.summit.util.CustomFabWithBottomNavBehavior
import com.idunnololz.summit.util.PrettyPrintUtils
import com.idunnololz.summit.util.SharedElementTransition
import com.idunnololz.summit.util.StatefulData
import com.idunnololz.summit.util.Utils
import com.idunnololz.summit.util.ext.navigateSafe
import com.idunnololz.summit.util.ext.setup
import com.idunnololz.summit.util.ext.showAllowingStateLoss
import com.idunnololz.summit.util.getParcelableCompat
import com.idunnololz.summit.util.insetViewStartAndEndByPadding
import com.idunnololz.summit.util.setupForFragment
import com.idunnololz.summit.util.showMoreLinkOptions
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

@AndroidEntryPoint
class CommunityFragment :
    BaseFragment<FragmentCommunityBinding>(),
    SignInNavigator,
    OldAlertDialogFragment.AlertDialogFragmentListener {

    companion object {
        private const val TAG = "CommunityFragment"
    }

    private val args: CommunityFragmentArgs by navArgs()

    val viewModel: CommunityViewModel by viewModels()
    private val communityInfoViewModel: CommunityInfoViewModel by viewModels()

    private var adapter: PostListAdapter? = null

    @Inject
    lateinit var moreActionsHelper: MoreActionsHelper

    @Inject
    lateinit var historyManager: HistoryManager

    @Inject
    lateinit var offlineManager: OfflineManager

    @Inject
    lateinit var userCommunitiesManager: UserCommunitiesManager

    @Inject
    lateinit var postListViewBuilder: PostListViewBuilder

    @Inject
    lateinit var accountManager: AccountManager

    @Inject
    lateinit var accountInfoManager: AccountInfoManager

    @Inject
    lateinit var perCommunityPreferences: PerCommunityPreferences

    @Inject
    lateinit var nsfwModeManager: NsfwModeManager

    @Inject
    lateinit var animationsHelper: AnimationsHelper

    @Inject
    lateinit var userTagsManager: UserTagsManager

    @Inject
    lateinit var lemmyApiClient: LemmyApiClient

    @Inject
    lateinit var avatarHelper: AvatarHelper

    @Inject
    lateinit var json: Json

    lateinit var preferences: Preferences

    private var slidingPaneController: SlidingPaneController? = null

    private var isCustomAppBarExpandedPercent = 0f

    private var lemmyAppBarController: LemmyAppBarController? = null

    private var swipeActionCallback: LemmySwipeActionCallback? = null
    private var itemTouchHelper: ItemTouchHelper? = null

    private val onBackPressedHandler = object : OnBackPressedCallback(false) {
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
        makeSortByMenu().apply {
            addDivider()

            addItem(
                R.id.set_default_sort_order,
                getString(R.string.set_default_sort_order_for_this_feed),
            )

            setOnMenuItemClickListener { menuItem ->
                when (menuItem.id) {
                    R.id.sort_order_top ->
                        getMainActivity()?.showBottomMenu(getSortByTopMenu())
                    R.id.set_default_sort_order ->
                        getMainActivity()?.showBottomMenu(getDefaultSortOrderSortByMenu())
                    else ->
                        idToSortOrder(menuItem.id)?.let {
                            viewModel.setSortOrder(it)
                        }
                }
            }
        }
    }

    private val _sortByTopMenu: BottomMenu by lazy {
        makeSortByTopMenu(requireContext()).apply {
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

    private val _defaultSortOrderSortByMenu by lazy {
        makeSortByMenu().apply {
            setOnMenuItemClickListener { menuItem ->
                when (menuItem.id) {
                    R.id.sort_order_top ->
                        getMainActivity()?.showBottomMenu(getDefaultSortOrderSortByTopMenu())
                    else ->
                        idToSortOrder(menuItem.id)?.let {
                            viewModel.setDefaultSortOrder(it)
                        }
                }
            }
        }
    }

    private val _defaultSortOrderSortByTopMenu by lazy {
        makeSortByTopMenu(requireContext()).apply {
            setOnMenuItemClickListener { menuItem ->
                idToSortOrder(menuItem.id)?.let {
                    viewModel.setDefaultSortOrder(it)
                }
            }
        }
    }

    private fun makeSortByMenu() = BottomMenu(requireContext()).apply {
        setTitle(R.string.sort_by)
        addItem(R.id.sort_order_active, R.string.sort_order_active)
        addItem(R.id.sort_order_hot, R.string.sort_order_hot)
        addItem(
            R.id.sort_order_top,
            R.string.sort_order_top,
            R.drawable.baseline_chevron_right_24,
        )
        addItem(R.id.sort_order_new, R.string.sort_order_new)
        addItem(R.id.sort_order_old, R.string.sort_order_old)
        addItem(R.id.sort_order_most_comments, R.string.sort_order_most_comments)
        addItem(R.id.sort_order_new_comments, R.string.sort_order_new_comments)
        addItem(R.id.sort_order_controversial, R.string.sort_order_controversial)
        addItem(R.id.sort_order_scaled, R.string.sort_order_scaled)
    }

    private fun makeSortByTopMenu(context: Context) = BottomMenu(requireContext()).apply {
        setTitle(R.string.sort_by_top)
        addItem(R.id.sort_order_top_last_hour, R.string.time_frame_last_hour)
        addItem(
            R.id.sort_order_top_last_six_hour,
            getString(R.string.time_frame_last_hours_format, "6"),
        )
        addItem(
            R.id.sort_order_top_last_twelve_hour,
            getString(R.string.time_frame_last_hours_format, "12"),
        )
        addItem(R.id.sort_order_top_day, R.string.time_frame_today)
        addItem(R.id.sort_order_top_week, R.string.time_frame_this_week)
        addItem(R.id.sort_order_top_month, R.string.time_frame_this_month)
        addItem(
            R.id.sort_order_top_last_three_month,
            getString(R.string.time_frame_last_months_format, "3"),
        )
        addItem(
            R.id.sort_order_top_last_six_month,
            getString(R.string.time_frame_last_months_format, "6"),
        )
        addItem(
            R.id.sort_order_top_last_nine_month,
            getString(R.string.time_frame_last_months_format, "9"),
        )
        addItem(R.id.sort_order_top_year, R.string.time_frame_this_year)
        addItem(R.id.sort_order_top_all_time, R.string.time_frame_all_time)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        preferences = viewModel.preferences

        viewModel.setTag(parentFragment?.tag)

        val context = requireContext()
        if (adapter == null) {
            adapter = PostListAdapter(
                postListViewBuilder = postListViewBuilder,
                context = context,
                postListEngine = viewModel.postListEngine,
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
                    OldAlertDialogFragment.Builder()
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
                onImageClick = { accountId, postView, sharedElementView, url ->
                    val altUrl = if (url == postView.post.thumbnail_url) {
                        postView.post.url
                    } else {
                        null
                    }

                    getMainActivity()?.openImage(
                        sharedElement = sharedElementView,
                        appBar = lemmyAppBarController?.appBarRoot,
                        title = postView.post.name,
                        url = url,
                        mimeType = null,
                        urlAlt = altUrl,
                    )
                    moreActionsHelper.onPostRead(
                        postView = postView,
                        delayMs = 0,
                        accountId = accountId,
                        read = true,
                    )
                },
                onVideoClick = { url, videoType, state ->
                    getMainActivity()?.openVideo(url, videoType, state)
                },
                onVideoLongClickListener = { url ->
                    showMoreVideoOptions(url, url, moreActionsHelper, childFragmentManager)
                },
                onPageClick = { accountId, pageRef ->
                    getMainActivity()?.launchPage(pageRef)
                },
                onItemClick = {
                        accountId,
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
                        accountId = accountId,
                        videoState = videoState,
                    )
                },
                onShowMoreActions = { accountId, postView ->
                    showMorePostOptions(
                        instance = viewModel.apiInstance,
                        accountId = accountId,
                        postView = postView,
                        moreActionsHelper = moreActionsHelper,
                        fragmentManager = childFragmentManager,
                    )
                },
                onPostRead = { accountId, postView ->
                    moreActionsHelper.onPostRead(
                        postView = postView,
                        delayMs = 0,
                        accountId = accountId,
                        read = true,
                    )
                },
                onLoadPage = {
                    viewModel.fetchPage(it)
                },
                onLinkClick = { accountId, url, text, linkType ->
                    onLinkClick(url, text, linkType)
                },
                onLinkLongClick = { accountId, url, text ->
                    getMainActivity()?.showMoreLinkOptions(url, text)
                },
            ).apply {
                stateRestorationPolicy = Adapter.StateRestorationPolicy.PREVENT_WHEN_EMPTY

                updateWithPreferences(preferences)
                updateNsfwMode(nsfwModeManager)
            }
            onSelectedLayoutChanged()
        }

        viewModel.changeCommunity(args.communityRef)

        if (savedInstanceState != null) {
            restoreState(
                CommunityViewState.restoreFromBundle(savedInstanceState, json),
                reload = false,
            )
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
                        accountId = null,
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

        val context = requireContext()

        with(binding) {
            var job: Job? = null
            viewLifecycleOwner.lifecycleScope.launch {
                viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                    viewModel.apiInstanceFlow.collect { instance ->
                        job?.cancel()
                        job = viewLifecycleOwner.lifecycleScope.launch {
                            val instanceFlows =
                                viewModel.hiddenPostsManager.getInstanceFlows(instance)
                            instanceFlows.onHidePostFlow.collect {
                                viewModel.onHidePost.postValue(it)
                            }
                        }
                    }
                }
            }

            viewModel.updatePreferences()

            lemmyAppBarController = LemmyAppBarController(
                mainActivity = requireMainActivity(),
                baseFragment = this@CommunityFragment,
                parentContainer = coordinatorLayout,
                accountInfoManager = accountInfoManager,
                communityInfoViewModel = communityInfoViewModel,
                viewLifecycleOwner = viewLifecycleOwner,
                avatarHelper = avatarHelper,
                useHeader = preferences.usePostsFeedHeader,
                moreActionsHelper = moreActionsHelper,
                state = lemmyAppBarController?.state,
            )

            // Prevent flickers by setting the app bar here first
            lemmyAppBarController?.setCommunity(args.communityRef)
            // Prevent flickers by setting the header thing
            lemmyAppBarController?.setUseHeader(preferences.usePostsFeedHeader)

            viewModel.defaultCommunity.observe(viewLifecycleOwner) {
                if (it != null) {
                    lemmyAppBarController?.setDefaultCommunity(it)
                }
            }
            viewModel.currentAccount.observe(viewLifecycleOwner) {
                lemmyAppBarController?.onAccountChanged(it)
            }
            viewModel.sortOrder.observe(viewLifecycleOwner) {
                lemmyAppBarController?.setSortOrder(it)
            }

            installOnActionResultHandler(
                moreActionsHelper = moreActionsHelper,
                snackbarContainer = coordinatorLayout,
                onPostUpdated = { postId, accountId ->
                    updatePost(postId, accountId)
                },
                onBlockInstanceChanged = {
                    viewModel.onBlockSettingsChanged()
                },
                onBlockCommunityChanged = {
                    viewModel.onBlockSettingsChanged()
                },
                onBlockPersonChanged = {
                    viewModel.onBlockSettingsChanged()
                },
            )

            requireMainActivity().apply {
                if (navBarController.useNavigationRail) {
                    navBarController.updatePaddingForNavBar(coordinatorLayout)
                }
                lemmyAppBarController?.percentShown?.observe(viewLifecycleOwner) {
                    if (!isBindingAvailable() || viewModel.lockBottomBar) {
                        return@observe
                    }

                    if (!navBarController.useNavigationRail) {
                        navBarController.setNavBarOpenPercent(it)
                    }

                    isCustomAppBarExpandedPercent = 1f - it

                    updateFabState()
                }
                lemmyAppBarController?.setup(
                    communitySelectedListener = { controller, communityRef ->
                        val action =
                            CommunityFragmentDirections.actionCommunityFragmentSwitchCommunity(
                                communityRef = communityRef,
                                tab = args.tab,
                            )
                        findNavController().navigate(action)
                        Utils.hideKeyboard(activity)
                        controller.hide()
                    },
                    onAccountClick = {
                        AccountsAndSettingsDialogFragment.newInstance()
                            .showAllowingStateLoss(
                                childFragmentManager,
                                "AccountsDialogFragment",
                            )
                    },
                    onSortOrderClick = {
                        getMainActivity()?.showBottomMenu(getSortByMenu())
                    },
                    onChangeInstanceClick = {
                        InstancePickerDialogFragment.show(childFragmentManager)
                    },
                    onCommunityLongClick = { communityRef, text ->
                        val url = communityRef?.toUrl(viewModel.apiInstance)
                        if (url != null) {
                            showMoreLinkOptions(url, text)
                            true
                        } else {
                            false
                        }
                    },
                )
            }

            runAfterLayout {
                if (!isBindingAvailable()) return@runAfterLayout

                adapter?.contentMaxWidth = recyclerView.measuredWidth
                adapter?.contentPreferredHeight = recyclerView.measuredHeight
            }

            runOnReady {
                onReady()
            }

            fab.setup(preferences)
            fab.setOnClickListener a@{
                showOverflowMenu()
            }
            fab.setOnLongClickListener {
                when (preferences.homeFabQuickAction) {
                    HomeFabQuickActionIds.CreatePost -> {
                        createMoreMenuActionHandler(context, viewModel.currentCommunityRef.value)(
                            R.id.create_post,
                        )
                        true
                    }

                    HomeFabQuickActionIds.HideRead -> {
                        createMoreMenuActionHandler(context, viewModel.currentCommunityRef.value)(
                            R.id.hide_read,
                        )
                        true
                    }

                    HomeFabQuickActionIds.ToggleNsfwMode -> {
                        createMoreMenuActionHandler(context, viewModel.currentCommunityRef.value)(
                            R.id.toggle_nsfw_mode,
                        )
                        true
                    }

                    else -> false
                }
            }

            requireActivity().onBackPressedDispatcher
                .addCallback(viewLifecycleOwner, onBackPressedHandler)

            slidingPaneController = SlidingPaneController(
                fragment = this@CommunityFragment,
                slidingPaneLayout = slidingPaneLayout,
                childFragmentManager = childFragmentManager,
                viewModel = viewModel,
                globalLayoutMode = preferences.globalLayoutMode,
                retainClosedPosts = preferences.retainLastPost,
                emptyScreenText = getString(R.string.select_a_post),
                fragmentContainerId = R.id.post_fragment_container,
                useSwipeBetweenPosts = preferences.swipeBetweenPosts,
            ).apply {
                onPageSelectedListener = a@{ isOpen ->
                    if (!isBindingAvailable()) {
                        return@a
                    }

                    if (!isOpen) {
                        val lastSelectedPost = viewModel.lastSelectedItem?.leftOrNull()
                        if (lastSelectedPost != null) {
                            // We came from a post...
                            adapter?.highlightPost(lastSelectedPost)
                            viewModel.lastSelectedItem = null
                        }
                    } else {
                        val lastSelectedPost = viewModel.lastSelectedItem?.leftOrNull()
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
                                mainFragment?.setStartPanelLockState(
                                    OverlappingPanelsLayout.LockState.UNLOCKED,
                                )
                            }
                        }
                    }
                }

                onPostOpen = { accountId, postView ->
                    if (postView != null) {
                        if (!postView.read) {
                            moreActionsHelper.onPostRead(
                                postView = postView,
                                delayMs = 0,
                                accountId = accountId,
                                read = true,
                            )
                        }

                        viewModel.updatePost(
                            postView = postView.copy(
                                unread_comments = 0,
                            ),
                        )
                    }
                }

                init()
            }
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

    fun updatePost(postId: PostId, accountId: Long?) {
        viewModel.updatePost(postId, accountId)
    }

    private fun onReady() {
        if (!isBindingAvailable()) return

        val view = binding.root
        val context = requireContext()

        checkNotNull(view.findNavController()) { "NavController was null!" }

        requireMainActivity().apply {
            insetViewAutomaticallyByPaddingAndNavUi(
                viewLifecycleOwner,
                binding.recyclerView,
                applyTopInset = false,
                applyLeftInset = false,
                applyRightInset = false,
            )
            insetViewStartAndEndByPadding(
                viewLifecycleOwner,
                binding.fastScroller,
            )

            val customFabBehavior =
                (binding.fab.layoutParams as? CoordinatorLayout.LayoutParams)
                    ?.behavior as? CustomFabWithBottomNavBehavior

            customFabBehavior?.apply {
                updateBottomNavHeight(getBottomNavHeight().toFloat())
                binding.fab.translationY = -getBottomNavHeight().toFloat()
            }

            insets.observe(viewLifecycleOwner) {
                binding.coordinatorLayout.post {
                    if (!isBindingAvailable()) return@post

                    customFabBehavior?.updateBottomNavHeight(getBottomNavHeight().toFloat())
                    customFabBehavior?.updateBottomInset(it.bottomInset)
                    customFabBehavior?.onDependentViewChanged(
                        binding.coordinatorLayout,
                        binding.fab,
                        binding.coordinatorLayout,
                    )
                }
            }
        }

        with(binding) {
            swipeRefreshLayout.setOnRefreshListener {
                viewModel.fetchCurrentPage(
                    force = true,
                    resetHideRead = true,
                    clearPages = true,
                    scrollToTop = true,
                )
            }
            loadingView.setOnRefreshClickListener {
                viewModel.fetchCurrentPage(true)
            }

            val layoutManager = LinearLayoutManager(context)
            adapter?.viewLifecycleOwner = viewLifecycleOwner
            recyclerView.adapter = adapter
            recyclerView.setHasFixedSize(true)
            recyclerView.setup(animationsHelper)
            recyclerView.layoutManager = layoutManager

            if (preferences.markPostsAsReadOnScroll) {
                recyclerView.addOnScrollListener(
                    object : RecyclerView.OnScrollListener() {
                        override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                            super.onScrolled(recyclerView, dx, dy)

                            val range =
                                layoutManager.findFirstCompletelyVisibleItemPosition()..layoutManager.findLastCompletelyVisibleItemPosition()

                            range.forEach {
                                adapter?.seenItemPositions?.add(it)
                            }
                        }
                    },
                )
            }

            updateDecoratorAndGestureHandler(recyclerView)

            fastScroller.setRecyclerView(recyclerView)

            fun fetchPageIfLoadItem(vararg positions: Int) {
                val items = adapter?.items ?: return

                for (p in positions) {
                    val pageToFetch = (items.getOrNull(p) as? PostListEngineItem.AutoLoadItem)
                        ?.pageToLoad
                        ?: continue

                    viewModel.fetchPage(pageToFetch)
                }
            }

            recyclerView.addOnScrollListener(
                object : RecyclerView.OnScrollListener() {
                    override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                        super.onScrollStateChanged(recyclerView, newState)

                        val firstPos = layoutManager.findFirstVisibleItemPosition()
                        val lastPos = layoutManager.findLastVisibleItemPosition()
                        if (newState == SCROLL_STATE_IDLE) {
                            fetchPageIfLoadItem(
                                firstPos,
                                firstPos - 1,
                                lastPos - 1,
                                lastPos,
                                lastPos + 1,
                            )
                        }
                    }

                    override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                        super.onScrolled(recyclerView, dx, dy)

                        val adapter = adapter ?: return
                        val firstPos = layoutManager.findFirstVisibleItemPosition()
                        val lastPos = layoutManager.findLastVisibleItemPosition()
                        (adapter.items.getOrNull(firstPos) as? PostListEngineItem.VisiblePostItem)
                            ?.pageIndex
                            ?.let { pageIndex ->
                                if (firstPos != 0 && lastPos == adapter.itemCount - 1) {
                                    // firstPos != 0 - ensures that the page is scrollable even
                                    viewModel.setPagePositionAtBottom(pageIndex)
                                } else {
                                    val firstView = layoutManager.findViewByPosition(firstPos)
                                    viewModel.setPagePosition(
                                        pageIndex,
                                        firstPos,
                                        firstView?.top ?: 0,
                                    )
                                }
                            }

                        if (viewModel.infinity) {
                            fetchPageIfLoadItem(
                                firstPos,
                                firstPos - 1,
                                lastPos - 1,
                                lastPos,
                                lastPos + 1,
                            )
                        }

                        viewModel.postListEngine.updateViewingPosition(firstPos, lastPos)
                    }
                },
            )

            rootView.post {
                if (!isBindingAvailable()) return@post
                scheduleStartPostponedTransition(rootView)
            }

            viewModel.loadedPostsData.observe(viewLifecycleOwner) a@{
                when (it) {
                    is StatefulData.Error -> {
                        swipeRefreshLayout.isRefreshing = false

                        if (it.error is LoadNsfwCommunityWhenNsfwDisabled) {
                            recyclerView.visibility = View.GONE
                            loadingView.showErrorText(
                                R.string.error_cannot_load_nsfw_community_when_nsfw_posts_are_hidden,
                            )
                        } else if (it.error is FilterTooAggressiveException) {
                            recyclerView.visibility = View.GONE
                            loadingView.showErrorText(R.string.error_filter_too_aggressive)
                        } else if (it.error is ContentTypeFilterTooAggressiveException) {
                            recyclerView.visibility = View.GONE
                            loadingView.showErrorText(
                                R.string.error_content_type_filter_too_aggressive,
                            )
                        } else if (viewModel.infinity) {
                            loadingView.hideAll()
                            adapter?.onItemsChanged()
                        } else {
                            recyclerView.visibility = View.GONE
                            loadingView.showDefaultErrorMessageFor(it.error)
                        }
                    }

                    is StatefulData.Loading -> {
                        if (!swipeRefreshLayout.isRefreshing) {
                            loadingView.showProgressBar()
                        }
                    }

                    is StatefulData.NotStarted -> {
                        loadingView.hideAll()
                    }

                    is StatefulData.Success -> {
                        loadingView.hideAll()

                        val adapter = adapter ?: return@a

                        if (it.data.hideReadCount != null) {
                            Snackbar
                                .make(
                                    coordinatorLayout,
                                    getString(
                                        R.string.posts_hidden_format,
                                        PrettyPrintUtils.defaultDecimalFormat.format(
                                            it.data.hideReadCount,
                                        ),
                                    ),
                                    Snackbar.LENGTH_LONG,
                                )
                                .show()
                        }

                        if (it.data.isReadPostUpdate) {
                            adapter.onItemsChanged(animate = false)
                        } else {
                            adapter.seenItemPositions.clear()
                            adapter.onItemsChanged()

                            if (it.data.scrollToTop) {
                                recyclerView.scrollToPosition(0)
                            } else {
                                val biggestPageIndex = viewModel.postListEngine.biggestPageIndex
                                if (biggestPageIndex != null && !viewModel.infinity) {
                                    val pagePosition = viewModel.getPagePosition(
                                        biggestPageIndex,
                                    )
                                    if (pagePosition.isAtBottom) {
                                        (recyclerView.layoutManager as LinearLayoutManager)
                                            .scrollToPositionWithOffset(adapter.itemCount - 1, 0)
                                    } else if (pagePosition.itemIndex != 0 || pagePosition.offset != 0) {
                                        (recyclerView.layoutManager as LinearLayoutManager)
                                            .scrollToPositionWithOffset(
                                                pagePosition.itemIndex,
                                                pagePosition.offset,
                                            )
                                    } else {
                                        recyclerView.scrollToPosition(0)
                                        lemmyAppBarController?.setExpanded(true)
                                    }
                                }
                            }

                            swipeRefreshLayout.isRefreshing = false
                            recyclerView.visibility = View.VISIBLE

                            if (viewModel.postListEngine.items.isEmpty()) {
                                loadingView.showErrorText(R.string.no_posts)
                            }
                        }
                    }
                }
            }
            viewModel.onHidePost.observe(viewLifecycleOwner) {
                val hiddenPost = it ?: return@observe

                viewModel.onHidePost.postValue(null)

                Snackbar
                    .make(
                        coordinatorLayout,
                        R.string.post_hidden,
                        Snackbar.LENGTH_LONG,
                    )
                    .setAction(R.string.undo) {
                        viewModel.unhidePost(
                            id = hiddenPost.postId,
                            instance = hiddenPost.instance,
                            postView = null,
                        )
                    }
                    .show()
            }

            viewLifecycleOwner.lifecycleScope.launch {
                userTagsManager.onChangedFlow.collect {
                    adapter?.notifyDataSetChanged()
                }
            }

            if (adapter?.items.isNullOrEmpty()) {
                viewModel.fetchInitialPage()
            }
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
                context = context,
                recyclerView = binding.recyclerView,
                onActionSelected = { action, vh ->
                    val fetchedPost = vh.itemView.getTag(R.id.fetched_post) as? FetchedPost
                        ?: return@LemmySwipeActionCallback
                    val postView = fetchedPost.postView

                    when (action.id) {
                        PostGestureAction.Upvote -> {
                            moreActionsHelper.vote(postView, 1, toggle = true)
                        }

                        PostGestureAction.Downvote -> {
                            moreActionsHelper.vote(postView, -1, toggle = true)
                        }

                        PostGestureAction.Bookmark -> {
                            moreActionsHelper.savePost(postView.post.id, save = true)
                        }

                        PostGestureAction.Reply -> {
                            slidingPaneController?.openPost(
                                instance = fetchedPost.source.instance ?: viewModel.apiInstance,
                                id = postView.post.id,
                                reveal = false,
                                post = postView,
                                jumpToComments = false,
                                currentCommunity = viewModel.currentCommunityRef.value,
                                accountId = fetchedPost.source.accountId,
                                videoState = null,
                            )

                            AddOrEditCommentFragment.showReplyDialog(
                                instance = viewModel.apiInstance,
                                postOrCommentView = Either.Left(postView),
                                fragmentManager = childFragmentManager,
                                accountId = fetchedPost.source.accountId,
                            )
                        }

                        PostGestureAction.Hide -> {
                            viewModel.hidePost(id = postView.post.id, postView = postView)
                        }

                        PostGestureAction.MarkAsRead -> {
                            moreActionsHelper.togglePostRead(
                                postView = postView,
                                delayMs = 250,
                                accountId = fetchedPost.source.accountId,
                            )
                        }
                    }
                },
                gestureSize = preferences.postGestureSize,
                hapticsEnabled = preferences.hapticsEnabled,
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
        this.hapticsEnabled = preferences.hapticsEnabled
        this.actions = preferences.getPostSwipeActions(context)
    }

    override fun onResume() {
        super.onResume()

        requireMainActivity().apply {
            setupForFragment<CommunityFragment>()
        }

        viewModel.changeCommunity(args.communityRef)

        runOnReady {
            val customAppBarController = lemmyAppBarController ?: return@runOnReady

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
            customAppBarController.setUseHeader(preferences.usePostsFeedHeader)
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

            adapter?.updateWithPreferences(preferences)
            adapter?.updateNsfwMode(nsfwModeManager)

            onSelectedLayoutChanged()

            viewModel.recheckPreferences()
            viewModel.recheckNsfwMode()

            binding.fastScroller.visibility = if (preferences.postFeedShowScrollBar) {
                View.VISIBLE
            } else {
                View.GONE
            }
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
        viewModel.createState()?.writeToBundle(outState, json)
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
            is CommunitySortOrder.TopOrder -> _sortByMenu.setChecked(R.id.sort_order_top)
            else -> _sortByMenu.setChecked(viewModel.getCurrentSortOrder().toApiSortOrder().toId())
        }

        return _sortByMenu
    }
    private fun getSortByTopMenu(): BottomMenu {
        _sortByTopMenu.setChecked(viewModel.getCurrentSortOrder().toApiSortOrder().toId())

        return _sortByTopMenu
    }

    private fun getDefaultSortOrderSortByMenu(): BottomMenu {
        when (viewModel.getCurrentSortOrder()) {
            is CommunitySortOrder.TopOrder -> _defaultSortOrderSortByMenu.setChecked(
                R.id.sort_order_top,
            )
            else -> _defaultSortOrderSortByMenu.setChecked(
                viewModel.getCurrentSortOrder().toApiSortOrder().toId(),
            )
        }

        return _defaultSortOrderSortByMenu
    }
    private fun getDefaultSortOrderSortByTopMenu(): BottomMenu {
        _defaultSortOrderSortByTopMenu.setChecked(
            viewModel.getCurrentSortOrder().toApiSortOrder().toId(),
        )

        return _defaultSortOrderSortByTopMenu
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
            CommunityLayout.ListWithCards ->
                _layoutSelectorMenu.setChecked(R.id.layout_list_with_card)
            CommunityLayout.FullWithCards ->
                _layoutSelectorMenu.setChecked(R.id.layout_full_with_card)
        }

        return _layoutSelectorMenu
    }

    private fun showOverflowMenu() {
        val context = context ?: return

        val fullAccount = moreActionsHelper.fullAccount
        val currentCommunityRef = viewModel.currentCommunityRef.value
        val currentDefaultPage = preferences.defaultPage
        val isBookmarked =
            if (currentCommunityRef == null) {
                false
            } else {
                userCommunitiesManager.isCommunityBookmarked(currentCommunityRef)
            }
        val isCurrentPageDefault = currentCommunityRef == currentDefaultPage

        val bottomMenu = BottomMenu(context).apply {
            val isCommunityMenu = currentCommunityRef is CommunityRef.CommunityRefByName
            val communityRefByName = currentCommunityRef as? CommunityRef.CommunityRefByName

            if (isCommunityMenu) {
                setTitle(R.string.community_options)
            } else {
                setTitle(R.string.instance_options)
            }

            addItemWithIcon(R.id.create_post, R.string.create_post, R.drawable.baseline_add_24)

            addItemWithIcon(R.id.ca_share, R.string.share, R.drawable.baseline_share_24)
            addItemWithIcon(R.id.hide_read, R.string.hide_read, R.drawable.baseline_clear_all_24)

            addItemWithIcon(R.id.sort, R.string.sort, R.drawable.baseline_sort_24)

            when (currentCommunityRef) {
                is CommunityRef.CommunityRefByName -> {
                    addItemWithIcon(
                        id = R.id.community_info,
                        title = R.string.community_info,
                        icon = R.drawable.ic_default_community,
                    )
                }
                is CommunityRef.ModeratedCommunities -> {
                    addItemWithIcon(
                        id = R.id.feed_info,
                        title = R.string.feed_info,
                        icon = R.drawable.baseline_dynamic_feed_24,
                    )
                }
                is CommunityRef.MultiCommunity -> {
                    addItemWithIcon(
                        id = R.id.community_info,
                        title = R.string.multi_community_info,
                        icon = R.drawable.baseline_dynamic_feed_24,
                    )
                }
                is CommunityRef.Subscribed -> {
                    addItemWithIcon(
                        id = R.id.feed_info,
                        title = R.string.feed_info,
                        icon = R.drawable.baseline_dynamic_feed_24,
                    )
                }
                is CommunityRef.AllSubscribed -> {
                    addItemWithIcon(
                        id = R.id.feed_info,
                        title = R.string.feed_info,
                        icon = R.drawable.baseline_dynamic_feed_24,
                    )
                }
                is CommunityRef.Local,
                is CommunityRef.All,
                null,
                -> {
                    addItemWithIcon(
                        id = R.id.community_info,
                        title = R.string.instance_info,
                        icon = R.drawable.baseline_web_24,
                    )
                }
            }

            if (!isCurrentPageDefault) {
                addItemWithIcon(
                    id = R.id.set_as_default,
                    title = R.string.set_as_home_page,
                    icon = R.drawable.baseline_home_24,
                )
            }

            if (currentCommunityRef != null) {
                if (isBookmarked) {
                    addItemWithIcon(
                        id = R.id.remove_bookmark,
                        title = R.string.remove_bookmark,
                        icon = R.drawable.baseline_bookmark_remove_24,
                    )
                } else {
                    if (isCommunityMenu) {
                        addItemWithIcon(
                            id = R.id.add_bookmark,
                            title = R.string.bookmark_community,
                            icon = R.drawable.baseline_bookmark_add_24,
                        )
                    } else {
                        addItemWithIcon(
                            id = R.id.add_bookmark,
                            title = R.string.bookmark_feed,
                            icon = R.drawable.baseline_bookmark_add_24,
                        )
                    }
                }
            }

            if (isCommunityMenu) {
                val isSubbed = accountInfoManager.subscribedCommunities.value
                    .any { it.toCommunityRef() == currentCommunityRef }

                if (isSubbed) {
                    addItemWithIcon(
                        id = R.id.unsubscribe,
                        title = R.string.unsubscribe,
                        icon = R.drawable.baseline_subscriptions_remove_24,
                    )
                } else {
                    addItemWithIcon(
                        id = R.id.subscribe,
                        title = R.string.subscribe,
                        icon = R.drawable.baseline_subscriptions_add_24,
                    )
                }
            }
            if (communityRefByName != null && fullAccount != null) {
                if (fullAccount.isCommunityBlocked(communityRefByName)) {
                    addItemWithIcon(
                        id = R.id.unblock_community,
                        title = getString(
                            R.string.unblock_this_community_format,
                            communityRefByName.getName(context),
                        ),
                        icon = R.drawable.ic_default_community,
                    )
                } else {
                    addItemWithIcon(
                        id = R.id.block_community,
                        title = context.getString(
                            R.string.block_this_community_format,
                            communityRefByName.getName(context),
                        ),
                        icon = R.drawable.baseline_block_24,
                    )
                }
            }

            addDivider()

            addItemWithIcon(R.id.layout, R.string.layout, R.drawable.baseline_view_comfy_24)
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
            if (nsfwModeManager.nsfwModeEnabled.value) {
                addItemWithIcon(
                    id = R.id.disable_nsfw_mode,
                    title = getString(R.string.disable_nsfw_mode),
                    icon = R.drawable.ic_no_nsfw_24,
                )
            } else {
                addItemWithIcon(
                    id = R.id.enable_nsfw_mode,
                    title = getString(R.string.enable_nsfw_mode),
                    icon = R.drawable.ic_nsfw_24,
                )
            }

            addItemWithIcon(
                id = R.id.make_available_offline,
                title = getString(R.string.make_available_offline),
                icon = R.drawable.baseline_download_for_offline_24,
            )

            addItemWithIcon(
                id = R.id.go_to,
                title = getString(R.string.go_to),
                icon = R.drawable.baseline_arrow_forward_24,
            )
            addItemWithIcon(
                id = R.id.give_feedback,
                title = getString(R.string.help_and_feedback),
                icon = R.drawable.outline_feedback_24,
            )

            if (currentCommunityRef != null) {
                addDivider()
                addItemWithIcon(
                    id = R.id.per_community_settings,
                    title = getString(
                        R.string.per_community_settings_format,
                        currentCommunityRef.getName(context),
                    ),
                    icon = R.drawable.ic_community_24,
                )
            }

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
                                R.id.filteredPostsAndCommentsTabbedFragment,
                                getString(R.string.saved),
                                R.drawable.baseline_bookmark_24,
                            )
                        }
                        NavBarDestinations.Search -> {
                            addItemWithIcon(
                                R.id.searchHomeFragment,
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
                        NavBarDestinations.You -> {
                            addItemWithIcon(
                                R.id.youFragment,
                                getString(R.string.you),
                                R.drawable.outline_account_circle_24,
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
                createMoreMenuActionHandler(context, currentCommunityRef)(menuItem.id)
            }
        }

        getMainActivity()?.showBottomMenu(bottomMenu, expandFully = false)
    }

    private fun createMoreMenuActionHandler(
        context: Context,
        currentCommunityRef: CommunityRef?,
    ): (Int) -> Unit = a@{ actionId ->
        when (actionId) {
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
                    is CommunityRef.AllSubscribed -> {}
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
                    OldAlertDialogFragment.Builder()
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
                    (adapter?.items?.getOrNull(it) as? PostListEngineItem.VisiblePostItem)
                        ?.fetchedPost?.postView?.post?.id
                }
                viewModel.onHideRead(anchors)
            }

            R.id.sort -> {
                getMainActivity()?.showBottomMenu(getSortByMenu())
            }

            R.id.set_as_default -> {
                currentCommunityRef ?: return@a
                viewModel.setDefaultPage(currentCommunityRef)

                Snackbar.make(
                    binding.coordinatorLayout,
                    R.string.home_page_set,
                    Snackbar.LENGTH_LONG,
                ).show()
            }
            R.id.layout -> {
                getMainActivity()?.showBottomMenu(getLayoutMenu())
            }

            R.id.feed_info,
            R.id.community_info,
            -> {
                currentCommunityRef ?: return@a
                getMainActivity()?.showCommunityInfo(currentCommunityRef)
            }

            R.id.my_communities -> {
                (parentFragment?.parentFragment as? MainFragment)?.expandStartPane()
            }

            R.id.add_bookmark -> {
                currentCommunityRef ?: return@a

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
                        is CommunityRef.AllSubscribed,
                        -> null
                    },
                )
            }

            R.id.remove_bookmark -> {
                currentCommunityRef ?: return@a

                if (currentCommunityRef is CommunityRef.MultiCommunity) {
                    MaterialAlertDialogBuilder(context)
                        .setTitle(R.string.prompt_delete_multicommunity)
                        .setMessage(R.string.prompt_delete_multicommunity_desc)
                        .setPositiveButton(R.string.delete) { _, _ ->
                            userCommunitiesManager.removeCommunity(currentCommunityRef)
                        }
                        .setNegativeButton(R.string.cancel) { _, _ -> }
                        .show()
                } else {
                    userCommunitiesManager.removeCommunity(currentCommunityRef)
                }
            }
            R.id.browse_communities -> {
                lemmyAppBarController?.showCommunitySelector()
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
                getMainActivity()?.navigateTopLevel(actionId)
            }
            R.id.filteredPostsAndCommentsTabbedFragment -> {
                getMainActivity()?.navigateTopLevel(actionId)
            }
            R.id.searchHomeFragment -> {
                getMainActivity()?.navigateTopLevel(actionId)
            }
            R.id.historyFragment -> {
                getMainActivity()?.navigateTopLevel(actionId)
            }
            R.id.inboxTabbedFragment -> {
                getMainActivity()?.navigateTopLevel(actionId)
            }
            R.id.back_to_the_beginning -> {
                binding.recyclerView.scrollToPosition(0)
            }
            R.id.per_community_settings -> {
                currentCommunityRef ?: return@a
                showPerCommunitySettings(currentCommunityRef)
            }
            R.id.subscribe,
            R.id.unsubscribe,
            -> {
                if (currentCommunityRef is CommunityRef.CommunityRefByName) {
                    moreActionsHelper.updateSubscription(
                        currentCommunityRef,
                        actionId == R.id.subscribe,
                    )
                }
            }
            R.id.enable_nsfw_mode,
            R.id.disable_nsfw_mode,
            R.id.toggle_nsfw_mode,
            -> {
                val newValue = if (actionId == R.id.enable_nsfw_mode) {
                    true
                } else if (actionId == R.id.disable_nsfw_mode) {
                    false
                } else {
                    !nsfwModeManager.nsfwModeEnabled.value
                }

                nsfwModeManager.nsfwModeEnabled.value = newValue

                adapter?.updateNsfwMode(nsfwModeManager)
                viewModel.recheckNsfwMode()
            }
            R.id.make_available_offline -> {
                if (currentCommunityRef != null) {
                    MakeOfflineDialogFragment.newInstance(currentCommunityRef)
                        .show(childFragmentManager, "MakeOfflineDialogFragment")
                }
            }
            R.id.go_to -> {
                GoToDialogFragment.show(childFragmentManager)
            }
            R.id.block_community -> {
                if (currentCommunityRef is CommunityRef.CommunityRefByName) {
                    moreActionsHelper.blockCommunity(currentCommunityRef, true)
                }
            }
            R.id.unblock_community -> {
                if (currentCommunityRef is CommunityRef.CommunityRefByName) {
                    moreActionsHelper.blockCommunity(currentCommunityRef, false)
                }
            }
            R.id.give_feedback -> {
                showHelpAndFeedbackOptions()
            }
        }
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
            addItemWithIcon(
                R.id.sort_order,
                getString(R.string.sort_by),
                R.drawable.baseline_sort_24,
            )
            addDivider()
            addItemWithIcon(
                R.id.reset_settings,
                R.string.reset_settings,
                R.drawable.baseline_reset_wrench_24,
            )

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
                    R.id.sort_order -> {
                        getMainActivity()?.showBottomMenu(getDefaultSortOrderSortByMenu())
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

    override fun onPositiveClick(dialog: OldAlertDialogFragment, tag: String?) {
    }

    override fun onNegativeClick(dialog: OldAlertDialogFragment, tag: String?) {
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

    private fun makeLayoutSelectorMenu(onLayoutSelected: (CommunityLayout) -> Unit): BottomMenu =
        BottomMenu(requireContext()).apply {
            addItemWithIcon(R.id.layout_list, R.string.list, R.drawable.baseline_view_list_24)
            addItemWithIcon(
                R.id.layout_large_list,
                R.string.large_list,
                R.drawable.baseline_view_list_24,
            )
            addItemWithIcon(
                R.id.layout_list_with_card,
                R.string.list_with_cards,
                R.drawable.baseline_view_list_24,
            )
            addItemWithIcon(R.id.layout_compact, R.string.compact, R.drawable.baseline_list_24)
            addItemWithIcon(R.id.layout_card, R.string.card, R.drawable.baseline_article_24)
            addItemWithIcon(R.id.layout_card2, R.string.card2, R.drawable.baseline_article_24)
            addItemWithIcon(R.id.layout_card3, R.string.card3, R.drawable.baseline_article_24)
            addItemWithIcon(R.id.layout_full, R.string.full, R.drawable.baseline_view_day_24)
            addItemWithIcon(
                R.id.layout_full_with_card,
                R.string.full_with_cards,
                R.drawable.outline_cards_24,
            )
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
                    R.id.layout_list_with_card ->
                        onLayoutSelected(CommunityLayout.ListWithCards)
                    R.id.layout_full_with_card ->
                        onLayoutSelected(CommunityLayout.FullWithCards)
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
