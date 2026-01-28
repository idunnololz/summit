package com.idunnololz.summit.localTracking.community

import android.util.Log
import arrow.core.Either
import com.idunnololz.summit.account.AccountManager
import com.idunnololz.summit.api.AccountAwareLemmyClient
import com.idunnololz.summit.coroutine.CoroutineScopeFactory
import com.idunnololz.summit.lemmy.CommunityRef
import com.idunnololz.summit.lemmy.utils.stateStorage.GlobalStateStorage
import com.idunnololz.summit.localTracking.LocalTracker
import com.idunnololz.summit.localTracking.OnTrackingEventListener
import com.idunnololz.summit.localTracking.TrackedAction
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CommunityTracker @Inject constructor(
  private val accountManager: AccountManager,
  private val coroutineScopeFactory: CoroutineScopeFactory,
  private val localTracker: LocalTracker,
  private val communityTrackerDao: CommunityTrackerDao,
  private val globalStateStorage: GlobalStateStorage,
  private val accountAwareLemmyClient: AccountAwareLemmyClient,
) {

  companion object {
    private const val TAG = "CommunityTracker"

    // 1 day
    private const val MIN_METADATA_UPDATE_TIME_MS = 24 * 60 * 60 * 1000
  }

  private val coroutineScope = coroutineScopeFactory.create()

  fun init() {
    if (!globalStateStorage.migratedTrackingDataToCommunityData) {
      coroutineScope.launch {
        globalStateStorage.migratedTrackingDataToCommunityData = true

        val events = localTracker.getEvents(TrackedAction.VIEW)

        events.forEach {
          it.communityRef?.let {
            this@CommunityTracker.onViewCommunity(it)
          }
        }
      }
    }

    localTracker.registerOnTrackingEventListener(object : OnTrackingEventListener {
      override fun onDeleteAll() {
        coroutineScope.launch {
          communityTrackerDao.deleteAll()
        }
      }

      override fun onViewCommunity(communityRef: CommunityRef) {
        this@CommunityTracker.onViewCommunity(communityRef)
      }
    })
  }

  private fun onViewCommunity(communityRef: CommunityRef) {
    coroutineScope.launch {
      val entries = communityTrackerDao.getEntryByCommunityRef(communityRef)
      var entry: CommunityStatEntry?

      if (entries.isEmpty()) {
        val ts = System.currentTimeMillis()

        entry = CommunityStatEntry(
          id = 0,
          userId = accountManager.currentAccount.value.id,
          lastUpdateTs = ts,
          createdTs = ts,
          communityRef = communityRef,
          icon = null,
          lastMetadataUpdateTs = 0,
          hits = 1,
        )

        val id = communityTrackerDao.insert(entry)

        entry = entry.copy(id = id)
      } else {
        entry = entries.first()
        entry = entry.copy(
          lastUpdateTs = System.currentTimeMillis(),
          communityRef = communityRef,
          hits = entry.hits + 1,
        )

        communityTrackerDao.insert(entry)
      }

      fetchCommunityInfoIfNeeded(entry)
    }
  }

  private fun fetchCommunityInfoIfNeeded(entry: CommunityStatEntry) {
    coroutineScope.launch {
      when (entry.communityRef) {
        is CommunityRef.All -> {}
        is CommunityRef.AllSubscribed -> {}
        is CommunityRef.CommunityRefByName -> {
          if (System.currentTimeMillis() - entry.lastMetadataUpdateTs < MIN_METADATA_UPDATE_TIME_MS) {
            Log.d(TAG, "Community ${entry.communityRef} data is up to date already.")
            return@launch
          }

          accountAwareLemmyClient
            .fetchCommunityWithRetry(
              Either.Right(entry.communityRef.getServerId(accountAwareLemmyClient.apiClient.instance)),
              false)
            .onSuccess {
              Log.d(TAG, "Community ${entry.communityRef} data updated.")
              communityTrackerDao.insert(
                entry.copy(
                  icon = it.community_view.community.icon,
                  lastMetadataUpdateTs = System.currentTimeMillis(),
                  mau = it.community_view.counts.users_active_month,
                )
              )
            }
        }
        is CommunityRef.Local -> {}
        is CommunityRef.ModeratedCommunities -> {}
        is CommunityRef.MultiCommunity -> {}
        is CommunityRef.Subscribed -> {}
      }
    }
  }

  suspend fun getCommunityStats(): List<CommunityStatEntry> {
    return communityTrackerDao.getTop1000()
  }
}