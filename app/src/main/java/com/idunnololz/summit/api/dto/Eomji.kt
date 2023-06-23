package com.idunnololz.summit.api.dto



data class CustomEmojiView(
    val custom_emoji: CustomEmoji,
    val keywords: List<CustomEmojiKeyword>,
)

data class CustomEmoji(
    val id: CustomEmojiId,
    val local_site_id: LocalSiteId,
    val shortcode: String,
    val image_url: String,
    val alt_text: String,
    val category: String,
    val published: String,
    val updated: String? = null,
)
data class CustomEmojiKeyword(
    val id: Int,
    val custom_emoji_id: CustomEmojiId,
    val keyword: String,
)

typealias CustomEmojiId = Int