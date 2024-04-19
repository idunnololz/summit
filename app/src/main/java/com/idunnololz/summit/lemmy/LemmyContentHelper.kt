package com.idunnololz.summit.lemmy

import android.content.Context
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.text.TextUtils
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.view.ViewGroup.MarginLayoutParams
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.LayoutRes
import androidx.core.view.get
import androidx.core.view.updateLayoutParams
import coil.load
import com.commit451.coiltransformations.BlurTransformation
import com.google.android.material.card.MaterialCardView
import com.idunnololz.summit.R
import com.idunnololz.summit.api.dto.PostView
import com.idunnololz.summit.api.utils.PostType
import com.idunnololz.summit.api.utils.getPreviewInfo
import com.idunnololz.summit.api.utils.getThumbnailPreviewInfo
import com.idunnololz.summit.api.utils.getThumbnailUrl
import com.idunnololz.summit.api.utils.getType
import com.idunnololz.summit.api.utils.getVideoInfo
import com.idunnololz.summit.api.utils.shouldHideItem
import com.idunnololz.summit.lemmy.post.QueryMatchHelper.HighlightTextData
import com.idunnololz.summit.lemmy.postListView.FullContentConfig
import com.idunnololz.summit.lemmy.screenshotMode.ScreenshotModeViewModel
import com.idunnololz.summit.lemmy.screenshotMode.ScreenshotModeViewModel.PostViewType
import com.idunnololz.summit.links.LinkContext
import com.idunnololz.summit.links.LinkResolver
import com.idunnololz.summit.offline.OfflineManager
import com.idunnololz.summit.preview.VideoType
import com.idunnololz.summit.util.ContentUtils
import com.idunnololz.summit.util.ContentUtils.getVideoType
import com.idunnololz.summit.util.PreviewInfo
import com.idunnololz.summit.util.RecycledState
import com.idunnololz.summit.util.Size
import com.idunnololz.summit.util.ViewRecycler
import com.idunnololz.summit.util.assertMainThread
import com.idunnololz.summit.util.ext.getSize
import com.idunnololz.summit.util.ext.setup
import com.idunnololz.summit.video.ExoPlayerManager
import com.idunnololz.summit.video.VideoState
import com.idunnololz.summit.video.getVideoState
import com.idunnololz.summit.view.CustomPlayerView
import com.idunnololz.summit.view.LoadingView

class LemmyContentHelper(
    private val context: Context,
    private val offlineManager: OfflineManager,
    private val exoPlayerManager: ExoPlayerManager,
    private val viewRecycler: ViewRecycler<View> = ViewRecycler(),
) {

    companion object {
        private const val TAG = "LemmyContentHelper"
    }

    var config: FullContentConfig = FullContentConfig()
        set(value) {
            field = value

            textSizeMultiplier = config.textSizeMultiplier
        }

    private val inflater = LayoutInflater.from(context)

    private var textSizeMultiplier: Float = config.textSizeMultiplier
    var globalFontSizeMultiplier: Float = 1f
    var alwaysShowLinkBelowPost: Boolean = false

    /**
     * @param lazyUpdate If true, content will not be refreshed. Only non content related things will be setup (eg. removed post warning)
     */
    fun setupFullContent(
        reveal: Boolean,
        tempSize: Size,
        videoViewMaxHeight: Int,
        contentMaxWidth: Int,
        fullImageViewTransitionName: String,
        postView: PostView,
        instance: String,
        rootView: View,
        fullContentContainerView: ViewGroup,
        lazyUpdate: Boolean = false,
        videoState: VideoState? = null,
        contentMaxLines: Int = -1,
        highlight: HighlightTextData? = null,
        screenshotConfig: ScreenshotModeViewModel.ScreenshotConfig? = null,
        onFullImageViewClickListener: (imageView: View?, url: String) -> Unit,
        onImageClickListener: (url: String) -> Unit,
        onVideoClickListener: (url: String, videoType: VideoType, videoState: VideoState?) -> Unit,
        onVideoLongClickListener: (url: String) -> Unit,
        onItemClickListener: () -> Unit,
        onRevealContentClickedFn: () -> Unit,
        onLemmyUrlClick: (PageRef) -> Unit,
        onLinkClick: (url: String, text: String?, linkContext: LinkContext) -> Unit,
        onLinkLongClick: (url: String, text: String?) -> Unit,
    ) {
        assertMainThread()
        if (!lazyUpdate) {
            fullContentContainerView.removeAllViews()
        }

        val context = rootView.context
        // Handles crossposts
        val targetPostView = postView

        val postViewType = screenshotConfig?.postViewType
        val showText = postViewType != PostViewType.ImageOnly &&
            postViewType != PostViewType.TitleAndImageOnly
        val showImage = postViewType != PostViewType.TextOnly
        val showLink = postViewType == null ||
            screenshotConfig.postViewType == PostViewType.Full
        val onlyImage = postViewType == PostViewType.ImageOnly

        @Suppress("UNCHECKED_CAST")
        fun <T : View> getView(@LayoutRes resId: Int): T = (
            viewRecycler.getRecycledView(resId)
                ?: inflater.inflate(
                    resId,
                    fullContentContainerView,
                    false,
                )
            )
            .also {
                it.setTag(R.id.view_type, resId)
                fullContentContainerView.addView(it)
            } as T

        fun addFooter() {
            if (lazyUpdate) {
                // remove previous footers...
                for (i in fullContentContainerView.childCount - 1 downTo 0) {
                    val v = fullContentContainerView[i]
                    if (v.getTag(R.id.is_footer) == true) {
                        fullContentContainerView.removeViewAt(i)
                    }
                }
            }

            if (postView.post.locked) {
                val postRemovedView = getView<View>(R.layout.full_content_post_locked_view)
                postRemovedView.setTag(R.id.is_footer, true)
                val cardView: MaterialCardView = postRemovedView.findViewById(R.id.card_view)
                val textView: TextView = postRemovedView.findViewById(R.id.text)

                textView.textSize = config.bodyTextSizeSp.toTextSize()

                postRemovedView.layoutParams =
                    (postRemovedView.layoutParams as MarginLayoutParams).apply {
                        topMargin = context.resources.getDimensionPixelOffset(R.dimen.padding)
                    }

                textView.text = context.getString(
                    R.string.locked_post_message,
                    postView.community.name,
                )

                // workaround a cardview bug
                cardView.setContentPadding(0, 0, 0, 0)
            }

            if (postView.post.removed) {
                val postRemovedView = getView<View>(R.layout.full_content_post_removed_view)
                postRemovedView.setTag(R.id.is_footer, true)
                val cardView: MaterialCardView = postRemovedView.findViewById(R.id.card_view)
                val textView: TextView = postRemovedView.findViewById(R.id.text)

                textView.textSize = config.bodyTextSizeSp.toTextSize()

                postRemovedView.layoutParams =
                    (postRemovedView.layoutParams as MarginLayoutParams).apply {
                        topMargin = context.resources.getDimensionPixelOffset(R.dimen.padding)
                    }

                textView.text = context.getString(
                    R.string.removed_by_mod_message,
                    postView.community.name,
                )

                // workaround a cardview bug
                cardView.setContentPadding(0, 0, 0, 0)
            } else if (postView.post.deleted) {
                val postRemovedView = getView<View>(R.layout.full_content_post_removed_view)
                postRemovedView.setTag(R.id.is_footer, true)
                val cardView: MaterialCardView = postRemovedView.findViewById(R.id.card_view)
                val textView: TextView = postRemovedView.findViewById(R.id.text)

                textView.textSize = config.bodyTextSizeSp.toTextSize()

                postRemovedView.layoutParams =
                    (postRemovedView.layoutParams as MarginLayoutParams).apply {
                        topMargin = context.resources.getDimensionPixelOffset(R.dimen.padding)
                    }

                textView.text = context.getString(R.string.removed_by_op_message)

                // workaround a cardview bug
                cardView.setContentPadding(0, 0, 0, 0)
            }
        }

        if (postView.shouldHideItem() && !reveal && !lazyUpdate) {
            val fullContentHiddenView = getView<View>(R.layout.full_content_hidden_view)
            val fullImageView = fullContentHiddenView.findViewById<ImageView>(R.id.full_image)
            val textView = fullContentHiddenView.findViewById<TextView>(R.id.message)
            val button = fullContentHiddenView.findViewById<Button>(R.id.button)

            fullImageView.load(null)
            textView.textSize = config.bodyTextSizeSp.toTextSize()

            val imageUrl = targetPostView.post.url ?: postView.getThumbnailUrl(false)

            fun updateLayoutParams() {
                imageUrl ?: return

                offlineManager.getImageSizeHint(imageUrl, tempSize)
                if (tempSize.width > 0 && tempSize.height > 0) {
                    val thumbnailMaxHeight =
                        (contentMaxWidth * (tempSize.height.toDouble() / tempSize.width)).toInt()
                    fullImageView.updateLayoutParams<LayoutParams> {
                        this.height = thumbnailMaxHeight
                    }
                } else {
                    fullImageView.updateLayoutParams<LayoutParams> {
                        this.height = WRAP_CONTENT
                    }
                }
            }

            fun fetchFullImage() {
                imageUrl ?: return

                offlineManager.fetchImageWithError(
                    rootView,
                    imageUrl,
                    b@{
                        offlineManager.getMaxImageSizeHint(it, tempSize)

                        Log.d(TAG, "image size: $tempSize")

                        var w: Int? = null
                        var h: Int? = null
                        if (tempSize.height > 0 && tempSize.width > 0) {
                            val heightToWidthRatio = tempSize.height / tempSize.width

                            if (heightToWidthRatio > 10) {
                                // shrink the image if needed
                                w = tempSize.width
                                h = tempSize.height
                            }
                        }

                        fullImageView.load(it) {
                            allowHardware(false)
                            val sampling = (contentMaxWidth * 0.04f).coerceAtLeast(10f)
                            this.transformations(BlurTransformation(context, sampling = sampling))

                            val finalW = w
                            val finalH = h
                            if (finalW != null && finalH != null) {
                                this.size(finalW, finalH)
                            }

                            listener { _, result ->
                                val d = result.drawable
                                if (d is BitmapDrawable) {
                                    offlineManager.setImageSizeHint(
                                        imageUrl,
                                        d.bitmap.width,
                                        d.bitmap.height,
                                    )
                                    Log.d(TAG, "w: ${d.bitmap.width} h: ${d.bitmap.height}")

                                    updateLayoutParams()
                                }
                            }
                        }
                    },
                    {
                    },
                )
            }

            if (imageUrl != null) {
                updateLayoutParams()
                fullImageView.setOnLongClickListener {
                    onLinkLongClick(imageUrl, null)
                    true
                }

                fetchFullImage()
            } else {
                fullImageView.visibility = View.GONE
            }

            when {
                postView.post.nsfw -> {
                    textView.setText(R.string.reveal_warning_nsfw)
                }
//                listingItem.spoiler -> {
//                    textView.setText(R.string.reveal_warning_spoiler)
//                }
                else -> {
                    textView.setText(R.string.reveal_warning_default)
                }
            }
            button.setText(R.string.show_post)
            button.setOnClickListener {
                onRevealContentClickedFn()
            }

            addFooter()
            return
        }

        var customPlayerView: CustomPlayerView? = null

        fun loadPreviewInfo(imageView: ImageView, attachClickHandler: Boolean = false) {
            val previewInfo: PreviewInfo? = postView.getPreviewInfo()
                ?: postView.getThumbnailPreviewInfo()

            imageView.load(null)

            if (!previewInfo?.getUrl().isNullOrBlank()) {
                val url = checkNotNull(previewInfo).getUrl()
                offlineManager.fetchImage(rootView, url) {
                    offlineManager.getMaxImageSizeHint(it, tempSize)

                    imageView.load(it) {
                        allowHardware(false)
                    }
                }

                if (attachClickHandler) {
                    imageView.transitionName = fullImageViewTransitionName
                    imageView.setOnClickListener {
                        onFullImageViewClickListener(it, previewInfo.getUrl())
                    }
                    imageView.setOnLongClickListener {
                        onLinkLongClick(previewInfo.getUrl(), null)
                        true
                    }
                }
            } else {
                imageView.visibility = View.GONE
            }
        }

        fun appendUiForExternalOrInternalUrl(url: String) {
            val externalContentView =
                getView<View>(R.layout.full_content_external_content_view)
            val thumbnailView = externalContentView.findViewById<ImageView>(R.id.thumbnail)
            val externalContentTextView =
                externalContentView.findViewById<TextView>(R.id.external_content_text)

            externalContentTextView.textSize = config.bodyTextSizeSp.toTextSize()

            loadPreviewInfo(thumbnailView)

            externalContentTextView.text = Uri.parse(url).host ?: url
            externalContentView.setOnClickListener {
                val uri = Uri.parse(url)
                val pageRef = LinkResolver.parseUrl(url, instance)
                if (pageRef != null) {
                    onLemmyUrlClick(pageRef)
                } else if (ContentUtils.isUrlImage(url)) {
                    onImageClickListener(url)
                } else if (uri.path?.endsWith(".gifv") == true ||
                    uri.path?.endsWith(".mp4") == true
                ) {
                    onVideoClickListener(url, VideoType.Unknown, customPlayerView?.getVideoState())
                } else if (uri.host == "gfycat.com") {
                    val keyLowerCase = uri.path?.substring(1)?.split("-")?.get(0) ?: ""
                    val thumbnailUrl = requireNotNull(targetPostView.post.thumbnail_url)
                    val startIndex = thumbnailUrl.indexOf(keyLowerCase, ignoreCase = true)
                    if (startIndex > -1 && keyLowerCase.isNotBlank()) {
                        val key =
                            thumbnailUrl.substring(startIndex, startIndex + keyLowerCase.length)
                        onVideoClickListener(
                            "https://thumbs.gfycat.com/$key-mobile.mp4",
                            VideoType.Unknown,
                            customPlayerView?.getVideoState(),
                        )
                    } else {
                        onLinkClick(thumbnailUrl, null, LinkContext.Rich)
                    }
                } else if (uri.host == "imgur.com") {
                    onImageClickListener(url)
                } else {
                    onLinkClick(url, null, LinkContext.Rich)
                }
            }
            externalContentView.setOnLongClickListener {
                onLinkLongClick(url, null)
                true
            }
        }
        fun insertAndLoadFullImage(originalImageUrl: String, fallback: String? = null) {
            if (!showImage) return

            val fullContentImageView = getView<View>(R.layout.full_content_image_view)
            val fullImageView = fullContentImageView.findViewById<ImageView>(R.id.full_image)
            val loadingView =
                fullContentImageView.findViewById<LoadingView>(R.id.loading_view)

            if (onlyImage) {
                fullImageView.updateLayoutParams<MarginLayoutParams> {
                    this.topMargin = 0
                }
            }

            fullImageView.load(null)
            fullImageView.setOnLongClickListener {
                onLinkLongClick(originalImageUrl, null)
                true
            }

            fun updateLayoutParams() {
                offlineManager.getImageSizeHint(originalImageUrl, tempSize)
                if (tempSize.width > 0 && tempSize.height > 0) {
                    val thumbnailMaxHeight =
                        (contentMaxWidth * (tempSize.height.toDouble() / tempSize.width)).toInt()
                    fullImageView.updateLayoutParams<LayoutParams> {
                        this.height = thumbnailMaxHeight
                    }
                } else {
                    fullImageView.updateLayoutParams<LayoutParams> {
                        this.height = WRAP_CONTENT
                    }
                }
            }

            fun fetchFullImage(imageUrl: String) {
                loadingView?.showProgressBar()
                offlineManager.fetchImageWithError(
                    rootView,
                    imageUrl,
                    b@{
                        loadingView?.hideAll(animate = false)
                        offlineManager.getMaxImageSizeHint(it, tempSize)

                        Log.d(TAG, "image size: $tempSize")

                        var w: Int? = null
                        var h: Int? = null
                        if (tempSize.height > 0 && tempSize.width > 0) {
                            val heightToWidthRatio = tempSize.height / tempSize.width

                            if (heightToWidthRatio > 10) {
                                // shrink the image if needed
                                w = tempSize.width
                                h = tempSize.height
                            }
                        }

                        fullImageView.load(it) {
                            allowHardware(false)

                            val finalW = w
                            val finalH = h
                            if (finalW != null && finalH != null) {
                                this.size(finalW, finalH)
                            }

                            listener { _, result ->
                                result.drawable.getSize(tempSize)
                                Log.d(TAG, "w: ${tempSize.width} h: ${tempSize.height}")

                                if (tempSize.width > 0 && tempSize.height > 0) {
                                    offlineManager.setImageSizeHint(
                                        originalImageUrl,
                                        tempSize.width,
                                        tempSize.height,
                                    )
                                }

                                updateLayoutParams()
                            }
                        }
                    },
                    {
                        if (imageUrl != fallback && fallback != null) {
                            fetchFullImage(fallback)
                        } else {
                            loadingView?.showDefaultErrorMessageFor(it)
                        }
                    },
                )
            }

            updateLayoutParams()
            offlineManager.getImageSizeHint(originalImageUrl, tempSize)

            loadingView?.setOnRefreshClickListener {
                fetchFullImage(originalImageUrl)
            }
            fetchFullImage(originalImageUrl)

            fullImageView.transitionName = fullImageViewTransitionName
            fullImageView.setOnClickListener {
                onFullImageViewClickListener(it, originalImageUrl)
            }
        }

        if (!lazyUpdate) {
            val postType = postView.getType()
            when (postType) {
                PostType.Image -> {
                    insertAndLoadFullImage(
                        originalImageUrl = requireNotNull(targetPostView.post.url),
                        fallback = targetPostView.post.thumbnail_url,
                    )
                }
                PostType.Video -> {
                    val containerView = getView<View>(R.layout.full_content_video_view)
                    val playerView = containerView.findViewById<CustomPlayerView>(R.id.player_view)

                    customPlayerView = playerView

                    val videoInfo = targetPostView.getVideoInfo()
                    if (videoInfo != null) {
                        val videoType = getVideoType(videoInfo.videoUrl)

                        val bestSize = LemmyUtils.calculateBestVideoSize(
                            context,
                            videoInfo,
                            availableH = videoViewMaxHeight,
                        )
                        if (bestSize.y > 0) {
                            containerView.layoutParams = containerView.layoutParams.apply {
                                height = bestSize.y
                            }
                            playerView.layoutParams = playerView.layoutParams.apply {
                                height = bestSize.y
                            }
                        } else {
                            containerView.layoutParams = containerView.layoutParams.apply {
                                height = WRAP_CONTENT
                            }
                            playerView.layoutParams = playerView.layoutParams.apply {
                                height = WRAP_CONTENT
                            }
                        }
                        rootView.requestLayout()

                        playerView.findViewById<ImageButton>(R.id.exo_more).setOnClickListener {
                            onVideoLongClickListener(videoInfo.videoUrl)
                        }
                        playerView.setFullscreenButtonClickListener {
                            onVideoClickListener(
                                videoInfo.videoUrl,
                                videoType,
                                customPlayerView?.getVideoState()?.let {
                                    it.copy(
                                        currentTime = it.currentTime - ExoPlayerManager.CONVENIENCE_REWIND_TIME_MS,
                                    )
                                },
                            )
                        }

                        playerView.setup()

                        playerView.player = null
                        playerView.player =
                            exoPlayerManager.getPlayerForUrl(
                                videoInfo.videoUrl,
                                videoType,
                                videoState = videoState,
                            )
                    } else {
                        playerView.visibility = View.GONE
                    }
                }
                PostType.Text, PostType.Link -> {
                    val thumbnail = targetPostView.post.thumbnail_url
                    if (thumbnail != null && ContentUtils.isUrlImage(thumbnail)) {
                        insertAndLoadFullImage(thumbnail)
                    }
                }
            }

            if (!postView.post.body.isNullOrBlank() && showText) {
                val fullTextView = getView<View>(R.layout.full_content_text_view)
                val bodyTextView: TextView = fullTextView.findViewById(R.id.body)
                fullContentContainerView.setTag(R.id.body, bodyTextView)

                bodyTextView.visibility = View.VISIBLE
                bodyTextView.textSize = config.bodyTextSizeSp.toTextSize()

                if (contentMaxLines > 0) {
                    bodyTextView.ellipsize = TextUtils.TruncateAt.END
                    bodyTextView.maxLines = contentMaxLines
                    bodyTextView.setHorizontallyScrolling(false)
                } else {
                    bodyTextView.maxLines = Integer.MAX_VALUE
                }
                bodyTextView.setOnClickListener {
                    onItemClickListener()
                }
            }

            if ((
                    alwaysShowLinkBelowPost ||
                        postType == PostType.Link ||
                        postType == PostType.Text
                    ) && showLink
            ) {
                if (targetPostView.post.embed_video_url != null) {
                    appendUiForExternalOrInternalUrl(targetPostView.post.embed_video_url)
                } else if (targetPostView.post.url != null &&
                    targetPostView.post.thumbnail_url != targetPostView.post.url
                ) {
                    appendUiForExternalOrInternalUrl(targetPostView.post.url)
                }
            }
        }

        (fullContentContainerView.getTag(R.id.body) as? TextView)?.let { textView ->
            LemmyTextHelper.bindText(
                textView = textView,
                text = postView.post.body ?: "",
                instance = instance,
                highlight = highlight,
                onImageClick = onImageClickListener,
                onVideoClick = {
                    onVideoClickListener(it, VideoType.Unknown, null)
                },
                onPageClick = onLemmyUrlClick,
                onLinkClick = onLinkClick,
                onLinkLongClick = onLinkLongClick,
            )
        }

        addFooter()
    }

    fun recycleFullContent(fullContentContainerView: ViewGroup): RecycledState =
        getState(fullContentContainerView, recycle = true)

    fun getState(fullContentContainerView: ViewGroup, recycle: Boolean = false): RecycledState {
        assertMainThread()

        val stateBuilder = RecycledState.Builder()

        if (fullContentContainerView.childCount == 0) return stateBuilder.build()

        for (i in 0 until fullContentContainerView.childCount) {
            val c = fullContentContainerView.getChildAt(0)
            val viewId = c.getTag(R.id.view_type) as Int

            if (viewId == R.layout.full_content_video_view) {
                val playerView = c.findViewById<CustomPlayerView>(R.id.player_view)
                stateBuilder.setVideoState(playerView?.player?.getVideoState())

                if (recycle) {
                    exoPlayerManager.release(playerView.player)
                }
            }

            if (recycle) {
                fullContentContainerView.removeViewAt(0)
                viewRecycler.addRecycledView(c, viewId)
            }
        }

        if (recycle) {
            fullContentContainerView.setTag(R.id.body, null)
        }

        return stateBuilder.build()
    }

    private fun Float.toTextSize(): Float = this * textSizeMultiplier * globalFontSizeMultiplier
}
