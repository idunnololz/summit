package com.idunnololz.summit.api.dto.lemmy

data class DeleteCustomEmoji(
  val id: CustomEmojiId,
  val auth: String,
)
