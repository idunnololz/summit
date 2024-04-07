package com.idunnololz.summit.lemmy.post

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePaddingRelative
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import arrow.core.Either
import com.idunnololz.summit.R
import com.idunnololz.summit.api.dto.CommentId
import com.idunnololz.summit.api.dto.CommentView
import com.idunnololz.summit.api.dto.PostId
import com.idunnololz.summit.api.dto.PostView
import com.idunnololz.summit.api.utils.getUniqueKey
import com.idunnololz.summit.databinding.GenericLoadingItemBinding
import com.idunnololz.summit.databinding.GenericSpaceFooterItemBinding
import com.idunnololz.summit.databinding.PostCommentCollapsedItemBinding
import com.idunnololz.summit.databinding.PostCommentExpandedCompactItemBinding
import com.idunnololz.summit.databinding.PostCommentExpandedItemBinding
import com.idunnololz.summit.databinding.PostCommentFilteredItemBinding
import com.idunnololz.summit.databinding.PostHeaderItemBinding
import com.idunnololz.summit.databinding.PostMissingCommentItemBinding
import com.idunnololz.summit.databinding.PostMoreCommentsItemBinding
import com.idunnololz.summit.databinding.PostPendingCommentCollapsedItemBinding
import com.idunnololz.summit.databinding.PostPendingCommentExpandedItemBinding
import com.idunnololz.summit.databinding.ScreenshotModeOptionsBinding
import com.idunnololz.summit.databinding.ViewAllCommentsBinding
import com.idunnololz.summit.lemmy.CommentNodeData
import com.idunnololz.summit.lemmy.PageRef
import com.idunnololz.summit.lemmy.flatten
import com.idunnololz.summit.lemmy.post.PostAdapter.Item.FooterItem
import com.idunnololz.summit.lemmy.post.PostAdapter.Item.HeaderItem
import com.idunnololz.summit.lemmy.post.PostAdapter.Item.MoreCommentsItem
import com.idunnololz.summit.lemmy.post.PostAdapter.Item.PendingCommentItem
import com.idunnololz.summit.lemmy.post.PostAdapter.Item.ProgressOrErrorItem
import com.idunnololz.summit.lemmy.post.QueryMatchHelper.HighlightTextData
import com.idunnololz.summit.lemmy.post.QueryMatchHelper.QueryResult
import com.idunnololz.summit.lemmy.postAndCommentView.CommentExpandedViewHolder
import com.idunnololz.summit.lemmy.postAndCommentView.PostAndCommentViewBuilder
import com.idunnololz.summit.lemmy.screenshotMode.ScreenshotModeViewModel
import com.idunnololz.summit.links.LinkContext
import com.idunnololz.summit.preview.VideoType
import com.idunnololz.summit.util.Utils
import com.idunnololz.summit.util.ext.getDimen
import com.idunnololz.summit.util.recyclerView.ViewBindingViewHolder
import com.idunnololz.summit.util.recyclerView.getBinding
import com.idunnololz.summit.util.recyclerView.isBinding
import com.idunnololz.summit.video.VideoState
import java.util.LinkedList

class PostAdapter(
    private val postAndCommentViewBuilder: PostAndCommentViewBuilder,
    private val context: Context,
    private val containerView: View,
    private val lifecycleOwner: LifecycleOwner,
    var instance: String,
    private val revealAll: Boolean,
    private val useFooter: Boolean,
    /**
     * Set this to true if this adapter is used for an embedded view.
     */
    private val isEmbedded: Boolean,
    private val videoState: VideoState?,
    private var autoCollapseCommentThreshold: Float,
    private val onRefreshClickCb: () -> Unit,
    private val onSignInRequired: () -> Unit,
    private val onInstanceMismatch: (String, String) -> Unit,
    private val onAddCommentClick: (Either<PostView, CommentView>) -> Unit,
    private val onImageClick: (Either<PostView, CommentView>?, View?, String) -> Unit,
    private val onVideoClick: (String, VideoType, VideoState?) -> Unit,
    private val onVideoLongClickListener: (url: String) -> Unit,
    private val onPageClick: (PageRef) -> Unit,
    private val onPostActionClick: (PostView, String, actionId: Int) -> Unit,
    private val onCommentActionClick: (CommentView, String, actionId: Int) -> Unit,
    private val onFetchComments: (CommentId) -> Unit,
    private val onLoadPost: (PostId) -> Unit,
    private val onLinkClick: (url: String, text: String?, linkContext: LinkContext) -> Unit,
    private val onLinkLongClick: (url: String, text: String?) -> Unit,
) : RecyclerView.Adapter<ViewHolder>() {

    interface ScreenshotOptions

    private sealed class Item(
        open val id: String,
    ) {

        data class HeaderItem(
            val postView: PostView,
            var videoState: VideoState?,
            val showBottomDivider: Boolean,
            val query: String?,
            val currentMatch: QueryResult?,
            val screenshotMode: Boolean,
        ) : Item(postView.getUniqueKey()), ScreenshotOptions

        data class VisibleCommentItem(
            val commentId: CommentId,
            val content: String,
            val comment: CommentView,
            val depth: Int,
            val baseDepth: Int,
            val isExpanded: Boolean,
            val isPending: Boolean,
            val view: PostViewModel.ListView.CommentListView,
            val childrenCount: Int,
            val isPostLocked: Boolean,
            val isUpdating: Boolean,
            val isDeleting: Boolean,
            val isRemoved: Boolean,
            val isActionsExpanded: Boolean,
            val isHighlighted: Boolean,
            val query: String?,
            val currentMatch: QueryResult?,
            val screenshotMode: Boolean,
        ) : Item(
            "comment_${comment.comment.id}",
        ),
            ScreenshotOptions

        data class FilteredCommentItem(
            val commentId: CommentId,
            val content: String,
            val comment: CommentView,
            val depth: Int,
            val baseDepth: Int,
            val isPending: Boolean,
            val view: PostViewModel.ListView.CommentListView,
            val childrenCount: Int,
            val isPostLocked: Boolean,
            val isUpdating: Boolean,
            val isDeleting: Boolean,
            val isRemoved: Boolean,
            val isActionsExpanded: Boolean,
            val isHighlighted: Boolean,
            val query: String?,
            val currentMatch: QueryResult?,
            val screenshotMode: Boolean,
        ) : Item(
            "comment_${comment.comment.id}",
        ),
            ScreenshotOptions

        data class PendingCommentItem(
            val commentId: CommentId?,
            val content: String,
            val author: String?,
            val depth: Int,
            val baseDepth: Int,
            val isExpanded: Boolean,
            val isPending: Boolean,
            val view: PostViewModel.ListView.PendingCommentListView,
            val childrenCount: Int,
            val query: String?,
            val screenshotMode: Boolean,
        ) : Item(
            "pending_comment_${view.pendingCommentView.id}",
        ),
            ScreenshotOptions

        data class MoreCommentsItem(
            val parentId: CommentId?,
            val moreCount: Int,
            val depth: Int,
            val baseDepth: Int,
            val screenshotMode: Boolean,
        ) : Item(
            "more_comments_$parentId",
        ),
            ScreenshotOptions

        data class MissingCommentItem(
            val view: PostViewModel.ListView.MissingCommentItem,
            val commentId: CommentId?,
            val depth: Int,
            val baseDepth: Int,
            val screenshotMode: Boolean,
            val isExpanded: Boolean,
        ) : Item(
            "deleted_$commentId",
        ),
            ScreenshotOptions

        class ProgressOrErrorItem(
            val error: Throwable? = null,
        ) : Item("wew_pls_no_progress")

        data class ViewAllComments(
            val postId: PostId,
            val screenshotMode: Boolean,
        ) : Item("view_all_yo"), ScreenshotOptions

        data object FooterItem : Item("footer")
    }

    private val inflater = LayoutInflater.from(context)

    private var items: List<Item> = listOf()

    private var parentHeight: Int = 0

    /**
     * Set of items that is hidden by default but is reveals (ie. nsfw or spoiler tagged)
     */
    private var revealedItems = mutableSetOf<String>()

    private var actionExpandedComments = mutableSetOf<CommentId>()
    private var includedInScreenshot = mutableSetOf<String>()

    private var rawData: PostViewModel.PostData? = null

    private var highlightedComment: CommentId = -1
    private var highlightedCommentForever: CommentId = -1

    private var topLevelCommentIndices = listOf<Int>()
    private var absolutionPositionToTopLevelCommentPosition = listOf<Int>()

    private var seenCommentIds = mutableSetOf<CommentId>()

    private var collapsedItemIds = mutableSetOf<Long>()

    private var query: String? = null
    var currentMatch: QueryResult? = null
        set(value) {
            field = value

            refreshItems(animate = false)
        }

    var isLoaded: Boolean = false
    var error: Throwable? = null
        set(value) {
            field = value

            refreshItems()
        }

    var contentMaxWidth = 0
        set(value) {
            if (value == field) {
                return
            }
            field = value

            val indentationPerLevelPx = Utils.convertDpToPixel(
                postAndCommentViewBuilder.commentUiConfig.indentationPerLevelDp.toFloat(),
            )
            maxDepth = ((contentMaxWidth / 2) / indentationPerLevelPx).toInt()

            for (i in 0..items.lastIndex) {
                if (items[i] is HeaderItem) {
                    notifyItemChanged(i)
                }
            }
        }

    var maxDepth: Int = Int.MAX_VALUE

    var isScreenshoting: Boolean = false
    var screenshotMaxWidth: Int = 0
    var screenshotConfig: ScreenshotModeViewModel.ScreenshotConfig? = null
        set(value) {
            if (value == field) {
                return
            }
            field = value

            refreshItems()
        }

    var screenshotMode: Boolean = false
        set(value) {
            if (value == field) {
                return
            }
            field = value

            refreshItems()
        }

    override fun getItemViewType(position: Int): Int = when (val item = items[position]) {
        is HeaderItem -> R.layout.post_header_item
        is Item.VisibleCommentItem ->
            if (item.isExpanded) {
                if (postAndCommentViewBuilder.hideCommentActions) {
                    R.layout.post_comment_expanded_compact_item
                } else {
                    R.layout.post_comment_expanded_item
                }
            } else {
                R.layout.post_comment_collapsed_item
            }
        is Item.FilteredCommentItem ->
            R.layout.post_comment_filtered_item
        is PendingCommentItem ->
            if (item.isExpanded) {
                R.layout.post_pending_comment_expanded_item
            } else {
                R.layout.post_pending_comment_collapsed_item
            }
        is ProgressOrErrorItem -> R.layout.generic_loading_item
        is MoreCommentsItem -> R.layout.post_more_comments_item
        is FooterItem -> R.layout.generic_space_footer_item
        is Item.ViewAllComments -> R.layout.view_all_comments
        is Item.MissingCommentItem -> R.layout.post_missing_comment_item
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val v = inflater.inflate(viewType, parent, false)

        parentHeight = parent.height

        return when (viewType) {
            R.layout.post_header_item -> ViewBindingViewHolder(PostHeaderItemBinding.bind(v))
            R.layout.post_comment_expanded_item ->
                CommentExpandedViewHolder.fromBinding(PostCommentExpandedItemBinding.bind(v))
                    .apply {
                        headerView.textView2.setCompoundDrawablesRelativeWithIntrinsicBounds(
                            R.drawable.baseline_arrow_upward_16,
                            0,
                            0,
                            0,
                        )
                        headerView.textView2.compoundDrawablePadding =
                            Utils.convertDpToPixel(4f).toInt()

                        headerView.textView3.setCompoundDrawablesRelativeWithIntrinsicBounds(
                            R.drawable.baseline_arrow_downward_16,
                            0,
                            0,
                            0,
                        )
                        headerView.textView3.compoundDrawablePadding =
                            Utils.convertDpToPixel(4f).toInt()
                        headerView.textView3.updatePaddingRelative(
                            start = Utils.convertDpToPixel(8f).toInt(),
                        )
                    }
            R.layout.post_comment_expanded_compact_item ->
                CommentExpandedViewHolder.fromBinding(PostCommentExpandedCompactItemBinding.bind(v))
                    .apply {
                        headerView.textView2.setCompoundDrawablesRelativeWithIntrinsicBounds(
                            R.drawable.baseline_arrow_upward_16,
                            0,
                            0,
                            0,
                        )
                        headerView.textView2.compoundDrawablePadding =
                            Utils.convertDpToPixel(4f).toInt()

                        headerView.textView3.setCompoundDrawablesRelativeWithIntrinsicBounds(
                            R.drawable.baseline_arrow_downward_16,
                            0,
                            0,
                            0,
                        )
                        headerView.textView3.compoundDrawablePadding =
                            Utils.convertDpToPixel(4f).toInt()
                        headerView.textView3.updatePaddingRelative(
                            start = Utils.convertDpToPixel(8f).toInt(),
                        )
                    }
            R.layout.post_comment_filtered_item ->
                ViewBindingViewHolder(PostCommentFilteredItemBinding.bind(v))
            R.layout.post_comment_collapsed_item ->
                ViewBindingViewHolder(PostCommentCollapsedItemBinding.bind(v))
            R.layout.post_pending_comment_expanded_item ->
                ViewBindingViewHolder(PostPendingCommentExpandedItemBinding.bind(v))
            R.layout.post_pending_comment_collapsed_item ->
                ViewBindingViewHolder(PostPendingCommentCollapsedItemBinding.bind(v))
            R.layout.post_more_comments_item ->
                ViewBindingViewHolder(PostMoreCommentsItemBinding.bind(v))
            R.layout.generic_loading_item ->
                ViewBindingViewHolder(GenericLoadingItemBinding.bind(v))
            R.layout.generic_space_footer_item ->
                ViewBindingViewHolder(GenericSpaceFooterItemBinding.bind(v))
            R.layout.view_all_comments ->
                ViewBindingViewHolder(ViewAllCommentsBinding.bind(v))
            R.layout.post_missing_comment_item ->
                ViewBindingViewHolder(PostMissingCommentItemBinding.bind(v))
            else -> throw RuntimeException("ViewType: $viewType")
        }
    }

    override fun onBindViewHolder(
        holder: ViewHolder,
        position: Int,
        payloads: MutableList<Any>,
    ) {
        when (val item = items[position]) {
            is HeaderItem -> {
                if (payloads.isEmpty()) {
                    super.onBindViewHolder(holder, position, payloads)
                } else {
                    // this is an incremental update... Only update the stats, do not update content...
                    val b = holder.getBinding<PostHeaderItemBinding>()
                    val post = item.postView
                    val postKey = post.getUniqueKey()
                    val contentMaxWidth = if (isScreenshoting) {
                        screenshotMaxWidth
                    } else {
                        contentMaxWidth
                    }
                    val onImageClick = if (isScreenshoting) {
                        { _, _, _ -> }
                    } else {
                        onImageClick
                    }
                    val onVideoClick = if (isScreenshoting) {
                        { _, _, _ -> }
                    } else {
                        onVideoClick
                    }
                    val screenshotConfig = if (isScreenshoting) {
                        screenshotConfig
                    } else {
                        null
                    }

                    postAndCommentViewBuilder.bindPostView(
                        binding = b,
                        container = containerView,
                        postView = item.postView,
                        instance = instance,
                        isRevealed = revealAll || revealedItems.contains(postKey),
                        contentMaxWidth = contentMaxWidth,
                        viewLifecycleOwner = lifecycleOwner,
                        videoState = item.videoState,
                        updateContent = false,
                        highlightTextData = if (item.query == null) {
                            null
                        } else {
                            HighlightTextData(
                                item.query,
                                item.currentMatch?.relativeMatchIndex,
                                item.currentMatch?.targetSubtype,
                            )
                        },
                        onRevealContentClickedFn = {
                            revealedItems.add(postKey)
                            notifyItemChanged(holder.absoluteAdapterPosition)
                        },
                        onImageClick = onImageClick,
                        onVideoClick = onVideoClick,
                        onVideoLongClickListener = onVideoLongClickListener,
                        onPageClick = onPageClick,
                        onAddCommentClick = onAddCommentClick,
                        onPostActionClick = { postView, actionId ->
                            onPostActionClick(postView, item.id, actionId)
                        },
                        onSignInRequired = onSignInRequired,
                        onInstanceMismatch = onInstanceMismatch,
                        onLinkClick = onLinkClick,
                        onLinkLongClick = onLinkLongClick,
                        screenshotConfig = screenshotConfig,
                    )

                    if (item.showBottomDivider) {
                        b.bottomDivider.visibility = View.VISIBLE
                    } else {
                        b.bottomDivider.visibility = View.GONE
                    }

                    updateScreenshotMode(
                        viewHolder = holder,
                        screenshotMode = item.screenshotMode,
                        startGuideline = b.startGuideline,
                        root = b.root,
                        item = item,
                    )
                }
            }
            else -> super.onBindViewHolder(holder, position, payloads)
        }
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        when (val item = items[position]) {
            is HeaderItem -> {
                val b = holder.getBinding<PostHeaderItemBinding>()
                val post = item.postView
                val postKey = post.getUniqueKey()
                val contentMaxWidth = if (isScreenshoting) {
                    screenshotMaxWidth
                } else {
                    contentMaxWidth
                }
                val onImageClick = if (isScreenshoting) {
                    { _, _, _ -> }
                } else {
                    onImageClick
                }
                val onVideoClick = if (isScreenshoting) {
                    { _, _, _ -> }
                } else {
                    onVideoClick
                }
                val screenshotConfig = if (isScreenshoting) {
                    screenshotConfig
                } else {
                    null
                }

                holder.itemView.setTag(R.id.swipeable, true)

                postAndCommentViewBuilder.bindPostView(
                    binding = b,
                    container = containerView,
                    postView = item.postView,
                    instance = instance,
                    isRevealed = revealAll || revealedItems.contains(postKey),
                    contentMaxWidth = contentMaxWidth,
                    viewLifecycleOwner = lifecycleOwner,
                    videoState = item.videoState,
                    updateContent = true,
                    highlightTextData = if (item.query == null) {
                        null
                    } else {
                        HighlightTextData(
                            item.query,
                            item.currentMatch?.relativeMatchIndex,
                            item.currentMatch?.targetSubtype,
                        )
                    },
                    onRevealContentClickedFn = {
                        revealedItems.add(postKey)
                        notifyItemChanged(holder.absoluteAdapterPosition)
                    },
                    onImageClick = onImageClick,
                    onVideoClick = onVideoClick,
                    onVideoLongClickListener = onVideoLongClickListener,
                    onPageClick = onPageClick,
                    onAddCommentClick = onAddCommentClick,
                    onPostActionClick = { postView, actionId ->
                        onPostActionClick(postView, item.id, actionId)
                    },
                    onSignInRequired = onSignInRequired,
                    onInstanceMismatch = onInstanceMismatch,
                    onLinkClick = onLinkClick,
                    onLinkLongClick = onLinkLongClick,
                    screenshotConfig = screenshotConfig,
                )

                if (item.showBottomDivider) {
                    b.bottomDivider.visibility = View.VISIBLE
                } else {
                    b.bottomDivider.visibility = View.GONE
                }

                if (isScreenshoting && screenshotConfig?.showPostDivider == false) {
                    b.bottomDivider.visibility = View.GONE
                }

                updateScreenshotMode(holder, item.screenshotMode, b.startGuideline, b.root, item)
            }
            is Item.VisibleCommentItem -> {
                if (item.isExpanded) {
                    val b = holder as CommentExpandedViewHolder
                    val highlight = highlightedComment == item.commentId
                    val highlightForever = highlightedCommentForever == item.commentId

                    postAndCommentViewBuilder.bindCommentViewExpanded(
                        h = holder,
                        holder = b,
                        baseDepth = item.baseDepth,
                        depth = item.depth,
                        maxDepth = maxDepth,
                        commentView = item.comment,
                        isDeleting = item.isDeleting,
                        isRemoved = item.isRemoved,
                        content = item.content,
                        instance = instance,
                        isPostLocked = item.isPostLocked,
                        isUpdating = item.isUpdating,
                        highlight = highlight,
                        highlightForever = highlightForever,
                        viewLifecycleOwner = lifecycleOwner,
                        isActionsExpanded = item.isActionsExpanded,
                        onImageClick = onImageClick,
                        onVideoClick = onVideoClick,
                        onPageClick = onPageClick,
                        highlightTextData = if (item.query == null) {
                            null
                        } else {
                            HighlightTextData(
                                item.query,
                                item.currentMatch?.relativeMatchIndex,
                                item.currentMatch?.targetSubtype,
                            )
                        },
                        collapseSection = {
                            collapseSection(holder.bindingAdapterPosition)
                        },
                        toggleActionsExpanded = {
                            toggleActions(item.comment.comment.id)
                        },
                        onCommentActionClick = { commentView, actionId ->
                            onCommentActionClick(commentView, item.id, actionId)
                        },
                        onLinkLongClick = onLinkLongClick,
                        onSignInRequired = onSignInRequired,
                        onLinkClick = onLinkClick,
                        onInstanceMismatch = onInstanceMismatch,
                    )

                    updateScreenshotMode(holder, item.screenshotMode, b.startGuideline, b.root, item)
                } else {
                    // collapsed
                    val b = holder.getBinding<PostCommentCollapsedItemBinding>()
                    val highlight = highlightedComment == item.commentId
                    val highlightForever = highlightedCommentForever == item.commentId

                    postAndCommentViewBuilder.bindCommentViewCollapsed(
                        h = holder,
                        binding = b,
                        baseDepth = item.baseDepth,
                        depth = item.depth,
                        maxDepth = maxDepth,
                        childrenCount = item.childrenCount,
                        highlight = highlight,
                        highlightForever = highlightForever,
                        isUpdating = item.isUpdating,
                        commentView = item.comment,
                        instance = instance,
                        expandSection = ::expandSection,
                        onPageClick = onPageClick,
                        onLinkClick = onLinkClick,
                        onLinkLongClick = onLinkLongClick,
                    )

                    updateScreenshotMode(
                        viewHolder = holder,
                        screenshotMode = item.screenshotMode,
                        startGuideline = b.startGuideline,
                        root = b.root,
                        item = item,
                    )
                }

                holder.itemView.setTag(R.id.swipeable, true)
                holder.itemView.setTag(R.id.comment_view, item.comment)
                holder.itemView.setTag(R.id.expanded, item.isExpanded)
            }
            is Item.FilteredCommentItem -> {
                val b = holder.getBinding<PostCommentFilteredItemBinding>()

                postAndCommentViewBuilder.bindCommentFilteredItem(
                    b = b,
                    baseDepth = item.baseDepth,
                    depth = item.depth,
                    maxDepth = maxDepth,
                    onTap = {
                        showFilteredComment(item)
                    },
                )
            }
            is PendingCommentItem -> {
                if (item.isExpanded) {
                    val b = holder.getBinding<PostPendingCommentExpandedItemBinding>()
                    val highlight = highlightedComment == item.commentId
                    val highlightForever = highlightedCommentForever == item.commentId

                    postAndCommentViewBuilder.bindPendingCommentViewExpanded(
                        h = holder,
                        binding = b,
                        baseDepth = item.baseDepth,
                        depth = item.depth,
                        maxDepth = maxDepth,
                        content = item.content,
                        instance = instance,
                        author = item.author,
                        highlight = highlight,
                        highlightForever = highlightForever,
                        onImageClick = onImageClick,
                        onVideoClick = onVideoClick,
                        onPageClick = onPageClick,
                        onLinkClick = onLinkClick,
                        onLinkLongClick = onLinkLongClick,
                        collapseSection = ::collapseSection,
                    )

                    updateScreenshotMode(holder, item.screenshotMode, b.startGuideline, b.root, item)
                } else {
                    // collapsed
                    val b = holder.getBinding<PostPendingCommentCollapsedItemBinding>()
                    val highlight = highlightedComment == item.commentId
                    val highlightForever = highlightedCommentForever == item.commentId

                    postAndCommentViewBuilder.bindPendingCommentViewCollapsed(
                        holder = holder,
                        binding = b,
                        baseDepth = item.baseDepth,
                        depth = item.depth,
                        maxDepth = maxDepth,
                        author = item.author,
                        highlight = highlight,
                        highlightForever = highlightForever,
                        expandSection = ::collapseSection,
                    )

                    updateScreenshotMode(holder, item.screenshotMode, b.startGuideline, b.root, item)
                }
                holder.itemView.setTag(R.id.expanded, item.isExpanded)
            }
            is ProgressOrErrorItem -> {
                val b = holder.getBinding<GenericLoadingItemBinding>()
                if (item.error != null) {
                    b.loadingView.showDefaultErrorMessageFor(item.error)
                } else if (!isLoaded) {
                    b.loadingView.showProgressBar()
                }
                b.loadingView.setOnRefreshClickListener {
                    onRefreshClickCb()
                }
            }
            is MoreCommentsItem -> {
                val b = holder.getBinding<PostMoreCommentsItemBinding>()

                postAndCommentViewBuilder.bindMoreCommentsItem(
                    b = b,
                    depth = item.depth,
                    baseDepth = item.baseDepth,
                    maxDepth = maxDepth,
                )
                b.moreButton.text = context.resources.getQuantityString(
                    R.plurals.more_replies_format,
                    item.moreCount,
                    item.moreCount,
                )

                b.moreButton.setOnClickListener {
                    if (item.parentId != null) {
                        onFetchComments(item.parentId)
                    }
                }
                updateScreenshotMode(holder, item.screenshotMode, b.startGuideline, b.root, item)
            }

            FooterItem -> {}
            is Item.ViewAllComments -> {
                val b = holder.getBinding<ViewAllCommentsBinding>()
                b.button.setOnClickListener {
                    onLoadPost(item.postId)
                }
                updateScreenshotMode(holder, item.screenshotMode, b.startGuideline, b.root, item)
            }

            is Item.MissingCommentItem -> {
                val b = holder.getBinding<PostMissingCommentItemBinding>()

                postAndCommentViewBuilder.bindMissingCommentItem(
                    b = b,
                    depth = item.depth,
                    baseDepth = item.baseDepth,
                    maxDepth = maxDepth,
                    isExpanded = item.isExpanded,
                    onToggleClick = {
                        toggleSection(holder.absoluteAdapterPosition)
                    },
                )
            }
        }
    }

    override fun onViewRecycled(holder: ViewHolder) {
        super.onViewRecycled(holder)

        val item = items.getOrNull(holder.absoluteAdapterPosition)

        if (holder.isBinding<PostHeaderItemBinding>()) {
            val b = holder.getBinding<PostHeaderItemBinding>()
            val state = postAndCommentViewBuilder.recycle(b)

            (item as? HeaderItem)?.videoState = state.videoState
        } else if (holder is CommentExpandedViewHolder) {
            postAndCommentViewBuilder.recycle(holder)
        }
    }

    override fun getItemCount(): Int = items.size

    private fun updateScreenshotMode(
        viewHolder: ViewHolder,
        screenshotMode: Boolean,
        startGuideline: View,
        root: ConstraintLayout,
        item: Item,
    ) {
        if (isScreenshoting) {
            return
        }

        if (screenshotMode) {
            startGuideline.updateLayoutParams<ConstraintLayout.LayoutParams> {
                marginStart = context.getDimen(R.dimen.screenshot_options_size)
            }

            val previousBinding = root.getTag(R.id.screenshot_options_view)
                as? ScreenshotModeOptionsBinding
            val binding = previousBinding ?: ScreenshotModeOptionsBinding.inflate(
                LayoutInflater.from(context),
                root,
                false,
            )

            binding.root.updateLayoutParams<ConstraintLayout.LayoutParams> { }
            binding.root.setOnClickListener {
                binding.screenshotCb.isChecked = !binding.screenshotCb.isChecked
            }
            binding.screenshotCb.setOnCheckedChangeListener(null)
            binding.screenshotCb.isChecked = includedInScreenshot.contains(item.id)
            binding.screenshotCb.setOnCheckedChangeListener a@{ _, isChecked ->
                val itemClicked = items.getOrNull(viewHolder.absoluteAdapterPosition) ?: return@a

                if (isChecked) {
                    selectItemForScreenshot(itemClicked.id)
                } else {
                    deselectItemForScreenshot(itemClicked.id)
                }
            }

            if (previousBinding == null) {
                root.addView(binding.root)
                root.setTag(R.id.screenshot_mode, true)
                root.setTag(R.id.screenshot_options_view, binding)
            }
        } else {
            if (root.getTag(R.id.screenshot_mode) as? Boolean != true) {
                return
            }

            (root.getTag(R.id.screenshot_options_view) as? ScreenshotModeOptionsBinding)?.let {
                root.removeView(it.root)
                startGuideline.updateLayoutParams<ConstraintLayout.LayoutParams> {
                    marginStart = 0
                }
            }

            root.setTag(R.id.screenshot_mode, false)
            root.setTag(R.id.screenshot_options_view, null)
        }
    }

    private fun toggleItemForScreenshot(id: String) {
        if (includedInScreenshot.contains(id)) {
            deselectItemForScreenshot(id)
        } else {
            selectItemForScreenshot(id)
        }
    }

    fun selectItemForScreenshot(id: String) {
        includedInScreenshot.add(id)
    }

    fun deselectItemForScreenshot(id: String) {
        includedInScreenshot.remove(id)
    }

    /**
     * @param refreshHeader Pass false to not always refresh header. Useful for web view headers
     * as refreshing them might cause a relayout causing janky animations
     */
    private fun refreshItems(
        refreshHeader: Boolean = true,
        animate: Boolean = true,
        cb: () -> Unit = {},
    ) {
        val rawData = rawData
        val oldItems = items
        val query = query
        val currentMatch = currentMatch

        val topLevelCommentIndices = mutableListOf<Int>()
        val absolutionPositionToTopLevelCommentPosition = mutableListOf<Int>()

        val newItems =
            if (error == null) {
                rawData ?: return

                val finalItems = mutableListOf<Item>()

                val changed = autoCollapseComments(rawData.commentTree)

                val postView = rawData.postView
                val commentItems = rawData.commentTree.flatten(collapsedItemIds)
                finalItems += HeaderItem(
                    postView = postView.post,
                    videoState = videoState,
                    showBottomDivider = !isEmbedded || commentItems.isNotEmpty(),
                    query = query,
                    currentMatch = if (currentMatch?.targetId == postView.post.post.id) {
                        currentMatch
                    } else {
                        null
                    },
                    screenshotMode,
                )

                absolutionPositionToTopLevelCommentPosition += -1

                if (rawData.isSingleComment) {
                    finalItems += Item.ViewAllComments(postView.post.post.id, screenshotMode)
                    absolutionPositionToTopLevelCommentPosition += -1
                }

                var lastTopLevelCommentPosition = -1

                for (commentItem in commentItems) {
                    var depth = -1

                    when (val commentView = commentItem.listView) {
                        is PostViewModel.ListView.CommentListView -> {
                            val commentId = commentView.comment.comment.id
                            val isCurrentMatchThisComment = currentMatch?.targetId == commentId
                            val isDeleting =
                                commentView.pendingCommentView?.isActionDelete == true
                            val show = when (commentView) {
                                is PostViewModel.ListView.FilteredCommentItem -> commentView.show
                                is PostViewModel.ListView.VisibleCommentListView -> true
                            }

                            depth = commentItem.depth

                            if (depth == 0) {
                                lastTopLevelCommentPosition++
                            }

                            finalItems +=
                                if (show) {
                                    Item.VisibleCommentItem(
                                        commentId = commentId,
                                        content =
                                        commentView.pendingCommentView?.content
                                            ?: commentView.comment.comment.content,
                                        comment = commentView.comment,
                                        depth = commentItem.depth,
                                        baseDepth = 0,
                                        isExpanded = !collapsedItemIds.contains(commentView.id) || isCurrentMatchThisComment,
                                        isPending = false,
                                        view = commentView,
                                        childrenCount = commentItem.children.size,
                                        isPostLocked = commentView.comment.post.locked,
                                        isUpdating = commentView.pendingCommentView != null,
                                        isDeleting = isDeleting,
                                        isRemoved = commentView.isRemoved,
                                        isActionsExpanded = actionExpandedComments.contains(
                                            commentId,
                                        ),
                                        isHighlighted = rawData.selectedCommentId == commentId,
                                        query = query,
                                        currentMatch = if (isCurrentMatchThisComment) {
                                            currentMatch
                                        } else {
                                            null
                                        },
                                        screenshotMode,
                                    )
                                } else {
                                    Item.FilteredCommentItem(
                                        commentId = commentId,
                                        content =
                                        commentView.pendingCommentView?.content
                                            ?: commentView.comment.comment.content,
                                        comment = commentView.comment,
                                        depth = commentItem.depth,
                                        baseDepth = 0,
                                        isPending = false,
                                        view = commentView,
                                        childrenCount = commentItem.children.size,
                                        isPostLocked = commentView.comment.post.locked,
                                        isUpdating = commentView.pendingCommentView != null,
                                        isDeleting = isDeleting,
                                        isRemoved = commentView.isRemoved,
                                        isActionsExpanded = actionExpandedComments.contains(
                                            commentId,
                                        ),
                                        isHighlighted = rawData.selectedCommentId == commentId,
                                        query = query,
                                        currentMatch = if (isCurrentMatchThisComment) {
                                            currentMatch
                                        } else {
                                            null
                                        },
                                        screenshotMode,
                                    )
                                }
                            absolutionPositionToTopLevelCommentPosition += lastTopLevelCommentPosition
                        }
                        is PostViewModel.ListView.PendingCommentListView -> {
                            depth = commentItem.depth
                            if (depth == 0) {
                                lastTopLevelCommentPosition++
                            }
                            finalItems += PendingCommentItem(
                                commentId = commentView.pendingCommentView.commentId,
                                content = commentView.pendingCommentView.content,
                                author = commentView.author,
                                depth = commentItem.depth,
                                baseDepth = 0,
                                isExpanded = !collapsedItemIds.contains(commentView.id),
                                isPending = false,
                                view = commentView,
                                childrenCount = commentItem.children.size,
                                query = query,
                                screenshotMode,
                            )
                            absolutionPositionToTopLevelCommentPosition += lastTopLevelCommentPosition
                        }
                        is PostViewModel.ListView.PostListView -> {
                            // should never happen
                        }
                        is PostViewModel.ListView.MoreCommentsItem -> {
                            if (commentView.parentCommentId != null) {
                                depth = commentItem.depth
                                if (depth == 0) {
                                    lastTopLevelCommentPosition++
                                }
                                finalItems += MoreCommentsItem(
                                    parentId = commentView.parentCommentId,
                                    moreCount = commentView.moreCount,
                                    depth = commentItem.depth,
                                    baseDepth = 0,
                                    screenshotMode,
                                )
                                absolutionPositionToTopLevelCommentPosition += lastTopLevelCommentPosition
                            }
                        }

                        is PostViewModel.ListView.MissingCommentItem -> {
                            finalItems += Item.MissingCommentItem(
                                view = commentView,
                                commentId = commentView.commentId,
                                depth = commentItem.depth,
                                baseDepth = 0,
                                screenshotMode,
                                isExpanded = !collapsedItemIds.contains(commentView.id),
                            )
                        }
                    }

                    if (depth == 0) {
                        topLevelCommentIndices += finalItems.lastIndex
                    }
                }

                if (!isLoaded) {
                    finalItems += listOf(ProgressOrErrorItem())
                }

                if (useFooter) {
                    finalItems += FooterItem
                }

                finalItems
            } else {
                val finalItems = mutableListOf<Item>()
                rawData?.postView?.let {
                    finalItems += HeaderItem(
                        postView = it.post,
                        videoState = videoState,
                        showBottomDivider = true,
                        query = query,
                        currentMatch = if (currentMatch?.targetId == it.post.post.id) {
                            currentMatch
                        } else {
                            null
                        },
                        screenshotMode,
                    )
                }
                finalItems += ProgressOrErrorItem(error)

                finalItems
            }

        val diff = DiffUtil.calculateDiff(
            object : DiffUtil.Callback() {
                override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                    return oldItems[oldItemPosition].id == newItems[newItemPosition].id
                }

                override fun getOldListSize(): Int = oldItems.size

                override fun getNewListSize(): Int = newItems.size

                override fun areContentsTheSame(
                    oldItemPosition: Int,
                    newItemPosition: Int,
                ): Boolean {
                    val oldItem = oldItems[oldItemPosition]
                    val newItem = newItems[newItemPosition]
                    return when (oldItem) {
                        is HeaderItem ->
                            oldItem.postView.post == (newItem as HeaderItem).postView.post
                        else -> oldItem == newItem
                    }
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
        if (refreshHeader) {
            notifyItemChanged(0, Unit)
        }

        this.topLevelCommentIndices = topLevelCommentIndices
        this.absolutionPositionToTopLevelCommentPosition = absolutionPositionToTopLevelCommentPosition

        cb()
    }

    private fun autoCollapseComments(commentTree: List<CommentNodeData>): Boolean {
        var changed = false

        val toVisit = LinkedList(commentTree)
        while (toVisit.isNotEmpty()) {
            val node = requireNotNull(toVisit.pollFirst())

            toVisit.addAll(node.children)

            when (val commentView = node.listView) {
                is PostViewModel.ListView.VisibleCommentListView -> {
                    val commentId = commentView.comment.comment.id
                    if (seenCommentIds.add(commentId)) {
                        val upvotes = commentView.comment.counts.upvotes
                        val downvotes = commentView.comment.counts.downvotes
                        val totalVotes = upvotes + downvotes
                        val upvoteRate = upvotes.toFloat() / totalVotes
                        val autoCollapse =
                            upvoteRate < autoCollapseCommentThreshold && totalVotes >= 10

                        if (autoCollapse &&
                            !collapsedItemIds.contains(commentView.id)
                        ) {
                            collapsedItemIds.add(commentView.id)
                            changed = true
                        }
                    }
                }
                is PostViewModel.ListView.FilteredCommentItem -> {
                }
                is PostViewModel.ListView.MissingCommentItem -> {
                }
                is PostViewModel.ListView.MoreCommentsItem -> {
                }
                is PostViewModel.ListView.PendingCommentListView -> {
                }
                is PostViewModel.ListView.PostListView -> {
                }
            }
        }
        return changed
    }

    fun getPositionOfComment(commentId: CommentId): Int =
        items.indexOfFirst {
            when (it) {
                is Item.VisibleCommentItem -> it.commentId == commentId
                is Item.FilteredCommentItem -> it.commentId == commentId
                is HeaderItem -> false
                is MoreCommentsItem -> false
                is PendingCommentItem -> it.commentId == commentId
                is ProgressOrErrorItem -> false
                FooterItem -> false
                is Item.ViewAllComments -> false
                is Item.MissingCommentItem -> false
            }
        }

    fun getPrevTopLevelCommentPosition(position: Int): Int? {
        val topLevelPosition = absolutionPositionToTopLevelCommentPosition.getOrNull(position)
            ?: if (position in 0 until itemCount) {
                return 0
            } else {
                return null
            }

        val curP = topLevelCommentIndices.getOrNull(topLevelPosition)
        return if (position == curP) {
            topLevelCommentIndices.getOrNull(topLevelPosition - 1) ?: 0
        } else {
            curP
        }
    }

    fun getNextTopLevelCommentPosition(position: Int): Int? {
        val topLevelPosition = absolutionPositionToTopLevelCommentPosition.getOrNull(position)
            ?: return null
        return topLevelCommentIndices.getOrNull(topLevelPosition + 1)
    }

    fun setStartingData(data: PostViewModel.PostData) {
        rawData = data

        refreshItems()
    }

    fun hasStartingData(): Boolean = rawData?.postView != null

    fun setData(data: PostViewModel.PostData) {
        if (!isLoaded) {
            isLoaded = true
        }

        error = null
        rawData = data

        refreshItems()
    }

    fun highlightComment(commentId: CommentId) {
        val pos = getPositionOfComment(commentId)
        if (pos == -1) {
            return
        }

        highlightedComment = commentId
        highlightedCommentForever = -1

        notifyItemChanged(pos, Unit)
    }

    fun highlightCommentForever(commentId: CommentId) {
        val pos = getPositionOfComment(commentId)
        if (pos == -1) {
            return
        }

        this.highlightedComment = -1
        this.highlightedCommentForever = commentId

        notifyItemChanged(pos, Unit)
    }

    fun clearHighlightComment() {
        val pos = getPositionOfComment(highlightedComment)
        if (pos == -1) {
            return
        }

        highlightedComment = -1
        notifyItemChanged(pos, Unit)
    }

    private fun collapseSection(position: Int) {
        if (position < 0) return

        when (val item = items[position]) {
            is Item.VisibleCommentItem -> collapsedItemIds.add(item.view.id)
            is Item.FilteredCommentItem -> collapsedItemIds.add(item.view.id)
            is PendingCommentItem -> collapsedItemIds.add(item.view.id)
            is Item.MissingCommentItem -> collapsedItemIds.add(item.view.id)
            FooterItem,
            is HeaderItem,
            is MoreCommentsItem,
            is ProgressOrErrorItem,
            is Item.ViewAllComments,
            -> {}
        }

        refreshItems(refreshHeader = false)
    }

    private fun expandSection(position: Int) {
        if (position < 0) return

        when (val item = items[position]) {
            is Item.VisibleCommentItem -> collapsedItemIds.remove(item.view.id)
            is Item.FilteredCommentItem -> collapsedItemIds.remove(item.view.id)
            is PendingCommentItem -> collapsedItemIds.remove(item.view.id)
            is Item.MissingCommentItem -> collapsedItemIds.remove(item.view.id)
            FooterItem,
            is HeaderItem,
            is MoreCommentsItem,
            is ProgressOrErrorItem,
            is Item.ViewAllComments,
            -> {}
        }

        refreshItems(refreshHeader = false)
    }

    private fun showFilteredComment(commentItem: Item.FilteredCommentItem) {
        val filteredComment = commentItem.view as? PostViewModel.ListView.FilteredCommentItem
            ?: return
        filteredComment.show = true

        refreshItems()
    }

    fun toggleSection(position: Int) {
        val isCollapsed = when (val item = items[position]) {
            is Item.VisibleCommentItem -> collapsedItemIds.contains(item.view.id)
            is Item.FilteredCommentItem -> collapsedItemIds.contains(item.view.id)
            is PendingCommentItem -> collapsedItemIds.contains(item.view.id)
            is Item.MissingCommentItem -> collapsedItemIds.contains(item.view.id)
            FooterItem,
            is HeaderItem,
            is MoreCommentsItem,
            is ProgressOrErrorItem,
            is Item.ViewAllComments,
            -> null
        }

        if (isCollapsed == true) {
            expandSection(position)
        } else if (isCollapsed == false) {
            collapseSection(position)
        }
    }

    private fun toggleActions(id: CommentId) {
        if (actionExpandedComments.contains(id)) {
            actionExpandedComments.remove(id)
        } else {
            actionExpandedComments.add(id)
        }

        refreshItems(refreshHeader = false)
    }

    fun setQuery(query: String?, cb: (List<QueryResult>) -> Unit) {
        val realQuery = if (query.isNullOrBlank()) {
            null
        } else {
            query
        }

        if (realQuery == this.query) {
            return
        }

        this.query = realQuery

        val occurrences = mutableListOf<QueryResult>()

        fun count(s: String?, query: String, cb: (count: Int) -> Unit) {
            s ?: return

            var count = 0
            val queryLength = query.length
            var lastIndex = 0

            while (true) {
                val nextIndex = s.indexOf(query, lastIndex, ignoreCase = true)

                if (nextIndex < 0) break

                cb(count)

                count += 1

                lastIndex = nextIndex + queryLength
            }
        }

        refreshItems(refreshHeader = true) {
            val finalQuery = this.query ?: return@refreshItems

            items.withIndex().forEach { (index, item) ->
                when (item) {
                    is Item.VisibleCommentItem -> {
                        count(item.comment.comment.content, finalQuery) {
                            occurrences.add(
                                QueryResult(
                                    targetId = item.commentId,
                                    targetSubtype = 0,
                                    relativeMatchIndex = it,
                                    itemIndex = index,
                                    matchIndex = occurrences.size,
                                ),
                            )
                        }
                    }
                    is Item.FilteredCommentItem -> {
                        count(item.comment.comment.content, finalQuery) {
                            occurrences.add(
                                QueryResult(
                                    targetId = item.commentId,
                                    targetSubtype = 0,
                                    relativeMatchIndex = it,
                                    itemIndex = index,
                                    matchIndex = occurrences.size,
                                ),
                            )
                        }
                    }
                    FooterItem -> {}
                    is HeaderItem -> {
                        count(item.postView.post.name, finalQuery) {
                            occurrences.add(
                                QueryResult(
                                    targetId = item.postView.post.id,
                                    targetSubtype = 0,
                                    relativeMatchIndex = it,
                                    itemIndex = index,
                                    matchIndex = occurrences.size,
                                ),
                            )
                        }
                        count(item.postView.post.body, finalQuery) {
                            occurrences.add(
                                QueryResult(
                                    targetId = item.postView.post.id,
                                    targetSubtype = 1,
                                    relativeMatchIndex = it,
                                    itemIndex = index,
                                    matchIndex = occurrences.size,
                                ),
                            )
                        }
                    }
                    is Item.MissingCommentItem -> {}
                    is MoreCommentsItem -> {}
                    is PendingCommentItem -> {}
                    is ProgressOrErrorItem -> {}
                    is Item.ViewAllComments -> {}
                }
            }

            cb(occurrences)
        }
    }

    fun isSelectedForScreenshot(position: Int): Boolean {
        val item = items[position]
        return if (item is ScreenshotOptions) {
            includedInScreenshot.contains(item.id)
        } else {
            false
        }
    }

    fun isPost(position: Int): Boolean =
        items[position] is HeaderItem
}
