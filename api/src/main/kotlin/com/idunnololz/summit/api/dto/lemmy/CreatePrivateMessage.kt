package com.idunnololz.summit.api.dto.lemmy

data class CreatePrivateMessage(
  val content: String,
  val recipient_id: PersonId,
)
