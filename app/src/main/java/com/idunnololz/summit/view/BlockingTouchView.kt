package com.idunnololz.summit.view

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View

class BlockingTouchView : View {

  constructor(context: Context?) : super(context)
  constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)
  constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(
    context,
    attrs,
    defStyleAttr,
  )

  override fun performClick(): Boolean {
    super.performClick()
    return true
  }

  override fun onTouchEvent(event: MotionEvent?): Boolean {
    super.onTouchEvent(event)
    return true
  }
}