package com.idunnololz.summit.api.dto.lemmy

data class ListPrivateMessageReports(
  val page: Int? = null,
  val limit: Int? = null,
  val unresolved_only: Boolean? = null,
  val auth: String,
)
