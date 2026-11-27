package com.idunnololz.summit.util.coil

import android.graphics.Rect
import android.graphics.drawable.Animatable
import android.graphics.drawable.Drawable
import android.os.Looper
import android.os.SystemClock
import android.text.DynamicLayout
import android.text.NoCopySpan
import android.text.Spannable
import android.text.style.UpdateAppearance
import android.view.View
import android.widget.TextView
import com.idunnololz.animatedavif.AnimatedAvifDrawable
import com.idunnololz.summit.R
import io.noties.markwon.ext.tables.TableRowSpan
import io.noties.markwon.image.AsyncDrawable
import io.noties.markwon.image.AsyncDrawableSpan

object AsyncDrawableSchedulerFixed {
  internal fun hostView(drawable: AsyncDrawable): TextView? =
    (drawable.getCallback() as? DrawableCallbackImpl)?.view

  fun schedule(textView: TextView) {
    // we need a simple check if current text has already scheduled drawables
    // we need this in order to allow multiple calls to schedule (different plugins
    // might use AsyncDrawable), but we do not want to repeat the task
    //
    // hm... we need the same thing for unschedule then... we can check if last hash is !null,
    // if it's not -> unschedule, else ignore

    // @since 4.0.0
    val lastTextHashCode =
      textView.getTag(R.id.markwon_drawables_scheduler_last_text_hashcode) as? Int
    val textHashCode = textView.text.hashCode()
    if (lastTextHashCode != null &&
      lastTextHashCode == textHashCode
    ) {
      return
    }
    textView.setTag(R.id.markwon_drawables_scheduler_last_text_hashcode, textHashCode)
    val spans = extractRecursive(textView)
    if (!spans.isNullOrEmpty()) {
      if (textView.getTag(R.id.markwon_drawables_scheduler) == null) {
        val listener: View.OnAttachStateChangeListener =
          object : View.OnAttachStateChangeListener {
            override fun onViewAttachedToWindow(v: View) {
              schedule(textView)
            }

            override fun onViewDetachedFromWindow(v: View) {
              unschedule(textView)
//                            v.removeOnAttachStateChangeListener(this)
//                            v.setTag(R.id.markwon_drawables_scheduler, null)
            }
          }
        textView.addOnAttachStateChangeListener(listener)
        textView.setTag(R.id.markwon_drawables_scheduler, listener)
      }

      // @since 4.1.0
      val invalidator: DrawableCallbackImpl.Invalidator = TextViewInvalidator(textView)
      var drawable: AsyncDrawable
      for (span in spans) {
        drawable = span.drawableSpan.drawable

        (drawable.result as? AnimatedAvifDrawable)?.setHostView(textView)

        drawable.setCallback2(
          DrawableCallbackImpl(
            view = textView,
            drawable = drawable,
            textViewSpan = span.textViewSpan,
            invalidator = span.tableRow?.let {
              TableRowTextViewInvalidator(it, textView)
            } ?: invalidator,
          ),
        )
      }
    }
  }

  // must be called when text manually changed in TextView
  fun unschedule(view: TextView) {
    // @since 4.0.0
    if (view.getTag(R.id.markwon_drawables_scheduler_last_text_hashcode) == null) {
      return
    }
    view.setTag(R.id.markwon_drawables_scheduler_last_text_hashcode, null)
    val spans = extractRecursive(view)
    if (!spans.isNullOrEmpty()) {
      for (span in spans) {
        span.drawableSpan.drawable.setCallback2(null)
        val result = span.drawableSpan.drawable.result
        if (result is Animatable) {
          result.start()
        }
      }
    }
  }

  private class ExtractedDrawableSpan(
    val drawableSpan: AsyncDrawableSpan,
    val tableRow: TableRowSpan?,
    outermostTableRow: TableRowSpan?,
  ) {
    val textViewSpan: Any = outermostTableRow ?: drawableSpan
  }

  private fun extractRecursive(textView: TextView): MutableList<ExtractedDrawableSpan>? {
    val spannable = textView.text as? Spannable
    val length = spannable?.length ?: 0

    if (length == 0) {
      return null
    }

    if (spannable == null) {
      return null
    }

    val spans = mutableListOf<ExtractedDrawableSpan>()
    extractFromSpannable(accumulator = spans, spannable = spannable)

    return spans
  }

  private fun extractFromSpannable(
    accumulator: MutableList<ExtractedDrawableSpan>,
    spannable: Spannable,
    tableRow: TableRowSpan? = null,
    outermostTableRow: TableRowSpan? = tableRow,
  ) {
    val length = spannable.length
    val tableRows = spannable.getSpans(0, length, TableRowSpan::class.java)

    for (row in tableRows) {
      val cells = row.cells

      for (cell in cells) {
        val cellText = cell.text()
        val spannable = cellText as? Spannable
          ?: continue

        extractFromSpannable(
          accumulator = accumulator,
          spannable = spannable,
          tableRow = row,
          outermostTableRow = outermostTableRow ?: row,
        )
      }
    }

    accumulator.addAll(
      spannable.getSpans(0, length, AsyncDrawableSpan::class.java)
        .map {
          ExtractedDrawableSpan(
            drawableSpan = it,
            tableRow = tableRow,
            outermostTableRow = outermostTableRow,
          )
        },
    )
  }

  private class DrawableCallbackImpl(
    val view: TextView,
    private val drawable: AsyncDrawable,
    private val textViewSpan: Any,
    // @since 4.1.0
    private val invalidator: Invalidator,
  ) : Drawable.Callback {
    // @since 4.1.0
    // interface to be used when bounds change and view must be invalidated
    interface Invalidator {
      fun invalidate()
    }

    private val previousBounds = Rect(drawable.bounds)
    private val appearanceChange = object : UpdateAppearance, NoCopySpan {}
    private val redraw = Runnable {
      if (drawable.callback !== this) return@Runnable
      val text = view.text as? Spannable ?: return@Runnable
      val start = text.getSpanStart(textViewSpan)
      val end = text.getSpanEnd(textViewSpan)
      if (start !in 0..<end) return@Runnable

      // Hacky way to force the text view to invalidate its draw cache and force a re-render.
      text.setSpan(appearanceChange, start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
      text.removeSpan(appearanceChange)
    }

    override fun invalidateDrawable(who: Drawable) {
      if (Looper.myLooper() != Looper.getMainLooper()) {
        view.post { invalidateDrawable(who) }
        return
      }
      if (drawable.callback !== this) return
      val rect = who.bounds

      // okay... the thing is IF we do not change bounds size, normal invalidate would do
      // but if the size has changed, then we need to update the whole layout...
      if (previousBounds != rect) {
        // @since 4.1.0
        // invalidation moved to upper level (so invalidation can be deferred,
        // and multiple calls combined)
        invalidator.invalidate()
        previousBounds.set(rect)
      } else {
        if (view.isHardwareAccelerated && view.layout is DynamicLayout) {
          view.removeCallbacks(redraw)
          view.postOnAnimation(redraw)
        } else {
          view.postInvalidateOnAnimation()
        }
      }
    }

    override fun scheduleDrawable(who: Drawable, what: Runnable, whenMs: Long) {
      val delay = whenMs - SystemClock.uptimeMillis()
      view.postDelayed(what, delay)
    }

    override fun unscheduleDrawable(who: Drawable, what: Runnable) {
      view.removeCallbacks(what)
    }
  }

  private class TextViewInvalidator(
    private val textView: TextView,
  ) : DrawableCallbackImpl.Invalidator,
    Runnable {
    override fun invalidate() {
      textView.removeCallbacks(this)
      textView.post(this)
    }

    override fun run() {
      textView.text = textView.text
    }
  }

  private class TableRowTextViewInvalidator(
    private val tableRowSpan: TableRowSpan,
    private val textView: TextView,
  ) : DrawableCallbackImpl.Invalidator,
    Runnable {
    override fun invalidate() {
      textView.removeCallbacks(this)
      textView.post(this)
    }

    override fun run() {
      tableRowSpan.invalidate()
      textView.text = textView.text
    }
  }
}
