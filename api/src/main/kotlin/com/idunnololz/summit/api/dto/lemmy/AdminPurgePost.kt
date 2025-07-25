package com.idunnololz.summit.api.dto.lemmy

import kotlinx.serialization.Serializable

@Serializable
data class AdminPurgePost(
  val id: Int,
  val admin_person_id: PersonId,
  val community_id: CommunityId,
  val reason: String? = null,
  val when_: String,
)
