package com.idunnololz.summit.util

import android.annotation.SuppressLint
import android.content.Context
import android.icu.text.CompactDecimalFormat
import android.os.Build
import com.idunnololz.summit.R
import java.text.CharacterIterator
import java.text.NumberFormat
import java.text.StringCharacterIterator
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

    fun humanReadableByteCountSi(bytes: Long): String {
        var bytesLeft = bytes
        if (-1000 < bytesLeft && bytesLeft < 1000) {
            return "$bytesLeft B"
        }
        val ci: CharacterIterator = StringCharacterIterator("kMGTPE")
        while (bytesLeft <= -999950 || bytesLeft >= 999950) {
            bytesLeft /= 1000
            ci.next()
        }
        return String.format(Locale.US, "%.1f %cB", bytesLeft / 1000.0, ci.current())
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

fun tsToConcise(context: Context, dateStr: String): String =
    tsToConcise(context, dateStringToTs(dateStr))

fun tsToConcise(context: Context, ts: Long, style: Int = PrettyPrintStyles.SHORT): String {
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

fun relativeTimeToConcise(context: Context, diffMs: Long): String {
    val shortDate = if (diffMs < MINUTE_MILLIS) {
        context.getString(R.string.elapsed_time_just_now)
    } else if (diffMs < 2 * MINUTE_MILLIS) {
        context.getString(R.string.elapsed_time_a_minute_ago)
    } else if (diffMs < 50 * MINUTE_MILLIS) {
        context.getString(R.string.elapsed_time_minutes_ago, diffMs / MINUTE_MILLIS)
    } else if (diffMs < 120 * MINUTE_MILLIS) {
        context.getString(R.string.elapsed_time_an_hour_ago)
    } else if (diffMs < 24 * HOUR_MILLIS) {
        context.getString(R.string.elapsed_time_hours_ago, diffMs / HOUR_MILLIS)
    } else if (diffMs < 48 * HOUR_MILLIS) {
        context.getString(R.string.elapsed_time_yesterday)
    } else if (diffMs < MONTH_MILLIS) {
        context.getString(R.string.elapsed_time_days_ago, diffMs / DAY_MILLIS)
    } else if (diffMs < 2 * MONTH_MILLIS) {
        context.getString(R.string.elapsed_time_a_month_ago)
    } else if (diffMs < YEAR_MILLIS) {
        context.getString(R.string.elapsed_time_months_ago, diffMs / MONTH_MILLIS)
    } else if (diffMs < 2 * YEAR_MILLIS) {
        context.getString(R.string.elapsed_time_a_year_ago)
    } else {
        context.getString(R.string.elapsed_time_years_ago, diffMs / YEAR_MILLIS)
    }

    return shortDate
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

@SuppressLint("DefaultLocale")
fun durationToPretty(durationMs: Long): String {
    val s = durationMs / 1000
    if (s > 3600) {
        return String.format("%dh%02dm%02ds", s / 3600, (s % 3600) / 60, (s % 60))
    } else if (s > 60) {
        return String.format("%dm%02ds", (s % 3600) / 60, (s % 60))
    } else {
        return String.format("%ds", (s % 60))
    }
}

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
