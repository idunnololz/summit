package com.idunnololz.summit.api.dto.lemmy

data class MarkCommentReplyAsRead(
  val comment_reply_id: CommentReplyId,
  val read: Boolean,
  val auth: String,
)
