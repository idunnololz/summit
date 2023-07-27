package com.idunnololz.summit.view

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatEditText
import androidx.core.content.res.use
import com.idunnololz.summit.R

class PrefixedEditText : AppCompatEditText {
    var originalLeftPadding = -1f

    private var prefix: String? = null

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        loadAttrs(attrs)
    }

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr,
    ) {
        loadAttrs(attrs, defStyleAttr)
    }

    @SuppressLint("Recycle") // Recycled using "use()"
    private fun loadAttrs(attrs: AttributeSet?, defStyleAttr: Int = 0) {
        attrs ?: return

        context.obtainStyledAttributes(attrs, R.styleable.PrefixedEditText, defStyleAttr, 0).use {
            prefix = it.getString(R.styleable.PrefixedEditText_prefix)
        }
    }

    override fun onMeasure(
        widthMeasureSpec: Int,
        heightMeasureSpec: Int,
    ) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        calculatePrefix()
    }

    private fun calculatePrefix() {
        val prefix = prefix ?: return
        if (originalLeftPadding == -1f) {
            val widths = FloatArray(prefix.length)
            paint.getTextWidths(prefix, widths)
            var textWidth = 0f
            for (w in widths) {
                textWidth += w
            }
            originalLeftPadding = compoundPaddingLeft.toFloat()
            setPadding(
                (textWidth + originalLeftPadding).toInt(),
                paddingTop,
                paddingRight,
                paddingBottom,
            )
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        prefix?.let { prefix ->
            canvas.drawText(
                prefix,
                originalLeftPadding,
                getLineBounds(0, null).toFloat(),
                paint,
            )
        }
    }
}
