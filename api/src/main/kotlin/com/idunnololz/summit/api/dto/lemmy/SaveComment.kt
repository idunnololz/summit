package com.idunnololz.summit.api.dto.lemmy

data class SaveComment(
  val comment_id: CommentId,
  val save: Boolean,
  val auth: String,
)
