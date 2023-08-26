package com.idunnololz.summit.util

import android.content.Context
import android.content.SharedPreferences
import java.util.HashSet
import java.util.StringTokenizer

object PreferenceUtil {

    var usingCustomFont: Boolean = false
    lateinit var preferences: SharedPreferences
        private set

    const val DEFAULT_PREF = "pref"

    const val OFFLINE_MODE_AUTO = 0
    const val OFFLINE_MODE_ON = 1
    const val OFFLINE_MODE_OFF = 2

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
     * START OF DEPRECATED KEYS
     */
    const val KEY_COMPATIBILITY_MODE = "KEY_COMPATIBILITY_MODE"

    /**
     * END OF DEPRECATED KEYS
     */

    /**
     * Scheduling options
     */
    const val KEY_ENABLE_OFFLINE_SCHEDULE = "enable_offline_schedule"
    const val KEY_OFFLINE_SCHEDULE = "offline_schedule"

    const val KEY_LAST_SUCCESSFUL_OFFLINE_DOWNLOAD = "last_successful_offline_download"

    const val KEY_SUBREDDIT_LAYOUT = "KEY_SUBREDDIT_LAYOUT"
    const val KEY_POST_UI_CONFIG_COMPACT = "KEY_POST_UI_CONFIG_COMPACT"
    const val KEY_POST_UI_CONFIG_LIST = "KEY_POST_UI_CONFIG_LIST"
    const val KEY_POST_UI_CONFIG_LARGE_LIST = "KEY_POST_UI_CONFIG_LARGE_LIST"
    const val KEY_POST_UI_CONFIG_CARD = "KEY_POST_UI_CONFIG_CARD"
    const val KEY_POST_UI_CONFIG_CARD2 = "KEY_POST_UI_CONFIG_CARD2"
    const val KEY_POST_UI_CONFIG_CARD3 = "KEY_POST_UI_CONFIG_CARD3"
    const val KEY_POST_UI_CONFIG_FULL = "KEY_POST_UI_CONFIG_FULL"

    const val KEY_BASE_THEME = "KEY_BASE_THEME"

    const val KEY_USE_MATERIAL_YOU = "KEY_USE_MATERIAL_YOU"

    const val KEY_USE_BLACK_THEME = "KEY_USE_BLACK_THEME"

    const val KEY_POST_AND_COMMENTS_UI_CONFIG = "KEY_POST_AND_COMMENTS_UI_CONFIG"

    const val KEY_MARK_POSTS_AS_READ_ON_SCROLL = "KEY_MARK_POSTS_AS_READ_ON_SCROLL"

    const val KEY_USE_GESTURE_ACTIONS = "KEY_USE_GESTURE_ACTIONS"

    const val KEY_HIDE_COMMENT_ACTIONS = "KEY_HIDE_COMMENT_ACTIONS"
    const val KEY_TAP_COMMENT_TO_COLLAPSE = "KEY_TAP_COMMENT_TO_COLLAPSE"

    const val KEY_INFINITY = "KEY_INFINITY"

    const val KEY_POST_GESTURE_ACTION_1 = "KEY_POST_GESTURE_ACTION_1"
    const val KEY_POST_GESTURE_ACTION_2 = "KEY_POST_GESTURE_ACTION_2"
    const val KEY_POST_GESTURE_ACTION_3 = "KEY_POST_GESTURE_ACTION_3"
    const val KEY_POST_GESTURE_SIZE = "KEY_POST_GESTURE_SIZE"

    const val KEY_COMMENT_GESTURE_ACTION_1 = "KEY_COMMENT_GESTURE_ACTION_1"
    const val KEY_COMMENT_GESTURE_ACTION_2 = "KEY_COMMENT_GESTURE_ACTION_2"
    const val KEY_COMMENT_GESTURE_ACTION_3 = "KEY_COMMENT_GESTURE_ACTION_3"
    const val KEY_COMMENT_GESTURE_SIZE = "KEY_COMMENT_GESTURE_SIZE"

    const val KEY_BLUR_NSFW_POSTS = "KEY_BLUR_NSFW_POSTS"

    const val KEY_SHOW_LINK_POSTS = "KEY_SHOW_LINK_POSTS"
    const val KEY_SHOW_IMAGE_POSTS = "KEY_SHOW_IMAGE_POSTS"
    const val KEY_SHOW_VIDEO_POSTS = "KEY_SHOW_VIDEO_POSTS"
    const val KEY_SHOW_TEXT_POSTS = "KEY_SHOW_TEXT_POSTS"
    const val KEY_SHOW_NSFW_POSTS = "KEY_SHOW_NSFW_POSTS"

    const val KEY_GLOBAL_FONT_SIZE = "KEY_GLOBAL_FONT_SIZE"
    const val KEY_GLOBAL_FONT_COLOR = "KEY_GLOBAL_FONT_COLOR"

    const val KEY_DEFAULT_COMMUNITY_SORT_ORDER = "KEY_DEFAULT_COMMUNITY_SORT_ORDER"
    const val KEY_DEFAULT_COMMENTS_SORT_ORDER = "KEY_DEFAULT_COMMUNITY_SORT_ORDER"

    const val KEY_ALWAYS_SHOW_LINK_BUTTON_BELOW_POST = "KEY_ALWAYS_SHOW_LINK_BUTTON_BELOW_POST"
    const val KEY_POST_LIST_VIEW_IMAGE_ON_SINGLE_TAP = "KEY_POST_LIST_VIEW_IMAGE_ON_SINGLE_TAP"

    private const val KEY_OFFLINE_STORAGE_CAP_BYTES = "KEY_OFFLINE_STORAGE_CAP_BYTES"
    private const val KEY_VIDEO_PLAYER_ROTATION_LOCKED = "KEY_VIDEO_PLAYER_ROTATION_LOCKED"

    const val KEY_COMMENT_THREAD_STYLE = "KEY_COMMENT_THREAD_STYLE"

    const val KEY_COLOR_SCHEME = "KEY_COLOR_SCHEME"

    const val KEY_COMMENTS_NAVIGATION_FAB = "KEY_COMMENTS_NAVIGATION_FAB"
    const val KEY_USE_VOLUME_BUTTON_NAVIGATION = "KEY_USE_VOLUME_BUTTON_NAVIGATION"
    const val KEY_COMMENTS_NAVIGATION_FAB_OFF_X = "KEY_COMMENTS_NAVIGATION_FAB_OFF_X"
    const val KEY_COMMENTS_NAVIGATION_FAB_OFF_Y = "KEY_COMMENTS_NAVIGATION_FAB_OFF_Y"
    const val KEY_COMPATIBILITY_MODE2 = "KEY_COMPATIBILITY_MODE2"

    const val KEY_HIDE_POST_SCORES = "KEY_HIDE_POST_SCORES"
    const val KEY_HIDE_COMMENT_SCORES = "KEY_HIDE_COMMENT_SCORES"
    const val KEY_GLOBAL_FONT = "KEY_GLOBAL_FONT"

    const val KEY_UPVOTE_COLOR = "KEY_UPVOTE_COLOR"
    const val KEY_DOWNVOTE_COLOR = "KEY_DOWNVOTE_COLOR"

    const val KEY_COLLAPSE_CHILD_COMMENTS_BY_DEFAULT = "KEY_COLLAPSE_CHILD_COMMENTS_BY_DEFAULT"

    const val KEY_OPEN_LINKS_IN_APP = "KEY_OPEN_LINKS_IN_APP"
    const val KEY_AUTO_LINK_PHONE_NUMBERS = "KEY_AUTO_LINK_PHONE_NUMBERS"
    const val KEY_SHOW_UP_AND_DOWN_VOTES = "KEY_SHOW_UP_AND_DOWN_VOTES"
    const val KEY_DISPLAY_INSTANCE_STYLE = "KEY_DISPLAY_INSTANCE_STYLE"

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
        defaultArr: IntArray,
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
