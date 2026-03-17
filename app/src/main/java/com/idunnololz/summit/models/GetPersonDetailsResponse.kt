package com.idunnololz.summit.models

import com.idunnololz.summit.api.dto.lemmy.CommentView
import com.idunnololz.summit.api.dto.lemmy.CommunityModeratorView
import com.idunnololz.summit.api.dto.lemmy.PersonView

data class GetPersonDetailsResponse(
  val personView: PersonView,
  val comments: List<CommentView>,
  val posts: List<PostView>,
  val moderates: List<CommunityModeratorView>,
)