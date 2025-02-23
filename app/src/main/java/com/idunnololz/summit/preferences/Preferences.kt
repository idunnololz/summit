package com.idunnololz.summit.preferences

import android.content.Context
import android.content.SharedPreferences
import android.util.Base64
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.idunnololz.summit.BuildConfig
import com.idunnololz.summit.R
import com.idunnololz.summit.cache.CachePolicy
import com.idunnololz.summit.coroutine.CoroutineScopeFactory
import com.idunnololz.summit.lemmy.CommentsSortOrder
import com.idunnololz.summit.lemmy.CommunityRef
import com.idunnololz.summit.lemmy.CommunitySortOrder
import com.idunnololz.summit.lemmy.community.CommunityLayout
import com.idunnololz.summit.lemmy.postListView.PostAndCommentsUiConfig
import com.idunnololz.summit.lemmy.postListView.PostInListUiConfig
import com.idunnololz.summit.lemmy.postListView.getDefaultPostAndCommentsUiConfig
import com.idunnololz.summit.lemmy.postListView.getDefaultPostUiConfig
import com.idunnololz.summit.links.PreviewLinkOptions.PreviewTextLinks
import com.idunnololz.summit.settings.misc.DisplayInstanceOptions
import com.idunnololz.summit.settings.navigation.NavBarConfig
import com.idunnololz.summit.util.AnimationsHelper
import com.idunnololz.summit.util.PreferenceUtil
import com.idunnololz.summit.util.PreferenceUtil.KEY_ALWAYS_SHOW_LINK_BUTTON_BELOW_POST
import com.idunnololz.summit.util.PreferenceUtil.KEY_ANIMATION_LEVEL
import com.idunnololz.summit.util.PreferenceUtil.KEY_AUTO_COLLAPSE_COMMENT_THRESHOLD
import com.idunnololz.summit.util.PreferenceUtil.KEY_AUTO_LINK_IP_ADDRESSES
import com.idunnololz.summit.util.PreferenceUtil.KEY_AUTO_LINK_PHONE_NUMBERS
import com.idunnololz.summit.util.PreferenceUtil.KEY_AUTO_LOAD_MORE_POSTS
import com.idunnololz.summit.util.PreferenceUtil.KEY_AUTO_PLAY_VIDEOS
import com.idunnololz.summit.util.PreferenceUtil.KEY_BASE_THEME
import com.idunnololz.summit.util.PreferenceUtil.KEY_BLUR_NSFW_POSTS
import com.idunnololz.summit.util.PreferenceUtil.KEY_CACHE_POLICY
import com.idunnololz.summit.util.PreferenceUtil.KEY_COLLAPSE_CHILD_COMMENTS_BY_DEFAULT
import com.idunnololz.summit.util.PreferenceUtil.KEY_COLOR_SCHEME
import com.idunnololz.summit.util.PreferenceUtil.KEY_COMMENTS_NAVIGATION_FAB
import com.idunnololz.summit.util.PreferenceUtil.KEY_COMMENTS_NAVIGATION_FAB_OFF_X
import com.idunnololz.summit.util.PreferenceUtil.KEY_COMMENTS_NAVIGATION_FAB_OFF_Y
import com.idunnololz.summit.util.PreferenceUtil.KEY_COMMENTS_SHOW_INLINE_MEDIA_AS_LINKS
import com.idunnololz.summit.util.PreferenceUtil.KEY_COMMENT_GESTURE_ACTION_1
import com.idunnololz.summit.util.PreferenceUtil.KEY_COMMENT_GESTURE_ACTION_2
import com.idunnololz.summit.util.PreferenceUtil.KEY_COMMENT_GESTURE_ACTION_3
import com.idunnololz.summit.util.PreferenceUtil.KEY_COMMENT_GESTURE_ACTION_COLOR_1
import com.idunnololz.summit.util.PreferenceUtil.KEY_COMMENT_GESTURE_ACTION_COLOR_2
import com.idunnololz.summit.util.PreferenceUtil.KEY_COMMENT_GESTURE_ACTION_COLOR_3
import com.idunnololz.summit.util.PreferenceUtil.KEY_COMMENT_GESTURE_SIZE
import com.idunnololz.summit.util.PreferenceUtil.KEY_COMMENT_HEADER_LAYOUT
import com.idunnololz.summit.util.PreferenceUtil.KEY_COMMENT_QUICK_ACTIONS
import com.idunnololz.summit.util.PreferenceUtil.KEY_COMMENT_SHOW_UP_AND_DOWN_VOTES
import com.idunnololz.summit.util.PreferenceUtil.KEY_COMMENT_THREAD_STYLE
import com.idunnololz.summit.util.PreferenceUtil.KEY_DATE_SCREENSHOTS
import com.idunnololz.summit.util.PreferenceUtil.KEY_DEFAULT_COMMENTS_SORT_ORDER
import com.idunnololz.summit.util.PreferenceUtil.KEY_DEFAULT_COMMUNITY_SORT_ORDER
import com.idunnololz.summit.util.PreferenceUtil.KEY_DISPLAY_INSTANCE_STYLE
import com.idunnololz.summit.util.PreferenceUtil.KEY_DOWNLOAD_DIRECTORY
import com.idunnololz.summit.util.PreferenceUtil.KEY_DOWNVOTE_COLOR
import com.idunnololz.summit.util.PreferenceUtil.KEY_ENABLE_HIDDEN_POSTS
import com.idunnololz.summit.util.PreferenceUtil.KEY_GLOBAL_FONT
import com.idunnololz.summit.util.PreferenceUtil.KEY_GLOBAL_FONT_COLOR
import com.idunnololz.summit.util.PreferenceUtil.KEY_GLOBAL_FONT_SIZE
import com.idunnololz.summit.util.PreferenceUtil.KEY_GLOBAL_LAYOUT_MODE
import com.idunnololz.summit.util.PreferenceUtil.KEY_GUEST_ACCOUNT_SETTINGS
import com.idunnololz.summit.util.PreferenceUtil.KEY_HAPTICS_ENABLED
import com.idunnololz.summit.util.PreferenceUtil.KEY_HAPTICS_ON_ACTIONS
import com.idunnololz.summit.util.PreferenceUtil.KEY_HIDE_COMMENT_ACTIONS
import com.idunnololz.summit.util.PreferenceUtil.KEY_HIDE_COMMENT_SCORES
import com.idunnololz.summit.util.PreferenceUtil.KEY_HIDE_DUPLICATE_POSTS_ON_READ
import com.idunnololz.summit.util.PreferenceUtil.KEY_HIDE_POST_SCORES
import com.idunnololz.summit.util.PreferenceUtil.KEY_HOME_FAB_QUICK_ACTION
import com.idunnololz.summit.util.PreferenceUtil.KEY_IMAGE_PREVIEW_HIDE_UI_BY_DEFAULT
import com.idunnololz.summit.util.PreferenceUtil.KEY_INDICATE_CONTENT_FROM_CURRENT_USER
import com.idunnololz.summit.util.PreferenceUtil.KEY_INFINITY
import com.idunnololz.summit.util.PreferenceUtil.KEY_INFINITY_PAGE_INDICATOR
import com.idunnololz.summit.util.PreferenceUtil.KEY_IS_NOTIFICATIONS_ON
import com.idunnololz.summit.util.PreferenceUtil.KEY_LAST_ACCOUNT_NOTIFICATION_ID
import com.idunnololz.summit.util.PreferenceUtil.KEY_LEFT_HAND_MODE
import com.idunnololz.summit.util.PreferenceUtil.KEY_LOCK_BOTTOM_BAR
import com.idunnololz.summit.util.PreferenceUtil.KEY_MARK_POSTS_AS_READ_ON_SCROLL
import com.idunnololz.summit.util.PreferenceUtil.KEY_NAVIGATION_RAIL_MODE
import com.idunnololz.summit.util.PreferenceUtil.KEY_NAV_BAR_ITEMS
import com.idunnololz.summit.util.PreferenceUtil.KEY_NOTIFICATIONS_CHECK_INTERVAL_MS
import com.idunnololz.summit.util.PreferenceUtil.KEY_OPEN_LINKS_IN_APP
import com.idunnololz.summit.util.PreferenceUtil.KEY_PARSE_MARKDOWN_IN_POST_TITLES
import com.idunnololz.summit.util.PreferenceUtil.KEY_POST_AND_COMMENTS_UI_CONFIG
import com.idunnololz.summit.util.PreferenceUtil.KEY_POST_FEED_SHOW_SCROLL_BAR
import com.idunnololz.summit.util.PreferenceUtil.KEY_POST_GESTURE_ACTION_1
import com.idunnololz.summit.util.PreferenceUtil.KEY_POST_GESTURE_ACTION_2
import com.idunnololz.summit.util.PreferenceUtil.KEY_POST_GESTURE_ACTION_3
import com.idunnololz.summit.util.PreferenceUtil.KEY_POST_GESTURE_ACTION_COLOR_1
import com.idunnololz.summit.util.PreferenceUtil.KEY_POST_GESTURE_ACTION_COLOR_2
import com.idunnololz.summit.util.PreferenceUtil.KEY_POST_GESTURE_ACTION_COLOR_3
import com.idunnololz.summit.util.PreferenceUtil.KEY_POST_GESTURE_SIZE
import com.idunnololz.summit.util.PreferenceUtil.KEY_POST_LIST_VIEW_IMAGE_ON_SINGLE_TAP
import com.idunnololz.summit.util.PreferenceUtil.KEY_POST_QUICK_ACTIONS
import com.idunnololz.summit.util.PreferenceUtil.KEY_POST_SHOW_UP_AND_DOWN_VOTES
import com.idunnololz.summit.util.PreferenceUtil.KEY_PREFETCH_POSTS
import com.idunnololz.summit.util.PreferenceUtil.KEY_PREF_VERSION
import com.idunnololz.summit.util.PreferenceUtil.KEY_PREVIEW_LINKS
import com.idunnololz.summit.util.PreferenceUtil.KEY_RETAIN_LAST_POST
import com.idunnololz.summit.util.PreferenceUtil.KEY_ROTATE_INSTANCE_ON_UPLOAD_FAIL
import com.idunnololz.summit.util.PreferenceUtil.KEY_SAVE_DRAFTS_AUTOMATICALLY
import com.idunnololz.summit.util.PreferenceUtil.KEY_SCREENSHOT_WATERMARK
import com.idunnololz.summit.util.PreferenceUtil.KEY_SCREENSHOT_WIDTH_DP
import com.idunnololz.summit.util.PreferenceUtil.KEY_SEARCH_HOME_CONFIG
import com.idunnololz.summit.util.PreferenceUtil.KEY_SHOW_COMMENT_UPVOTE_PERCENTAGE
import com.idunnololz.summit.util.PreferenceUtil.KEY_SHOW_EDITED_DATE
import com.idunnololz.summit.util.PreferenceUtil.KEY_SHOW_FILTERED_POSTS
import com.idunnololz.summit.util.PreferenceUtil.KEY_SHOW_IMAGE_POSTS
import com.idunnololz.summit.util.PreferenceUtil.KEY_SHOW_LINK_POSTS
import com.idunnololz.summit.util.PreferenceUtil.KEY_SHOW_NSFW_POSTS
import com.idunnololz.summit.util.PreferenceUtil.KEY_SHOW_POST_UPVOTE_PERCENTAGE
import com.idunnololz.summit.util.PreferenceUtil.KEY_SHOW_PROFILE_ICONS
import com.idunnololz.summit.util.PreferenceUtil.KEY_SHOW_TEXT_POSTS
import com.idunnololz.summit.util.PreferenceUtil.KEY_SHOW_VIDEO_POSTS
import com.idunnololz.summit.util.PreferenceUtil.KEY_TAP_COMMENT_TO_COLLAPSE
import com.idunnololz.summit.util.PreferenceUtil.KEY_TEXT_FIELD_TOOLBAR_SETTINGS
import com.idunnololz.summit.util.PreferenceUtil.KEY_TRACK_BROWSING_HISTORY
import com.idunnololz.summit.util.PreferenceUtil.KEY_TRANSPARENT_NOTIFICATION_BAR
import com.idunnololz.summit.util.PreferenceUtil.KEY_UPLOAD_IMAGES_TO_IMGUR
import com.idunnololz.summit.util.PreferenceUtil.KEY_UPVOTE_COLOR
import com.idunnololz.summit.util.PreferenceUtil.KEY_USE_BOTTOM_NAV_BAR
import com.idunnololz.summit.util.PreferenceUtil.KEY_USE_CONDENSED_FOR_COMMENT_HEADERS
import com.idunnololz.summit.util.PreferenceUtil.KEY_USE_CUSTOM_NAV_BAR
import com.idunnololz.summit.util.PreferenceUtil.KEY_USE_FIREBASE
import com.idunnololz.summit.util.PreferenceUtil.KEY_USE_GESTURE_ACTIONS
import com.idunnololz.summit.util.PreferenceUtil.KEY_USE_LESS_DARK_BACKGROUND
import com.idunnololz.summit.util.PreferenceUtil.KEY_USE_MULTILINE_POST_HEADERS
import com.idunnololz.summit.util.PreferenceUtil.KEY_USE_PER_COMMUNITY_SETTINGS
import com.idunnololz.summit.util.PreferenceUtil.KEY_USE_POSTS_FEED_HEADER
import com.idunnololz.summit.util.PreferenceUtil.KEY_USE_PREDICTIVE_BACK
import com.idunnololz.summit.util.PreferenceUtil.KEY_USE_VOLUME_BUTTON_NAVIGATION
import com.idunnololz.summit.util.PreferenceUtil.KEY_WARN_REPLY_TO_OLD_CONTENT
import com.idunnololz.summit.util.PreferenceUtil.KEY_WARN_REPLY_TO_OLD_CONTENT_THRESHOLD_MS
import com.idunnololz.summit.util.Utils
import com.idunnololz.summit.util.ext.fromJsonSafe
import com.idunnololz.summit.util.ext.getColorCompat
import com.idunnololz.summit.util.ext.getIntOrNull
import com.idunnololz.summit.util.ext.getLongSafe
import com.idunnololz.summit.util.ext.getMoshiValue
import com.idunnololz.summit.util.ext.putMoshiValue
import com.idunnololz.summit.util.ext.toJsonSafe
import com.idunnololz.summit.util.moshi
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Duration
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.json.JSONObject

private val Context.offlineModeDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "offlineModePreferences",
)

class Preferences(
    @ApplicationContext private val context: Context,
    private val prefs: SharedPreferences,
    private val coroutineScopeFactory: CoroutineScopeFactory,
) {

    companion object {
        private const val TAG = "Preferences"
    }

    private val coroutineScope = coroutineScopeFactory.create()

    val onPreferenceChangeFlow = MutableSharedFlow<Unit>()

    init {
        prefs.registerOnSharedPreferenceChangeListener { _, _ ->
            coroutineScope.launch {
                onPreferenceChangeFlow.emit(Unit)
            }
        }
    }

    val all: MutableMap<String, *>
        get() = prefs.all

    fun getDefaultPage(): CommunityRef {
        val communityJson = prefs.getString(PreferenceUtil.KEY_DEFAULT_PAGE, null)
        val communityRef = if (communityJson == null) {
            null
        } else {
            try {
                moshi.adapter(CommunityRef::class.java)
                    .fromJson(communityJson)
            } catch (e: Exception) {
                null
            }
        }

        return communityRef ?: CommunityRef.All()
    }

    fun setDefaultPage(communityRef: CommunityRef) {
        prefs.edit()
            .putString(
                PreferenceUtil.KEY_DEFAULT_PAGE,
                moshi.adapter(CommunityRef::class.java).toJson(communityRef),
            )
            .apply()
    }

    fun getPostsLayout(): CommunityLayout = try {
        CommunityLayout.valueOf(
            PreferenceUtil.preferences
                .getString(PreferenceUtil.KEY_COMMUNITY_LAYOUT, null) ?: "",
        )
    } catch (e: IllegalArgumentException) {
        CommunityLayout.List
    }

    fun setPostsLayout(layout: CommunityLayout) {
        PreferenceUtil.preferences.edit()
            .putString(PreferenceUtil.KEY_COMMUNITY_LAYOUT, layout.name)
            .apply()
    }

    fun getPostInListUiConfig(): PostInListUiConfig {
        return getPostInListUiConfig(getPostsLayout())
    }

    fun setPostInListUiConfig(config: PostInListUiConfig) {
        prefs.putMoshiValue(getPostUiConfigKey(getPostsLayout()), config)
    }

    fun getPostInListUiConfig(layout: CommunityLayout): PostInListUiConfig {
        return prefs.getMoshiValue<PostInListUiConfig>(getPostUiConfigKey(layout))
            ?: layout.getDefaultPostUiConfig()
    }

    fun getPostAndCommentsUiConfig(): PostAndCommentsUiConfig {
        return prefs.getMoshiValue<PostAndCommentsUiConfig>(KEY_POST_AND_COMMENTS_UI_CONFIG)
            ?: getDefaultPostAndCommentsUiConfig()
    }

    fun setPostAndCommentsUiConfig(config: PostAndCommentsUiConfig) {
        prefs.putMoshiValue(KEY_POST_AND_COMMENTS_UI_CONFIG, config)
    }

    private fun getPostUiConfigKey(layout: CommunityLayout) = when (layout) {
        CommunityLayout.Compact ->
            PreferenceUtil.KEY_POST_UI_CONFIG_COMPACT
        CommunityLayout.List ->
            PreferenceUtil.KEY_POST_UI_CONFIG_LIST
        CommunityLayout.LargeList ->
            PreferenceUtil.KEY_POST_UI_CONFIG_LARGE_LIST
        CommunityLayout.Card ->
            PreferenceUtil.KEY_POST_UI_CONFIG_CARD
        CommunityLayout.Card2 ->
            PreferenceUtil.KEY_POST_UI_CONFIG_CARD2
        CommunityLayout.Card3 ->
            PreferenceUtil.KEY_POST_UI_CONFIG_CARD3
        CommunityLayout.Full ->
            PreferenceUtil.KEY_POST_UI_CONFIG_FULL
        CommunityLayout.ListWithCards ->
            PreferenceUtil.KEY_POST_UI_CONFIG_LIST_WITH_CARDS
        CommunityLayout.FullWithCards ->
            PreferenceUtil.KEY_POST_UI_CONFIG_FULL_WITH_CARDS
    }

    fun getBaseTheme(): BaseTheme {
        return prefs.getMoshiValue<BaseTheme>(KEY_BASE_THEME)
            ?: BaseTheme.Dark
    }

    fun setBaseTheme(baseTheme: BaseTheme) {
        prefs.putMoshiValue(KEY_BASE_THEME, baseTheme)
    }

    fun isUseMaterialYou(): Boolean {
        return prefs.getBoolean(PreferenceUtil.KEY_USE_MATERIAL_YOU, false)
    }

    fun setUseMaterialYou(b: Boolean) {
        prefs.edit().putBoolean(PreferenceUtil.KEY_USE_MATERIAL_YOU, b).apply()
    }

    fun isBlackTheme(): Boolean {
        return prefs.getBoolean(PreferenceUtil.KEY_USE_BLACK_THEME, false)
    }

    fun setUseBlackTheme(b: Boolean) {
        prefs.edit().putBoolean(PreferenceUtil.KEY_USE_BLACK_THEME, b).apply()
    }

    var useLessDarkBackgroundTheme: Boolean
        get() = prefs.getBoolean(KEY_USE_LESS_DARK_BACKGROUND, false)
        set(value) {
            prefs.edit().putBoolean(KEY_USE_LESS_DARK_BACKGROUND, value).apply()
        }

    var markPostsAsReadOnScroll: Boolean
        get() = prefs.getBoolean(KEY_MARK_POSTS_AS_READ_ON_SCROLL, false)
        set(value) {
            prefs.edit().putBoolean(KEY_MARK_POSTS_AS_READ_ON_SCROLL, value).apply()
        }

    var useGestureActions: Boolean
        get() = prefs.getBoolean(KEY_USE_GESTURE_ACTIONS, true)
        set(value) {
            prefs.edit().putBoolean(KEY_USE_GESTURE_ACTIONS, value).apply()
        }

    var hideCommentActions: Boolean
        get() = prefs.getBoolean(KEY_HIDE_COMMENT_ACTIONS, true)
        set(value) {
            prefs.edit().putBoolean(KEY_HIDE_COMMENT_ACTIONS, value).apply()
        }

    var tapCommentToCollapse: Boolean
        get() = prefs.getBoolean(KEY_TAP_COMMENT_TO_COLLAPSE, false)
        set(value) {
            prefs.edit().putBoolean(KEY_TAP_COMMENT_TO_COLLAPSE, value).apply()
        }

    var infinity: Boolean
        get() = prefs.getBoolean(KEY_INFINITY, true)
        set(value) {
            prefs.edit().putBoolean(KEY_INFINITY, value).apply()
        }

    var postGestureAction1: Int
        get() = prefs.getInt(KEY_POST_GESTURE_ACTION_1, PostGestureAction.Upvote)
        set(value) {
            prefs.edit().putInt(KEY_POST_GESTURE_ACTION_1, value).apply()
        }

    var postGestureAction2: Int
        get() = prefs.getInt(KEY_POST_GESTURE_ACTION_2, PostGestureAction.Reply)
        set(value) {
            prefs.edit().putInt(KEY_POST_GESTURE_ACTION_2, value).apply()
        }

    var postGestureAction3: Int
        get() = prefs.getInt(KEY_POST_GESTURE_ACTION_3, PostGestureAction.MarkAsRead)
        set(value) {
            prefs.edit().putInt(KEY_POST_GESTURE_ACTION_3, value).apply()
        }

    var postGestureActionColor1: Int?
        get() = prefs.getIntOrNull(KEY_POST_GESTURE_ACTION_COLOR_1)
        set(value) {
            if (value != null) {
                prefs.edit().putInt(KEY_POST_GESTURE_ACTION_COLOR_1, value).apply()
            }
        }

    var postGestureActionColor2: Int?
        get() = prefs.getIntOrNull(KEY_POST_GESTURE_ACTION_COLOR_2)
        set(value) {
            if (value != null) {
                prefs.edit().putInt(KEY_POST_GESTURE_ACTION_COLOR_2, value).apply()
            }
        }

    var postGestureActionColor3: Int?
        get() = prefs.getIntOrNull(KEY_POST_GESTURE_ACTION_COLOR_3)
        set(value) {
            if (value != null) {
                prefs.edit().putInt(KEY_POST_GESTURE_ACTION_COLOR_3, value).apply()
            }
        }

    var postGestureSize: Float
        get() = prefs.getFloat(KEY_POST_GESTURE_SIZE, 0.5f)
        set(value) {
            prefs.edit().putFloat(KEY_POST_GESTURE_SIZE, value).apply()
        }

    var commentGestureAction1: Int
        get() = prefs.getInt(KEY_COMMENT_GESTURE_ACTION_1, CommentGestureAction.Upvote)
        set(value) {
            prefs.edit().putInt(KEY_COMMENT_GESTURE_ACTION_1, value).apply()
        }

    var commentGestureAction2: Int
        get() = prefs.getInt(KEY_COMMENT_GESTURE_ACTION_2, CommentGestureAction.Downvote)
        set(value) {
            prefs.edit().putInt(KEY_COMMENT_GESTURE_ACTION_2, value).apply()
        }

    var commentGestureAction3: Int
        get() = prefs.getInt(KEY_COMMENT_GESTURE_ACTION_3, CommentGestureAction.Reply)
        set(value) {
            prefs.edit().putInt(KEY_COMMENT_GESTURE_ACTION_3, value).apply()
        }

    var commentGestureActionColor1: Int?
        get() = prefs.getIntOrNull(KEY_COMMENT_GESTURE_ACTION_COLOR_1)
        set(value) {
            if (value != null) {
                prefs.edit().putInt(KEY_COMMENT_GESTURE_ACTION_COLOR_1, value).apply()
            }
        }

    var commentGestureActionColor2: Int?
        get() = prefs.getIntOrNull(KEY_COMMENT_GESTURE_ACTION_COLOR_2)
        set(value) {
            if (value != null) {
                prefs.edit().putInt(KEY_COMMENT_GESTURE_ACTION_COLOR_2, value).apply()
            }
        }

    var commentGestureActionColor3: Int?
        get() = prefs.getIntOrNull(KEY_COMMENT_GESTURE_ACTION_COLOR_3)
        set(value) {
            if (value != null) {
                prefs.edit().putInt(KEY_COMMENT_GESTURE_ACTION_COLOR_3, value).apply()
            }
        }

    var commentGestureSize: Float
        get() = prefs.getFloat(KEY_COMMENT_GESTURE_SIZE, 0.5f)
        set(value) {
            prefs.edit().putFloat(KEY_COMMENT_GESTURE_SIZE, value).apply()
        }

    var commentThreadStyle: CommentThreadStyleId
        get() = prefs.getInt(KEY_COMMENT_THREAD_STYLE, CommentsThreadStyle.MODERN)
        set(value) {
            prefs.edit().putInt(KEY_COMMENT_THREAD_STYLE, value).apply()
        }

    var blurNsfwPosts: Boolean
        get() = prefs.getBoolean(KEY_BLUR_NSFW_POSTS, true)
        set(value) {
            prefs.edit().putBoolean(KEY_BLUR_NSFW_POSTS, value).apply()
        }

    var showLinkPosts: Boolean
        get() = prefs.getBoolean(KEY_SHOW_LINK_POSTS, true)
        set(value) {
            prefs.edit().putBoolean(KEY_SHOW_LINK_POSTS, value).apply()
        }
    var showImagePosts: Boolean
        get() = prefs.getBoolean(KEY_SHOW_IMAGE_POSTS, true)
        set(value) {
            prefs.edit().putBoolean(KEY_SHOW_IMAGE_POSTS, value).apply()
        }
    var showVideoPosts: Boolean
        get() = prefs.getBoolean(KEY_SHOW_VIDEO_POSTS, true)
        set(value) {
            prefs.edit().putBoolean(KEY_SHOW_VIDEO_POSTS, value).apply()
        }
    var showTextPosts: Boolean
        get() = prefs.getBoolean(KEY_SHOW_TEXT_POSTS, true)
        set(value) {
            prefs.edit().putBoolean(KEY_SHOW_TEXT_POSTS, value).apply()
        }
    var showNsfwPosts: Boolean
        get() = prefs.getBoolean(KEY_SHOW_NSFW_POSTS, true)
        set(value) {
            prefs.edit().putBoolean(KEY_SHOW_NSFW_POSTS, value).apply()
        }

    var globalFontSize: Int
        get() = prefs.getInt(KEY_GLOBAL_FONT_SIZE, GlobalFontSizeId.Normal)
        set(value) {
            prefs.edit().putInt(KEY_GLOBAL_FONT_SIZE, value).apply()
        }

    var globalFontColor: Int
        get() = prefs.getInt(KEY_GLOBAL_FONT_COLOR, GlobalFontColorId.Calm)
        set(value) {
            prefs.edit().putInt(KEY_GLOBAL_FONT_COLOR, value).apply()
        }

    var defaultCommunitySortOrder: CommunitySortOrder?
        get() = moshi.fromJsonSafe(prefs.getString(KEY_DEFAULT_COMMUNITY_SORT_ORDER, null))
        set(value) {
            prefs.edit()
                .putString(KEY_DEFAULT_COMMUNITY_SORT_ORDER, moshi.toJsonSafe(value))
                .apply()
        }

    var defaultCommentsSortOrder: CommentsSortOrder?
        get() = moshi.fromJsonSafe(prefs.getString(KEY_DEFAULT_COMMENTS_SORT_ORDER, null))
        set(value) {
            prefs.edit()
                .putString(KEY_DEFAULT_COMMENTS_SORT_ORDER, moshi.toJsonSafe(value))
                .apply()
        }

    var alwaysShowLinkButtonBelowPost: Boolean
        get() = prefs.getBoolean(KEY_ALWAYS_SHOW_LINK_BUTTON_BELOW_POST, false)
        set(value) {
            prefs.edit()
                .putBoolean(KEY_ALWAYS_SHOW_LINK_BUTTON_BELOW_POST, value)
                .apply()
        }

    var postListViewImageOnSingleTap: Boolean
        get() = prefs.getBoolean(KEY_POST_LIST_VIEW_IMAGE_ON_SINGLE_TAP, false)
        set(value) {
            prefs.edit()
                .putBoolean(KEY_POST_LIST_VIEW_IMAGE_ON_SINGLE_TAP, value)
                .apply()
        }

    var colorScheme: ColorSchemeId
        get() = prefs.getInt(KEY_COLOR_SCHEME, ColorSchemes.Default)
        set(value) {
            prefs.edit()
                .putInt(KEY_COLOR_SCHEME, value)
                .apply()
        }

    var commentsNavigationFab: Boolean
        get() = prefs.getBoolean(KEY_COMMENTS_NAVIGATION_FAB, false)
        set(value) {
            prefs.edit()
                .putBoolean(KEY_COMMENTS_NAVIGATION_FAB, value)
                .apply()
        }
    var useVolumeButtonNavigation: Boolean
        get() = prefs.getBoolean(KEY_USE_VOLUME_BUTTON_NAVIGATION, false)
        set(value) {
            prefs.edit()
                .putBoolean(KEY_USE_VOLUME_BUTTON_NAVIGATION, value)
                .apply()
        }
    var collapseChildCommentsByDefault: Boolean
        get() = prefs.getBoolean(KEY_COLLAPSE_CHILD_COMMENTS_BY_DEFAULT, false)
        set(value) {
            prefs.edit()
                .putBoolean(KEY_COLLAPSE_CHILD_COMMENTS_BY_DEFAULT, value)
                .apply()
        }

    var commentsNavigationFabOffX: Int
        get() = prefs.getInt(KEY_COMMENTS_NAVIGATION_FAB_OFF_X, 0)
        set(value) {
            prefs.edit()
                .putInt(KEY_COMMENTS_NAVIGATION_FAB_OFF_X, value)
                .apply()
        }

    var commentsNavigationFabOffY: Int
        get() = prefs.getInt(
            KEY_COMMENTS_NAVIGATION_FAB_OFF_Y,
            -Utils.convertDpToPixel(24f).toInt(),
        )
        set(value) {
            prefs.edit()
                .putInt(KEY_COMMENTS_NAVIGATION_FAB_OFF_Y, value)
                .apply()
        }

    var hidePostScores: Boolean
        get() = prefs.getBoolean(KEY_HIDE_POST_SCORES, false)
        set(value) {
            prefs.edit()
                .putBoolean(KEY_HIDE_POST_SCORES, value)
                .apply()
        }

    var hideCommentScores: Boolean
        get() = try {
            // Accidentally set this to an int once...
            prefs.getBoolean(KEY_HIDE_COMMENT_SCORES, false)
        } catch (e: Exception) {
            false
        }
        set(value) {
            prefs.edit()
                .putBoolean(KEY_HIDE_COMMENT_SCORES, value)
                .apply()
        }

    var globalFont: Int
        get() = prefs.getInt(KEY_GLOBAL_FONT, FontIds.DEFAULT)
        set(value) {
            prefs.edit()
                .putInt(KEY_GLOBAL_FONT, value)
                .apply()
        }

    var upvoteColor: Int
        get() = prefs.getInt(KEY_UPVOTE_COLOR, context.getColorCompat(R.color.upvoteColor))
        set(value) {
            prefs.edit()
                .apply {
                    putInt(KEY_UPVOTE_COLOR, value)
                }
                .apply()
        }
    var downvoteColor: Int
        get() = prefs.getInt(KEY_DOWNVOTE_COLOR, context.getColorCompat(R.color.downvoteColor))
        set(value) {
            prefs.edit()
                .apply {
                    putInt(KEY_DOWNVOTE_COLOR, value)
                }
                .apply()
        }

    var openLinksInExternalApp: Boolean
        get() = prefs.getBoolean(KEY_OPEN_LINKS_IN_APP, false)
        set(value) {
            prefs.edit()
                .putBoolean(KEY_OPEN_LINKS_IN_APP, value)
                .apply()
        }
    var autoLinkPhoneNumbers: Boolean
        get() = prefs.getBoolean(KEY_AUTO_LINK_PHONE_NUMBERS, false)
        set(value) {
            prefs.edit()
                .putBoolean(KEY_AUTO_LINK_PHONE_NUMBERS, value)
                .apply()
        }
    var autoLinkIpAddresses: Boolean
        get() = prefs.getBoolean(KEY_AUTO_LINK_IP_ADDRESSES, true)
        set(value) {
            prefs.edit()
                .putBoolean(KEY_AUTO_LINK_IP_ADDRESSES, value)
                .apply()
        }
    var postShowUpAndDownVotes: Boolean
        get() = prefs.getBoolean(KEY_POST_SHOW_UP_AND_DOWN_VOTES, false)
        set(value) {
            prefs.edit()
                .putBoolean(KEY_POST_SHOW_UP_AND_DOWN_VOTES, value)
                .apply()
        }
    var commentShowUpAndDownVotes: Boolean
        get() = prefs.getBoolean(KEY_COMMENT_SHOW_UP_AND_DOWN_VOTES, false)
        set(value) {
            prefs.edit()
                .putBoolean(KEY_COMMENT_SHOW_UP_AND_DOWN_VOTES, value)
                .apply()
        }

    var displayInstanceStyle: Int
        get() = prefs.getInt(
            KEY_DISPLAY_INSTANCE_STYLE,
            DisplayInstanceOptions.OnlyDisplayNonLocalInstances,
        )
        set(value) {
            prefs.edit()
                .putInt(KEY_DISPLAY_INSTANCE_STYLE, value)
                .apply()
        }

    var retainLastPost: Boolean
        get() = prefs.getBoolean(KEY_RETAIN_LAST_POST, true)
        set(value) {
            prefs.edit()
                .putBoolean(KEY_RETAIN_LAST_POST, value)
                .apply()
        }

    var leftHandMode: Boolean
        get() = prefs.getBoolean(KEY_LEFT_HAND_MODE, false)
        set(value) {
            prefs.edit()
                .putBoolean(KEY_LEFT_HAND_MODE, value)
                .apply()
        }

    var transparentNotificationBar: Boolean
        get() = prefs.getBoolean(KEY_TRANSPARENT_NOTIFICATION_BAR, false)
        set(value) {
            prefs.edit()
                .putBoolean(KEY_TRANSPARENT_NOTIFICATION_BAR, value)
                .apply()
        }

    var lockBottomBar: Boolean
        get() = prefs.getBoolean(KEY_LOCK_BOTTOM_BAR, false)
        set(value) {
            prefs.edit()
                .putBoolean(KEY_LOCK_BOTTOM_BAR, value)
                .apply()
        }

    var appVersionLastLaunch: Int
        get() = prefs.getInt(KEY_PREF_VERSION, 0)
        set(value) {
            prefs.edit()
                .putInt(KEY_PREF_VERSION, value)
                .apply()
        }

    var previewLinks: Int
        get() = try {
            prefs.getInt(KEY_PREVIEW_LINKS, PreviewTextLinks)
        } catch (e: Exception) {
            PreviewTextLinks
        }
        set(value) {
            prefs.edit()
                .putInt(KEY_PREVIEW_LINKS, value)
                .apply()
        }

    var screenshotWidthDp: Int
        get() = prefs.getInt(KEY_SCREENSHOT_WIDTH_DP, 500)
        set(value) {
            prefs.edit()
                .putInt(KEY_SCREENSHOT_WIDTH_DP, value)
                .apply()
        }

    var dateScreenshots: Boolean
        get() = prefs.getBoolean(KEY_DATE_SCREENSHOTS, true)
        set(value) {
            prefs.edit()
                .putBoolean(KEY_DATE_SCREENSHOTS, value)
                .apply()
        }

    var screenshotWatermark: Int
        get() = prefs.getInt(KEY_SCREENSHOT_WATERMARK, ScreenshotWatermarkId.LEMMY)
        set(value) {
            prefs.edit()
                .putInt(KEY_SCREENSHOT_WATERMARK, value)
                .apply()
        }

    var useFirebase: Boolean
        get() = prefs.getBoolean(KEY_USE_FIREBASE, true)
        set(value) {
            prefs.edit()
                .putBoolean(KEY_USE_FIREBASE, value)
                .apply()
        }

    var autoCollapseCommentThreshold: Float
        get() = prefs.getFloat(KEY_AUTO_COLLAPSE_COMMENT_THRESHOLD, 0.3f)
        set(value) {
            prefs.edit()
                .putFloat(KEY_AUTO_COLLAPSE_COMMENT_THRESHOLD, value)
                .apply()
        }

    var trackBrowsingHistory: Boolean
        get() = prefs.getBoolean(KEY_TRACK_BROWSING_HISTORY, true)
        set(value) {
            prefs.edit()
                .putBoolean(KEY_TRACK_BROWSING_HISTORY, value)
                .apply()
        }

    var useCustomNavBar: Boolean
        get() = prefs.getBoolean(KEY_USE_CUSTOM_NAV_BAR, false)
        set(value) {
            prefs.edit()
                .putBoolean(KEY_USE_CUSTOM_NAV_BAR, value)
                .apply()
        }

    var navBarConfig: NavBarConfig
        get() = prefs.getMoshiValue<NavBarConfig>(KEY_NAV_BAR_ITEMS) ?: NavBarConfig()
        set(value) {
            prefs.putMoshiValue(KEY_NAV_BAR_ITEMS, value)
        }

    var useBottomNavBar: Boolean
        get() = prefs.getBoolean(KEY_USE_BOTTOM_NAV_BAR, true)
        set(value) {
            prefs.edit()
                .putBoolean(KEY_USE_BOTTOM_NAV_BAR, value)
                .apply()
        }

    var isHiddenPostsEnabled: Boolean
        get() = prefs.getBoolean(KEY_ENABLE_HIDDEN_POSTS, true)
        set(value) {
            prefs.edit()
                .putBoolean(KEY_ENABLE_HIDDEN_POSTS, value)
                .apply()
        }

    var usePredictiveBack: Boolean
        get() = prefs.getBoolean(KEY_USE_PREDICTIVE_BACK, false)
        set(value) {
            prefs.edit()
                .putBoolean(KEY_USE_PREDICTIVE_BACK, value)
                .apply()
        }

    var autoLoadMorePosts: Boolean
        get() = prefs.getBoolean(KEY_AUTO_LOAD_MORE_POSTS, true)
        set(value) {
            prefs.edit()
                .putBoolean(KEY_AUTO_LOAD_MORE_POSTS, value)
                .apply()
        }

    var infinityPageIndicator: Boolean
        get() = prefs.getBoolean(KEY_INFINITY_PAGE_INDICATOR, false)
        set(value) {
            prefs.edit()
                .putBoolean(KEY_INFINITY_PAGE_INDICATOR, value)
                .apply()
        }

    var warnReplyToOldContent: Boolean
        get() = prefs.getBoolean(KEY_WARN_REPLY_TO_OLD_CONTENT, true)
        set(value) {
            prefs.edit()
                .putBoolean(KEY_WARN_REPLY_TO_OLD_CONTENT, value)
                .apply()
        }

    var warnReplyToOldContentThresholdMs: Long
        get() = prefs.getLongSafe(
            KEY_WARN_REPLY_TO_OLD_CONTENT_THRESHOLD_MS,
            Duration.ofDays(2).toMillis(),
        )
        set(value) {
            prefs.edit()
                .putLong(KEY_WARN_REPLY_TO_OLD_CONTENT_THRESHOLD_MS, value)
                .apply()
        }

    var showPostUpvotePercentage: Boolean
        get() = prefs.getBoolean(KEY_SHOW_POST_UPVOTE_PERCENTAGE, false)
        set(value) {
            prefs.edit()
                .putBoolean(KEY_SHOW_POST_UPVOTE_PERCENTAGE, value)
                .apply()
        }

    var showCommentUpvotePercentage: Boolean
        get() = prefs.getBoolean(KEY_SHOW_COMMENT_UPVOTE_PERCENTAGE, false)
        set(value) {
            prefs.edit()
                .putBoolean(KEY_SHOW_COMMENT_UPVOTE_PERCENTAGE, value)
                .apply()
        }

    var useMultilinePostHeaders: Boolean
        get() = prefs.getBoolean(KEY_USE_MULTILINE_POST_HEADERS, true)
        set(value) {
            prefs.edit()
                .putBoolean(KEY_USE_MULTILINE_POST_HEADERS, value)
                .apply()
        }

    var indicatePostsAndCommentsCreatedByCurrentUser: Boolean
        get() = prefs.getBoolean(KEY_INDICATE_CONTENT_FROM_CURRENT_USER, true)
        set(value) {
            prefs.edit()
                .putBoolean(KEY_INDICATE_CONTENT_FROM_CURRENT_USER, value)
                .apply()
        }

    var saveDraftsAutomatically: Boolean
        get() = prefs.getBoolean(KEY_SAVE_DRAFTS_AUTOMATICALLY, true)
        set(value) {
            prefs.edit()
                .putBoolean(KEY_SAVE_DRAFTS_AUTOMATICALLY, value)
                .apply()
        }

    var showProfileIcons: Boolean
        get() = prefs.getBoolean(KEY_SHOW_PROFILE_ICONS, true)
        set(value) {
            prefs.edit()
                .putBoolean(KEY_SHOW_PROFILE_ICONS, value)
                .apply()
        }

    var commentHeaderLayout: Int
        get() = prefs.getInt(KEY_COMMENT_HEADER_LAYOUT, CommentHeaderLayoutId.SingleLine)
        set(value) {
            prefs.edit()
                .putInt(KEY_COMMENT_HEADER_LAYOUT, value)
                .apply()
        }

    var navigationRailMode: Int
        get() = prefs.getInt(KEY_NAVIGATION_RAIL_MODE, NavigationRailModeId.ManualOff)
        set(value) {
            prefs.edit()
                .putInt(KEY_NAVIGATION_RAIL_MODE, value)
                .apply()
        }

    var downloadDirectory: String?
        get() = prefs.getString(KEY_DOWNLOAD_DIRECTORY, null)
        set(value) {
            prefs.edit()
                .putString(KEY_DOWNLOAD_DIRECTORY, value)
                .apply()
        }

    var usePerCommunitySettings: Boolean
        get() = prefs.getBoolean(KEY_USE_PER_COMMUNITY_SETTINGS, true)
        set(value) {
            prefs.edit()
                .putBoolean(KEY_USE_PER_COMMUNITY_SETTINGS, value)
                .apply()
        }

    var guestAccountSettings: GuestAccountSettings?
        get() = prefs.getMoshiValue(KEY_GUEST_ACCOUNT_SETTINGS)
        set(value) {
            prefs.putMoshiValue(KEY_GUEST_ACCOUNT_SETTINGS, value)
        }

    var textFieldToolbarSettings: TextFieldToolbarSettings?
        get() = prefs.getMoshiValue(KEY_TEXT_FIELD_TOOLBAR_SETTINGS)
        set(value) {
            prefs.putMoshiValue(KEY_TEXT_FIELD_TOOLBAR_SETTINGS, value)
        }

    var postQuickActions: PostQuickActionsSettings?
        get() =
            prefs.getMoshiValue(KEY_POST_QUICK_ACTIONS)
        set(value) {
            prefs.putMoshiValue(KEY_POST_QUICK_ACTIONS, value)
        }

    var commentQuickActions: CommentQuickActionsSettings?
        get() =
            prefs.getMoshiValue(KEY_COMMENT_QUICK_ACTIONS)
        set(value) {
            prefs.putMoshiValue(KEY_COMMENT_QUICK_ACTIONS, value)
        }

    var globalLayoutMode: GlobalLayoutMode
        get() = prefs.getInt(KEY_GLOBAL_LAYOUT_MODE, GlobalLayoutModes.Auto)
        set(value) {
            prefs.edit()
                .putInt(KEY_GLOBAL_LAYOUT_MODE, value)
                .apply()
        }

    var rotateInstanceOnUploadFail: Boolean
        get() = prefs.getBoolean(KEY_ROTATE_INSTANCE_ON_UPLOAD_FAIL, false)
        set(value) {
            prefs.edit()
                .putBoolean(KEY_ROTATE_INSTANCE_ON_UPLOAD_FAIL, value)
                .apply()
        }

    var showFilteredPosts: Boolean
        get() = prefs.getBoolean(KEY_SHOW_FILTERED_POSTS, false)
        set(value) {
            prefs.edit()
                .putBoolean(KEY_SHOW_FILTERED_POSTS, value)
                .apply()
        }

    var commentsShowInlineMediaAsLinks: Boolean
        get() = prefs.getBoolean(KEY_COMMENTS_SHOW_INLINE_MEDIA_AS_LINKS, false)
        set(value) {
            prefs.edit()
                .putBoolean(KEY_COMMENTS_SHOW_INLINE_MEDIA_AS_LINKS, value)
                .apply()
        }

    var isNotificationsOn: Boolean
        get() = prefs.getBoolean(KEY_IS_NOTIFICATIONS_ON, false)
        set(value) {
            prefs.edit()
                .putBoolean(KEY_IS_NOTIFICATIONS_ON, value)
                .apply()
        }

    var lastAccountNotificationId: Int
        get() = prefs.getInt(KEY_LAST_ACCOUNT_NOTIFICATION_ID, 0)
        set(value) {
            prefs.edit()
                .putInt(KEY_LAST_ACCOUNT_NOTIFICATION_ID, value)
                .apply()
        }

    var notificationsCheckIntervalMs: Long
        get() = prefs.getLongSafe(KEY_NOTIFICATIONS_CHECK_INTERVAL_MS, 1000L * 60L * 15L)
        set(value) {
            prefs.edit()
                .putLong(KEY_NOTIFICATIONS_CHECK_INTERVAL_MS, value)
                .apply()
        }

    var homeFabQuickAction: Int
        get() = prefs.getInt(KEY_HOME_FAB_QUICK_ACTION, HomeFabQuickActionIds.None)
        set(value) {
            prefs.edit()
                .putInt(KEY_HOME_FAB_QUICK_ACTION, value)
                .apply()
        }

    var showEditedDate: Boolean
        get() = prefs.getBoolean(KEY_SHOW_EDITED_DATE, true)
        set(value) {
            prefs.edit()
                .putBoolean(KEY_SHOW_EDITED_DATE, value)
                .apply()
        }

    var imagePreviewHideUiByDefault: Boolean
        get() = prefs.getBoolean(KEY_IMAGE_PREVIEW_HIDE_UI_BY_DEFAULT, false)
        set(value) {
            prefs.edit()
                .putBoolean(KEY_IMAGE_PREVIEW_HIDE_UI_BY_DEFAULT, value)
                .apply()
        }

    var prefetchPosts: Boolean
        get() = prefs.getBoolean(KEY_PREFETCH_POSTS, true)
        set(value) {
            prefs.edit()
                .putBoolean(KEY_PREFETCH_POSTS, value)
                .apply()
        }

    var autoPlayVideos: Boolean
        get() = prefs.getBoolean(KEY_AUTO_PLAY_VIDEOS, true)
        set(value) {
            prefs.edit()
                .putBoolean(KEY_AUTO_PLAY_VIDEOS, value)
                .apply()
        }

    var uploadImagesToImgur: Boolean
        get() = prefs.getBoolean(KEY_UPLOAD_IMAGES_TO_IMGUR, false)
        set(value) {
            prefs.edit()
                .putBoolean(KEY_UPLOAD_IMAGES_TO_IMGUR, value)
                .apply()
        }

    var animationLevel: AnimationsHelper.AnimationLevel
        get() = AnimationsHelper.AnimationLevel.parse(
            prefs.getInt(
                KEY_ANIMATION_LEVEL,
                AnimationsHelper.AnimationLevel.Max.animationLevel,
            ),
        )
        set(value) {
            prefs.edit()
                .putInt(KEY_ANIMATION_LEVEL, value.animationLevel)
                .apply()
        }

    var cachePolicy: CachePolicy
        get() = CachePolicy.parse(prefs.getInt(KEY_CACHE_POLICY, CachePolicy.Moderate.value))
        set(value) {
            prefs.edit()
                .putInt(KEY_CACHE_POLICY, value.value)
                .apply()
        }

    var useCondensedTypefaceForCommentHeaders: Boolean
        get() = prefs.getBoolean(KEY_USE_CONDENSED_FOR_COMMENT_HEADERS, true)
        set(value) {
            prefs.edit()
                .putBoolean(KEY_USE_CONDENSED_FOR_COMMENT_HEADERS, value)
                .apply()
        }

    var parseMarkdownInPostTitles: Boolean
        get() = prefs.getBoolean(KEY_PARSE_MARKDOWN_IN_POST_TITLES, true)
        set(value) {
            prefs.edit()
                .putBoolean(KEY_PARSE_MARKDOWN_IN_POST_TITLES, value)
                .apply()
        }

    var searchHomeConfig: SearchHomeConfig
        get() =
            prefs.getMoshiValue<SearchHomeConfig>(KEY_SEARCH_HOME_CONFIG)
                ?: SearchHomeConfig()
        set(value) {
            prefs.putMoshiValue(KEY_SEARCH_HOME_CONFIG, value)
        }

    var postFeedShowScrollBar: Boolean
        get() = prefs.getBoolean(KEY_POST_FEED_SHOW_SCROLL_BAR, true)
        set(value) {
            prefs.edit()
                .putBoolean(KEY_POST_FEED_SHOW_SCROLL_BAR, value)
                .apply()
        }

    var hapticsEnabled: Boolean
        get() = prefs.getBoolean(KEY_HAPTICS_ENABLED, true)
        set(value) {
            prefs.edit()
                .putBoolean(KEY_HAPTICS_ENABLED, value)
                .apply()
        }

    var hapticsOnActions: Boolean
        get() = hapticsEnabled && prefs.getBoolean(KEY_HAPTICS_ON_ACTIONS, true)
        set(value) {
            prefs.edit()
                .putBoolean(KEY_HAPTICS_ON_ACTIONS, value)
                .apply()
        }

    var hideDuplicatePostsOnRead: Boolean
        get() = prefs.getBoolean(KEY_HIDE_DUPLICATE_POSTS_ON_READ, false)
        set(value) {
            prefs.edit()
                .putBoolean(KEY_HIDE_DUPLICATE_POSTS_ON_READ, value)
                .apply()
        }

    var usePostsFeedHeader: Boolean
        get() = prefs.getBoolean(KEY_USE_POSTS_FEED_HEADER, false)
        set(value) {
            prefs.edit()
                .putBoolean(KEY_USE_POSTS_FEED_HEADER, value)
                .apply()
        }

    suspend fun getOfflinePostCount(): Int =
        context.offlineModeDataStore.data.first()[intPreferencesKey("offlinePostCount")]
            ?: 100

    fun reset(key: String) {
        prefs.edit().remove(key).apply()
    }

    fun asJson(): JSONObject {
        val json = JSONObject()

        for ((key, value) in all.entries) {
            when (value) {
                is String -> json.put(key, value)
                is Boolean -> json.put(key, value)
                is Number -> json.put(key, value)
                null -> json.put(key, null)
                else -> Log.d(TAG, "Unsupported type ${value::class}. Key was $key.")
            }
        }

        json.put(PreferenceUtil.PREFERENCE_VERSION_CODE, BuildConfig.VERSION_CODE)

        return json
    }

    fun generateCode(): String {
        val json = this.asJson()
        return Utils.compress(json.toString(), Base64.NO_WRAP)
    }

    fun importSettings(settingsToImport: JSONObject, excludeKeys: Set<String>) {
        val allKeys = settingsToImport.keys().asSequence()
        val editor = prefs.edit()

        for (key in allKeys) {
            if (excludeKeys.contains(key)) continue

            when (val value = settingsToImport.opt(key)) {
                is Float -> editor.putFloat(key, value)
                is Long -> editor.putLong(key, value)
                is String -> editor.putString(key, value)
                is Boolean -> editor.putBoolean(key, value)
                is Int -> editor.putInt(key, value)
                null -> editor.remove(key)
            }
        }

        editor.apply()
    }

    fun clear() {
        prefs.edit().clear().commit()
    }
}
