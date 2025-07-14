package com.idunnololz.summit.api.dto.lemmy

import kotlinx.serialization.Serializable

@Serializable
data class AdminPurgeCommunityView(
  val admin_purge_community: AdminPurgeCommunity,
  val admin: Person? = null,
)
