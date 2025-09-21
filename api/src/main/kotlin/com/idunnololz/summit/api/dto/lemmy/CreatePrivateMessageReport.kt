package com.idunnololz.summit.api.dto.lemmy

data class CreatePrivateMessageReport(
  val private_message_id: PrivateMessageId,
  val reason: String,
)
