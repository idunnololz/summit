package com.idunnololz.summit.lemmy.utils

import android.content.Context
import android.graphics.RectF
import android.graphics.Typeface
import android.text.Spannable
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.text.buildSpannedString
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.RecyclerView.Adapter
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.idunnololz.summit.R
import com.idunnololz.summit.api.dto.CommentId
import com.idunnololz.summit.api.dto.CommentView
import com.idunnololz.summit.api.utils.instance
import com.idunnololz.summit.databinding.AutoLoadItemBinding
import com.idunnololz.summit.databinding.CommentListCommentItemBinding
import com.idunnololz.summit.databinding.CommentListEndItemBinding
import com.idunnololz.summit.databinding.LoadingViewItemBinding
import com.idunnololz.summit.databinding.PostCommentFilteredItemBinding
import com.idunnololz.summit.lemmy.CommentPage
import com.idunnololz.summit.lemmy.CommentRef
import com.idunnololz.summit.lemmy.CommunityRef
import com.idunnololz.summit.lemmy.FilteredCommentItem
import com.idunnololz.summit.lemmy.LemmyTextHelper
import com.idunnololz.summit.lemmy.LinkResolver
import com.idunnololz.summit.lemmy.PageRef
import com.idunnololz.summit.lemmy.VisibleCommentItem
import com.idunnololz.summit.lemmy.appendSeparator
import com.idunnololz.summit.lemmy.postAndCommentView.GeneralQuickActionsViewHolder
import com.idunnololz.summit.lemmy.postAndCommentView.PostAndCommentViewBuilder
import com.idunnololz.summit.links.LinkContext
import com.idunnololz.summit.preview.VideoType
import com.idunnololz.summit.util.CustomLinkMovementMethod
import com.idunnololz.summit.util.DefaultLinkLongClickListener
import com.idunnololz.summit.util.LinkUtils
import com.idunnololz.summit.util.ext.appendLink
import com.idunnololz.summit.util.recyclerView.AdapterHelper
import com.idunnololz.summit.video.VideoState

class CommentListAdapter(
    private val context: Context,
    private val postAndCommentViewBuilder: PostAndCommentViewBuilder,
    private val onLoadPage: (Int) -> Unit,
    private val onImageClick: (View?, String) -> Unit,
    private val onVideoClick: (String, VideoType, VideoState?) -> Unit,
    private val onPageClick: (PageRef) -> Unit,
    private val onCommentClick: (CommentRef) -> Unit,
    private val onCommentActionClick: (CommentView, actionId: Int) -> Unit,
    private val onSignInRequired: () -> Unit,
    private val onInstanceMismatch: (String, String) -> Unit,
    private val onLinkClick: (url: String, text: String, linkContext: LinkContext) -> Unit,
    private val onLinkLongClick: (url: String, text: String) -> Unit,
) : Adapter<ViewHolder>() {

    sealed interface Item {
        data class VisibleCommentItem(
            val commentView: CommentView,
            val instance: String,
            val pageIndex: Int,
            val highlight: Boolean,
            val highlightForever: Boolean,
        ) : Item
        data class FilteredCommentItem(
            val commentView: CommentView,
            val instance: String,
            val pageIndex: Int,
            val highlight: Boolean,
            val highlightForever: Boolean,
        ) : Item

        data class AutoLoadItem(val pageToLoad: Int) : Item

        data class ErrorItem(val error: Throwable, val pageToLoad: Int) : Item

        data object EndItem : Item
    }

    private val regularColor: Int = ContextCompat.getColor(context, R.color.colorText)

    var viewLifecycleOwner: LifecycleOwner? = null
    val items: List<Item>
        get() = adapterHelper.items

    private var commentPages: List<CommentPage> = listOf()

    private var commentToHighlightForever: CommentRef? = null
    private var commentToHighlight: CommentRef? = null

    private val adapterHelper = AdapterHelper<Item>(
        areItemsTheSame = { old, new ->
            old::class == new::class && when (old) {
                is Item.AutoLoadItem ->
                    old.pageToLoad == (new as Item.AutoLoadItem).pageToLoad
                is Item.VisibleCommentItem ->
                    old.commentView.comment.id == (new as Item.VisibleCommentItem).commentView.comment.id
                is Item.FilteredCommentItem ->
                    old.commentView.comment.id == (new as Item.FilteredCommentItem).commentView.comment.id
                Item.EndItem -> true
                is Item.ErrorItem ->
                    old.pageToLoad == (new as Item.ErrorItem).pageToLoad
            }
        },
    ).apply {
        addItemType(Item.AutoLoadItem::class, AutoLoadItemBinding::inflate) { _, b, _ ->
            b.loadingView.showProgressBar()
        }
        addItemType(
            Item.VisibleCommentItem::class,
            CommentListCommentItemBinding::inflate,
        ) { item, b, _ ->
            val post = item.commentView.post
            val viewHolder = b.root.getTag(R.id.view_holder) as? GeneralQuickActionsViewHolder
                ?: run {
                    val vh = GeneralQuickActionsViewHolder(
                        root = b.root,
                        quickActionsTopBarrier = b.text,
                    )
                    b.root.setTag(R.id.view_holder, vh)
                    vh
                }

            postAndCommentViewBuilder.ensureCommentsActionButtons(
                viewHolder,
                viewHolder.root,
            )

            b.postInfo.text = buildSpannedString {
                appendLink(
                    item.commentView.community.name,
                    LinkUtils.getLinkForCommunity(
                        CommunityRef.CommunityRefByName(
                            item.commentView.community.name,
                            item.commentView.community.instance,
                        ),
                    ),
                )
                appendSeparator()

                val s = length
                appendLink(
                    post.name,
                    LinkUtils.getLinkForPost(item.instance, post.id),
                    underline = false,
                )
                val e = length
                setSpan(
                    ForegroundColorSpan(regularColor),
                    s,
                    e,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE,
                )
                setSpan(
                    StyleSpan(Typeface.BOLD),
                    s,
                    e,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE,
                )
            }
            b.postInfo.movementMethod = CustomLinkMovementMethod().apply {
                onLinkLongClickListener = DefaultLinkLongClickListener(context, onLinkLongClick)
                onLinkClickListener = object : CustomLinkMovementMethod.OnLinkClickListener {
                    override fun onClick(
                        textView: TextView,
                        url: String,
                        text: String,
                        rect: RectF,
                    ): Boolean {
                        val pageRef = LinkResolver.parseUrl(url, item.instance)

                        return if (pageRef != null) {
                            onPageClick(pageRef)
                            true
                        } else {
                            false
                        }
                    }
                }
            }
            postAndCommentViewBuilder
                .populateHeaderSpan(
                    headerContainer = b.headerContainer,
                    commentView = item.commentView,
                    instance = item.instance,
                    onPageClick = onPageClick,
                    onLinkClick = onLinkClick,
                    onLinkLongClick = onLinkLongClick,
                )
            LemmyTextHelper.bindText(
                textView = b.text,
                text = item.commentView.comment.content,
                instance = item.instance,
                onImageClick = {
                    onImageClick(null, it)
                },
                onVideoClick = {
                    onVideoClick(it, VideoType.Unknown, null)
                },
                onPageClick = onPageClick,
                onLinkClick = onLinkClick,
                onLinkLongClick = onLinkLongClick,
            )

            val scoreCount: TextView? = viewHolder.qaScoreCount
            val upvoteCount: TextView?
            val downvoteCount: TextView?

            if (scoreCount != null) {
                if (viewHolder.qaDownvoteCount != null) {
                    upvoteCount = viewHolder.qaUpvoteCount
                    downvoteCount = viewHolder.qaDownvoteCount
                } else {
                    upvoteCount = null
                    downvoteCount = null
                }

                postAndCommentViewBuilder.voteUiHandler.bind(
                    lifecycleOwner = requireNotNull(viewLifecycleOwner),
                    instance = item.instance,
                    commentView = item.commentView,
                    upVoteView = viewHolder.upvoteButton,
                    downVoteView = viewHolder.downvoteButton,
                    scoreView = scoreCount,
                    upvoteCount = upvoteCount,
                    downvoteCount = downvoteCount,
                    onUpdate = null,
                    onSignInRequired = onSignInRequired,
                    onInstanceMismatch = onInstanceMismatch,
                )
            }

            viewHolder.actionButtons.forEach {
                it.setOnClickListener {
                    onCommentActionClick(item.commentView, it.id)
                }
                if (it.id == R.id.ca_reply) {
                    it.isEnabled = !item.commentView.post.locked
                }
            }

            if (item.highlightForever) {
                b.highlightBg.visibility = View.VISIBLE
                b.highlightBg.alpha = 1f
            } else if (item.highlight) {
                b.highlightBg.visibility = View.VISIBLE
                b.highlightBg.animate()
                    .alpha(0f)
                    .apply {
                        duration = 350
                    }
                    .withEndAction {
                        b.highlightBg.visibility = View.GONE

                        onHighlightComplete()
                    }
            } else {
                b.highlightBg.visibility = View.GONE
            }

            b.root.setOnClickListener {
                onCommentClick(CommentRef(item.instance, item.commentView.comment.id))
            }
            b.root.setOnLongClickListener {
                onCommentActionClick(item.commentView, R.id.ca_more)
                true
            }
        }
        addItemType(
            Item.FilteredCommentItem::class,
            PostCommentFilteredItemBinding::inflate,
        ) { item, b, _ ->
            b.root.setOnClickListener {
                showFilteredMessage(item.commentView.comment.id)
            }
        }
        addItemType(Item.EndItem::class, CommentListEndItemBinding::inflate) { _, _, _ -> }
        addItemType(Item.ErrorItem::class, LoadingViewItemBinding::inflate) { item, b, _ ->
            b.loadingView.showDefaultErrorMessageFor(item.error)
            b.loadingView.setOnRefreshClickListener {
                onLoadPage(item.pageToLoad)
            }
        }
    }

    private fun showFilteredMessage(id: CommentId) {
        var commentItem: FilteredCommentItem? = null

        out@for (commentPage in commentPages) {
            for (comment in commentPage.comments) {
                if (comment is FilteredCommentItem && comment.commentView.comment.id == id) {
                    commentItem = comment
                    break@out
                }
            }
        }

        if (commentItem == null) {
            return
        }

        commentItem.show = true

        refreshItems()
    }

    override fun getItemViewType(position: Int): Int = adapterHelper.getItemViewType(position)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
        adapterHelper.onCreateViewHolder(parent, viewType)

    override fun getItemCount(): Int = adapterHelper.itemCount

    override fun onBindViewHolder(holder: ViewHolder, position: Int) =
        adapterHelper.onBindViewHolder(holder, position)

    fun setData(commentPages: List<CommentPage>) {
        this.commentPages = commentPages

        refreshItems()
    }

    private fun refreshItems() {
        val commentPages = commentPages

        if (commentPages.isEmpty()) return

        val newItems = mutableListOf<Item>()

        for (page in commentPages) {
            for (comment in page.comments) {
                newItems.add(
                    when (comment) {
                        is FilteredCommentItem ->
                            Item.FilteredCommentItem(
                                commentView = comment.commentView,
                                instance = page.instance,
                                pageIndex = page.pageIndex,
                                highlight = commentToHighlight?.id == comment.commentView.comment.id,
                                highlightForever = commentToHighlightForever?.id == comment.commentView.comment.id,
                            )
                        is VisibleCommentItem ->
                            Item.VisibleCommentItem(
                                commentView = comment.commentView,
                                instance = page.instance,
                                pageIndex = page.pageIndex,
                                highlight = commentToHighlight?.id == comment.commentView.comment.id,
                                highlightForever = commentToHighlightForever?.id == comment.commentView.comment.id,
                            )
                    },
                )
            }
        }

        val lastPage = commentPages.last()
        if (lastPage.error != null) {
            newItems.add(Item.ErrorItem(lastPage.error, lastPage.pageIndex + 1))
        } else if (lastPage.hasMore) {
            newItems.add(Item.AutoLoadItem(lastPage.pageIndex + 1))
        } else {
            newItems.add(Item.EndItem)
        }

        adapterHelper.setItems(newItems, this)
    }

    fun highlight(commentToHighlight: CommentRef) {
        this.commentToHighlight = commentToHighlight
        this.commentToHighlightForever = null

        items.indexOfFirst {
            when (it) {
                is Item.AutoLoadItem -> false
                is Item.VisibleCommentItem -> commentToHighlight.id == it.commentView.comment.id
                is Item.FilteredCommentItem -> commentToHighlight.id == it.commentView.comment.id
                Item.EndItem -> false
                is Item.ErrorItem -> false
            }
        }

        refreshItems()
    }

    fun highlightForever(commentToHighlight: CommentRef) {
        this.commentToHighlight = null
        this.commentToHighlightForever = commentToHighlight

        items.indexOfFirst {
            when (it) {
                is Item.AutoLoadItem -> false
                is Item.VisibleCommentItem -> commentToHighlight.id == it.commentView.comment.id
                is Item.FilteredCommentItem -> commentToHighlight.id == it.commentView.comment.id
                Item.EndItem -> false
                is Item.ErrorItem -> false
            }
        }

        refreshItems()
    }

    fun endHighlightForever() {
        val commentToHighlight = commentToHighlightForever

        if (commentToHighlight != null) {
            highlight(commentToHighlight)
        }

        refreshItems()
    }

    fun onHighlightComplete() {
        if (commentToHighlight == null && commentToHighlightForever == null) {
            return
        }

        this.commentToHighlight = null
        this.commentToHighlightForever = null

        refreshItems()
    }
}
