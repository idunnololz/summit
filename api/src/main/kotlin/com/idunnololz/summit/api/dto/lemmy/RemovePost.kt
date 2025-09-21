package com.idunnololz.summit.api.dto.lemmy

data class RemovePost(
  val post_id: PostId,
  val removed: Boolean,
  val reason: String? = null,
)
