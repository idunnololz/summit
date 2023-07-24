package com.idunnololz.summit.util

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.ViewConfiguration
import android.view.animation.DecelerateInterpolator
import android.widget.Scroller
import androidx.viewpager.widget.ViewPager
import java.lang.reflect.Field
import kotlin.math.abs


class CustomScrollViewPager : ViewPager {

    private var enabled = true

    var disableLeftSwipe = false
    var disableRightSwipe = false

    private var lastMotionX = 0f
    private var activePointerId = 0
    private var touchSlop = 0

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)

    init {
        val configuration = ViewConfiguration.get(context)
        touchSlop = configuration.scaledPagingTouchSlop
    }
    
    override fun onTouchEvent(event: MotionEvent): Boolean {
        return if (enabled) {

            val action: Int = event.action
            when (action and MotionEvent.ACTION_MASK) {

                MotionEvent.ACTION_MOVE -> {
                    val pointerIndex: Int = event.findPointerIndex(activePointerId)
                    if (pointerIndex == -1) {
                        activePointerId = 0
                        return false
                    }

                    val x: Float = event.getX(pointerIndex)
                    val xDiff = x - lastMotionX
                    val xDiffAbs = abs(xDiff)

                    if (xDiffAbs >= touchSlop) {
                        if (xDiff < 0 && disableLeftSwipe) {
                            return false
                        }
                    }
                }

                MotionEvent.ACTION_UP -> {}

                MotionEvent.ACTION_CANCEL -> {}

                MotionEvent.ACTION_DOWN -> {
                    val index: Int = event.actionIndex
                    activePointerId = event.getPointerId(0)
                    val x: Float = event.getX(index)
                    lastMotionX = x
                }
                MotionEvent.ACTION_POINTER_DOWN -> {
                }

                MotionEvent.ACTION_POINTER_UP -> {
                }
            }

            try {
                super.onTouchEvent(event)
            } catch (e: Exception) {
                // sometimes sht happens...
                // especially with multi touch
            }
            false
        } else false
    }

    override fun onInterceptTouchEvent(event: MotionEvent): Boolean {
        return if (enabled) {
            val action: Int = event.action
            when (action and MotionEvent.ACTION_MASK) {

                MotionEvent.ACTION_MOVE -> {
                    val pointerIndex: Int = event.findPointerIndex(activePointerId)
                    if (pointerIndex == -1) {
                        activePointerId = 0
                        return false
                    }

                    val x: Float = event.getX(pointerIndex)
                    val xDiff = x - lastMotionX
                    val xDiffAbs = abs(xDiff)

                    if (xDiffAbs >= touchSlop) {
                        if (xDiff < 0 && disableLeftSwipe) {
                            return false
                        }
                    }
                }

                MotionEvent.ACTION_UP -> {}

                MotionEvent.ACTION_CANCEL -> {}

                MotionEvent.ACTION_DOWN -> {
                    val index: Int = event.actionIndex
                    activePointerId = event.getPointerId(0)
                    val x: Float = event.getX(index)
                    lastMotionX = x
                }
                MotionEvent.ACTION_POINTER_DOWN -> {
                }

                MotionEvent.ACTION_POINTER_UP -> {
                }
            }
            super.onInterceptTouchEvent(event)
        } else false
    }

    fun setPagingEnabled(enabled: Boolean) {
        this.enabled = enabled
    }

//    fun setDurationScroll(millis: Int) {
//        try {
//            val viewpager: Class<*> = ViewPager::class.java
//            val scroller: Field = viewpager.getDeclaredField("mScroller")
//            scroller.setAccessible(true)
//            scroller.set(this, OwnScroller(context, millis))
//        } catch (e: Exception) {
//            e.printStackTrace()
//        }
//    }
//
//    class OwnScroller(context: Context?, durationScroll: Int) :
//        Scroller(context, DecelerateInterpolator()) {
//        private var durationScrollMillis = 1
//
//        init {
//            durationScrollMillis = durationScroll
//        }
//
//        override fun startScroll(startX: Int, startY: Int, dx: Int, dy: Int, duration: Int) {
//            super.startScroll(startX, startY, dx, dy, durationScrollMillis)
//        }
//    }
}