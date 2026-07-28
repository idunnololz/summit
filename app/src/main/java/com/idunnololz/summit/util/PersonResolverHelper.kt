package com.idunnololz.summit.util

import androidx.lifecycle.viewModelScope
import com.idunnololz.summit.api.AccountAwareLemmyClient
import com.idunnololz.summit.api.dto.lemmy.Person
import com.idunnololz.summit.localTracking.screen.list.LocalStatsListModel
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.collections.get

class PersonResolverHelper @AssistedInject constructor(
  @Assisted private val coroutineScope: CoroutineScope,
  private val accountAwareLemmyClient: AccountAwareLemmyClient,
) {


  @AssistedFactory
  interface Factory {
    fun create(coroutineScope: CoroutineScope): PersonResolverHelper
  }

  var personDictionary = mapOf<Long, Result<Person>>()
  val onPersonDictionaryChanged = MutableSharedFlow<Unit>()

  private var fetchingPerson = setOf<Long>()

  private val mutex = Mutex()

  fun fetchPerson(
    personId: Long,
  ) {
    coroutineScope.launch(Dispatchers.IO) {
      if (fetchingPerson.contains(personId) || personDictionary.contains(personId)) {
        return@launch
      }

      mutex.withLock {
        if (fetchingPerson.contains(personId) || personDictionary.contains(personId)) {
          return@launch
        }

        fetchingPerson += personId
      }

      val person = accountAwareLemmyClient.fetchPersonByIdWithRetry(personId, force = false)
        .map { it.personView.person }

      mutex.withLock {
        personDictionary = personDictionary + (personId to person)
        fetchingPerson -= personId

        onPersonDictionaryChanged.emit(Unit)
      }
    }
  }
}