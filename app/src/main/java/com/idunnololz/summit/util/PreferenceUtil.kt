package com.idunnololz.summit.util

import android.content.Context
import android.content.SharedPreferences
import com.idunnololz.summit.lemmy.CommunityRef
import com.idunnololz.summit.lemmy.community.CommunityLayout

import java.util.HashSet
import java.util.StringTokenizer

object PreferenceUtil {

    lateinit var preferences: SharedPreferences
        private set

    const val DEFAULT_PREF = "pref"

    const val OFFLINE_MODE_AUTO = 0
    const val OFFLINE_MODE_ON = 1
    const val OFFLINE_MODE_OFF = 2

    const val DEFAULT_CHAMPION_ITEM_SIZE = 68

    const val KEY_DEFAULT_PAGE = "KEY_DEFAULT_PAGE"
    const val KEY_DEFAULT_PAGE_THUMBNAIL_SIGNATURE = "KEY_DEFAULT_PAGE_THUMBNAIL_SIGNATURE"
    const val KEY_THEME = "theme"

    const val WHICH_GAME_LAYOUT_LOL = 0x1 shl 2
    const val WHICH_GAME_LAYOUT_TFT = 0x1 shl 3
    const val WHICH_GAME_LAYOUT_LOL_AND_TFT = WHICH_GAME_LAYOUT_LOL or WHICH_GAME_LAYOUT_TFT

    /**
     * Value representing the next time to check if disabled_ads is still purchased.
     */
    const val NEXT_PURCHASE_SYNC = "next_purchase_sync"


    const val NEXT_UPDATE_CHECK = "next_update_check"

    /**
     * Value representing the next time to check if a new version of LoL is available.
     */
    const val NEXT_PATCH_UPDATE_CHECK = "next_patch_update_check"

    const val LAST_AD_STATUS_SYNC = "last_ad_status_sync"

    /**
     * Timestamp based off of [System.currentTimeMillis] of when to turn off ads until.
     */
    const val HALT_ADS_UNTIL = "aaaa_bbbb"

    /**
     * Ad status. See [SyncAdResult].
     */
    const val KEY_AD_STATUS = "aaaa_dddd"

    const val ADS_TRACKER_DATA = "aaaa_cccc"

    const val KEY_OAUTH_TOKEN = "aaaa_dddd"
    const val KEY_REFRESH_TOKEN = "aaaa_ddde"
    const val KEY_STATE_TOKEN = "aaaa_eeee"

    const val KEY_USER_ID = "user_id"

    /**
     * Scheduling options
     */
    const val KEY_ENABLE_OFFLINE_SCHEDULE = "enable_offline_schedule"
    const val KEY_OFFLINE_SCHEDULE = "offline_schedule"

    const val KEY_LAST_SUCCESSFUL_OFFLINE_DOWNLOAD = "last_successful_offline_download"

    private const val KEY_SUBREDDIT_LAYOUT = "KEY_SUBREDDIT_LAYOUT"
    private const val KEY_OFFLINE_STORAGE_CAP_BYTES = "KEY_OFFLINE_STORAGE_CAP_BYTES"
    private const val KEY_VIDEO_PLAYER_ROTATION_LOCKED = "KEY_VIDEO_PLAYER_ROTATION_LOCKED"

    fun initialize(context: Context) {
        if (!::preferences.isInitialized) {
            preferences = context.getSharedPreferences(DEFAULT_PREF, Context.MODE_PRIVATE)
        }
    }

    fun putList(prefs: SharedPreferences, list: List<Int>, arrayName: String) {
        prefs.edit().apply {
            putInt(arrayName + "_size", list.size)
            for (i in list.indices) {
                putInt(arrayName + "_" + i, list[i])
            }
        }.apply()
    }

    fun getArray(
        prefs: SharedPreferences,
        arrayName: String,
        defaultArr: IntArray
    ): IntArray {
        val size = prefs.getInt(arrayName + "_size", -1)
        if (size == -1) return defaultArr
        return IntArray(size) { i ->
            prefs.getInt(arrayName + "_" + i, -1)
        }
    }

    fun intSetToString(set: Set<Int>): String {
        return StringBuilder().apply {
            for (i in set) {
                append(i)
                append(",")
            }
        }.toString()
    }

    fun stringToIntSet(str: String): Set<Int> {
        val st = StringTokenizer(str, ",")
        val set = HashSet<Int>()
        while (st.hasMoreTokens()) {
            set.add(Integer.parseInt(st.nextToken()))
        }
        return set
    }


    fun getSubredditLayout(): CommunityLayout =
        try {
            CommunityLayout.valueOf(preferences.getString(KEY_SUBREDDIT_LAYOUT, null) ?: "")
        } catch (e: IllegalArgumentException) {
            CommunityLayout.LIST
        }

    fun setSubredditLayout(layout: CommunityLayout) {
        preferences.edit()
            .putString(KEY_SUBREDDIT_LAYOUT, layout.name)
            .apply()
    }

    fun getOfflineStorageCap(): Long =
        preferences.getLong(KEY_OFFLINE_STORAGE_CAP_BYTES, 1073741824 /* 1GB */)

    fun setOfflineStorageCap(cap: Long) {
        preferences.edit()
            .putLong(KEY_OFFLINE_STORAGE_CAP_BYTES, cap)
            .apply()
    }

    fun isVideoPlayerRotationLocked(): Boolean =
        preferences.getBoolean(KEY_VIDEO_PLAYER_ROTATION_LOCKED, false)

    fun setVideoPlayerRotationLocked(b: Boolean) {
        preferences.edit()
            .putBoolean(KEY_VIDEO_PLAYER_ROTATION_LOCKED, b)
            .apply()
    }
}
