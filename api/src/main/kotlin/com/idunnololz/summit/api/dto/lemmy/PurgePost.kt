package com.idunnololz.summit.api.dto.lemmy

data class PurgePost(
  val post_id: PostId,
  val reason: String? = null,
)
