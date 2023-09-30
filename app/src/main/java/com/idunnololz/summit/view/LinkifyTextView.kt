package com.idunnololz.summit.view

import android.content.Context
import android.text.Spannable
import android.util.AttributeSet
import android.view.MotionEvent
import androidx.appcompat.widget.AppCompatTextView

class LinkifyTextView : AppCompatTextView {

    var disableTouch: Boolean = false

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr,
    )

    override fun dispatchTouchEvent(event: MotionEvent?): Boolean {
        val movementMethod = movementMethod
        val spannable = text as? Spannable
        val handled = if (spannable != null && !disableTouch) {
            movementMethod.onTouchEvent(this, text as? Spannable, event)
        } else {
            false
        }
        return handled
    }
}
