package com.idunnololz.summit.util.ext

import android.content.Context
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.URLSpan
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import com.idunnololz.summit.spans.CenteredImageSpan
import com.idunnololz.summit.util.CustomUrlSpan
import com.idunnololz.summit.util.Utils

fun SpannableStringBuilder.appendLink(text: String, url: String, underline: Boolean = false) {
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
        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE,
    )
}

fun SpannableStringBuilder.appendImage(
    context: Context,
    @DrawableRes drawableRes: Int,
    @ColorRes colorRes: Int,
) {
    val d = Utils.tint(context, drawableRes, colorRes)
    val size: Int = Utils.convertDpToPixel(16f).toInt()
    d.setBounds(0, 0, size, size)
    val s = length
    append("  ")
    val e = length
    setSpan(
        CenteredImageSpan(d),
        s,
        e,
        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE,
    )
}
