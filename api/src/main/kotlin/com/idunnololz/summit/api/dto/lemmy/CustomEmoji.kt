package com.idunnololz.summit.api.dto.lemmy

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
