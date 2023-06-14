package com.idunnololz.summit.subreddit

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
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavOptions
import androidx.navigation.findNavController
import androidx.navigation.fragment.FragmentNavigatorExtras
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DecodeFormat
import com.google.android.material.button.MaterialButton
import com.google.android.material.snackbar.Snackbar
import com.idunnololz.summit.R
import com.idunnololz.summit.databinding.FragmentSubredditBinding
import com.idunnololz.summit.history.HistoryManager
import com.idunnololz.summit.history.HistorySaveReason
import com.idunnololz.summit.main.MainActivity
import com.idunnololz.summit.main.MainFragment
import com.idunnololz.summit.offline.OfflineManager
import com.idunnololz.summit.reddit.*
import com.idunnololz.summit.reddit.ext.ListingItemType
import com.idunnololz.summit.reddit.ext.getFormattedTitle
import com.idunnololz.summit.reddit.ext.getType
import com.idunnololz.summit.reddit_objects.ListingData
import com.idunnololz.summit.reddit_objects.ListingItem
import com.idunnololz.summit.reddit_objects.ListingItemObject
import com.idunnololz.summit.reddit_objects.getLikesWithLikesManager
import com.idunnololz.summit.tabs.TabsManager
import com.idunnololz.summit.util.*
import com.idunnololz.summit.util.ext.*
import com.idunnololz.summit.video.ExoPlayerManager
import com.idunnololz.summit.view.RedditHeaderView
import com.idunnololz.summit.view.RedditSortOrderView

class SubredditFragment : BaseFragment<FragmentSubredditBinding>() {

    companion object {
        private val TAG = "SubredditFragment"
    }

    private val args: SubredditFragmentArgs by navArgs()

    private lateinit var redditViewModel: RedditViewModel

    private var adapter: ListingItemAdapter? = null

    private var shouldScrollToTopAfterFresh = false

    private val onSortOrderChangedListener =
        object : RedditSortOrderView.OnSortOrderChangedListener {
            override fun onSortOrderChanged(newSortOrder: RedditSortOrder) {
                redditViewModel.setSortOrder(newSortOrder)
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val context = requireContext()
        redditViewModel = ViewModelProvider(this).get(RedditViewModel::class.java)
        if (adapter == null) {
            adapter = ListingItemAdapter(context)
            onSelectedLayoutChanged()
        }

        redditViewModel.changeToSubreddit(args.url)

        if (savedInstanceState != null) {
            restoreState(SubredditViewState.restoreFromState(savedInstanceState), reload = false)
        } else if (args.pages != null) {
            val pages: List<String> = args.pages?.split(',') ?: listOf()
            redditViewModel.setPages(pages.map { RedditPageLoader.PageInfo(it, 0) }, args.pageIndex)
        }

        val callback = object : OnBackPressedCallback(true /* enabled by default */) {
            override fun handleOnBackPressed() {
                if (redditViewModel.currentPageIndex.value != 0) {
                    redditViewModel.fetchPrevPage()
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
            setupForFragment<SubredditFragment>()
        }

        setBinding(FragmentSubredditBinding.inflate(inflater, container, false))

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        Log.d(TAG, "onViewCreated(): ${args.url}")
        super.onViewCreated(view, savedInstanceState)

        checkNotNull(view.findNavController())

        val mainActivity = requireMainActivity()
        mainActivity.apply {
            headerOffset.observe(viewLifecycleOwner, Observer {
                if (it != null)
                    getView()?.translationY = it.toFloat()
            })
        }

        mainActivity.insetRootViewAutomatically(viewLifecycleOwner, view)

        val context = requireContext()

        binding.swipeRefreshLayout.setOnRefreshListener {
            shouldScrollToTopAfterFresh = true
            redditViewModel.fetchCurrentPage(true)
            binding.recyclerView.scrollToPosition(0)
        }
        binding.loadingView.setOnRefreshClickListener {
            redditViewModel.fetchCurrentPage(true)
        }

        binding.recyclerView.adapter = adapter
        binding.recyclerView.setHasFixedSize(true)
        binding.recyclerView.layoutManager = LinearLayoutManager(context)
        binding.recyclerView.addItemDecoration(CustomDividerItemDecoration(
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
        })
        binding.fastScroller.setRecyclerView(binding.recyclerView)

        binding.recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)

                val adapter = adapter ?: return
                val pageIndex = adapter.pageIndex ?: return
                val layoutManager = (recyclerView.layoutManager as LinearLayoutManager)
                val lastPos = layoutManager.findLastVisibleItemPosition()
                if (lastPos == adapter.itemCount - 1) {
                    redditViewModel.setPagePositionAtBottom(pageIndex)
                } else {
                    val firstPos = layoutManager.findFirstVisibleItemPosition()
                    val firstView = layoutManager.findViewByPosition(firstPos)
                    redditViewModel.setPagePosition(pageIndex, firstPos, firstView?.getTop() ?: 0)
                }
            }
        })

        setupMainActivityButtons()
        scheduleStartPostponedTransition(binding.rootView)

        redditViewModel.loadedListingData.observe(viewLifecycleOwner,
            Observer a@{
                when (it.status) {
                    Status.LOADING ->
                        binding.loadingView.showProgressBar()
                    Status.SUCCESS -> {
                        val adapter = adapter ?: return@a

                        adapter.setItems(it.data.pageIndex, it.data.listingData)

                        adapter.curDataSource = it.data.url

                        if (shouldScrollToTopAfterFresh) {
                            shouldScrollToTopAfterFresh = false
                            binding.recyclerView.scrollToPosition(0)
                        } else {
                            val pagePosition = redditViewModel.getPagePosition(it.data.pageIndex)
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
                    Status.FAILED -> {
                        binding.swipeRefreshLayout.isRefreshing = false
                        binding.recyclerView.visibility = View.GONE
                        binding.loadingView.showDefaultErrorMessageFor(it.requireError())
                    }
                }
            }
        )

        requireMainActivity().getCustomAppBarController().setupSortOrderSelector(
            viewLifecycleOwner, redditViewModel.getCurrentSortOrder(), onSortOrderChangedListener)

        if (adapter?.getItems().isNullOrEmpty()) {
            redditViewModel.fetchCurrentPage()
        }
    }

    override fun onResume() {
        super.onResume()

        val customAppBarController = requireMainActivity().getCustomAppBarController()
        val context = requireContext()

        redditViewModel.currentSubreddit.observe(viewLifecycleOwner, {
            customAppBarController.setSubreddit(if (it.isBlank()) {
                context.getString(R.string.home)
            } else {
                it
            })
        })
        redditViewModel.currentPageIndex.observe(viewLifecycleOwner, {
            Log.d(TAG, "Current page: $it")
            customAppBarController.setPageIndex(it) { pageIndex ->
                redditViewModel.fetchPage(pageIndex)
            }
        })
    }

    override fun onSaveInstanceState(outState: Bundle) {
        redditViewModel.createState().writeToBundle(outState)
        super.onSaveInstanceState(outState)
    }

    override fun onPause() {
        val context = requireContext()
        val viewState = redditViewModel.createState()
        HistoryManager.instance.recordSubredditState(
            tabId = MainFragment.getIdFromTag(parentFragment?.tag ?: ""),
            saveReason = HistorySaveReason.LEAVE_SCREEN,
            state = viewState,
            shortDesc = viewState.getShortDesc(context)
        )

        super.onPause()
    }

    override fun onDestroyView() {
        Log.d(TAG, "onDestroyView()")
        super.onDestroyView()
    }

    fun restoreState(state: SubredditViewState?, reload: Boolean) {
        redditViewModel.restoreFromState(state)
        if (reload)
            redditViewModel.fetchCurrentPage()
    }

    private fun setupMainActivityButtons() {
        (activity as? MainActivity)?.apply {

            val context = this

            getCustomAppBarController().setup(
                { controller, url ->
                    val action = SubredditFragmentDirections.actionSubredditFragmentSwitchSubreddit(
                        url = url
                    )
                    findNavController().navigate(action)
                    Utils.hideKeyboard(activity)
                    controller.hide()
                },
                {
                    PopupMenu(context, it).apply {
                        inflate(R.menu.menu_fragment_main)
                        forceShowIcons()

                        val selectedItem = when (PreferenceUtil.getSubredditLayout()) {
                            SubredditLayout.LIST -> {
                                menu.findItem(R.id.layoutList)
                            }
                            SubredditLayout.CARD -> {
                                menu.findItem(R.id.layoutCard)
                            }
                            SubredditLayout.FULL -> {
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

                        val currentSubreddit = redditViewModel.currentSubreddit.value
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
                                            redditViewModel.getSharedLinkForCurrentPage()
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
                                    PreferenceUtil.setSubredditLayout(SubredditLayout.LIST)
                                    onSelectedLayoutChanged()
                                    true
                                }
                                R.id.layoutCard -> {
                                    PreferenceUtil.setSubredditLayout(SubredditLayout.CARD)
                                    onSelectedLayoutChanged()
                                    true
                                }
                                R.id.layoutFull -> {
                                    PreferenceUtil.setSubredditLayout(SubredditLayout.FULL)
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
            val listingItem: ListingItem
        ) : Item()

        class FooterItem(val data: ListingData) : Item()
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
        private var rawData: ListingData? = null
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
            this@SubredditFragment,
            offlineManager,
            ExoPlayerManager.get(this@SubredditFragment)
        )

        private val postImageWidth: Int = (Utils.getScreenWidth(context) * 0.2).toInt()

        private val tempSize = Size()

        private var layout: SubredditLayout = SubredditLayout.LIST

        var curDataSource: String? = null

        private val likesManager = LikesManager.instance

        override fun getItemViewType(position: Int): Int = when (items[position]) {
            is Item.PostItem -> when (layout) {
                SubredditLayout.LIST -> R.layout.listing_item_list
                SubredditLayout.CARD -> R.layout.listing_item_card
                SubredditLayout.FULL -> R.layout.listing_item_full
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

                        redditHeaderHelper.populateHeaderSpan(h.headerContainer, item.listingItem)

                        h.commentButton.text =
                            RedditUtils.abbrevNumber(item.listingItem.numComments.toLong())
                        h.upvoteCount.text = RedditUtils.getUpvoteText(item.listingItem)

                        h.commentButton.isEnabled = !item.listingItem.locked

                        UserActionsHelper.setupActions(
                            item.listingItem.name,
                            item.listingItem.getLikesWithLikesManager(),
                            viewLifecycleOwner,
                            childFragmentManager,
                            h.upvoteButton,
                            h.downvoteButton
                        ) { onVote ->
                            item.listingItem.likes = when {
                                onVote > 0 -> true
                                onVote < 0 -> false
                                else -> null
                            }
                            notifyItemChanged(h.adapterPosition, Unit)
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
                    val isRevealed = revealedItems.contains(item.listingItem.name)

                    redditHeaderHelper.populateHeaderSpan(h.headerContainer, item.listingItem)

                    val thumbnailUrl = item.listingItem.getThumbnailUrl(isRevealed)
                    val itemType = item.listingItem.getType()

                    ViewCompat.setTransitionName(h.title, "title:${item.listingItem.id}")

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

                        h.image.setImageResource(0)

                        offlineManager.fetchImage(h.itemView, thumbnailUrl) {
                            Glide.with(this@SubredditFragment)
                                .load(it)
                                .format(DecodeFormat.PREFER_RGB_565)
                                .into(h.image)
                        }
                    }

                    h.linkTypeImage?.visibility = View.GONE
                    h.iconImage?.visibility = View.GONE
                    h.iconImage?.setOnClickListener(null)
                    h.iconImage?.isClickable = false
                    h.image?.setOnClickListener(null)
                    h.image?.isClickable = false

                    fun showFullContent() {
                        redditContentHelper.setupFullContent(
                            reveal = isRevealed,
                            tempSize = tempSize,
                            videoViewMaxHeight = (binding.recyclerView.height
                                    - Utils.convertDpToPixel(56f)
                                    - Utils.convertDpToPixel(16f)
                                    ).toInt(),
                            fullImageViewTransitionName = "full_image_$position",
                            listingItem = item.listingItem,
                            rootView = h.itemView,
                            fullContentContainerView = h.fullContentContainerView,
                            onFullImageViewClickListener = { v, url ->

                                val action =
                                    SubredditFragmentDirections.actionMainFragmentToImageViewerFragment(
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
                                revealedItems.add(item.listingItem.name)
                                notifyItemChanged(h.adapterPosition)
                            }
                        )
                    }

                    // Set up any icons we should show on the image and what clicking on the image should do
                    when (itemType) {
                        ListingItemType.DEFAULT_SELF -> {
                            showDefaultImage()
                        }
                        ListingItemType.REDDIT_IMAGE -> {
                            loadAndShowImage()

                            if (expandedItems.contains(item.listingItem.name)) {
                                showFullContent()
                            }
                            h.image?.setOnClickListener {
                                toggleItem(h.adapterPosition, item)
                                notifyItemChanged(h.adapterPosition)
                            }
                        }
                        ListingItemType.REDDIT_VIDEO -> {
                            loadAndShowImage()

                            h.iconImage?.visibility = View.VISIBLE
                            h.iconImage?.setImageResource(R.drawable.baseline_play_circle_filled_black_24)

                            if (expandedItems.contains(item.listingItem.name)) {
                                showFullContent()
                            }

                            h.image?.setOnClickListener {
                                toggleItem(h.adapterPosition, item)
                            }
                        }
                        ListingItemType.REDDIT_GALLERY -> {
                            loadAndShowImage()

                            if (expandedItems.contains(item.listingItem.name)) {
                                showFullContent()
                            }
                            h.image?.setOnClickListener {
                                toggleItem(h.adapterPosition, item)
                                notifyItemChanged(h.adapterPosition)
                            }
                        }
                        ListingItemType.UNKNOWN -> {
                            if (thumbnailUrl == null) {
                                h.image?.visibility = View.GONE
                            } else {
                                loadAndShowImage()
                            }

                            if (expandedItems.contains(item.listingItem.name)) {
                                showFullContent()
                            }

                            h.linkTypeImage?.visibility = View.VISIBLE
                            h.linkTypeImage?.setImageResource(R.drawable.baseline_open_in_new_black_18)
                            h.image?.setOnClickListener {
                                toggleItem(h.adapterPosition, item)
                            }
                        }
                    }

                    h.image?.layoutParams = h.image?.layoutParams?.apply {
                        width = postImageWidth
                    }
                    h.iconImage?.layoutParams = h.iconImage?.layoutParams?.apply {
                        width = postImageWidth
                    }

                    h.title.text = item.listingItem.getFormattedTitle()
                    h.commentButton.text =
                        RedditUtils.abbrevNumber(item.listingItem.numComments.toLong())
                    h.upvoteCount.text = RedditUtils.getUpvoteText(item.listingItem)

                    h.itemView.setOnClickListener {
                        val extras = FragmentNavigatorExtras(
                            h.title to "title"
                        )

                        val action = SubredditFragmentDirections.actionMainFragmentToPostFragment(
                            "https://oauth.reddit.com${item.listingItem.permalink}",
                            reveal = revealedItems.contains(item.listingItem.name),
                            originalPost = item.listingItem,
                            currentSubreddit = item.listingItem.subredditNamePrefixed,
                            videoState = redditContentHelper.getState(h.fullContentContainerView).videoState?.let {
                                it.copy(currentTime = it.currentTime - ExoPlayerManager.CONVENIENCE_REWIND_TIME_MS)
                            })
                        findNavController().navigateSafe(action, extras)
                    }
                    h.commentButton.setOnClickListener {
                        val action = SubredditFragmentDirections.actionMainFragmentToPostFragment(
                            "https://oauth.reddit.com/${item.listingItem.permalink}",
                            jumpToComments = true,
                            reveal = revealedItems.contains(item.listingItem.name),
                            originalPost = item.listingItem
                        )
                        findNavController().navigateSafe(action)
                    }
                    h.commentButton.isEnabled = !item.listingItem.locked

                    UserActionsHelper.setupActions(
                        item.listingItem.name,
                        item.listingItem.getLikesWithLikesManager(),
                        viewLifecycleOwner,
                        childFragmentManager,
                        h.upvoteButton,
                        h.downvoteButton
                    ) { onVote ->
                        item.listingItem.likes = when {
                            onVote > 0 -> true
                            onVote < 0 -> false
                            else -> null
                        }
                        notifyItemChanged(h.adapterPosition, Unit)
                    }

                    if (isCard || isFullView) {
                        showFullContent()
                    }
                }
                R.layout.main_footer_item -> {
                    val h = holder as FooterViewHolder
                    val item = items[position] as Item.FooterItem
                    if (item.data.after == null) {
                        h.nextButton.visibility = View.INVISIBLE
                    } else {
                        h.nextButton.visibility = View.VISIBLE
                        h.nextButton.setOnClickListener {
                            redditViewModel.fetchNextPage(clearPagePosition = true)
                        }
                    }
                    if (redditViewModel.currentPageIndex.value == 0) {
                        h.prevButton.visibility = View.INVISIBLE
                    } else {
                        h.prevButton.visibility = View.VISIBLE
                        h.prevButton.setOnClickListener {
                            redditViewModel.fetchPrevPage()
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
            if (expandedItems.contains(item.listingItem.name)) {
                expandedItems.remove(item.listingItem.name)
            } else {
                expandedItems.add(item.listingItem.name)
            }

            notifyItemChanged(position)
        }

        override fun getItemCount(): Int = items.size

        fun setItems(pageIndex: Int, data: ListingData) {
            rawData = data
            this.pageIndex = pageIndex
            refreshItems()
        }

        fun refreshItems() {
            val newItems = rawData?.let {
                it.getChildrenAs<ListingItemObject>()
                    .mapNotNull { it.data }
                    .map { Item.PostItem(it) } + Item.FooterItem(it)
            } ?: listOf()
            val oldItems = items

            val diff = DiffUtil.calculateDiff(object : DiffUtil.Callback() {
                override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                    val oldItem = oldItems[oldItemPosition]
                    val newItem = newItems[newItemPosition]

                    return if (oldItem::class.java == newItem::class.java) {
                        if (oldItem is Item.PostItem && newItem is Item.PostItem) {
                            oldItem.listingItem.id == newItem.listingItem.id
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

        fun setLayout(subredditLayout: SubredditLayout) {
            layout = subredditLayout
            notifyDataSetChanged()
        }
    }
}
