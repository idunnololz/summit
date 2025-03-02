package com.idunnololz.summit.lemmy.community

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.idunnololz.summit.R
import com.idunnololz.summit.account.AccountImageGenerator
import com.idunnololz.summit.api.dto.PostView
import com.idunnololz.summit.api.utils.getUniqueKey
import com.idunnololz.summit.databinding.AutoLoadItemBinding
import com.idunnololz.summit.databinding.FilteredPostItemBinding
import com.idunnololz.summit.databinding.GenericSpaceFooterItemBinding
import com.idunnololz.summit.databinding.ItemGenericHeaderBinding
import com.idunnololz.summit.databinding.ListingItemCard2Binding
import com.idunnololz.summit.databinding.ListingItemCard3Binding
import com.idunnololz.summit.databinding.ListingItemCardBinding
import com.idunnololz.summit.databinding.ListingItemCompactBinding
import com.idunnololz.summit.databinding.ListingItemFullBinding
import com.idunnololz.summit.databinding.ListingItemFullWithCardsBinding
import com.idunnololz.summit.databinding.ListingItemLargeListBinding
import com.idunnololz.summit.databinding.ListingItemListBinding
import com.idunnololz.summit.databinding.ListingItemListWithCardsBinding
import com.idunnololz.summit.databinding.LoadingViewItemBinding
import com.idunnololz.summit.databinding.MainFooterItemBinding
import com.idunnololz.summit.databinding.ManualLoadItemBinding
import com.idunnololz.summit.databinding.PageTitleItemBinding
import com.idunnololz.summit.databinding.PersistentErrorItemBinding
import com.idunnololz.summit.databinding.PostListEndItemBinding
import com.idunnololz.summit.lemmy.CommunityRef
import com.idunnololz.summit.lemmy.PageRef
import com.idunnololz.summit.lemmy.PostRef
import com.idunnololz.summit.lemmy.multicommunity.FetchedPost
import com.idunnololz.summit.lemmy.multicommunity.Source
import com.idunnololz.summit.lemmy.multicommunity.accountId
import com.idunnololz.summit.lemmy.postListView.ListingItemViewHolder
import com.idunnololz.summit.lemmy.postListView.PostListViewBuilder
import com.idunnololz.summit.links.LinkContext
import com.idunnololz.summit.nsfwMode.NsfwModeManager
import com.idunnololz.summit.preferences.Preferences
import com.idunnololz.summit.preview.VideoType
import com.idunnololz.summit.util.recyclerView.ViewBindingViewHolder
import com.idunnololz.summit.util.recyclerView.getBinding
import com.idunnololz.summit.util.toErrorMessage
import com.idunnololz.summit.video.VideoState

class PostListAdapter(
    private val postListViewBuilder: PostListViewBuilder,
    private val context: Context,
    private var postListEngine: PostListEngine,
    private val onNextClick: () -> Unit,
    private val onPrevClick: () -> Unit,
    private val onSignInRequired: () -> Unit,
    private val onInstanceMismatch: (String, String) -> Unit,
    private val onImageClick: (accountId: Long?, PostView, View?, String) -> Unit,
    private val onVideoClick: (String, VideoType, VideoState?) -> Unit,
    private val onVideoLongClickListener: (url: String) -> Unit,
    private val onPageClick: (accountId: Long?, PageRef) -> Unit,
    private val onItemClick: (
        accountId: Long?,
        instance: String,
        id: Int,
        currentCommunity: CommunityRef?,
        post: PostView,
        jumpToComments: Boolean,
        reveal: Boolean,
        videoState: VideoState?,
    ) -> Unit,
    private val onShowMoreActions: (accountId: Long?, PostView) -> Unit,
    private val onPostRead: (accountId: Long?, PostView) -> Unit,
    private val onLoadPage: (Int) -> Unit,
    private val onLinkClick: (
        accountId: Long?,
        url: String,
        text: String?,
        linkContext: LinkContext,
    ) -> Unit,
    private val onLinkLongClick: (accountId: Long?, url: String, text: String?) -> Unit,
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val inflater = LayoutInflater.from(context)
    private val accountImageGenerator = AccountImageGenerator(context)

    var markPostsAsReadOnScroll: Boolean = false
    var alwaysRenderAsUnread: Boolean = false
    var blurNsfwPosts: Boolean = true
    var nsfwMode: Boolean = false

    var items: List<PostListEngineItem> = listOf()
        private set

    /**
     * Set of items that is hidden by default but is reveals (ie. nsfw or spoiler tagged)
     */
    private var revealedItems = mutableSetOf<String>()

    var layout: CommunityLayout = CommunityLayout.List
        set(value) {
            if (value != field) {
                field = value

                @Suppress("NotifyDataSetChanged")
                notifyDataSetChanged()
            }
        }

    var contentMaxWidth: Int = 0
        set(value) {
            if (value == 0 || value == field) {
                return
            }

            field = value

            @Suppress("NotifyDataSetChanged")
            notifyDataSetChanged()
        }
    var contentPreferredHeight: Int = 0

    var viewLifecycleOwner: LifecycleOwner? = null

    var seenItemPositions = mutableSetOf<Int>()

    override fun getItemViewType(position: Int): Int = when (items[position]) {
        is PostListEngineItem.VisiblePostItem -> when (layout) {
            CommunityLayout.Compact -> R.layout.listing_item_compact
            CommunityLayout.List -> R.layout.listing_item_list
            CommunityLayout.LargeList -> R.layout.listing_item_large_list
            CommunityLayout.Card -> R.layout.listing_item_card
            CommunityLayout.Card2 -> R.layout.listing_item_card2
            CommunityLayout.Card3 -> R.layout.listing_item_card3
            CommunityLayout.Full -> R.layout.listing_item_full
            CommunityLayout.ListWithCards -> R.layout.listing_item_list_with_cards
            CommunityLayout.FullWithCards -> R.layout.listing_item_full_with_cards
        }
        is PostListEngineItem.FilteredPostItem -> R.layout.filtered_post_item
        is PostListEngineItem.FooterItem -> R.layout.main_footer_item
        is PostListEngineItem.AutoLoadItem -> R.layout.auto_load_item
        PostListEngineItem.EndItem -> R.layout.post_list_end_item
        PostListEngineItem.FooterSpacerItem -> R.layout.generic_space_footer_item
        is PostListEngineItem.ErrorItem -> R.layout.loading_view_item
        is PostListEngineItem.PersistentErrorItem -> R.layout.persistent_error_item
        is PostListEngineItem.ManualLoadItem -> R.layout.manual_load_item
        is PostListEngineItem.PageTitle -> R.layout.page_title_item
        PostListEngineItem.HeaderItem -> R.layout.item_generic_header
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val v = inflater.inflate(viewType, parent, false)
        return when (viewType) {
            R.layout.listing_item_compact ->
                ListingItemViewHolder.fromBinding(ListingItemCompactBinding.bind(v))
            R.layout.listing_item_list ->
                ListingItemViewHolder.fromBinding(ListingItemListBinding.bind(v))
            R.layout.listing_item_large_list ->
                ListingItemViewHolder.fromBinding(ListingItemLargeListBinding.bind(v))
            R.layout.listing_item_card ->
                ListingItemViewHolder.fromBinding(ListingItemCardBinding.bind(v))
            R.layout.listing_item_card2 ->
                ListingItemViewHolder.fromBinding(ListingItemCard2Binding.bind(v))
            R.layout.listing_item_card3 ->
                ListingItemViewHolder.fromBinding(ListingItemCard3Binding.bind(v))
            R.layout.listing_item_full ->
                ListingItemViewHolder.fromBinding(ListingItemFullBinding.bind(v))
            R.layout.listing_item_list_with_cards ->
                ListingItemViewHolder.fromBinding(ListingItemListWithCardsBinding.bind(v))
            R.layout.listing_item_full_with_cards ->
                ListingItemViewHolder.fromBinding(ListingItemFullWithCardsBinding.bind(v))
            R.layout.filtered_post_item -> ViewBindingViewHolder(FilteredPostItemBinding.bind(v))
            R.layout.main_footer_item -> ViewBindingViewHolder(MainFooterItemBinding.bind(v))
            R.layout.auto_load_item ->
                ViewBindingViewHolder(AutoLoadItemBinding.bind(v))
            R.layout.post_list_end_item ->
                ViewBindingViewHolder(PostListEndItemBinding.bind(v))
            R.layout.loading_view_item ->
                ViewBindingViewHolder(LoadingViewItemBinding.bind(v))
            R.layout.generic_space_footer_item ->
                ViewBindingViewHolder(GenericSpaceFooterItemBinding.bind(v))
            R.layout.persistent_error_item ->
                ViewBindingViewHolder(PersistentErrorItemBinding.bind(v))
            R.layout.manual_load_item ->
                ViewBindingViewHolder(ManualLoadItemBinding.bind(v))
            R.layout.page_title_item ->
                ViewBindingViewHolder(PageTitleItemBinding.bind(v))
            R.layout.item_generic_header ->
                ViewBindingViewHolder(
                    ItemGenericHeaderBinding.bind(v).apply {
                        root.setTag(R.id.ghost_item, true)
                    },
                )
            else -> throw RuntimeException("Unknown view type: $viewType")
        }
    }

    override fun onBindViewHolder(
        holder: RecyclerView.ViewHolder,
        position: Int,
        payloads: MutableList<Any>,
    ) {
        when (val item = items[position]) {
            is PostListEngineItem.VisiblePostItem -> {
                if (payloads.isEmpty()) {
                    super.onBindViewHolder(holder, position, payloads)
                } else {
                    val h: ListingItemViewHolder = holder as ListingItemViewHolder
                    val isRevealed =
                        revealedItems.contains(item.fetchedPost.postView.getUniqueKey()) ||
                            !blurNsfwPosts ||
                            nsfwMode
                    val isActionsExpanded = item.isActionExpanded
                    val isExpanded = item.isExpanded

                    h.root.setTag(R.id.fetched_post, item.fetchedPost)

                    val source = item.fetchedPost.source
                    val accountId: Long?
                    val themeColor: Int?
                    if (source is Source.AccountSource) {
                        themeColor = accountImageGenerator.getColorForPerson(
                            source.name,
                            source.id,
                            source.instance,
                        )
                        accountId = source.id
                    } else {
                        themeColor = null
                        accountId = null
                    }

                    postListViewBuilder.bind(
                        holder = h,
                        fetchedPost = item.fetchedPost,
                        instance = item.instance,
                        isRevealed = isRevealed,
                        contentMaxWidth = contentMaxWidth,
                        contentPreferredHeight = contentPreferredHeight,
                        viewLifecycleOwner = requireNotNull(viewLifecycleOwner),
                        isExpanded = isExpanded,
                        isActionsExpanded = isActionsExpanded,
                        alwaysRenderAsUnread = alwaysRenderAsUnread,
                        updateContent = false,
                        highlight = item.highlight,
                        highlightForever = item.highlightForever,
                        themeColor = themeColor,
                        isDuplicatePost = item.isDuplicatePost,
                        onRevealContentClickedFn = {
                            revealedItems.add(item.fetchedPost.postView.getUniqueKey())
                            notifyItemChanged(h.absoluteAdapterPosition)
                        },
                        onImageClick = onImageClick,
                        onVideoClick = onVideoClick,
                        onVideoLongClickListener = onVideoLongClickListener,
                        onPageClick = onPageClick,
                        onItemClick = onItemClick,
                        onShowMoreOptions = onShowMoreActions,
                        toggleItem = this::toggleItem,
                        toggleActions = this::toggleActions,
                        onSignInRequired = onSignInRequired,
                        onInstanceMismatch = onInstanceMismatch,
                        onHighlightComplete = {
                            clearHighlight()
                        },
                        onLinkClick = onLinkClick,
                        onLinkLongClick = onLinkLongClick,
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
            is PostListEngineItem.FooterSpacerItem -> {}
            is PostListEngineItem.FooterItem -> {
                val b = holder.getBinding<MainFooterItemBinding>()
                if (item.hasMore) {
                    b.nextButton.visibility = View.VISIBLE
                    b.nextButton.setOnClickListener {
                        if (markPostsAsReadOnScroll) {
                            seenItemPositions.forEach {
                                (items.getOrNull(it) as? PostListEngineItem.VisiblePostItem)?.let {
                                    onPostRead(
                                        it.fetchedPost.source.accountId,
                                        it.fetchedPost.postView,
                                    )
                                }
                            }
                        }
                        onNextClick()
                    }
                } else {
                    b.nextButton.visibility = View.INVISIBLE
                }
                if (item.hasLess) {
                    b.prevButton.visibility = View.VISIBLE
                    b.prevButton.setOnClickListener {
                        onPrevClick()
                    }
                } else {
                    b.prevButton.visibility = View.INVISIBLE
                }
            }
            is PostListEngineItem.VisiblePostItem -> {
                val h: ListingItemViewHolder = holder as ListingItemViewHolder
                val isRevealed = revealedItems.contains(item.fetchedPost.postView.getUniqueKey()) ||
                    !blurNsfwPosts ||
                    nsfwMode
                val isActionsExpanded = item.isActionExpanded
                val isExpanded = item.isExpanded

                h.root.setTag(R.id.fetched_post, item.fetchedPost)
                h.root.setTag(R.id.swipeable, true)

                val source = item.fetchedPost.source
                val themeColor: Int? = if (source is Source.AccountSource) {
                    accountImageGenerator.getColorForPerson(
                        source.name,
                        source.id,
                        source.instance,
                    )
                } else {
                    null
                }

                postListViewBuilder.bind(
                    holder = h,
                    fetchedPost = item.fetchedPost,
                    instance = item.instance,
                    isRevealed = isRevealed,
                    contentMaxWidth = contentMaxWidth,
                    contentPreferredHeight = contentPreferredHeight,
                    viewLifecycleOwner = requireNotNull(viewLifecycleOwner),
                    isExpanded = isExpanded,
                    isActionsExpanded = isActionsExpanded,
                    alwaysRenderAsUnread = alwaysRenderAsUnread,
                    updateContent = true,
                    highlight = item.highlight,
                    highlightForever = item.highlightForever,
                    themeColor = themeColor,
                    isDuplicatePost = item.isDuplicatePost,
                    onRevealContentClickedFn = {
                        revealedItems.add(item.fetchedPost.postView.getUniqueKey())
                        notifyItemChanged(h.absoluteAdapterPosition)
                    },
                    onImageClick = onImageClick,
                    onShowMoreOptions = onShowMoreActions,
                    onVideoClick = onVideoClick,
                    onVideoLongClickListener = onVideoLongClickListener,
                    onPageClick = onPageClick,
                    onItemClick = onItemClick,
                    toggleItem = this::toggleItem,
                    toggleActions = this::toggleActions,
                    onSignInRequired = onSignInRequired,
                    onInstanceMismatch = onInstanceMismatch,
                    onHighlightComplete = {
                        clearHighlight()
                    },
                    onLinkClick = onLinkClick,
                    onLinkLongClick = onLinkLongClick,
                )
                h.root.setTag(R.id.post_item, true)
            }

            is PostListEngineItem.FilteredPostItem -> {
                val b = holder.getBinding<FilteredPostItemBinding>()

                b.text.text = context.getString(R.string.post_filtered_format, item.filterReason)
                b.root.setOnClickListener {
                    postListEngine.unfilter(item.fetchedPost.postView.post.id)
                    postListEngine.createItems()
                    refreshItems(true)
                }
                b.root.setTag(R.id.post_item, true)
            }

            is PostListEngineItem.AutoLoadItem -> {
                val b = holder.getBinding<AutoLoadItemBinding>()
                b.loadingView.showProgressBar()
            }

            PostListEngineItem.EndItem -> {}
            is PostListEngineItem.ErrorItem -> {
                val b = holder.getBinding<LoadingViewItemBinding>()
                if (item.isLoading) {
                    b.loadingView.showProgressBar()
                } else {
                    b.loadingView.showErrorWithRetry(item.message)
                }

                b.loadingView.setOnRefreshClickListener {
                    postListEngine.setPageItemLoading(item.pageToLoad)
                    onItemsChanged()

                    onLoadPage(item.pageToLoad)
                }
            }

            is PostListEngineItem.PersistentErrorItem -> {
                val b = holder.getBinding<PersistentErrorItemBinding>()
                b.errorText.text = item.exception.toErrorMessage(context)
            }

            is PostListEngineItem.ManualLoadItem -> {
                val b = holder.getBinding<ManualLoadItemBinding>()
                b.loadMoreText.text = context.getString(
                    R.string.load_more_page_format,
                    (item.pageToLoad + 1).toString(),
                )

                b.loadingView.visibility = View.GONE
                b.loadMoreText.visibility = View.VISIBLE

                fun loadPage() {
                    b.loadingView.visibility = View.VISIBLE
                    b.loadMoreText.visibility = View.GONE
                    onLoadPage(item.pageToLoad)
                }

                b.loadMoreText.setOnClickListener {
                    loadPage()
                }
                b.root.setOnClickListener {
                    loadPage()
                }
            }

            is PostListEngineItem.PageTitle -> {
                val b = holder.getBinding<PageTitleItemBinding>()

                b.title.text = context.getString(
                    R.string.page_format,
                    (item.pageIndex + 1).toString(),
                )
            }
            PostListEngineItem.HeaderItem -> {}
        }
    }

    override fun onViewDetachedFromWindow(holder: RecyclerView.ViewHolder) {
        val position = holder.absoluteAdapterPosition

        super.onViewDetachedFromWindow(holder)

        if (holder is ListingItemViewHolder) {
            if (markPostsAsReadOnScroll) {
                val fetchedPost = holder.root.getTag(R.id.fetched_post) as? FetchedPost
                if (fetchedPost != null && seenItemPositions.contains(position)) {
                    onPostRead(
                        fetchedPost.source.accountId,
                        fetchedPost.postView,
                    )
                }
            }
        }
    }

    override fun onViewRecycled(holder: RecyclerView.ViewHolder) {
        super.onViewRecycled(holder)

        if (holder is ListingItemViewHolder) {
            postListViewBuilder.recycle(
                holder,
            )
        }
    }

    private fun toggleItem(fetchedPost: FetchedPost) {
        val isExpanded = postListEngine.toggleItem(fetchedPost.postView)

        if (isExpanded) {
            onPostRead(
                fetchedPost.source.accountId,
                fetchedPost.postView,
            )
        }

        postListEngine.createItems()
        refreshItems(animate = true)
    }

    private fun toggleActions(fetchedPost: FetchedPost) {
        postListEngine.toggleActions(fetchedPost.postView)
        postListEngine.createItems()
        refreshItems(animate = true)
    }

    override fun getItemCount(): Int = items.size

    fun onItemsChanged(animate: Boolean = true) {
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

    fun endHighlightForever() {
        val index = postListEngine.endHighlightForever()

        if (index >= 0) {
            postListEngine.createItems()
            refreshItems(animate = false)
        }
    }

    fun refreshItems(animate: Boolean) {
        val newItems = postListEngine.items
        val oldItems = items

        val diff = DiffUtil.calculateDiff(
            object : DiffUtil.Callback() {
                override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                    val oldItem = oldItems[oldItemPosition]
                    val newItem = newItems[newItemPosition]

                    return oldItem::class == newItem::class && when (oldItem) {
                        is PostListEngineItem.FooterItem -> true
                        is PostListEngineItem.PostItem ->
                            oldItem.fetchedPost.postView.getUniqueKey() ==
                                (newItem as PostListEngineItem.PostItem).fetchedPost.postView.getUniqueKey()
                        is PostListEngineItem.AutoLoadItem ->
                            oldItem.pageToLoad ==
                                (newItem as PostListEngineItem.AutoLoadItem).pageToLoad

                        PostListEngineItem.EndItem -> true
                        PostListEngineItem.FooterSpacerItem -> true
                        is PostListEngineItem.ErrorItem ->
                            oldItem.pageToLoad ==
                                (newItem as PostListEngineItem.ErrorItem).pageToLoad

                        is PostListEngineItem.PersistentErrorItem ->
                            oldItem.exception == (newItem as PostListEngineItem.PersistentErrorItem).exception

                        is PostListEngineItem.ManualLoadItem ->
                            oldItem.pageToLoad ==
                                (newItem as PostListEngineItem.ManualLoadItem).pageToLoad

                        is PostListEngineItem.PageTitle ->
                            oldItem.pageIndex ==
                                (newItem as PostListEngineItem.PageTitle).pageIndex

                        PostListEngineItem.HeaderItem -> true
                    }
                }

                override fun getOldListSize(): Int = oldItems.size

                override fun getNewListSize(): Int = newItems.size

                override fun areContentsTheSame(
                    oldItemPosition: Int,
                    newItemPosition: Int,
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
            },
        )
        this.items = newItems
        diff.dispatchUpdatesTo(this)
    }

    fun updateWithPreferences(preferences: Preferences) {
        markPostsAsReadOnScroll = preferences.markPostsAsReadOnScroll
        blurNsfwPosts = preferences.blurNsfwPosts
    }

    fun updateNsfwMode(nsfwModeManager: NsfwModeManager) {
        val newValue = nsfwModeManager.nsfwModeEnabled.value
        if (nsfwMode == newValue) {
            return
        }

        nsfwMode = newValue

        @Suppress("NotifyDataSetChanged")
        notifyDataSetChanged()
    }

    fun clearHighlight() {
        postListEngine.clearHighlight()
        postListEngine.createItems()
        refreshItems(animate = false)
    }
}
