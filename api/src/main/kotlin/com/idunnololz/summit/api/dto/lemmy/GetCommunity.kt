package com.idunnololz.summit.api.dto.lemmy

data class GetCommunity(
  val id: CommunityId? = null,
  val name: String? = null,
  val auth: String? = null,
)
