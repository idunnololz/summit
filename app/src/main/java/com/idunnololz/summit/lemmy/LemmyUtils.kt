package com.idunnololz.summit.lemmy

import android.content.Context
import android.graphics.Point
import android.icu.text.CompactDecimalFormat
import android.icu.text.DecimalFormat
import android.net.Uri
import android.os.Build
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.ForegroundColorSpan
import androidx.core.content.ContextCompat
import com.idunnololz.summit.R
import com.idunnololz.summit.api.dto.Person
import com.idunnololz.summit.api.dto.SearchType
import com.idunnololz.summit.util.NumberFormatUtil
import com.idunnololz.summit.util.Size
import com.idunnololz.summit.util.Utils
import com.idunnololz.summit.util.dateStringToTs
import com.idunnololz.summit.util.ext.appendLink
import com.idunnololz.summit.video.VideoSizeHint
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.Period
import java.time.ZoneOffset
import java.util.Locale

object LemmyUtils {

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

fun Person.getAccountAgeString(): String {
    val ts = dateStringToTs(published)
    val accountCreationTime = LocalDateTime
        .ofEpochSecond(ts / 1000, 0, ZoneOffset.UTC)
        .toLocalDate()
    val period = Period.between(accountCreationTime, LocalDate.now())

    val years = period.years
    val months = period.months
    val days = period.days

    return buildString {
        if (years > 0) {
            append(years)
            append("y ")
        }
        if (months > 0) {
            append(months)
            append("m ")
        }
        if (days > 0 && years == 0) {
            append(days)
            append("d ")
        }
    }.trim()
}