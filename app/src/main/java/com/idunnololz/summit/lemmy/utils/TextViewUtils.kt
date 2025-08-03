package com.idunnololz.summit.lemmy.utils

import android.content.res.ColorStateList
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.widget.TextView
import androidx.annotation.StyleRes
import androidx.core.view.doOnNextLayout
import androidx.core.widget.TextViewCompat
import com.idunnololz.summit.util.ext.getResIdFromAttribute

/**
 * From https://stackoverflow.com/a/73911699/1299750
 */
fun TextView.addEllipsizeToSpannedOnLayout() {
  doOnNextLayout {
    if (maxLines != -1 && lineCount > maxLines) {
      val endOfLastLine = layout.getLineEnd(maxLines - 1)
      val spannedDropLastChar = text.subSequence(0, endOfLastLine - 1) as? Spanned

      if (spannedDropLastChar != null) {
        val spannableBuilder = SpannableStringBuilder()
          .append(spannedDropLastChar)
          .append("…")

        text = spannableBuilder

        post {
          requestLayout()
        }
      }
    }
  }
}

fun TextView.setTextAppearanceCompat(@StyleRes resId: Int) {
  val typeFace = typeface
  TextViewCompat.setTextAppearance(this, resId)
  this.typeface = typeFace
}

var TextView.compoundDrawableTintListCompat: ColorStateList?
  get() = TextViewCompat.getCompoundDrawableTintList(this)
  set(value) {
    TextViewCompat.setCompoundDrawableTintList(this, value)
  }
