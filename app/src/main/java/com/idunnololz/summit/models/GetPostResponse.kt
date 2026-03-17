package com.idunnololz.summit.models

import com.idunnololz.summit.api.dto.lemmy.CommunityModeratorView
import com.idunnololz.summit.api.dto.lemmy.CommunityView

data class GetPostResponse(
  val postView: PostView,
  val communityView: CommunityView,
  val moderators: List<CommunityModeratorView>,
  val crossPosts: List<PostView>,
  val online: Int,
)
