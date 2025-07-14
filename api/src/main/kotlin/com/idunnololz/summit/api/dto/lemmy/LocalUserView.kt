package com.idunnololz.summit.api.dto.lemmy

data class LocalUserView(
  val local_user: LocalUser,
  val person: Person,
  val counts: PersonAggregates,
)
