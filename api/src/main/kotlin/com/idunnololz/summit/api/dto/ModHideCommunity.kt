package com.idunnololz.summit.api.dto

import kotlinx.serialization.Serializable

@Serializable
data class ModHideCommunity(
  val id: Int,
  val community_id: CommunityId,
  val mod_person_id: PersonId,
  val when_: String,
  val reason: String? = null,
  val hidden: Boolean,
)
