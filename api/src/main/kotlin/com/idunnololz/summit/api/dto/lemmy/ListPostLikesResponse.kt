package com.idunnololz.summit.api.dto.lemmy

data class ListPostLikesResponse(
  val post_likes: List<VoteView>,
  val next_page: String?,
  val prev_page: String?,
)
