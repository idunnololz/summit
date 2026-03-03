package com.idunnololz.summit.preview

import kotlinx.coroutines.flow.MutableStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PeekImageManager @Inject constructor() {

  sealed interface PeekStatus {
    data object Idle : PeekStatus
    data class Started(val x: Int, val y: Int) : PeekStatus
  }

  val peekStatusFlow = MutableStateFlow<PeekStatus>(PeekStatus.Idle)

  val peekStatus
    get() = peekStatusFlow.value

  fun onPeek(x: Int, y: Int) {
    peekStatusFlow.value = PeekStatus.Started(x, y)
  }

  fun onStopPeek() {
    peekStatusFlow.value = PeekStatus.Idle
  }
}