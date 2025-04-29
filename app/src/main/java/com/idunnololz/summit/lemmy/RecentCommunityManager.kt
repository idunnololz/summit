package com.idunnololz.summit.lemmy

import android.content.SharedPreferences
import android.util.Log
import androidx.core.content.edit
import com.idunnololz.summit.api.LemmyApiClient
import com.idunnololz.summit.coroutine.CoroutineScopeFactory
import com.idunnololz.summit.preferences.SharedPreferencesManager
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
    private val lemmyApiClientFactory: LemmyApiClient.Factory,
    private val coroutineScopeFactory: CoroutineScopeFactory,
    private val sharedPreferencesManager: SharedPreferencesManager,
    private val json: Json,
) {
    companion object {
        private const val TAG = "RecentCommunityManager"

        private const val MAX_RECENTS = 3
        private const val PREF_KEY_RECENT_COMMUNITIES = "PREF_KEY_RECENT_COMMUNITIES"
    }

    private val lemmyApiClient = lemmyApiClientFactory.create()

    private val coroutineScope = coroutineScopeFactory.create()

    /**
     * Priority queue where the last item is the most recent recent.
     */
    private var _recentCommunities: LinkedHashMap<String, CommunityHistoryEntry>? = null

    init {
        val preferences = sharedPreferencesManager.currentSharedPreferences
        if (preferences.contains(PREF_KEY_RECENT_COMMUNITIES)) {
            val str = preferences.getString(PREF_KEY_RECENT_COMMUNITIES, "")

            statePreferences.edit {
                putString(PREF_KEY_RECENT_COMMUNITIES, str)
            }
            preferences.edit {
                remove(PREF_KEY_RECENT_COMMUNITIES)
            }
        }
    }

    fun getRecentCommunities(): List<CommunityHistoryEntry> =
        getRecents().values.sortedByDescending { it.ts }

    fun addRecentCommunity(communityRef: CommunityRef, iconUrl: String? = null) {
        if (communityRef is CommunityRef.All ||
            communityRef is CommunityRef.Subscribed ||
            communityRef is CommunityRef.Local
        ) {
            // These communities are always at the top anyways...
            return
        }

        val key = communityRef.getKey()
        Log.d(TAG, "Add recent community: $key")

        val recents = getRecents()

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
        val resultsList = ArrayList(recents.values)

        coroutineScope.launch(Dispatchers.Default) {
            // serialize
            statePreferences.edit {
                putString(
                    PREF_KEY_RECENT_COMMUNITIES,
                    json.encodeToString(RecentCommunityData(resultsList)),
                )
            }
        }

        if (communityRef is CommunityRef.CommunityRefByName && iconUrl == null) {
            fetchRecentIcon(communityRef)
        }
    }

    private fun fetchRecentIcon(communityRef: CommunityRef.CommunityRefByName) {
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
                    addRecentCommunity(communityRef, iconUrl)
                }
            }
        }
    }

    private fun getRecents(): LinkedHashMap<String, CommunityHistoryEntry> {
        val recentCommunities = _recentCommunities
        if (recentCommunities != null) {
            return recentCommunities
        }
        val jsonStr = statePreferences.getString(PREF_KEY_RECENT_COMMUNITIES, null)
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
            _recentCommunities = it
        }
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
}
