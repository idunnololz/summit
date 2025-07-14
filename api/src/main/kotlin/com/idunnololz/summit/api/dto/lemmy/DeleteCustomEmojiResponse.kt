package com.idunnololz.summit.api.dto.lemmy

data class DeleteCustomEmojiResponse(
  val id: CustomEmojiId,
  val success: Boolean,
)
