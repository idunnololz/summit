package com.idunnololz.summit.localTracking.screen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.idunnololz.summit.account.AccountManager
import com.idunnololz.summit.account.asAccount
import com.idunnololz.summit.api.AccountAwareLemmyClient
import com.idunnololz.summit.api.dto.lemmy.Person
import com.idunnololz.summit.lemmy.CommunityRef
import com.idunnololz.summit.localTracking.TrackedAction
import com.idunnololz.summit.localTracking.TrackingEvent
import com.idunnololz.summit.localTracking.TrackingEventsDao
import com.idunnololz.summit.localTracking.screen.list.LocalStatsListModel
import com.idunnololz.summit.models.GetPersonDetailsResponse
import com.idunnololz.summit.util.resolver.PersonResolverHelper
import com.idunnololz.summit.util.StatefulLiveData
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.serialization.cbor.Cbor
import kotlinx.serialization.decodeFromByteArray
import kotlin.collections.get
import kotlin.collections.map

@HiltViewModel
class LocalStatsViewModel @Inject constructor(
  private val accountManager: AccountManager,
  private val trackingEventsDao: TrackingEventsDao,
  private val personResolverHelperFactory: PersonResolverHelper.Factory,
) : ViewModel() {

  data class Model(
    val events: Int,
    val mostVisitedCommunities: List<MutableMap.MutableEntry<CommunityRef?, Int>>,
    val favoriteCommunities: List<MutableMap.MutableEntry<CommunityRef?, Int>>,
    val userInteractions: List<UserInteraction>,
  )

  data class UserInteraction(
    val personId: Long?,
    val personResult: Result<Person>?,
    val count: Int,
  )

  val personResolverHelper = personResolverHelperFactory.create(viewModelScope)

  val data = StatefulLiveData<Model>()

  init {
    viewModelScope.launch {
      personResolverHelper.onPersonDictionaryChanged.collect {
        val model = data.valueOrNull ?: return@collect
        data.postValue(
          model.copy(
            userInteractions = model.userInteractions.map {
              it.copy(
                personResult = personResolverHelper.personDictionary[it.personId],
              )
            }
          )
        )
      }
    }
  }

  fun loadData(force: Boolean) {
    val currentAccountId = accountManager.currentAccount.asAccount?.id ?: return

    if (!force && data.isLoaded) {
      return
    }

    data.setIsLoading()

    viewModelScope.launch {
      val events = trackingEventsDao.getAll()
        .filter {
          it.userId == currentAccountId
        }

      val mostVisitedCommunities = mutableMapOf<CommunityRef?, Int>()
      val mostVisitedCommunitiesByPost = mutableMapOf<CommunityRef?, Int>()
      val userInteractions = mutableMapOf<Long?, Int>()

      events.forEach {
        val trackingEvent: TrackingEvent = Cbor.decodeFromByteArray(it.trackingEventCbor)
        when (trackingEvent.action) {
          TrackedAction.UPVOTE -> {
            userInteractions[trackingEvent.targetUserId] =
              (userInteractions[trackingEvent.targetUserId] ?: 0) + 1
          }
          TrackedAction.DOWNVOTE -> {
            userInteractions[trackingEvent.targetUserId] =
              (userInteractions[trackingEvent.targetUserId] ?: 0) + 1
          }
          TrackedAction.CLEAR_VOTE -> {}
          TrackedAction.VIEW -> {
            if (trackingEvent.postId == null) {
              mostVisitedCommunities[trackingEvent.communityRef] =
                (mostVisitedCommunities[trackingEvent.communityRef] ?: 0) + 1
            } else {
              userInteractions[trackingEvent.targetUserId] =
                (userInteractions[trackingEvent.targetUserId] ?: 0) + 1
              mostVisitedCommunitiesByPost[trackingEvent.communityRef] =
                (mostVisitedCommunitiesByPost[trackingEvent.communityRef] ?: 0) + 1
            }
          }
          TrackedAction.REPLY -> {
            userInteractions[trackingEvent.targetUserId] =
              (userInteractions[trackingEvent.targetUserId] ?: 0) + 1
          }
          TrackedAction.DELETE_REPLY -> {}
          TrackedAction.POST -> {}
        }
      }

      val topUserInteractions = userInteractions.entries
        .sortedByDescending { it.value }
        .take(10)

      topUserInteractions.forEach {
        val personId = it.key
        if (personId!= null) {
          personResolverHelper.fetchPerson(personId, force = false)
        }
      }

      data.postValue(
        Model(
          events = events.size,
          mostVisitedCommunities = mostVisitedCommunities.entries
            .sortedByDescending { it.value }
            .take(10),
          favoriteCommunities = mostVisitedCommunitiesByPost.entries
            .sortedByDescending { it.value }
            .take(10),
          userInteractions = topUserInteractions
            .map {
              val key = it.key

              UserInteraction(
                it.key,
                personResolverHelper.personDictionary[key],
                it.value
              )
            },
        ),
      )
    }
  }
}
