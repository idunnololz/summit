package com.idunnololz.summit.api.dto.lemmy

data class LockPost(
  val post_id: PostId,
  val locked: Boolean,
  val auth: String,
)
