package com.idunnololz.summit.api.dto.lemmy

data class DeleteComment(
  val comment_id: CommentId,
  val deleted: Boolean,
  val auth: String,
)
