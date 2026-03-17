package com.idunnololz.summit.models

data class GetPostsResponse(
  val posts: List<PostView>,
  val nextPage: String?,
)
