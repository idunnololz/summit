package com.idunnololz.summit.api.dto.lemmy

data class SearchResponse(
  val type_: SearchType /* "All" | "Comments" | "Posts" | "Communities" | "Users" | "Url" */,
  val comments: List<CommentView>,
  val posts: List<PostView>,
  val communities: List<CommunityView>,
  val users: List<PersonView>,
)
