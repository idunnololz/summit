package com.idunnololz.summit.view

import android.content.Context
import android.graphics.Rect
import android.os.Build
import android.util.AttributeSet
import android.view.MotionEvent
import androidx.appcompat.widget.AppCompatTextView

class FixedTextView : AppCompatTextView {

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    )

    override fun performLongClick(x: Float, y: Float): Boolean {
        return try {
            super.performLongClick(x, y)
        } catch (e: IllegalStateException) {
            // only override startDrag for Android 9 or later to workaround the following issue:
            // "IllegalStateException: Drag shadow dimensions must be positive"
            // - see https://issuetracker.google.com/issues/113347222
            // - also https://github.com/wordpress-mobile/WordPress-Android/issues/10492
            // rationale: the LongClick gesture takes precedence over the startDrag operation
            // so, listening to it first gives us the possibility to discard processing the event
            // when the crash conditions would be otherwise met. Conditions follow.
            // In the case of a zero width character being the sole selection, the shadow dimensions
            // would be zero, incurring in the actual crash. Given it doesn't really make sense to
            // select a newline and try dragging it around, we're just completely capturing the event
            // and signaling the OS that it was handled, so it doesn't propagate to the TextView's
            // longClickListener actually implementing dragging.
            false
        }
    }
}