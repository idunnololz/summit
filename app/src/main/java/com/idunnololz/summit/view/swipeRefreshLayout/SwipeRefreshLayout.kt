package com.idunnololz.summit.view.swipeRefreshLayout

import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import com.idunnololz.summit.R
import com.idunnololz.summit.util.ext.getColorFromAttribute
import com.idunnololz.summit.util.ext.getDimen
import com.idunnololz.summit.view.loadingIndicator.LoadingIndicator

class SwipeRefreshLayout : SimpleSwipeRefreshLayout {

  constructor(context: Context?) : super(context)
  constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)
  constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(
    context,
    attrs,
    defStyleAttr,
  )

  private val loadingIndicator = LoadingIndicator(context).apply {
    drawable.state
    containerColor = context.getColorFromAttribute(
      com.google.android.material.R.attr.colorPrimaryContainer)
  }

  init {
    addView(loadingIndicator).apply {
      layoutParams = LayoutParams(WRAP_CONTENT, WRAP_CONTENT).apply {
        topMargin = context.getDimen(R.dimen.padding)
        bottomMargin = context.getDimen(R.dimen.padding)
      }
    }
    loadingIndicator.playing = false

    addProgressListener {
      loadingIndicator.alpha = it
      loadingIndicator.setProgress(it / 100f)
    }
    addTriggerListener {
      loadingIndicator.playing = true
    }
  }

  override fun stopRefreshingComplete() {
    loadingIndicator.playing = false
    loadingIndicator.setProgress(0f)
    loadingIndicator.alpha = 0f
  }
}