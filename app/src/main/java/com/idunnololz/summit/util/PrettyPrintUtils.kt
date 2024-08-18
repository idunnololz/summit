package com.idunnololz.summit.util

import android.content.Context
import android.icu.text.CompactDecimalFormat
import android.os.Build
import com.idunnololz.summit.R
import java.text.NumberFormat
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

object PrettyPrintUtils {

    private val df = NumberFormat.getNumberInstance().apply {
        this.maximumFractionDigits = 1
    }
    private val pf = NumberFormat.getPercentInstance().apply {
        maximumFractionDigits = 2
    }

    val defaultDecimalFormat: NumberFormat
        get() = df

    val defaultPercentFormat: NumberFormat
        get() = pf

    val defaultShortPercentFormat: NumberFormat
        get() = NumberFormat.getPercentInstance().apply {
            maximumFractionDigits = 0
        }
}

private const val SECOND_MILLIS: Long = 1000
private const val MINUTE_MILLIS = 60 * SECOND_MILLIS
private const val HOUR_MILLIS = 60 * MINUTE_MILLIS
private const val DAY_MILLIS = 24 * HOUR_MILLIS
private const val MONTH_MILLIS = 30 * DAY_MILLIS
private const val YEAR_MILLIS = 12 * MONTH_MILLIS

object PrettyPrintStyles {
    const val SHORT = 1

    /**
     * Short if it's a "recent date" or long if it's older.
     */
    const val SHORT_DYNAMIC = 2
}

fun dateStringToPretty(context: Context, dateStr: String): String =
    dateStringToPretty(context, dateStringToTs(dateStr))

fun dateStringToPretty(
    context: Context,
    ts: Long,
    style: Int = PrettyPrintStyles.SHORT
): String {

    val now = System.currentTimeMillis()
    val diff: Long = now - ts
    var isRecent = false

    val shortDate = if (diff < MINUTE_MILLIS) {
        isRecent = true
        context.getString(R.string.elapsed_time_just_now)
    } else if (diff < 2 * MINUTE_MILLIS) {
        isRecent = true
        context.getString(R.string.elapsed_time_a_minute_ago)
    } else if (diff < 50 * MINUTE_MILLIS) {
        isRecent = true
        context.getString(R.string.elapsed_time_minutes_ago, diff / MINUTE_MILLIS)
    } else if (diff < 120 * MINUTE_MILLIS) {
        isRecent = true
        context.getString(R.string.elapsed_time_an_hour_ago)
    } else if (diff < 24 * HOUR_MILLIS) {
        isRecent = true
        context.getString(R.string.elapsed_time_hours_ago, diff / HOUR_MILLIS)
    } else if (diff < 48 * HOUR_MILLIS) {
        isRecent = true
        context.getString(R.string.elapsed_time_yesterday)
    } else if (diff < MONTH_MILLIS) {
        context.getString(R.string.elapsed_time_days_ago, diff / DAY_MILLIS)
    } else if (diff < 2 * MONTH_MILLIS) {
        context.getString(R.string.elapsed_time_a_month_ago)
    } else if (diff < YEAR_MILLIS) {
        context.getString(R.string.elapsed_time_months_ago, diff / MONTH_MILLIS)
    } else if (diff < 2 * YEAR_MILLIS) {
        context.getString(R.string.elapsed_time_a_year_ago)
    } else {
        context.getString(R.string.elapsed_time_years_ago, diff / YEAR_MILLIS)
    }

    return if (style == PrettyPrintStyles.SHORT) {
        shortDate
    } else if (style == PrettyPrintStyles.SHORT_DYNAMIC && !isRecent) {
        "$shortDate (${tsToShortDate(ts)})"
    } else {
        shortDate
    }
}

fun dateStringToFullDateTime(dateStr: String): String {
    return tsToFullDateTime(dateStringToTs(dateStr))
}

fun tsToFullDateTime(ts: Long): String {
    val epochSecond = ts / 1000
    val localDateTime = LocalDateTime.ofEpochSecond(epochSecond, 0, ZoneOffset.UTC)

    return localDateTime.format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM))
}

fun tsToShortDate(ts: Long): String {
    val epochSecond = ts / 1000
    val localDateTime = LocalDateTime.ofEpochSecond(epochSecond, 0, ZoneOffset.UTC)

    return localDateTime.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT))
}

fun dateStringToTs(dateString: String): Long = Instant.parse(
    if (dateString.endsWith("Z")) {
        dateString
    } else {
        dateString + "Z"
    },
).toEpochMilli()

fun abbrevNumber(number: Long): String {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        val formatter = CompactDecimalFormat.getInstance(
            Locale.getDefault(),
            CompactDecimalFormat.CompactStyle.SHORT,
        )

        formatter.format(number)
    } else {
        NumberFormatUtil.format(number)
    }
}
