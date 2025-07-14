package com.idunnololz.summit.api.dto.lemmy

data class DeletePrivateMessage(
  val private_message_id: PrivateMessageId,
  val deleted: Boolean,
  val auth: String,
)
