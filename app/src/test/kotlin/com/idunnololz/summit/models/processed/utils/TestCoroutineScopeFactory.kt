package com.idunnololz.summit.models.processed.utils

import com.idunnololz.summit.coroutine.CoroutineScopeFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.test.TestDispatcher

class TestCoroutineScopeFactory(
  private val testDispatcher: TestDispatcher,
) : CoroutineScopeFactory {
  override fun create(): CoroutineScope = CoroutineScope(testDispatcher)

  override fun createConfined(): CoroutineScope {
    TODO("Not yet implemented")
  }
}
