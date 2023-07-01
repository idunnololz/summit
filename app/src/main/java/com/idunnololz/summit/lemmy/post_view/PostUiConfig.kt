package com.idunnololz.summit.lemmy.post_view

import com.idunnololz.summit.lemmy.community.CommunityLayout
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class PostUiConfig(
    val imageWidthPercent: Float,
    val textSizeMultiplier: Float = 1f,
    val headerTextSizeSp: Float = 14f,
    val titleTextSizeSp: Float = 14f,
    val footerTextSizeSp: Float = 14f,

    // We need this because some layouts show the full content or can show it
    val fullContentConfig: FullContentConfig = FullContentConfig(),
    val preferImagesAtEnd: Boolean = false,
) {
    fun updateTextSizeMultiplier(it: Float): PostUiConfig =
        this.copy(
            textSizeMultiplier = it,
            fullContentConfig = this.fullContentConfig.copy(textSizeMultiplier = it)
        )
}

@JsonClass(generateAdapter = true)
data class FullContentConfig(
    val titleTextSizeSp: Float = 18f,
    val bodyTextSizeSp: Float = 14f,
    val textSizeMultiplier: Float = 1f,
)

fun CommunityLayout.getDefaultPostUiConfig(): PostUiConfig =
    when (this) {
        CommunityLayout.Compact ->
            PostUiConfig(
                imageWidthPercent = 0.2f,
                headerTextSizeSp = 12f,
                footerTextSizeSp = 12f
            )
        CommunityLayout.List ->
            PostUiConfig(
                imageWidthPercent = 0.2f
            )
        CommunityLayout.Card ->
            PostUiConfig(
                imageWidthPercent = 1f
            )
        CommunityLayout.Full ->
            PostUiConfig(
                imageWidthPercent = 0.2f
            )
    }