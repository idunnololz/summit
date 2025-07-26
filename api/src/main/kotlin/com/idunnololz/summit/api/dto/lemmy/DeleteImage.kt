package com.idunnololz.summit.api.dto.lemmy

data class DeleteImage(
  val delete_token: String,
  val filename: String,
)