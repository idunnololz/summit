package com.idunnololz.summit.lemmy

import android.util.Log
import com.google.gson.reflect.TypeToken
import com.idunnololz.summit.coroutine.CoroutineScopeFactory
import com.idunnololz.summit.util.DirectoryHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import kotlinx.coroutines.runInterruptible
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

@OptIn(FlowPreview::class)
class DataBackedByCache<T> constructor(
  private val primaryKey: String,
  private val initialValue: T,
  private val clazz: Class<T>,
  private val directoryHelper: DirectoryHelper,
  private val coroutineScopeFactory: CoroutineScopeFactory,
) {

  companion object {
    private const val TAG = "ListBackedByCache"
  }

  @Singleton
  class Factory @Inject constructor(
    private val directoryHelper: DirectoryHelper,
    private val coroutineScopeFactory: CoroutineScopeFactory,
  ) {
    inline fun <reified T> create(
      primaryKey: String,
      initialValue: T,
    ): DataBackedByCache<T> =
      create(primaryKey, initialValue, T::class.java)

    fun <T> create(
      primaryKey: String,
      initialValue: T,
      clazz: Class<T>,
    ): DataBackedByCache<T> =
      DataBackedByCache(
        primaryKey = primaryKey,
        initialValue = initialValue,
        clazz = clazz,
        directoryHelper = directoryHelper,
        coroutineScopeFactory = coroutineScopeFactory,
      )
  }

  private val coroutineScope = coroutineScopeFactory.create()
  private val mutex = Mutex()

  private val source = MutableStateFlow<T>(initialValue)
  @Volatile private var writeJobIndex = 0
  var restored = false

  var value
    get() = source.value
    set(value) {
      source.value = value
    }

  var commitChanges: Boolean = true

  init {
    coroutineScope.launch {
      source
        .drop(1)
        .debounce(1000)
        .collect {
          val nextWriteJobIndex = writeJobIndex + 1
          writeJobIndex = nextWriteJobIndex

          if (commitChanges) {
            commit(it, nextWriteJobIndex)
          }
        }
    }
  }

  suspend fun tryRestore() {
    if (restored) return

    mutex.withLock {
      if (restored) return

      restored = true

      val restored = runInterruptible {
        directoryHelper.listsDiskCache.getCachedObject<T>(
          primaryKey,
          clazz
        )
      } ?: return

      source.value = restored

      Log.d(TAG, "[${primaryKey}] Successfully restored!")
    }
  }

  private fun commit(value: T?, writeJobIndex: Int) {
    coroutineScope.launch(Dispatchers.IO) {
      mutex.withLock {
        if (writeJobIndex < this@DataBackedByCache.writeJobIndex) {
          return@launch
        }

        runInterruptible {
          directoryHelper.listsDiskCache.cacheObject(primaryKey, value, clazz)
          directoryHelper.listsDiskCache.printDebugInfo()
        }
      }
    }
  }

}