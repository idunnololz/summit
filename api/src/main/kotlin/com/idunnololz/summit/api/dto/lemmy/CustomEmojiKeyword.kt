package com.idunnololz.summit.api.dto.lemmy

data class CustomEmojiKeyword(
  val id: Int,
  val custom_emoji_id: CustomEmojiId,
  val keyword: String,
)
