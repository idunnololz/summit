package com.idunnololz.summit.api.dto

import kotlinx.serialization.Serializable

@Serializable
data class ModAdd(
  val id: Int,
  val mod_person_id: PersonId,
  val other_person_id: PersonId,
  val removed: Boolean,
  val when_: String,
)
