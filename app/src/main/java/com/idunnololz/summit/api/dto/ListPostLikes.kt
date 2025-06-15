package com.idunnololz.summit.api.dto

internal data class ListPostLikes(
  val post_id: PostId,
  val page: Long? = null,
  val limit: Long? = null,
)
