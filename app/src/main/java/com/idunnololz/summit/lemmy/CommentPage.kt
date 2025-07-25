package com.idunnololz.summit.lemmy

import com.idunnololz.summit.api.dto.lemmy.CommentView

data class CommentPage(
  val comments: List<CommentItem>,
  val instance: String,
  val pageIndex: Int,
  val hasMore: Boolean,
  val error: Throwable?,
)

sealed interface CommentItem {
  val commentView: CommentView
}

data class VisibleCommentItem(
  override val commentView: CommentView,
  val commentHeaderInfo: CommentHeaderInfo,
) : CommentItem

data class FilteredCommentItem(
  override val commentView: CommentView,
  var show: Boolean = false,
) : CommentItem
