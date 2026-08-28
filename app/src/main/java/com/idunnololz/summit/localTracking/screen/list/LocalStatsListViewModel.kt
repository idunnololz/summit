package com.idunnololz.summit.localTracking.screen.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.idunnololz.summit.account.AccountManager
import com.idunnololz.summit.account.asAccount
import com.idunnololz.summit.lemmy.CommunityRef
import com.idunnololz.summit.localTracking.TrackedAction
import com.idunnololz.summit.localTracking.TrackingEvent
import com.idunnololz.summit.localTracking.TrackingEventsDao
import com.idunnololz.summit.util.StatefulLiveData
import com.idunnololz.summit.util.resolver.PersonResolverHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlin.collections.get
import kotlinx.coroutines.launch
import kotlinx.serialization.cbor.Cbor
import kotlinx.serialization.decodeFromByteArray

@HiltViewModel
class LocalStatsListViewModel @Inject constructor(
  private val accountManager: AccountManager,
  private val trackingEventsDao: TrackingEventsDao,
  private val personResolverHelperFactory: PersonResolverHelper.Factory,
) : ViewModel() {

  private val personResolverHelper = personResolverHelperFactory.create(viewModelScope)

  val data = StatefulLiveData<LocalStatsListModel>()

  init {
    viewModelScope.launch {
      personResolverHelper.onPersonDictionaryChanged.collect {
        val model = data.valueOrNull ?: return@collect
        data.postValue(
          model.copy(
            items = model.items.map {
              when (it) {
                is LocalStatsListModel.Item.CommunityStatItem -> it
                is LocalStatsListModel.Item.PersonStatItem -> it.copy(
                  personResult = personResolverHelper.personDictionary[it.personId],
                )
              }
            },
          ),
        )
      }
    }
  }

  fun loadStats(type: LocalStatsListFragment.LocalStatsListType, force: Boolean = false) {
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

      for (event in events) {
        val trackingEvent: TrackingEvent = Cbor.decodeFromByteArray(event.trackingEventCbor)
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

      data.postValue(
        LocalStatsListModel(
          items = when (type) {
            LocalStatsListFragment.LocalStatsListType.FrequentedCommunities -> {
              mostVisitedCommunities.entries
                .sortedByDescending { it.value }
                .map {
                  LocalStatsListModel.Item.CommunityStatItem(
                    communityRef = it.key,
                    count = it.value,
                  )
                }
            }
            LocalStatsListFragment.LocalStatsListType.FavoriteCommunities -> {
              mostVisitedCommunitiesByPost.entries
                .sortedByDescending { it.value }
                .map {
                  LocalStatsListModel.Item.CommunityStatItem(
                    communityRef = it.key,
                    count = it.value,
                  )
                }
            }
            LocalStatsListFragment.LocalStatsListType.UserInteractions -> {
              userInteractions.entries
                .sortedByDescending { it.value }
                .map {
                  LocalStatsListModel.Item.PersonStatItem(
                    personId = it.key,
                    personResult = personResolverHelper.personDictionary[it.key],
                    count = it.value,
                  )
                }
            }
          },
        ),
      )
    }
  }

  fun fetchPerson(personId: Long, force: Boolean) {
    personResolverHelper.fetchPerson(personId, force)
  }
}
