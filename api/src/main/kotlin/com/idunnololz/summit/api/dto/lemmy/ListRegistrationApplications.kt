package com.idunnololz.summit.api.dto.lemmy

data class ListRegistrationApplications(
  val unread_only: Boolean? = null,
  val page: Int? = null,
  val limit: Int? = null,
  val auth: String,
)
