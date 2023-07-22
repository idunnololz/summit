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
import androidx.appcompat.widget.PopupMenu
import androidx.core.content.ContextCompat
import androidx.core.text.buildSpannedString
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.RecyclerView.Adapter
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import arrow.core.Either
import com.idunnololz.summit.R
import com.idunnololz.summit.api.dto.CommentView
import com.idunnololz.summit.api.dto.PersonId
import com.idunnololz.summit.api.dto.PostView
import com.idunnololz.summit.api.utils.instance
import com.idunnololz.summit.databinding.AutoLoadItemBinding
import com.idunnololz.summit.databinding.CommentListCommentItemBinding
import com.idunnololz.summit.databinding.CommentListEndItemBinding
import com.idunnololz.summit.databinding.LoadingViewItemBinding
import com.idunnololz.summit.lemmy.CommentPageResult
import com.idunnololz.summit.lemmy.CommentRef
import com.idunnololz.summit.lemmy.CommunityRef
import com.idunnololz.summit.lemmy.LemmyTextHelper
import com.idunnololz.summit.lemmy.LinkResolver
import com.idunnololz.summit.lemmy.PageRef
import com.idunnololz.summit.lemmy.appendSeparator
import com.idunnololz.summit.lemmy.postAndCommentView.PostAndCommentViewBuilder
import com.idunnololz.summit.util.CustomLinkMovementMethod
import com.idunnololz.summit.util.DefaultLinkLongClickListener
import com.idunnololz.summit.util.LinkUtils
import com.idunnololz.summit.util.ext.appendLink
import com.idunnololz.summit.util.recyclerView.AdapterHelper

class CommentListAdapter(
    private val context: Context,
    private val postAndCommentViewBuilder: PostAndCommentViewBuilder,
    private val onLoadPage: (Int) -> Unit,
    private val onImageClick: (View?, String) -> Unit,
    private val onPageClick: (PageRef) -> Unit,
    private val onAddCommentClick: (Either<PostView, CommentView>) -> Unit,
    private val onCommentMoreClick: (CommentView) -> Unit,
    private val onSignInRequired: () -> Unit,
    private val onInstanceMismatch: (String, String) -> Unit,
    private val onLinkLongClick: (url: String, text: String) -> Unit,
) : Adapter<ViewHolder>() {

    sealed interface Item {
        data class CommentItem(
            val commentView: CommentView,
            val instance: String,
            val pageIndex: Int,
        ) : Item

        data class AutoLoadItem(val pageToLoad: Int) : Item

        data class ErrorItem(val error: Throwable, val pageToLoad: Int) : Item

        object EndItem : Item
    }

    private val regularColor: Int = ContextCompat.getColor(context, R.color.colorText)

    var viewLifecycleOwner: LifecycleOwner? = null
    val items: List<Item>
        get() = adapterHelper.items

    private var commentPages: List<CommentPageResult> = listOf()

    private val adapterHelper = AdapterHelper<Item>(
        areItemsTheSame = { old, new ->
            old::class == new::class && when (old) {
                is Item.AutoLoadItem ->
                    old.pageToLoad == (new as Item.AutoLoadItem).pageToLoad
                is Item.CommentItem ->
                    old.commentView.comment.id == (new as Item.CommentItem).commentView.comment.id
                Item.EndItem -> true
                is Item.ErrorItem ->
                    old.pageToLoad == (new as Item.ErrorItem).pageToLoad
            }
        }
    ).apply {
        addItemType(Item.AutoLoadItem::class, AutoLoadItemBinding::inflate) { _, b, _ ->
            b.loadingView.showProgressBar()
        }
        addItemType(Item.CommentItem::class, CommentListCommentItemBinding::inflate) { item, b, _ ->
            val post = item.commentView.post
            b.postInfo.text = buildSpannedString {
                appendLink(
                    item.commentView.community.name,
                    LinkUtils.getLinkForCommunity(
                        CommunityRef.CommunityRefByName(
                            item.commentView.community.name,
                            item.commentView.community.instance))
                )
                appendSeparator()

                val s = length
                appendLink(
                    post.name,
                    LinkUtils.getLinkForPost(item.instance, post.id),
                    underline = false
                )
                val e = length
                setSpan(
                    ForegroundColorSpan(regularColor),
                    s,
                    e,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                )
                setSpan(
                    StyleSpan(Typeface.BOLD),
                    s,
                    e,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }
            b.postInfo.movementMethod = CustomLinkMovementMethod().apply {
                onLinkLongClickListener = DefaultLinkLongClickListener(context, onLinkLongClick)
                onLinkClickListener = object : CustomLinkMovementMethod.OnLinkClickListener {
                    override fun onClick(
                        textView: TextView,
                        url: String,
                        text: String,
                        rect: RectF
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
            postAndCommentViewBuilder.lemmyHeaderHelper
                .populateHeaderSpan(
                    headerContainer = b.headerContainer,
                    item = item.commentView,
                    instance = item.instance,
                    onPageClick = onPageClick,
                    onLinkLongClick = onLinkLongClick,
                )
            LemmyTextHelper.bindText(
                textView = b.text,
                text = item.commentView.comment.content,
                instance = item.instance,
                onImageClick = {
                    onImageClick(null, it)
                },
                onPageClick = onPageClick,
                onLinkLongClick = onLinkLongClick,
            )

            postAndCommentViewBuilder.voteUiHandler.bind(
                requireNotNull(viewLifecycleOwner),
                item.instance,
                item.commentView,
                b.upvoteButton,
                b.downvoteButton,
                b.upvoteCount,
                null,
                onSignInRequired,
                onInstanceMismatch,
            )

            b.commentButton.isEnabled = !post.locked
            b.commentButton.setOnClickListener {
                onAddCommentClick(Either.Right(item.commentView))
            }
            b.moreButton.setOnClickListener {
                onCommentMoreClick(item.commentView)
            }
            b.root.setOnClickListener {
                onPageClick(CommentRef(item.instance, item.commentView.comment.id))
            }
            b.root.setOnLongClickListener {
                onCommentMoreClick(item.commentView)
                true
            }
        }
        addItemType(Item.EndItem::class, CommentListEndItemBinding::inflate) { _, _, _ -> }
        addItemType(Item.ErrorItem::class, LoadingViewItemBinding::inflate) { item, b, h ->
            b.loadingView.showDefaultErrorMessageFor(item.error)
            b.loadingView.setOnRefreshClickListener {
                onLoadPage(item.pageToLoad)
            }
        }
    }

    override fun getItemViewType(position: Int): Int =
        adapterHelper.getItemViewType(position)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
        adapterHelper.onCreateViewHolder(parent, viewType)

    override fun getItemCount(): Int = adapterHelper.itemCount

    override fun onBindViewHolder(holder: ViewHolder, position: Int) =
        adapterHelper.onBindViewHolder(holder, position)

    fun setData(commentPages: List<CommentPageResult>) {
        this.commentPages = commentPages

        refreshItems()
    }

    private fun refreshItems() {
        val commentPages = commentPages

        if (commentPages.isEmpty()) return

        val newItems = mutableListOf<Item>()

        for (page in commentPages) {
            for (comment in page.comments) {
                newItems.add(Item.CommentItem(
                    comment,
                    page.instance,
                    page.pageIndex,
                ))
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
}