package com.idunnololz.summit.api.dto.lemmy

data class CustomEmojiView(
  val custom_emoji: CustomEmoji,
  val keywords: List<CustomEmojiKeyword>,
)
