package com.idunnololz.summit.image

import androidx.exifinterface.media.ExifInterface
import java.text.ParsePosition
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import java.util.regex.Pattern
import kotlin.math.min

object ExifInterfaceUtils {
    private val sFormatterPrimary: SimpleDateFormat =
        SimpleDateFormat("yyyy:MM:dd HH:mm:ss", Locale.US)
    private val sFormatterSecondary: SimpleDateFormat =
        SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)

    private val NON_ZERO_TIME_PATTERN = Pattern.compile(".*[1-9].*")

    init {
        sFormatterPrimary.timeZone = TimeZone.getTimeZone("UTC")
        sFormatterSecondary.timeZone = TimeZone.getTimeZone("UTC")
    }

    fun parseDateTime(
        dateTimeString: String?, subSecs: String?,
        offsetString: String?,
    ): Long? {
        if (dateTimeString == null || !NON_ZERO_TIME_PATTERN.matcher(dateTimeString)
                .matches()
        ) {
            return null
        }
        val pos = ParsePosition(0)
        return try {
            // The exif field is in local time. Parsing it as if it is UTC will yield time
            // since 1/1/1970 local time
            var dateTime = sFormatterPrimary.parse(dateTimeString, pos)
            if (dateTime == null) {
                dateTime = sFormatterSecondary.parse(dateTimeString, pos)
                if (dateTime == null) {
                    return null
                }
            }
            var msecs = dateTime.time
            if (offsetString != null) {
                val sign = offsetString.substring(0, 1)
                val hour = offsetString.substring(1, 3).toInt()
                val min = offsetString.substring(4, 6).toInt()
                if (("+" == sign || "-" == sign) && ":" == offsetString.substring(
                        3,
                        4,
                    ) && hour <= 14 /* max UTC hour value */) {
                    msecs += ((hour * 60 + min) * 60 * 1000 * if ("-" == sign) 1 else -1).toLong()
                }
            }
            if (subSecs != null) {
                msecs += parseSubSeconds(subSecs)
            }
            msecs
        } catch (e: IllegalArgumentException) {
            null
        }
    }

    fun parseSubSeconds(subSec: String): Long {
        try {
            val len = min(subSec.length.toDouble(), 3.0).toInt()
            var sub = subSec.substring(0, len).toLong()
            for (i in len..2) {
                sub *= 10
            }
            return sub
        } catch (e: NumberFormatException) {
            // Ignored
        }
        return 0L
    }
}