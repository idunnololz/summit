package com.idunnololz.summit.emoji

data class InstanceEmoji(
  val id: Long,
  val shortcode: String,
  val imageUrl: String,
  val altText: String,
  val category: String,
  val publishedMs: Long,
  val updatedMs: Long? = null,
)

val InstanceEmoji.markdownCode: String
  get() {
    val altText = altText.takeIf { it.isNotBlank() } ?: shortcode
    return "![${altText}](${imageUrl} \"emoji ${shortcode}\")"
  }