package com.idunnololz.summit.lemmy

import android.app.AlertDialog
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
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.TextView
import androidx.annotation.LayoutRes
import androidx.core.view.get
import androidx.core.view.updateLayoutParams
import coil.load
import coil.size.Scale
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
import com.idunnololz.summit.lemmy.post.QueryMatchHelper
import com.idunnololz.summit.lemmy.post.QueryMatchHelper.HighlightTextData
import com.idunnololz.summit.lemmy.postListView.FullContentConfig
import com.idunnololz.summit.offline.OfflineManager
import com.idunnololz.summit.preview.VideoType
import com.idunnololz.summit.util.ContentUtils
import com.idunnololz.summit.util.ContentUtils.isUrlMp4
import com.idunnololz.summit.util.PreviewInfo
import com.idunnololz.summit.util.RecycledState
import com.idunnololz.summit.util.Size
import com.idunnololz.summit.util.Utils
import com.idunnololz.summit.util.ViewRecycler
import com.idunnololz.summit.util.assertMainThread
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
    private val viewRecycler: ViewRecycler<View> = ViewRecycler<View>(),
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

        onFullImageViewClickListener: (imageView: View?, url: String) -> Unit,
        onImageClickListener: (url: String) -> Unit,
        onVideoClickListener: (url: String, videoType: VideoType, videoState: VideoState?) -> Unit,
        onItemClickListener: () -> Unit,
        onRevealContentClickedFn: () -> Unit,
        onLemmyUrlClick: (PageRef) -> Unit,
        onLinkLongClick: (url: String, text: String?) -> Unit,
    ) {
        assertMainThread()
        if (!lazyUpdate) {
            fullContentContainerView.removeAllViews()
        }

        val context = rootView.context
        // Handles crossposts
        val targetPostView = postView

        @Suppress("UNCHECKED_CAST")
        fun <T : View> getView(@LayoutRes resId: Int): T =
            (
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
                val cardView: MaterialCardView = postRemovedView.findViewById(R.id.cardView)
                val textView: TextView = postRemovedView.findViewById(R.id.text)

                textView.textSize = config.bodyTextSizeSp.toTextSize()

                postRemovedView.layoutParams =
                    (postRemovedView.layoutParams as ViewGroup.MarginLayoutParams).apply {
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
                val cardView: MaterialCardView = postRemovedView.findViewById(R.id.cardView)
                val textView: TextView = postRemovedView.findViewById(R.id.text)

                textView.textSize = config.bodyTextSizeSp.toTextSize()

                postRemovedView.layoutParams =
                    (postRemovedView.layoutParams as ViewGroup.MarginLayoutParams).apply {
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
                val cardView: MaterialCardView = postRemovedView.findViewById(R.id.cardView)
                val textView: TextView = postRemovedView.findViewById(R.id.text)

                textView.textSize = config.bodyTextSizeSp.toTextSize()

                postRemovedView.layoutParams =
                    (postRemovedView.layoutParams as ViewGroup.MarginLayoutParams).apply {
                        topMargin = context.resources.getDimensionPixelOffset(R.dimen.padding)
                    }

                textView.text = context.getString(R.string.removed_by_op_message)

                // workaround a cardview bug
                cardView.setContentPadding(0, 0, 0, 0)
            }
        }

        if (postView.shouldHideItem() && !reveal && !lazyUpdate) {
            val fullContentHiddenView = getView<View>(R.layout.full_content_hidden_view)
            val fullImageView = fullContentHiddenView.findViewById<ImageView>(R.id.fullImage)
            val textView = fullContentHiddenView.findViewById<TextView>(R.id.message)
            val button = fullContentHiddenView.findViewById<Button>(R.id.button)

            textView.textSize = config.bodyTextSizeSp.toTextSize()

            val imageUrl = postView.getThumbnailUrl(false)

            if (imageUrl != null) {
                fullImageView.load(null)

                fun fetchFullImage() {
                    offlineManager.fetchImage(rootView, imageUrl) b@{
                        offlineManager.calculateImageMaxSizeIfNeeded(it)
                        offlineManager.getMaxImageSizeHint(it, tempSize)

                        fullImageView.load(it) {
                            val sampling = (contentMaxWidth * 0.04f).coerceAtLeast(10f)
                            this.transformations(BlurTransformation(context, sampling = sampling))

                            listener { _, result ->
                                val d = result.drawable
                                if (d is BitmapDrawable) {
                                    offlineManager.setImageSizeHint(
                                        imageUrl,
                                        d.bitmap.width,
                                        d.bitmap.height,
                                    )
                                    Log.d(TAG, "w: ${d.bitmap.width} h: ${d.bitmap.height}")
                                }
                            }
                        }
                    }
                }

                offlineManager.getImageSizeHint(imageUrl, tempSize)
                if (tempSize.width > 0 && tempSize.height > 0) {
                    val thumbnailMaxHeight =
                        (contentMaxWidth * (tempSize.height.toDouble() / tempSize.width)).toInt()
                    fullImageView.updateLayoutParams<LayoutParams> {
                        this.height = thumbnailMaxHeight
                    }
                }
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
            button.setText(R.string.show_post_question)
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
                    offlineManager.calculateImageMaxSizeIfNeeded(it)
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
                externalContentView.findViewById<TextView>(R.id.externalContentText)

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
                    onVideoClickListener(url, VideoType.UNKNOWN, customPlayerView?.getVideoState())
                } else if (uri.host == "gfycat.com") {
                    val keyLowerCase = uri.path?.substring(1)?.split("-")?.get(0) ?: ""
                    val url = requireNotNull(targetPostView.post.thumbnail_url)
                    val startIndex = url.indexOf(keyLowerCase, ignoreCase = true)
                    if (startIndex > -1 && keyLowerCase.isNotBlank()) {
                        val key =
                            url.substring(startIndex, startIndex + keyLowerCase.length)
                        onVideoClickListener(
                            "https://thumbs.gfycat.com/$key-mobile.mp4",
                            VideoType.UNKNOWN,
                            customPlayerView?.getVideoState(),
                        )
                    } else {
                        Utils.openExternalLink(context, url)
                    }
                } else if (uri.host == "imgur.com") {
                    onImageClickListener(url)
                } else {
                    Utils.openExternalLink(context, url)
                }
            }
            externalContentView.setOnLongClickListener {
                onLinkLongClick(url, null)
                true
            }
        }
        fun insertAndLoadFullImage(imageUrl: String) {
            val fullContentImageView = getView<View>(R.layout.full_content_image_view)
            val fullImageView = fullContentImageView.findViewById<ImageView>(R.id.fullImage)
            val loadingView =
                fullContentImageView.findViewById<LoadingView>(R.id.loadingView)

            fullImageView.load(null)
            fullImageView.setOnLongClickListener {
                onLinkLongClick(imageUrl, null)
                true
            }

            fun updateLayoutParams() {
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
                loadingView?.showProgressBar()
                offlineManager.fetchImageWithError(
                    rootView,
                    imageUrl,
                    b@{
                        loadingView?.hideAll(animate = false)
                        offlineManager.calculateImageMaxSizeIfNeeded(it)
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

                            if (w != null && h != null) {
                                this.size(w, h)
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
                        loadingView?.showDefaultErrorMessageFor(it)
                    },
                )
            }

            updateLayoutParams()

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
                    insertAndLoadFullImage(requireNotNull(targetPostView.post.url))
                }
                PostType.Video -> {
                    val containerView = getView<View>(R.layout.full_content_video_view)
                    val playerView = containerView.findViewById<CustomPlayerView>(R.id.playerView)

                    customPlayerView = playerView

                    val videoInfo = targetPostView.getVideoInfo()
                    if (videoInfo != null) {
                        val videoType = if (isUrlMp4(videoInfo.videoUrl)) {
                            VideoType.MP4
                        } else {
                            VideoType.DASH
                        }

                        val bestSize = LemmyUtils.calculateBestVideoSize(
                            context,
                            videoInfo,
                            availableH = videoViewMaxHeight,
                        )
                        if (bestSize.y > 0) {
                            containerView.layoutParams = containerView.layoutParams.apply {
                                // width = bestSize.x
                                height = bestSize.y
                            }
                            playerView.layoutParams = playerView.layoutParams.apply {
                                // width = bestSize.x
                                height = bestSize.y
                            }
                        } else {
                            containerView.layoutParams = containerView.layoutParams.apply {
                                // width = bestSize.x
                                height = LayoutParams.WRAP_CONTENT
                            }
                            playerView.layoutParams = playerView.layoutParams.apply {
                                // width = bestSize.x
                                height = LayoutParams.WRAP_CONTENT
                            }
                        }
                        rootView.requestLayout()

                        playerView.findViewById<ImageButton>(R.id.exo_more).setOnClickListener {
                            PopupMenu(context, it).apply {
                                inflate(R.menu.video_menu)

                                setOnMenuItemClickListener {
                                    when (it.itemId) {
                                        R.id.save -> {
                                            AlertDialog.Builder(context)
                                                .setMessage(R.string.coming_soon)
                                                .show()
                                            true
                                        }
                                        else -> false
                                    }
                                }

                                show()
                            }
                        }
                        playerView.setFullscreenButtonClickListener {
                            onVideoClickListener(
                                videoInfo.videoUrl,
                                videoType,
                                customPlayerView?.getVideoState()?.let {
                                    it.copy(currentTime = it.currentTime - ExoPlayerManager.CONVENIENCE_REWIND_TIME_MS)
                                },
                            )
                        }
//                        playerView.s

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

            if (!postView.post.body.isNullOrBlank()) {
                val fullTextView = getView<View>(R.layout.full_content_text_view)
                val bodyTextView: TextView = fullTextView.findViewById(R.id.body)
                rootView.setTag(R.id.body, bodyTextView)

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

            if (alwaysShowLinkBelowPost || postType == PostType.Link || postType == PostType.Text) {
                if (targetPostView.post.embed_video_url != null) {
                    appendUiForExternalOrInternalUrl(targetPostView.post.embed_video_url)
                } else if (targetPostView.post.url != null &&
                    targetPostView.post.thumbnail_url != targetPostView.post.url
                ) {
                    appendUiForExternalOrInternalUrl(targetPostView.post.url)
                }
            }
        }

        (rootView.getTag(R.id.body) as? TextView)?.let { textView ->
            LemmyTextHelper.bindText(
                textView = textView,
                text = postView.post.body ?: "",
                instance = instance,
                highlight = highlight,
                onImageClick = onImageClickListener,
                onVideoClick = {
                    onVideoClickListener(it, VideoType.UNKNOWN, null)
                },
                onPageClick = onLemmyUrlClick,
                onLinkLongClick = onLinkLongClick,
            )
        }

        addFooter()
    }

    fun recycleFullContent(
        fullContentContainerView: ViewGroup,
    ): RecycledState = getState(fullContentContainerView, recycle = true)

    fun getState(
        fullContentContainerView: ViewGroup,
        recycle: Boolean = false,
    ): RecycledState {
        assertMainThread()

        val stateBuilder = RecycledState.Builder()

        if (fullContentContainerView.childCount == 0) return stateBuilder.build()

        for (i in 0 until fullContentContainerView.childCount) {
            val c = fullContentContainerView.getChildAt(0)
            val viewId = c.getTag(R.id.view_type) as Int

            if (viewId == R.layout.full_content_video_view) {
                val playerView = c.findViewById<CustomPlayerView>(R.id.playerView)
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

        return stateBuilder.build()
    }

    private fun Float.toTextSize(): Float =
        this * textSizeMultiplier * globalFontSizeMultiplier
}
