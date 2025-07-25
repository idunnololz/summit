package com.idunnololz.summit.api.dto.lemmy

data class RemoveCommunity(
  val community_id: CommunityId,
  val removed: Boolean,
  val reason: String? = null,
  val auth: String,
)
