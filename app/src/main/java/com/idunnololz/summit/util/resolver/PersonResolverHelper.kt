package com.idunnololz.summit.util.resolver

import com.idunnololz.summit.api.AccountAwareLemmyClient
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CoroutineScope

class PersonResolverHelper @AssistedInject constructor(
  @Assisted private val coroutineScope: CoroutineScope,
  private val accountAwareLemmyClient: AccountAwareLemmyClient,
) {

  @AssistedFactory
  interface Factory {
    fun create(coroutineScope: CoroutineScope): PersonResolverHelper
  }

  private val resolverHelper = GenericResolverHelper(
    coroutineScope,
    { personId, force ->
      accountAwareLemmyClient.fetchPersonByIdWithRetry(personId, force = force)
        .map { it.personView.person }
    },
  )

  val personDictionary
    get() = resolverHelper.objectDictionary
  val onPersonDictionaryChanged
    get() = resolverHelper.onObjectDictionaryChanged

  fun fetchPerson(personId: Long, force: Boolean) {
    resolverHelper.fetchObject(personId, force)
  }
}
