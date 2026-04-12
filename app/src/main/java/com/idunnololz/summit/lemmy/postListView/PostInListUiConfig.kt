package com.idunnololz.summit.lemmy.postListView

import com.idunnololz.summit.lemmy.community.CommunityLayout
import com.idunnololz.summit.preferences.PostHeaderVersion
import kotlinx.serialization.Serializable

@Serializable
data class PostInListUiConfig(
  val imageWidthPercent: Float,
  val textSizeMultiplier: Float = 1f,
  val headerTextSizeSp: Float = 14f,
  val titleTextSizeSp: Float = 14f,
  val footerTextSizeSp: Float = 14f,
  val subtitleTextSizeSp: Float? = null,
  val horizontalMarginDp: Float? = null,
  val verticalMarginDp: Float? = null,

  // We need this because some layouts show the full content or can show it
  val fullContentConfig: FullContentConfig = FullContentConfig(),
  val preferImagesAtEnd: Boolean = false,
  val preferFullSizeImages: Boolean = true,
  val preferTitleText: Boolean = false,
  val contentMaxLines: Int = -1,
  val contentMaxHeightDp: Int = -1,
  val showCommunityIcon: Boolean = true,
//  val dimReadPosts: Boolean? = null,
  val readPostStyle: Int? = -1,
  val showTextPreviewIcon: Boolean? = null,
  // Only works with some layouts.
  val showUrlDomain: Boolean? = null,
  /**
   * This is a special setting for "smart" layouts. Eg. layouts that combine multiple other layouts.
   * For smart layouts, we don't want to use the [imageWidthPercent] fields for all layouts so we
   * have this special field to tell us not to do that.
   */
  val fullImageWidthWhenFullWidthLayout: Boolean? = null,

  val postHeaderVersion: PostHeaderVersion? = null,
) {
  fun updateTextSizeMultiplier(it: Float): PostInListUiConfig = this.copy(
    textSizeMultiplier = it,
    fullContentConfig = this.fullContentConfig.copy(textSizeMultiplier = it),
  )
  fun updateTitleTextSize(it: Float): PostInListUiConfig = this.copy(
    titleTextSizeSp = it,
    fullContentConfig = this.fullContentConfig.copy(titleTextSizeSp = it),
  )

  fun showUrlDomain() = showUrlDomain ?: false

  fun subtitleTextSizeSp() = subtitleTextSizeSp ?: 11f

  fun postHeaderVersion() = postHeaderVersion ?: PostHeaderVersion.V1
}

@Serializable
data class PostAndCommentsUiConfig(
  val postUiConfig: PostUiConfig = PostUiConfig(),
  val commentUiConfig: CommentUiConfig = CommentUiConfig(),
)

@Serializable
data class PostUiConfig(
  val textSizeMultiplier: Float = 1f,
  val headerTextSizeSp: Float = 14f,
  val titleTextSizeSp: Float = 20f,
  val footerTextSizeSp: Float = 14f,
  val fullContentConfig: FullContentConfig = FullContentConfig(),
) {
  fun updateTextSizeMultiplier(it: Float): PostUiConfig = this.copy(
    textSizeMultiplier = it,
    fullContentConfig = this.fullContentConfig.copy(textSizeMultiplier = it),
  )
}

@Serializable
data class CommentUiConfig(
  val textSizeMultiplier: Float = 1f,
  val headerTextSizeSp: Float = 14f,
  val footerTextSizeSp: Float = 14f,
  val contentTextSizeSp: Float = 14f,
  val indentationPerLevelDp: Int = 16,
) {
  fun updateTextSizeMultiplier(it: Float): CommentUiConfig = this.copy(
    textSizeMultiplier = it,
  )

  fun updateIndentationPerLevelDp(it: Float): CommentUiConfig = this.copy(
    indentationPerLevelDp = it.toInt(),
  )
}

@Serializable
data class FullContentConfig(
  val titleTextSizeSp: Float = 18f,
  val bodyTextSizeSp: Float = 14f,
  val textSizeMultiplier: Float = 1f,
)

fun getDefaultPostAndCommentsUiConfig() = PostAndCommentsUiConfig()

fun CommunityLayout.getDefaultPostUiConfig(): PostInListUiConfig = when (this) {
  CommunityLayout.Compact ->
    PostInListUiConfig(
      imageWidthPercent = 0.2f,
      headerTextSizeSp = 12f,
      footerTextSizeSp = 12f,
      readPostStyle = ReadPostStyleIds.DIM_TITLE,
      showCommunityIcon = false,
      postHeaderVersion = PostHeaderVersion.V2,
    )
  CommunityLayout.List ->
    PostInListUiConfig(
      imageWidthPercent = 0.2f,
      readPostStyle = ReadPostStyleIds.DIM_TITLE,
    )
  CommunityLayout.List2 ->
    PostInListUiConfig(
      imageWidthPercent = 0.25f,
      headerTextSizeSp = 14f,
      titleTextSizeSp = 16f,
      preferImagesAtEnd = true,
      readPostStyle = ReadPostStyleIds.DIM_TITLE,
      postHeaderVersion = PostHeaderVersion.V2,
    )
  CommunityLayout.ListWithCards ->
    PostInListUiConfig(
      imageWidthPercent = 0.2f,
      readPostStyle = ReadPostStyleIds.DIM_TITLE,
    )
  CommunityLayout.FullWithCards ->
    PostInListUiConfig(
      imageWidthPercent = 0.2f,
      readPostStyle = ReadPostStyleIds.DIM_CONTENT,
    )
  CommunityLayout.LargeList ->
    PostInListUiConfig(
      imageWidthPercent = 1f,
      readPostStyle = ReadPostStyleIds.DIM_CONTENT,
    )
  CommunityLayout.Card ->
    PostInListUiConfig(
      imageWidthPercent = 1f,
      readPostStyle = ReadPostStyleIds.DIM_CONTENT,
    )
  CommunityLayout.Card2 ->
    PostInListUiConfig(
      imageWidthPercent = 1f,
      readPostStyle = ReadPostStyleIds.DIM_CONTENT,
    )
  CommunityLayout.Card3 ->
    PostInListUiConfig(
      titleTextSizeSp = 16f,
      imageWidthPercent = 1f,
      readPostStyle = ReadPostStyleIds.DIM_CONTENT,
    )
  CommunityLayout.Full ->
    PostInListUiConfig(
      imageWidthPercent = 0.2f,
      readPostStyle = ReadPostStyleIds.DIM_CONTENT,
    )
  CommunityLayout.SmartList ->
    PostInListUiConfig(
      imageWidthPercent = 0.2f,
      readPostStyle = ReadPostStyleIds.DIM_TITLE,
      fullImageWidthWhenFullWidthLayout = true,
    )
}

val CommunityLayout.defaultReadPostStyle
  get() =
    when (this) {
      CommunityLayout.Compact,
      CommunityLayout.List,
      CommunityLayout.List2,
      CommunityLayout.ListWithCards,
      CommunityLayout.FullWithCards,
      CommunityLayout.SmartList,
      -> ReadPostStyleIds.DIM_TITLE
      CommunityLayout.LargeList,
      CommunityLayout.Card,
      CommunityLayout.Card2,
      CommunityLayout.Card3,
      CommunityLayout.Full,
      -> ReadPostStyleIds.DIM_CONTENT
    }
