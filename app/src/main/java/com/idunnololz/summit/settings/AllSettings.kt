package com.idunnololz.summit.settings

import android.content.Context
import com.idunnololz.summit.BuildConfig
import com.idunnololz.summit.R
import com.idunnololz.summit.cache.CachePolicy
import com.idunnololz.summit.links.PreviewLinkOptions
import com.idunnololz.summit.preferences.ColorSchemes
import com.idunnololz.summit.preferences.CommentGestureAction
import com.idunnololz.summit.preferences.CommentHeaderLayoutId
import com.idunnololz.summit.preferences.CommentsThreadStyle
import com.idunnololz.summit.preferences.FontIds
import com.idunnololz.summit.preferences.GestureSwipeDirectionIds
import com.idunnololz.summit.preferences.HomeFabQuickActionIds
import com.idunnololz.summit.preferences.NavigationRailModeId
import com.idunnololz.summit.preferences.PostFabQuickActions
import com.idunnololz.summit.preferences.PostGestureAction
import com.idunnololz.summit.preferences.UserAgentChoiceIds
import com.idunnololz.summit.settings.SettingPath.getPageName
import com.idunnololz.summit.settings.misc.DisplayInstanceOptions
import com.idunnololz.summit.settings.navigation.NavBarDestinations
import com.idunnololz.summit.util.PreferenceUtils
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
import com.idunnololz.summit.util.PreferenceUtils.KEY_COMMENT_GESTURE_ACTION_1
import com.idunnololz.summit.util.PreferenceUtils.KEY_COMMENT_GESTURE_ACTION_2
import com.idunnololz.summit.util.PreferenceUtils.KEY_COMMENT_GESTURE_ACTION_3
import com.idunnololz.summit.util.PreferenceUtils.KEY_COMMENT_GESTURE_ACTION_COLOR_1
import com.idunnololz.summit.util.PreferenceUtils.KEY_COMMENT_GESTURE_ACTION_COLOR_2
import com.idunnololz.summit.util.PreferenceUtils.KEY_COMMENT_GESTURE_ACTION_COLOR_3
import com.idunnololz.summit.util.PreferenceUtils.KEY_COMMENT_GESTURE_SIZE
import com.idunnololz.summit.util.PreferenceUtils.KEY_COMMENT_SHOW_UP_AND_DOWN_VOTES
import com.idunnololz.summit.util.PreferenceUtils.KEY_COMMUNITY_SELECTOR_SHOW_COMMUNITY_SUGGESTIONS
import com.idunnololz.summit.util.PreferenceUtils.KEY_DEFAULT_COMMENTS_SORT_ORDER
import com.idunnololz.summit.util.PreferenceUtils.KEY_DEFAULT_COMMUNITY_SORT_ORDER
import com.idunnololz.summit.util.PreferenceUtils.KEY_DISPLAY_INSTANCE_STYLE
import com.idunnololz.summit.util.PreferenceUtils.KEY_DOWNLOAD_DIRECTORY
import com.idunnololz.summit.util.PreferenceUtils.KEY_DOWNVOTE_COLOR
import com.idunnololz.summit.util.PreferenceUtils.KEY_ENABLE_HIDDEN_POSTS
import com.idunnololz.summit.util.PreferenceUtils.KEY_GESTURE_SWIPE_DIRECTION
import com.idunnololz.summit.util.PreferenceUtils.KEY_GLOBAL_FONT
import com.idunnololz.summit.util.PreferenceUtils.KEY_GLOBAL_FONT_COLOR
import com.idunnololz.summit.util.PreferenceUtils.KEY_GLOBAL_FONT_SIZE
import com.idunnololz.summit.util.PreferenceUtils.KEY_GLOBAL_LAYOUT_MODE
import com.idunnololz.summit.util.PreferenceUtils.KEY_HAPTICS_ENABLED
import com.idunnololz.summit.util.PreferenceUtils.KEY_HAPTICS_ON_ACTIONS
import com.idunnololz.summit.util.PreferenceUtils.KEY_HIDE_COMMENT_SCORES
import com.idunnololz.summit.util.PreferenceUtils.KEY_HIDE_DUPLICATE_POSTS_ON_READ
import com.idunnololz.summit.util.PreferenceUtils.KEY_HIDE_POST_SCORES
import com.idunnololz.summit.util.PreferenceUtils.KEY_HOME_FAB_QUICK_ACTION
import com.idunnololz.summit.util.PreferenceUtils.KEY_IMAGE_PREVIEW_HIDE_UI_BY_DEFAULT
import com.idunnololz.summit.util.PreferenceUtils.KEY_INDICATE_CONTENT_FROM_CURRENT_USER
import com.idunnololz.summit.util.PreferenceUtils.KEY_INFINITY
import com.idunnololz.summit.util.PreferenceUtils.KEY_INFINITY_PAGE_INDICATOR
import com.idunnololz.summit.util.PreferenceUtils.KEY_IS_NOTIFICATIONS_ON
import com.idunnololz.summit.util.PreferenceUtils.KEY_LEFT_HAND_MODE
import com.idunnololz.summit.util.PreferenceUtils.KEY_LOCK_BOTTOM_BAR
import com.idunnololz.summit.util.PreferenceUtils.KEY_MARK_POSTS_AS_READ_ON_SCROLL
import com.idunnololz.summit.util.PreferenceUtils.KEY_NAV_BAR_ITEMS
import com.idunnololz.summit.util.PreferenceUtils.KEY_NOTIFICATIONS_CHECK_INTERVAL_MS
import com.idunnololz.summit.util.PreferenceUtils.KEY_OPEN_LINKS_IN_APP
import com.idunnololz.summit.util.PreferenceUtils.KEY_PARSE_MARKDOWN_IN_POST_TITLES
import com.idunnololz.summit.util.PreferenceUtils.KEY_POST_AND_COMMENTS_UI_CONFIG
import com.idunnololz.summit.util.PreferenceUtils.KEY_POST_FAB_QUICK_ACTION
import com.idunnololz.summit.util.PreferenceUtils.KEY_POST_FEED_SHOW_SCROLL_BAR
import com.idunnololz.summit.util.PreferenceUtils.KEY_POST_FULL_BLEED_IMAGE
import com.idunnololz.summit.util.PreferenceUtils.KEY_POST_GESTURE_ACTION_1
import com.idunnololz.summit.util.PreferenceUtils.KEY_POST_GESTURE_ACTION_2
import com.idunnololz.summit.util.PreferenceUtils.KEY_POST_GESTURE_ACTION_3
import com.idunnololz.summit.util.PreferenceUtils.KEY_POST_GESTURE_ACTION_COLOR_1
import com.idunnololz.summit.util.PreferenceUtils.KEY_POST_GESTURE_ACTION_COLOR_2
import com.idunnololz.summit.util.PreferenceUtils.KEY_POST_GESTURE_ACTION_COLOR_3
import com.idunnololz.summit.util.PreferenceUtils.KEY_POST_GESTURE_SIZE
import com.idunnololz.summit.util.PreferenceUtils.KEY_POST_LIST_VIEW_IMAGE_ON_SINGLE_TAP
import com.idunnololz.summit.util.PreferenceUtils.KEY_POST_SHOW_UP_AND_DOWN_VOTES
import com.idunnololz.summit.util.PreferenceUtils.KEY_PREFERRED_LOCALE
import com.idunnololz.summit.util.PreferenceUtils.KEY_PREFETCH_POSTS
import com.idunnololz.summit.util.PreferenceUtils.KEY_PREVIEW_LINKS
import com.idunnololz.summit.util.PreferenceUtils.KEY_RETAIN_LAST_POST
import com.idunnololz.summit.util.PreferenceUtils.KEY_ROTATE_INSTANCE_ON_UPLOAD_FAIL
import com.idunnololz.summit.util.PreferenceUtils.KEY_SAVE_DRAFTS_AUTOMATICALLY
import com.idunnololz.summit.util.PreferenceUtils.KEY_SEARCH_HOME_CONFIG
import com.idunnololz.summit.util.PreferenceUtils.KEY_SHAKE_TO_SEND_FEEDBACK
import com.idunnololz.summit.util.PreferenceUtils.KEY_SHOW_COMMENT_UPVOTE_PERCENTAGE
import com.idunnololz.summit.util.PreferenceUtils.KEY_SHOW_EDITED_DATE
import com.idunnololz.summit.util.PreferenceUtils.KEY_SHOW_FILTERED_POSTS
import com.idunnololz.summit.util.PreferenceUtils.KEY_SHOW_IMAGE_POSTS
import com.idunnololz.summit.util.PreferenceUtils.KEY_SHOW_LABELS_IN_NAV_BAR
import com.idunnololz.summit.util.PreferenceUtils.KEY_SHOW_LINK_POSTS
import com.idunnololz.summit.util.PreferenceUtils.KEY_SHOW_NSFW_POSTS
import com.idunnololz.summit.util.PreferenceUtils.KEY_SHOW_POST_UPVOTE_PERCENTAGE
import com.idunnololz.summit.util.PreferenceUtils.KEY_SHOW_PROFILE_ICONS
import com.idunnololz.summit.util.PreferenceUtils.KEY_SHOW_TEXT_POSTS
import com.idunnololz.summit.util.PreferenceUtils.KEY_SHOW_VIDEO_POSTS
import com.idunnololz.summit.util.PreferenceUtils.KEY_TRACK_BROWSING_HISTORY
import com.idunnololz.summit.util.PreferenceUtils.KEY_TRANSPARENT_NOTIFICATION_BAR
import com.idunnololz.summit.util.PreferenceUtils.KEY_UPLOAD_IMAGES_TO_IMGUR
import com.idunnololz.summit.util.PreferenceUtils.KEY_UPVOTE_COLOR
import com.idunnololz.summit.util.PreferenceUtils.KEY_USE_BLACK_THEME
import com.idunnololz.summit.util.PreferenceUtils.KEY_USE_BOTTOM_NAV_BAR
import com.idunnololz.summit.util.PreferenceUtils.KEY_USE_CONDENSED_FOR_COMMENT_HEADERS
import com.idunnololz.summit.util.PreferenceUtils.KEY_USE_CUSTOM_NAV_BAR
import com.idunnololz.summit.util.PreferenceUtils.KEY_USE_FIREBASE
import com.idunnololz.summit.util.PreferenceUtils.KEY_USE_GESTURE_ACTIONS
import com.idunnololz.summit.util.PreferenceUtils.KEY_USE_LESS_DARK_BACKGROUND
import com.idunnololz.summit.util.PreferenceUtils.KEY_USE_MATERIAL_YOU
import com.idunnololz.summit.util.PreferenceUtils.KEY_USE_MULTILINE_POST_HEADERS
import com.idunnololz.summit.util.PreferenceUtils.KEY_USE_PER_COMMUNITY_SETTINGS
import com.idunnololz.summit.util.PreferenceUtils.KEY_USE_POSTS_FEED_HEADER
import com.idunnololz.summit.util.PreferenceUtils.KEY_USE_PREDICTIVE_BACK
import com.idunnololz.summit.util.PreferenceUtils.KEY_USE_VOLUME_BUTTON_NAVIGATION
import com.idunnololz.summit.util.PreferenceUtils.KEY_WARN_NEW_PERSON
import com.idunnololz.summit.util.PreferenceUtils.KEY_WARN_REPLY_TO_OLD_CONTENT
import com.idunnololz.summit.util.PreferenceUtils.KEY_WARN_REPLY_TO_OLD_CONTENT_THRESHOLD_MS
import com.idunnololz.summit.util.PrettyPrintUtils
import dagger.hilt.android.qualifiers.ActivityContext
import javax.inject.Inject
import kotlin.reflect.KClass

sealed interface SearchableSettings {
    val allSettings: List<SettingItem>
    val parents: List<KClass<out SearchableSettings>>
}

object SettingPath {
    fun KClass<out SearchableSettings>.getPageName(context: Context): String {
        return when (this) {
            AboutSettings::class ->
                context.getString(R.string.about_summit)
            CacheSettings::class ->
                context.getString(R.string.cache)
            PostAndCommentsSettings::class ->
                context.getString(R.string.post_and_comments)
            GestureSettings::class ->
                context.getString(R.string.gestures)
            HiddenPostsSettings::class ->
                context.getString(R.string.hidden_posts)
            LemmyWebSettings::class ->
                context.getString(R.string.lemmy_web_preferences)
            MainSettings::class ->
                context.getString(R.string.settings)
            PostAndCommentsAppearanceSettings::class ->
                context.getString(R.string.post_and_comments_appearance)
            PostsFeedSettings::class ->
                context.getString(R.string.post_list)
            ThemeSettings::class ->
                context.getString(R.string.theme)
            MiscSettings::class ->
                context.getString(R.string.misc)
            PostsFeedAppearanceSettings::class ->
                context.getString(R.string.post_appearance)
            LoggingSettings::class ->
                context.getString(R.string.logging)
            HistorySettings::class ->
                context.getString(R.string.history)
            NavigationSettings::class ->
                context.getString(R.string.navigation)
            ImportAndExportSettings::class ->
                context.getString(R.string.backup_and_restore_settings)

            DownloadSettings::class ->
                context.getString(R.string.downloads)
            PerCommunitySettings::class ->
                context.getString(R.string.per_community_settings)
            PerAccountSettings::class ->
                context.getString(R.string.per_account_settings)
            ActionsSettings::class ->
                context.getString(R.string.user_actions)
            NotificationSettings::class ->
                context.getString(R.string.notifications)

            SearchHomeSettings::class ->
                context.getString(R.string.search_screen_settings)

            HapticSettings::class ->
                context.getString(R.string.vibration_and_haptics)

            VideoPlayerSettings::class ->
                context.getString(R.string.video_player)

            DefaultAppsSettings::class ->
                context.getString(R.string.default_apps)

            else -> error("No name for $this")
        }
    }

    fun SearchableSettings.getPageName(context: Context): String = this::class.getPageName(context)
}

class MainSettings @Inject constructor(
    @ActivityContext private val context: Context,
) : SearchableSettings {
    override val parents: List<KClass<out SearchableSettings>> = listOf()

    val settingTheme = BasicSettingItem(
        R.drawable.baseline_palette_24,
        context.getString(R.string.theme),
        context.getString(R.string.theme_settings_desc),
    )
    val settingPostAndComments = BasicSettingItem(
        R.drawable.baseline_comment_24,
        context.getString(R.string.post_and_comments),
        context.getString(R.string.post_and_comments_settings_desc),
    )
    val settingLemmyWeb = BasicSettingItem(
        R.drawable.ic_lemmy_24,
        context.getString(R.string.lemmy_web_preferences),
        context.getString(R.string.lemmy_web_preferences_desc),
    )
    val settingAccount = BasicSettingItem(
        R.drawable.outline_account_circle_24,
        context.getString(R.string.summit_account_settings),
        context.getString(R.string.summit_account_settings_desc),
    )
    val settingGestures = BasicSettingItem(
        R.drawable.baseline_gesture_24,
        context.getString(R.string.gestures),
        context.getString(R.string.gestures_desc),
    )
    val settingCache = BasicSettingItem(
        R.drawable.baseline_cached_24,
        context.getString(R.string.cache),
        context.getString(R.string.cache_info_and_preferences),
    )
    val settingHiddenPosts = BasicSettingItem(
        R.drawable.baseline_hide_24,
        context.getString(R.string.hidden_posts),
        context.getString(R.string.hidden_posts_desc),
    )
    val settingPostsFeed = BasicSettingItem(
        R.drawable.baseline_pages_24,
        context.getString(R.string.post_list),
        context.getString(R.string.setting_post_list_desc),
    )
    val settingAbout = BasicSettingItem(
        R.drawable.outline_info_24,
        context.getString(R.string.about_summit),
        context.getString(R.string.about_summit_desc),
    )
    val settingSummitCommunity = BasicSettingItem(
        R.drawable.ic_logo_mono_24,
        context.getString(R.string.c_summit),
        context.getString(R.string.summit_community_desc),
    )

    val commentListSettings = BasicSettingItem(
        R.drawable.baseline_comment_24,
        context.getString(R.string.comment_list),
        context.getString(R.string.comment_list_desc),
    )

    val miscSettings = BasicSettingItem(
        R.drawable.baseline_miscellaneous_services_24,
        context.getString(R.string.misc),
        context.getString(R.string.misc_desc),
    )
    val loggingSettings = BasicSettingItem(
        R.drawable.outline_analytics_24,
        context.getString(R.string.logging),
        context.getString(R.string.logging_desc),
    )
    val historySettings = BasicSettingItem(
        R.drawable.baseline_history_24,
        context.getString(R.string.history),
        context.getString(R.string.history_desc),
    )
    val navigationSettings = BasicSettingItem(
        R.drawable.outline_navigation_24,
        context.getString(R.string.navigation),
        context.getString(R.string.navigation_desc),
    )
    val userActionsSettings = BasicSettingItem(
        R.drawable.outline_play_arrow_24,
        context.getString(R.string.user_actions),
        context.getString(R.string.user_actions_desc),
    )
    val backupAndRestoreSettings = BasicSettingItem(
        R.drawable.baseline_import_export_24,
        context.getString(R.string.backup_and_restore_settings),
        context.getString(R.string.backup_and_restore_settings_desc),
    )
    val downloadSettings = BasicSettingItem(
        R.drawable.baseline_download_24,
        context.getString(R.string.downloads),
        context.getString(R.string.downloads_desc),
    )
    val notificationSettings = BasicSettingItem(
        R.drawable.outline_notifications_24,
        context.getString(R.string.notifications),
        context.getString(R.string.notifications_desc),
    )
    val hapticSettings = BasicSettingItem(
        R.drawable.baseline_vibration_24,
        context.getString(R.string.vibration_and_haptics),
        null,
    )
    val videoPlayerSettings = BasicSettingItem(
        R.drawable.outline_video_player_24,
        context.getString(R.string.video_player),
        context.getString(R.string.video_player_desc),
    )
    val defaultAppsSettings = BasicSettingItem(
        R.drawable.outline_apps_24,
        context.getString(R.string.default_apps),
        context.getString(R.string.default_apps_desc),
    )

    override val allSettings = listOf(
        SubgroupItem(
            context.getString(R.string.look_and_feel),
            listOf(
                settingTheme,
                settingPostsFeed,
                settingPostAndComments,
                settingGestures,
                hapticSettings,
                videoPlayerSettings,
                miscSettings,
            ),
        ),
        SubgroupItem(
            context.getString(R.string.account_settings),
            listOf(
                settingLemmyWeb,
                settingAccount,
            ),
        ),
        SubgroupItem(
            context.getString(R.string.systems),
            listOf(
                settingCache,
                settingHiddenPosts,
                loggingSettings,
                historySettings,
                navigationSettings,
                userActionsSettings,
                downloadSettings,
                notificationSettings,
                defaultAppsSettings,
                backupAndRestoreSettings,
            ),
        ),
        SubgroupItem(
            context.getString(R.string.about),
            listOf(
                settingAbout,
                settingSummitCommunity,
            ),
        ),
    )
}

class LemmyWebSettings @Inject constructor(
    @ActivityContext private val context: Context,
) : SearchableSettings {
    override val parents: List<KClass<out SearchableSettings>> = listOf(
        MainSettings::class,
    )

    val instanceSetting = TextValueSettingItem(
        title = context.getString(R.string.instance),
        description = null,
        supportsRichText = false,
        isEnabled = false,
    )

    val displayNameSetting = TextValueSettingItem(
        title = context.getString(R.string.display_name),
        description = null,
        supportsRichText = false,
    )

    val bioSetting = TextValueSettingItem(
        title = context.getString(R.string.biography),
        description = null,
        supportsRichText = true,
    )

    val emailSetting = TextValueSettingItem(
        title = context.getString(R.string.email),
        description = null,
        supportsRichText = false,
    )

    val matrixSetting = TextValueSettingItem(
        title = context.getString(R.string.matrix_user),
        description = null,
        supportsRichText = false,
        hint = "@user:example.com",
    )

    val avatarSetting = ImageValueSettingItem(
        title = context.getString(R.string.profile_image),
        description = null,
        isSquare = true,
    )

    val bannerSetting = ImageValueSettingItem(
        title = context.getString(R.string.banner_image),
        description = null,
        isSquare = false,
    )

    val defaultSortType = RadioGroupSettingItem(
        null,
        context.getString(R.string.default_sort_type),
        null,
        makeCommunitySortOrderChoices(context),
    )

    val showNsfwSetting = OnOffSettingItem(
        null,
        context.getString(R.string.show_nsfw),
        null,
    )

    val showReadPostsSetting = OnOffSettingItem(
        null,
        context.getString(R.string.show_read_posts),
        null,
    )

    val botAccountSetting = OnOffSettingItem(
        null,
        context.getString(R.string.bot_account),
        null,
    )

    val showBotAccountsSetting = OnOffSettingItem(
        null,
        context.getString(R.string.show_bot_accounts),
        null,
    )

    val sendNotificationsToEmailSetting = OnOffSettingItem(
        null,
        context.getString(R.string.send_notifications_to_email),
        null,
    )

    val blockSettings = BasicSettingItem(
        null,
        context.getString(R.string.account_block_settings),
        context.getString(R.string.account_block_settings_desc),
    )

    val changePassword = BasicSettingItem(
        null,
        context.getString(R.string.change_password),
        null,
    )

    override val allSettings: List<SettingItem> = listOf(
        instanceSetting,
        displayNameSetting,
        bioSetting,
        emailSetting,
        matrixSetting,
        avatarSetting,
        bannerSetting,
        defaultSortType,
        showNsfwSetting,
        showReadPostsSetting,
        botAccountSetting,
        showBotAccountsSetting,
        sendNotificationsToEmailSetting,
        blockSettings,
        changePassword,
    )
}

class GestureSettings @Inject constructor(
    @ActivityContext private val context: Context,
) : SearchableSettings {
    override val parents: List<KClass<out SearchableSettings>> = listOf(
        MainSettings::class,
    )
    private val postGestureActionOptions =
        listOf(
            RadioGroupSettingItem.RadioGroupOption(
                PostGestureAction.Upvote,
                context.getString(R.string.upvote),
                null,
                R.drawable.baseline_arrow_upward_24,
            ),
            RadioGroupSettingItem.RadioGroupOption(
                PostGestureAction.Downvote,
                context.getString(R.string.downvote),
                null,
                R.drawable.baseline_arrow_downward_24,
            ),
            RadioGroupSettingItem.RadioGroupOption(
                PostGestureAction.Reply,
                context.getString(R.string.reply),
                null,
                R.drawable.baseline_reply_24,
            ),
            RadioGroupSettingItem.RadioGroupOption(
                PostGestureAction.MarkAsRead,
                context.getString(R.string.mark_as_read),
                null,
                R.drawable.baseline_check_24,
            ),
            RadioGroupSettingItem.RadioGroupOption(
                PostGestureAction.Hide,
                context.getString(R.string.hide_post),
                null,
                R.drawable.baseline_hide_24,
            ),
            RadioGroupSettingItem.RadioGroupOption(
                PostGestureAction.Bookmark,
                context.getString(R.string.bookmark),
                null,
                R.drawable.baseline_bookmark_add_24,
            ),
            RadioGroupSettingItem.RadioGroupOption(
                PostGestureAction.None,
                context.getString(R.string.none),
                null,
                R.drawable.baseline_none_24,
            ),
        )
    private val commentGestureActionOptions =
        listOf(
            RadioGroupSettingItem.RadioGroupOption(
                CommentGestureAction.Upvote,
                context.getString(R.string.upvote),
                null,
                R.drawable.baseline_arrow_upward_24,
            ),
            RadioGroupSettingItem.RadioGroupOption(
                CommentGestureAction.Downvote,
                context.getString(R.string.downvote),
                null,
                R.drawable.baseline_arrow_downward_24,
            ),
            RadioGroupSettingItem.RadioGroupOption(
                CommentGestureAction.Reply,
                context.getString(R.string.reply),
                null,
                R.drawable.baseline_reply_24,
            ),
            RadioGroupSettingItem.RadioGroupOption(
                CommentGestureAction.Bookmark,
                context.getString(R.string.bookmark),
                null,
                R.drawable.baseline_bookmark_add_24,
            ),
            RadioGroupSettingItem.RadioGroupOption(
                CommentGestureAction.CollapseOrExpand,
                context.getString(R.string.collapse_or_expand),
                null,
                R.drawable.baseline_unfold_less_24,
            ),
            RadioGroupSettingItem.RadioGroupOption(
                CommentGestureAction.None,
                context.getString(R.string.none),
                null,
                R.drawable.baseline_none_24,
            ),
        )

    val useGestureActions = OnOffSettingItem(
        null,
        context.getString(R.string.use_gesture_actions),
        context.getString(R.string.use_gesture_actions_desc),
        relatedKeys = listOf(KEY_USE_GESTURE_ACTIONS),
    )
    val gestureSwipeDirection = RadioGroupSettingItem(
        null,
        context.getString(R.string.gesture_swipe_direction),
        null,
        listOf(
            RadioGroupSettingItem.RadioGroupOption(
                GestureSwipeDirectionIds.LEFT,
                context.getString(R.string.right_to_left),
                null,
                null,
            ),
            RadioGroupSettingItem.RadioGroupOption(
                GestureSwipeDirectionIds.RIGHT,
                context.getString(R.string.left_to_right),
                null,
                null,
            ),
            RadioGroupSettingItem.RadioGroupOption(
                GestureSwipeDirectionIds.ANY,
                context.getString(R.string.any_direction),
                null,
                null,
            ),
        ),
        relatedKeys = listOf(KEY_GESTURE_SWIPE_DIRECTION),
    )
    val postGestureAction1 = RadioGroupSettingItem(
        null,
        context.getString(R.string.gesture_action_1),
        null,
        postGestureActionOptions,
        relatedKeys = listOf(KEY_POST_GESTURE_ACTION_1),
    )
    val postGestureActionColor1 = ColorSettingItem(
        null,
        context.getString(R.string.gesture_action_1_color),
        null,
        relatedKeys = listOf(KEY_POST_GESTURE_ACTION_COLOR_1),
    )
    val postGestureAction2 = RadioGroupSettingItem(
        null,
        context.getString(R.string.gesture_action_2),
        null,
        postGestureActionOptions,
        relatedKeys = listOf(KEY_POST_GESTURE_ACTION_2),
    )
    val postGestureActionColor2 = ColorSettingItem(
        null,
        context.getString(R.string.gesture_action_2_color),
        null,
        relatedKeys = listOf(KEY_POST_GESTURE_ACTION_COLOR_2),
    )
    val postGestureAction3 = RadioGroupSettingItem(
        null,
        context.getString(R.string.gesture_action_3),
        null,
        postGestureActionOptions,
        relatedKeys = listOf(KEY_POST_GESTURE_ACTION_3),
    )
    val postGestureActionColor3 = ColorSettingItem(
        null,
        context.getString(R.string.gesture_action_3_color),
        null,
        relatedKeys = listOf(KEY_POST_GESTURE_ACTION_COLOR_3),
    )
    val postGestureSize = SliderSettingItem(
        context.getString(R.string.post_gesture_size),
        0f,
        1f,
        0.01f,
        relatedKeys = listOf(KEY_POST_GESTURE_SIZE),
    )

    val commentGestureAction1 = RadioGroupSettingItem(
        null,
        context.getString(R.string.gesture_action_1),
        null,
        commentGestureActionOptions,
        relatedKeys = listOf(KEY_COMMENT_GESTURE_ACTION_1),
    )
    val commentGestureActionColor1 = ColorSettingItem(
        null,
        context.getString(R.string.gesture_action_1_color),
        null,
        relatedKeys = listOf(KEY_COMMENT_GESTURE_ACTION_COLOR_1),
    )
    val commentGestureAction2 = RadioGroupSettingItem(
        null,
        context.getString(R.string.gesture_action_2),
        null,
        commentGestureActionOptions,
        relatedKeys = listOf(KEY_COMMENT_GESTURE_ACTION_2),
    )
    val commentGestureActionColor2 = ColorSettingItem(
        null,
        context.getString(R.string.gesture_action_2_color),
        null,
        relatedKeys = listOf(KEY_COMMENT_GESTURE_ACTION_COLOR_2),
    )
    val commentGestureAction3 = RadioGroupSettingItem(
        null,
        context.getString(R.string.gesture_action_3),
        null,
        commentGestureActionOptions,
        relatedKeys = listOf(KEY_COMMENT_GESTURE_ACTION_3),
    )
    val commentGestureActionColor3 = ColorSettingItem(
        null,
        context.getString(R.string.gesture_action_3_color),
        null,
        relatedKeys = listOf(KEY_COMMENT_GESTURE_ACTION_COLOR_3),
    )
    val commentGestureSize = SliderSettingItem(
        context.getString(R.string.comment_gesture_size),
        0f,
        1f,
        0.01f,
        relatedKeys = listOf(KEY_COMMENT_GESTURE_SIZE),
    )
    override val allSettings: List<SettingItem> = listOf(
        postGestureAction1,
        postGestureAction2,
        postGestureAction3,
        postGestureSize,
        gestureSwipeDirection,

        commentGestureAction1,
        commentGestureAction2,
        commentGestureAction3,
        commentGestureSize,
    )
}

class PostsFeedSettings @Inject constructor(
    @ActivityContext private val context: Context,
) : SearchableSettings {
    override val parents: List<KClass<out SearchableSettings>> = listOf(
        MainSettings::class,
    )

    val settingsPostFeedAppearance = BasicSettingItem(
        R.drawable.baseline_view_agenda_24,
        context.getString(R.string.post_appearance),
        context.getString(R.string.view_type_settings_desc),
    )
    val infinity = OnOffSettingItem(
        null,
        context.getString(R.string.infinity),
        context.getString(R.string.no_your_limits),
        relatedKeys = listOf(KEY_INFINITY),
    )
    val markPostsAsReadOnScroll = OnOffSettingItem(
        null,
        context.getString(R.string.mark_posts_as_read_on_scroll),
        context.getString(R.string.mark_posts_as_read_on_scroll_desc),
        relatedKeys = listOf(KEY_MARK_POSTS_AS_READ_ON_SCROLL),
    )
    val blurNsfwPosts = OnOffSettingItem(
        null,
        context.getString(R.string.blur_nsfw_posts),
        null,
        relatedKeys = listOf(KEY_BLUR_NSFW_POSTS),
    )
    val defaultCommunitySortOrder = RadioGroupSettingItem(
        null,
        context.getString(R.string.default_posts_sort_order),
        null,
        makeCommunitySortOrderChoices(context) +
            RadioGroupSettingItem.RadioGroupOption(
                R.id.community_sort_order_default,
                context.getString(R.string._default),
                null,
                null,
            ),
        relatedKeys = listOf(KEY_DEFAULT_COMMUNITY_SORT_ORDER),
    )

    val viewImageOnSingleTap = OnOffSettingItem(
        null,
        context.getString(R.string.view_image_on_single_tap),
        context.getString(R.string.view_image_on_single_tap_desc),
        relatedKeys = listOf(KEY_POST_LIST_VIEW_IMAGE_ON_SINGLE_TAP),
    )

    val postScores = RadioGroupSettingItem(
        null,
        context.getString(R.string.post_scores),
        context.getString(R.string.post_scores_desc),
        listOf(
            RadioGroupSettingItem.RadioGroupOption(
                R.id.show_scores,
                context.getString(R.string.show_scores),
                null,
                null,
            ),
            RadioGroupSettingItem.RadioGroupOption(
                R.id.show_up_and_down_votes,
                context.getString(R.string.show_up_and_down_votes),
                null,
                null,
            ),
            RadioGroupSettingItem.RadioGroupOption(
                R.id.hide_scores,
                context.getString(R.string.hide_scores),
                null,
                null,
            ),
        ),
        relatedKeys = listOf(KEY_HIDE_POST_SCORES, KEY_POST_SHOW_UP_AND_DOWN_VOTES),
    )
    val keywordFilters = BasicSettingItem(
        null,
        context.getString(R.string.keyword_filters),
        context.getString(R.string.keyword_filters_desc),
    )
    val instanceFilters = BasicSettingItem(
        null,
        context.getString(R.string.instance_filters),
        context.getString(R.string.instance_filters_desc),
    )
    val communityFilters = BasicSettingItem(
        null,
        context.getString(R.string.community_filters),
        context.getString(R.string.community_filters_desc),
    )
    val userFilters = BasicSettingItem(
        null,
        context.getString(R.string.user_filters),
        context.getString(R.string.user_filters_desc),
    )
    val showLinkPosts = OnOffSettingItem(
        R.drawable.baseline_link_24,
        context.getString(R.string.show_link_posts),
        null,
        relatedKeys = listOf(KEY_SHOW_LINK_POSTS),
    )
    val showImagePosts = OnOffSettingItem(
        R.drawable.baseline_image_24,
        context.getString(R.string.show_image_posts),
        null,
        relatedKeys = listOf(KEY_SHOW_IMAGE_POSTS),
    )
    val showVideoPosts = OnOffSettingItem(
        R.drawable.baseline_videocam_24,
        context.getString(R.string.show_video_posts),
        null,
        relatedKeys = listOf(KEY_SHOW_VIDEO_POSTS),
    )
    val showTextPosts = OnOffSettingItem(
        R.drawable.baseline_text_fields_24,
        context.getString(R.string.show_text_posts),
        null,
        relatedKeys = listOf(KEY_SHOW_TEXT_POSTS),
    )
    val showNsfwPosts = OnOffSettingItem(
        R.drawable.ic_nsfw_24,
        context.getString(R.string.show_nsfw_posts),
        null,
        relatedKeys = listOf(KEY_SHOW_NSFW_POSTS),
    )
    val lockBottomBar = OnOffSettingItem(
        null,
        context.getString(R.string.lock_bottom_bar),
        context.getString(R.string.lock_bottom_bar_desc),
        relatedKeys = listOf(KEY_LOCK_BOTTOM_BAR),
    )
    val autoLoadMorePosts = OnOffSettingItem(
        null,
        context.getString(R.string.auto_load_more_posts),
        context.getString(R.string.auto_load_more_posts_desc),
        relatedKeys = listOf(KEY_AUTO_LOAD_MORE_POSTS),
    )
    val infinityPageIndicator = OnOffSettingItem(
        null,
        context.getString(R.string.show_page_numbers),
        context.getString(R.string.show_page_numbers_desc),
        relatedKeys = listOf(KEY_INFINITY_PAGE_INDICATOR),
    )
    val showPostUpvotePercentage = OnOffSettingItem(
        null,
        context.getString(R.string.show_post_upvote_percentage),
        null,
        relatedKeys = listOf(KEY_SHOW_POST_UPVOTE_PERCENTAGE),
    )
    val useMultilinePostHeaders = OnOffSettingItem(
        null,
        context.getString(R.string.use_multiline_post_header),
        null,
        relatedKeys = listOf(KEY_USE_MULTILINE_POST_HEADERS),
    )
    val showFilteredPosts = OnOffSettingItem(
        null,
        context.getString(R.string.show_filtered_posts),
        context.getString(R.string.show_filtered_posts_desc),
        relatedKeys = listOf(KEY_SHOW_FILTERED_POSTS),
    )
    val prefetchPosts = OnOffSettingItem(
        null,
        context.getString(R.string.prefetch_posts),
        context.getString(R.string.prefetch_posts_desc),
        relatedKeys = listOf(KEY_PREFETCH_POSTS),
    )
    val homeFabQuickAction = RadioGroupSettingItem(
        null,
        context.getString(R.string.home_fab_quick_action),
        context.getString(R.string.home_fab_quick_action_desc),
        listOf(
            RadioGroupSettingItem.RadioGroupOption(
                HomeFabQuickActionIds.None,
                context.getString(R.string.no_action),
                null,
                null,
            ),
            RadioGroupSettingItem.RadioGroupOption(
                HomeFabQuickActionIds.CreatePost,
                context.getString(R.string.create_post),
                null,
                null,
            ),
            RadioGroupSettingItem.RadioGroupOption(
                HomeFabQuickActionIds.HideRead,
                context.getString(R.string.hide_read),
                null,
                null,
            ),
            RadioGroupSettingItem.RadioGroupOption(
                HomeFabQuickActionIds.ToggleNsfwMode,
                context.getString(R.string.toggle_nsfw),
                null,
                null,
            ),
        ),
        relatedKeys = listOf(KEY_HOME_FAB_QUICK_ACTION),
    )
    val parseMarkdownInPostTitles = OnOffSettingItem(
        null,
        context.getString(R.string.parse_markdown_in_post_titles),
        context.getString(R.string.parse_markdown_in_post_titles_desc),
        relatedKeys = listOf(KEY_PARSE_MARKDOWN_IN_POST_TITLES),
    )
    val homePage = TextValueSettingItem(
        context.getString(R.string.home_page),
        context.getString(R.string.home_page_desc),
        false,
    )
    val postFeedShowScrollBar = OnOffSettingItem(
        null,
        context.getString(R.string.show_scroll_bar),
        null,
        relatedKeys = listOf(KEY_POST_FEED_SHOW_SCROLL_BAR),
    )
    val hideDuplicatePostsOnRead = OnOffSettingItem(
        null,
        context.getString(R.string.hide_duplicate_posts_on_read),
        context.getString(R.string.hide_duplicate_posts_on_read_desc),
        relatedKeys = listOf(KEY_HIDE_DUPLICATE_POSTS_ON_READ),
    )
    val usePostsFeedHeader = OnOffSettingItem(
        null,
        context.getString(R.string.use_posts_feed_header),
        context.getString(R.string.use_posts_feed_header_desc),
        relatedKeys = listOf(KEY_USE_POSTS_FEED_HEADER),
    )

    override val allSettings: List<SettingItem> = listOf(
        infinity,
        markPostsAsReadOnScroll,
        blurNsfwPosts,
        defaultCommunitySortOrder,
        viewImageOnSingleTap,
        postScores,
        keywordFilters,
        instanceFilters,
        communityFilters,
        userFilters,
        showLinkPosts,
        showImagePosts,
        showVideoPosts,
        showTextPosts,
        showNsfwPosts,
        lockBottomBar,
        showFilteredPosts,
        homeFabQuickAction,
        prefetchPosts,
        homePage,
        postFeedShowScrollBar,
        hideDuplicatePostsOnRead,
    )
}

class PostAndCommentsSettings @Inject constructor(
    @ActivityContext private val context: Context,
) : SearchableSettings {
    override val parents: List<KClass<out SearchableSettings>> = listOf(
        MainSettings::class,
    )
    val settingPostAndCommentsAppearance = BasicSettingItem(
        R.drawable.baseline_comment_24,
        context.getString(R.string.post_and_comments_appearance),
        context.getString(R.string.post_and_comments_appearance_settings_desc),
    )
    val customizePostQuickActions = BasicSettingItem(
        null,
        context.getString(R.string.customize_post_quick_actions),
        context.getString(R.string.customize_post_quick_actions_desc),
    )
    val defaultCommentsSortOrder = RadioGroupSettingItem(
        null,
        context.getString(R.string.default_comments_sort_order),
        null,
        listOf(
            RadioGroupSettingItem.RadioGroupOption(
                R.id.sort_order_hot,
                context.getString(R.string.sort_order_hot),
                null,
                null,
            ),
            RadioGroupSettingItem.RadioGroupOption(
                R.id.sort_order_top,
                context.getString(R.string.sort_order_top),
                null,
                null,
            ),
            RadioGroupSettingItem.RadioGroupOption(
                R.id.sort_order_new,
                context.getString(R.string.sort_order_new),
                null,
                null,
            ),
            RadioGroupSettingItem.RadioGroupOption(
                R.id.sort_order_old,
                context.getString(R.string.sort_order_old),
                null,
                null,
            ),
            RadioGroupSettingItem.RadioGroupOption(
                R.id.comments_sort_order_default,
                context.getString(R.string._default),
                null,
                null,
            ),
        ),
        relatedKeys = listOf(KEY_DEFAULT_COMMENTS_SORT_ORDER),
    )
    val relayStyleNavigation = OnOffSettingItem(
        null,
        context.getString(R.string.relay_style_navigation),
        context.getString(R.string.relay_style_navigation_desc),
        relatedKeys = listOf(KEY_COMMENTS_NAVIGATION_FAB),
    )
    val useVolumeButtonNavigation = OnOffSettingItem(
        null,
        context.getString(R.string.use_volume_button_navigation),
        context.getString(R.string.use_volume_button_navigation_desc),
        relatedKeys = listOf(KEY_USE_VOLUME_BUTTON_NAVIGATION),
    )
    val collapseChildCommentsByDefault = OnOffSettingItem(
        null,
        context.getString(R.string.collapse_child_comments),
        context.getString(R.string.collapse_child_comments_desc),
        relatedKeys = listOf(KEY_COLLAPSE_CHILD_COMMENTS_BY_DEFAULT),
    )
    val autoCollapseCommentThreshold = RadioGroupSettingItem(
        null,
        context.getString(R.string.auto_collapse_comment_threshold),
        null,
        listOf(
            RadioGroupSettingItem.RadioGroupOption(
                R.id.auto_collapse_comment_threshold_50,
                PrettyPrintUtils.defaultPercentFormat.format(0.5f),
                null,
                null,
            ),
            RadioGroupSettingItem.RadioGroupOption(
                R.id.auto_collapse_comment_threshold_40,
                PrettyPrintUtils.defaultPercentFormat.format(0.4f),
                null,
                null,
            ),
            RadioGroupSettingItem.RadioGroupOption(
                R.id.auto_collapse_comment_threshold_30,
                PrettyPrintUtils.defaultPercentFormat.format(0.3f),
                null,
                null,
            ),
            RadioGroupSettingItem.RadioGroupOption(
                R.id.auto_collapse_comment_threshold_20,
                PrettyPrintUtils.defaultPercentFormat.format(0.2f),
                null,
                null,
            ),
            RadioGroupSettingItem.RadioGroupOption(
                R.id.auto_collapse_comment_threshold_10,
                PrettyPrintUtils.defaultPercentFormat.format(0.1f),
                null,
                null,
            ),
            RadioGroupSettingItem.RadioGroupOption(
                R.id.auto_collapse_comment_threshold_never_collapse,
                context.getString(R.string.never_auto_collapse_comments),
                null,
                null,
            ),
        ),
        relatedKeys = listOf(KEY_AUTO_COLLAPSE_COMMENT_THRESHOLD),
    )
    val showCommentUpvotePercentage = OnOffSettingItem(
        null,
        context.getString(R.string.show_comment_upvote_percentage),
        null,
        relatedKeys = listOf(KEY_SHOW_COMMENT_UPVOTE_PERCENTAGE),
    )
    val showProfileIcons = OnOffSettingItem(
        null,
        context.getString(R.string.show_profile_icons),
        context.getString(R.string.show_profile_icons_desc),
        relatedKeys = listOf(KEY_SHOW_PROFILE_ICONS),
    )
    val commentHeaderLayout = RadioGroupSettingItem(
        null,
        context.getString(R.string.comment_header_layout),
        null,
        listOf(
            RadioGroupSettingItem.RadioGroupOption(
                CommentHeaderLayoutId.SingleLine,
                context.getString(R.string.single_line),
                null,
                null,
            ),
//            RadioGroupSettingItem.RadioGroupOption(
//                CommentHeaderLayoutId.Wrap,
//                context.getString(R.string.wrap),
//                null,
//                null,
//            ),
            RadioGroupSettingItem.RadioGroupOption(
                CommentHeaderLayoutId.Multiline,
                context.getString(R.string.multiline),
                null,
                null,
            ),
        ),
        relatedKeys = listOf(KEY_POST_AND_COMMENTS_UI_CONFIG),
    )
    val customizeCommentQuickActions = BasicSettingItem(
        null,
        context.getString(R.string.customize_comment_quick_actions),
        context.getString(R.string.customize_comment_quick_actions_desc),
    )
    val keywordFilters = BasicSettingItem(
        null,
        context.getString(R.string.keyword_filters),
        context.getString(R.string.comment_keyword_filters_desc),
    )
    val instanceFilters = BasicSettingItem(
        null,
        context.getString(R.string.instance_filters),
        context.getString(R.string.comment_instance_filters_desc),
    )
    val userFilters = BasicSettingItem(
        null,
        context.getString(R.string.user_filters),
        context.getString(R.string.comment_user_filters_desc),
    )
    val showInlineMediaAsLinks = OnOffSettingItem(
        null,
        context.getString(R.string.show_inline_media_as_links),
        context.getString(R.string.show_inline_media_as_links_desc),
    )
    val commentScores = RadioGroupSettingItem(
        null,
        context.getString(R.string.comment_scores),
        context.getString(R.string.comment_scores_desc),
        listOf(
            RadioGroupSettingItem.RadioGroupOption(
                R.id.show_scores,
                context.getString(R.string.show_scores),
                null,
                null,
            ),
            RadioGroupSettingItem.RadioGroupOption(
                R.id.show_up_and_down_votes,
                context.getString(R.string.show_up_and_down_votes),
                null,
                null,
            ),
            RadioGroupSettingItem.RadioGroupOption(
                R.id.hide_scores,
                context.getString(R.string.hide_scores),
                null,
                null,
            ),
        ),
        relatedKeys = listOf(KEY_HIDE_COMMENT_SCORES, KEY_COMMENT_SHOW_UP_AND_DOWN_VOTES),
    )
    val swipeBetweenPosts = OnOffSettingItem(
        null,
        context.getString(R.string.swipe_between_posts),
        context.getString(R.string.swipe_between_posts_desc),
    )
    val postFabQuickAction = RadioGroupSettingItem(
        null,
        context.getString(R.string.post_fab_quick_action),
        context.getString(R.string.post_fab_quick_action_desc),
        listOf(
            RadioGroupSettingItem.RadioGroupOption(
                PostFabQuickActions.NONE,
                context.getString(R.string.no_action),
                null,
                null,
            ),
            RadioGroupSettingItem.RadioGroupOption(
                PostFabQuickActions.ADD_COMMENT,
                context.getString(R.string.add_comment),
                null,
                null,
            ),
            RadioGroupSettingItem.RadioGroupOption(
                PostFabQuickActions.COLLAPSE_ALL_COMMENTS,
                context.getString(R.string.collapse_all_comments),
                null,
                null,
            ),
            RadioGroupSettingItem.RadioGroupOption(
                PostFabQuickActions.SCROLL_TO_TOP,
                context.getString(R.string.scroll_to_top),
                null,
                null,
            ),
        ),
        relatedKeys = listOf(KEY_POST_FAB_QUICK_ACTION),
    )

    override val allSettings: List<SettingItem> = listOf(
        defaultCommentsSortOrder,
        relayStyleNavigation,
        useVolumeButtonNavigation,
        commentHeaderLayout,
        settingPostAndCommentsAppearance,
        commentScores,
        swipeBetweenPosts,
        postFabQuickAction,
    )
}

class PostAndCommentsAppearanceSettings @Inject constructor(
    @ActivityContext private val context: Context,
) : SearchableSettings {
    override val parents: List<KClass<out SearchableSettings>> = listOf(
        MainSettings::class,
        PostAndCommentsSettings::class,
    )

    val resetPostStyles = BasicSettingItem(
        null,
        context.getString(R.string.reset_post_styles),
        null,
    )

    val resetCommentStyles = BasicSettingItem(
        null,
        context.getString(R.string.reset_comment_styles),
        null,
    )

    val postFontSize = SliderSettingItem(
        context.getString(R.string.font_size),
        0.2f,
        3f,
        0.1f,
        relatedKeys = listOf(KEY_POST_AND_COMMENTS_UI_CONFIG),
    )

    val commentFontSize = SliderSettingItem(
        context.getString(R.string.font_size),
        0.2f,
        3f,
        0.1f,
        relatedKeys = listOf(KEY_POST_AND_COMMENTS_UI_CONFIG),
    )

    val commentIndentationLevel = SliderSettingItem(
        context.getString(R.string.indentation_per_level),
        0f,
        32f,
        stepSize = 1f,
        relatedKeys = listOf(KEY_POST_AND_COMMENTS_UI_CONFIG),
    )

    val showCommentActions = OnOffSettingItem(
        null,
        context.getString(R.string.show_comment_actions),
        null,
        relatedKeys = listOf(KEY_POST_AND_COMMENTS_UI_CONFIG),
    )

    val fullBleedImage = OnOffSettingItem(
        null,
        context.getString(R.string.full_bleed_image),
        null,
        relatedKeys = listOf(KEY_POST_FULL_BLEED_IMAGE),
    )

    val commentsThreadStyle = RadioGroupSettingItem(
        null,
        context.getString(R.string.comments_thread_style),
        null,
        listOf(
            RadioGroupSettingItem.RadioGroupOption(
                CommentsThreadStyle.MODERN,
                context.getString(R.string.modern),
                null,
                null,
            ),
            RadioGroupSettingItem.RadioGroupOption(
                CommentsThreadStyle.LEGACY,
                context.getString(R.string.classic),
                null,
                null,
            ),
            RadioGroupSettingItem.RadioGroupOption(
                CommentsThreadStyle.LEGACY_WITH_COLORS,
                context.getString(R.string.classic_but_with_colors),
                null,
                null,
            ),
            RadioGroupSettingItem.RadioGroupOption(
                CommentsThreadStyle.LEGACY_WITH_COLORS_AND_DIVIDERS,
                context.getString(R.string.classic_but_with_colors_and_dividers),
                null,
                null,
            ),
        ),
        relatedKeys = listOf(KEY_POST_AND_COMMENTS_UI_CONFIG),
    )

    val tapCommentToCollapse = OnOffSettingItem(
        null,
        context.getString(R.string.tap_comment_to_collapse),
        context.getString(R.string.tap_comment_to_collapse_desc),
        relatedKeys = listOf(KEY_POST_AND_COMMENTS_UI_CONFIG),
    )

    val alwaysShowLinkBelowPost = OnOffSettingItem(
        null,
        context.getString(R.string.always_show_link_below_post),
        context.getString(R.string.always_show_link_below_post_desc),
        relatedKeys = listOf(KEY_POST_AND_COMMENTS_UI_CONFIG),
    )

    val useCondensedTypefaceForCommentHeader = OnOffSettingItem(
        icon = null,
        title = context.getString(R.string.use_condensed_style_for_comment_header),
        description = null,
        relatedKeys = listOf(KEY_USE_CONDENSED_FOR_COMMENT_HEADERS),
    )

    override val allSettings: List<SettingItem> = listOf(
        resetPostStyles,
        resetCommentStyles,
        postFontSize,
        commentFontSize,
        commentIndentationLevel,
        showCommentActions,
        commentsThreadStyle,
        tapCommentToCollapse,
        alwaysShowLinkBelowPost,
        useCondensedTypefaceForCommentHeader,
    )
}

class ThemeSettings @Inject constructor(
    @ActivityContext private val context: Context,
) : SearchableSettings {

    val baseTheme = RadioGroupSettingItem(
        0,
        context.getString(R.string.base_theme),
        null,
        listOf(
            RadioGroupSettingItem.RadioGroupOption(
                R.id.setting_option_use_system,
                context.getString(R.string.use_system_theme),
                null,
                R.drawable.baseline_auto_awesome_24,
            ),
            RadioGroupSettingItem.RadioGroupOption(
                R.id.setting_option_light_theme,
                context.getString(R.string.light_theme),
                null,
                R.drawable.baseline_light_mode_24,
            ),
            RadioGroupSettingItem.RadioGroupOption(
                R.id.setting_option_dark_theme,
                context.getString(R.string.dark_theme),
                null,
                R.drawable.baseline_dark_mode_24,
            ),
        ),
        relatedKeys = listOf(KEY_BASE_THEME),
    )

    val materialYou = OnOffSettingItem(
        null,
        context.getString(R.string.material_you),
        context.getString(R.string.personalized_theming_based_on_your_wallpaper),
        relatedKeys = listOf(KEY_USE_MATERIAL_YOU),
    )

    val colorScheme = RadioGroupSettingItem(
        null,
        context.getString(R.string.color_scheme),
        null,
        listOf(
            RadioGroupSettingItem.RadioGroupOption(
                ColorSchemes.Default,
                context.getString(R.string._default),
                null,
                null,
            ),
            RadioGroupSettingItem.RadioGroupOption(
                ColorSchemes.Blue,
                context.getString(R.string.blue),
                null,
                null,
            ),
            RadioGroupSettingItem.RadioGroupOption(
                ColorSchemes.Red,
                context.getString(R.string.red),
                null,
                null,
            ),
            RadioGroupSettingItem.RadioGroupOption(
                ColorSchemes.Blue,
                context.getString(R.string.blue),
                null,
                null,
            ),
            RadioGroupSettingItem.RadioGroupOption(
                ColorSchemes.TalhaPurple,
                context.getString(R.string.talha_e_purple),
                null,
                null,
            ),
            RadioGroupSettingItem.RadioGroupOption(
                ColorSchemes.TalhaGreen,
                context.getString(R.string.talha_e_green),
                null,
                null,
            ),
            RadioGroupSettingItem.RadioGroupOption(
                ColorSchemes.TalhaPink,
                context.getString(R.string.talha_e_pink),
                null,
                null,
            ),
            RadioGroupSettingItem.RadioGroupOption(
                ColorSchemes.Peachie,
                context.getString(R.string.peachie),
                null,
                null,
            ),
            RadioGroupSettingItem.RadioGroupOption(
                ColorSchemes.Fuchsia,
                context.getString(R.string.fuchsia),
                null,
                null,
            ),
            RadioGroupSettingItem.RadioGroupOption(
                ColorSchemes.Minty,
                context.getString(R.string.minty),
                null,
                null,
            ),
        ),
        relatedKeys = listOf(KEY_COLOR_SCHEME),
    )

    val blackTheme = OnOffSettingItem(
        null,
        context.getString(R.string.black_theme),
        context.getString(R.string.black_theme_desc),
        relatedKeys = listOf(KEY_USE_BLACK_THEME),
    )
    val lessDarkBackgroundTheme = OnOffSettingItem(
        null,
        context.getString(R.string.less_dark_background_theme),
        context.getString(R.string.less_dark_background_theme_desc),
        relatedKeys = listOf(KEY_USE_LESS_DARK_BACKGROUND),
    )

    val font = RadioGroupSettingItem(
        null,
        context.getString(R.string.font),
        null,
        listOf(
            RadioGroupSettingItem.RadioGroupOption(
                FontIds.DEFAULT,
                context.getString(R.string._default),
                null,
                null,
            ),
            RadioGroupSettingItem.RadioGroupOption(
                FontIds.ROBOTO,
                context.getString(R.string.roboto),
                null,
                null,
            ),
            RadioGroupSettingItem.RadioGroupOption(
                FontIds.ROBOTO_SERIF,
                context.getString(R.string.roboto_serif),
                null,
                null,
            ),
            RadioGroupSettingItem.RadioGroupOption(
                FontIds.OPEN_SANS,
                context.getString(R.string.open_sans),
                null,
                null,
            ),
        ),
        relatedKeys = listOf(KEY_GLOBAL_FONT),
    )

    val fontSize = TextOnlySettingItem(
        context.getString(R.string.font_size),
        "",
        relatedKeys = listOf(KEY_GLOBAL_FONT_SIZE),
    )

    val fontColor = TextOnlySettingItem(
        context.getString(R.string.font_color),
        "",
        relatedKeys = listOf(KEY_GLOBAL_FONT_COLOR),
    )

    val upvoteColor = ColorSettingItem(
        null,
        context.getString(R.string.upvote_color),
        context.getString(R.string.updoot_color),
        relatedKeys = listOf(KEY_UPVOTE_COLOR),
    )

    val downvoteColor = ColorSettingItem(
        null,
        context.getString(R.string.downvote_color),
        context.getString(R.string.downdoot_color),
        relatedKeys = listOf(KEY_DOWNVOTE_COLOR),
    )

    val transparentNotificationBar = OnOffSettingItem(
        null,
        context.getString(R.string.transparent_notification_bar),
        null,
        relatedKeys = listOf(KEY_TRANSPARENT_NOTIFICATION_BAR),
    )

    override val parents: List<KClass<out SearchableSettings>> = listOf(
        MainSettings::class,
    )

    override val allSettings: List<SettingItem> = listOf(
        baseTheme,
        colorScheme,
        blackTheme,
        font,
        fontSize,
        fontColor,
        upvoteColor,
        downvoteColor,
        transparentNotificationBar,
    )
}

class PostsFeedAppearanceSettings @Inject constructor(
    @ActivityContext private val context: Context,
) : SearchableSettings {

    val baseViewType = TextOnlySettingItem(
        context.getString(R.string.base_view_type),
        "",
    )

    val fontSize = SliderSettingItem(
        context.getString(R.string.font_size),
        0.2f,
        3f,
        0.1f,
    )

    val imageSize = SliderSettingItem(
        context.getString(R.string.image_size),
        0.1f,
        1f,
        0.05f,
    )

    val preferImageAtEnd = OnOffSettingItem(
        null,
        context.getString(R.string.prefer_image_at_the_end),
        null,
    )

    val preferFullImage = OnOffSettingItem(
        null,
        context.getString(R.string.prefer_full_size_image),
        null,
    )

    val preferTitleText = OnOffSettingItem(
        null,
        context.getString(R.string.prefer_title_text),
        null,
    )

    val preferCommunityIcon = OnOffSettingItem(
        null,
        context.getString(R.string.prefer_community_icon),
        null,
    )

    val contentMaxLines =
        TextOnlySettingItem(
            context.getString(R.string.full_content_max_lines),
            "",
        )
    val dimReadPosts = OnOffSettingItem(
        null,
        context.getString(R.string.dim_read_posts),
        context.getString(R.string.dim_read_posts_desc),
    )
    val preferTextPreviewIcon = OnOffSettingItem(
        null,
        context.getString(R.string.prefer_text_preview_icon),
        context.getString(R.string.prefer_text_preview_icon_desc),
    )

    val horizontalMarginSize = SliderSettingItem(
        context.getString(R.string.horizontal_margin_size),
        0f,
        32f,
        1f,
    )

    override val parents: List<KClass<out SearchableSettings>> = listOf(
        MainSettings::class,
        PostsFeedSettings::class,
    )

    override val allSettings: List<SettingItem> = listOf(
        baseViewType,
        fontSize,
        preferImageAtEnd,
        preferFullImage,
        preferTitleText,
        preferCommunityIcon,
        dimReadPosts,
        horizontalMarginSize,
    )
}

class AboutSettings @Inject constructor(
    @ActivityContext private val context: Context,
) : SearchableSettings {
    override val parents: List<KClass<out SearchableSettings>> = listOf(
        MainSettings::class,
    )
    val version = BasicSettingItem(
        null,
        context.getString(R.string.build_version, BuildConfig.VERSION_NAME),
        context.getString(R.string.version_code, BuildConfig.VERSION_CODE.toString()),
    )
    val googlePlayLink = BasicSettingItem(
        null,
        context.getString(R.string.view_on_the_play_store),
        null,
    )
    val giveFeedback = BasicSettingItem(
        null,
        context.getString(R.string.give_feedback),
        context.getString(R.string.give_feedback_desc),
    )
    val patreonSettings = BasicSettingItem(
        R.drawable.baseline_attach_money_24,
        context.getString(R.string.patreon_supporters),
        context.getString(R.string.patreon_supporters_desc),
    )
    val translatorsSettings = BasicSettingItem(
        R.drawable.baseline_translate_24,
        context.getString(R.string.translators),
        context.getString(R.string.translators_desc),
    )
    override val allSettings: List<SettingItem> = listOf(
        version,
        googlePlayLink,
        giveFeedback,
        patreonSettings,
        translatorsSettings,
    )
}

class CacheSettings @Inject constructor(
    @ActivityContext private val context: Context,
) : SearchableSettings {
    override val parents: List<KClass<out SearchableSettings>> = listOf(
        MainSettings::class,
    )
    val clearCache = BasicSettingItem(
        null,
        context.getString(R.string.clear_media_cache),
        null,
    )
    val cachePolicy = RadioGroupSettingItem(
        null,
        context.getString(R.string.cache_policy),
        context.getString(R.string.cache_policy_desc),
        listOf(
            RadioGroupSettingItem.RadioGroupOption(
                CachePolicy.Aggressive.value,
                context.getString(R.string.aggressive),
                null,
                null,
            ),
            RadioGroupSettingItem.RadioGroupOption(
                CachePolicy.Moderate.value,
                context.getString(R.string.moderate),
                null,
                null,
                isDefault = true,
            ),
            RadioGroupSettingItem.RadioGroupOption(
                CachePolicy.Lite.value,
                context.getString(R.string.lite),
                null,
                null,
            ),
            RadioGroupSettingItem.RadioGroupOption(
                CachePolicy.Minimum.value,
                context.getString(R.string.minimum),
                null,
                null,
            ),
        ),
        relatedKeys = listOf(KEY_CACHE_POLICY),
    )
    override val allSettings: List<SettingItem> = listOf(
        clearCache,
    )
}

class HiddenPostsSettings @Inject constructor(
    @ActivityContext private val context: Context,
) : SearchableSettings {
    override val parents: List<KClass<out SearchableSettings>> = listOf(
        MainSettings::class,
    )
    val enableHiddenPosts = OnOffSettingItem(
        null,
        context.getString(R.string.hide_posts),
        context.getString(R.string.hide_posts_desc),
        relatedKeys = listOf(KEY_ENABLE_HIDDEN_POSTS),
    )
    val resetHiddenPosts = BasicSettingItem(
        null,
        context.getString(R.string.reset_hidden_posts),
        null,
    )
    val hiddenPostsCount = BasicSettingItem(
        null,
        context.getString(R.string.hidden_posts_count),
        null,
    )
    val viewHiddenPosts = BasicSettingItem(
        null,
        context.getString(R.string.view_hidden_posts),
        null,
    )
    override val allSettings: List<SettingItem> = listOf(
        resetHiddenPosts,
        hiddenPostsCount,
        viewHiddenPosts,
    )
}

class HapticSettings @Inject constructor(
    @ActivityContext private val context: Context,
) : SearchableSettings {
    override val parents: List<KClass<out SearchableSettings>> = listOf(
        MainSettings::class,
    )
    val haptics = OnOffSettingItem(
        null,
        context.getString(R.string.vibration_and_haptics),
        null,
        relatedKeys = listOf(KEY_HAPTICS_ENABLED),
    )
    val moreHaptics = OnOffSettingItem(
        null,
        context.getString(R.string.more_haptics),
        context.getString(R.string.more_haptics_desc),
        relatedKeys = listOf(KEY_HAPTICS_ON_ACTIONS),
    )

    override val allSettings: List<SettingItem> = listOf(
        haptics,
    )
}

class MiscSettings @Inject constructor(
    @ActivityContext private val context: Context,
) : SearchableSettings {
    override val parents: List<KClass<out SearchableSettings>> = listOf(
        MainSettings::class,
    )
    val openLinksInExternalBrowser = OnOffSettingItem(
        null,
        context.getString(R.string.open_links_in_external_browser),
        context.getString(R.string.open_links_in_external_browser_desc),
        relatedKeys = listOf(KEY_OPEN_LINKS_IN_APP),
    )
    val autoLinkPhoneNumbers = OnOffSettingItem(
        null,
        context.getString(R.string.auto_convert_phone_numbers_to_links),
        null,
        relatedKeys = listOf(KEY_AUTO_LINK_PHONE_NUMBERS),
    )
    val autoLinkIpAddresses = OnOffSettingItem(
        null,
        context.getString(R.string.auto_convert_ip_addresses_to_links),
        null,
        relatedKeys = listOf(KEY_AUTO_LINK_IP_ADDRESSES),
    )
    val instanceNameStyle = RadioGroupSettingItem(
        null,
        context.getString(R.string.display_instance_names),
        context.getString(R.string.display_instance_names_desc),
        listOf(
            RadioGroupSettingItem.RadioGroupOption(
                DisplayInstanceOptions.NeverDisplayInstance,
                context.getString(R.string.never),
                null,
                null,
            ),
            RadioGroupSettingItem.RadioGroupOption(
                DisplayInstanceOptions.OnlyDisplayNonLocalInstances,
                context.getString(R.string.only_for_different_instances),
                null,
                null,
            ),
            RadioGroupSettingItem.RadioGroupOption(
                DisplayInstanceOptions.AlwaysDisplayInstance,
                context.getString(R.string.always),
                null,
                null,
            ),
        ),
        relatedKeys = listOf(KEY_DISPLAY_INSTANCE_STYLE),
    )

    val retainLastPost = OnOffSettingItem(
        null,
        context.getString(R.string.retain_last_post),
        context.getString(R.string.retain_last_post_desc),
        relatedKeys = listOf(KEY_RETAIN_LAST_POST),
    )

    val leftHandMode = OnOffSettingItem(
        null,
        context.getString(R.string.left_hand_mode),
        context.getString(R.string.left_hand_mode_desc),
        relatedKeys = listOf(KEY_LEFT_HAND_MODE),
    )

    val previewLinks = RadioGroupSettingItem(
        null,
        context.getString(R.string.preview_links),
        context.getString(R.string.preview_links_desc),
        listOf(
            RadioGroupSettingItem.RadioGroupOption(
                PreviewLinkOptions.PreviewTextLinks,
                context.getString(R.string.preview_text_links),
                null,
                null,
            ),
            RadioGroupSettingItem.RadioGroupOption(
                PreviewLinkOptions.PreviewNoLinks,
                context.getString(R.string.dont_preview_links),
                null,
                null,
            ),
            RadioGroupSettingItem.RadioGroupOption(
                PreviewLinkOptions.PreviewAllLinks,
                context.getString(R.string.preview_all_links),
                null,
                null,
            ),
        ),
        relatedKeys = listOf(KEY_PREVIEW_LINKS),
    )

    val usePredictiveBack = OnOffSettingItem(
        null,
        context.getString(R.string.use_predictive_back),
        context.getString(R.string.use_predictive_back_desc),
        relatedKeys = listOf(KEY_USE_PREDICTIVE_BACK),
    )

    val warnReplyToOldContentThresholdMs = RadioGroupSettingItem(
        null,
        context.getString(R.string.warn_when_replying_to_old_post_or_comment),
        context.getString(R.string.warn_when_replying_to_old_post_or_comment_desc),
        listOf(
            RadioGroupSettingItem.RadioGroupOption(
                R.id.warn_reply_to_old_dont_warn,
                context.getString(R.string.dont_warn),
                null,
                null,
            ),
            RadioGroupSettingItem.RadioGroupOption(
                R.id.warn_reply_to_old_1_day,
                context.resources.getQuantityString(R.plurals.day_format, 1, "1"),
                null,
                null,
            ),
            RadioGroupSettingItem.RadioGroupOption(
                R.id.warn_reply_to_old_2_day,
                context.resources.getQuantityString(R.plurals.day_format, 2, "2"),
                null,
                null,
            ),
            RadioGroupSettingItem.RadioGroupOption(
                R.id.warn_reply_to_old_3_day,
                context.resources.getQuantityString(R.plurals.day_format, 3, "3"),
                null,
                null,
            ),
            RadioGroupSettingItem.RadioGroupOption(
                R.id.warn_reply_to_old_4_day,
                context.resources.getQuantityString(R.plurals.day_format, 4, "4"),
                null,
                null,
            ),
            RadioGroupSettingItem.RadioGroupOption(
                R.id.warn_reply_to_old_5_day,
                context.resources.getQuantityString(R.plurals.day_format, 5, "5"),
                null,
                null,
            ),
            RadioGroupSettingItem.RadioGroupOption(
                R.id.warn_reply_to_old_week,
                context.getString(R.string.a_week),
                null,
                null,
            ),
            RadioGroupSettingItem.RadioGroupOption(
                R.id.warn_reply_to_old_month,
                context.getString(R.string.a_month),
                null,
                null,
            ),
            RadioGroupSettingItem.RadioGroupOption(
                R.id.warn_reply_to_old_year,
                context.getString(R.string.a_year),
                null,
                null,
            ),
        ),
        relatedKeys = listOf(
            KEY_WARN_REPLY_TO_OLD_CONTENT,
            KEY_WARN_REPLY_TO_OLD_CONTENT_THRESHOLD_MS,
        ),
    )

    val indicatePostsAndCommentsCreatedByCurrentUser = OnOffSettingItem(
        null,
        context.getString(R.string.indicate_posts_and_comments_created_by_current_user),
        null,
        relatedKeys = listOf(KEY_INDICATE_CONTENT_FROM_CURRENT_USER),
    )

    val saveDraftsAutomatically = OnOffSettingItem(
        null,
        context.getString(R.string.save_drafts_automatically),
        context.getString(R.string.save_drafts_automatically_desc),
        relatedKeys = listOf(KEY_SAVE_DRAFTS_AUTOMATICALLY),
    )

    val perCommunitySettings = BasicSettingItem(
        null,
        context.getString(R.string.per_community_settings),
        context.getString(R.string.per_community_settings_desc),
    )

    val largeScreenSupport = OnOffSettingItem(
        null,
        context.getString(R.string.large_screen_support),
        context.getString(R.string.large_screen_support_desc),
        relatedKeys = listOf(KEY_GLOBAL_LAYOUT_MODE),
    )

    val rotateInstanceOnUploadFail = OnOffSettingItem(
        null,
        "Beep boop",
        "Beep",
        relatedKeys = listOf(KEY_ROTATE_INSTANCE_ON_UPLOAD_FAIL),
    )
    val showEditedDate = OnOffSettingItem(
        null,
        context.getString(R.string.show_edited_time),
        context.getString(R.string.show_edited_time_desc),
        relatedKeys = listOf(KEY_SHOW_EDITED_DATE),
    )
    val imagePreviewHideUiByDefault = OnOffSettingItem(
        null,
        context.getString(R.string.image_preview_hide_ui_by_default),
        null,
        relatedKeys = listOf(KEY_IMAGE_PREVIEW_HIDE_UI_BY_DEFAULT),
    )
    val uploadImagesToImgur = OnOffSettingItem(
        null,
        context.getString(R.string.use_imgur),
        context.getString(R.string.use_imgur_desc),
        relatedKeys = listOf(KEY_UPLOAD_IMAGES_TO_IMGUR),
    )
    val animationLevel = RadioGroupSettingItem(
        null,
        context.getString(R.string.animation_level),
        context.getString(R.string.animation_level_misc),
        listOf(
            RadioGroupSettingItem.RadioGroupOption(
                R.id.animation_level_min,
                context.getString(R.string.minimum),
                null,
                null,
            ),
            RadioGroupSettingItem.RadioGroupOption(
                R.id.animation_level_low,
                context.getString(R.string.low),
                null,
                null,
            ),
            RadioGroupSettingItem.RadioGroupOption(
                R.id.animation_level_max,
                context.getString(R.string.max),
                null,
                null,
            ),
        ),
        relatedKeys = listOf(
            KEY_ANIMATION_LEVEL,
        ),
    )
    val shakeToSendFeedback = OnOffSettingItem(
        null,
        context.getString(R.string.shake_to_send_feedback),
        null,
        relatedKeys = listOf(KEY_SHAKE_TO_SEND_FEEDBACK),
    )
    val showLabelsInNavBar = OnOffSettingItem(
        null,
        context.getString(R.string.show_labels_in_navigation_bar),
        null,
        relatedKeys = listOf(KEY_SHOW_LABELS_IN_NAV_BAR),
    )
    val showNewPersonWarning = OnOffSettingItem(
        null,
        context.getString(R.string.show_new_person_tag),
        context.getString(R.string.show_new_person_tag_desc),
        relatedKeys = listOf(KEY_WARN_NEW_PERSON),
    )
    val preferredLocale = BasicSettingItem(
        null,
        context.getString(R.string.preferred_locale),
        null,
        relatedKeys = listOf(KEY_PREFERRED_LOCALE),
    )
    val communitySelectorShowCommunitySuggestions = OnOffSettingItem(
        null,
        context.getString(R.string.show_community_suggestions_in_community_selector),
        context.getString(R.string.show_community_suggestions_in_community_selector_desc),
        relatedKeys = listOf(KEY_COMMUNITY_SELECTOR_SHOW_COMMUNITY_SUGGESTIONS),
    )
    val userAgentChoice = RadioGroupSettingItem(
        null,
        context.getString(R.string.user_agent_choice),
        context.getString(R.string.user_agent_choice_desc),
        listOf(
            RadioGroupSettingItem.RadioGroupOption(
                UserAgentChoiceIds.UNSET,
                context.getString(R.string.let_the_app_decide),
                null,
                null,
            ),
            RadioGroupSettingItem.RadioGroupOption(
                UserAgentChoiceIds.LEGACY_USER_AGENT,
                context.getString(R.string.use_legacy_user_agent),
                null,
                null,
            ),
            RadioGroupSettingItem.RadioGroupOption(
                UserAgentChoiceIds.NEW_USER_AGENT,
                context.getString(R.string.use_test_user_agent),
                null,
                null,
            ),
            RadioGroupSettingItem.RadioGroupOption(
                UserAgentChoiceIds.NEW_USER_AGENT_2,
                context.getString(R.string.use_test_user_agent_2),
                null,
                null,
            ),
            RadioGroupSettingItem.RadioGroupOption(
                UserAgentChoiceIds.OKHTTP_USER_AGENT,
                context.getString(R.string.use_okhttp),
                null,
                null,
            ),
            RadioGroupSettingItem.RadioGroupOption(
                UserAgentChoiceIds.FLUTTER_USER_AGENT,
                context.getString(R.string.use_flutter),
                null,
                null,
            ),
        ),
        relatedKeys = listOf(
            KEY_ANIMATION_LEVEL,
        ),
    )

    override val allSettings: List<SettingItem> = listOf(
        openLinksInExternalBrowser,
        autoLinkPhoneNumbers,
        retainLastPost,
        leftHandMode,
        previewLinks,
        usePredictiveBack,
        perCommunitySettings,
        indicatePostsAndCommentsCreatedByCurrentUser,
        warnReplyToOldContentThresholdMs,
        saveDraftsAutomatically,
        largeScreenSupport,
        showEditedDate,
        imagePreviewHideUiByDefault,
        rotateInstanceOnUploadFail,
        uploadImagesToImgur,
        animationLevel,
        shakeToSendFeedback,
        showLabelsInNavBar,
        showNewPersonWarning,
        preferredLocale,
        communitySelectorShowCommunitySuggestions,
        userAgentChoice,
    )
}

/**
 * User actions.
 */
class ActionsSettings @Inject constructor(
    @ActivityContext private val context: Context,
) : SearchableSettings {
    override val parents: List<KClass<out SearchableSettings>> = listOf(
        MainSettings::class,
    )
    override val allSettings: List<SettingItem> = listOf()
}

class LoggingSettings @Inject constructor(
    @ActivityContext private val context: Context,
) : SearchableSettings {

    val useFirebase = OnOffSettingItem(
        null,
        context.getString(R.string.use_crash_logger),
        context.getString(R.string.use_crash_logger_desc),
        relatedKeys = listOf(KEY_USE_FIREBASE),
    )

    override val parents: List<KClass<out SearchableSettings>> = listOf(
        MainSettings::class,
    )

    override val allSettings: List<SettingItem> = listOf(
        useFirebase,
    )
}

class HistorySettings @Inject constructor(
    @ActivityContext private val context: Context,
) : SearchableSettings {

    val viewHistory = BasicSettingItem(
        null,
        context.getString(R.string.view_history),
        null,
    )

    val recordBrowsingHistory = OnOffSettingItem(
        null,
        context.getString(R.string.record_browsing_history),
        context.getString(R.string.record_browsing_history_desc),
        relatedKeys = listOf(KEY_TRACK_BROWSING_HISTORY),
    )

    override val parents: List<KClass<out SearchableSettings>> = listOf(
        MainSettings::class,
    )

    override val allSettings: List<SettingItem> = listOf(
        recordBrowsingHistory,
    )
}

class NavigationSettings @Inject constructor(
    @ActivityContext private val context: Context,
) : SearchableSettings {

    private val navBarDestOptions =
        listOf(
            RadioGroupSettingItem.RadioGroupOption(
                NavBarDestinations.Home,
                context.getString(R.string.home),
                null,
                null,
            ),
            RadioGroupSettingItem.RadioGroupOption(
                NavBarDestinations.History,
                context.getString(R.string.history),
                null,
                null,
            ),
            RadioGroupSettingItem.RadioGroupOption(
                NavBarDestinations.Inbox,
                context.getString(R.string.inbox),
                null,
                null,
            ),
            RadioGroupSettingItem.RadioGroupOption(
                NavBarDestinations.Saved,
                context.getString(R.string.save),
                null,
                null,
            ),
            RadioGroupSettingItem.RadioGroupOption(
                NavBarDestinations.Search,
                context.getString(R.string.search),
                null,
                null,
            ),
            RadioGroupSettingItem.RadioGroupOption(
                NavBarDestinations.Profile,
                context.getString(R.string.user_profile),
                null,
                null,
            ),
            RadioGroupSettingItem.RadioGroupOption(
                NavBarDestinations.You,
                context.getString(R.string.you),
                null,
                null,
            ),
            RadioGroupSettingItem.RadioGroupOption(
                NavBarDestinations.None,
                context.getString(R.string.none),
                null,
                null,
            ),
        )

    val useCustomNavBar = OnOffSettingItem(
        null,
        context.getString(R.string.use_custom_navigation_bar),
        context.getString(R.string.use_custom_navigation_bar_desc),
        relatedKeys = listOf(KEY_USE_CUSTOM_NAV_BAR),
    )

    val navBarDest1 = RadioGroupSettingItem(
        null,
        context.getString(R.string.nav_bar_option_format, "1"),
        null,
        navBarDestOptions,
        relatedKeys = listOf(KEY_NAV_BAR_ITEMS),
    )

    val navBarDest2 = RadioGroupSettingItem(
        null,
        context.getString(R.string.nav_bar_option_format, "2"),
        null,
        navBarDestOptions,
        relatedKeys = listOf(KEY_NAV_BAR_ITEMS),
    )

    val navBarDest3 = RadioGroupSettingItem(
        null,
        context.getString(R.string.nav_bar_option_format, "3"),
        null,
        navBarDestOptions,
        relatedKeys = listOf(KEY_NAV_BAR_ITEMS),
    )

    val navBarDest4 = RadioGroupSettingItem(
        null,
        context.getString(R.string.nav_bar_option_format, "4"),
        null,
        navBarDestOptions,
        relatedKeys = listOf(KEY_NAV_BAR_ITEMS),
    )

    val navBarDest5 = RadioGroupSettingItem(
        null,
        context.getString(R.string.nav_bar_option_format, "5"),
        null,
        navBarDestOptions,
        relatedKeys = listOf(KEY_NAV_BAR_ITEMS),
    )

    val useBottomNavBar = OnOffSettingItem(
        null,
        context.getString(R.string.use_bottom_nav_bar),
        context.getString(R.string.use_bottom_nav_bar_desc),
        relatedKeys = listOf(KEY_USE_BOTTOM_NAV_BAR),
    )

    val navigationRailMode = RadioGroupSettingItem(
        null,
        context.getString(R.string.navigation_rail_mode),
        context.getString(R.string.navigation_rail_mode_desc),
        listOf(
            RadioGroupSettingItem.RadioGroupOption(
                NavigationRailModeId.Auto,
                context.getString(R.string.auto),
                null,
                null,
            ),
            RadioGroupSettingItem.RadioGroupOption(
                NavigationRailModeId.ManualOn,
                context.getString(R.string.on),
                null,
                null,
            ),
            RadioGroupSettingItem.RadioGroupOption(
                NavigationRailModeId.ManualOff,
                context.getString(R.string.off),
                null,
                null,
            ),
        ),
        relatedKeys = listOf(
            PreferenceUtils.KEY_NAVIGATION_RAIL_MODE,
        ),
    )

    override val parents: List<KClass<out SearchableSettings>> = listOf(
        MainSettings::class,
    )

    override val allSettings: List<SettingItem> = listOf(
        navBarDest1,
        navBarDest2,
        navBarDest3,
        navBarDest4,
        navBarDest5,
        useBottomNavBar,
        useCustomNavBar,
        navigationRailMode,
    )
}

class ImportAndExportSettings @Inject constructor(
    @ActivityContext private val context: Context,
) : SearchableSettings {

    val importSettings = BasicSettingItem(
        null,
        context.getString(R.string.restore_settings),
        context.getString(R.string.restore_settings_desc),
    )

    val exportSettings = BasicSettingItem(
        null,
        context.getString(R.string.backup_settings),
        context.getString(R.string.backup_settings_desc),
    )

    val resetSettingsWithBackup = BasicSettingItem(
        null,
        context.getString(R.string.reset_settings_with_backup),
        context.getString(R.string.reset_settings_with_backup_desc),
    )

    val manageInternalSettingsBackups = BasicSettingItem(
        null,
        context.getString(R.string.manage_internal_settings_backups),
        null,
    )

    val viewCurrentSettings = BasicSettingItem(
        null,
        context.getString(R.string.view_current_settings),
        null,
    )

    override val parents: List<KClass<out SearchableSettings>> = listOf(
        MainSettings::class,
    )

    override val allSettings: List<SettingItem> = listOf(
        importSettings,
        exportSettings,
        resetSettingsWithBackup,
        manageInternalSettingsBackups,
    )
}

class PerAccountSettings @Inject constructor(
    @ActivityContext private val context: Context,
) : SearchableSettings {

    val settingTheme = BasicSettingItem(
        R.drawable.baseline_palette_24,
        context.getString(R.string.theme),
        context.getString(R.string.theme_settings_desc),
    )
    val settingViewType = BasicSettingItem(
        R.drawable.baseline_view_agenda_24,
        context.getString(R.string.post_appearance),
        context.getString(R.string.view_type_settings_desc),
    )
    val settingPostAndComment = BasicSettingItem(
        R.drawable.baseline_mode_comment_24,
        context.getString(R.string.post_and_comments_appearance),
        context.getString(R.string.post_and_comments_settings_desc),
    )
    val settingAccount = BasicSettingItem(
        R.drawable.outline_account_circle_24,
        context.getString(R.string.summit_account_settings),
        context.getString(R.string.summit_account_settings_desc),
    )
    val settingGestures = BasicSettingItem(
        R.drawable.baseline_gesture_24,
        context.getString(R.string.gestures),
        context.getString(R.string.gestures_desc),
    )
    val settingCache = BasicSettingItem(
        R.drawable.baseline_cached_24,
        context.getString(R.string.cache),
        context.getString(R.string.cache_info_and_preferences),
    )
    val settingHiddenPosts = BasicSettingItem(
        R.drawable.baseline_hide_24,
        context.getString(R.string.hidden_posts),
        context.getString(R.string.hidden_posts_desc),
    )
    val settingPostList = BasicSettingItem(
        R.drawable.baseline_pages_24,
        context.getString(R.string.post_list),
        context.getString(R.string.setting_post_list_desc),
    )
    val settingAbout = BasicSettingItem(
        R.drawable.outline_info_24,
        context.getString(R.string.about_summit),
        context.getString(R.string.about_summit_desc),
    )
    val settingSummitCommunity = BasicSettingItem(
        R.drawable.ic_logo_mono_24,
        context.getString(R.string.c_summit),
        context.getString(R.string.summit_community_desc),
    )

    val commentListSettings = BasicSettingItem(
        R.drawable.baseline_comment_24,
        context.getString(R.string.comment_list),
        context.getString(R.string.comment_list_desc),
    )
    val miscSettings = BasicSettingItem(
        R.drawable.baseline_miscellaneous_services_24,
        context.getString(R.string.misc),
        context.getString(R.string.misc_desc),
    )
    val loggingSettings = BasicSettingItem(
        R.drawable.outline_analytics_24,
        context.getString(R.string.logging),
        context.getString(R.string.logging_desc),
    )
    val historySettings = BasicSettingItem(
        R.drawable.baseline_history_24,
        context.getString(R.string.history),
        context.getString(R.string.history_desc),
    )
    val navigationSettings = BasicSettingItem(
        R.drawable.outline_navigation_24,
        context.getString(R.string.navigation),
        context.getString(R.string.navigation_desc),
    )
    val userActionsSettings = BasicSettingItem(
        R.drawable.outline_play_arrow_24,
        context.getString(R.string.user_actions),
        context.getString(R.string.user_actions_desc),
    )
    val backupAndRestoreSettings = BasicSettingItem(
        R.drawable.baseline_import_export_24,
        context.getString(R.string.backup_and_restore_settings),
        context.getString(R.string.backup_and_restore_settings_desc),
    )
    val manageSettings = BasicSettingItem(
        R.drawable.outline_settings_applications_24,
        context.getString(R.string.manage_settings),
        context.getString(R.string.manage_settings_desc),
    )
    val desc = DescriptionSettingItem(
        "",
        context.getString(R.string.per_account_settings_desc),
    )

    override val parents: List<KClass<out SearchableSettings>> = listOf(
        MainSettings::class,
    )

    override val allSettings: List<SettingItem> = listOf(
        desc,
        settingTheme,
        settingPostList,
        manageSettings,
    )
}

class DownloadSettings @Inject constructor(
    @ActivityContext private val context: Context,
) : SearchableSettings {

    val downloadDirectory = TextValueSettingItem(
        title = context.getString(R.string.download_location),
        description = null,
        supportsRichText = false,
        relatedKeys = listOf(KEY_DOWNLOAD_DIRECTORY),
    )

    val resetDownloadDirectory = BasicSettingItem(
        icon = null,
        title = context.getString(R.string.reset_download_location),
        description = null,
        relatedKeys = listOf(KEY_DOWNLOAD_DIRECTORY),
    )

    override val parents: List<KClass<out SearchableSettings>> = listOf(
        MainSettings::class,
    )

    override val allSettings: List<SettingItem> = listOf(
        downloadDirectory,
        resetDownloadDirectory,
    )
}

class PerCommunitySettings @Inject constructor(
    @ActivityContext private val context: Context,
) : SearchableSettings {

    val usePerCommunitySettings = OnOffSettingItem(
        null,
        context.getString(R.string.use_per_community_settings),
        null,
        relatedKeys = listOf(KEY_USE_PER_COMMUNITY_SETTINGS),
    )

    val clearPerCommunitySettings = BasicSettingItem(
        icon = null,
        title = context.getString(R.string.clear_per_community_settings),
        description = null,
    )

    override val parents: List<KClass<out SearchableSettings>> = listOf(
        MainSettings::class,
        MiscSettings::class,
    )

    override val allSettings: List<SettingItem> = listOf(
        usePerCommunitySettings,
        clearPerCommunitySettings,
    )
}

class NotificationSettings @Inject constructor(
    @ActivityContext private val context: Context,
) : SearchableSettings {

    private val refreshIntervalOptions =
        listOf(
            RadioGroupSettingItem.RadioGroupOption(
                R.id.refresh_interval_15_minutes,
                context.getString(R.string.every_minutes_format, "15"),
                null,
                null,
            ),
            RadioGroupSettingItem.RadioGroupOption(
                R.id.refresh_interval_30_minutes,
                context.getString(R.string.every_minutes_format, "30"),
                null,
                null,
            ),
            RadioGroupSettingItem.RadioGroupOption(
                R.id.refresh_interval_60_minutes,
                context.getString(R.string.every_minutes_format, "60"),
                null,
                null,
            ),
            RadioGroupSettingItem.RadioGroupOption(
                R.id.refresh_interval_2_hours,
                context.getString(R.string.every_hours_format, "2"),
                null,
                null,
            ),
            RadioGroupSettingItem.RadioGroupOption(
                R.id.refresh_interval_4_hours,
                context.getString(R.string.every_hours_format, "4"),
                null,
                null,
            ),
            RadioGroupSettingItem.RadioGroupOption(
                R.id.refresh_interval_12_hours,
                context.getString(R.string.every_hours_format, "12"),
                null,
                null,
            ),
        )

    val isNotificationsEnabled = OnOffSettingItem(
        null,
        context.getString(R.string.notifications),
        null,
        relatedKeys = listOf(KEY_IS_NOTIFICATIONS_ON),
    )

    val checkInterval = RadioGroupSettingItem(
        null,
        context.getString(R.string.check_interval),
        null,
        refreshIntervalOptions,
        relatedKeys = listOf(KEY_NOTIFICATIONS_CHECK_INTERVAL_MS),
    )

    override val parents: List<KClass<out SearchableSettings>> = listOf(
        MainSettings::class,
    )

    override val allSettings: List<SettingItem> = listOf(
        isNotificationsEnabled,
        checkInterval,
    )
}

class SearchHomeSettings @Inject constructor(
    @ActivityContext private val context: Context,
) : SearchableSettings {

    val searchSuggestions = OnOffSettingItem(
        null,
        context.getString(R.string.search_suggestions),
        null,
        relatedKeys = listOf(KEY_SEARCH_HOME_CONFIG),
    )

    val subscribedCommunities = OnOffSettingItem(
        null,
        context.getString(R.string.subscribed_communities),
        null,
        relatedKeys = listOf(KEY_SEARCH_HOME_CONFIG),
    )

    val topCommunities = OnOffSettingItem(
        null,
        context.getString(R.string.top_communities_this_week),
        null,
        relatedKeys = listOf(KEY_SEARCH_HOME_CONFIG),
    )

    val trendingCommunities = OnOffSettingItem(
        null,
        context.getString(R.string.trending_communities),
        null,
        relatedKeys = listOf(KEY_SEARCH_HOME_CONFIG),
    )

    val risingCommunities = OnOffSettingItem(
        null,
        context.getString(R.string.rising_communities),
        null,
        relatedKeys = listOf(KEY_SEARCH_HOME_CONFIG),
    )

    override val parents: List<KClass<out SearchableSettings>> = listOf(
        MainSettings::class,
    )

    override val allSettings: List<SettingItem> = listOf(
        searchSuggestions,
        subscribedCommunities,
        topCommunities,
        trendingCommunities,
        risingCommunities,
    )
}

class VideoPlayerSettings @Inject constructor(
    @ActivityContext private val context: Context,
) : SearchableSettings {

    val inlineVideoVolume = SliderSettingItem(
        context.getString(R.string.default_volume),
        0f,
        1f,
        null,
    )
    val autoPlayVideos = OnOffSettingItem(
        null,
        context.getString(R.string.auto_play_videos),
        null,
        relatedKeys = listOf(KEY_AUTO_PLAY_VIDEOS),
    )

    override val parents: List<KClass<out SearchableSettings>> = listOf(
        MainSettings::class,
    )

    override val allSettings: List<SettingItem> = listOf(
        inlineVideoVolume,
        autoPlayVideos,
    )
}

class DefaultAppsSettings @Inject constructor(
    @ActivityContext private val context: Context,
) : SearchableSettings {

    val defaultWebApp = BasicSettingItem(
        null,
        context.getString(R.string.default_web_app),
        null,
    )

    override val parents: List<KClass<out SearchableSettings>> = listOf(
        MainSettings::class,
    )

    override val allSettings: List<SettingItem> = listOf(
        defaultWebApp,
    )
}

class AllSettings @Inject constructor(
    @ActivityContext private val context: Context,
    mainSettings: MainSettings,
    lemmyWebSettings: LemmyWebSettings,
    gestureSettings: GestureSettings,
    postsFeedSettings: PostsFeedSettings,
    postAndCommentsSettings: PostAndCommentsSettings,
    aboutSettings: AboutSettings,
    cacheSettings: CacheSettings,
    hiddenPostsSettings: HiddenPostsSettings,
    postAndCommentsAppearanceSettings: PostAndCommentsAppearanceSettings,
    themeSettings: ThemeSettings,
    postsFeedAppearanceSettings: PostsFeedAppearanceSettings,
    miscSettings: MiscSettings,
    loggingSettings: LoggingSettings,
    historySettings: HistorySettings,
    navigationSettings: NavigationSettings,
    actionsSettings: ActionsSettings,
    importAndExportSettings: ImportAndExportSettings,
    perAccountSettings: PerAccountSettings,
    downloadSettings: DownloadSettings,
    perCommunitySettings: PerCommunitySettings,
    notificationSettings: NotificationSettings,
    searchHomeSettings: SearchHomeSettings,
    hapticSettings: HapticSettings,
    videoPlayerSettings: VideoPlayerSettings,
    defaultAppsSettings: DefaultAppsSettings,
) {
    val allSearchableSettings: List<SearchableSettings> = listOf(
        mainSettings,
        lemmyWebSettings,
        gestureSettings,
        postsFeedSettings,
        postAndCommentsSettings,
        aboutSettings,
        cacheSettings,
        hiddenPostsSettings,
        postAndCommentsAppearanceSettings,
        themeSettings,
        postsFeedAppearanceSettings,
        miscSettings,
        loggingSettings,
        historySettings,
        navigationSettings,
        actionsSettings,
        importAndExportSettings,
        perAccountSettings,
        downloadSettings,
        perCommunitySettings,
        notificationSettings,
        searchHomeSettings,
        hapticSettings,
        videoPlayerSettings,
        defaultAppsSettings,
    )

    init {
        if (BuildConfig.DEBUG) {
            val classes: MutableSet<KClass<*>> =
                SearchableSettings::class.sealedSubclasses.toMutableSet()
            allSearchableSettings.forEach {
                classes.remove(it::class)
            }

            assert(classes.isEmpty()) {
                "Some setting pages not added: $classes"
            }

            for (c in SearchableSettings::class.sealedSubclasses) {
                c.getPageName(context)
            }
        }
    }

    fun generateMapFromKeysToRelatedSettingItems(): MutableMap<String, MutableList<SettingItem>> {
        val keyToSettingItems = mutableMapOf<String, MutableList<SettingItem>>()

        allSearchableSettings.forEach {
            it.allSettings.forEach { settingItem ->
                for (key in settingItem.relatedKeys) {
                    val list = keyToSettingItems.getOrPut(key) { mutableListOf() }
                    list.add(settingItem)
                }
            }
        }
        return keyToSettingItems
    }
}

fun makeCommunitySortOrderChoices(context: Context) = listOf(
    RadioGroupSettingItem.RadioGroupOption(
        R.id.sort_order_active,
        context.getString(R.string.sort_order_active),
        null,
        null,
    ),
    RadioGroupSettingItem.RadioGroupOption(
        R.id.sort_order_hot,
        context.getString(R.string.sort_order_hot),
        null,
        null,
    ),
    RadioGroupSettingItem.RadioGroupOption(
        R.id.sort_order_new,
        context.getString(R.string.sort_order_new),
        null,
        null,
    ),
    RadioGroupSettingItem.RadioGroupOption(
        R.id.sort_order_old,
        context.getString(R.string.sort_order_old),
        null,
        null,
    ),
    RadioGroupSettingItem.RadioGroupOption(
        R.id.sort_order_most_comments,
        context.getString(R.string.sort_order_most_comments),
        null,
        null,
    ),
    RadioGroupSettingItem.RadioGroupOption(
        R.id.sort_order_new_comments,
        context.getString(R.string.sort_order_new_comments),
        null,
        null,
    ),
    RadioGroupSettingItem.RadioGroupOption(
        R.id.sort_order_controversial,
        context.getString(R.string.sort_order_controversial),
        null,
        null,
    ),
    RadioGroupSettingItem.RadioGroupOption(
        R.id.sort_order_scaled,
        context.getString(R.string.sort_order_scaled),
        null,
        null,
    ),

    RadioGroupSettingItem.RadioGroupOption(
        R.id.sort_order_top_last_hour,
        context.getString(
            R.string.sort_order_top_format,
            context.getString(R.string.time_frame_last_hour),
        ),
        null,
        null,
    ),
    RadioGroupSettingItem.RadioGroupOption(
        R.id.sort_order_top_last_six_hour,
        context.getString(
            R.string.sort_order_top_format,
            context.getString(R.string.time_frame_last_hours_format, "6"),
        ),
        null,
        null,
    ),
    RadioGroupSettingItem.RadioGroupOption(
        R.id.sort_order_top_last_twelve_hour,
        context.getString(
            R.string.sort_order_top_format,
            context.getString(R.string.time_frame_last_hours_format, "12"),
        ),
        null,
        null,
    ),
    RadioGroupSettingItem.RadioGroupOption(
        R.id.sort_order_top_day,
        context.getString(
            R.string.sort_order_top_format,
            context.getString(R.string.time_frame_today),
        ),
        null,
        null,
    ),
    RadioGroupSettingItem.RadioGroupOption(
        R.id.sort_order_top_week,
        context.getString(
            R.string.sort_order_top_format,
            context.getString(R.string.time_frame_this_week),
        ),
        null,
        null,
    ),
    RadioGroupSettingItem.RadioGroupOption(
        R.id.sort_order_top_month,
        context.getString(
            R.string.sort_order_top_format,
            context.getString(R.string.time_frame_this_month),
        ),
        null,
        null,
    ),
    RadioGroupSettingItem.RadioGroupOption(
        R.id.sort_order_top_last_three_month,
        context.getString(
            R.string.sort_order_top_format,
            context.getString(R.string.time_frame_last_months_format, "3"),
        ),
        null,
        null,
    ),
    RadioGroupSettingItem.RadioGroupOption(
        R.id.sort_order_top_last_six_month,
        context.getString(
            R.string.sort_order_top_format,
            context.getString(R.string.time_frame_last_months_format, "6"),
        ),
        null,
        null,
    ),
    RadioGroupSettingItem.RadioGroupOption(
        R.id.sort_order_top_last_nine_month,
        context.getString(
            R.string.sort_order_top_format,
            context.getString(R.string.time_frame_last_months_format, "9"),
        ),
        null,
        null,
    ),
    RadioGroupSettingItem.RadioGroupOption(
        R.id.sort_order_top_year,
        context.getString(
            R.string.sort_order_top_format,
            context.getString(R.string.time_frame_this_year),
        ),
        null,
        null,
    ),
    RadioGroupSettingItem.RadioGroupOption(
        R.id.sort_order_top_all_time,
        context.getString(
            R.string.sort_order_top_format,
            context.getString(R.string.time_frame_all_time),
        ),
        null,
        null,
    ),
)
