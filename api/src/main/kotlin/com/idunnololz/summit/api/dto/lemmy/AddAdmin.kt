package com.idunnololz.summit.api.dto.lemmy

data class AddAdmin(
  val person_id: PersonId,
  val added: Boolean,
  val auth: String,
)
