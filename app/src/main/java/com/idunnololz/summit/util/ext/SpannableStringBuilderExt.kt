package com.idunnololz.summit.util.ext

import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.URLSpan

fun SpannableStringBuilder.appendLink(text: String, url: String) {
    val start = length
    append(text)
    val end = length
    setSpan(URLSpan(url), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
}