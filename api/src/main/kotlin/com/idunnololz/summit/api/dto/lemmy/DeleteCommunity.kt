package com.idunnololz.summit.api.dto.lemmy

data class DeleteCommunity(
  val community_id: CommunityId,
  val deleted: Boolean,
  val auth: String,
)
