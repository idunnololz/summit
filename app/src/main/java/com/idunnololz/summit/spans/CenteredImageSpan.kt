package com.idunnololz.summit.spans

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Paint.FontMetricsInt
import android.graphics.drawable.Drawable
import android.text.style.ImageSpan
import androidx.core.graphics.withSave


class CenteredImageSpan @JvmOverloads constructor(
  drawable: Drawable,
  verticalAlignment: Int = ALIGN_BOTTOM,
) : ImageSpan(drawable, verticalAlignment) {

  override fun draw(
    canvas: Canvas,
    text: CharSequence,
    start: Int,
    end: Int,
    x: Float,
    top: Int,
    y: Int,
    bottom: Int,
    paint: Paint,
  ) {
    val drawable = getDrawable()
    canvas.withSave {
      val fmPaint = paint.getFontMetricsInt()
      val fontHeight = fmPaint.descent - fmPaint.ascent
      val centerY = y + fmPaint.descent - fontHeight / 2
      val transY = centerY - (drawable.getBounds().bottom - drawable.getBounds().top) / 2f
      translate(x, transY)
      drawable.draw(this)
    }
  }

  // Method used to redefined the Font Metrics when an ImageSpan is added
  override fun getSize(
    paint: Paint,
    text: CharSequence,
    start: Int,
    end: Int,
    fm: FontMetricsInt?,
  ): Int {
    val drawable = getDrawable()
    val rect = drawable.getBounds()
    if (fm != null) {
      val fmPaint = paint.getFontMetricsInt()
      val fontHeight = fmPaint.descent - fmPaint.ascent
      val drHeight = rect.bottom - rect.top
      val centerY = fmPaint.ascent + fontHeight / 2

      fm.ascent = centerY - drHeight / 2
      fm.top = fm.ascent
      fm.bottom = centerY + drHeight / 2
      fm.descent = fm.bottom
    }
    return rect.right
  }
}
