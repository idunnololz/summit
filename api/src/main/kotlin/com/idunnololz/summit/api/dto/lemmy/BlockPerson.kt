package com.idunnololz.summit.api.dto.lemmy

data class BlockPerson(
  val person_id: PersonId,
  val block: Boolean,
)
