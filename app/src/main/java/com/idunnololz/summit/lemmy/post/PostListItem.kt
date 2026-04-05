package com.idunnololz.summit.lemmy.post

import com.idunnololz.summit.actions.PendingCommentView
import com.idunnololz.summit.api.dto.lemmy.CommentId
import com.idunnololz.summit.api.dto.lemmy.CommentView
import com.idunnololz.summit.lemmy.CommentHeaderInfo
import com.idunnololz.summit.lemmy.PostHeaderInfo
import com.idunnololz.summit.models.PostView

interface PostListItem {

  val id: Long

  companion object {
    private const val POST_FLAG = 0x100000000L
    private const val COMMENT_FLAG = 0x200000000L
    private const val PENDING_COMMENT_FLAG = 0x300000000L
    private const val MORE_COMMENTS_FLAG = 0x400000000L
    private const val POST_ERROR = 0x500000000L
  }

  sealed interface PostListView : PostListItem

  data class PostLoadedListView(
    val post: PostView,
    override val id: Long = post.post.id.toLong() or POST_FLAG,
    val postHeaderInfo: PostHeaderInfo,
  ) : PostListView

  data class PostErrorListView(
    val error: Throwable,
    override val id: Long = POST_ERROR,
  ) : PostListView

  sealed interface CommentListView : PostListView {
    val commentView: CommentView
    val pendingCommentView: PendingCommentView?
    var isRemoved: Boolean
    val commentHeaderInfo: CommentHeaderInfo
  }

  data class VisibleCommentListView(
    override val commentView: CommentView,
    override val pendingCommentView: PendingCommentView? = null,
    override var isRemoved: Boolean = false,
    override val id: Long = commentView.comment.id.toLong() or COMMENT_FLAG,
    override val commentHeaderInfo: CommentHeaderInfo,
  ) : CommentListView

  data class FilteredCommentItem(
    override val commentView: CommentView,
    override val pendingCommentView: PendingCommentView? = null,
    override var isRemoved: Boolean = false,
    override val id: Long = commentView.comment.id.toLong() or COMMENT_FLAG,
    override val commentHeaderInfo: CommentHeaderInfo,
    var show: Boolean = false,
  ) : CommentListView

  data class PendingCommentListView(
    val pendingCommentView: PendingCommentView,
    var author: String?,
    override val id: Long = pendingCommentView.id or PENDING_COMMENT_FLAG,
  ) : PostListItem

  data class MoreCommentsItem(
    val parentCommentId: CommentId?,
    val depth: Int,
    val moreCount: Int,
    override val id: Long = (parentCommentId?.toLong() ?: 0L) or MORE_COMMENTS_FLAG,
  ) : PostListItem

  data class MissingCommentItem(
    val commentId: CommentId,
    val parentCommentId: CommentId?,
    override val id: Long = commentId.toLong() or COMMENT_FLAG,
  ) : PostListItem
}

val PostListItem.asLoaded: PostListItem.PostLoadedListView?
  get() = this as? PostListItem.PostLoadedListView

val PostListItem.asError: PostListItem.PostErrorListView?
  get() = this as? PostListItem.PostErrorListView
