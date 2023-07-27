package com.idunnololz.summit.util

import android.content.Context
import android.icu.text.CompactDecimalFormat
import android.os.Build
import com.idunnololz.summit.R
import org.threeten.bp.Instant
import java.text.NumberFormat
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
}

private const val SECOND_MILLIS: Long = 1000
private const val MINUTE_MILLIS = 60 * SECOND_MILLIS
private const val HOUR_MILLIS = 60 * MINUTE_MILLIS
private const val DAY_MILLIS = 24 * HOUR_MILLIS
private const val MONTH_MILLIS = 30 * DAY_MILLIS
private const val YEAR_MILLIS = 12 * MONTH_MILLIS

fun dateStringToPretty(context: Context, dateStr: String): String {
    val ts = dateStringToTs(dateStr)
    val now = System.currentTimeMillis()
    val diff: Long = now - ts

    return if (diff < MINUTE_MILLIS) {
        context.getString(R.string.elapsed_time_just_now)
    } else if (diff < 2 * MINUTE_MILLIS) {
        context.getString(R.string.elapsed_time_a_minute_ago)
    } else if (diff < 50 * MINUTE_MILLIS) {
        context.getString(R.string.elapsed_time_minutes_ago, diff / MINUTE_MILLIS)
    } else if (diff < 120 * MINUTE_MILLIS) {
        context.getString(R.string.elapsed_time_an_hour_ago)
    } else if (diff < 24 * HOUR_MILLIS) {
        context.getString(R.string.elapsed_time_hours_ago, diff / HOUR_MILLIS)
    } else if (diff < 48 * HOUR_MILLIS) {
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
}

fun dateStringToTs(dateString: String): Long =
    Instant.parse(
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
