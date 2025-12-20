package com.idunnololz.summit.lemmy

import android.content.SharedPreferences
import android.util.Log
import androidx.core.content.edit
import com.idunnololz.summit.api.LemmyApiClient
import com.idunnololz.summit.coroutine.CoroutineScopeFactory
import com.idunnololz.summit.localTracking.LocalTracker
import com.idunnololz.summit.preferences.Preferences
import com.idunnololz.summit.preferences.StateSharedPreference
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Singleton
class RecentCommunityManager @Inject constructor(
  @StateSharedPreference private val statePreferences: SharedPreferences,
  private val preferences: Preferences,
  private val lemmyApiClientFactory: LemmyApiClient.Factory,
  private val coroutineScopeFactory: CoroutineScopeFactory,
  private val tracker: LocalTracker,
  private val json: Json,
) {
  companion object {
    private const val TAG = "RecentCommunityManager"

    private const val MAX_RECENTS = 10
    private const val PREF_KEY_RECENT_COMMUNITIES = "PREF_KEY_RECENT_COMMUNITIES"
    private const val PREF_KEY_RECENT_COMMUNITIES_POSTED_TO =
      "PREF_KEY_RECENT_COMMUNITIES_POSTED_TO"
  }

  private val lemmyApiClient = lemmyApiClientFactory.create()

  private val coroutineScope = coroutineScopeFactory.create()

  /**
   * Priority queue where the last item is the most recent recent.
   */
  private val recentCommunities = RecentCommunityCache(PREF_KEY_RECENT_COMMUNITIES)

  private val recentCommunitiesPostedTo =
    RecentCommunityCache(PREF_KEY_RECENT_COMMUNITIES_POSTED_TO)

  init {
  }

  fun getRecentCommunitiesVisited(): List<CommunityHistoryEntry> =
    recentCommunities.getRecentsOrLoad()
      .values
      .sortedByDescending { it.ts }

  fun addRecentCommunityVisited(communityRef: CommunityRef, iconUrl: String? = null) =
    recentCommunities.addRecent(communityRef, iconUrl)

  fun removeRecentCommunityVisited(communityRef: CommunityRef) =
    recentCommunities.removeRecent(communityRef)

  fun getRecentCommunitiesPostedTo(): List<CommunityHistoryEntry> =
    recentCommunitiesPostedTo.getRecentsOrLoad()
      .values
      .sortedByDescending { it.ts }

  fun addRecentCommunityPostedTo(communityRef: CommunityRef, iconUrl: String? = null) =
    recentCommunitiesPostedTo.addRecent(communityRef, iconUrl)

  fun removeRecentCommunityPostedTo(communityRef: CommunityRef) =
    recentCommunitiesPostedTo.removeRecent(communityRef)

  private fun RecentCommunityCache.addRecent(communityRef: CommunityRef, iconUrl: String? = null) {
    if (communityRef is CommunityRef.All ||
      communityRef is CommunityRef.Subscribed ||
      communityRef is CommunityRef.Local
    ) {
      // These communities are always at the top anyways...
      return
    }

    if (!preferences.saveRecentCommunities) {
      return
    }

    val key = communityRef.getKey()
    Log.d(TAG, "Add recent community: $key type: ${this.prefKey}")

    val recents = this.getRecentsOrLoad()

    // move item to front...
    recents.remove(key)
    recents[key] = CommunityHistoryEntry(
      communityRef,
      iconUrl,
      System.currentTimeMillis(),
    )

    if (recents.size > MAX_RECENTS) {
      var toRemove = recents.size - MAX_RECENTS
      val it = recents.iterator()
      while (it.hasNext()) {
        it.next()
        it.remove()
        toRemove--
        if (toRemove == 0) break
      }
    }

    saveChangedAsync()

    if (communityRef is CommunityRef.CommunityRefByName && iconUrl == null) {
      fetchRecentIcon(communityRef)
    }
  }

  private fun RecentCommunityCache.removeRecent(communityRef: CommunityRef) {
    val key = communityRef.getKey()
    val recents = this.getRecentsOrLoad()

    recents.remove(key)
    saveChangedAsync()
  }

  private fun RecentCommunityCache.fetchRecentIcon(communityRef: CommunityRef.CommunityRefByName) {
    val instance = communityRef.instance
      ?: return

    coroutineScope.launch {
      lemmyApiClient.changeInstance(instance)
      val iconUrl = lemmyApiClient.getCommunity(null, communityRef.name, instance, false)
        .fold(
          {
            it.community.icon
          },
          {
            null
          },
        )

      if (iconUrl != null) {
        withContext(Dispatchers.Main) {
          addRecent(communityRef, iconUrl)
        }
      }
    }
  }

  private fun RecentCommunityCache.getRecentsOrLoad(): LinkedHashMap<String, CommunityHistoryEntry> {
    val cache = this
    val recentCommunities = cache.recents
    if (recentCommunities != null) {
      return recentCommunities
    }
    val jsonStr = statePreferences.getString(cache.prefKey, null)
    val data = try {
      if (jsonStr != null) {
        json.decodeFromString<RecentCommunityData?>(jsonStr)
      } else {
        null
      }
    } catch (e: Exception) {
      Log.e(TAG, "Error loading recents", e)
      null
    }

    val map = LinkedHashMap<String, CommunityHistoryEntry>()
    if (data == null) {
      // do nothing
    } else {
      data.entries.forEach {
        map[it.key] = it
      }
    }

    return map.also {
      cache.recents = it
    }
  }

  private fun RecentCommunityCache.saveChangedAsync() {
    val cachedRecents = recents ?: return
    val resultsList = ArrayList(cachedRecents.values)

    coroutineScope.launch(Dispatchers.Default) {
      // serialize
      statePreferences.edit {
        putString(
          prefKey,
          json.encodeToString(RecentCommunityData(resultsList)),
        )
      }
    }
  }

  fun clear() {
    val recents = recentCommunities.getRecentsOrLoad()

    recents.clear()
    recentCommunities.saveChangedAsync()
  }

  @Serializable
  data class CommunityHistoryEntry(
    val communityRef: CommunityRef,
    val iconUrl: String?,
    val ts: Long?,
  ) {
    val key: String
      get() = communityRef.getKey()
  }

  @Serializable
  data class RecentCommunityData(
    val entries: List<CommunityHistoryEntry>,
  )

  class RecentCommunityCache(
    val prefKey: String,
  ) {
    var recents: LinkedHashMap<String, CommunityHistoryEntry>? = null
  }
}
