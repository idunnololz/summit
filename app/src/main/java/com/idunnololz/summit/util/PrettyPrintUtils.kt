package com.idunnololz.summit.util

import android.icu.text.CompactDecimalFormat
import android.os.Build
import android.text.format.DateUtils
import org.threeten.bp.Instant
import java.util.Locale

fun dateStringToPretty(dateStr: String, includeAgo: Boolean = false): CharSequence? {
    return DateUtils.getRelativeTimeSpanString(
        Instant.parse(dateStr + "Z").toEpochMilli(),
        System.currentTimeMillis(),
        DateUtils.SECOND_IN_MILLIS,
        DateUtils.FORMAT_ABBREV_RELATIVE
    )
}

fun dateStringToTs(dateString: String): Long =
    Instant.parse(dateString + "Z").toEpochMilli()

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