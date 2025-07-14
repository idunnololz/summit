package com.idunnololz.summit.api.dto.lemmy

data class DeleteAccount(
  val password: String,
  val auth: String,
)
