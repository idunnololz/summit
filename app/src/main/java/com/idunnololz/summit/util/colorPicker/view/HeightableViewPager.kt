package com.idunnololz.summit.util.colorPicker.view

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import androidx.viewpager.widget.ViewPager
import kotlin.math.max

class HeightableViewPager : ViewPager {
    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)

    override fun onInterceptTouchEvent(event: MotionEvent): Boolean {
        return false
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        var heightMeasureSpec = heightMeasureSpec
        var height = 0

        val adapter = adapter
        if (adapter != null && adapter is Heightable) height = (adapter as Heightable).getHeightAt(
            currentItem, widthMeasureSpec, MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED)
        )

        if (height <= 0) {
            val v = getChildAt(currentItem)
            if (v != null) {
                v.measure(widthMeasureSpec, MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED))
                height = v.measuredHeight
            }
        }

        if (height <= 0) {
            for (i in 0 until childCount) {
                val v = getChildAt(i)
                v.measure(widthMeasureSpec, MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED))
                height = max(height.toDouble(), v.measuredHeight.toDouble()).toInt()
            }
        }

        if (height > 0) heightMeasureSpec = MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY)

        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
    }

    interface Heightable {
        fun getHeightAt(position: Int, widthMeasureSpec: Int, heightMeasureSpec: Int): Int
    }
}