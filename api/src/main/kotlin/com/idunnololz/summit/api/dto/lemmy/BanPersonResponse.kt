package com.idunnololz.summit.api.dto.lemmy

data class BanPersonResponse(
  val person_view: PersonView,
  val banned: Boolean,
)
