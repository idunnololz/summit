package com.idunnololz.summit.lemmy

import android.util.Log
import com.idunnololz.summit.util.PreferenceUtil
import com.idunnololz.summit.util.moshi
import com.squareup.moshi.JsonClass
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RecentCommunityManager @Inject constructor() {
    companion object {
        private const val TAG = "RecentCommunityManager"

        private const val MAX_RECENTS = 3
        private const val PREF_KEY_RECENT_COMMUNITIES = "PREF_KEY_RECENT_COMMUNITIES"
    }

    private val coroutineScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private val adapter = moshi.adapter(RecentCommunityData::class.java)

    /**
     * Priority queue where the last item is the most recent recent.
     */
    private var _recentSubreddits: LinkedHashMap<String, CommunityHistoryEntry>? = null

    fun getRecentCommunities(): List<CommunityHistoryEntry> =
        getRecents().values.reversed().toList()

    fun addRecentCommunity(communityRef: CommunityRef) {
        if (communityRef is CommunityRef.All ||
            communityRef is CommunityRef.Subscribed ||
            communityRef is CommunityRef.Local) {

            // These communities are always at the top anyways...
            return
        }

        val key = communityRef.getKey()
        Log.d(TAG, "Add recent community: ${key}")

        val recents = getRecents()

        // move item to front...
        recents.remove(key)
        recents[key] = CommunityHistoryEntry(communityRef)

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
            PreferenceUtil.preferences.edit()
                .putString(PREF_KEY_RECENT_COMMUNITIES, adapter.toJson(
                    RecentCommunityData(resultsList)
                ))
                .apply()
        }
    }

    private fun getRecents(): LinkedHashMap<String, CommunityHistoryEntry> {
        val recentSubreddits = _recentSubreddits
        if (recentSubreddits != null) {
            return recentSubreddits
        }
        val jsonStr = PreferenceUtil.preferences.getString(PREF_KEY_RECENT_COMMUNITIES, null)
        val data = try {
            if (jsonStr != null) {
                adapter.fromJson(jsonStr)
            } else {
                null
            }
        } catch (e: Exception) {
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
            _recentSubreddits = it
        }
    }

    @JsonClass(generateAdapter = true)
    data class CommunityHistoryEntry(
        val communityRef: CommunityRef
    ) {
        val key: String
            get() = communityRef.getKey()
    }

    @JsonClass(generateAdapter = true)
    data class RecentCommunityData(
        val entries: List<CommunityHistoryEntry>
    )
}