package com.idunnololz.summit.api.dto

data class BanFromCommunity(
  val community_id: CommunityId,
  val person_id: PersonId,
  val ban: Boolean,
  val remove_data: Boolean? = null,
  val reason: String? = null,
  val expires: Long? = null,
  val auth: String,
)
