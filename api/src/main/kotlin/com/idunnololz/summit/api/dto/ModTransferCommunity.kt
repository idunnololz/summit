package com.idunnololz.summit.api.dto

import kotlinx.serialization.Serializable

@Serializable
data class ModTransferCommunity(
  val id: Int,
  val mod_person_id: PersonId,
  val other_person_id: PersonId,
  val community_id: CommunityId,
  val when_: String,
)
