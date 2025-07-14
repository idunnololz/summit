package com.idunnololz.summit.api.dto.lemmy

data class ListPostLikesResponse(
  val post_likes: List<VoteView>,
)
