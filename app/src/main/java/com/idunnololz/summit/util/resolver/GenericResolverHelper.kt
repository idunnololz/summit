package com.idunnololz.summit.util.resolver

import com.idunnololz.summit.api.dto.lemmy.Person
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class GenericResolverHelper<T>(
  private val coroutineScope: CoroutineScope,
  private val fetchObject: suspend (Long, Boolean) -> Result<T>,
) {

  var objectDictionary = mapOf<Long, Result<T>>()
  val onObjectDictionaryChanged = MutableSharedFlow<Unit>()

  private var fetchingObject = setOf<Long>()

  private val mutex = Mutex()

  fun fetchObject(
    objectId: Long,
    force: Boolean,
  ) {
    coroutineScope.launch(Dispatchers.IO) {
      if (fetchingObject.contains(objectId) || objectDictionary.contains(objectId)) {
        return@launch
      }

      mutex.withLock {
        if (fetchingObject.contains(objectId) || objectDictionary.contains(objectId)) {
          return@launch
        }

        fetchingObject += objectId
      }

      val obj = fetchObject.invoke(objectId, force)

      mutex.withLock {
        objectDictionary = objectDictionary + (objectId to obj)
        fetchingObject -= objectId

        onObjectDictionaryChanged.emit(Unit)
      }
    }
  }
}