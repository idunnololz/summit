package com.idunnololz.summit.reddit

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.idunnololz.summit.util.PreferenceUtil
import com.idunnololz.summit.util.Utils
import java.util.*
import kotlin.collections.LinkedHashSet

class RecentSubredditsManager(
    context: Context,
    val gson: Gson = Utils.gson
) {
    companion object {
        lateinit var instance: RecentSubredditsManager

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

        fun initialize(context: Context) {
            instance = RecentSubredditsManager(context)
        }
    }

    /**
     * Priority queue where the last item is the most recent recent.
     */
    private var _recentSubreddits: LinkedHashSet<String>? = null

    fun getRecentSubreddits(): List<String> = getRawRecents().reversed().toList()

    fun addRecentSubreddit(key: String) {
        val keyLower = key.toLowerCase(Locale.US)
        if (RECENTS_BLACKLIST.contains(keyLower)) {
            return
        }

        val recents = getRawRecents()

        // move item to front...
        recents.remove(keyLower)
        recents.add(keyLower)

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
            .putString(PREF_KEY_RECENT_SUBREDDITS, gson.toJson(recents.toList()))
            .apply()
    }

    private fun getRawRecents(): LinkedHashSet<String> =
        _recentSubreddits ?: gson.fromJson<List<String>>(
            PreferenceUtil.preferences.getString(PREF_KEY_RECENT_SUBREDDITS, "[]"),
            object : TypeToken<List<String>>() {}.type
        ).map { it.toLowerCase(Locale.US) }.let {
            LinkedHashSet<String>().apply {
                addAll(it)
            }.also {
                _recentSubreddits = it
            }
        }
}