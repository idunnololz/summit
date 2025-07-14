package com.idunnololz.summit.api.dto.lemmy

data class GetReportCount(
  val community_id: CommunityId? = null,
  val auth: String,
)
