package com.idunnololz.summit.offline

import android.util.Log
import android.view.View

class OfflineViewTargetRegistrationManager(
  private val offlineManager: OfflineManager,
) : View.OnAttachStateChangeListener {

  var currentRequest: OfflineManager.Registration? = null

  fun cancelRequest() {
    val currentRequest = currentRequest ?: return
    currentRequest.cancel(offlineManager)
    this.currentRequest = null
  }

  override fun onViewAttachedToWindow(v: View) {
    val currentRequest = currentRequest ?: return

    if (currentRequest.registered) {
      return
    }

    this.currentRequest = offlineManager.fetch(currentRequest)
  }

  override fun onViewDetachedFromWindow(v: View) {
    val currentRequest = currentRequest ?: return

    currentRequest.cancel(offlineManager)
    currentRequest.registered = false
  }
}