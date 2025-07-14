package com.idunnololz.summit.api.dto.lemmy

import kotlinx.serialization.Serializable

@Serializable
data class ModHideCommunityView(
  val mod_hide_community: ModHideCommunity,
  val admin: Person? = null,
  val community: Community,
)
