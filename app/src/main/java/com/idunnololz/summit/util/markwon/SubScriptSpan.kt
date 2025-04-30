package com.idunnololz.summit.util.markwon

import android.text.TextPaint
import android.text.style.MetricAffectingSpan

class SubScriptSpan : MetricAffectingSpan() {
  override fun updateDrawState(tp: TextPaint) {
    apply(tp)
  }

  override fun updateMeasureState(tp: TextPaint) {
    apply(tp)
  }

  private fun apply(paint: TextPaint) {
    paint.textSize = paint.textSize * 0.75f
    paint.baselineShift -= (paint.ascent() / 2).toInt()
  }
}
