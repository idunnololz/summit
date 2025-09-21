package com.idunnololz.summit.api.dto.lemmy

data class PurgePerson(
  val person_id: PersonId,
  val reason: String? = null,
)
