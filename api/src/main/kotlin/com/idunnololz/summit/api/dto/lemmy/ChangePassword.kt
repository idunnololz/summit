package com.idunnololz.summit.api.dto.lemmy

data class ChangePassword(
  val new_password: String,
  val new_password_verify: String,
  val old_password: String,
)
