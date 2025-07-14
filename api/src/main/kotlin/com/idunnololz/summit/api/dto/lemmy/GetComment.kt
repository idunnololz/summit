package com.idunnololz.summit.api.dto.lemmy

data class GetComment(
  val id: CommentId,
  val auth: String? = null,
)
