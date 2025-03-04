package com.idunnololz.summit.view

import android.content.Context
import android.os.Build
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import androidx.appcompat.R
import kotlin.math.max

/**
 * An extension of LinearLayout that automatically switches to vertical
 * orientation when it can't fit its child views horizontally.
 */
// From https://android.googlesource.com/platform/frameworks/support/+/26079d87c79a64829f036236353fac1dae4e0613/appcompat/src/main/java/androidx/appcompat/widget/ButtonBarLayout.java
class ButtonBarLayout(context: Context, attrs: AttributeSet?) :
    LinearLayout(context, attrs) {
    /** Whether the current configuration allows stacking.  */
    private var mAllowStacking: Boolean
    private var mLastWidthSize = -1
    private val mMinimumHeight = 0

    init {
        val ta = context.obtainStyledAttributes(attrs, R.styleable.ButtonBarLayout)
        if (Build.VERSION.SDK_INT >= 29) {
            saveAttributeDataForStyleable(context, R.styleable.ButtonBarLayout, attrs, ta, 0, 0)
        }
        mAllowStacking = ta.getBoolean(R.styleable.ButtonBarLayout_allowStacking, true)
        ta.recycle()
    }

    fun setAllowStacking(allowStacking: Boolean) {
        if (mAllowStacking != allowStacking) {
            mAllowStacking = allowStacking
            if (!mAllowStacking && orientation == VERTICAL) {
                isStacked = false
            }
            requestLayout()
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val widthSize = MeasureSpec.getSize(widthMeasureSpec)
        if (mAllowStacking) {
            if (widthSize > mLastWidthSize && isStacked) {
                // We're being measured wider this time, try un-stacking.
                isStacked = false
            }
            mLastWidthSize = widthSize
        }
        var needsRemeasure = false
        // If we're not stacked, make sure the measure spec is AT_MOST rather
        // than EXACTLY. This ensures that we'll still get TOO_SMALL so that we
        // know to stack the buttons.
        val initialWidthMeasureSpec: Int
        if (!isStacked && MeasureSpec.getMode(widthMeasureSpec) == MeasureSpec.EXACTLY) {
            initialWidthMeasureSpec = MeasureSpec.makeMeasureSpec(widthSize, MeasureSpec.AT_MOST)
            // We'll need to remeasure again to fill excess space.
            needsRemeasure = true
        } else {
            initialWidthMeasureSpec = widthMeasureSpec
        }
        super.onMeasure(initialWidthMeasureSpec, heightMeasureSpec)
        if (mAllowStacking && !isStacked) {
            val stack: Boolean
            val measuredWidth = measuredWidthAndState
            val measuredWidthState = measuredWidth and MEASURED_STATE_MASK
            stack = measuredWidthState == MEASURED_STATE_TOO_SMALL
            if (stack) {
                isStacked = true
                // Measure again in the new orientation.
                needsRemeasure = true
            }
        }
        if (needsRemeasure) {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        }
        // Compute minimum height such that, when stacked, some portion of the
        // second button is visible.
        var minHeight = 0
        val firstVisible = getNextVisibleChildIndex(0)
        if (firstVisible >= 0) {
            val firstButton = getChildAt(firstVisible)
            val firstParams = firstButton.layoutParams as LayoutParams
            minHeight += (
                paddingTop + firstButton.measuredHeight +
                    firstParams.topMargin + firstParams.bottomMargin
                )
            if (isStacked) {
                val secondVisible = getNextVisibleChildIndex(firstVisible + 1)
                if (secondVisible >= 0) {
                    minHeight += (
                        getChildAt(secondVisible).paddingTop +
                            (PEEK_BUTTON_DP * resources.displayMetrics.density).toInt()
                        )
                }
            } else {
                minHeight += paddingBottom
            }
        }
        if (this.minimumHeight != minHeight) {
            minimumHeight = minHeight
        }
    }

    private fun getNextVisibleChildIndex(index: Int): Int {
        var i = index
        val count = childCount
        while (i < count) {
            if (getChildAt(i).visibility == VISIBLE) {
                return i
            }
            i++
        }
        return -1
    }

    override fun getMinimumHeight(): Int {
        return max(mMinimumHeight.toDouble(), super.getMinimumHeight().toDouble()).toInt()
    }

    private var isStacked: Boolean
        get() = orientation == VERTICAL
        private set(stacked) {
            orientation = if (stacked) VERTICAL else HORIZONTAL
            gravity = if (stacked) Gravity.RIGHT else Gravity.BOTTOM
            val spacer =
                findViewById<View>(R.id.spacer)
            if (spacer != null) {
                spacer.visibility = if (stacked) GONE else INVISIBLE
            }
            // Reverse the child order. This is specific to the Material button
            // bar's layout XML and will probably not generalize.
            val childCount = childCount
            for (i in childCount - 2 downTo 0) {
                bringChildToFront(getChildAt(i))
            }
        }

    companion object {
        /** Amount of the second button to "peek" above the fold when stacked.  */
        private const val PEEK_BUTTON_DP = 16
    }
}
