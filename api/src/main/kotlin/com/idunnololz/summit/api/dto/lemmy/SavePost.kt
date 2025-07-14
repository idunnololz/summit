package com.idunnololz.summit.api.dto.lemmy

data class SavePost(
  val post_id: PostId,
  val save: Boolean,
  val auth: String,
)
