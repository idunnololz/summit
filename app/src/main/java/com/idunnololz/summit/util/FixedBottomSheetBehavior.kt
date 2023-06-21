package com.idunnololz.summit.util

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import androidx.coordinatorlayout.widget.CoordinatorLayout
import com.google.android.material.bottomsheet.BottomSheetBehavior

@Suppress("unused") // WARNING: THIS IS USED!!!
class FixedBottomSheetBehavior<V : View> : BottomSheetBehavior<V> {

    companion object {
        private val TAG: String? = FixedBottomSheetBehavior::class.java.canonicalName
    }

    constructor()

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)

    override fun onInterceptTouchEvent(parent: CoordinatorLayout, child: V, event: MotionEvent): Boolean {
        return try {
            super.onInterceptTouchEvent(parent, child, event)
        } catch (ignored: NullPointerException) {
            // BottomSheetBehavior receives input events too soon and mNestedScrollingChildRef is not set yet.
            false
        }
    }
}
