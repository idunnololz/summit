package com.idunnololz.summit.api.dto.lemmy

data class ListCommunitiesResponse(
  val communities: List<CommunityView>,
  val next_page: String? = null,
  val prev_page: String? = null,
)
