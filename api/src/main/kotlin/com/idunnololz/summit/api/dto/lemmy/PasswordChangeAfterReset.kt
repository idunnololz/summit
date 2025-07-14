package com.idunnololz.summit.api.dto.lemmy

data class PasswordChangeAfterReset(
  val token: String,
  val password: String,
  val password_verify: String,
)
