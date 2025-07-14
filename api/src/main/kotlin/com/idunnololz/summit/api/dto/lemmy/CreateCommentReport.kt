package com.idunnololz.summit.api.dto.lemmy

data class CreateCommentReport(
  val comment_id: CommentId,
  val reason: String,
  val auth: String,
)
