package com.idunnololz.summit.lemmy.community

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.idunnololz.summit.R
import com.idunnololz.summit.api.dto.PostView
import com.idunnololz.summit.api.utils.getUniqueKey
import com.idunnololz.summit.databinding.AutoLoadItemBinding
import com.idunnololz.summit.databinding.GenericSpaceFooterItemBinding
import com.idunnololz.summit.databinding.ListingItemCard2Binding
import com.idunnololz.summit.databinding.ListingItemCard3Binding
import com.idunnololz.summit.databinding.ListingItemCardBinding
import com.idunnololz.summit.databinding.ListingItemCompactBinding
import com.idunnololz.summit.databinding.ListingItemFullBinding
import com.idunnololz.summit.databinding.ListingItemLargeListBinding
import com.idunnololz.summit.databinding.ListingItemListBinding
import com.idunnololz.summit.databinding.LoadingViewItemBinding
import com.idunnololz.summit.databinding.MainFooterItemBinding
import com.idunnololz.summit.databinding.PersistentErrorItemBinding
import com.idunnololz.summit.databinding.PostListEndItemBinding
import com.idunnololz.summit.lemmy.CommunityRef
import com.idunnololz.summit.lemmy.PageRef
import com.idunnololz.summit.lemmy.PostRef
import com.idunnololz.summit.lemmy.postListView.ListingItemViewHolder
import com.idunnololz.summit.lemmy.postListView.PostListViewBuilder
import com.idunnololz.summit.links.LinkType
import com.idunnololz.summit.preferences.Preferences
import com.idunnololz.summit.preview.VideoType
import com.idunnololz.summit.util.recyclerView.ViewBindingViewHolder
import com.idunnololz.summit.util.recyclerView.getBinding
import com.idunnololz.summit.util.toErrorMessage
import com.idunnololz.summit.video.VideoState

class ListingItemAdapter(
    private val postListViewBuilder: PostListViewBuilder,
    private val context: Context,
    private var postListEngine: PostListEngine,
    private val onNextClick: () -> Unit,
    private val onPrevClick: () -> Unit,
    private val onSignInRequired: () -> Unit,
    private val onInstanceMismatch: (String, String) -> Unit,
    private val onImageClick: (PostView, View?, String) -> Unit,
    private val onVideoClick: (String, VideoType, VideoState?) -> Unit,
    private val onPageClick: (PageRef) -> Unit,
    private val onItemClick: (
        instance: String,
        id: Int,
        currentCommunity: CommunityRef?,
        post: PostView,
        jumpToComments: Boolean,
        reveal: Boolean,
        videoState: VideoState?,
    ) -> Unit,
    private val onShowMoreActions: (PostView) -> Unit,
    private val onPostRead: (PostView) -> Unit,
    private val onLoadPage: (Int) -> Unit,
    private val onLinkClick: (url: String, text: String?, linkType: LinkType) -> Unit,
    private val onLinkLongClick: (url: String, text: String?) -> Unit,
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val inflater = LayoutInflater.from(context)

    var markPostsAsReadOnScroll: Boolean = false
    var alwaysRenderAsUnread: Boolean = false
    var blurNsfwPosts: Boolean = true

    var items: List<Item> = listOf()
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
        is Item.PostItem -> when (layout) {
            CommunityLayout.Compact -> R.layout.listing_item_compact
            CommunityLayout.List -> R.layout.listing_item_list
            CommunityLayout.LargeList -> R.layout.listing_item_large_list
            CommunityLayout.Card -> R.layout.listing_item_card
            CommunityLayout.Card2 -> R.layout.listing_item_card2
            CommunityLayout.Card3 -> R.layout.listing_item_card3
            CommunityLayout.Full -> R.layout.listing_item_full
        }
        is Item.FooterItem -> R.layout.main_footer_item
        is Item.AutoLoadItem -> R.layout.auto_load_item
        Item.EndItem -> R.layout.post_list_end_item
        Item.FooterSpacerItem -> R.layout.generic_space_footer_item
        is Item.ErrorItem -> R.layout.loading_view_item
        is Item.PersistentErrorItem -> R.layout.persistent_error_item
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
            else -> throw RuntimeException("Unknown view type: $viewType")
        }
    }

    override fun onBindViewHolder(
        holder: RecyclerView.ViewHolder,
        position: Int,
        payloads: MutableList<Any>,
    ) {
        when (val item = items[position]) {
            is Item.PostItem -> {
                if (payloads.isEmpty()) {
                    super.onBindViewHolder(holder, position, payloads)
                } else {
                    val h: ListingItemViewHolder = holder as ListingItemViewHolder
                    val isRevealed = revealedItems.contains(item.postView.getUniqueKey()) ||
                        !blurNsfwPosts
                    val isActionsExpanded = item.isActionExpanded
                    val isExpanded = item.isExpanded

                    h.root.setTag(R.id.post_view, item.postView)

                    postListViewBuilder.bind(
                        holder = h,
                        postView = item.postView,
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
            is Item.FooterSpacerItem -> {}
            is Item.FooterItem -> {
                val b = holder.getBinding<MainFooterItemBinding>()
                if (item.hasMore) {
                    b.nextButton.visibility = View.VISIBLE
                    b.nextButton.setOnClickListener {
                        if (markPostsAsReadOnScroll) {
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
                if (item.hasLess) {
                    b.prevButton.visibility = View.VISIBLE
                    b.prevButton.setOnClickListener {
                        onPrevClick()
                    }
                } else {
                    b.prevButton.visibility = View.INVISIBLE
                }
            }
            is Item.PostItem -> {
                val h: ListingItemViewHolder = holder as ListingItemViewHolder
                val isRevealed = revealedItems.contains(item.postView.getUniqueKey()) ||
                    !blurNsfwPosts
                val isActionsExpanded = item.isActionExpanded
                val isExpanded = item.isExpanded

                h.root.setTag(R.id.post_view, item.postView)
                h.root.setTag(R.id.swipeable, true)

                postListViewBuilder.bind(
                    holder = h,
                    postView = item.postView,
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
                    },
                    onLinkClick = onLinkClick,
                    onLinkLongClick = onLinkLongClick,
                )
            }

            is Item.AutoLoadItem -> {
                val b = holder.getBinding<AutoLoadItemBinding>()
                b.loadingView.showProgressBar()
            }

            Item.EndItem -> {}
            is Item.ErrorItem -> {
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

            is Item.PersistentErrorItem -> {
                val b = holder.getBinding<PersistentErrorItemBinding>()
                b.errorText.text = item.exception.toErrorMessage(context)
            }
        }
    }

    override fun onViewRecycled(holder: RecyclerView.ViewHolder) {
        val position = holder.absoluteAdapterPosition
        super.onViewRecycled(holder)

        if (holder is ListingItemViewHolder) {
            postListViewBuilder.recycle(
                holder,
            )

            if (markPostsAsReadOnScroll) {
                val postView = holder.root.getTag(R.id.post_view) as? PostView
                if (postView != null && seenItemPositions.contains(position)) {
                    onPostRead(postView)
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
        animate: Boolean = true,
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

        val diff = DiffUtil.calculateDiff(
            object : DiffUtil.Callback() {
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
                        Item.FooterSpacerItem -> true
                        is Item.ErrorItem ->
                            oldItem.pageToLoad ==
                                (newItem as Item.ErrorItem).pageToLoad

                        is Item.PersistentErrorItem ->
                            oldItem.exception == (newItem as Item.PersistentErrorItem).exception
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
}
