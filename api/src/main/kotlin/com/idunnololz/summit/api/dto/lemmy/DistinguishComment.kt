package com.idunnololz.summit.api.dto.lemmy

data class DistinguishComment(
  val comment_id: CommentId,
  val distinguished: Boolean,
  val auth: String,
)
