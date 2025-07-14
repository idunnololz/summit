package com.idunnololz.summit.api.dto.lemmy

data class BanFromCommunityResponse(
  val person_view: PersonView,
  val banned: Boolean,
)
