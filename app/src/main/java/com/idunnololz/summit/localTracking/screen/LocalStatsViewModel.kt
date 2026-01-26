package com.idunnololz.summit.localTracking.screen

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.RoomRawQuery
import com.idunnololz.summit.account.AccountManager
import com.idunnololz.summit.account.asAccount
import com.idunnololz.summit.api.AccountAwareLemmyClient
import com.idunnololz.summit.api.dto.lemmy.GetPersonDetailsResponse
import com.idunnololz.summit.api.dto.lemmy.Person
import com.idunnololz.summit.lemmy.CommunityRef
import com.idunnololz.summit.localTracking.TrackedAction
import com.idunnololz.summit.localTracking.TrackingEvent
import com.idunnololz.summit.localTracking.TrackingEventsDao
import com.idunnololz.summit.localTracking.getTableSize
import com.idunnololz.summit.util.StatefulLiveData
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.cbor.Cbor
import kotlinx.serialization.decodeFromByteArray
import javax.inject.Inject

@HiltViewModel
class LocalStatsViewModel @Inject constructor(
  private val accountManager: AccountManager,
  private val trackingEventsDao: TrackingEventsDao,
  private val accountAwareLemmyClient: AccountAwareLemmyClient,
) : ViewModel() {

  data class Model(
    val events: Int,
    val mostVisitedCommunities: List<MutableMap.MutableEntry<CommunityRef?, Int>>,
    val favoriteCommunities: List<MutableMap.MutableEntry<CommunityRef?, Int>>,
    val userInteractions: List<Pair<Person?, Int>>,
  )

  val data = StatefulLiveData<Model>()

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
        }
      }

      val topUserInteractions = userInteractions.entries
        .sortedByDescending { it.value }
        .take(10)

      val allPeopleData = fetchPeople(topUserInteractions.mapNotNull { it.key })

      data.postValue(
        Model(
          events = events.size,
          mostVisitedCommunities = mostVisitedCommunities.entries
            .sortedByDescending { it.value },
          favoriteCommunities = mostVisitedCommunitiesByPost.entries
            .sortedByDescending { it.value },
          userInteractions = topUserInteractions
            .map {
              val key = it.key
              if (key != null) {
                val user = allPeopleData[key]

                user?.getOrNull()?.person_view?.person to it.value
              } else {
                null to it.value
              }
            },
        )
      )
    }
  }

  private suspend fun fetchPeople(personIds: List<Long>): Map<Long, Result<GetPersonDetailsResponse>> {
    return personIds
      .map {
        viewModelScope.async(Dispatchers.IO) {
          it to accountAwareLemmyClient.fetchPersonByIdWithRetry(it, force = false)
        }
      }
      .awaitAll()
      .associate { it }
  }
}