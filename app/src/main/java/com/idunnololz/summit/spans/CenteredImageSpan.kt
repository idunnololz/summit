package com.idunnololz.summit.spans

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Paint.FontMetricsInt
import android.graphics.drawable.Drawable
import android.text.style.DynamicDrawableSpan
import android.text.style.ImageSpan
import java.lang.ref.WeakReference

class CenteredImageSpan @JvmOverloads constructor(
    drawable: Drawable,
    verticalAlignment: Int = DynamicDrawableSpan.ALIGN_BOTTOM
) : ImageSpan(drawable, verticalAlignment) {

    private var mDrawableRef: WeakReference<Drawable?>? = null

    // Extra variables used to redefine the Font Metrics when an ImageSpan is added
    private var initialDescent = 0
    private var extraSpace = 0

    override fun draw(
        canvas: Canvas, text: CharSequence,
        start: Int, end: Int, x: Float,
        top: Int, y: Int, bottom: Int, paint: Paint
    ) {
        canvas.save()
        canvas.translate(x, 0f)
        drawable.draw(canvas)
        canvas.restore()
    }

    // Method used to redefined the Font Metrics when an ImageSpan is added
    override fun getSize(
        paint: Paint,
        text: CharSequence,
        start: Int,
        end: Int,
        fm: FontMetricsInt?
    ): Int {
        val d = cachedDrawable
        val rect = d!!.bounds
        val fmi = paint.fontMetricsInt
        if (rect.bottom - (fmi.descent - fmi.ascent) >= 0) { // Stores the initial descent and computes the margin available
            initialDescent = fmi.descent
            extraSpace = rect.bottom - (fmi.descent - fmi.ascent)
        }
        fmi.descent = extraSpace / 2 + initialDescent
        fmi.bottom = fmi.descent
        fmi.ascent = -rect.bottom + fmi.descent
        fmi.top = fmi.ascent
        return rect.right
    }

    // Redefined locally because it is a private member from DynamicDrawableSpan
    private val cachedDrawable: Drawable?
        private get() {
            val wr = mDrawableRef
            var d: Drawable? = null
            if (wr != null) d = wr.get()
            if (d == null) {
                d = drawable
                mDrawableRef = WeakReference(d)
            }
            return d
        }
}