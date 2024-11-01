package com.idunnololz.summit.lemmy.utils

import android.content.res.ColorStateList
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.widget.TextView
import androidx.core.view.doOnNextLayout
import androidx.core.widget.TextViewCompat

/**
 * From https://stackoverflow.com/a/73911699/1299750
 */
fun TextView.addEllipsizeToSpannedOnLayout() {
    doOnNextLayout {
        if (maxLines != -1 && lineCount > maxLines) {
            val endOfLastLine = layout.getLineEnd(maxLines - 1)
            val spannedDropLast3Chars = text.subSequence(0, endOfLastLine - 3) as? Spanned

            if (spannedDropLast3Chars != null) {
                val spannableBuilder = SpannableStringBuilder()
                    .append(spannedDropLast3Chars)
                    .append("…")

                text = spannableBuilder

                post {
                    requestLayout()
                }
            }
        }
    }
}

var TextView.compoundDrawableTintListCompat: ColorStateList?
    get() = TextViewCompat.getCompoundDrawableTintList(this)
    set(value) {
        TextViewCompat.setCompoundDrawableTintList(this, value)
    }
