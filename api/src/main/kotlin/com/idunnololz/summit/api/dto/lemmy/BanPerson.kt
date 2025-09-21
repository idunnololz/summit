package com.idunnololz.summit.api.dto.lemmy

data class BanPerson(
  val person_id: PersonId,
  val ban: Boolean,
  val remove_data: Boolean? = null,
  val reason: String? = null,
  val expires: Long? = null,
)
