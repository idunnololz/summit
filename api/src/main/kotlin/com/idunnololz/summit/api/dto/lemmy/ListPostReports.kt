package com.idunnololz.summit.api.dto.lemmy

data class ListPostReports(
  val page: Int? = null,
  val limit: Int? = null,
  val unresolved_only: Boolean? = null,
  val community_id: CommunityId? = null,
  val auth: String,
)
