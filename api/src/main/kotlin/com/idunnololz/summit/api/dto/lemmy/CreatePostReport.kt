package com.idunnololz.summit.api.dto.lemmy

data class CreatePostReport(
  val post_id: PostId,
  val reason: String,
  val auth: String,
)
