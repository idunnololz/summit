package com.idunnololz.summit.util

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.idunnololz.summit.util.ext.getIntOrNull
import com.idunnololz.summit.util.ext.getLongSafe
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json

private const val TAG = "PreferenceUtils"

object PreferenceUtils {

    var usingCustomFont: Boolean = false
    lateinit var preferences: SharedPreferences
        private set

    private const val DEFAULT_PREF = "pref"

    const val KEY_DEFAULT_PAGE = "KEY_DEFAULT_PAGE"
    const val KEY_THEME = "theme"

    const val PREFERENCE_VERSION_CODE = "PREFERENCE_VERSION_CODE"

    /**
     * END OF DEPRECATED KEYS
     */

    /**
     * Scheduling options
     */
    const val KEY_ENABLE_OFFLINE_SCHEDULE = "enable_offline_schedule"
    const val KEY_OFFLINE_SCHEDULE = "offline_schedule"

    const val KEY_LAST_SUCCESSFUL_OFFLINE_DOWNLOAD = "last_successful_offline_download"

    const val KEY_COMMUNITY_LAYOUT = "KEY_SUBREDDIT_LAYOUT"
    const val KEY_POST_UI_CONFIG_COMPACT = "KEY_POST_UI_CONFIG_COMPACT"
    const val KEY_POST_UI_CONFIG_LIST = "KEY_POST_UI_CONFIG_LIST"
    const val KEY_POST_UI_CONFIG_LARGE_LIST = "KEY_POST_UI_CONFIG_LARGE_LIST"
    const val KEY_POST_UI_CONFIG_CARD = "KEY_POST_UI_CONFIG_CARD"
    const val KEY_POST_UI_CONFIG_CARD2 = "KEY_POST_UI_CONFIG_CARD2"
    const val KEY_POST_UI_CONFIG_CARD3 = "KEY_POST_UI_CONFIG_CARD3"
    const val KEY_POST_UI_CONFIG_FULL = "KEY_POST_UI_CONFIG_FULL"
    const val KEY_POST_UI_CONFIG_LIST_WITH_CARDS = "KEY_POST_UI_CONFIG_LIST_WITH_CARDS"
    const val KEY_POST_UI_CONFIG_FULL_WITH_CARDS = "KEY_POST_UI_CONFIG_FULL_WITH_CARDS"

    const val KEY_BASE_THEME = "KEY_BASE_THEME"

    const val KEY_USE_MATERIAL_YOU = "KEY_USE_MATERIAL_YOU"

    const val KEY_USE_BLACK_THEME = "KEY_USE_BLACK_THEME"

    const val KEY_USE_LESS_DARK_BACKGROUND = "KEY_USE_LESS_DARK_BACKGROUND"

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

    const val KEY_POST_GESTURE_ACTION_COLOR_1 = "KEY_POST_GESTURE_ACTION_COLOR_1"
    const val KEY_POST_GESTURE_ACTION_COLOR_2 = "KEY_POST_GESTURE_ACTION_COLOR_2"
    const val KEY_POST_GESTURE_ACTION_COLOR_3 = "KEY_POST_GESTURE_ACTION_COLOR_3"

    const val KEY_COMMENT_GESTURE_ACTION_1 = "KEY_COMMENT_GESTURE_ACTION_1"
    const val KEY_COMMENT_GESTURE_ACTION_2 = "KEY_COMMENT_GESTURE_ACTION_2"
    const val KEY_COMMENT_GESTURE_ACTION_3 = "KEY_COMMENT_GESTURE_ACTION_3"
    const val KEY_COMMENT_GESTURE_SIZE = "KEY_COMMENT_GESTURE_SIZE"

    const val KEY_COMMENT_GESTURE_ACTION_COLOR_1 = "KEY_COMMENT_GESTURE_ACTION_COLOR_1"
    const val KEY_COMMENT_GESTURE_ACTION_COLOR_2 = "KEY_COMMENT_GESTURE_ACTION_COLOR_2"
    const val KEY_COMMENT_GESTURE_ACTION_COLOR_3 = "KEY_COMMENT_GESTURE_ACTION_COLOR_3"

    const val KEY_BLUR_NSFW_POSTS = "KEY_BLUR_NSFW_POSTS"

    const val KEY_SHOW_LINK_POSTS = "KEY_SHOW_LINK_POSTS"
    const val KEY_SHOW_IMAGE_POSTS = "KEY_SHOW_IMAGE_POSTS"
    const val KEY_SHOW_VIDEO_POSTS = "KEY_SHOW_VIDEO_POSTS"
    const val KEY_SHOW_TEXT_POSTS = "KEY_SHOW_TEXT_POSTS"
    const val KEY_SHOW_NSFW_POSTS = "KEY_SHOW_NSFW_POSTS"

    const val KEY_GLOBAL_FONT_SIZE = "KEY_GLOBAL_FONT_SIZE"
    const val KEY_GLOBAL_FONT_COLOR = "KEY_GLOBAL_FONT_COLOR"

    const val KEY_DEFAULT_COMMUNITY_SORT_ORDER = "KEY_DEFAULT_COMMUNITY_SORT_ORDER"
    const val KEY_DEFAULT_COMMENTS_SORT_ORDER = "KEY_DEFAULT_COMMENTS_SORT_ORDER"

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
    const val KEY_AUTO_LINK_IP_ADDRESSES = "KEY_AUTO_LINK_IP_ADDRESSES"
    const val KEY_POST_SHOW_UP_AND_DOWN_VOTES = "KEY_SHOW_UP_AND_DOWN_VOTES"
    const val KEY_COMMENT_SHOW_UP_AND_DOWN_VOTES = "KEY_COMMENT_SHOW_UP_AND_DOWN_VOTES"
    const val KEY_DISPLAY_INSTANCE_STYLE = "KEY_DISPLAY_INSTANCE_STYLE"
    const val KEY_RETAIN_LAST_POST = "KEY_RETAIN_LAST_POST"
    const val KEY_LEFT_HAND_MODE = "KEY_LEFT_HAND_MODE"
    const val KEY_TRANSPARENT_NOTIFICATION_BAR = "KEY_TRANSPARENT_NOTIFICATION_BAR"
    const val KEY_LOCK_BOTTOM_BAR = "KEY_LOCK_BOTTOM_BAR"
    const val KEY_PREVIEW_LINKS = "KEY_PREVIEW_LINKS"
    const val KEY_SCREENSHOT_WIDTH_DP = "KEY_SCREENSHOT_WIDTH"
    const val KEY_DATE_SCREENSHOTS = "KEY_DATE_SCREENSHOTS"
    const val KEY_SCREENSHOT_WATERMARK = "KEY_SCREENSHOT_WATERMARK"
    const val KEY_USE_FIREBASE = "KEY_USE_FIREBASE"
    const val KEY_AUTO_COLLAPSE_COMMENT_THRESHOLD = "KEY_AUTO_COLLAPSE_COMMENT_THRESHOLD"
    const val KEY_TRACK_BROWSING_HISTORY = "KEY_TRACK_BROWSING_HISTORY"
    const val KEY_NAV_BAR_ITEMS = "KEY_NAV_BAR_ITEMS"
    const val KEY_USE_CUSTOM_NAV_BAR = "KEY_USE_CUSTOM_NAV_BAR"
    const val KEY_USE_BOTTOM_NAV_BAR = "KEY_USE_BOTTOM_NAV_BAR"
    const val KEY_ENABLE_HIDDEN_POSTS = "KEY_ENABLE_HIDDEN_POSTS"
    const val KEY_USE_PREDICTIVE_BACK = "KEY_USE_PREDICTIVE_BACK"
    const val KEY_AUTO_LOAD_MORE_POSTS = "KEY_AUTO_LOAD_MORE_POSTS"
    const val KEY_INFINITY_PAGE_INDICATOR = "KEY_INFINITY_PAGE_INDICATOR"
    const val KEY_WARN_REPLY_TO_OLD_CONTENT = "KEY_WARN_REPLY_TO_OLD_CONTENT"
    const val KEY_WARN_REPLY_TO_OLD_CONTENT_THRESHOLD_MS =
        "KEY_WARN_REPLY_TO_OLD_CONTENT_THRESHOLD_MS"
    const val KEY_CHECK_INTERVAL = "KEY_CHECK_INTERVAL"

    const val KEY_SHOW_POST_UPVOTE_PERCENTAGE = "KEY_SHOW_POST_UPVOTE_PERCENTAGE"
    const val KEY_SHOW_COMMENT_UPVOTE_PERCENTAGE = "KEY_SHOW_COMMENT_UPVOTE_PERCENTAGE"

    const val KEY_USE_MULTILINE_POST_HEADERS = "KEY_USE_MULTILINE_POST_HEADERS"
    const val KEY_INDICATE_CONTENT_FROM_CURRENT_USER = "KEY_INDICATE_CONTENT_FROM_CURRENT_USER"
    const val KEY_SAVE_DRAFTS_AUTOMATICALLY = "KEY_SAVE_DRAFTS_AUTOMATICALLY"
    const val KEY_SHOW_PROFILE_ICONS = "KEY_SHOW_PROFILE_ICONS"
    const val KEY_NAVIGATION_RAIL_MODE = "KEY_NAVIGATION_RAIL_MODE"
    const val KEY_DOWNLOAD_DIRECTORY = "KEY_DOWNLOAD_DIRECTORY"
    const val KEY_USE_PER_COMMUNITY_SETTINGS = "KEY_USE_PER_COMMUNITY_SETTINGS"
    const val KEY_COMMENT_HEADER_LAYOUT = "KEY_COMMENT_HEADER_LAYOUT"
    const val KEY_GUEST_ACCOUNT_SETTINGS = "KEY_GUEST_ACCOUNT_SETTINGS"
    const val KEY_TEXT_FIELD_TOOLBAR_SETTINGS = "KEY_TEXT_FIELD_TOOLBAR_SETTINGS"
    const val KEY_POST_QUICK_ACTIONS = "KEY_POST_QUICK_ACTIONS"
    const val KEY_COMMENT_QUICK_ACTIONS = "KEY_COMMENT_QUICK_ACTIONS"
    const val KEY_GLOBAL_LAYOUT_MODE = "KEY_GLOBAL_LAYOUT_MODE"
    const val KEY_ROTATE_INSTANCE_ON_UPLOAD_FAIL = "KEY_ROTATE_INSTANCE_ON_UPLOAD_FAIL"
    const val KEY_SHOW_FILTERED_POSTS = "KEY_SHOW_FILTERED_POSTS"
    const val KEY_COMMENTS_SHOW_INLINE_MEDIA_AS_LINKS = "KEY_COMMENTS_SHOW_INLINE_MEDIA_AS_LINKS"
    const val KEY_IS_NOTIFICATIONS_ON = "KEY_IS_NOTIFICATIONS_ON"
    const val KEY_LAST_ACCOUNT_NOTIFICATION_ID = "KEY_LAST_ACCOUNT_NOTIFICATION_ID"
    const val KEY_NOTIFICATIONS_CHECK_INTERVAL_MS = "KEY_NOTIFICATIONS_CHECK_INTERVAL_MS"
    const val KEY_HOME_FAB_QUICK_ACTION = "KEY_HOME_FAB_QUICK_ACTION"
    const val KEY_SHOW_EDITED_DATE = "KEY_SHOW_EDITED_DATE"
    const val KEY_IMAGE_PREVIEW_HIDE_UI_BY_DEFAULT = "KEY_IMAGE_PREVIEW_HIDE_UI_BY_DEFAULT"
    const val KEY_PREFETCH_POSTS = "KEY_PREFETCH_POSTS"
    const val KEY_AUTO_PLAY_VIDEOS = "KEY_AUTO_PLAY_VIDEOS"

    const val KEY_PREF_VERSION = "pref_version"

    const val KEY_UPLOAD_IMAGES_TO_IMGUR = "KEY_UPLOAD_IMAGES_TO_IMGUR"
    const val KEY_ANIMATION_LEVEL = "KEY_ANIMATION_LEVEL"
    const val KEY_CACHE_POLICY = "KEY_CACHE_POLICY"
    const val KEY_USE_CONDENSED_FOR_COMMENT_HEADERS = "KEY_USE_CONDENSED_FOR_COMMENT_HEADERS"
    const val KEY_PARSE_MARKDOWN_IN_POST_TITLES = "KEY_PARSE_MARKDOWN_IN_POST_TITLES"
    const val KEY_SEARCH_HOME_CONFIG = "KEY_SEARCH_HOME_CONFIG"
    const val KEY_POST_FEED_SHOW_SCROLL_BAR = "KEY_POST_FEED_SHOW_SCROLL_BAR"
    const val KEY_HAPTICS_ENABLED = "KEY_HAPTICS_ENABLED2"
    const val KEY_HAPTICS_ON_ACTIONS = "KEY_HAPTICS_ON_ACTIONS"
    const val KEY_HIDE_DUPLICATE_POSTS_ON_READ = "KEY_HIDE_DUPLICATE_POSTS_ON_READ"
    const val KEY_USE_POSTS_FEED_HEADER = "KEY_USE_POSTS_FEED_HEADER"
    const val KEY_INLINE_VIDEO_DEFAULT_VOLUME = "KEY_INLINE_VIDEO_DEFAULT_VOLUME"
    const val KEY_SWIPE_BETWEEN_POSTS = "KEY_SWIPE_BETWEEN_POSTS"
    const val KEY_POST_FAB_QUICK_ACTION = "KEY_POST_FAB_QUICK_ACTION"
    const val KEY_SHAKE_TO_SEND_FEEDBACK = "KEY_SHAKE_TO_SEND_FEEDBACK"
    const val KEY_SHOW_LABELS_IN_NAV_BAR = "KEY_SHOW_LABELS_IN_NAV_BAR"
    const val KEY_WARN_NEW_PERSON = "KEY_WARN_NEW_PERSON"
    const val KEY_GESTURE_SWIPE_DIRECTION = "KEY_GESTURE_SWIPE_DIRECTION"
    const val KEY_DEFAULT_APP_WEB_BROWSER = "KEY_DEFAULT_APP_WEB_BROWSER"
    const val KEY_PREFERRED_LOCALE = "KEY_PREFERRED_LOCALE"
    const val KEY_COMMUNITY_SELECTOR_SHOW_COMMUNITY_SUGGESTIONS =
        "KEY_COMMUNITY_SELECTOR_SHOW_COMMUNITY_SUGGESTIONS"
    const val KEY_POST_FULL_BLEED_IMAGE = "KEY_POST_FULL_BLEED_IMAGE"

    // Unused/dead keys
    @Suppress("unused")
    const val DEAD_KEY_SHARE_IMAGES_DIRECTLY = "KEY_SHARE_IMAGES_DIRECTLY"

    @Suppress("unused")
    const val DEAD_KEY_HAPTICS_ENABLED = "KEY_HAPTICS_ENABLED"

    fun initialize(context: Context) {
        if (!::preferences.isInitialized) {
            preferences = context.getSharedPreferences(DEFAULT_PREF, Context.MODE_PRIVATE)
        }
    }

    fun isVideoPlayerRotationLocked(): Boolean =
        preferences.getBoolean(KEY_VIDEO_PLAYER_ROTATION_LOCKED, false)

    fun setVideoPlayerRotationLocked(b: Boolean) {
        preferences.edit()
            .putBoolean(KEY_VIDEO_PLAYER_ROTATION_LOCKED, b)
            .apply()
    }
}

class StringPreferenceDelegate(
    val prefs: SharedPreferences,
    val key: String,
    val defaultValue: String? = "",
) : ReadWriteProperty<Any, String?> {

    override fun getValue(thisRef: Any, property: KProperty<*>) = prefs.getString(key, defaultValue)

    override fun setValue(thisRef: Any, property: KProperty<*>, value: String?) =
        prefs.edit().putString(key, value).apply()
}

class FloatPreferenceDelegate(
    val prefs: SharedPreferences,
    val key: String,
    val defaultValue: Float = 0f,
) : ReadWriteProperty<Any, Float> {

    override fun getValue(thisRef: Any, property: KProperty<*>) = prefs.getFloat(key, defaultValue)

    override fun setValue(thisRef: Any, property: KProperty<*>, value: Float) =
        prefs.edit().putFloat(key, value).apply()
}

class BooleanPreferenceDelegate(
    val prefs: SharedPreferences,
    val key: String,
    val defaultValue: Boolean = false,
) : ReadWriteProperty<Any, Boolean> {

    override fun getValue(thisRef: Any, property: KProperty<*>) =
        prefs.getBoolean(key, defaultValue)

    override fun setValue(thisRef: Any, property: KProperty<*>, value: Boolean) =
        prefs.edit().putBoolean(key, value).apply()
}

class IntPreferenceDelegate(
    val prefs: SharedPreferences,
    val key: String,
    val defaultValue: Int = 0,
) : ReadWriteProperty<Any, Int> {

    override fun getValue(thisRef: Any, property: KProperty<*>) = prefs.getInt(key, defaultValue)

    override fun setValue(thisRef: Any, property: KProperty<*>, value: Int) =
        prefs.edit().putInt(key, value).apply()
}

class NullableIntPreferenceDelegate(
    val prefs: SharedPreferences,
    val key: String,
) : ReadWriteProperty<Any, Int?> {

    override fun getValue(thisRef: Any, property: KProperty<*>) = prefs.getIntOrNull(key)

    override fun setValue(thisRef: Any, property: KProperty<*>, value: Int?) {
        if (value != null) {
            prefs.edit().putInt(key, value).apply()
        }
    }
}

class LongPreferenceDelegate(
    private val prefs: SharedPreferences,
    val key: String,
    val defaultValue: Long = 0,
) : ReadWriteProperty<Any, Long> {

    override fun getValue(thisRef: Any, property: KProperty<*>) =
        prefs.getLongSafe(key, defaultValue)

    override fun setValue(thisRef: Any, property: KProperty<*>, value: Long) =
        prefs.edit().putLong(key, value).apply()
}

class JsonPreferenceDelegate<T>(
    val prefs: SharedPreferences,
    val json: Json,
    val key: String,
    val serializer: KSerializer<T>,
    val default: () -> T,
) : ReadWriteProperty<Any, T> {

//    var isCached: Boolean = false
//    var cache: T? = null

    override fun getValue(thisRef: Any, property: KProperty<*>): T {
//        if (isCached) {
//            @Suppress("UNCHECKED_CAST")
//            return cache as T
//        }

        val s = prefs.getString(key, null)
        return try {
            if (s != null) {
                json.decodeFromString(serializer, s)
                    ?: default()
            } else {
                default()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error reading json. Key: $key", e)
            default()
        }
//            .also {
//            cache(it)
//        }
    }

    override fun setValue(thisRef: Any, property: KProperty<*>, value: T) {
        val s = try {
            json.encodeToString(serializer, value)
        } catch (e: Exception) {
            Log.e(TAG, "Error converting object to json. Key: $key", e)
            null
        }
        prefs.edit()
            .putString(key, s)
            .apply()

//        cache(value)
    }

//    @Suppress("NOTHING_TO_INLINE")
//    inline fun cache(value: T?) {
//        isCached = true
//        cache = value
//    }
}
