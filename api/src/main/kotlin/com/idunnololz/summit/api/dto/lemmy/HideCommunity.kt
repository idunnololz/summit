package com.idunnololz.summit.api.dto.lemmy

data class HideCommunity(
  val community_id: CommunityId,
  val hidden: Boolean,
  val reason: String? = null,
)
