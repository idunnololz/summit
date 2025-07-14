package com.idunnololz.summit.api.dto.lemmy

data class EditPrivateMessage(
  val private_message_id: PrivateMessageId,
  val content: String,
  val auth: String,
)
