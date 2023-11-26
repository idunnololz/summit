package com.idunnololz.summit.lemmy

import android.content.Context
import android.content.Intent
import android.graphics.Point
import android.icu.text.CompactDecimalFormat
import android.icu.text.DecimalFormat
import android.net.Uri
import android.os.Build
import android.view.View
import android.widget.*
import com.idunnololz.summit.R
import com.idunnololz.summit.main.MainActivity
import com.idunnololz.summit.util.*
import com.idunnololz.summit.video.VideoSizeHint
import java.util.*
import java.util.regex.Pattern

object LemmyUtils {

    private val TAG = LemmyUtils::class.java.canonicalName

    private val SUBREDDIT_NAME_MAX_LENGTH = 20

    private val GIPHY_REGEX = Pattern.compile("\\(giphy\\|([^\\s]*)\\)")

    private var compactDecimalFormat: DecimalFormat? = null

    fun formatAuthor(author: String): String = "u/%s".format(author)

    fun abbrevNumber(number: Long?): String {
        if (number == null) {
            return "⬤"
        }
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            val formatter = compactDecimalFormat ?: CompactDecimalFormat.getInstance(
                Locale.getDefault(),
                CompactDecimalFormat.CompactStyle.SHORT,
            ).also {
                compactDecimalFormat = it
            }

            formatter.format(number)
        } else {
            NumberFormatUtil.format(number)
        }
    }

    fun calculateBestVideoSize(
        context: Context,
        redditVideo: VideoSizeHint,
        availableW: Int = Utils.getScreenWidth(context) - context.resources.getDimensionPixelOffset(
            R.dimen.padding_half,
        ) * 2,
        availableH: Int = Utils.getScreenHeight(context),
    ): Point {
        val w = redditVideo.width
        val h = redditVideo.height

        // find limiting factor
        val scale = availableW.toDouble() / w
        val scaledH = h * scale
        if (scaledH > availableH) {
            return Point((availableH.toDouble() / h * w).toInt(), availableH)
        } else {
            return Point(availableW, scaledH.toInt())
        }
    }

    fun calculateMaxImagePreviewSize(
        context: Context,
        imageWidth: Int,
        imageHeight: Int,
        availableW: Int = Utils.getScreenWidth(context),
        availableH: Int = Utils.getScreenHeight(context),
    ): Size {
        if (availableW > imageWidth && availableH > imageHeight) {
            return Size(imageWidth, imageHeight)
        }

        val w = imageWidth
        val h = imageHeight

        // find limiting factor
        val scale = availableW.toDouble() / w
        val scaledH = h * scale
        if (scaledH > availableH) {
            return Size((availableH.toDouble() / h * w).toInt(), availableH)
        } else {
            return Size(availableW, scaledH.toInt())
        }
    }

    fun needsWebView(html: String): Boolean = html.contains("&lt;table&gt;")

    fun isUrlAGif(url: String): Boolean = url.endsWith(".gif", ignoreCase = true)

    fun setImageViewSizeBasedOnPreview(
        context: Context,
        previewInfo: PreviewInfo?,
        rootView: View,
        imageView: ImageView,
    ) {
        previewInfo ?: return
        val width = previewInfo.width
        val height = previewInfo.height
        val screenSize = Utils.getScreenSize(context)

        rootView.measure(
            View.MeasureSpec.makeMeasureSpec(screenSize.x, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(screenSize.y, View.MeasureSpec.AT_MOST),
        )
        if (width != 0 && height != 0) {
            val thumbnailHeight = (imageView.measuredWidth * (height.toDouble() / width)).toInt()
            imageView.layoutParams = imageView.layoutParams.apply {
                this.width = imageView.measuredWidth
                this.height = thumbnailHeight
            }
        }
    }

    fun convertRedditUrl(url: String, desiredFormat: String = "", sharable: Boolean): String {
        val uri = Uri.parse(url)
        val path = uri.path ?: ""
        val cleanPath = if (path.endsWith(".xml")) {
            path.substring(0, path.lastIndexOf(".xml") - 1)
        } else if (path.endsWith(".json")) {
            path.substring(0, path.lastIndexOf(".json") - 1)
        } else {
            path
        }

        return uri.buildUpon().apply {
            path("$cleanPath$desiredFormat")

            if (uri.authority == "oauth.reddit.com" && sharable) {
                authority("www.reddit.com")
            } else if (uri.authority == "amp.reddit.com") {
                authority("www.reddit.com")
            }
        }.build().toString()
    }

    fun openRedditUrl(context: Context, url: String) {
        context.startActivity(
            Intent(context, MainActivity::class.java)
                .setData(Uri.parse(url)),
        )
    }

    fun isUrlReddit(url: String): Boolean =
        isUriReddit(Uri.parse(url))

    fun isUriReddit(uri: Uri): Boolean =
        uri.host == "www.reddit.com" ||
            uri.host == "reddit.com" ||
            uri.host == "oauth.reddit.com" ||
            uri.host == "redd.it" ||
            uri.host == "amp.reddit.com"

    fun isUrlRedirect(url: String): Boolean =
        isUriRedirect(Uri.parse(url))

    fun isUriRedirect(uri: Uri): Boolean =
        uri.host == "redd.it"

    fun isUriGallery(uri: Uri): Boolean =
        uri.pathSegments[0] == "gallery"

    fun findGiphyLinks(s: String): List<String> {
        val matcher = GIPHY_REGEX.matcher(s)
        val matches = mutableListOf<String>()
        while (matcher.find()) {
            val g1 = matcher.group(1)
            if (g1 != null) {
                matches.add(g1)
            }
        }
        return matches
    }
}

fun Int.toLemmyPageIndex() =
    this + 1 // lemmy pages are 1 indexed
