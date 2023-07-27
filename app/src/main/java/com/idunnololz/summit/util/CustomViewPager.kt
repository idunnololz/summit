package com.idunnololz.summit.util

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import androidx.viewpager.widget.ViewPager

class CustomViewPager : ViewPager {

    private var enabled = true

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)

    override fun onTouchEvent(event: MotionEvent): Boolean {
        return if (enabled) {
            super.onTouchEvent(event)
        } else {
            false
        }
    }

    override fun onInterceptTouchEvent(event: MotionEvent): Boolean {
        return if (enabled) {
            super.onInterceptTouchEvent(event)
        } else {
            false
        }
    }

    fun setPagingEnabled(enabled: Boolean) {
        this.enabled = enabled
    }
}
