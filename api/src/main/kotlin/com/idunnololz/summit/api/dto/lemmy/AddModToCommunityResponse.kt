package com.idunnololz.summit.api.dto.lemmy

data class AddModToCommunityResponse(
  val moderators: List<CommunityModeratorView>,
)
