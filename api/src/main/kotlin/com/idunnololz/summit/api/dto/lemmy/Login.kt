package com.idunnololz.summit.api.dto.lemmy

data class Login(
  val username_or_email: String,
  val password: String,
  val totp_2fa_token: String? = null,
)
