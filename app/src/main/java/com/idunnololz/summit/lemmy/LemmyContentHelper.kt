package com.idunnololz.summit.lemmy

import android.content.Context
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.util.Base64
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.view.ViewTreeObserver
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.TextView
import androidx.annotation.LayoutRes
import androidx.core.view.ViewCompat
import androidx.core.view.get
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import coil.load
import com.commit451.coiltransformations.BlurTransformation
import com.google.android.material.card.MaterialCardView
import com.idunnololz.summit.R
import com.idunnololz.summit.api.utils.PostType
import com.idunnololz.summit.api.dto.PostView
import com.idunnololz.summit.api.utils.getPreviewInfo
import com.idunnololz.summit.api.utils.getThumbnailPreviewInfo
import com.idunnololz.summit.api.utils.getThumbnailUrl
import com.idunnololz.summit.api.utils.getType
import com.idunnololz.summit.api.utils.getVideoInfo
import com.idunnololz.summit.api.utils.shouldHideItem
import com.idunnololz.summit.offline.OfflineManager
import com.idunnololz.summit.preview.ImageViewerFragmentArgs
import com.idunnololz.summit.preview.VideoType
import com.idunnololz.summit.preview.VideoViewerFragmentArgs
import com.idunnololz.summit.reddit.LemmyUtils
import com.idunnololz.summit.util.CustomLinkMovementMethod
import com.idunnololz.summit.util.DefaultLinkLongClickListener
import com.idunnololz.summit.util.PreviewInfo
import com.idunnololz.summit.util.RecycledState
import com.idunnololz.summit.util.Size
import com.idunnololz.summit.util.Utils
import com.idunnololz.summit.util.ViewRecycler
import com.idunnololz.summit.util.ext.getColorCompat
import com.idunnololz.summit.util.ext.setup
import com.idunnololz.summit.video.ExoPlayerManager
import com.idunnololz.summit.video.VideoState
import com.idunnololz.summit.video.getVideoState
import com.idunnololz.summit.view.CustomPlayerView
import com.idunnololz.summit.view.LoadingView

class LemmyContentHelper(
    private val context: Context,
    private val fragment: Fragment,
    private val offlineManager: OfflineManager,
    private val exoPlayerManager: ExoPlayerManager,
    private val viewRecycler: ViewRecycler<View> = ViewRecycler<View>()
) {

    companion object {
        private const val TAG = "LemmyContentHelper"
    }

    private val inflater = LayoutInflater.from(context)


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

        onFullImageViewClickListener: (imageView: ImageView?, url: String) -> Unit,
        onImageClickListener: (url: String) -> Unit,
        onItemClickListener: () -> Unit,
        onRevealContentClickedFn: () -> Unit,
        lazyUpdate: Boolean = false,
        videoState: VideoState? = null,

    ) {
        if (!lazyUpdate) {
            fullContentContainerView.removeAllViews()
        }

        val context = rootView.context
//        val targetListingItem = if (listingItem.crosspostParent != null) {
//            listingItem.crosspostParentList?.first() ?: listingItem
//        } else {
//            listingItem
//        }
        val targetPostView = postView

        @Suppress("UNCHECKED_CAST")
        fun <T : View> getView(@LayoutRes resId: Int): T =
            (viewRecycler.getRecycledView(resId)
                ?: inflater.inflate(
                    resId,
                    fullContentContainerView,
                    false
                ))
                .also {
                    it.setTag(R.id.view_type, resId)
                    fullContentContainerView.addView(it)
                } as T

        fun addFooter() {
            if (lazyUpdate) {
                // remove previous footers...
                for (i in 0 until fullContentContainerView.childCount) {
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

                postRemovedView.layoutParams =
                    (postRemovedView.layoutParams as ViewGroup.MarginLayoutParams).apply {
                        topMargin = context.resources.getDimensionPixelOffset(R.dimen.padding)
                    }

                textView.text = context.getString(
                    R.string.locked_post_message,
                    postView.community.name
                )

                // workaround a cardview bug
                cardView.setContentPadding(0, 0, 0, 0)
            }


            if (postView.post.removed) {
                val postRemovedView = getView<View>(R.layout.full_content_post_removed_view)
                postRemovedView.setTag(R.id.is_footer, true)
                val cardView: MaterialCardView = postRemovedView.findViewById(R.id.cardView)
                val textView: TextView = postRemovedView.findViewById(R.id.text)

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

            val imageUrl = postView.getThumbnailUrl(false)

            if (imageUrl != null) {
                fullImageView.setImageResource(0)

                fun fetchFullImage() {
                    offlineManager.fetchImage(rootView, imageUrl) b@{
                        if (!fragment.isAdded || fragment.context == null) {
                            return@b
                        }

                        offlineManager.calculateImageMaxSizeIfNeeded(it)
                        offlineManager.getMaxImageSizeHint(it, tempSize)

                        fullImageView.load(it) {

                            this.transformations(BlurTransformation(context, sampling = 30f))

                            listener { _, result ->
                                val d = result.drawable
                                if (d is BitmapDrawable) {
                                    offlineManager.setImageSizeHint(
                                        imageUrl,
                                        d.bitmap.width,
                                        d.bitmap.height
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
                    fullImageView.updateLayoutParams<ViewGroup.LayoutParams> {
                        this.height = thumbnailMaxHeight
                    }
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
            LemmyUtils.setImageViewSizeBasedOnPreview(context, previewInfo, rootView, imageView)

            imageView.load(null)

            if (!previewInfo?.getUrl().isNullOrBlank()) {
                val url = checkNotNull(previewInfo).getUrl()
                offlineManager.fetchImage(rootView, url) {
                    offlineManager.calculateImageMaxSizeIfNeeded(it)
                    offlineManager.getMaxImageSizeHint(it, tempSize)

                    imageView.load(it)
                }

                if (attachClickHandler) {
                    ViewCompat.setTransitionName(imageView, fullImageViewTransitionName)
                    imageView.setOnClickListener {
                        onFullImageViewClickListener(null, previewInfo.getUrl())
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

            loadPreviewInfo(thumbnailView)

            externalContentTextView.text = Uri.parse(url).host ?: url
            externalContentView.setOnClickListener {
                val uri = Uri.parse(url)
                if (LemmyUtils.isUriReddit(uri)) {
                    LemmyUtils.openRedditUrl(context, url)
                } else if (uri.path?.endsWith(".jpg") == true ||
                    uri.path?.endsWith(".jpeg") == true ||
                    uri.path?.endsWith(".png") == true) {
                    val args = ImageViewerFragmentArgs(
                        null,
                        url,
                        null
                    )
                    fragment.findNavController()
                        .navigate(R.id.imageViewerFragment, args.toBundle())
                } else if (uri.path?.endsWith(".gifv") == true) {
                    val args = VideoViewerFragmentArgs(
                        url = url,
                        videoType = VideoType.UNKNOWN,
                        videoState = customPlayerView?.getVideoState()
                    )
                    fragment.findNavController()
                        .navigate(R.id.videoViewerFragment, args.toBundle())
                } else if (uri.host == "gfycat.com") {
                    val keyLowerCase = uri.path?.substring(1)?.split("-")?.get(0) ?: ""
                    val url = requireNotNull(targetPostView.post.thumbnail_url)
                    val startIndex = url.indexOf(keyLowerCase, ignoreCase = true)
                    if (startIndex > -1 && keyLowerCase.isNotBlank()) {
                        val key =
                            url.substring(startIndex, startIndex + keyLowerCase.length)
                        val args = VideoViewerFragmentArgs(
                            url = "https://thumbs.gfycat.com/${key}-mobile.mp4",
                            videoType = VideoType.UNKNOWN,
                            videoState = customPlayerView?.getVideoState()
                        )
                        fragment.findNavController()
                            .navigate(R.id.videoViewerFragment, args.toBundle())
                    } else {
                        Utils.openExternalLink(context, url)
                    }
                } else if (uri.host == "imgur.com") {
                    val args = ImageViewerFragmentArgs(
                        null,
                        url,
                        null
                    )
                    fragment.findNavController()
                        .navigate(R.id.imageViewerFragment, args.toBundle())
                } else {
                    Utils.openExternalLink(context, url)
                }
            }
            externalContentView.setOnLongClickListener {
                val popupMenu = PopupMenu(context, it)
                popupMenu.inflate(R.menu.menu_url)
                popupMenu.setOnMenuItemClickListener { item ->
                    when (item.itemId) {
                        R.id.copy_link -> {
                            Utils.copyToClipboard(context, url)
                            true
                        }
                        R.id.share_link -> {
                            Utils.shareText(context, url)
                            true
                        }
                        else -> false
                    }
                }
                popupMenu.show()
                true
            }
        }

        if (!lazyUpdate) {
            when (postView.getType()) {
                PostType.Image -> {
                    val fullContentImageView = getView<View>(R.layout.full_content_image_view)
                    val fullImageView = fullContentImageView.findViewById<ImageView>(R.id.fullImage)
                    val loadingView =
                        fullContentImageView.findViewById<LoadingView>(R.id.loadingView)

                    val imageUrl = requireNotNull(targetPostView.post.thumbnail_url)

                    fullImageView.setImageResource(0)

                    fun fetchFullImage() {
                        loadingView?.showProgressBar()
                        offlineManager.fetchImageWithError(rootView, imageUrl, b@{
                            if (!fragment.isAdded || fragment.context == null) {
                                return@b
                            }

                            loadingView?.hideAll(animate = false)
                            offlineManager.calculateImageMaxSizeIfNeeded(it)
                            offlineManager.getMaxImageSizeHint(it, tempSize)

                            fullImageView.load(it) {
                                listener { _, result ->
                                    val d = result.drawable
                                    if (d is BitmapDrawable) {
                                        offlineManager.setImageSizeHint(
                                            imageUrl,
                                            d.bitmap.width,
                                            d.bitmap.height
                                        )
                                        Log.d(TAG, "w: ${d.bitmap.width} h: ${d.bitmap.height}")
                                    }
                                }
                            }
                        }, {
                            loadingView?.showDefaultErrorMessageFor(it)
                        })
                    }

                    offlineManager.getImageSizeHint(imageUrl, tempSize)
                    if (tempSize.width > 0 && tempSize.height > 0) {
                        val thumbnailMaxHeight =
                            (contentMaxWidth * (tempSize.height.toDouble() / tempSize.width)).toInt()
                        fullImageView.updateLayoutParams<ViewGroup.LayoutParams> {
                            this.height = thumbnailMaxHeight
                        }
                    } else {
                        fullImageView.updateLayoutParams<ViewGroup.LayoutParams> {
                            this.height = WRAP_CONTENT
                        }
                    }

                    // try to guess image size...
//                    if (!offlineManager.hasImageSizeHint(imageUrl)) {
//                        var width = 0
//                        var height = 0
//                        targetListingItem.preview?.images?.first()?.source?.let {
//                            width = it.width
//                            height = it.height
//                        }
//                        rootView.measure(
//                            View.MeasureSpec.makeMeasureSpec(
//                                Utils.getScreenWidth(context),
//                                View.MeasureSpec.EXACTLY
//                            ),
//                            View.MeasureSpec.makeMeasureSpec(
//                                Utils.getScreenHeight(context),
//                                View.MeasureSpec.AT_MOST
//                            )
//                        )
//                        if (width != 0 && height != 0) {
//                            val thumbnailHeight =
//                                (fullImageView.measuredWidth * (height.toDouble() / width)).toInt()
//                            offlineManager.setImageSizeHint(
//                                targetListingItem.url,
//                                fullImageView.measuredWidth,
//                                thumbnailHeight
//                            )
//                        }
//                    }

                    loadingView?.setOnRefreshClickListener {
                        fetchFullImage()
                    }
                    fetchFullImage()

                    //ViewCompat.setTransitionName(fullImageView, fullImageViewTransitionName)
                    fullImageView.setOnClickListener {
                        onFullImageViewClickListener(null, imageUrl)
                    }
                }
                PostType.Video -> {
                    val containerView = getView<View>(R.layout.full_content_video_view)
                    val playerView = containerView.findViewById<CustomPlayerView>(R.id.playerView)

                    customPlayerView = playerView

                    val videoInfo = targetPostView.getVideoInfo()
                    if (videoInfo != null) {
                        val bestSize = LemmyUtils.calculateBestVideoSize(
                            context,
                            videoInfo,
                            availableH = videoViewMaxHeight
                        )
                        containerView.layoutParams = containerView.layoutParams.apply {
                            //width = bestSize.x
                            height = bestSize.y
                        }
                        playerView.layoutParams = playerView.layoutParams.apply {
                            //width = bestSize.x
                            height = bestSize.y
                        }

                        rootView.requestLayout()

                        playerView.findViewById<ImageButton>(R.id.exo_more).setOnClickListener {
                            PopupMenu(context, it).apply {
                                inflate(R.menu.video_menu)

                                setOnMenuItemClickListener {
                                    when (it.itemId) {
                                        R.id.save -> {
//                                            FileDownloadHelper
//                                                .downloadDashVideo(
//                                                    context = context,
//                                                    offlineManager = offlineManager,
//                                                    url = media.redditVideo.dashUrl,
//                                                    quality = FileDownloadHelper.Quality.WORST
//                                                )
//                                                .subscribeOn(Schedulers.io())
//                                                .observeOn(AndroidSchedulers.mainThread())
//                                                .subscribe({
//                                                    Log.d(TAG, "Download complete!")
//                                                }, {
//                                                    Log.e(TAG, "", it)
//                                                })

                                            true
                                        }
                                        R.id.save_hq -> {
//                                            FileDownloadHelper
//                                                .downloadDashVideo(
//                                                    context = context,
//                                                    offlineManager = offlineManager,
//                                                    url = media.redditVideo.dashUrl,
//                                                    quality = FileDownloadHelper.Quality.BEST
//                                                )
//                                                .subscribeOn(Schedulers.io())
//                                                .observeOn(AndroidSchedulers.mainThread())
//                                                .subscribe({
//                                                    Log.d(TAG, "Download complete!")
//                                                }, {
//                                                    Log.e(TAG, "", it)
//                                                })

                                            true
                                        }
                                        else -> false
                                    }
                                }

                                show()
                            }
                        }
                        playerView.findViewById<ImageButton>(R.id.exo_fullscreen)
                            .setOnClickListener {
                                val args = VideoViewerFragmentArgs(
                                    url = videoInfo.dashUrl,
                                    videoType = VideoType.DASH,
                                    videoState = customPlayerView?.getVideoState()?.let {
                                        it.copy(currentTime = it.currentTime - ExoPlayerManager.CONVENIENCE_REWIND_TIME_MS)
                                    }
                                )
                                fragment.findNavController()
                                    .navigate(R.id.videoViewerFragment, args.toBundle())
                            }

                        playerView.setup()

                        playerView.player = null
                        playerView.player = exoPlayerManager.getPlayerForUrl(
                            videoInfo.dashUrl,
                            VideoType.DASH,
                            videoState = videoState
                        )
                    } else {
                        playerView.visibility = View.GONE
                    }
                }
                PostType.Text -> {
                }
            }

            if (!postView.post.body.isNullOrBlank()) {
                val content = postView.post.body
                if (LemmyUtils.needsWebView(content)) {
                    val fullTextView = getView<View>(R.layout.full_content_web_view)
                    val webView: WebView = fullTextView.findViewById(R.id.webView)
                    val textColorHex = String.format(
                        "#%06X",
                        (0xFFFFFF and context.getColorCompat(R.color.colorText))
                    )
                    val bgColorHex = String.format(
                        "#%06X",
                        (0xFFFFFF and context.getColorCompat(R.color.colorBackground))
                    )
                    val aLinkColorHex = String.format(
                        "#%06X",
                        (0xFFFFFF and context.getColorCompat(R.color.colorLink))
                    )
                    val cssText = "<head><style type=\"text/css\">" +
                            "body{margin:0;padding:0;color:${textColorHex};background-color:${bgColorHex};}" +
                            "a{color:${aLinkColorHex}}" +
                            "td{border-bottom:1px solid ${textColorHex};}" +
                            "table{border-collapse:collapse;}" +
                            "</style></head>"

                    webView.setLayerType(View.LAYER_TYPE_HARDWARE, null);

                    webView.settings.apply {
                        javaScriptEnabled = false
                    }

                    webView.setBackgroundColor(0) // transparent

                    // Need to base 64 encode html due to stupid bug...
                    val html =
                        "<html>$cssText<body>${Utils.fromHtml(content)}</body></html>"
                    val b64 = Base64.encodeToString(html.toByteArray(), Base64.DEFAULT)
                    webView.loadData(
                        b64,
                        "text/html; charset=utf-8",
                        "base64"
                    )
                    webView.isVerticalScrollBarEnabled = false
                    webView.webViewClient = object : WebViewClient() {
                        override fun onPageFinished(view: WebView?, url: String?) {
                            super.onPageFinished(view, url)

                            webView.viewTreeObserver.addOnPreDrawListener(object :
                                ViewTreeObserver.OnPreDrawListener {
                                override fun onPreDraw(): Boolean {
                                    if (webView.measuredHeight != 0) {
                                        webView.layoutParams = webView.layoutParams.apply {
                                            width = webView.measuredWidth
                                            height = webView.measuredHeight
                                        }
                                        webView.viewTreeObserver.removeOnPreDrawListener(this)
                                    }
                                    return true
                                }
                            })
                        }
                    }
                } else {
                    val fullTextView = getView<View>(R.layout.full_content_text_view)
                    val bodyTextView: TextView = fullTextView.findViewById(R.id.body)
                    bodyTextView.visibility = View.VISIBLE
                    LemmyUtils.bindLemmyText(bodyTextView, content, instance)
                    bodyTextView.movementMethod = CustomLinkMovementMethod().apply {
                        onLinkLongClickListener = DefaultLinkLongClickListener(context)
                        this.onImageClickListener = onImageClickListener
                    }
                    bodyTextView.setOnClickListener {
                        onItemClickListener()
                    }
                }
            }

            if (targetPostView.post.embed_video_url != null) {
                appendUiForExternalOrInternalUrl(targetPostView.post.embed_video_url )
            } else if (targetPostView.post.url != null &&
                targetPostView.post.thumbnail_url != targetPostView.post.url) {
                appendUiForExternalOrInternalUrl(targetPostView.post.url)
            }
//            when (postType) {
//                ListingItemType.DEFAULT_SELF -> {
//                }
//                ListingItemType.REDDIT_IMAGE -> {
//                }
//                ListingItemType.REDDIT_VIDEO -> {
//                }
//                ListingItemType.REDDIT_GALLERY -> {
//                    val fullContentGalleryView = getView<View>(R.layout.full_content_gallery_view)
//                    val galleryRecyclerView =
//                        fullContentGalleryView.findViewById<RecyclerView>(R.id.galleryRecyclerView)
//                    val loadingView =
//                        fullContentGalleryView.findViewById<LoadingView>(R.id.loadingView)
//                    val pageIndicator =
//                        fullContentGalleryView.findViewById<GalleryPageIndicator>(R.id.pageIndicator)
//
//                    galleryRecyclerView.adapter =
//                        GalleryAdapter(
//                            context,
//                            requireNotNull(listingItem.mediaMetadata),
//                            requireNotNull(listingItem.galleryData),
//                            fragment,
//                            onFullImageViewClickListener
//                        )
//                    galleryRecyclerView.layoutManager =
//                        LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
//                    galleryRecyclerView.setHasFixedSize(true)
//
//                    if (galleryRecyclerView.onFlingListener == null) {
//                        // Check if PagerSnapHelper is already attached
//                        PagerSnapHelper().attachToRecyclerView(galleryRecyclerView)
//                    }
//
//                    pageIndicator.setup(galleryRecyclerView)
//
//                    Log.d(TAG, "GalleryView attached")
//                }
//                ListingItemType.UNKNOWN -> {
//                    appendUiForExternalOrRedditUrl()
//                }
//            }
        }

        addFooter()
    }

    fun recycleFullContent(
        fullContentContainerView: ViewGroup
    ): RecycledState = getState(fullContentContainerView, recycle = true)

    fun getState(
        fullContentContainerView: ViewGroup,
        recycle: Boolean = false
    ): RecycledState {
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
}