package com.idunnololz.summit.lemmy

import android.content.Context
import android.content.Intent
import android.graphics.Point
import android.icu.text.CompactDecimalFormat
import android.icu.text.DecimalFormat
import android.net.Uri
import android.os.Build
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.ForegroundColorSpan
import android.view.View
import android.widget.ImageView
import androidx.core.content.ContextCompat
import com.idunnololz.summit.R
import com.idunnololz.summit.api.dto.SearchType
import com.idunnololz.summit.main.MainActivity
import com.idunnololz.summit.util.NumberFormatUtil
import com.idunnololz.summit.util.PreviewInfo
import com.idunnololz.summit.util.Size
import com.idunnololz.summit.util.Utils
import com.idunnololz.summit.util.ext.appendLink
import com.idunnololz.summit.video.VideoSizeHint
import java.util.Locale
import java.util.regex.Pattern

object LemmyUtils {

    private val TAG = LemmyUtils::class.java.canonicalName

    private val GIPHY_REGEX = Pattern.compile("\\(giphy\\|([^\\s]*)\\)")

    private var compactDecimalFormat: DecimalFormat? = null

    fun formatAuthor(author: String): String = "@%s".format(author)

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
        videoSizeHint: VideoSizeHint,
        availableW: Int = Utils.getScreenWidth(context) - context.resources.getDimensionPixelOffset(
            R.dimen.padding_half,
        ) * 2,
        availableH: Int = Utils.getScreenHeight(context),
    ): Point {
        val w = videoSizeHint.width
        val h = videoSizeHint.height

        // find limiting factor
        val scale = availableW.toDouble() / w
        val scaledH = h * scale

        return if (scaledH > availableH) {
            Point((availableH.toDouble() / h * w).toInt(), availableH)
        } else {
            Point(availableW, scaledH.toInt())
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

    fun cleanUrl(url: String, desiredFormat: String = ""): String {
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
        }.build().toString()
    }

    fun isUrlRedirect(url: String): Boolean = isUriRedirect(Uri.parse(url))

    fun isUriRedirect(uri: Uri): Boolean = uri.host == "redd.it"

    fun isUriGallery(uri: Uri): Boolean = uri.pathSegments[0] == "gallery"

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

fun Int.toLemmyPageIndex() = this + 1 // lemmy pages are 1 indexed

fun SearchType.toLocalizedString(context: Context) = when (this) {
    SearchType.All -> context.getString(R.string.all)
    SearchType.Comments -> context.getString(R.string.comments)
    SearchType.Posts -> context.getString(R.string.posts)
    SearchType.Communities -> context.getString(R.string.communities)
    SearchType.Users -> context.getString(R.string.users)
    SearchType.Url -> context.getString(R.string.urls)
}

fun SpannableStringBuilder.appendNameWithInstance(
    context: Context,
    name: String,
    instance: String,
    url: String? = null,
) {
    val text = "$name@$instance"
    if (url != null) {
        appendLink(
            text = text,
            url = url,
            underline = false,
        )
    } else {
        append(text)
    }
    val end = length
    setSpan(
        ForegroundColorSpan(ContextCompat.getColor(context, R.color.colorTextFaint)),
        end - instance.length - 1,
        end,
        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE,
    )
}
