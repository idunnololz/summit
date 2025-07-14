package com.idunnololz.summit.api.dto.lemmy

data class CreatePostLike(
  val post_id: PostId,
  val score: Int,
  val auth: String,
)
