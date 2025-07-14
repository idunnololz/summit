package com.idunnololz.summit.api.dto.lemmy

data class EditPost(
  val post_id: PostId,
  val name: String? = null,
  val url: String? = null,
  val body: String? = null,
  val nsfw: Boolean? = null,
  val language_id: LanguageId? = null,
  val auth: String,
  val alt_text: String? = null,
  val custom_thumbnail: String? = null,
)
