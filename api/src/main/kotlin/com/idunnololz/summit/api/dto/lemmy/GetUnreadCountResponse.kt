package com.idunnololz.summit.api.dto.lemmy

data class GetUnreadCountResponse(
  val replies: Int,
  val mentions: Int,
  val private_messages: Int,
)
