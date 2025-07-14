package com.idunnololz.summit.api.dto.lemmy

data class MarkPrivateMessageAsRead(
  val private_message_id: PrivateMessageId,
  val read: Boolean,
  val auth: String,
)
