package com.idunnololz.summit.lemmy

import android.content.Context
import com.idunnololz.summit.util.PreferenceUtil
import com.idunnololz.summit.util.moshi
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RecentCommunityManager @Inject constructor(
    @ApplicationContext context: Context,
) {
    companion object {
        private const val MAX_RECENTS = 3
        private const val PREF_KEY_RECENT_SUBREDDITS = "PREF_KEY_RECENT_SUBREDDITS"

        /**
         * Set of subreddits we do not want to have on the recents list...
         */
        private val RECENTS_BLACKLIST = setOf(
            "r/all",
            "r/popular",
            ""
        )
    }

    private val adapter = moshi.adapter<List<Community>>(
        Types.newParameterizedType(List::class.java, Community::class.java))

    /**
     * Priority queue where the last item is the most recent recent.
     */
    private var _recentCommunities: LinkedHashMap<String, Community>? = null

    fun getRecentCommunities(): List<Community> = getRawRecents().values.reversed().toList()

    fun addRecentCommunity(community: Community) {
        val keyLower = community.getKey()
        if (RECENTS_BLACKLIST.contains(keyLower)) {
            return
        }

        val recents = getRawRecents()

        // move item to front...
        recents.remove(keyLower)
        recents[keyLower] = community

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

        // serialize
        PreferenceUtil.preferences.edit()
            .putString(PREF_KEY_RECENT_SUBREDDITS, adapter.toJson(recents.values.toList()))
            .apply()
    }

    private fun getRawRecents(): LinkedHashMap<String, Community> {
        val recentCommunities = _recentCommunities
        if (recentCommunities != null) {
            return recentCommunities
        }

        val json = PreferenceUtil.preferences.getString(PREF_KEY_RECENT_SUBREDDITS, null)
            ?: return LinkedHashMap()

        return adapter.fromJson(
            json
        )?.let {
            it.associateByTo(LinkedHashMap()) {
                it.getKey()
            }.also {
                _recentCommunities = it
            }
        } ?: LinkedHashMap()
    }
}