package com.idunnololz.summit.api.dto.lemmy

data class FollowCommunity(
  val community_id: CommunityId,
  val follow: Boolean,
  val auth: String,
)
