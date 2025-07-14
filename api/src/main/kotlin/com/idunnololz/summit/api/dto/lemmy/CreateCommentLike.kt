package com.idunnololz.summit.api.dto.lemmy

data class CreateCommentLike(
  val comment_id: CommentId,
  val score: Int,
  val auth: String,
)
