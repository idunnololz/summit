package com.idunnololz.summit.api.dto.lemmy

data class RemoveComment(
  val comment_id: CommentId,
  val removed: Boolean,
  val reason: String? = null,
  val auth: String,
)
