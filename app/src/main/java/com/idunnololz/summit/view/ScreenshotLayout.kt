package com.idunnololz.summit.view

import android.content.Context
import android.graphics.Canvas
import android.util.AttributeSet
import android.view.ViewGroup
import android.widget.LinearLayout

class ScreenshotLayout : LinearLayout {

    interface ScreenshotDecorator {
        fun onDraw(c: Canvas, parent: ViewGroup)
    }

    constructor(context: Context?) : super(context)
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr,
    )

    constructor(
        context: Context?,
        attrs: AttributeSet?,
        defStyleAttr: Int,
        defStyleRes: Int,
    ) : super(context, attrs, defStyleAttr, defStyleRes)

    var decorator: ScreenshotDecorator? = null
        set(value) {
            field = value

            invalidate()
        }

    init {
        setWillNotDraw(false)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        decorator?.onDraw(canvas, this)
    }
}
