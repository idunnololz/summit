package com.idunnololz.summit.api.local

class GetModlogResponse(
  val modEvents: List<ModEvent>,
  val nextPage: String?,
  val prevPage: String?,
)