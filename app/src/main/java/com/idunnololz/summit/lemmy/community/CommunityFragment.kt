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
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.widget.PopupMenu
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
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
import com.idunnololz.summit.api.dto.PostType
import com.idunnololz.summit.api.dto.PostView
import com.idunnololz.summit.databinding.FragmentSubredditBinding
import com.idunnololz.summit.history.HistoryManager
import com.idunnololz.summit.history.HistorySaveReason
import com.idunnololz.summit.lemmy.CommunitySortOrder
import com.idunnololz.summit.lemmy.LemmyContentHelper
import com.idunnololz.summit.lemmy.LemmyHeaderHelper
import com.idunnololz.summit.lemmy.CommunityViewState
import com.idunnololz.summit.lemmy.getFormattedTitle
import com.idunnololz.summit.lemmy.getLikesWithLikesManager
import com.idunnololz.summit.lemmy.getShortDesc
import com.idunnololz.summit.lemmy.getUpvoteText
import com.idunnololz.summit.lemmy.toCommunity
import com.idunnololz.summit.main.MainActivity
import com.idunnololz.summit.main.MainFragment
import com.idunnololz.summit.offline.OfflineManager
import com.idunnololz.summit.reddit.LikesManager
import com.idunnololz.summit.reddit.RedditContentHelper
import com.idunnololz.summit.reddit.RedditHeaderHelper
import com.idunnololz.summit.reddit.RedditPageLoader
import com.idunnololz.summit.reddit.RedditSortOrder
import com.idunnololz.summit.reddit.RedditUtils
import com.idunnololz.summit.reddit.SubredditViewState
import com.idunnololz.summit.reddit.UserActionsHelper
import com.idunnololz.summit.reddit.getShortDesc
import com.idunnololz.summit.tabs.TabsManager
import com.idunnololz.summit.util.BaseFragment
import com.idunnololz.summit.util.CustomDividerItemDecoration
import com.idunnololz.summit.util.PreferenceUtil
import com.idunnololz.summit.util.SharedElementTransition
import com.idunnololz.summit.util.Size
import com.idunnololz.summit.util.StatefulData
import com.idunnololz.summit.util.Utils
import com.idunnololz.summit.util.ext.addDefaultAnim
import com.idunnololz.summit.util.ext.drawToBitmap
import com.idunnololz.summit.util.ext.forceShowIcons
import com.idunnololz.summit.util.ext.getColorCompat
import com.idunnololz.summit.util.ext.navigateSafe
import com.idunnololz.summit.video.ExoPlayerManager
import com.idunnololz.summit.view.LemmySortOrderView
import com.idunnololz.summit.view.RedditHeaderView
import com.idunnololz.summit.view.RedditSortOrderView
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class CommunityFragment : BaseFragment<FragmentSubredditBinding>() {

    companion object {
        private const val TAG = "SubredditFragment"
    }

    private val args: CommunityFragmentArgs by navArgs()

    private val communityViewModel: CommunityViewModel by viewModels()

    private var adapter: ListingItemAdapter? = null

    private var shouldScrollToTopAfterFresh = false

    private val onSortOrderChangedListener =
        object : LemmySortOrderView.OnSortOrderChangedListener {
            override fun onSortOrderChanged(newSortOrder: CommunitySortOrder) {
                communityViewModel.setSortOrder(newSortOrder)
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val context = requireContext()
        if (adapter == null) {
            adapter = ListingItemAdapter(context)
            onSelectedLayoutChanged()
        }

        communityViewModel.changeCommunity(args.community)

        if (savedInstanceState != null) {
            restoreState(CommunityViewState.restoreFromBundle(savedInstanceState), reload = false)
        } else if (args.pages != null) {
            val pages: List<String> = args.pages?.split(',') ?: listOf()
            communityViewModel.setPages(pages.map { RedditPageLoader.PageInfo(it, 0) }, args.pageIndex)
        }

        val callback = object : OnBackPressedCallback(true /* enabled by default */) {
            override fun handleOnBackPressed() {
                if (communityViewModel.currentPageIndex.value != 0) {
                    communityViewModel.fetchPrevPage()
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

        checkNotNull(view.findNavController())

        val mainActivity = requireMainActivity()
        mainActivity.apply {
            headerOffset.observe(viewLifecycleOwner) {
                if (it != null)
                    getView()?.translationY = it.toFloat()
            }
        }

        mainActivity.insetRootViewAutomatically(viewLifecycleOwner, view)

        val context = requireContext()

        binding.swipeRefreshLayout.setOnRefreshListener {
            shouldScrollToTopAfterFresh = true
            communityViewModel.fetchCurrentPage(true)
            binding.recyclerView.scrollToPosition(0)
        }
        binding.loadingView.setOnRefreshClickListener {
            communityViewModel.fetchCurrentPage(true)
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
                    communityViewModel.setPagePositionAtBottom(pageIndex)
                } else {
                    val firstPos = layoutManager.findFirstVisibleItemPosition()
                    val firstView = layoutManager.findViewByPosition(firstPos)
                    communityViewModel.setPagePosition(pageIndex, firstPos, firstView?.top ?: 0)
                }
            }
        })

        setupMainActivityButtons()
        scheduleStartPostponedTransition(binding.rootView)

        communityViewModel.loadedListingData.observe(viewLifecycleOwner,
            Observer a@{
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

//                        adapter.setItems(it.data.pageIndex, it.data.listingData)

                        adapter.curDataSource = it.data.url

                        if (shouldScrollToTopAfterFresh) {
                            shouldScrollToTopAfterFresh = false
                            binding.recyclerView.scrollToPosition(0)
                        } else {
                            val pagePosition = communityViewModel.getPagePosition(it.data.pageIndex)
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
                            }
                        }

                        binding.swipeRefreshLayout.isRefreshing = false
                        binding.recyclerView.visibility = View.VISIBLE
                        binding.loadingView.hideAll()

                        // delay screenshot due to fade transition
                        binding.loadingView.postDelayed({
                            if (ViewCompat.isLaidOut(binding.recyclerView)) {
                                val bgColor = ContextCompat.getColor(
                                    context,
                                    R.color.colorBackground
                                )
                                TabsManager.instance.currentTabId.value?.let {
                                    TabsManager.instance.updateTabPreview(
                                        it,
                                        binding.recyclerView.drawToBitmap(backgroundColor = bgColor)
                                    )
                                }
                            }
                        }, 1000)
                    }
                }
            }
        )

        communityViewModel.loadedPostsData.observe(viewLifecycleOwner) a@{
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

                    adapter.curDataSource = it.data.url

                    if (shouldScrollToTopAfterFresh) {
                        shouldScrollToTopAfterFresh = false
                        binding.recyclerView.scrollToPosition(0)
                    } else {
                        val pagePosition = communityViewModel.getPagePosition(it.data.pageIndex)
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
                        }
                    }

                    binding.swipeRefreshLayout.isRefreshing = false
                    binding.recyclerView.visibility = View.VISIBLE
                    binding.loadingView.hideAll()

                    // delay screenshot due to fade transition
                    binding.loadingView.postDelayed({
                        if (ViewCompat.isLaidOut(binding.recyclerView)) {
                            val bgColor = ContextCompat.getColor(
                                context,
                                R.color.colorBackground
                            )
                            TabsManager.instance.currentTabId.value?.let {
                                TabsManager.instance.updateTabPreview(
                                    it,
                                    binding.recyclerView.drawToBitmap(backgroundColor = bgColor)
                                )
                            }
                        }
                    }, 1000)
                }
            }
        }

        requireMainActivity().getCustomAppBarController().setupSortOrderSelector(
            viewLifecycleOwner, communityViewModel.getCurrentSortOrder(), onSortOrderChangedListener)

        if (adapter?.getItems().isNullOrEmpty()) {
            communityViewModel.fetchCurrentPage()
        }
    }

    override fun onResume() {
        super.onResume()

        val customAppBarController = requireMainActivity().getCustomAppBarController()

        communityViewModel.currentCommunity.observe(viewLifecycleOwner) {
            customAppBarController.setCommunity(it)
        }
        communityViewModel.currentPageIndex.observe(viewLifecycleOwner) {
            Log.d(TAG, "Current page: $it")
            customAppBarController.setPageIndex(it) { pageIndex ->
                communityViewModel.fetchPage(pageIndex)
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        communityViewModel.createState()?.writeToBundle(outState)
        super.onSaveInstanceState(outState)
    }

    override fun onPause() {
        val context = requireContext()
        val viewState = communityViewModel.createState()
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
        communityViewModel.restoreFromState(state ?: return)
        if (reload)
            communityViewModel.fetchCurrentPage()
    }

    private fun setupMainActivityButtons() {
        (activity as? MainActivity)?.apply {

            val context = this

            getCustomAppBarController().setup(
                { controller, url ->
                    TODO()
//                    val action = CommunityFragmentDirections.actionSubredditFragmentSwitchSubreddit(
//                        community =
//                    )
//                    findNavController().navigate(action)
//                    Utils.hideKeyboard(activity)
//                    controller.hide()
                },
                {
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

                        val currentSubreddit = communityViewModel.currentCommunity.value
                        val currentDefaultPage = PreferenceUtil.getDefaultPage()

                        menu.findItem(R.id.set_as_default).isVisible =
                            currentSubreddit != null && currentSubreddit != currentDefaultPage

                        setOnMenuItemClickListener {
                            when (it.itemId) {
                                R.id.share -> {
                                    val sendIntent: Intent = Intent().apply {
                                        action = Intent.ACTION_SEND
                                        putExtra(
                                            Intent.EXTRA_TEXT,
                                            communityViewModel.getSharedLinkForCurrentPage()
                                        )
                                        type = "text/plain"
                                    }

                                    val shareIntent = Intent.createChooser(sendIntent, null)
                                    startActivity(shareIntent)
                                    true
                                }
                                R.id.go_to -> {
                                    findNavController().navigate(R.id.goToDialogFragment)
                                    true
                                }
                                R.id.sort -> {
                                    expandCustomActionBar()
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
                                    if (currentSubreddit != null) {
                                        PreferenceUtil.setDefaultPage(currentSubreddit)
                                        TabsManager.instance.onDefaultPageChanged()

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
                })
        }
    }

    private fun onSelectedLayoutChanged() {
        adapter?.setLayout(PreferenceUtil.getSubredditLayout())
    }

    private sealed class Item {
        class PostItem(
            val postView: PostView
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

    private class FooterViewHolder(
        itemView: View
    ) : RecyclerView.ViewHolder(itemView) {
        val prevButton: Button = itemView.findViewById(R.id.prevButton)
        val nextButton: Button = itemView.findViewById(R.id.nextButton)
    }

    private inner class ListingItemAdapter(
        private val context: Context
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

        private val redditHeaderHelper = RedditHeaderHelper(context)
        private val redditContentHelper = RedditContentHelper(
            context,
            this@CommunityFragment,
            offlineManager,
            ExoPlayerManager.get(this@CommunityFragment)
        )

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
                R.layout.main_footer_item -> FooterViewHolder(v)
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

                        UserActionsHelper.setupActions(
                            item.postView.getUniqueKey(),
                            item.postView.getLikesWithLikesManager(),
                            viewLifecycleOwner,
                            childFragmentManager,
                            h.upvoteButton,
                            h.downvoteButton
                        ) { onVote ->
//                            item.postView.my_vote = when {
//                                onVote > 0 -> true
//                                onVote < 0 -> false
//                                else -> null
//                            }
                            notifyItemChanged(h.absoluteAdapterPosition, Unit)
                        }
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

                    fun showFullContent() {
                        lemmyContentHelper.setupFullContent(
                            reveal = isRevealed,
                            tempSize = tempSize,
                            videoViewMaxHeight = (binding.recyclerView.height
                                    - Utils.convertDpToPixel(56f)
                                    - Utils.convertDpToPixel(16f)
                                    ).toInt(),
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
                        val extras = FragmentNavigatorExtras(
                            h.title to "title"
                        )

                        val action = CommunityFragmentDirections.actionMainFragmentToPostFragment(
                            id = item.postView.post.id,
                            reveal = revealedItems.contains(item.postView.getUniqueKey()),
                            post = item.postView,
                            currentCommunity = item.postView.community.toCommunity(),
                            videoState = redditContentHelper.getState(h.fullContentContainerView).videoState?.let {
                                it.copy(currentTime = it.currentTime - ExoPlayerManager.CONVENIENCE_REWIND_TIME_MS)
                            })
                        findNavController().navigateSafe(action, extras)
                    }
                    h.commentButton.setOnClickListener {
                        val action = CommunityFragmentDirections.actionMainFragmentToPostFragment(
                            id = item.postView.post.id,
                            jumpToComments = true,
                            reveal = revealedItems.contains(item.postView.getUniqueKey()),
                            currentCommunity = item.postView.community.toCommunity(),
                            post = item.postView,
                        )
                        findNavController().navigateSafe(action)
                    }
                    h.commentButton.isEnabled = !item.postView.post.locked

                    UserActionsHelper.setupActions(
                        item.postView.getUniqueKey(),
                        item.postView.getLikesWithLikesManager(),
                        viewLifecycleOwner,
                        childFragmentManager,
                        h.upvoteButton,
                        h.downvoteButton
                    ) { onVote ->
                        notifyItemChanged(h.adapterPosition, Unit)
                    }

                    if (isCard || isFullView) {
                        showFullContent()
                    }
                }
                R.layout.main_footer_item -> {
                    val h = holder as FooterViewHolder
                    val item = items[position] as Item.FooterItem
                    if (item.hasMore) {
                        h.nextButton.visibility = View.VISIBLE
                        h.nextButton.setOnClickListener {
                            communityViewModel.fetchNextPage(clearPagePosition = true)
                        }
                    } else {
                        h.nextButton.visibility = View.INVISIBLE
                    }
                    if (communityViewModel.currentPageIndex.value == 0) {
                        h.prevButton.visibility = View.INVISIBLE
                    } else {
                        h.prevButton.visibility = View.VISIBLE
                        h.prevButton.setOnClickListener {
                            communityViewModel.fetchPrevPage()
                        }
                    }
                }
            }
        }

        override fun onViewRecycled(holder: RecyclerView.ViewHolder) {
            super.onViewRecycled(holder)

            offlineManager.cancelFetch(holder.itemView)
            if (holder is ListingItemViewHolder) {
                redditContentHelper.recycleFullContent(
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
            val newItems = rawData?.let {
                it.posts
                    .map { Item.PostItem(it) } + Item.FooterItem(it.hasMore)
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
    }
}
