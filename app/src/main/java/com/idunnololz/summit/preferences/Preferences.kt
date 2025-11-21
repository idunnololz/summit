package com.idunnololz.summit.preferences

import android.content.Context
import android.content.SharedPreferences
import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import android.util.Base64
import androidx.core.content.edit
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
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
import com.idunnololz.summit.preferences.DisplayDeletedPostIds.ALWAYS_HIDE_DELETED_POSTS
import com.idunnololz.summit.preferences.PreferenceKeys.KEY_ALWAYS_SHOW_LINK_BUTTON_BELOW_POST
import com.idunnololz.summit.preferences.PreferenceKeys.KEY_ANIMATION_LEVEL
import com.idunnololz.summit.preferences.PreferenceKeys.KEY_AUTO_COLLAPSE_COMMENT_THRESHOLD
import com.idunnololz.summit.preferences.PreferenceKeys.KEY_AUTO_FOCUS_SEARCH_BAR
import com.idunnololz.summit.preferences.PreferenceKeys.KEY_AUTO_HIDE_UI_ON_PLAY
import com.idunnololz.summit.preferences.PreferenceKeys.KEY_AUTO_LINK_IP_ADDRESSES
import com.idunnololz.summit.preferences.PreferenceKeys.KEY_AUTO_LINK_PHONE_NUMBERS
import com.idunnololz.summit.preferences.PreferenceKeys.KEY_AUTO_LOAD_MORE_POSTS
import com.idunnololz.summit.preferences.PreferenceKeys.KEY_AUTO_PLAY_VIDEOS
import com.idunnololz.summit.preferences.PreferenceKeys.KEY_BASE_THEME
import com.idunnololz.summit.preferences.PreferenceKeys.KEY_BLUR_NSFW_POSTS
import com.idunnololz.summit.preferences.PreferenceKeys.KEY_CACHE_POLICY
import com.idunnololz.summit.preferences.PreferenceKeys.KEY_COLLAPSE_CHILD_COMMENTS_BY_DEFAULT
import com.idunnololz.summit.preferences.PreferenceKeys.KEY_COLOR_SCHEME
import com.idunnololz.summit.preferences.PreferenceKeys.KEY_COMMENTS_NAVIGATION_FAB
import com.idunnololz.summit.preferences.PreferenceKeys.KEY_COMMENTS_NAVIGATION_FAB_OFF_X
import com.idunnololz.summit.preferences.PreferenceKeys.KEY_COMMENTS_NAVIGATION_FAB_OFF_Y
import com.idunnololz.summit.preferences.PreferenceKeys.KEY_COMMENTS_SHOW_INLINE_MEDIA_AS_LINKS
import com.idunnololz.summit.preferences.PreferenceKeys.KEY_COMMENT_GESTURE_ACTION_1
import com.idunnololz.summit.preferences.PreferenceKeys.KEY_COMMENT_GESTURE_ACTION_2
import com.idunnololz.summit.preferences.PreferenceKeys.KEY_COMMENT_GESTURE_ACTION_3
import com.idunnololz.summit.preferences.PreferenceKeys.KEY_COMMENT_GESTURE_ACTION_COLOR_1
import com.idunnololz.summit.preferences.PreferenceKeys.KEY_COMMENT_GESTURE_ACTION_COLOR_2
import com.idunnololz.summit.preferences.PreferenceKeys.KEY_COMMENT_GESTURE_ACTION_COLOR_3
import com.idunnololz.summit.preferences.PreferenceKeys.KEY_COMMENT_GESTURE_SIZE
import com.idunnololz.summit.preferences.PreferenceKeys.KEY_COMMENT_HEADER_LAYOUT
import com.idunnololz.summit.preferences.PreferenceKeys.KEY_COMMENT_QUICK_ACTIONS
import com.idunnololz.summit.preferences.PreferenceKeys.KEY_COMMENT_SHOW_UP_AND_DOWN_VOTES
import com.idunnololz.summit.preferences.PreferenceKeys.KEY_COMMENT_THREAD_STYLE
import com.idunnololz.summit.preferences.PreferenceKeys.KEY_COMMUNITY_SELECTOR_SHOW_COMMUNITY_SUGGESTIONS
import com.idunnololz.summit.preferences.PreferenceKeys.KEY_DATE_SCREENSHOTS
import com.idunnololz.summit.preferences.PreferenceKeys.KEY_DEFAULT_APP_WEB_BROWSER
import com.idunnololz.summit.preferences.PreferenceKeys.KEY_DEFAULT_COMMENTS_SORT_ORDER
import com.idunnololz.summit.preferences.PreferenceKeys.KEY_DEFAULT_COMMUNITY_SORT_ORDER
import com.idunnololz.summit.preferences.PreferenceKeys.KEY_DEFAULT_PAGE
import com.idunnololz.summit.preferences.PreferenceKeys.KEY_DISPLAY_DELETED_POSTS
import com.idunnololz.summit.preferences.PreferenceKeys.KEY_DISPLAY_INSTANCE_STYLE
import com.idunnololz.summit.preferences.PreferenceKeys.KEY_DOWNLOAD_DIRECTORY
import com.idunnololz.summit.preferences.PreferenceKeys.KEY_DOWNVOTE_COLOR
import com.idunnololz.summit.preferences.PreferenceKeys.KEY_DO_NOT_BLUR_NSFW_CONTENT_IN_NSFW_COMMUNITY_FEED
import com.idunnololz.summit.preferences.PreferenceKeys.KEY_ENABLE_HIDDEN_POSTS
import com.idunnololz.summit.preferences.PreferenceKeys.KEY_FINISH_APP_ON_BACK
import com.idunnololz.summit.preferences.PreferenceKeys.KEY_GESTURE_SWIPE_DIRECTION
import com.idunnololz.summit.preferences.PreferenceKeys.KEY_GLOBAL_FONT
import com.idunnololz.summit.preferences.PreferenceKeys.KEY_GLOBAL_FONT_COLOR
import com.idunnololz.summit.preferences.PreferenceKeys.KEY_GLOBAL_FONT_SIZE
import com.idunnololz.summit.preferences.PreferenceKeys.KEY_GLOBAL_LAYOUT_MODE
import com.idunnololz.summit.preferences.PreferenceKeys.KEY_GUEST_ACCOUNT_SETTINGS
import com.idunnololz.summit.preferences.PreferenceKeys.KEY_HAPTICS_ENABLED
import com.idunnololz.summit.preferences.PreferenceKeys.KEY_HAPTICS_ON_ACTIONS
import com.idunnololz.summit.preferences.PreferenceKeys.KEY_HIDE_COMMENT_ACTIONS
import com.idunnololz.summit.preferences.PreferenceKeys.KEY_HIDE_COMMENT_SCORES
import com.idunnololz.summit.preferences.PreferenceKeys.KEY_HIDE_DUPLICATE_POSTS_ON_READ
import com.idunnololz.summit.preferences.PreferenceKeys.KEY_HIDE_HEADER_BANNER_IF_NO_BANNER
import com.idunnololz.summit.preferences.PreferenceKeys.KEY_HIDE_POST_SCORES
import com.idunnololz.summit.preferences.PreferenceKeys.KEY_HIDE_READ_BY_DEFAULT
import com.idunnololz.summit.preferences.PreferenceKeys.KEY_HIGHLIGHT_NEW_COMMENTS
import com.idunnololz.summit.preferences.PreferenceKeys.KEY_HOME_FAB_QUICK_ACTION
import com.idunnololz.summit.preferences.PreferenceKeys.KEY_IMAGE_PREVIEW_HIDE_UI_BY_DEFAULT
import com.idunnololz.summit.preferences.PreferenceKeys.KEY_INBOX_AUTO_MARK_AS_READ
import com.idunnololz.summit.preferences.PreferenceKeys.KEY_INBOX_FAB_ACTION
import com.idunnololz.summit.preferences.PreferenceKeys.KEY_INBOX_LAYOUT
import com.idunnololz.summit.preferences.PreferenceKeys.KEY_INDICATE_CONTENT_FROM_CURRENT_USER
import com.idunnololz.summit.preferences.PreferenceKeys.KEY_INFINITY
import com.idunnololz.summit.preferences.PreferenceKeys.KEY_INFINITY_PAGE_INDICATOR
import com.idunnololz.summit.preferences.PreferenceKeys.KEY_INLINE_URLS_IN_PRIVATE_MESSAGES
import com.idunnololz.summit.preferences.PreferenceKeys.KEY_INLINE_VIDEO_DEFAULT_VOLUME
import com.idunnololz.summit.preferences.PreferenceKeys.KEY_IS_NOTIFICATIONS_ON
import com.idunnololz.summit.preferences.PreferenceKeys.KEY_LAST_ACCOUNT_NOTIFICATION_ID
import com.idunnololz.summit.preferences.PreferenceKeys.KEY_LEFT_HAND_MODE
import com.idunnololz.summit.preferences.PreferenceKeys.KEY_LOCK_BOTTOM_BAR
import com.idunnololz.summit.preferences.PreferenceKeys.KEY_LOOP_VIDEO_BY_DEFAULT
import com.idunnololz.summit.preferences.PreferenceKeys.KEY_MARK_AS_READ_ON_HIDE_POST
import com.idunnololz.summit.preferences.PreferenceKeys.KEY_MARK_POSTS_AS_READ_ON_SCROLL
import com.idunnololz.summit.preferences.PreferenceKeys.KEY_NAVIGATION_RAIL_MODE
import com.idunnololz.summit.preferences.PreferenceKeys.KEY_NAV_BAR_ITEMS
import com.idunnololz.summit.preferences.PreferenceKeys.KEY_NAV_RAIL_GRAVITY
import com.idunnololz.summit.preferences.PreferenceKeys.KEY_NOTIFICATIONS_CHECK_INTERVAL_MS
import com.idunnololz.summit.preferences.PreferenceKeys.KEY_OPEN_LINKS_IN_APP
import com.idunnololz.summit.preferences.PreferenceKeys.KEY_OPEN_LINK_WHEN_THUMBNAIL_TAPPED
import com.idunnololz.summit.preferences.PreferenceKeys.KEY_OP_TAG_STYLE
import com.idunnololz.summit.preferences.PreferenceKeys.KEY_PARSE_MARKDOWN_IN_POST_TITLES
import com.idunnololz.summit.preferences.PreferenceKeys.KEY_PERF_DELAY_WHEN_LOADING_DATA
import com.idunnololz.summit.preferences.PreferenceKeys.KEY_POSTS_IN_FEED_QUICK_ACTIONS
import com.idunnololz.summit.preferences.PreferenceKeys.KEY_POST_AND_COMMENTS_UI_CONFIG
import com.idunnololz.summit.preferences.PreferenceKeys.KEY_POST_FAB_QUICK_ACTION
import com.idunnololz.summit.preferences.PreferenceKeys.KEY_POST_FEED_SHOW_SCROLL_BAR
import com.idunnololz.summit.preferences.PreferenceKeys.KEY_POST_FULL_BLEED_IMAGE
import com.idunnololz.summit.preferences.PreferenceKeys.KEY_POST_GESTURE_ACTION_1
import com.idunnololz.summit.preferences.PreferenceKeys.KEY_POST_GESTURE_ACTION_2
import com.idunnololz.summit.preferences.PreferenceKeys.KEY_POST_GESTURE_ACTION_3
import com.idunnololz.summit.preferences.PreferenceKeys.KEY_POST_GESTURE_ACTION_COLOR_1
import com.idunnololz.summit.preferences.PreferenceKeys.KEY_POST_GESTURE_ACTION_COLOR_2
import com.idunnololz.summit.preferences.PreferenceKeys.KEY_POST_GESTURE_ACTION_COLOR_3
import com.idunnololz.summit.preferences.PreferenceKeys.KEY_POST_GESTURE_SIZE
import com.idunnololz.summit.preferences.PreferenceKeys.KEY_POST_LIST_VIEW_IMAGE_ON_SINGLE_TAP
import com.idunnololz.summit.preferences.PreferenceKeys.KEY_POST_QUICK_ACTIONS
import com.idunnololz.summit.preferences.PreferenceKeys.KEY_POST_SHOW_UP_AND_DOWN_VOTES
import com.idunnololz.summit.preferences.PreferenceKeys.KEY_PREFERRED_LOCALE
import com.idunnololz.summit.preferences.PreferenceKeys.KEY_PREFER_COMMUNITY_DISPLAY_NAME
import com.idunnololz.summit.preferences.PreferenceKeys.KEY_PREFER_USER_DISPLAY_NAME
import com.idunnololz.summit.preferences.PreferenceKeys.KEY_PREFETCH_POSTS
import com.idunnololz.summit.preferences.PreferenceKeys.KEY_PREF_VERSION
import com.idunnololz.summit.preferences.PreferenceKeys.KEY_PREVIEW_LINKS
import com.idunnololz.summit.preferences.PreferenceKeys.KEY_RESTORE_BROWSING_SESSIONS
import com.idunnololz.summit.preferences.PreferenceKeys.KEY_RETAIN_LAST_POST
import com.idunnololz.summit.preferences.PreferenceKeys.KEY_ROTATE_INSTANCE_ON_UPLOAD_FAIL
import com.idunnololz.summit.preferences.PreferenceKeys.KEY_SAVE_DRAFTS_AUTOMATICALLY
import com.idunnololz.summit.preferences.PreferenceKeys.KEY_SCREENSHOT_WATERMARK
import com.idunnololz.summit.preferences.PreferenceKeys.KEY_SCREENSHOT_WIDTH_DP
import com.idunnololz.summit.preferences.PreferenceKeys.KEY_SEARCH_HOME_CONFIG
import com.idunnololz.summit.preferences.PreferenceKeys.KEY_SHAKE_TO_SEND_FEEDBACK
import com.idunnololz.summit.preferences.PreferenceKeys.KEY_SHOW_COMMENT_UPVOTE_PERCENTAGE
import com.idunnololz.summit.preferences.PreferenceKeys.KEY_SHOW_CROSS_POSTS_IN_POST
import com.idunnololz.summit.preferences.PreferenceKeys.KEY_SHOW_DEFAULT_PROFILE_ICONS
import com.idunnololz.summit.preferences.PreferenceKeys.KEY_SHOW_EDITED_DATE
import com.idunnololz.summit.preferences.PreferenceKeys.KEY_SHOW_FILTERED_POSTS
import com.idunnololz.summit.preferences.PreferenceKeys.KEY_SHOW_IMAGE_POSTS
import com.idunnololz.summit.preferences.PreferenceKeys.KEY_SHOW_LABELS_IN_NAV_BAR
import com.idunnololz.summit.preferences.PreferenceKeys.KEY_SHOW_LINK_POSTS
import com.idunnololz.summit.preferences.PreferenceKeys.KEY_SHOW_NAVIGATION_BAR_ON_POST
import com.idunnololz.summit.preferences.PreferenceKeys.KEY_SHOW_NSFW_POSTS
import com.idunnololz.summit.preferences.PreferenceKeys.KEY_SHOW_POST_TYPE
import com.idunnololz.summit.preferences.PreferenceKeys.KEY_SHOW_POST_UPVOTE_PERCENTAGE
import com.idunnololz.summit.preferences.PreferenceKeys.KEY_SHOW_PROFILE_ICONS
import com.idunnololz.summit.preferences.PreferenceKeys.KEY_SHOW_TEXT_POSTS
import com.idunnololz.summit.preferences.PreferenceKeys.KEY_SHOW_VIDEO_POSTS
import com.idunnololz.summit.preferences.PreferenceKeys.KEY_STRING_FOR_NULL_SCORE
import com.idunnololz.summit.preferences.PreferenceKeys.KEY_SWIPE_BETWEEN_POSTS
import com.idunnololz.summit.preferences.PreferenceKeys.KEY_TAP_ANYWHERE_TO_PLAY_PAUSE
import com.idunnololz.summit.preferences.PreferenceKeys.KEY_TAP_COMMENT_TO_COLLAPSE
import com.idunnololz.summit.preferences.PreferenceKeys.KEY_TEXT_FIELD_TOOLBAR_SETTINGS
import com.idunnololz.summit.preferences.PreferenceKeys.KEY_TRACK_BROWSING_HISTORY
import com.idunnololz.summit.preferences.PreferenceKeys.KEY_TRANSPARENT_NOTIFICATION_BAR
import com.idunnololz.summit.preferences.PreferenceKeys.KEY_UPLOAD_IMAGES_TO_IMGUR
import com.idunnololz.summit.preferences.PreferenceKeys.KEY_UPVOTE_COLOR
import com.idunnololz.summit.preferences.PreferenceKeys.KEY_USER_AGENT_CHOICE
import com.idunnololz.summit.preferences.PreferenceKeys.KEY_USE_BLACK_THEME
import com.idunnololz.summit.preferences.PreferenceKeys.KEY_USE_BOTTOM_NAV_BAR
import com.idunnololz.summit.preferences.PreferenceKeys.KEY_USE_CONDENSED_FOR_COMMENT_HEADERS
import com.idunnololz.summit.preferences.PreferenceKeys.KEY_USE_CUSTOM_NAV_BAR
import com.idunnololz.summit.preferences.PreferenceKeys.KEY_USE_FIREBASE
import com.idunnololz.summit.preferences.PreferenceKeys.KEY_USE_GESTURE_ACTIONS
import com.idunnololz.summit.preferences.PreferenceKeys.KEY_USE_LESS_DARK_BACKGROUND
import com.idunnololz.summit.preferences.PreferenceKeys.KEY_USE_MATERIAL_YOU
import com.idunnololz.summit.preferences.PreferenceKeys.KEY_USE_MULTILINE_POST_HEADERS
import com.idunnololz.summit.preferences.PreferenceKeys.KEY_USE_PER_COMMUNITY_SETTINGS
import com.idunnololz.summit.preferences.PreferenceKeys.KEY_USE_POSTS_FEED_HEADER
import com.idunnololz.summit.preferences.PreferenceKeys.KEY_USE_PREDICTIVE_BACK
import com.idunnololz.summit.preferences.PreferenceKeys.KEY_USE_VOLUME_BUTTON_NAVIGATION
import com.idunnololz.summit.preferences.PreferenceKeys.KEY_VIDEO_PLAYER_ROTATION_LOCKED
import com.idunnololz.summit.preferences.PreferenceKeys.KEY_WARN_NEW_PERSON
import com.idunnololz.summit.preferences.PreferenceKeys.KEY_WARN_REPLY_TO_OLD_CONTENT
import com.idunnololz.summit.preferences.PreferenceKeys.KEY_WARN_REPLY_TO_OLD_CONTENT_THRESHOLD_MS
import com.idunnololz.summit.preferences.PreferenceKeys.KEY_WRAP_COMMENT_HEADER
import com.idunnololz.summit.settings.misc.DisplayInstanceOptions
import com.idunnololz.summit.settings.navigation.NavBarConfig
import com.idunnololz.summit.util.AnimationsHelper
import com.idunnololz.summit.util.Utils
import com.idunnololz.summit.util.ext.asJson
import com.idunnololz.summit.util.ext.base
import com.idunnololz.summit.util.ext.getColorCompat
import com.idunnololz.summit.util.ext.getJsonValue
import com.idunnololz.summit.util.ext.putJsonValue
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import org.json.JSONObject

private val Context.offlineModeDataStore: DataStore<Preferences> by preferencesDataStore(
  name = "offlineModePreferences",
)

class Preferences(
  @ApplicationContext private val context: Context,
  private var sharedPreferencesManager: SharedPreferencesManager,
  override var sharedPreferences: SharedPreferences,
  private val coroutineScopeFactory: CoroutineScopeFactory,
  override val json: Json,
) : SharedPreferencesPreferences {

  companion object {
    private const val TAG = "Preferences"
  }

  private val coroutineScope = coroutineScopeFactory.create()

  @Suppress("ObjectLiteralToLambda")
  private val preferenceChangeListener = object : OnSharedPreferenceChangeListener {
    override fun onSharedPreferenceChanged(p0: SharedPreferences?, p1: String?) {
      coroutineScope.launch {
        onPreferenceChangeFlow.emit(Unit)
      }
    }
  }

  val onPreferenceChangeFlow = MutableSharedFlow<Unit>()

  init {
    sharedPreferences.registerOnSharedPreferenceChangeListener(preferenceChangeListener)
  }

  val all: MutableMap<String, *>
    get() = sharedPreferences.all

  var defaultPage: CommunityRef
    by jsonPreference(KEY_DEFAULT_PAGE) { CommunityRef.All() }

  fun updateWhichSharedPreference() {
    sharedPreferences = sharedPreferencesManager.currentSharedPreferences
  }

  fun getPostsLayout(): CommunityLayout = try {
    CommunityLayout.valueOf(
      sharedPreferences
        .getString(PreferenceKeys.KEY_COMMUNITY_LAYOUT, null) ?: "",
    )
  } catch (e: IllegalArgumentException) {
    CommunityLayout.List
  }

  fun setPostsLayout(layout: CommunityLayout) {
    sharedPreferences.base.edit {
      putString(PreferenceKeys.KEY_COMMUNITY_LAYOUT, layout.name)
    }
  }

  fun getPostInListUiConfig(): PostInListUiConfig = getPostInListUiConfig(getPostsLayout())

  fun setPostInListUiConfig(config: PostInListUiConfig) {
    sharedPreferences.putJsonValue(json, getPostUiConfigKey(getPostsLayout()), config)
  }

  fun getPostInListUiConfig(layout: CommunityLayout): PostInListUiConfig =
    sharedPreferences.getJsonValue<PostInListUiConfig>(json, getPostUiConfigKey(layout))
      ?: layout.getDefaultPostUiConfig()

  var postAndCommentsUiConfig: PostAndCommentsUiConfig
    by jsonPreference(KEY_POST_AND_COMMENTS_UI_CONFIG) { getDefaultPostAndCommentsUiConfig() }

  private fun getPostUiConfigKey(layout: CommunityLayout) = when (layout) {
    CommunityLayout.Compact ->
      PreferenceKeys.KEY_POST_UI_CONFIG_COMPACT
    CommunityLayout.List ->
      PreferenceKeys.KEY_POST_UI_CONFIG_LIST
    CommunityLayout.LargeList ->
      PreferenceKeys.KEY_POST_UI_CONFIG_LARGE_LIST
    CommunityLayout.Card ->
      PreferenceKeys.KEY_POST_UI_CONFIG_CARD
    CommunityLayout.Card2 ->
      PreferenceKeys.KEY_POST_UI_CONFIG_CARD2
    CommunityLayout.Card3 ->
      PreferenceKeys.KEY_POST_UI_CONFIG_CARD3
    CommunityLayout.Full ->
      PreferenceKeys.KEY_POST_UI_CONFIG_FULL
    CommunityLayout.ListWithCards ->
      PreferenceKeys.KEY_POST_UI_CONFIG_LIST_WITH_CARDS
    CommunityLayout.FullWithCards ->
      PreferenceKeys.KEY_POST_UI_CONFIG_FULL_WITH_CARDS
    CommunityLayout.SmartList ->
      PreferenceKeys.KEY_POST_UI_CONFIG_SMART_LIST
  }

  var isUseMaterialYou: Boolean
    by booleanPreference(KEY_USE_MATERIAL_YOU, true)

  var isBlackTheme: Boolean
    by booleanPreference(KEY_USE_BLACK_THEME, false)

  var isVideoPlayerRotationLocked: Boolean
    by booleanPreference(KEY_VIDEO_PLAYER_ROTATION_LOCKED, false)

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
    by booleanPreference(KEY_HIDE_COMMENT_SCORES, false)

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
    by intPreference(KEY_PREVIEW_LINKS, PreviewTextLinks)

  var screenshotWidthDp: Int
    by intPreference(KEY_SCREENSHOT_WIDTH_DP, 400)

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
  var showDefaultProfileIcons: Boolean
    by booleanPreference(KEY_SHOW_DEFAULT_PROFILE_ICONS, true)

  var commentHeaderLayout: Int
    by intPreference(KEY_COMMENT_HEADER_LAYOUT, CommentHeaderLayoutId.SingleLine)

  var wrapCommentHeader: Boolean
    by booleanPreference(KEY_WRAP_COMMENT_HEADER, true)

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
  var postsInFeedQuickActions: PostsInFeedQuickActionsSettings?
    by jsonPreference(KEY_POSTS_IN_FEED_QUICK_ACTIONS) { null }

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
    by intPreference(KEY_HOME_FAB_QUICK_ACTION, HomeFabQuickActionIds.HideRead)

  var showEditedDate: Boolean
    by booleanPreference(KEY_SHOW_EDITED_DATE, true)

  var imagePreviewHideUiByDefault: Boolean
    by booleanPreference(KEY_IMAGE_PREVIEW_HIDE_UI_BY_DEFAULT, false)

  var prefetchPosts: Boolean
    by booleanPreference(KEY_PREFETCH_POSTS, true)

  var autoPlayVideos: Boolean
    by booleanPreference(KEY_AUTO_PLAY_VIDEOS, true)
  var autoHideUiOnPlay: Boolean
    by booleanPreference(KEY_AUTO_HIDE_UI_ON_PLAY, true)
  var tapAnywhereToPlayPause: Boolean
    by booleanPreference(KEY_TAP_ANYWHERE_TO_PLAY_PAUSE, false)

  var uploadImagesToImgur: Boolean
    by booleanPreference(KEY_UPLOAD_IMAGES_TO_IMGUR, false)
  var displayDeletedPosts: Int
    by intPreference(KEY_DISPLAY_DELETED_POSTS, ALWAYS_HIDE_DELETED_POSTS)

  var animationLevel: AnimationsHelper.AnimationLevel
    get() = AnimationsHelper.AnimationLevel.parse(
      sharedPreferences.getInt(
        KEY_ANIMATION_LEVEL,
        AnimationsHelper.AnimationLevel.Max.animationLevel,
      ),
    )
    set(value) {
      sharedPreferences.edit()
        .putInt(KEY_ANIMATION_LEVEL, value.animationLevel)
        .apply()
    }

  var cachePolicy: CachePolicy
    get() = CachePolicy.parse(
      sharedPreferences.getInt(KEY_CACHE_POLICY, CachePolicy.Moderate.value),
    )
    set(value) {
      sharedPreferences.edit()
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
    get() = hapticsEnabled && sharedPreferences.getBoolean(KEY_HAPTICS_ON_ACTIONS, true)
    set(value) {
      sharedPreferences.edit {
        putBoolean(KEY_HAPTICS_ON_ACTIONS, value)
      }
    }

  var hideDuplicatePostsOnRead: Boolean
    by booleanPreference(KEY_HIDE_DUPLICATE_POSTS_ON_READ, false)
  var usePostsFeedHeader: Boolean
    by booleanPreference(KEY_USE_POSTS_FEED_HEADER, false)
  var inlineVideoDefaultVolume: Float
    by floatPreference(KEY_INLINE_VIDEO_DEFAULT_VOLUME, 0f)
  var swipeBetweenPosts: Boolean
    by booleanPreference(KEY_SWIPE_BETWEEN_POSTS, false)
  var postFabQuickAction: Int
    by intPreference(KEY_POST_FAB_QUICK_ACTION, PostFabQuickActions.NONE)
  var shakeToSendFeedback: Boolean
    by booleanPreference(KEY_SHAKE_TO_SEND_FEEDBACK, false)
  var showLabelsInNavBar: Boolean
    by booleanPreference(KEY_SHOW_LABELS_IN_NAV_BAR, true)
  var warnNewPerson: Boolean
    by booleanPreference(KEY_WARN_NEW_PERSON, true)
  var gestureSwipeDirection: Int
    by intPreference(KEY_GESTURE_SWIPE_DIRECTION, GestureSwipeDirectionIds.LEFT)
  var defaultWebApp: DefaultAppPreference?
    by jsonPreference(KEY_DEFAULT_APP_WEB_BROWSER) { null }
  var preferredLocale: String?
    by stringPreference(KEY_PREFERRED_LOCALE)
  var communitySelectorShowCommunitySuggestions: Boolean
    by booleanPreference(KEY_COMMUNITY_SELECTOR_SHOW_COMMUNITY_SUGGESTIONS, true)
  var postFullBleedImage: Boolean
    by booleanPreference(KEY_POST_FULL_BLEED_IMAGE, true)
  var userAgentChoice: Int
    by intPreference(KEY_USER_AGENT_CHOICE, UserAgentChoiceIds.UNSET)
  var openLinkWhenThumbnailTapped: Boolean
    by booleanPreference(KEY_OPEN_LINK_WHEN_THUMBNAIL_TAPPED, false)
  var showPostType: Boolean
    by booleanPreference(KEY_SHOW_POST_TYPE, false)
  var navRailGravity: Int
    by intPreference(KEY_NAV_RAIL_GRAVITY, NavRailGravityIds.TOP)
  var inlineUrlsInPrivateMessages: Boolean
    by booleanPreference(KEY_INLINE_URLS_IN_PRIVATE_MESSAGES, true)
  var showCrossPostsInPost: Boolean
    by booleanPreference(KEY_SHOW_CROSS_POSTS_IN_POST, true)
  var delayWhenLoadingData: Boolean
    by booleanPreference(KEY_PERF_DELAY_WHEN_LOADING_DATA, true)
  var loopVideoByDefault: Boolean
    by booleanPreference(KEY_LOOP_VIDEO_BY_DEFAULT, false)
  var doNotBlurNsfwContentInNsfwCommunityFeed: Boolean
    by booleanPreference(KEY_DO_NOT_BLUR_NSFW_CONTENT_IN_NSFW_COMMUNITY_FEED, false)
  var preferUserDisplayName: Boolean
    by booleanPreference(KEY_PREFER_USER_DISPLAY_NAME, false)
  var preferCommunityDisplayName: Boolean
    by booleanPreference(KEY_PREFER_COMMUNITY_DISPLAY_NAME, false)
  var opTagStyle: Int
    by intPreference(KEY_OP_TAG_STYLE, OpTagStyleIds.MIC)
  var autoFocusSearchBar: Boolean
    by booleanPreference(KEY_AUTO_FOCUS_SEARCH_BAR, false)
  var hideHeaderBannerIfNoBanner
    by booleanPreference(KEY_HIDE_HEADER_BANNER_IF_NO_BANNER, false)
  var restoreBrowsingSessions
    by booleanPreference(KEY_RESTORE_BROWSING_SESSIONS, true)
  var finishAppOnBack
    by booleanPreference(KEY_FINISH_APP_ON_BACK, false)
  var showNavigationBarOnPost
    by booleanPreference(KEY_SHOW_NAVIGATION_BAR_ON_POST, false)
  var inboxFabAction
    by intPreference(KEY_INBOX_FAB_ACTION, InboxFabActionId.MARK_ALL_AS_READ)
  var inboxLayout
    by intPreference(KEY_INBOX_LAYOUT, InboxLayoutId.COMPACT)
  var inboxAutoMarkAsRead
    by booleanPreference(KEY_INBOX_AUTO_MARK_AS_READ, false)
  var markAsReadOnHidePost
    by booleanPreference(KEY_MARK_AS_READ_ON_HIDE_POST, false)
  var stringForNullScore
    by stringPreference(KEY_STRING_FOR_NULL_SCORE, null)
  var hideReadByDefault
    by booleanPreference(KEY_HIDE_READ_BY_DEFAULT, false)
  var highlightNewComments
    by booleanPreference(KEY_HIGHLIGHT_NEW_COMMENTS, true)

  suspend fun getOfflinePostCount(): Int =
    context.offlineModeDataStore.data.first()[intPreferencesKey("offlinePostCount")]
      ?: 100

  fun reset(key: String) {
    sharedPreferences.edit { remove(key) }
  }

  fun asJson(): JSONObject = sharedPreferences.asJson()

  fun generateCode(): String {
    val json = this.asJson()
    return Utils.compress(json.toString(), Base64.NO_WRAP)
  }

  fun importSettings(settingsToImport: JSONObject, excludeKeys: Set<String>) {
    sharedPreferences.importSettings(settingsToImport, excludeKeys)
  }

  private fun SharedPreferences.importSettings(
    settingsToImport: JSONObject,
    excludeKeys: Set<String>,
  ) {
    val allKeys = settingsToImport.keys().asSequence()
    this.edit {
      for (key in allKeys) {
        if (excludeKeys.contains(key)) continue

        when (val value = settingsToImport.opt(key)) {
          is Float -> putFloat(key, value)
          is Long -> putLong(key, value)
          is String -> putString(key, value)
          is Boolean -> putBoolean(key, value)
          is Int -> putInt(key, value)
          null -> remove(key)
        }
      }
    }
  }

  fun clear() {
    sharedPreferences.edit(commit = true) { clear() }
  }
}
