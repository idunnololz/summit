package io.noties.markwon.image

import android.graphics.Canvas
import android.graphics.Paint
import android.text.Spanned
import android.text.style.ReplacementSpan
import androidx.annotation.IntDef
import androidx.annotation.IntRange
import io.noties.markwon.core.MarkwonTheme
import io.noties.markwon.core.spans.TextLayoutSpan
import io.noties.markwon.core.spans.TextViewSpan
import io.noties.markwon.tables.TableDrawableSpan
import io.noties.markwon.utils.SpanUtils
import kotlin.math.max
import kotlin.math.min

// @since 4.2.1 we do not set intrinsic bounds
//  at this point they will always be 0,0-1,1, but this
//  will trigger another invalidation when we will have bounds
class AsyncDrawableSpan(
  private val theme: MarkwonTheme,
  val drawable: AsyncDrawable,
  @param:Alignment private val alignment: Int,
  private val replacementTextIsLink: Boolean,
) : ReplacementSpan(), TableDrawableSpan {

  @IntDef(ALIGN_BOTTOM, ALIGN_BASELINE, ALIGN_CENTER)
  @Retention(
    AnnotationRetention.SOURCE,
  )
  internal annotation class Alignment

  private var canvasSize: Int = 0

  override fun getSize(
    paint: Paint,
    text: CharSequence,
    @IntRange(from = 0) start: Int,
    @IntRange(from = 0) end: Int,
    fm: Paint.FontMetricsInt?,
  ): Int {
    // if we have no async drawable result - we will just render text

    val size: Int

    if (drawable.hasResult()) {
      if (canvasSize > 0 && !drawable.hasKnownDimensions()) {
        drawable.initWithKnownDimensions(
          width(canvasSize, text),
          paint.textSize,
        )
      }

      val rect = drawable.bounds

      if (fm != null) {
        fm.ascent = -rect.bottom
        fm.descent = 0

        fm.top = fm.ascent
        fm.bottom = 0
      }

      size = rect.right
    } else {
      // we will apply style here in case if theme modifies textSize or style (affects metrics)

      if (replacementTextIsLink) {
        theme.applyLinkStyle(paint)
      }

      // NB, no specific text handling (no new lines, etc)
      size = (paint.measureText(text, start, end) + .5f).toInt()
    }

    return size
  }

  override fun draw(
    canvas: Canvas,
    text: CharSequence,
    @IntRange(from = 0) start: Int,
    @IntRange(from = 0) end: Int,
    x: Float,
    top: Int,
    y: Int,
    bottom: Int,
    paint: Paint,
  ) {
    val canvasWidth = SpanUtils.width(canvas, text)
    val minWidth = theme.minImageSize

    canvasSize = canvasWidth

    if (canvasWidth - x.toInt() < minWidth) {
      // Just take the entire column...
      drawable.initWithKnownDimensions(
        max(canvasWidth, minWidth),
        paint.textSize,
      )
    } else {
      drawable.initWithKnownDimensions(
        max(canvasWidth - x.toInt(), minWidth),
        paint.textSize,
      )
    }

    val drawable = this.drawable

    if (drawable.hasResult()) {
      val b = bottom - drawable.bounds.bottom

      val save = canvas.save()
      try {
        val translationY = if (ALIGN_CENTER == alignment) {
          b - ((bottom - top - drawable.bounds.height()) / 2)
        } else if (ALIGN_BASELINE == alignment) {
          b - paint.fontMetricsInt.descent
        } else {
          b
        }
        canvas.translate(x, translationY.toFloat())
        drawable.draw(canvas)
      } finally {
        canvas.restoreToCount(save)
      }
    } else {
      // will it make sense to have additional background/borders for an image replacement?
      // let's focus on main functionality and then think of it

      val textY = textCenterY(top, bottom, paint)
      if (replacementTextIsLink) {
        theme.applyLinkStyle(paint)
      }

      // NB, no specific text handling (no new lines, etc)
      canvas.drawText(text, start, end, x, textY, paint)
    }
  }

  companion object {
    const val ALIGN_BOTTOM: Int = 0
    const val ALIGN_BASELINE: Int = 1

    // will only center if drawable height is less than text line height
    const val ALIGN_CENTER: Int = 2

    private fun textCenterY(top: Int, bottom: Int, paint: Paint): Float {
      // @since 1.1.1 it's `top +` and not `bottom -`
      return (top + ((bottom - top) / 2) - ((paint.descent() + paint.ascent()) / 2f + .5f)).toInt()
        .toFloat()
    }
  }

  override fun setWidthHint(width: Int) {
    canvasSize = width
  }

  fun width(width: Int, cs: CharSequence): Int {
    // Layout
    // TextView
    // canvas

    if (cs is Spanned) {
      val spanned = cs

      // if we are displayed with layout information -> use it
      val layout = TextLayoutSpan.layoutOf(spanned)
      if (layout != null) {
        return layout.width
      }

      // if we have TextView -> obtain width from it (exclude padding)
      val textView = TextViewSpan.textViewOf(spanned)
      if (textView != null) {
        return textView.width - textView.paddingLeft - textView.paddingRight
      }
    }

    // else just use canvas width
    return width
  }
}
