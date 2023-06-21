package com.idunnololz.summit.lemmy.community

import android.content.Context
import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.widget.PopupMenu
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.doOnPreDraw
import androidx.fragment.app.viewModels
import androidx.navigation.NavOptions
import androidx.navigation.findNavController
import androidx.navigation.fragment.FragmentNavigatorExtras
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.google.android.material.button.MaterialButton
import com.google.android.material.snackbar.Snackbar
import com.idunnololz.summit.R
import com.idunnololz.summit.account_ui.AccountsAndSettingsDialogFragment
import com.idunnololz.summit.api.dto.PostType
import com.idunnololz.summit.api.dto.PostView
import com.idunnololz.summit.account_ui.PreAuthDialogFragment
import com.idunnololz.summit.account_ui.SignInNavigator
import com.idunnololz.summit.alert.AlertDialogFragment
import com.idunnololz.summit.databinding.FragmentSubredditBinding
import com.idunnololz.summit.databinding.MainFooterItemBinding
import com.idunnololz.summit.history.HistoryManager
import com.idunnololz.summit.history.HistorySaveReason
import com.idunnololz.summit.lemmy.CommunitySortOrder
import com.idunnololz.summit.lemmy.LemmyContentHelper
import com.idunnololz.summit.lemmy.LemmyHeaderHelper
import com.idunnololz.summit.lemmy.CommunityViewState
import com.idunnololz.summit.lemmy.utils.getFormattedTitle
import com.idunnololz.summit.lemmy.getShortDesc
import com.idunnololz.summit.lemmy.utils.getUpvoteText
import com.idunnololz.summit.lemmy.toCommunity
import com.idunnololz.summit.lemmy.utils.VoteUiHandler
import com.idunnololz.summit.lemmy.utils.bind
import com.idunnololz.summit.main.MainActivity
import com.idunnololz.summit.main.MainFragment
import com.idunnololz.summit.offline.OfflineManager
import com.idunnololz.summit.preferences.Preferences
import com.idunnololz.summit.reddit.RedditUtils
import com.idunnololz.summit.util.BaseFragment
import com.idunnololz.summit.util.BottomMenu
import com.idunnololz.summit.util.CustomDividerItemDecoration
import com.idunnololz.summit.util.PreferenceUtil
import com.idunnololz.summit.util.SharedElementTransition
import com.idunnololz.summit.util.Size
import com.idunnololz.summit.util.StatefulData
import com.idunnololz.summit.util.Utils
import com.idunnololz.summit.util.ext.addDefaultAnim
import com.idunnololz.summit.util.ext.forceShowIcons
import com.idunnololz.summit.util.ext.getColorCompat
import com.idunnololz.summit.util.ext.navigateSafe
import com.idunnololz.summit.util.ext.showAllowingStateLoss
import com.idunnololz.summit.util.recyclerView.ViewBindingViewHolder
import com.idunnololz.summit.util.recyclerView.getBinding
import com.idunnololz.summit.video.ExoPlayerManager
import com.idunnololz.summit.view.RedditHeaderView
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class CommunityFragment : BaseFragment<FragmentSubredditBinding>(), SignInNavigator,
    AlertDialogFragment.AlertDialogFragmentListener {

    companion object {
        private const val TAG = "SubredditFragment"
    }

    private val args: CommunityFragmentArgs by navArgs()

    private val viewModel: CommunityViewModel by viewModels()

    private var adapter: ListingItemAdapter? = null

    private var shouldScrollToTopAfterFresh = false

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
                    R.id.sort_order_active ->
                        viewModel.setSortOrder(CommunitySortOrder.Active)
                    R.id.sort_order_hot ->
                        viewModel.setSortOrder(CommunitySortOrder.Hot)
                    R.id.sort_order_top ->
                        getMainActivity()?.showBottomMenu(getSortByTopMenu())
                    R.id.sort_order_new ->
                        viewModel.setSortOrder(CommunitySortOrder.New)
                    R.id.sort_order_old ->
                        viewModel.setSortOrder(CommunitySortOrder.Old)
                    R.id.sort_order_most_comments ->
                        viewModel.setSortOrder(CommunitySortOrder.MostComments)
                    R.id.sort_order_new_comments ->
                        viewModel.setSortOrder(CommunitySortOrder.NewComments)
                }
            }
        }
    }

    private val _sortByTopMenu: BottomMenu by lazy {
        BottomMenu(requireContext()).apply {
            addItem(R.id.sort_order_top_day, R.string.time_frame_today)
            addItem(R.id.sort_order_top_week, R.string.time_frame_this_week)
            addItem(R.id.sort_order_top_month, R.string.time_frame_this_month)
            addItem(R.id.sort_order_top_year, R.string.time_frame_this_year)
            addItem(R.id.sort_order_top_all_time, R.string.time_frame_all_time)
            setTitle(R.string.sort_by_top)

            setOnMenuItemClickListener { menuItem ->
                when(menuItem.id) {
                    R.id.sort_order_top_day ->
                        viewModel.setSortOrder(
                            CommunitySortOrder.TopOrder(CommunitySortOrder.TimeFrame.Today))
                    R.id.sort_order_top_week ->
                        viewModel.setSortOrder(
                            CommunitySortOrder.TopOrder(CommunitySortOrder.TimeFrame.ThisWeek))
                    R.id.sort_order_top_month ->
                        viewModel.setSortOrder(
                            CommunitySortOrder.TopOrder(CommunitySortOrder.TimeFrame.ThisMonth))
                    R.id.sort_order_top_year ->
                        viewModel.setSortOrder(
                            CommunitySortOrder.TopOrder(CommunitySortOrder.TimeFrame.ThisYear))
                    R.id.sort_order_top_all_time ->
                        viewModel.setSortOrder(
                            CommunitySortOrder.TopOrder(CommunitySortOrder.TimeFrame.AllTime))
                }
            }
        }
    }

    @Inject
    lateinit var preferences: Preferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val context = requireContext()
        if (adapter == null) {
            adapter = ListingItemAdapter(
                context,
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
                viewModel.voteUiHandler
            )
            onSelectedLayoutChanged()
        }

        viewModel.changeCommunity(args.communityRef)

        if (savedInstanceState != null) {
            restoreState(CommunityViewState.restoreFromBundle(savedInstanceState), reload = false)
        }

        val callback = object : OnBackPressedCallback(true /* enabled by default */) {
            override fun handleOnBackPressed() {
                if (viewModel.currentPageIndex.value != 0) {
                    viewModel.fetchPrevPage()
                } else {
                    isEnabled = false
                    requireActivity().onBackPressed()
                }
            }
        }

        sharedElementEnterTransition = SharedElementTransition()
        sharedElementReturnTransition = SharedElementTransition()

        requireActivity().onBackPressedDispatcher.addCallback(this, callback)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        postponeEnterTransition()

        requireMainActivity().apply {
            setupForFragment<CommunityFragment>()
        }

        setBinding(FragmentSubredditBinding.inflate(inflater, container, false))

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.doOnPreDraw {
            adapter?.contentMaxWidth = binding.recyclerView.width
        }

        runOnReady {
            onReady()
        }
    }

    private fun onReady() {
        val view = binding.root
        checkNotNull(view.findNavController())

        val mainActivity = requireMainActivity()
        mainActivity.apply {
            headerOffset.observe(viewLifecycleOwner) {
                if (it != null)
                    getView()?.translationY = it.toFloat()
            }
        }

        mainActivity.insetViewExceptTopAutomaticallyByPadding(viewLifecycleOwner, binding.recyclerView)

        val context = requireContext()

        binding.swipeRefreshLayout.setOnRefreshListener {
            shouldScrollToTopAfterFresh = true
            viewModel.fetchCurrentPage(true)
            binding.recyclerView.scrollToPosition(0)
        }
        binding.loadingView.setOnRefreshClickListener {
            viewModel.fetchCurrentPage(true)
        }

        binding.recyclerView.adapter = adapter
        binding.recyclerView.setHasFixedSize(true)
        binding.recyclerView.layoutManager = LinearLayoutManager(context)
        binding.recyclerView.addItemDecoration(
            CustomDividerItemDecoration(
                context,
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
        binding.fastScroller.setRecyclerView(binding.recyclerView)

        binding.recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)

                val adapter = adapter ?: return
                val pageIndex = adapter.pageIndex ?: return
                val layoutManager = (recyclerView.layoutManager as LinearLayoutManager)
                val lastPos = layoutManager.findLastVisibleItemPosition()
                if (lastPos == adapter.itemCount - 1) {
                    viewModel.setPagePositionAtBottom(pageIndex)
                } else {
                    val firstPos = layoutManager.findFirstVisibleItemPosition()
                    val firstView = layoutManager.findViewByPosition(firstPos)
                    viewModel.setPagePosition(pageIndex, firstPos, firstView?.top ?: 0)
                }
            }
        })

        setupMainActivityButtons()
        scheduleStartPostponedTransition(binding.rootView)

        viewModel.loadedPostsData.observe(viewLifecycleOwner) a@{
            when (it) {
                is StatefulData.Error -> {
                    binding.swipeRefreshLayout.isRefreshing = false
                    binding.recyclerView.visibility = View.GONE
                    binding.loadingView.showDefaultErrorMessageFor(it.error)
                }
                is StatefulData.Loading -> {
                    binding.loadingView.showProgressBar()
                }
                is StatefulData.NotStarted -> {}
                is StatefulData.Success -> {
                    val adapter = adapter ?: return@a

                    adapter.setItems(it.data.pageIndex, it.data)

                    adapter.curDataSource = it.data.instance

                    if (shouldScrollToTopAfterFresh) {
                        shouldScrollToTopAfterFresh = false
                        binding.recyclerView.scrollToPosition(0)
                    } else {
                        val pagePosition = viewModel.getPagePosition(it.data.pageIndex)
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
                            getMainActivity()?.showCustomAppBar()
                        }
                    }

                    binding.swipeRefreshLayout.isRefreshing = false
                    binding.recyclerView.visibility = View.VISIBLE
                    binding.loadingView.hideAll()
                }
            }
        }

        viewModel.accountChanged.observe(viewLifecycleOwner) {
            shouldScrollToTopAfterFresh = true
            adapter?.reset()
        }

        if (adapter?.getItems().isNullOrEmpty()) {
            viewModel.fetchCurrentPage()
        }
    }

    override fun onResume() {
        super.onResume()

        runOnReady {
            val customAppBarController = requireMainActivity().getCustomAppBarController()

            viewModel.currentCommunityRef.observe(viewLifecycleOwner) {
                val currentDefaultPage = preferences.getDefaultPage()
                customAppBarController.setCommunity(it, it == currentDefaultPage)
            }
            viewModel.currentPageIndex.observe(viewLifecycleOwner) {
                Log.d(TAG, "Current page: $it")
                customAppBarController.setPageIndex(it) { pageIndex ->
                    viewModel.fetchPage(pageIndex)
                }
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        viewModel.createState()?.writeToBundle(outState)
        super.onSaveInstanceState(outState)
    }

    override fun onPause() {
        val context = requireContext()
        val viewState = viewModel.createState()
        if (viewState != null) {
            HistoryManager.instance.recordSubredditState(
                tabId = MainFragment.getIdFromTag(parentFragment?.tag ?: ""),
                saveReason = HistorySaveReason.LEAVE_SCREEN,
                state = viewState,
                shortDesc = viewState.getShortDesc(context)
            )
        }

        super.onPause()
    }

    override fun onDestroyView() {
        Log.d(TAG, "onDestroyView()")
        super.onDestroyView()
    }

    fun restoreState(state: CommunityViewState?, reload: Boolean) {
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
                }
            }
            else -> {}
        }

        return _sortByTopMenu
    }

    private fun setupMainActivityButtons() {
        (activity as? MainActivity)?.apply {

            val context = this

            getCustomAppBarController().setup(
                communitySelectedListener = { controller, communityRef ->
                    val action = CommunityFragmentDirections.actionSubredditFragmentSwitchSubreddit(
                        communityRef = communityRef
                    )
                    findNavController().navigate(action)
                    Utils.hideKeyboard(activity)
                    controller.hide()
                },
                abOverflowClickListener = {
                    PopupMenu(context, it).apply {
                        inflate(R.menu.menu_fragment_main)
                        forceShowIcons()

                        val selectedItem = when (PreferenceUtil.getSubredditLayout()) {
                            CommunityLayout.LIST -> {
                                menu.findItem(R.id.layoutList)
                            }

                            CommunityLayout.CARD -> {
                                menu.findItem(R.id.layoutCard)
                            }

                            CommunityLayout.FULL -> {
                                menu.findItem(R.id.layoutFull)
                            }
                        }

                        menu.findItem(R.id.layoutCard).isVisible = false

                        selectedItem.title = SpannableString(selectedItem.title).apply {
                            setSpan(
                                ForegroundColorSpan(context.getColorCompat(R.color.colorPrimary)),
                                0,
                                length,
                                0
                            )
                            setSpan(StyleSpan(Typeface.BOLD), 0, length, 0)
                        }

                        val currentCommunityRef = viewModel.currentCommunityRef.value
                        val currentDefaultPage = preferences.getDefaultPage()

                        menu.findItem(R.id.set_as_default).isVisible =
                            currentCommunityRef != null && currentCommunityRef != currentDefaultPage

                        setOnMenuItemClickListener {
                            when (it.itemId) {
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
                                    true
                                }

                                R.id.sort -> {
                                    showBottomMenu(getSortByMenu())
                                    true
                                }

                                R.id.layoutList -> {
                                    PreferenceUtil.setSubredditLayout(CommunityLayout.LIST)
                                    onSelectedLayoutChanged()
                                    true
                                }

                                R.id.layoutCard -> {
                                    PreferenceUtil.setSubredditLayout(CommunityLayout.CARD)
                                    onSelectedLayoutChanged()
                                    true
                                }

                                R.id.layoutFull -> {
                                    PreferenceUtil.setSubredditLayout(CommunityLayout.FULL)
                                    onSelectedLayoutChanged()
                                    true
                                }

                                R.id.set_as_default -> {
                                    if (currentCommunityRef != null) {
                                        viewModel.setDefaultPage(currentCommunityRef)

                                        Snackbar.make(
                                            requireMainActivity().getSnackbarContainer(),
                                            R.string.home_page_set,
                                            Snackbar.LENGTH_LONG
                                        ).show()
                                    }
                                    true
                                }

                                else -> false
                            }
                        }
                    }.show()
                },
                onAccountClick = {
                    AccountsAndSettingsDialogFragment.newInstance()
                        .showAllowingStateLoss(childFragmentManager, "AccountsDialogFragment")
                })
        }
    }

    override fun navigateToSignInScreen() {
        val direction = CommunityFragmentDirections.actionCommunityFragmentToLogin()
        findNavController().navigateSafe(direction)
    }

    override fun onPositiveClick(dialog: AlertDialogFragment, tag: String?) {
    }

    override fun onNegativeClick(dialog: AlertDialogFragment, tag: String?) {
        if (tag == "onInstanceMismatch") {
            dialog.dismiss()
            viewModel.resetToAccountInstance()
        }
    }

    private fun onSelectedLayoutChanged() {
        adapter?.setLayout(PreferenceUtil.getSubredditLayout())
    }

    private sealed class Item {
        class PostItem(
            val postView: PostView,
            val instance: String,
        ) : Item()

        class FooterItem(val hasMore: Boolean) : Item()
    }

    private class ListingItemViewHolder(
        itemView: View
    ) : RecyclerView.ViewHolder(itemView) {
        val headerContainer: RedditHeaderView = itemView.findViewById(R.id.headerContainer)
        val image: ImageView? = itemView.findViewById(R.id.image)
        val title: TextView = itemView.findViewById(R.id.title)
        val commentButton: MaterialButton = itemView.findViewById(R.id.commentButton)
        val upvoteCount: TextView = itemView.findViewById(R.id.upvoteCount)
        val upvoteButton: ImageView = itemView.findViewById(R.id.upvoteButton)
        val downvoteButton: ImageView = itemView.findViewById(R.id.downvoteButton)
        val linkTypeImage: ImageView? = itemView.findViewById(R.id.linkTypeImage)
        val iconImage: ImageView? = itemView.findViewById(R.id.iconImage)
        val fullContentContainerView: ViewGroup = itemView.findViewById(R.id.fullContent)
    }

    private inner class ListingItemAdapter(
        private val context: Context,
        private val onNextClick: () -> Unit,
        private val onPrevClick: () -> Unit,
        private val onSignInRequired: () -> Unit,
        private val onInstanceMismatch: (String, String) -> Unit,
        private val voteUiHandler: VoteUiHandler,
    ) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

        var pageIndex: Int? = null
        private var rawData: CommunityViewModel.LoadedPostsData? = null
        private val inflater = LayoutInflater.from(context)

        private var items: List<Item> = listOf()

        private var expandedItems = mutableSetOf<String>()

        /**
         * Set of items that is hidden by default but is reveals (ie. nsfw or spoiler tagged)
         */
        private var revealedItems = mutableSetOf<String>()

        private val offlineManager = OfflineManager.instance

        private val lemmyHeaderHelper = LemmyHeaderHelper(context)
        private val lemmyContentHelper = LemmyContentHelper(
            context,
            this@CommunityFragment,
            offlineManager,
            ExoPlayerManager.get(this@CommunityFragment)
        )

        private val postImageWidth: Int = (Utils.getScreenWidth(context) * 0.2).toInt()

        private val tempSize = Size()

        private var layout: CommunityLayout = CommunityLayout.LIST

        var curDataSource: String? = null

        var contentMaxWidth: Int = 0

        override fun getItemViewType(position: Int): Int = when (items[position]) {
            is Item.PostItem -> when (layout) {
                CommunityLayout.LIST -> R.layout.listing_item_list
                CommunityLayout.CARD -> R.layout.listing_item_card
                CommunityLayout.FULL -> R.layout.listing_item_full
            }
            is Item.FooterItem -> R.layout.main_footer_item
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            val v = inflater.inflate(viewType, parent, false)
            return when (viewType) {
                R.layout.listing_item_list -> ListingItemViewHolder(v)
                R.layout.listing_item_card -> ListingItemViewHolder(v)
                R.layout.listing_item_full -> ListingItemViewHolder(v)
                R.layout.main_footer_item -> ViewBindingViewHolder(MainFooterItemBinding.bind(v))
                else -> throw RuntimeException("Unknown view type: $viewType")
            }
        }

        override fun onBindViewHolder(
            holder: RecyclerView.ViewHolder,
            position: Int,
            payloads: MutableList<Any>
        ) {
            when (getItemViewType(position)) {
                R.layout.listing_item_card,
                R.layout.listing_item_list,
                R.layout.listing_item_full -> {
                    if (payloads.isEmpty()) {
                        super.onBindViewHolder(holder, position, payloads)
                    } else {
                        // this is an incremental update... Only update the stats, do not update content...
                        val h = holder as ListingItemViewHolder
                        val item = items[position] as Item.PostItem

                        lemmyHeaderHelper.populateHeaderSpan(h.headerContainer, item.postView)

                        h.commentButton.text =
                            RedditUtils.abbrevNumber(item.postView.counts.comments.toLong())
                        h.upvoteCount.text = item.postView.getUpvoteText()

                        h.commentButton.isEnabled = !item.postView.post.locked

                        voteUiHandler.bind(
                            item.instance,
                            item.postView,
                            h.upvoteButton,
                            h.downvoteButton,
                            onSignInRequired = onSignInRequired,
                            onInstanceMismatch = onInstanceMismatch,
                        )
                    }
                }
                else -> {
                    super.onBindViewHolder(holder, position, payloads)
                }
            }
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            when (val viewType = getItemViewType(position)) {
                R.layout.listing_item_card,
                R.layout.listing_item_list,
                R.layout.listing_item_full -> {
                    val isCard = viewType == R.layout.listing_item_card
                    val isFullView = viewType == R.layout.listing_item_full
                    val h: ListingItemViewHolder = holder as ListingItemViewHolder
                    val item = items[position] as Item.PostItem
                    val isRevealed = revealedItems.contains(item.postView.getUniqueKey())

                    lemmyHeaderHelper.populateHeaderSpan(h.headerContainer, item.postView)

                    val thumbnailUrl = item.postView.post.thumbnail_url
//                    val itemType = item.postView.post.

                    ViewCompat.setTransitionName(h.title, "title:${item.postView.getUniqueKey()}")

                    fun showDefaultImage() {
                        h.image?.visibility = View.GONE
                        h.iconImage?.visibility = View.VISIBLE
                        h.iconImage?.setImageResource(R.drawable.ic_text_box_black_24dp)
                    }

                    fun loadAndShowImage() {
                        if (activity == null) return

                        h.image ?: return

                        if (thumbnailUrl == "default") {
                            showDefaultImage()
                            return
                        }

                        if (thumbnailUrl.isNullOrBlank()) {
                            h.image.visibility = View.GONE
                            return
                        }

                        Log.d(TAG, "url: $thumbnailUrl")

                        h.image.visibility = View.VISIBLE
                        h.iconImage?.visibility = View.GONE

                        h.image.load(null)

                        offlineManager.fetchImage(h.itemView, thumbnailUrl) {
                            h.image.load(it)
                        }
                    }

                    h.linkTypeImage?.visibility = View.GONE
                    h.iconImage?.visibility = View.GONE
                    h.iconImage?.setOnClickListener(null)
                    h.iconImage?.isClickable = false
                    h.image?.setOnClickListener(null)
                    h.image?.isClickable = false

                    fun onItemClick() {
                        val action = CommunityFragmentDirections.actionMainFragmentToPostFragment(
                            instance = item.instance,
                            id = item.postView.post.id,
                            reveal = revealedItems.contains(item.postView.getUniqueKey()),
                            post = item.postView,
                            currentCommunity = item.postView.community.toCommunity(),
                            videoState = lemmyContentHelper.getState(h.fullContentContainerView).videoState?.let {
                                it.copy(currentTime = it.currentTime - ExoPlayerManager.CONVENIENCE_REWIND_TIME_MS)
                            })
                        findNavController().navigateSafe(action)
                    }

                    fun showFullContent() {
                        lemmyContentHelper.setupFullContent(
                            reveal = isRevealed,
                            tempSize = tempSize,
                            videoViewMaxHeight = (binding.recyclerView.height
                                    - Utils.convertDpToPixel(56f)
                                    - Utils.convertDpToPixel(16f)
                                    ).toInt(),
                            contentMaxWidth = contentMaxWidth,
                            fullImageViewTransitionName = "full_image_$position",
                            postView = item.postView,
                            rootView = h.itemView,
                            fullContentContainerView = h.fullContentContainerView,
                            onFullImageViewClickListener = { v, url ->

                                val action =
                                    CommunityFragmentDirections.actionMainFragmentToImageViewerFragment(
                                        null,
                                        url,
                                        null
                                    )

                                if (v != null) {
                                    val extras = FragmentNavigatorExtras(
                                        v to "image_view"
                                    )
                                    findNavController().navigate(action, extras)
                                } else {
                                    findNavController().navigate(
                                        action, NavOptions.Builder()
                                            .addDefaultAnim()
                                            .build()
                                    )
                                }

                            },
                            onImageClickListener = { url ->
                                val action =
                                    CommunityFragmentDirections.actionMainFragmentToImageViewerFragment(
                                        title = null,
                                        url = url,
                                        mimeType = null
                                    )
                                findNavController().navigateSafe(action)
                            },
                            onItemClickListener = {
                                onItemClick()
                            },
                            onRevealContentClickedFn = {
                                revealedItems.add(item.postView.getUniqueKey())
                                notifyItemChanged(h.absoluteAdapterPosition)
                            }
                        )
                    }

                    when (item.postView.getType()) {
                        PostType.Image -> {
                            loadAndShowImage()

                            if (expandedItems.contains(item.postView.getUniqueKey())) {
                                showFullContent()
                            }
                            h.image?.setOnClickListener {
                                toggleItem(h.absoluteAdapterPosition, item)
                                notifyItemChanged(h.absoluteAdapterPosition)
                            }
                        }
                        PostType.Video -> {
                            loadAndShowImage()

                            h.iconImage?.visibility = View.VISIBLE
                            h.iconImage?.setImageResource(R.drawable.baseline_play_circle_filled_black_24)

                            if (expandedItems.contains(item.postView.getUniqueKey())) {
                                showFullContent()
                            }

                            h.image?.setOnClickListener {
                                toggleItem(h.absoluteAdapterPosition, item)
                            }
                        }
                        PostType.Text -> {
                            if (thumbnailUrl == null) {
                                h.image?.visibility = View.GONE

                                // see if this text post has additional content
                                val hasAdditionalContent =
                                    !item.postView.post.body.isNullOrBlank() ||
                                            !item.postView.post.url.isNullOrBlank()

                                if (hasAdditionalContent) {
                                    showDefaultImage()
                                    h.iconImage?.setOnClickListener {
                                        toggleItem(h.absoluteAdapterPosition, item)
                                        notifyItemChanged(h.absoluteAdapterPosition)
                                    }
                                }
                            } else {
                                loadAndShowImage()
                            }

                            if (expandedItems.contains(item.postView.getUniqueKey())) {
                                showFullContent()
                            }

                            h.linkTypeImage?.visibility = View.VISIBLE
                            h.linkTypeImage?.setImageResource(R.drawable.baseline_open_in_new_black_18)
                            h.image?.setOnClickListener {
                                toggleItem(h.absoluteAdapterPosition, item)
                            }
                        }
//                        ListingItemType.REDDIT_GALLERY -> {
//                            loadAndShowImage()
//
//                            if (expandedItems.contains(item.listingItem.name)) {
//                                showFullContent()
//                            }
//                            h.image?.setOnClickListener {
//                                toggleItem(h.adapterPosition, item)
//                                notifyItemChanged(h.adapterPosition)
//                            }
//                        }
                    }

                    h.image?.layoutParams = h.image?.layoutParams?.apply {
                        width = postImageWidth
                    }
                    h.iconImage?.layoutParams = h.iconImage?.layoutParams?.apply {
                        width = postImageWidth
                    }

                    h.title.text = item.postView.getFormattedTitle()
                    h.commentButton.text =
                        RedditUtils.abbrevNumber(item.postView.counts.comments.toLong())
                    h.upvoteCount.text = item.postView.getUpvoteText()

                    h.itemView.setOnClickListener {
                        onItemClick()
                    }
                    h.commentButton.setOnClickListener {
                        val action = CommunityFragmentDirections.actionMainFragmentToPostFragment(
                            instance = item.instance,
                            id = item.postView.post.id,
                            jumpToComments = true,
                            reveal = revealedItems.contains(item.postView.getUniqueKey()),
                            currentCommunity = item.postView.community.toCommunity(),
                            post = item.postView,
                        )
                        findNavController().navigateSafe(action)
                    }
                    h.commentButton.isEnabled = !item.postView.post.locked

                    voteUiHandler.bind(
                        item.instance,
                        item.postView,
                        h.upvoteButton,
                        h.downvoteButton,
                        onSignInRequired = onSignInRequired,
                        onInstanceMismatch,
                    )

                    if (isCard || isFullView) {
                        showFullContent()
                    }
                }
                R.layout.main_footer_item -> {
                    val b = holder.getBinding<MainFooterItemBinding>()
                    val item = items[position] as Item.FooterItem
                    if (item.hasMore) {
                        b.nextButton.visibility = View.VISIBLE
                        b.nextButton.setOnClickListener {
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
            }
        }

        override fun onViewRecycled(holder: RecyclerView.ViewHolder) {
            super.onViewRecycled(holder)

            offlineManager.cancelFetch(holder.itemView)
            if (holder is ListingItemViewHolder) {
                lemmyContentHelper.recycleFullContent(
                    holder.fullContentContainerView
                )
            }
        }

        private fun toggleItem(position: Int, item: Item.PostItem) {
            if (expandedItems.contains(item.postView.getUniqueKey())) {
                expandedItems.remove(item.postView.getUniqueKey())
            } else {
                expandedItems.add(item.postView.getUniqueKey())
            }

            notifyItemChanged(position)
        }

        override fun getItemCount(): Int = items.size

        fun setItems(pageIndex: Int, data: CommunityViewModel.LoadedPostsData) {
            rawData = data
            this.pageIndex = pageIndex
            refreshItems()
        }

        fun refreshItems() {
            val newItems = rawData?.let { data ->
                data.posts
                    .map { Item.PostItem(it, data.instance) } + Item.FooterItem(data.hasMore)
            } ?: listOf()
            val oldItems = items

            val diff = DiffUtil.calculateDiff(object : DiffUtil.Callback() {
                override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                    val oldItem = oldItems[oldItemPosition]
                    val newItem = newItems[newItemPosition]

                    return if (oldItem::class.java == newItem::class.java) {
                        if (oldItem is Item.PostItem && newItem is Item.PostItem) {
                            oldItem.postView.getUniqueKey() == newItem.postView.getUniqueKey()
                        } else {
                            false
                        }
                    } else {
                        false
                    }
                }

                override fun getOldListSize(): Int = oldItems.size

                override fun getNewListSize(): Int = newItems.size

                override fun areContentsTheSame(
                    oldItemPosition: Int,
                    newItemPosition: Int
                ): Boolean {
                    return areItemsTheSame(oldItemPosition, newItemPosition)
                }

            })
            this.items = newItems
            diff.dispatchUpdatesTo(this)
        }

        fun getItems() = items

        fun setLayout(communityLayout: CommunityLayout) {
            layout = communityLayout
            notifyDataSetChanged()
        }

        fun reset() {
            rawData = null
            refreshItems()
        }
    }
}
