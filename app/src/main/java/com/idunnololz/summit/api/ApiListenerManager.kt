package com.idunnololz.summit.api

import javax.inject.Inject
import javax.inject.Singleton

typealias ApiListener = (Result<Any?>) -> Unit

@Singleton
class ApiListenerManager @Inject constructor() {
  private var listeners = listOf<ApiListener>()

  fun onRequestComplete(response: Result<Any?>) {
    val listeners = listeners
    for (listener in listeners) {
      listener(response)
    }
  }

  fun registerListener(listener: ApiListener) {
    listeners += listener
  }
}
