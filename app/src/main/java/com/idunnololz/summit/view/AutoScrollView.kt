package com.idunnololz.summit.view

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.widget.HorizontalScrollView

class AutoHorizontalScrollView : HorizontalScrollView {

    private var isScrollable: Boolean = true

    constructor(context: Context?) : super(context)
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr,
    )

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(ev: MotionEvent?): Boolean {
        return when (ev!!.action) {
            MotionEvent.ACTION_DOWN ->
                // if we can scroll pass the event to the superclass
                isScrollable && super.onTouchEvent(ev)

            else -> super.onTouchEvent(ev)
        }
    }

    override fun onInterceptTouchEvent(ev: MotionEvent?): Boolean {
        return isScrollable && super.onInterceptTouchEvent(ev)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)

        val childWidth = if (this.childCount == 0) {
            0
        } else {
            val child = getChildAt(0)
            child.measuredWidth + child.paddingLeft + child.paddingRight
        }

        if (childWidth == 0) {
            isScrollable = false
        } else {
            isScrollable = childWidth > this.measuredWidth
        }
    }
}