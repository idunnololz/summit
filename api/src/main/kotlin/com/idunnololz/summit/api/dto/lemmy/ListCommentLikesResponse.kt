package com.idunnololz.summit.api.dto.lemmy

data class ListCommentLikesResponse(
  val comment_likes: List<VoteView>,
)
