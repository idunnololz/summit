package com.idunnololz.summit.api.dto.lemmy

data class BlockPersonResponse(
  val person_view: PersonView,
  val blocked: Boolean,
)
