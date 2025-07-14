package com.idunnololz.summit.api.dto.lemmy

data class GetPostsResponse(
  val posts: List<PostView>,
  val next_page: String?,
)
