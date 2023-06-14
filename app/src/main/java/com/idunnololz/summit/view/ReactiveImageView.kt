package com.idunnololz.summit.view

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.drawable.RippleDrawable
import android.os.Build
import android.util.AttributeSet
import android.view.MotionEvent
import androidx.appcompat.widget.AppCompatImageView
import androidx.core.content.ContextCompat
import androidx.core.widget.ImageViewCompat
import com.idunnololz.summit.R
import com.idunnololz.summit.util.ext.getColorFromAttribute

class ReactiveImageView : AppCompatImageView {

    companion object {
        fun makePrimaryColorIconImageView(context: Context) = ReactiveImageView(context).apply {
            ImageViewCompat.setImageTintList(
                this,
                ColorStateList.valueOf(
                    context.getColorFromAttribute(androidx.appcompat.R.attr.colorPrimary))
            )
        }
    }

    private val isRipple = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
    private var drawOverlay = false
    private lateinit var paint: Paint

    constructor(context: Context) : super(context) {
        init()
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        init()
    }

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ) {
        init()
    }

    @SuppressLint("NewApi")
    private fun init() {
        paint = Paint().apply {
            color = ContextCompat.getColor(context, R.color.white50)
        }

        if (isRipple) {
            foreground = RippleDrawable(
                ColorStateList.valueOf(ContextCompat.getColor(context, R.color.white97)),
                null, null
            )
        }
    }

    override fun dispatchTouchEvent(event: MotionEvent): Boolean {
        if (!isRipple && isClickable && drawable != null) {
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    drawOverlay = true
                    invalidate()
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    drawOverlay = false
                    invalidate()
                }
            }
        }

        return super.dispatchTouchEvent(event)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (drawOverlay) {
            canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
        }
    }
}