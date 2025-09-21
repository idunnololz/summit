package com.idunnololz.summit.api.dto.lemmy

data class BlockCommunity(
  val community_id: CommunityId,
  val block: Boolean,
)
