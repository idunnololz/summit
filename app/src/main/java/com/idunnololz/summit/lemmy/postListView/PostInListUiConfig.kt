package com.idunnololz.summit.lemmy.postListView

import com.idunnololz.summit.lemmy.community.CommunityLayout
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class PostInListUiConfig(
    val imageWidthPercent: Float,
    val textSizeMultiplier: Float = 1f,
    val headerTextSizeSp: Float = 14f,
    val titleTextSizeSp: Float = 14f,
    val footerTextSizeSp: Float = 14f,

    // We need this because some layouts show the full content or can show it
    val fullContentConfig: FullContentConfig = FullContentConfig(),
    val preferImagesAtEnd: Boolean = false,
    val preferFullSizeImages: Boolean = false,
    val preferTitleText: Boolean = false,
    val contentMaxLines: Int = -1,
) {
    fun updateTextSizeMultiplier(it: Float): PostInListUiConfig =
        this.copy(
            textSizeMultiplier = it,
            fullContentConfig = this.fullContentConfig.copy(textSizeMultiplier = it),
        )
}

@JsonClass(generateAdapter = true)
data class PostAndCommentsUiConfig(
    val postUiConfig: PostUiConfig = PostUiConfig(),
    val commentUiConfig: CommentUiConfig = CommentUiConfig(),
)

@JsonClass(generateAdapter = true)
data class PostUiConfig(
    val textSizeMultiplier: Float = 1f,
    val headerTextSizeSp: Float = 14f,
    val titleTextSizeSp: Float = 20f,
    val footerTextSizeSp: Float = 14f,
    val fullContentConfig: FullContentConfig = FullContentConfig(),
) {
    fun updateTextSizeMultiplier(it: Float): PostUiConfig =
        this.copy(
            textSizeMultiplier = it,
            fullContentConfig = this.fullContentConfig.copy(textSizeMultiplier = it),
        )
}

@JsonClass(generateAdapter = true)
data class CommentUiConfig(
    val textSizeMultiplier: Float = 1f,
    val headerTextSizeSp: Float = 14f,
    val footerTextSizeSp: Float = 14f,
    val contentTextSizeSp: Float = 14f,
    val indentationPerLevelDp: Int = 16,
) {
    fun updateTextSizeMultiplier(it: Float): CommentUiConfig =
        this.copy(
            textSizeMultiplier = it,
        )

    fun updateIndentationPerLevelDp(it: Float): CommentUiConfig =
        this.copy(
            indentationPerLevelDp = it.toInt(),
        )
}

@JsonClass(generateAdapter = true)
data class FullContentConfig(
    val titleTextSizeSp: Float = 18f,
    val bodyTextSizeSp: Float = 14f,
    val textSizeMultiplier: Float = 1f,
)

fun getDefaultPostAndCommentsUiConfig() =
    PostAndCommentsUiConfig()

fun CommunityLayout.getDefaultPostUiConfig(): PostInListUiConfig =
    when (this) {
        CommunityLayout.Compact ->
            PostInListUiConfig(
                imageWidthPercent = 0.2f,
                headerTextSizeSp = 12f,
                footerTextSizeSp = 12f,
            )
        CommunityLayout.List ->
            PostInListUiConfig(
                imageWidthPercent = 0.2f,
            )
        CommunityLayout.LargeList ->
            PostInListUiConfig(
                imageWidthPercent = 1f,
            )
        CommunityLayout.Card ->
            PostInListUiConfig(
                imageWidthPercent = 1f,
            )
        CommunityLayout.Card2 ->
            PostInListUiConfig(
                imageWidthPercent = 1f,
            )
        CommunityLayout.Card3 ->
            PostInListUiConfig(
                titleTextSizeSp = 16f,
                imageWidthPercent = 1f,
            )
        CommunityLayout.Full ->
            PostInListUiConfig(
                imageWidthPercent = 0.2f,
            )
    }
