package com.idunnololz.summit.api.dto.lemmy

data class InstanceBlockView(
  val person: Person,
  val instance: Instance,
  val site: Site? = null,
)
