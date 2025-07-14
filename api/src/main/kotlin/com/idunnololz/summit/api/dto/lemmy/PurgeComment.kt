package com.idunnololz.summit.api.dto.lemmy

data class PurgeComment(
  val comment_id: CommentId,
  val reason: String? = null,
  val auth: String,
)
