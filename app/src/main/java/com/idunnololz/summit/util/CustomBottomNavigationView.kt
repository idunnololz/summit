package com.idunnololz.summit.util

import android.content.Context
import android.util.AttributeSet
import com.google.android.material.bottomnavigation.BottomNavigationView

class CustomBottomNavigationView : BottomNavigationView {

  constructor(context: Context) : super(context)
  constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
  constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
    context,
    attrs,
    defStyleAttr,
  )

  constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int, defStyleRes: Int) : super(
    context,
    attrs,
    defStyleAttr,
    defStyleRes,
  )

  override fun setTranslationY(translationY: Float) {
    super.setTranslationY(translationY)
  }
}
