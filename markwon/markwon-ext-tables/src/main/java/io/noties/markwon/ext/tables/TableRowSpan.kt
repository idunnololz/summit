package io.noties.markwon.ext.tables

import android.annotation.SuppressLint
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.text.Layout
import android.text.Spannable
import android.text.SpannableString
import android.text.Spanned
import android.text.StaticLayout
import android.text.TextPaint
import android.text.style.ReplacementSpan
import android.util.Log
import androidx.annotation.IntDef
import androidx.annotation.IntRange
import io.noties.markwon.core.spans.TextLayoutSpan
import io.noties.markwon.image.AsyncDrawableSpan
import io.noties.markwon.utils.LeadingMarginUtils
import io.noties.markwon.utils.SpanUtils

class TableRowSpan(
    private val theme: TableTheme,
    val cells: List<Cell>,
    private val header: Boolean,
    private val odd: Boolean,
) : ReplacementSpan() {
    @IntDef(value = [ALIGN_LEFT, ALIGN_CENTER, ALIGN_RIGHT])
    @Retention(
        AnnotationRetention.SOURCE,
    )
    annotation class Alignment

    interface Invalidator {
        fun invalidate()
    }

    class Cell(@param:Alignment val alignment: Int, val text: CharSequence) {
        @Alignment
        fun alignment(): Int {
            return alignment
        }

        fun text(): CharSequence {
            return text
        }

        override fun toString(): String {
            return "Cell{" +
                "alignment=" + alignment +
                ", text=" + text +
                '}'
        }
    }

    private val layouts: MutableList<Layout> = ArrayList(cells.size)
    private val textPaint = TextPaint()

    private val rect = Rect()
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    private var width = 0
    private var height = 0
    private var invalidator: Invalidator? = null

    override fun getSize(
        paint: Paint,
        text: CharSequence,
        @IntRange(from = 0) start: Int,
        @IntRange(from = 0) end: Int,
        fm: Paint.FontMetricsInt?,
    ): Int {
        // it's our absolute requirement to have width of the canvas here... because, well, it changes
        // the way we draw text. So, if we do not know the width of canvas we cannot correctly measure our text

        if (layouts.size > 0) {
            if (fm != null) {
                var max = 0
                for (layout in layouts) {
                    val height = layout.height
                    if (height > max) {
                        max = height
                    }
                }

                // we store actual height
                height = max

                // but apply height with padding
                val padding = theme.tableCellPadding() * 2

                fm.ascent = -(max + padding)
                fm.descent = 0

                fm.top = fm.ascent
                fm.bottom = 0
            }
        }

        return width
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
        p: Paint,
    ) {
        val spanWidth = SpanUtils.width(canvas, text)
        if (recreateLayouts(spanWidth)) {
            width = spanWidth
            // @since 4.3.1 it's important to cast to TextPaint in order to display links, etc
            if (p is TextPaint) {
                // there must be a reason why this method receives Paint instead of TextPaint...
                textPaint.set(p)
            } else {
                textPaint.set(p)
            }
            makeNewLayouts()
        }

        var maxHeight = 0

        val padding = theme.tableCellPadding()

        val size = layouts.size

        val w = cellWidth(size)

        // @since 4.6.0 roundingDiff to offset last vertical border
        val roundingDiff = w - (width / size)

        // @since 1.1.1
        // draw backgrounds
        run {
            if (header) {
                theme.applyTableHeaderRowStyle(paint)
            } else if (odd) {
                theme.applyTableOddRowStyle(paint)
            } else {
                // even
                theme.applyTableEvenRowStyle(paint)
            }
            // if present (0 is transparent)
            if (paint.color != 0) {
                val save = canvas.save()
                try {
                    rect[0, 0, width] = bottom - top
                    canvas.translate(x, top.toFloat())
                    canvas.drawRect(rect, paint)
                } finally {
                    canvas.restoreToCount(save)
                }
            }
        }

        // @since 1.1.1 reset after applying background color
        // as background changes color attribute and if not specific tableBorderColor
        // is specified then after this row all borders will have color of this row (plus alpha)
        paint.set(p)
        theme.applyTableBorderStyle(paint)

        val borderWidth = theme.tableBorderWidth(paint)
        val drawBorder = borderWidth > 0

        // why divided by 4 gives a more or less good result is still not clear (shouldn't it be 2?)
        val heightDiff = (bottom - top - height) / 4

        // required for borderTop calculation
        val isFirstTableRow: Boolean

        // @since 4.3.1
        if (drawBorder) {
            var first = false
            // only if first draw the line
            run {
                val spanned = text as Spanned
                val spans = spanned.getSpans(start, end, TableSpan::class.java)
                if (spans != null && spans.size > 0) {
                    val span = spans[0]
                    if (LeadingMarginUtils.selfStart(start, text, span)) {
                        first = true
                        rect[x.toInt(), top, width] = top + borderWidth
                        canvas.drawRect(rect, paint)
                    }
                }
            }

            // draw the line at the bottom
            rect[x.toInt(), bottom - borderWidth, width] = bottom
            canvas.drawRect(rect, paint)

            isFirstTableRow = first
        } else {
            isFirstTableRow = false
        }

        val borderWidthHalf = borderWidth / 2

        // to NOT overlap borders inset top and bottom
        val borderTop = if (isFirstTableRow) borderWidth else 0
        val borderBottom = bottom - top - borderWidth

        var layout: Layout
        for (i in 0 until size) {
            layout = layouts[i]
            val save = canvas.save()
            try {
                canvas.translate(x + (i * w), top.toFloat())

                // @since 4.3.1
                if (drawBorder) {
                    // first vertical border will have full width (it cannot exceed canvas)
                    if (i == 0) {
                        rect[0, borderTop, borderWidth] = borderBottom
                    } else {
                        rect[-borderWidthHalf, borderTop, borderWidthHalf] = borderBottom
                    }

                    canvas.drawRect(rect, paint)

                    if (i == (size - 1)) {
                        // @since 4.6.0 subtract rounding offset for the last vertical divider
                        rect[w - borderWidth - roundingDiff, borderTop, w - roundingDiff] =
                            borderBottom
                        canvas.drawRect(rect, paint)
                    }
                }

                canvas.translate(padding.toFloat(), (padding + heightDiff).toFloat())
                layout.draw(canvas)

                if (layout.height > maxHeight) {
                    maxHeight = layout.height
                }
            } finally {
                canvas.restoreToCount(save)
            }
        }

        if (height != maxHeight) {
            if (invalidator != null) {
                invalidator!!.invalidate()
            }
        }
    }

    private fun recreateLayouts(newWidth: Int): Boolean {
        return width != newWidth
    }

    private fun makeNewLayouts() {
        textPaint.isFakeBoldText = header

        val columns = cells.size
        val padding = theme.tableCellPadding() * 2
        val w = cellWidth(columns) - padding

        layouts.clear()

        var i = 0
        val size = cells.size
        while (i < size) {
            makeLayout(i, w, cells[i])
            i++
        }
    }

    private fun makeLayout(index: Int, width: Int, cell: Cell) {
        val recreate = Runnable {
            val invalidator = this@TableRowSpan.invalidator
            if (invalidator != null) {
                layouts.removeAt(index)
                makeLayout(index, width, cell)
                invalidator.invalidate()
            }
        }

        val spannable = if (cell.text is Spannable) {
            cell.text
        } else {
            SpannableString(cell.text)
        }
        spannable.getSpans(
            0,
            spannable.length,
            TableDrawableSpan::class.java,
        ).forEach {
            it.setWidthHint(width)
        }

        val layout: Layout = StaticLayout(
            spannable,
            textPaint,
            width,
            alignment(cell.alignment),
            1.0f,
            0.0f,
            false,
        )

        // @since 4.4.0
        TextLayoutSpan.applyTo(spannable, layout)

        // @since 4.4.0
        scheduleAsyncDrawables(spannable, recreate)

        layouts.add(index, layout)
    }

    private fun scheduleAsyncDrawables(spannable: Spannable, recreate: Runnable) {
        val spans = spannable.getSpans(
            0,
            spannable.length,
            AsyncDrawableSpan::class.java,
        )
        if (spans != null && spans.isNotEmpty()) {
            for (span in spans) {
                val drawable = span.drawable

                // it is absolutely crucial to check if drawable is already attached,
                //  otherwise we would end up with a loop
                if (drawable.isAttached) {
                    continue
                }

                drawable.setCallback2(object : CallbackAdapter() {
                    override fun invalidateDrawable(who: Drawable) {
                        recreate.run()
                    }
                })
            }
        }
    }

    /**
     * Obtain Layout given horizontal offset. Primary usage target - MovementMethod
     *
     * @since 4.6.0
     */
    fun findLayoutForHorizontalOffset(x: Int): Layout? {
        val size = layouts.size
        val w = cellWidth(size)
        val i = x / w
        if (i >= size) {
            return null
        }
        return layouts[i]
    }

    /**
     * @since 4.6.0
     */
    fun cellWidth(): Int {
        return cellWidth(layouts.size)
    }

    // @since 4.6.0
    protected fun cellWidth(size: Int): Int {
        return (1f * width / size + 0.5f).toInt()
    }

    fun invalidate() {
        width = 0
    }

    fun invalidator(invalidator: Invalidator?) {
        this.invalidator = invalidator
    }

    private abstract class CallbackAdapter : Drawable.Callback {
        override fun invalidateDrawable(who: Drawable) {
        }

        override fun scheduleDrawable(who: Drawable, what: Runnable, `when`: Long) {
        }

        override fun unscheduleDrawable(who: Drawable, what: Runnable) {
        }
    }

    companion object {
        const val ALIGN_LEFT: Int = 0
        const val ALIGN_CENTER: Int = 1
        const val ALIGN_RIGHT: Int = 2

        @SuppressLint("SwitchIntDef")
        private fun alignment(@Alignment alignment: Int): Layout.Alignment {
            val out = when (alignment) {
                ALIGN_CENTER -> Layout.Alignment.ALIGN_CENTER
                ALIGN_RIGHT -> Layout.Alignment.ALIGN_OPPOSITE
                else -> Layout.Alignment.ALIGN_NORMAL
            }
            return out
        }
    }
}
