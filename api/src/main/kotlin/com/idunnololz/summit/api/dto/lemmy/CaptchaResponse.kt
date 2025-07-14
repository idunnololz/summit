package com.idunnololz.summit.api.dto.lemmy

data class CaptchaResponse(
  val png: String,
  val wav: String,
  val uuid: String,
)
