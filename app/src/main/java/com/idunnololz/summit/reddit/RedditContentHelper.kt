package com.idunnololz.summit.reddit

import android.content.Context
import android.net.Uri
import android.os.Build
import android.text.method.LinkMovementMethod
import android.util.Base64
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.*
import androidx.annotation.LayoutRes
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.PagerSnapHelper
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.android.material.card.MaterialCardView
import com.idunnololz.summit.R
import com.idunnololz.summit.offline.OfflineManager
import com.idunnololz.summit.preview.ImageViewerFragmentArgs
import com.idunnololz.summit.preview.VideoType
import com.idunnololz.summit.preview.VideoViewerFragmentArgs
import com.idunnololz.summit.reddit.ext.ListingItemType
import com.idunnololz.summit.reddit.ext.getType
import com.idunnololz.summit.reddit.ext.isDomainSelf
import com.idunnololz.summit.reddit_objects.GalleryData
import com.idunnololz.summit.reddit_objects.ListingItem
import com.idunnololz.summit.reddit_objects.MediaMetadata
import com.idunnololz.summit.reddit_objects.PreviewInfo
import com.idunnololz.summit.util.FileDownloadHelper
import com.idunnololz.summit.util.Size
import com.idunnololz.summit.util.Utils
import com.idunnololz.summit.util.ViewRecycler
import com.idunnololz.summit.util.ext.setup
import com.idunnololz.summit.video.ExoPlayerManager
import com.idunnololz.summit.video.VideoState
import com.idunnololz.summit.video.getVideoState
import com.idunnololz.summit.view.CustomPlayerView
import com.idunnololz.summit.view.GalleryPageIndicator
import com.idunnololz.summit.view.LoadingView
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import java.util.*

class RedditContentHelper(
    private val context: Context,
    private val fragment: Fragment,
    private val offlineManager: OfflineManager,
    private val exoPlayerManager: ExoPlayerManager,
    private val viewRecycler: ViewRecycler<View> = ViewRecycler<View>()
) {

    companion object {
        private const val TAG = "RedditContentHelper"
    }

    private val inflater = LayoutInflater.from(context)


    /**
     * @param lazyUpdate If true, content will not be refreshed. Only non content related things will be setup (eg. removed post warning)
     */
    fun setupFullContent(
        reveal: Boolean,
        tempSize: Size,
        videoViewMaxHeight: Int,
        fullImageViewTransitionName: String,
        listingItem: ListingItem,

        rootView: View,
        fullContentContainerView: ViewGroup,

        onFullImageViewClickListener: (imageView: ImageView?, url: String) -> Unit,
        onRevealContentClickedFn: () -> Unit,
        lazyUpdate: Boolean = false,
        videoState: VideoState? = null
    ) {
        if (!lazyUpdate) {
            fullContentContainerView.removeAllViews()
        }

        val context = rootView.context
        val targetListingItem = if (listingItem.crosspostParent != null) {
            listingItem.crosspostParentList?.first() ?: listingItem
        } else {
            listingItem
        }
        val postType = listingItem.getType()

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
                for (i in 1 until fullContentContainerView.childCount) {
                    fullContentContainerView.removeViewAt(1)
                }
            }

            if (listingItem.locked) {
                val postRemovedView = getView<View>(R.layout.full_content_post_locked_view)
                val cardView: MaterialCardView = postRemovedView.findViewById(R.id.cardView)
                val textView: TextView = postRemovedView.findViewById(R.id.text)

                postRemovedView.layoutParams =
                    (postRemovedView.layoutParams as ViewGroup.MarginLayoutParams).apply {
                        topMargin = context.resources.getDimensionPixelOffset(R.dimen.padding)
                    }

                textView.text = context.getString(
                    R.string.locked_post_message,
                    listingItem.subredditNamePrefixed
                )

                // workaround a cardview bug
                cardView.setContentPadding(0, 0, 0, 0)
            }

            //https://oauth.reddit.com/r/worldnews/comments/fhaezr/kim_jongun_has_fled_pyongyang_over_fears_of_the/
            // Append any extra information if needed
            when (listingItem.removedByCategory?.toLowerCase(Locale.US)) {
                "moderator" -> {
                    val postRemovedView = getView<View>(R.layout.full_content_post_removed_view)
                    val cardView: MaterialCardView = postRemovedView.findViewById(R.id.cardView)
                    val textView: TextView = postRemovedView.findViewById(R.id.text)

                    postRemovedView.layoutParams =
                        (postRemovedView.layoutParams as ViewGroup.MarginLayoutParams).apply {
                            topMargin = context.resources.getDimensionPixelOffset(R.dimen.padding)
                        }

                    textView.text = context.getString(
                        R.string.removed_by_mod_message,
                        listingItem.subredditNamePrefixed
                    )

                    // workaround a cardview bug
                    cardView.setContentPadding(0, 0, 0, 0)
                }
                "deleted" -> {
                    val postRemovedView = getView<View>(R.layout.full_content_post_removed_view)
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
                null -> {
                }
                else -> {
                    Log.d(TAG, "Unsupported removedByCategory: ${listingItem.removedByCategory}")
                }
            }
        }

        if (listingItem.shouldHideItem() && !reveal) {
            val fullContentHiddenView = getView<View>(R.layout.full_content_hidden_view)
            val fullImageView = fullContentHiddenView.findViewById<ImageView>(R.id.fullImage)
            val textView = fullContentHiddenView.findViewById<TextView>(R.id.message)
            val button = fullContentHiddenView.findViewById<Button>(R.id.button)

            val previewInfo: PreviewInfo? = listingItem.getLowestResHiddenPreviewInfo()
            RedditUtils.setImageViewSizeBasedOnPreview(
                context,
                previewInfo,
                rootView,
                fullImageView
            )

            if (!previewInfo?.getUrl().isNullOrBlank()) {
                offlineManager.fetchImage(rootView, checkNotNull(previewInfo).getUrl()) {
                    Glide.with(rootView)
                        .load(it)
                        .dontTransform()
                        .into(fullImageView)
                }
            } else {
                fullImageView.visibility = View.GONE
            }

            when {
                listingItem.over_18 -> {
                    textView.setText(R.string.reveal_warning_nsfw)
                }
                listingItem.spoiler -> {
                    textView.setText(R.string.reveal_warning_spoiler)
                }
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
            val previewInfo: PreviewInfo? = listingItem.getPreviewInfo()
                ?: listingItem.getThumbnailPreviewInfo()
            RedditUtils.setImageViewSizeBasedOnPreview(context, previewInfo, rootView, imageView)

            imageView.setImageResource(0)

            if (!previewInfo?.getUrl().isNullOrBlank()) {
                val url = checkNotNull(previewInfo).getUrl()
                offlineManager.fetchImage(rootView, url) {
                    offlineManager.calculateImageMaxSizeIfNeeded(it)
                    offlineManager.getMaxImageSizeHint(it, tempSize)

                    Glide.with(rootView)
                        .load(it)
                        .dontTransform()
                        .apply {
                            if (tempSize.width > 0) {
                                override(tempSize.width, tempSize.height)
                            }
                        }
                        .into(imageView)
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

        fun appendUiForExternalOrRedditUrl() {
            val externalContentView =
                getView<View>(R.layout.full_content_external_content_view)
            val thumbnailView = externalContentView.findViewById<ImageView>(R.id.thumbnail)
            val externalContentTextView =
                externalContentView.findViewById<TextView>(R.id.externalContentText)

            loadPreviewInfo(thumbnailView)

            externalContentTextView.text = targetListingItem.domain
            externalContentView.setOnClickListener {
                val uri = Uri.parse(targetListingItem.url)
                if (RedditUtils.isUriReddit(uri)) {
                    RedditUtils.openRedditUrl(context, targetListingItem.url)
                    /*

						"media_metadata": {
							"gil6s4xwi4g51": {
								"status": "valid",
								"e": "Image",
								"m": "image/jpg",
								"p": [{
										"y": 216,
										"x": 108,
										"u": "https://preview.redd.it/gil6s4xwi4g51.jpg?width=108&amp;crop=smart&amp;auto=webp&amp;s=409020cc102c40460477dcafc2d5e662ef20de9a"
									}, {
										"y": 432,
										"x": 216,
										"u": "https://preview.redd.it/gil6s4xwi4g51.jpg?width=216&amp;crop=smart&amp;auto=webp&amp;s=b4b7dd2a5c1c63c22fb922437bd19ef7af39a766"
									}, {
										"y": 640,
										"x": 320,
										"u": "https://preview.redd.it/gil6s4xwi4g51.jpg?width=320&amp;crop=smart&amp;auto=webp&amp;s=26119cd70f31ddd328f28fdc499d5798c8737ab6"
									}
								],
								"s": {
									"y": 780,
									"x": 360,
									"u": "https://preview.redd.it/gil6s4xwi4g51.jpg?width=360&amp;format=pjpg&amp;auto=webp&amp;s=dfce79ae37bd468338653a414e995425ab6c836f"
								},
								"id": "gil6s4xwi4g51"
							},
							"0kj9a4xwi4g51": {
								"status": "valid",
								"e": "Image",
								"m": "image/jpg",
								"p": [{
										"y": 81,
										"x": 108,
										"u": "https://preview.redd.it/0kj9a4xwi4g51.jpg?width=108&amp;crop=smart&amp;auto=webp&amp;s=a42c4ea34915f9511349d7f921fd89d3f7274676"
									}, {
										"y": 162,
										"x": 216,
										"u": "https://preview.redd.it/0kj9a4xwi4g51.jpg?width=216&amp;crop=smart&amp;auto=webp&amp;s=c5d44908fb71570af50c33f142fdf911a66eba32"
									}, {
										"y": 240,
										"x": 320,
										"u": "https://preview.redd.it/0kj9a4xwi4g51.jpg?width=320&amp;crop=smart&amp;auto=webp&amp;s=d05c4a9f25f0319b6c0456e89394e50f8db05339"
									}
								],
								"s": {
									"y": 360,
									"x": 480,
									"u": "https://preview.redd.it/0kj9a4xwi4g51.jpg?width=480&amp;format=pjpg&amp;auto=webp&amp;s=032ecbf8a26f89c61ad2eb85e43b0ee4232e9055"
								},
								"id": "0kj9a4xwi4g51"
							}
						},
                     */
                } else if (uri.path?.endsWith(".jpg") == true || uri.path?.endsWith(".png") == true) {
                    val args = ImageViewerFragmentArgs(
                        null,
                        targetListingItem.url,
                        null
                    )
                    fragment.findNavController()
                        .navigate(R.id.imageViewerFragment, args.toBundle())
                } else if (uri.path?.endsWith(".gifv") == true) {
                    val args = VideoViewerFragmentArgs(
                        url = targetListingItem.url,
                        videoType = VideoType.UNKNOWN,
                        videoState = customPlayerView?.getVideoState()
                    )
                    fragment.findNavController()
                        .navigate(R.id.videoViewerFragment, args.toBundle())
                } else if (uri.host == "gfycat.com" && targetListingItem.media?.oembed != null) {
                    val keyLowerCase = uri.path?.substring(1)?.split("-")?.get(0) ?: ""
                    val url = targetListingItem.media.oembed.thumbnailUrl
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
                        Utils.openExternalLink(context, targetListingItem.url)
                    }
                } else if (uri.host == "imgur.com") {
                    val args = ImageViewerFragmentArgs(
                        null,
                        targetListingItem.url,
                        null
                    )
                    fragment.findNavController()
                        .navigate(R.id.imageViewerFragment, args.toBundle())
                } else {
                    Utils.openExternalLink(context, targetListingItem.url)
                }
            }
            externalContentView.setOnLongClickListener {
                val popupMenu = PopupMenu(context, it)
                popupMenu.inflate(R.menu.menu_url)
                popupMenu.setOnMenuItemClickListener { item ->
                    when (item.itemId) {
                        R.id.copy_link -> {
                            Utils.copyToClipboard(context, targetListingItem.url)
                            true
                        }
                        R.id.share_link -> {
                            Utils.shareText(context, targetListingItem.url)
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
            when (postType) {
                ListingItemType.DEFAULT_SELF -> {
                    if (listingItem.selftextHtml == null) {
                        // do nothing...
                    } else if (RedditUtils.needsWebView(listingItem.selftextHtml)) {
                        val fullTextView = getView<View>(R.layout.full_content_web_view)
                        val fullImageView = fullTextView.findViewById<ImageView>(R.id.fullImage)
                        val webView: WebView = fullTextView.findViewById(R.id.webView)
                        val textColorHex = String.format(
                            "#%06X",
                            (0xFFFFFF and ContextCompat.getColor(context, R.color.colorText))
                        )
                        val bgColorHex = String.format(
                            "#%06X",
                            (0xFFFFFF and ContextCompat.getColor(context, R.color.colorBackground))
                        )
                        val aLinkColorHex = String.format(
                            "#%06X",
                            (0xFFFFFF and ContextCompat.getColor(context, R.color.colorLink))
                        )
                        val cssText = "<head><style type=\"text/css\">" +
                                "body{margin:0;padding:0;color:${textColorHex};background-color:${bgColorHex};}" +
                                "a{color:${aLinkColorHex}}" +
                                "td{border-bottom:1px solid ${textColorHex};}" +
                                "table{border-collapse:collapse;}" +
                                "</style></head>"

                        if (Build.VERSION.SDK_INT >= 19) {
                            webView.setLayerType(View.LAYER_TYPE_HARDWARE, null);
                        } else {
                            webView.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
                        }

                        webView.settings.apply {
                            javaScriptEnabled = false
                        }

                        webView.setBackgroundColor(0) // transparent

                        // Need to base 64 encode html due to stupid bug...
                        val html =
                            "<html>$cssText<body>${Utils.fromHtml(targetListingItem.selftextHtml)}</body></html>"
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
                        loadPreviewInfo(fullImageView, attachClickHandler = true)
                    } else {
                        val fullTextView = getView<View>(R.layout.full_content_text_view)
                        val fullImageView = fullTextView.findViewById<ImageView>(R.id.fullImage)
                        val bodyTextView: TextView = fullTextView.findViewById(R.id.body)
                        bodyTextView.visibility = View.VISIBLE
                        bodyTextView.text =
                            RedditUtils.formatBodyText(
                                context,
                                listingItem.selftextHtml
                            )
                        bodyTextView.movementMethod = LinkMovementMethod.getInstance()
                        loadPreviewInfo(fullImageView, attachClickHandler = true)
                    }

                    if (!targetListingItem.isDomainSelf()) {
                        appendUiForExternalOrRedditUrl()
                    }
                }
                ListingItemType.REDDIT_IMAGE -> {
                    val fullContentImageView = getView<View>(R.layout.full_content_image_view)
                    val fullImageView = fullContentImageView.findViewById<ImageView>(R.id.fullImage)
                    val loadingView =
                        fullContentImageView.findViewById<LoadingView>(R.id.loadingView)

                    fullImageView.setImageResource(0)

                    fun fetchFullImage() {
                        loadingView?.showProgressBar()
                        val url = targetListingItem.url
                        offlineManager.fetchImageWithError(rootView, url, b@{
                            if (!fragment.isAdded || fragment.context == null) {
                                return@b
                            }

                            loadingView?.hideAll(animate = false)
                            offlineManager.calculateImageMaxSizeIfNeeded(it)
                            offlineManager.getMaxImageSizeHint(it, tempSize)

                            Glide.with(fragment)
                                .load(it)
                                .apply {
                                    if (tempSize.width > 0 && tempSize.height > 0) {
                                        override(tempSize.width, tempSize.height)
                                    }
                                }
                                .into(fullImageView)
                        }, {
                            loadingView?.showDefaultErrorMessageFor(it)
                        })
                    }

                    // try to guess image size...
                    if (!offlineManager.hasImageSizeHint(targetListingItem.url)) {
                        var width = 0
                        var height = 0
                        targetListingItem.preview?.images?.first()?.source?.let {
                            width = it.width
                            height = it.height
                        }
                        rootView.measure(
                            View.MeasureSpec.makeMeasureSpec(
                                Utils.getScreenWidth(context),
                                View.MeasureSpec.EXACTLY
                            ),
                            View.MeasureSpec.makeMeasureSpec(
                                Utils.getScreenHeight(context),
                                View.MeasureSpec.AT_MOST
                            )
                        )
                        if (width != 0 && height != 0) {
                            val thumbnailHeight =
                                (fullImageView.measuredWidth * (height.toDouble() / width)).toInt()
                            offlineManager.setImageSizeHint(
                                targetListingItem.url,
                                fullImageView.measuredWidth,
                                thumbnailHeight
                            )
                        }
                    }

                    offlineManager.getImageSizeHint(targetListingItem.url, tempSize)
                    if (tempSize.width > 0 && tempSize.height > 0) {
                        fullImageView.layoutParams = fullImageView.layoutParams.apply {
                            this.width = tempSize.width
                            this.height = tempSize.height
                        }
                    }

                    loadingView?.setOnRefreshClickListener {
                        fetchFullImage()
                    }
                    fetchFullImage()

                    //ViewCompat.setTransitionName(fullImageView, fullImageViewTransitionName)
                    fullImageView.setOnClickListener {
                        onFullImageViewClickListener(null, targetListingItem.url)
                    }
                }
                ListingItemType.REDDIT_VIDEO -> {
                    val containerView = getView<View>(R.layout.full_content_video_view)
                    val playerView = containerView.findViewById<CustomPlayerView>(R.id.playerView)

                    customPlayerView = playerView

                    val media = targetListingItem.media
                    if (media?.redditVideo != null) {
                        val bestSize = RedditUtils.calculateBestVideoSize(
                            context,
                            media.redditVideo,
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
                                    url = media.redditVideo.dashUrl,
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
                            media.redditVideo.dashUrl,
                            VideoType.DASH,
                            videoState = videoState
                        )
                    } else {
                        playerView.visibility = View.GONE
                    }
                }
                ListingItemType.REDDIT_GALLERY -> {
                    val fullContentGalleryView = getView<View>(R.layout.full_content_gallery_view)
                    val galleryRecyclerView =
                        fullContentGalleryView.findViewById<RecyclerView>(R.id.galleryRecyclerView)
                    val loadingView =
                        fullContentGalleryView.findViewById<LoadingView>(R.id.loadingView)
                    val pageIndicator =
                        fullContentGalleryView.findViewById<GalleryPageIndicator>(R.id.pageIndicator)

                    galleryRecyclerView.adapter =
                        GalleryAdapter(
                            context,
                            requireNotNull(listingItem.mediaMetadata),
                            requireNotNull(listingItem.galleryData),
                            fragment,
                            onFullImageViewClickListener
                        )
                    galleryRecyclerView.layoutManager =
                        LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
                    galleryRecyclerView.setHasFixedSize(true)

                    if (galleryRecyclerView.onFlingListener == null) {
                        // Check if PagerSnapHelper is already attached
                        PagerSnapHelper().attachToRecyclerView(galleryRecyclerView)
                    }

                    pageIndicator.setup(galleryRecyclerView)

                    Log.d(TAG, "GalleryView attached")
                }
                ListingItemType.UNKNOWN -> {
                    appendUiForExternalOrRedditUrl()
                }
            }
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

data class RecycledState(
    val videoState: VideoState?
) {
    data class Builder(
        var videoState: VideoState? = null
    ) {
        fun setVideoState(videoState: VideoState?) = apply {
            this.videoState = videoState
        }

        fun build() = RecycledState(videoState = videoState)
    }
}

private class GalleryItemViewHolder(v: View) : RecyclerView.ViewHolder(v)

class GalleryAdapter(
    context: Context,
    values: Map<String, MediaMetadata>,
    galleryData: GalleryData,
    private val fragment: Fragment,
    private val onFullImageViewClickListener: (imageView: ImageView?, url: String) -> Unit,
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val inflater = LayoutInflater.from(context)

    private val items: List<MediaMetadata>

    init {
        val newItems = mutableListOf<MediaMetadata>()
        galleryData.items.forEach {
            values[it.mediaId]?.let {
                newItems.add(it)
            }
        }
        items = newItems
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val v = inflater.inflate(R.layout.gallery_item, parent, false)
        return GalleryItemViewHolder(v)
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val imageView = holder.itemView as ImageView
        val item = items[position]
        val url = item.s?.getUrl()

        Glide.with(fragment)
            .load(url)
            .apply {
                val width = item.s?.x ?: 0
                val height = item.s?.y ?: 0

                val imageViewWidth = Utils.getScreenHeight(imageView.context)
                val thumbnailHeight =
                    (imageViewWidth * (height.toDouble() / width)).toInt()

                if (width > 0 && thumbnailHeight > 0) {
                    override(width, thumbnailHeight)
                }
            }
            .into(imageView)
        if (url == null) {
            imageView.setOnClickListener(null)
        } else {
            imageView.setOnClickListener {
                onFullImageViewClickListener(null, url)
            }
        }
    }

    override fun getItemCount(): Int = items.size

}