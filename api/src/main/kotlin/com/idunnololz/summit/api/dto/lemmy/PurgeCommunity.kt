package com.idunnololz.summit.api.dto.lemmy

data class PurgeCommunity(
  val community_id: CommunityId,
  val reason: String? = null,
  val auth: String,
)
