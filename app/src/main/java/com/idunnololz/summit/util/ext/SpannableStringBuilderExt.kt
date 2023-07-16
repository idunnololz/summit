package com.idunnololz.summit.util.ext

import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.URLSpan
import com.idunnololz.summit.util.CustomUrlSpan

fun SpannableStringBuilder.appendLink(text: String, url: String, underline: Boolean = true) {
    val start = length
    append(text)
    val end = length
    setSpan(
        if (underline) {
            URLSpan(url)
        } else {
            CustomUrlSpan(url)
        },
        start,
        end,
        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
    )
}