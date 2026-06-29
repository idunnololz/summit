package com.idunnololz.summit.api.dto.lemmy

data class PrivateMessagesResponse(
  val private_messages: List<PrivateMessageView>,
  val prev_page: String? = null,
  val next_page: String? = null,
)
