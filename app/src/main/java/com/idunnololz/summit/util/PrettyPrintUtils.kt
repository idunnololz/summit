package com.idunnololz.summit.util

import android.icu.text.CompactDecimalFormat
import android.os.Build
import android.text.format.DateUtils
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

fun dateStringToPretty(dateStr: String, includeAgo: Boolean = false): CharSequence? =
    try {
        DateUtils.getRelativeTimeSpanString(
            Instant.parse(if (dateStr.endsWith("Z")) {
                dateStr
            } else {
                dateStr + "Z"
            }).toEpochMilli(),
            System.currentTimeMillis(),
            DateUtils.SECOND_IN_MILLIS,
            DateUtils.FORMAT_ABBREV_RELATIVE
        )
    } catch (e: Exception) {
        null
    }

fun dateStringToTs(dateString: String): Long =
    Instant.parse(if (dateString.endsWith("Z")) {
        dateString
    } else {
        dateString + "Z"
    }).toEpochMilli()

fun abbrevNumber(number: Long): String {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        val formatter = CompactDecimalFormat.getInstance(
            Locale.getDefault(), CompactDecimalFormat.CompactStyle.SHORT
        );

        formatter.format(number)
    } else {
        NumberFormatUtil.format(number)
    }
}