package com.idunnololz.summit.api.dto.lemmy

data class PrivateMessageView(
  val private_message: PrivateMessage,
  val creator: Person,
  val recipient: Person,
)
