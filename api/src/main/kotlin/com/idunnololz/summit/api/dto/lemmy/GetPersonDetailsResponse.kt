package com.idunnololz.summit.api.dto.lemmy

data class GetPersonDetailsResponse(
  val person_view: PersonView,
  val comments: List<CommentView>,
  val posts: List<PostView>,
  val moderates: List<CommunityModeratorView>,
)
