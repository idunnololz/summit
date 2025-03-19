package com.idunnololz.summit.lemmy

import android.content.Context
import android.net.Uri
import android.text.Spanned
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
import arrow.core.Either
import coil3.load
import coil3.request.allowHardware
import coil3.request.transformations
import com.google.android.material.card.MaterialCardView
import com.idunnololz.summit.R
import com.idunnololz.summit.api.dto.PostView
import com.idunnololz.summit.api.utils.PostType
import com.idunnololz.summit.api.utils.getImageUrl
import com.idunnololz.summit.api.utils.getPreviewInfo
import com.idunnololz.summit.api.utils.getThumbnailPreviewInfo
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
import com.idunnololz.summit.offline.TaskFailedListener
import com.idunnololz.summit.preview.VideoType
import com.idunnololz.summit.util.ContentUtils
import com.idunnololz.summit.util.ContentUtils.getVideoType
import com.idunnololz.summit.util.ContentUtils.isUrlVideo
import com.idunnololz.summit.util.PreviewInfo
import com.idunnololz.summit.util.RecycledState
import com.idunnololz.summit.util.Size
import com.idunnololz.summit.util.ViewRecycler
import com.idunnololz.summit.util.assertMainThread
import com.idunnololz.summit.util.coil.BlurTransformation
import com.idunnololz.summit.util.ext.setup
import com.idunnololz.summit.video.ExoPlayerManager
import com.idunnololz.summit.video.VideoState
import com.idunnololz.summit.view.CustomPlayerView
import com.idunnololz.summit.view.LoadingView
import java.io.File

class LemmyContentHelper(
    private val context: Context,
    private val offlineManager: OfflineManager,
    private val getExoPlayerManager: () -> ExoPlayerManager,
    private val viewRecycler: ViewRecycler<View> = ViewRecycler(),
) {

    companion object {
        private const val TAG = "LemmyContentHelper"
    }

    private val inflater = LayoutInflater.from(context)

    var config: FullContentConfig = FullContentConfig()
        set(value) {
            field = value

            textSizeMultiplier = config.textSizeMultiplier
        }

    private var textSizeMultiplier: Float = config.textSizeMultiplier
    var globalFontSizeMultiplier: Float = 1f
    var alwaysShowLinkBelowPost: Boolean = false
    var fullBleedImage: Boolean = true

    /**
     * @param lazyUpdate If true, content will not be refreshed. Only non content related things
     * will be setup (eg. removed post warning)
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
        autoPlayVideos: Boolean,
        lazyUpdate: Boolean = false,
        videoState: VideoState? = null,
        contentMaxLines: Int = -1,
        highlight: HighlightTextData? = null,
        contentSpannable: Spanned? = null,
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
        onTextBound: (Spanned?) -> Unit = {},
    ) {
        assertMainThread()
        if (!lazyUpdate) {
            fullContentContainerView.removeAllViews()
        }

        val context = rootView.context

        val postUrl = postView.post.url
        val postViewType = screenshotConfig?.postViewType
        val showText = postViewType != PostViewType.ImageOnly &&
            postViewType != PostViewType.TitleAndImageOnly
        val showImage = postViewType != PostViewType.TextOnly
        val showLink = postViewType == null ||
            screenshotConfig.postViewType == PostViewType.Full
        val onlyImage = postViewType == PostViewType.ImageOnly
        val thumbnailUrl = postView.post.thumbnail_url
        val isThumbnailUrlValid = thumbnailUrl != null && ContentUtils.isUrlImage(thumbnailUrl)
        val imageMaxWidth = if (fullBleedImage) {
            contentMaxWidth
        } else {
            contentMaxWidth - (
                context.resources.getDimensionPixelOffset(
                    R.dimen.post_list_image_view_horizontal_margin,
                ) * 2
                )
        }.toInt()

        fun <T : View> getAndEnsureView(@LayoutRes resId: Int): T =
            fullContentContainerView.getAndEnsureView(resId)

        if (postView.shouldHideItem() && !reveal && !lazyUpdate) {
            val fullContentHiddenView = if (fullBleedImage) {
                getAndEnsureView<View>(R.layout.full_content_hidden_view)
            } else {
                getAndEnsureView<View>(R.layout.full_content_hidden_view2)
            }
            val fullImageView = fullContentHiddenView.findViewById<ImageView>(R.id.full_image)
            val textView = fullContentHiddenView.findViewById<TextView>(R.id.message)
            val button = fullContentHiddenView.findViewById<Button>(R.id.button)
            val loadingView = fullContentHiddenView.findViewById<LoadingView>(R.id.loading_view)

            fullImageView.load(null)
            textView.textSize = config.bodyTextSizeSp.toTextSize()

            val imageUrl = postView.getImageUrl(false)
                ?: if (postUrl != null && isUrlVideo(postUrl)) {
                    postUrl
                } else {
                    null
                }

            if (imageUrl != null) {
                fullImageView.visibility = View.VISIBLE
                fullImageView.setOnLongClickListener {
                    onLinkLongClick(imageUrl, null)
                    true
                }

                fun fetchFullImage() = loadThumbnailIntoImageView(
                    imageUrl = imageUrl,
                    imageSizeKey = imageUrl,
                    fallbackUrl = null,
                    contentMaxWidth = imageMaxWidth,
                    blur = true,
                    tempSize = tempSize,
                    rootView = fullContentContainerView,
                    loadingView = loadingView,
                    imageView = fullImageView,
                    preferFullSizeImage = true,
                )

                loadingView?.setOnRefreshClickListener {
                    fetchFullImage()
                }
                fetchFullImage()
            } else {
                loadingView.hideAll()
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

            addFooter(
                lazyUpdate = lazyUpdate,
                root = fullContentContainerView,
                postView = postView,
            )
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
                    offlineManager.getImageSizeHint(it, tempSize)

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
                getAndEnsureView<View>(R.layout.full_content_external_content_view)
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
                    val keyLowerCase = uri.path
                        ?.substring(1)
                        ?.split("-")
                        ?.get(0)
                        ?: ""
                    val thumbnailUrl = requireNotNull(thumbnailUrl)
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
        fun insertAndLoadFullImage(imageUrl: String, fallback: String? = null) {
            if (!showImage) return

            val fullContentImageView =
                if (fullBleedImage) {
                    getAndEnsureView<View>(R.layout.full_content_image_view)
                } else {
                    getAndEnsureView<View>(R.layout.full_content_image_view2)
                }
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
                onLinkLongClick(imageUrl, null)
                true
            }


            fun fetchFullImage() = loadThumbnailIntoImageView(
                imageUrl = imageUrl,
                imageSizeKey = imageUrl,
                fallbackUrl = fallback,
                contentMaxWidth = imageMaxWidth,
                blur = false,
                tempSize = tempSize,
                rootView = fullContentContainerView,
                loadingView = loadingView,
                imageView = fullImageView,
                preferFullSizeImage = true,
            )

            loadingView?.setOnRefreshClickListener {
                fetchFullImage()
            }
            fetchFullImage()

            fullImageView.transitionName = fullImageViewTransitionName
            fullImageView.setOnClickListener {
                onFullImageViewClickListener(it, imageUrl)
            }
        }

        if (!lazyUpdate) {
            val postType = postView.getType()
            when (postType) {
                PostType.Image -> {
                    insertAndLoadFullImage(
                        imageUrl = requireNotNull(postUrl),
                        fallback = thumbnailUrl,
                    )
                }
                PostType.Video -> {
                    val containerView = getAndEnsureView<View>(R.layout.full_content_video_view)
                    val playerView = containerView.findViewById<CustomPlayerView>(R.id.player_view)

                    customPlayerView = playerView

                    val videoInfo = postView.getVideoInfo()
                    if (videoInfo != null) {
                        val urlWithoutParams = videoInfo.videoUrl.split("?").getOrElse(0) { "" }
                        val videoType = getVideoType(urlWithoutParams)

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
                                        currentTime = it.currentTime -
                                            ExoPlayerManager.CONVENIENCE_REWIND_TIME_MS,
                                    )
                                },
                            )
                        }

                        playerView.setup()

                        playerView.player = null
                        playerView.player =
                            getExoPlayerManager().getPlayerForUrl(
                                url = videoInfo.videoUrl,
                                videoType = videoType,
                                videoState = videoState,
                                isInline = true,
                                autoPlay = autoPlayVideos,
                            )
                    } else {
                        playerView.visibility = View.GONE
                    }
                }
                PostType.Text, PostType.Link -> {
                    if (thumbnailUrl != null && isThumbnailUrlValid) {
                        insertAndLoadFullImage(thumbnailUrl)
                    }
                }
            }

            if (!postView.post.body.isNullOrBlank() && showText) {
                val fullTextView = getAndEnsureView<View>(R.layout.full_content_text_view)
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
                if (postView.post.embed_video_url != null) {
                    appendUiForExternalOrInternalUrl(postView.post.embed_video_url)
                } else if (postUrl != null &&
                    (thumbnailUrl != postUrl || !isThumbnailUrlValid)
                ) {
                    appendUiForExternalOrInternalUrl(postUrl)
                }
            }
        }

        (fullContentContainerView.getTag(R.id.body) as? TextView)?.let { textView ->
            val spannable = LemmyTextHelper.bindText(
                textView = textView,
                text = postView.post.body ?: "",
                instance = instance,
                spannedText = contentSpannable,
                highlight = highlight,
                onImageClick = onImageClickListener,
                onVideoClick = {
                    onVideoClickListener(it, VideoType.Unknown, null)
                },
                onPageClick = onLemmyUrlClick,
                onLinkClick = onLinkClick,
                onLinkLongClick = onLinkLongClick,
            )

            onTextBound(spannable)
        }

        addFooter(
            lazyUpdate = lazyUpdate,
            root = fullContentContainerView,
            postView = postView,
        )
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
                stateBuilder.setVideoState(playerView?.getVideoState())

                if (recycle) {
                    getExoPlayerManager().release(playerView.player)
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

    @Suppress("UNCHECKED_CAST")
    private fun <T : View> ViewGroup.getAndEnsureView(@LayoutRes resId: Int): T = (
        viewRecycler.getRecycledView(resId)
            ?: inflater.inflate(
                resId,
                this,
                false,
            )
        )
        .also {
            it.setTag(R.id.view_type, resId)
            this.addView(it)
        } as T

    fun addFooter(lazyUpdate: Boolean, root: ViewGroup, postView: PostView) {
        if (lazyUpdate) {
            // remove previous footers...
            for (i in root.childCount - 1 downTo 0) {
                val v = root[i]
                if (v.getTag(R.id.is_footer) == true) {
                    root.removeViewAt(i)
                }
            }
        }

        if (postView.post.locked) {
            val postRemovedView =
                root.getAndEnsureView<View>(R.layout.full_content_post_locked_view)
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
            val postRemovedView =
                root.getAndEnsureView<View>(R.layout.full_content_post_removed_view)
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
            val postRemovedView =
                root.getAndEnsureView<View>(R.layout.full_content_post_removed_view)
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

    fun loadThumbnailIntoImageView(
        imageUrl: String,
        imageSizeKey: String,
        fallbackUrl: String?,
        contentMaxWidth: Int,
        blur: Boolean,
        tempSize: Size,
        rootView: View,
        loadingView: LoadingView?,
        imageView: ImageView,
        preferFullSizeImage: Boolean,
        errorListener: TaskFailedListener? = null,
    ) {
        val isUrlVideo = isUrlVideo(imageUrl)
        imageView.visibility = View.VISIBLE
        loadingView?.showProgressBar()

        if (preferFullSizeImage) {
            imageView.updateLayoutParams(contentMaxWidth, imageSizeKey, tempSize)
        }

        fun onImageLoaded(urlOrFile: Either<String, File>) {
            urlOrFile.fold(
                { offlineManager.getImageSizeHint(it, tempSize) },
                { offlineManager.getImageSizeHint(it, tempSize) }
            )

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

            imageView.load(urlOrFile.getOrNull() ?: urlOrFile.leftOrNull()) {
                allowHardware(false)

                if (blur) {
                    val sampling = (contentMaxWidth * 0.04f).coerceAtLeast(10f)
                    this.transformations(BlurTransformation(context, sampling = sampling))
                }

                val finalW = w
                val finalH = h
                if (finalW != null && finalH != null) {
                    this.size(finalW, finalH)
                }

                listener { _, result ->
                    loadingView?.hideAll(animate = false)

                    tempSize.width = result.image.width
                    tempSize.height = result.image.height

                    Log.d(TAG, "w: ${tempSize.width} h: ${tempSize.height}")
                    imageView.alpha = 1f

                    if (preferFullSizeImage) {
                        if (tempSize.width > 0 && tempSize.height > 0) {
                            offlineManager.setImageSizeHint(
                                imageSizeKey,
                                tempSize.width,
                                tempSize.height,
                            )
                        }
                        imageView.updateLayoutParams(
                            contentMaxWidth = contentMaxWidth,
                            imageUrl = imageSizeKey,
                            tempSize = tempSize,
                        )
                    }
                }
            }
        }

        if (isUrlVideo) {
            onImageLoaded(Either.Left(imageUrl))
        } else {
            offlineManager.fetchImageWithError(
                rootView = rootView,
                url = imageUrl,
                listener = b@{
                    onImageLoaded(Either.Right(it))
                },
                errorListener = {
                    if (imageUrl != fallbackUrl && fallbackUrl != null) {
                        loadThumbnailIntoImageView(
                            imageUrl = fallbackUrl,
                            imageSizeKey = imageSizeKey,
                            fallbackUrl = fallbackUrl,
                            contentMaxWidth = contentMaxWidth,
                            blur = blur,
                            tempSize = tempSize,
                            rootView = rootView,
                            loadingView = loadingView,
                            imageView = imageView,
                            preferFullSizeImage = preferFullSizeImage,
                            errorListener = errorListener,
                        )
                    } else {
                        loadingView?.showDefaultErrorMessageFor(it)
                    }
                    errorListener?.invoke(it)
                },
            )
        }
    }

    private fun ImageView.updateLayoutParams(
        contentMaxWidth: Int,
        imageUrl: String,
        tempSize: Size,
    ) {
        val fullImageView = this
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

    private fun Float.toTextSize(): Float = this * textSizeMultiplier * globalFontSizeMultiplier
}
