package com.idunnololz.summit.util

import com.idunnololz.summit.view.LoadingView
import com.idunnololz.summit.view.swipeRefreshLayout.SwipeRefreshLayout

fun LoadingView.showProgressBarIfNeeded(swipeRefreshLayout: SwipeRefreshLayout) {
  if (swipeRefreshLayout.isRefreshing) {
    hideAll()
  } else {
    showProgressBar()
  }
}
