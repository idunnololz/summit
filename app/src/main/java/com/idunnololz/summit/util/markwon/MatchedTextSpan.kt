package com.idunnololz.summit.util.markwon

import android.text.TextPaint
import android.text.style.MetricAffectingSpan

class MatchedTextSpan(
  val matchIndex: Int,
  val backgroundColor: Int,
  val foregroundColor: Int,
  val highlightColor: Int,
  var highlight: Boolean,
) : MetricAffectingSpan() {

  override fun updateDrawState(textPaint: TextPaint?) {
    update(textPaint)
  }

  override fun updateMeasureState(textPaint: TextPaint) {
    update(textPaint)
  }

  fun update(textPaint: TextPaint?) {
    if (highlight) {
      textPaint?.bgColor = highlightColor
    } else {
      textPaint?.bgColor = backgroundColor
      textPaint?.setColor(foregroundColor)
    }
  }
}
