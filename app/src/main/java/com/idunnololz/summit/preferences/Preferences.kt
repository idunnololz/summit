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
import com.idunnololz.summit.util.BooleanPreferenceDelegate
import com.idunnololz.summit.util.FloatPreferenceDelegate
import com.idunnololz.summit.util.IntPreferenceDelegate
import com.idunnololz.summit.util.JsonPreferenceDelegate
import com.idunnololz.summit.util.LongPreferenceDelegate
import com.idunnololz.summit.util.NullableIntPreferenceDelegate
import com.idunnololz.summit.util.PreferenceUtils
import com.idunnololz.summit.util.PreferenceUtils.KEY_ALWAYS_SHOW_LINK_BUTTON_BELOW_POST
import com.idunnololz.summit.util.PreferenceUtils.KEY_ANIMATION_LEVEL
import com.idunnololz.summit.util.PreferenceUtils.KEY_AUTO_COLLAPSE_COMMENT_THRESHOLD
import com.idunnololz.summit.util.PreferenceUtils.KEY_AUTO_LINK_IP_ADDRESSES
import com.idunnololz.summit.util.PreferenceUtils.KEY_AUTO_LINK_PHONE_NUMBERS
import com.idunnololz.summit.util.PreferenceUtils.KEY_AUTO_LOAD_MORE_POSTS
import com.idunnololz.summit.util.PreferenceUtils.KEY_AUTO_PLAY_VIDEOS
import com.idunnololz.summit.util.PreferenceUtils.KEY_BASE_THEME
import com.idunnololz.summit.util.PreferenceUtils.KEY_BLUR_NSFW_POSTS
import com.idunnololz.summit.util.PreferenceUtils.KEY_CACHE_POLICY
import com.idunnololz.summit.util.PreferenceUtils.KEY_COLLAPSE_CHILD_COMMENTS_BY_DEFAULT
import com.idunnololz.summit.util.PreferenceUtils.KEY_COLOR_SCHEME
import com.idunnololz.summit.util.PreferenceUtils.KEY_COMMENTS_NAVIGATION_FAB
import com.idunnololz.summit.util.PreferenceUtils.KEY_COMMENTS_NAVIGATION_FAB_OFF_X
import com.idunnololz.summit.util.PreferenceUtils.KEY_COMMENTS_NAVIGATION_FAB_OFF_Y
import com.idunnololz.summit.util.PreferenceUtils.KEY_COMMENTS_SHOW_INLINE_MEDIA_AS_LINKS
import com.idunnololz.summit.util.PreferenceUtils.KEY_COMMENT_GESTURE_ACTION_1
import com.idunnololz.summit.util.PreferenceUtils.KEY_COMMENT_GESTURE_ACTION_2
import com.idunnololz.summit.util.PreferenceUtils.KEY_COMMENT_GESTURE_ACTION_3
import com.idunnololz.summit.util.PreferenceUtils.KEY_COMMENT_GESTURE_ACTION_COLOR_1
import com.idunnololz.summit.util.PreferenceUtils.KEY_COMMENT_GESTURE_ACTION_COLOR_2
import com.idunnololz.summit.util.PreferenceUtils.KEY_COMMENT_GESTURE_ACTION_COLOR_3
import com.idunnololz.summit.util.PreferenceUtils.KEY_COMMENT_GESTURE_SIZE
import com.idunnololz.summit.util.PreferenceUtils.KEY_COMMENT_HEADER_LAYOUT
import com.idunnololz.summit.util.PreferenceUtils.KEY_COMMENT_QUICK_ACTIONS
import com.idunnololz.summit.util.PreferenceUtils.KEY_COMMENT_SHOW_UP_AND_DOWN_VOTES
import com.idunnololz.summit.util.PreferenceUtils.KEY_COMMENT_THREAD_STYLE
import com.idunnololz.summit.util.PreferenceUtils.KEY_DATE_SCREENSHOTS
import com.idunnololz.summit.util.PreferenceUtils.KEY_DEFAULT_COMMENTS_SORT_ORDER
import com.idunnololz.summit.util.PreferenceUtils.KEY_DEFAULT_COMMUNITY_SORT_ORDER
import com.idunnololz.summit.util.PreferenceUtils.KEY_DEFAULT_PAGE
import com.idunnololz.summit.util.PreferenceUtils.KEY_DISPLAY_INSTANCE_STYLE
import com.idunnololz.summit.util.PreferenceUtils.KEY_DOWNLOAD_DIRECTORY
import com.idunnololz.summit.util.PreferenceUtils.KEY_DOWNVOTE_COLOR
import com.idunnololz.summit.util.PreferenceUtils.KEY_ENABLE_HIDDEN_POSTS
import com.idunnololz.summit.util.PreferenceUtils.KEY_GLOBAL_FONT
import com.idunnololz.summit.util.PreferenceUtils.KEY_GLOBAL_FONT_COLOR
import com.idunnololz.summit.util.PreferenceUtils.KEY_GLOBAL_FONT_SIZE
import com.idunnololz.summit.util.PreferenceUtils.KEY_GLOBAL_LAYOUT_MODE
import com.idunnololz.summit.util.PreferenceUtils.KEY_GUEST_ACCOUNT_SETTINGS
import com.idunnololz.summit.util.PreferenceUtils.KEY_HAPTICS_ENABLED
import com.idunnololz.summit.util.PreferenceUtils.KEY_HAPTICS_ON_ACTIONS
import com.idunnololz.summit.util.PreferenceUtils.KEY_HIDE_COMMENT_ACTIONS
import com.idunnololz.summit.util.PreferenceUtils.KEY_HIDE_COMMENT_SCORES
import com.idunnololz.summit.util.PreferenceUtils.KEY_HIDE_DUPLICATE_POSTS_ON_READ
import com.idunnololz.summit.util.PreferenceUtils.KEY_HIDE_POST_SCORES
import com.idunnololz.summit.util.PreferenceUtils.KEY_HOME_FAB_QUICK_ACTION
import com.idunnololz.summit.util.PreferenceUtils.KEY_IMAGE_PREVIEW_HIDE_UI_BY_DEFAULT
import com.idunnololz.summit.util.PreferenceUtils.KEY_INDICATE_CONTENT_FROM_CURRENT_USER
import com.idunnololz.summit.util.PreferenceUtils.KEY_INFINITY
import com.idunnololz.summit.util.PreferenceUtils.KEY_INFINITY_PAGE_INDICATOR
import com.idunnololz.summit.util.PreferenceUtils.KEY_INLINE_VIDEO_DEFAULT_VOLUME
import com.idunnololz.summit.util.PreferenceUtils.KEY_IS_NOTIFICATIONS_ON
import com.idunnololz.summit.util.PreferenceUtils.KEY_LAST_ACCOUNT_NOTIFICATION_ID
import com.idunnololz.summit.util.PreferenceUtils.KEY_LEFT_HAND_MODE
import com.idunnololz.summit.util.PreferenceUtils.KEY_LOCK_BOTTOM_BAR
import com.idunnololz.summit.util.PreferenceUtils.KEY_MARK_POSTS_AS_READ_ON_SCROLL
import com.idunnololz.summit.util.PreferenceUtils.KEY_NAVIGATION_RAIL_MODE
import com.idunnololz.summit.util.PreferenceUtils.KEY_NAV_BAR_ITEMS
import com.idunnololz.summit.util.PreferenceUtils.KEY_NOTIFICATIONS_CHECK_INTERVAL_MS
import com.idunnololz.summit.util.PreferenceUtils.KEY_OPEN_LINKS_IN_APP
import com.idunnololz.summit.util.PreferenceUtils.KEY_PARSE_MARKDOWN_IN_POST_TITLES
import com.idunnololz.summit.util.PreferenceUtils.KEY_POST_AND_COMMENTS_UI_CONFIG
import com.idunnololz.summit.util.PreferenceUtils.KEY_POST_FEED_SHOW_SCROLL_BAR
import com.idunnololz.summit.util.PreferenceUtils.KEY_POST_GESTURE_ACTION_1
import com.idunnololz.summit.util.PreferenceUtils.KEY_POST_GESTURE_ACTION_2
import com.idunnololz.summit.util.PreferenceUtils.KEY_POST_GESTURE_ACTION_3
import com.idunnololz.summit.util.PreferenceUtils.KEY_POST_GESTURE_ACTION_COLOR_1
import com.idunnololz.summit.util.PreferenceUtils.KEY_POST_GESTURE_ACTION_COLOR_2
import com.idunnololz.summit.util.PreferenceUtils.KEY_POST_GESTURE_ACTION_COLOR_3
import com.idunnololz.summit.util.PreferenceUtils.KEY_POST_GESTURE_SIZE
import com.idunnololz.summit.util.PreferenceUtils.KEY_POST_LIST_VIEW_IMAGE_ON_SINGLE_TAP
import com.idunnololz.summit.util.PreferenceUtils.KEY_POST_QUICK_ACTIONS
import com.idunnololz.summit.util.PreferenceUtils.KEY_POST_SHOW_UP_AND_DOWN_VOTES
import com.idunnololz.summit.util.PreferenceUtils.KEY_PREFETCH_POSTS
import com.idunnololz.summit.util.PreferenceUtils.KEY_PREF_VERSION
import com.idunnololz.summit.util.PreferenceUtils.KEY_PREVIEW_LINKS
import com.idunnololz.summit.util.PreferenceUtils.KEY_RETAIN_LAST_POST
import com.idunnololz.summit.util.PreferenceUtils.KEY_ROTATE_INSTANCE_ON_UPLOAD_FAIL
import com.idunnololz.summit.util.PreferenceUtils.KEY_SAVE_DRAFTS_AUTOMATICALLY
import com.idunnololz.summit.util.PreferenceUtils.KEY_SCREENSHOT_WATERMARK
import com.idunnololz.summit.util.PreferenceUtils.KEY_SCREENSHOT_WIDTH_DP
import com.idunnololz.summit.util.PreferenceUtils.KEY_SEARCH_HOME_CONFIG
import com.idunnololz.summit.util.PreferenceUtils.KEY_SHOW_COMMENT_UPVOTE_PERCENTAGE
import com.idunnololz.summit.util.PreferenceUtils.KEY_SHOW_EDITED_DATE
import com.idunnololz.summit.util.PreferenceUtils.KEY_SHOW_FILTERED_POSTS
import com.idunnololz.summit.util.PreferenceUtils.KEY_SHOW_IMAGE_POSTS
import com.idunnololz.summit.util.PreferenceUtils.KEY_SHOW_LINK_POSTS
import com.idunnololz.summit.util.PreferenceUtils.KEY_SHOW_NSFW_POSTS
import com.idunnololz.summit.util.PreferenceUtils.KEY_SHOW_POST_UPVOTE_PERCENTAGE
import com.idunnololz.summit.util.PreferenceUtils.KEY_SHOW_PROFILE_ICONS
import com.idunnololz.summit.util.PreferenceUtils.KEY_SHOW_TEXT_POSTS
import com.idunnololz.summit.util.PreferenceUtils.KEY_SHOW_VIDEO_POSTS
import com.idunnololz.summit.util.PreferenceUtils.KEY_TAP_COMMENT_TO_COLLAPSE
import com.idunnololz.summit.util.PreferenceUtils.KEY_TEXT_FIELD_TOOLBAR_SETTINGS
import com.idunnololz.summit.util.PreferenceUtils.KEY_TRACK_BROWSING_HISTORY
import com.idunnololz.summit.util.PreferenceUtils.KEY_TRANSPARENT_NOTIFICATION_BAR
import com.idunnololz.summit.util.PreferenceUtils.KEY_UPLOAD_IMAGES_TO_IMGUR
import com.idunnololz.summit.util.PreferenceUtils.KEY_UPVOTE_COLOR
import com.idunnololz.summit.util.PreferenceUtils.KEY_USE_BOTTOM_NAV_BAR
import com.idunnololz.summit.util.PreferenceUtils.KEY_USE_CONDENSED_FOR_COMMENT_HEADERS
import com.idunnololz.summit.util.PreferenceUtils.KEY_USE_CUSTOM_NAV_BAR
import com.idunnololz.summit.util.PreferenceUtils.KEY_USE_FIREBASE
import com.idunnololz.summit.util.PreferenceUtils.KEY_USE_GESTURE_ACTIONS
import com.idunnololz.summit.util.PreferenceUtils.KEY_USE_LESS_DARK_BACKGROUND
import com.idunnololz.summit.util.PreferenceUtils.KEY_USE_MULTILINE_POST_HEADERS
import com.idunnololz.summit.util.PreferenceUtils.KEY_USE_PER_COMMUNITY_SETTINGS
import com.idunnololz.summit.util.PreferenceUtils.KEY_USE_POSTS_FEED_HEADER
import com.idunnololz.summit.util.PreferenceUtils.KEY_USE_PREDICTIVE_BACK
import com.idunnololz.summit.util.PreferenceUtils.KEY_USE_VOLUME_BUTTON_NAVIGATION
import com.idunnololz.summit.util.PreferenceUtils.KEY_WARN_REPLY_TO_OLD_CONTENT
import com.idunnololz.summit.util.PreferenceUtils.KEY_WARN_REPLY_TO_OLD_CONTENT_THRESHOLD_MS
import com.idunnololz.summit.util.StringPreferenceDelegate
import com.idunnololz.summit.util.Utils
import com.idunnololz.summit.util.ext.getColorCompat
import com.idunnololz.summit.util.ext.getJsonValue
import com.idunnololz.summit.util.ext.putJsonValue
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import org.json.JSONObject

private val Context.offlineModeDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "offlineModePreferences",
)

class Preferences(
    @ApplicationContext private val context: Context,
    val prefs: SharedPreferences,
    private val coroutineScopeFactory: CoroutineScopeFactory,
    private val json: Json,
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

    var defaultPage: CommunityRef
        by jsonPreference(KEY_DEFAULT_PAGE) { CommunityRef.All() }

    fun getPostsLayout(): CommunityLayout = try {
        CommunityLayout.valueOf(
            PreferenceUtils.preferences
                .getString(PreferenceUtils.KEY_COMMUNITY_LAYOUT, null) ?: "",
        )
    } catch (e: IllegalArgumentException) {
        CommunityLayout.List
    }

    fun setPostsLayout(layout: CommunityLayout) {
        PreferenceUtils.preferences.edit()
            .putString(PreferenceUtils.KEY_COMMUNITY_LAYOUT, layout.name)
            .apply()
    }

    fun getPostInListUiConfig(): PostInListUiConfig {
        return getPostInListUiConfig(getPostsLayout())
    }

    fun setPostInListUiConfig(config: PostInListUiConfig) {
        prefs.putJsonValue(json, getPostUiConfigKey(getPostsLayout()), config)
    }

    fun getPostInListUiConfig(layout: CommunityLayout): PostInListUiConfig {
        return prefs.getJsonValue<PostInListUiConfig>(json, getPostUiConfigKey(layout))
            ?: layout.getDefaultPostUiConfig()
    }

    var postAndCommentsUiConfig: PostAndCommentsUiConfig
        by jsonPreference(KEY_POST_AND_COMMENTS_UI_CONFIG) { getDefaultPostAndCommentsUiConfig() }

    private fun getPostUiConfigKey(layout: CommunityLayout) = when (layout) {
        CommunityLayout.Compact ->
            PreferenceUtils.KEY_POST_UI_CONFIG_COMPACT
        CommunityLayout.List ->
            PreferenceUtils.KEY_POST_UI_CONFIG_LIST
        CommunityLayout.LargeList ->
            PreferenceUtils.KEY_POST_UI_CONFIG_LARGE_LIST
        CommunityLayout.Card ->
            PreferenceUtils.KEY_POST_UI_CONFIG_CARD
        CommunityLayout.Card2 ->
            PreferenceUtils.KEY_POST_UI_CONFIG_CARD2
        CommunityLayout.Card3 ->
            PreferenceUtils.KEY_POST_UI_CONFIG_CARD3
        CommunityLayout.Full ->
            PreferenceUtils.KEY_POST_UI_CONFIG_FULL
        CommunityLayout.ListWithCards ->
            PreferenceUtils.KEY_POST_UI_CONFIG_LIST_WITH_CARDS
        CommunityLayout.FullWithCards ->
            PreferenceUtils.KEY_POST_UI_CONFIG_FULL_WITH_CARDS
    }

    fun isUseMaterialYou(): Boolean {
        return prefs.getBoolean(PreferenceUtils.KEY_USE_MATERIAL_YOU, false)
    }

    fun setUseMaterialYou(b: Boolean) {
        prefs.edit().putBoolean(PreferenceUtils.KEY_USE_MATERIAL_YOU, b).apply()
    }

    fun isBlackTheme(): Boolean {
        return prefs.getBoolean(PreferenceUtils.KEY_USE_BLACK_THEME, false)
    }

    fun setUseBlackTheme(b: Boolean) {
        prefs.edit().putBoolean(PreferenceUtils.KEY_USE_BLACK_THEME, b).apply()
    }

    var baseTheme: BaseTheme
        by jsonPreference(KEY_BASE_THEME) { BaseTheme.Dark }

    var useLessDarkBackgroundTheme: Boolean
        by booleanPreference(KEY_USE_LESS_DARK_BACKGROUND, false)

    var markPostsAsReadOnScroll: Boolean
        by booleanPreference(KEY_MARK_POSTS_AS_READ_ON_SCROLL, false)

    var useGestureActions: Boolean
        by booleanPreference(KEY_USE_GESTURE_ACTIONS, true)

    var hideCommentActions: Boolean
        by booleanPreference(KEY_HIDE_COMMENT_ACTIONS, true)

    var tapCommentToCollapse: Boolean
        by booleanPreference(KEY_TAP_COMMENT_TO_COLLAPSE, false)

    var infinity: Boolean
        by booleanPreference(KEY_INFINITY, true)

    var postGestureAction1: Int
        by intPreference(KEY_POST_GESTURE_ACTION_1, PostGestureAction.Upvote)

    var postGestureAction2: Int
        by intPreference(KEY_POST_GESTURE_ACTION_2, PostGestureAction.Reply)

    var postGestureAction3: Int
        by intPreference(KEY_POST_GESTURE_ACTION_3, PostGestureAction.MarkAsRead)

    var postGestureActionColor1: Int?
        by nullableIntPreference(KEY_POST_GESTURE_ACTION_COLOR_1)

    var postGestureActionColor2: Int?
        by nullableIntPreference(KEY_POST_GESTURE_ACTION_COLOR_2)

    var postGestureActionColor3: Int?
        by nullableIntPreference(KEY_POST_GESTURE_ACTION_COLOR_3)

    var postGestureSize: Float
        by floatPreference(KEY_POST_GESTURE_SIZE, 0.5f)

    var commentGestureAction1: Int
        by intPreference(KEY_COMMENT_GESTURE_ACTION_1, CommentGestureAction.Upvote)

    var commentGestureAction2: Int
        by intPreference(KEY_COMMENT_GESTURE_ACTION_2, CommentGestureAction.Downvote)

    var commentGestureAction3: Int
        by intPreference(KEY_COMMENT_GESTURE_ACTION_3, CommentGestureAction.Reply)

    var commentGestureActionColor1: Int?
        by nullableIntPreference(KEY_COMMENT_GESTURE_ACTION_COLOR_1)

    var commentGestureActionColor2: Int?
        by nullableIntPreference(KEY_COMMENT_GESTURE_ACTION_COLOR_2)

    var commentGestureActionColor3: Int?
        by nullableIntPreference(KEY_COMMENT_GESTURE_ACTION_COLOR_3)

    var commentGestureSize: Float
        by floatPreference(KEY_COMMENT_GESTURE_SIZE, 0.5f)

    var commentThreadStyle: CommentThreadStyleId
        by intPreference(KEY_COMMENT_THREAD_STYLE, CommentsThreadStyle.MODERN)

    var blurNsfwPosts: Boolean
        by booleanPreference(KEY_BLUR_NSFW_POSTS, true)

    var showLinkPosts: Boolean
        by booleanPreference(KEY_SHOW_LINK_POSTS, true)
    var showImagePosts: Boolean
        by booleanPreference(KEY_SHOW_IMAGE_POSTS, true)
    var showVideoPosts: Boolean
        by booleanPreference(KEY_SHOW_VIDEO_POSTS, true)
    var showTextPosts: Boolean
        by booleanPreference(KEY_SHOW_TEXT_POSTS, true)
    var showNsfwPosts: Boolean
        by booleanPreference(KEY_SHOW_NSFW_POSTS, true)

    var globalFontSize: Int
        by intPreference(KEY_GLOBAL_FONT_SIZE, GlobalFontSizeId.Normal)

    var globalFontColor: Int
        by intPreference(KEY_GLOBAL_FONT_COLOR, GlobalFontColorId.Calm)

    var defaultCommunitySortOrder: CommunitySortOrder?
        by jsonPreference(KEY_DEFAULT_COMMUNITY_SORT_ORDER) { null }
    var defaultCommentsSortOrder: CommentsSortOrder?
        by jsonPreference(KEY_DEFAULT_COMMENTS_SORT_ORDER) { null }

    var alwaysShowLinkButtonBelowPost: Boolean
        by booleanPreference(KEY_ALWAYS_SHOW_LINK_BUTTON_BELOW_POST, false)

    var postListViewImageOnSingleTap: Boolean
        by booleanPreference(KEY_POST_LIST_VIEW_IMAGE_ON_SINGLE_TAP, false)

    var colorScheme: ColorSchemeId
        by intPreference(KEY_COLOR_SCHEME, ColorSchemes.Default)

    var commentsNavigationFab: Boolean
        by booleanPreference(KEY_COMMENTS_NAVIGATION_FAB, false)
    var useVolumeButtonNavigation: Boolean
        by booleanPreference(KEY_USE_VOLUME_BUTTON_NAVIGATION, false)
    var collapseChildCommentsByDefault: Boolean
        by booleanPreference(KEY_COLLAPSE_CHILD_COMMENTS_BY_DEFAULT, false)

    var commentsNavigationFabOffX: Int
        by intPreference(KEY_COMMENTS_NAVIGATION_FAB_OFF_X, 0)

    var commentsNavigationFabOffY: Int
        by intPreference(KEY_COMMENTS_NAVIGATION_FAB_OFF_Y, -Utils.convertDpToPixel(24f).toInt())

    var hidePostScores: Boolean
        by booleanPreference(KEY_HIDE_POST_SCORES, false)

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
        by intPreference(KEY_GLOBAL_FONT, FontIds.DEFAULT)

    var upvoteColor: Int
        by intPreference(KEY_UPVOTE_COLOR, context.getColorCompat(R.color.upvoteColor))
    var downvoteColor: Int
        by intPreference(KEY_DOWNVOTE_COLOR, context.getColorCompat(R.color.downvoteColor))

    var openLinksInExternalApp: Boolean
        by booleanPreference(KEY_OPEN_LINKS_IN_APP, false)
    var autoLinkPhoneNumbers: Boolean
        by booleanPreference(KEY_AUTO_LINK_PHONE_NUMBERS, false)
    var autoLinkIpAddresses: Boolean
        by booleanPreference(KEY_AUTO_LINK_IP_ADDRESSES, true)
    var postShowUpAndDownVotes: Boolean
        by booleanPreference(KEY_POST_SHOW_UP_AND_DOWN_VOTES, false)
    var commentShowUpAndDownVotes: Boolean
        by booleanPreference(KEY_COMMENT_SHOW_UP_AND_DOWN_VOTES, false)

    var displayInstanceStyle: Int
        by intPreference(
            KEY_DISPLAY_INSTANCE_STYLE,
            DisplayInstanceOptions.OnlyDisplayNonLocalInstances,
        )

    var retainLastPost: Boolean
        by booleanPreference(KEY_RETAIN_LAST_POST, true)

    var leftHandMode: Boolean
        by booleanPreference(KEY_LEFT_HAND_MODE, false)

    var transparentNotificationBar: Boolean
        by booleanPreference(KEY_TRANSPARENT_NOTIFICATION_BAR, false)

    var lockBottomBar: Boolean
        by booleanPreference(KEY_LOCK_BOTTOM_BAR, false)

    var appVersionLastLaunch: Int
        by intPreference(KEY_PREF_VERSION, 0)

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
        by intPreference(KEY_SCREENSHOT_WIDTH_DP, 500)

    var dateScreenshots: Boolean
        by booleanPreference(KEY_DATE_SCREENSHOTS, true)

    var screenshotWatermark: Int
        by intPreference(KEY_SCREENSHOT_WATERMARK, ScreenshotWatermarkId.LEMMY)

    var useFirebase: Boolean
        by booleanPreference(KEY_USE_FIREBASE, true)

    var autoCollapseCommentThreshold: Float
        by floatPreference(KEY_AUTO_COLLAPSE_COMMENT_THRESHOLD, 0.3f)

    var trackBrowsingHistory: Boolean
        by booleanPreference(KEY_TRACK_BROWSING_HISTORY, true)

    var useCustomNavBar: Boolean
        by booleanPreference(KEY_USE_CUSTOM_NAV_BAR, false)

    var navBarConfig: NavBarConfig
        by jsonPreference(KEY_NAV_BAR_ITEMS) { NavBarConfig() }

    var useBottomNavBar: Boolean
        by booleanPreference(KEY_USE_BOTTOM_NAV_BAR, true)

    var isHiddenPostsEnabled: Boolean
        by booleanPreference(KEY_ENABLE_HIDDEN_POSTS, true)

    var usePredictiveBack: Boolean
        by booleanPreference(KEY_USE_PREDICTIVE_BACK, false)

    var autoLoadMorePosts: Boolean
        by booleanPreference(KEY_AUTO_LOAD_MORE_POSTS, true)

    var infinityPageIndicator: Boolean
        by booleanPreference(KEY_INFINITY_PAGE_INDICATOR, false)

    var warnReplyToOldContent: Boolean
        by booleanPreference(KEY_WARN_REPLY_TO_OLD_CONTENT, true)

    var warnReplyToOldContentThresholdMs: Long
        by longPreference(
            KEY_WARN_REPLY_TO_OLD_CONTENT_THRESHOLD_MS,
            1000 * 60 * 60 * 24 * 2,
        )

    var showPostUpvotePercentage: Boolean
        by booleanPreference(KEY_SHOW_POST_UPVOTE_PERCENTAGE, false)

    var showCommentUpvotePercentage: Boolean
        by booleanPreference(KEY_SHOW_COMMENT_UPVOTE_PERCENTAGE, false)

    var useMultilinePostHeaders: Boolean
        by booleanPreference(KEY_USE_MULTILINE_POST_HEADERS, true)

    var indicatePostsAndCommentsCreatedByCurrentUser: Boolean
        by booleanPreference(KEY_INDICATE_CONTENT_FROM_CURRENT_USER, true)

    var saveDraftsAutomatically: Boolean
        by booleanPreference(KEY_SAVE_DRAFTS_AUTOMATICALLY, true)

    var showProfileIcons: Boolean
        by booleanPreference(KEY_SHOW_PROFILE_ICONS, true)

    var commentHeaderLayout: Int
        by intPreference(KEY_COMMENT_HEADER_LAYOUT, CommentHeaderLayoutId.SingleLine)

    var navigationRailMode: Int
        by intPreference(KEY_NAVIGATION_RAIL_MODE, NavigationRailModeId.ManualOff)

    var downloadDirectory: String?
        by stringPreference(KEY_DOWNLOAD_DIRECTORY, null)

    var usePerCommunitySettings: Boolean
        by booleanPreference(KEY_USE_PER_COMMUNITY_SETTINGS, true)

    var guestAccountSettings: GuestAccountSettings?
        by jsonPreference(KEY_GUEST_ACCOUNT_SETTINGS) { null }
    var textFieldToolbarSettings: TextFieldToolbarSettings?
        by jsonPreference(KEY_TEXT_FIELD_TOOLBAR_SETTINGS) { null }
    var postQuickActions: PostQuickActionsSettings?
        by jsonPreference(KEY_POST_QUICK_ACTIONS) { null }
    var commentQuickActions: CommentQuickActionsSettings?
        by jsonPreference(KEY_COMMENT_QUICK_ACTIONS) { null }

    var globalLayoutMode: GlobalLayoutMode
        by intPreference(KEY_GLOBAL_LAYOUT_MODE, GlobalLayoutModes.Auto)

    var rotateInstanceOnUploadFail: Boolean
        by booleanPreference(KEY_ROTATE_INSTANCE_ON_UPLOAD_FAIL, false)

    var showFilteredPosts: Boolean
        by booleanPreference(KEY_SHOW_FILTERED_POSTS, false)

    var commentsShowInlineMediaAsLinks: Boolean
        by booleanPreference(KEY_COMMENTS_SHOW_INLINE_MEDIA_AS_LINKS, false)

    var isNotificationsOn: Boolean
        by booleanPreference(KEY_IS_NOTIFICATIONS_ON, false)

    var lastAccountNotificationId: Int
        by intPreference(KEY_LAST_ACCOUNT_NOTIFICATION_ID, 0)

    var notificationsCheckIntervalMs: Long
        by longPreference(KEY_NOTIFICATIONS_CHECK_INTERVAL_MS, 1000L * 60L * 15L)

    var homeFabQuickAction: Int
        by intPreference(KEY_HOME_FAB_QUICK_ACTION, HomeFabQuickActionIds.None)

    var showEditedDate: Boolean
        by booleanPreference(KEY_SHOW_EDITED_DATE, true)

    var imagePreviewHideUiByDefault: Boolean
        by booleanPreference(KEY_IMAGE_PREVIEW_HIDE_UI_BY_DEFAULT, false)

    var prefetchPosts: Boolean
        by booleanPreference(KEY_PREFETCH_POSTS, true)

    var autoPlayVideos: Boolean
        by booleanPreference(KEY_AUTO_PLAY_VIDEOS, true)

    var uploadImagesToImgur: Boolean
        by booleanPreference(KEY_UPLOAD_IMAGES_TO_IMGUR, false)

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
        by booleanPreference(KEY_USE_CONDENSED_FOR_COMMENT_HEADERS, true)

    var parseMarkdownInPostTitles: Boolean
        by booleanPreference(KEY_PARSE_MARKDOWN_IN_POST_TITLES, true)

    var searchHomeConfig: SearchHomeConfig
        by jsonPreference(KEY_SEARCH_HOME_CONFIG) { SearchHomeConfig() }

    var postFeedShowScrollBar: Boolean
        by booleanPreference(KEY_POST_FEED_SHOW_SCROLL_BAR, true)

    var hapticsEnabled: Boolean
        by booleanPreference(KEY_HAPTICS_ENABLED, true)

    var hapticsOnActions: Boolean
        get() = hapticsEnabled && prefs.getBoolean(KEY_HAPTICS_ON_ACTIONS, true)
        set(value) {
            prefs.edit()
                .putBoolean(KEY_HAPTICS_ON_ACTIONS, value)
                .apply()
        }

    var hideDuplicatePostsOnRead: Boolean
        by booleanPreference(KEY_HIDE_DUPLICATE_POSTS_ON_READ, false)
    var usePostsFeedHeader: Boolean
        by booleanPreference(KEY_USE_POSTS_FEED_HEADER, false)

    var inlineVideoDefaultVolume: Float
        by floatPreference(KEY_INLINE_VIDEO_DEFAULT_VOLUME, 0f)

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

        json.put(PreferenceUtils.PREFERENCE_VERSION_CODE, BuildConfig.VERSION_CODE)

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

    @Suppress("NOTHING_TO_INLINE")
    private inline fun stringPreference(key: String, defaultValue: String? = "") =
        StringPreferenceDelegate(prefs, key, defaultValue)

    @Suppress("NOTHING_TO_INLINE")
    private inline fun floatPreference(key: String, defaultValue: Float = 0.0f) =
        FloatPreferenceDelegate(prefs, key, defaultValue)

    @Suppress("NOTHING_TO_INLINE")
    private inline fun booleanPreference(key: String, defaultValue: Boolean = false) =
        BooleanPreferenceDelegate(prefs, key, defaultValue)

    @Suppress("NOTHING_TO_INLINE")
    private inline fun intPreference(key: String, defaultValue: Int = 0) =
        IntPreferenceDelegate(prefs, key, defaultValue)

    @Suppress("NOTHING_TO_INLINE")
    private inline fun nullableIntPreference(key: String) =
        NullableIntPreferenceDelegate(prefs, key)

    @Suppress("NOTHING_TO_INLINE")
    private inline fun longPreference(key: String, defaultValue: Long = 0) =
        LongPreferenceDelegate(prefs, key)

    private inline fun <reified T> jsonPreference(key: String, noinline defaultValue: () -> T) =
        JsonPreferenceDelegate(prefs, json, key, serializer(), defaultValue)
}
