package com.idunnololz.summit.api.dto.lemmy

data class BlockCommunityResponse(
  val community_view: CommunityView,
  val blocked: Boolean,
)
