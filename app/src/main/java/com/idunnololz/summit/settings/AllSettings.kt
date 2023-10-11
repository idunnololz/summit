package com.idunnololz.summit.settings

import android.content.Context
import com.idunnololz.summit.BuildConfig
import com.idunnololz.summit.R
import com.idunnololz.summit.links.PreviewLinkOptions
import com.idunnololz.summit.preferences.ColorSchemes
import com.idunnololz.summit.preferences.CommentGestureAction
import com.idunnololz.summit.preferences.CommentsThreadStyle
import com.idunnololz.summit.preferences.FontIds
import com.idunnololz.summit.preferences.PostGestureAction
import com.idunnololz.summit.settings.misc.DisplayInstanceOptions
import com.idunnololz.summit.settings.navigation.NavBarDestinations
import com.idunnololz.summit.util.PrettyPrintUtils
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
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
            CommentListSettings::class ->
                context.getString(R.string.comment_list)
            GestureSettings::class ->
                context.getString(R.string.gestures)
            HiddenPostsSettings::class ->
                context.getString(R.string.hidden_posts)
            LemmyWebSettings::class ->
                context.getString(R.string.lemmy_web_preferences)
            MainSettings::class ->
                context.getString(R.string.settings)
            PostAndCommentsSettings::class ->
                context.getString(R.string.post_and_comments)
            PostListSettings::class ->
                context.getString(R.string.post_list)
            ThemeSettings::class ->
                context.getString(R.string.theme)
            MiscSettings::class ->
                context.getString(R.string.misc)
            ViewTypeSettings::class ->
                context.getString(R.string.view_type)
            LoggingSettings::class ->
                context.getString(R.string.logging)
            HistorySettings::class ->
                context.getString(R.string.history)
            NavigationSettings::class ->
                context.getString(R.string.navigation)

            else -> error("No name for $this")
        }
    }

    fun SearchableSettings.getPageName(context: Context): String =
        this::class.getPageName(context)
}

@Singleton
class MainSettings @Inject constructor(
    @ApplicationContext private val context: Context,
) : SearchableSettings {
    override val parents: List<KClass<out SearchableSettings>> = listOf()

    val settingTheme = BasicSettingItem(
        R.drawable.baseline_palette_24,
        context.getString(R.string.theme),
        context.getString(R.string.theme_settings_desc),
    )
    val settingViewType = BasicSettingItem(
        R.drawable.baseline_view_agenda_24,
        context.getString(R.string.view_type),
        context.getString(R.string.view_type_settings_desc),
    )
    val settingPostAndComment = BasicSettingItem(
        R.drawable.baseline_mode_comment_24,
        context.getString(R.string.post_and_comments),
        context.getString(R.string.post_and_comments_settings_desc),
    )
    val settingLemmyWeb = BasicSettingItem(
        R.drawable.ic_lemmy_24,
        context.getString(R.string.lemmy_web_preferences),
        context.getString(R.string.lemmy_web_preferences_desc),
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

    val patreonSettings = BasicSettingItem(
        R.drawable.baseline_attach_money_24,
        context.getString(R.string.patreon_supporters),
        context.getString(R.string.patreon_supporters_desc),
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

    override val allSettings = listOf(
        SubgroupItem(
            context.getString(R.string.look_and_feel),
            listOf(
                settingTheme,
                settingPostList,
                commentListSettings,
                settingViewType,
                settingPostAndComment,
                settingGestures,
                miscSettings,
            ),
        ),
        SubgroupItem(
            context.getString(R.string.account_settings),
            listOf(
                settingLemmyWeb,
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
                settingAbout,
                settingSummitCommunity,
                patreonSettings,
            ),
        ),
    )
}

@Singleton
class LemmyWebSettings @Inject constructor(
    @ApplicationContext private val context: Context,
) : SearchableSettings {
    override val parents: List<KClass<out SearchableSettings>> = listOf(
        MainSettings::class,
    )

    val instanceSetting = TextValueSettingItem(
        title = context.getString(R.string.instance),
        supportsRichText = false,
        isEnabled = false,
    )

    val displayNameSetting = TextValueSettingItem(
        title = context.getString(R.string.display_name),
        supportsRichText = false,
    )

    val bioSetting = TextValueSettingItem(
        title = context.getString(R.string.biography),
        supportsRichText = true,
    )

    val emailSetting = TextValueSettingItem(
        title = context.getString(R.string.email),
        supportsRichText = false,
    )

    val matrixSetting = TextValueSettingItem(
        title = context.getString(R.string.matrix_user),
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
    )
}

@Singleton
class GestureSettings @Inject constructor(
    @ApplicationContext private val context: Context,
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
    )
    val postGestureAction1 = RadioGroupSettingItem(
        null,
        context.getString(R.string.gesture_action_1),
        null,
        postGestureActionOptions,
    )
    val postGestureAction2 = RadioGroupSettingItem(
        null,
        context.getString(R.string.gesture_action_2),
        null,
        postGestureActionOptions,
    )
    val postGestureAction3 = RadioGroupSettingItem(
        null,
        context.getString(R.string.gesture_action_3),
        null,
        postGestureActionOptions,
    )
    val postGestureSize = SliderSettingItem(
        context.getString(R.string.post_gesture_size),
        0f,
        1f,
        0.01f,
    )

    val commentGestureAction1 = RadioGroupSettingItem(
        null,
        context.getString(R.string.gesture_action_1),
        null,
        commentGestureActionOptions,
    )
    val commentGestureAction2 = RadioGroupSettingItem(
        null,
        context.getString(R.string.gesture_action_2),
        null,
        commentGestureActionOptions,
    )
    val commentGestureAction3 = RadioGroupSettingItem(
        null,
        context.getString(R.string.gesture_action_3),
        null,
        commentGestureActionOptions,
    )
    val commentGestureSize = SliderSettingItem(
        context.getString(R.string.comment_gesture_size),
        0f,
        1f,
        0.01f,
    )
    override val allSettings: List<SettingItem> = listOf(
        postGestureAction1,
        postGestureAction2,
        postGestureAction3,
        postGestureSize,

        commentGestureAction1,
        commentGestureAction2,
        commentGestureAction3,
        commentGestureSize,
    )
}

@Singleton
class PostListSettings @Inject constructor(
    @ApplicationContext private val context: Context,
) : SearchableSettings {
    override val parents: List<KClass<out SearchableSettings>> = listOf(
        MainSettings::class,
    )
    val infinity = OnOffSettingItem(
        null,
        context.getString(R.string.infinity),
        context.getString(R.string.no_your_limits),
    )
    val markPostsAsReadOnScroll = OnOffSettingItem(
        null,
        context.getString(R.string.mark_posts_as_read_on_scroll),
        context.getString(R.string.mark_posts_as_read_on_scroll_desc),
    )
    val blurNsfwPosts = OnOffSettingItem(
        null,
        context.getString(R.string.blur_nsfw_posts),
        null,
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
    )

    val viewImageOnSingleTap = OnOffSettingItem(
        null,
        context.getString(R.string.view_image_on_single_tap),
        context.getString(R.string.view_image_on_single_tap_desc),
    )

    val compatibilityMode = OnOffSettingItem(
        null,
        context.getString(R.string.compatibility_mode),
        context.getString(R.string.compatibility_mode_desc),
    )

    val hidePostScores = OnOffSettingItem(
        null,
        context.getString(R.string.hide_post_scores),
        null,
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
    )
    val showImagePosts = OnOffSettingItem(
        R.drawable.baseline_image_24,
        context.getString(R.string.show_image_posts),
        null,
    )
    val showVideoPosts = OnOffSettingItem(
        R.drawable.baseline_videocam_24,
        context.getString(R.string.show_video_posts),
        null,
    )
    val showTextPosts = OnOffSettingItem(
        R.drawable.baseline_text_fields_24,
        context.getString(R.string.show_text_posts),
        null,
    )
    val showNsfwPosts = OnOffSettingItem(
        R.drawable.ic_nsfw_24,
        context.getString(R.string.show_nsfw_posts),
        null,
    )
    val lockBottomBar = OnOffSettingItem(
        null,
        context.getString(R.string.lock_bottom_bar),
        context.getString(R.string.lock_bottom_bar_desc),
    )

    override val allSettings: List<SettingItem> = listOf(
        infinity,
        markPostsAsReadOnScroll,
        blurNsfwPosts,
        defaultCommunitySortOrder,
        viewImageOnSingleTap,
        compatibilityMode,
        hidePostScores,
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
    )
}

@Singleton
class CommentListSettings @Inject constructor(
    @ApplicationContext private val context: Context,
) : SearchableSettings {
    override val parents: List<KClass<out SearchableSettings>> = listOf(
        MainSettings::class,
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
    )
    val relayStyleNavigation = OnOffSettingItem(
        null,
        context.getString(R.string.relay_style_navigation),
        context.getString(R.string.relay_style_navigation_desc),
    )
    val hideCommentScores = OnOffSettingItem(
        null,
        context.getString(R.string.hide_comment_scores),
        null,
    )
    val useVolumeButtonNavigation = OnOffSettingItem(
        null,
        context.getString(R.string.use_volume_button_navigation),
        context.getString(R.string.use_volume_button_navigation_desc),
    )
    val collapseChildCommentsByDefault = OnOffSettingItem(
        null,
        context.getString(R.string.collapse_child_comments),
        context.getString(R.string.collapse_child_comments_desc),
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
    )

    override val allSettings: List<SettingItem> = listOf(
        defaultCommentsSortOrder,
        relayStyleNavigation,
        hideCommentScores,
        useVolumeButtonNavigation,
    )
}

@Singleton
class PostAndCommentsSettings @Inject constructor(
    @ApplicationContext private val context: Context,
) : SearchableSettings {
    override val parents: List<KClass<out SearchableSettings>> = listOf(
        MainSettings::class,
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
    )

    val commentFontSize = SliderSettingItem(
        context.getString(R.string.font_size),
        0.2f,
        3f,
        0.1f,
    )

    val commentIndentationLevel = SliderSettingItem(
        context.getString(R.string.indentation_per_level),
        0f,
        32f,
        stepSize = 1f,
    )

    val showCommentActions = OnOffSettingItem(
        null,
        context.getString(R.string.show_comment_actions),
        null,
    )

    val commentsThreadStyle = RadioGroupSettingItem(
        null,
        context.getString(R.string.comments_thread_style),
        null,
        listOf(
            RadioGroupSettingItem.RadioGroupOption(
                CommentsThreadStyle.Modern,
                context.getString(R.string.modern),
                null,
                null,
            ),
            RadioGroupSettingItem.RadioGroupOption(
                CommentsThreadStyle.Legacy,
                context.getString(R.string.classic),
                null,
                null,
            ),
            RadioGroupSettingItem.RadioGroupOption(
                CommentsThreadStyle.LegacyWithColors,
                context.getString(R.string.classic_but_with_colors),
                null,
                null,
            ),
        ),
    )

    val tapCommentToCollapse = OnOffSettingItem(
        null,
        context.getString(R.string.tap_comment_to_collapse),
        context.getString(R.string.tap_comment_to_collapse_desc),
    )

    val alwaysShowLinkBelowPost = OnOffSettingItem(
        null,
        context.getString(R.string.always_show_link_below_post),
        context.getString(R.string.always_show_link_below_post_desc),
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
    )
}

@Singleton
class ThemeSettings @Inject constructor(
    @ApplicationContext private val context: Context,
) : SearchableSettings {
    override val parents: List<KClass<out SearchableSettings>> = listOf(
        MainSettings::class,
    )

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
    )

    val materialYou = OnOffSettingItem(
        null,
        context.getString(R.string.material_you),
        context.getString(R.string.personalized_theming_based_on_your_wallpaper),
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
        ),
    )

    val blackTheme = OnOffSettingItem(
        null,
        context.getString(R.string.black_theme),
        context.getString(R.string.black_theme_desc),
    )

    val font = RadioGroupSettingItem(
        null,
        context.getString(R.string.font),
        null,
        listOf(
            RadioGroupSettingItem.RadioGroupOption(
                FontIds.Default,
                context.getString(R.string._default),
                null,
                null,
            ),
            RadioGroupSettingItem.RadioGroupOption(
                FontIds.Roboto,
                context.getString(R.string.roboto),
                null,
                null,
            ),
            RadioGroupSettingItem.RadioGroupOption(
                FontIds.RobotoSerif,
                context.getString(R.string.roboto_serif),
                null,
                null,
            ),
            RadioGroupSettingItem.RadioGroupOption(
                FontIds.OpenSans,
                context.getString(R.string.open_sans),
                null,
                null,
            ),
        ),
    )

    val fontSize = TextOnlySettingItem(
        context.getString(R.string.font_size),
        "",
    )

    val fontColor = TextOnlySettingItem(
        context.getString(R.string.font_color),
        "",
    )

    val upvoteColor = ColorSettingItem(
        null,
        context.getString(R.string.upvote_color),
        context.getString(R.string.updoot_color),
    )

    val downvoteColor = ColorSettingItem(
        null,
        context.getString(R.string.downvote_color),
        context.getString(R.string.downdoot_color),
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
    )
}

@Singleton
class ViewTypeSettings @Inject constructor(
    @ApplicationContext private val context: Context,
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

    val contentMaxLines =
        TextOnlySettingItem(
            context.getString(R.string.full_content_max_lines),
            "",
        )

    override val parents: List<KClass<out SearchableSettings>> = listOf(
        MainSettings::class,
    )

    override val allSettings: List<SettingItem> = listOf(
        baseViewType,
        fontSize,
        preferImageAtEnd,
        preferFullImage,
        preferTitleText,
    )
}

@Singleton
class AboutSettings @Inject constructor(
    @ApplicationContext private val context: Context,
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
    override val allSettings: List<SettingItem> = listOf(
        version,
        googlePlayLink,
        giveFeedback,
    )
}

@Singleton
class CacheSettings @Inject constructor(
    @ApplicationContext private val context: Context,
) : SearchableSettings {
    override val parents: List<KClass<out SearchableSettings>> = listOf(
        MainSettings::class,
    )
    val clearCache = BasicSettingItem(
        null,
        context.getString(R.string.clear_media_cache),
        null,
    )
    override val allSettings: List<SettingItem> = listOf(
        clearCache,
    )
}

@Singleton
class HiddenPostsSettings @Inject constructor(
    @ApplicationContext private val context: Context,
) : SearchableSettings {
    override val parents: List<KClass<out SearchableSettings>> = listOf(
        MainSettings::class,
    )
    val resetHiddenPosts = BasicSettingItem(
        null,
        context.getString(R.string.reset_hidden_posts),
        null,
    )
    override val allSettings: List<SettingItem> = listOf(
        resetHiddenPosts,
    )
}

@Singleton
class MiscSettings @Inject constructor(
    @ApplicationContext private val context: Context,
) : SearchableSettings {
    override val parents: List<KClass<out SearchableSettings>> = listOf(
        MainSettings::class,
    )
    val openLinksInExternalBrowser = OnOffSettingItem(
        null,
        context.getString(R.string.open_links_in_external_browser),
        context.getString(R.string.open_links_in_external_browser_desc),
    )
    val autoLinkPhoneNumbers = OnOffSettingItem(
        null,
        context.getString(R.string.auto_convert_phone_numbers_to_links),
        null,
    )
    val showUpAndDownVotes = OnOffSettingItem(
        null,
        context.getString(R.string.show_up_and_down_votes),
        context.getString(R.string.show_up_and_down_votes_desc),
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
    )

    val retainLastPost = OnOffSettingItem(
        null,
        context.getString(R.string.retain_last_post),
        context.getString(R.string.retain_last_post_desc),
    )

    val leftHandMode = OnOffSettingItem(
        null,
        context.getString(R.string.left_hand_mode),
        context.getString(R.string.left_hand_mode_desc),
    )

    val transparentNotificationBar = OnOffSettingItem(
        null,
        context.getString(R.string.transparent_notification_bar),
        null,
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
    )

    override val allSettings: List<SettingItem> = listOf(
        openLinksInExternalBrowser,
        autoLinkPhoneNumbers,
        showUpAndDownVotes,
        retainLastPost,
        leftHandMode,
        transparentNotificationBar,
        previewLinks,
    )
}

class AllSettings @Inject constructor(
    private val mainSettings: MainSettings,
    private val lemmyWebSettings: LemmyWebSettings,
    private val gestureSettings: GestureSettings,
    private val postListSettings: PostListSettings,
    private val commentListSettings: CommentListSettings,
    private val aboutSettings: AboutSettings,
    private val cacheSettings: CacheSettings,
    private val hiddenPostsSettings: HiddenPostsSettings,
    private val postAndCommentsSettings: PostAndCommentsSettings,
    private val themeSettings: ThemeSettings,
    private val viewTypeSettings: ViewTypeSettings,
    private val miscSettings: MiscSettings,
    private val loggingSettings: LoggingSettings,
    private val historySettings: HistorySettings,
    private val navigationSettings: NavigationSettings,
) {
    val allSearchableSettings: List<SearchableSettings> = listOf(
        mainSettings,
        lemmyWebSettings,
        gestureSettings,
        postListSettings,
        commentListSettings,
        aboutSettings,
        cacheSettings,
        hiddenPostsSettings,
        postAndCommentsSettings,
        themeSettings,
        viewTypeSettings,
        miscSettings,
        loggingSettings,
        historySettings,
        navigationSettings,
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
        }
    }
}

fun makeCommunitySortOrderChoices(context: Context) =
    listOf(
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
            R.id.sort_order_top_last_hour,
            context.getString(R.string.time_frame_last_hour),
            null,
            null,
        ),
        RadioGroupSettingItem.RadioGroupOption(
            R.id.sort_order_top_last_six_hour,
            context.getString(R.string.time_frame_last_hours_format, "6"),
            null,
            null,
        ),
        RadioGroupSettingItem.RadioGroupOption(
            R.id.sort_order_top_last_twelve_hour,
            context.getString(R.string.time_frame_last_hours_format, "12"),
            null,
            null,
        ),
        RadioGroupSettingItem.RadioGroupOption(
            R.id.sort_order_top_day,
            context.getString(R.string.time_frame_today),
            null,
            null,
        ),
        RadioGroupSettingItem.RadioGroupOption(
            R.id.sort_order_top_week,
            context.getString(R.string.time_frame_this_week),
            null,
            null,
        ),
        RadioGroupSettingItem.RadioGroupOption(
            R.id.sort_order_top_month,
            context.getString(R.string.time_frame_this_month),
            null,
            null,
        ),
        RadioGroupSettingItem.RadioGroupOption(
            R.id.sort_order_top_last_three_month,
            context.getString(R.string.time_frame_last_months_format, "3"),
            null,
            null,
        ),
        RadioGroupSettingItem.RadioGroupOption(
            R.id.sort_order_top_last_six_month,
            context.getString(R.string.time_frame_last_months_format, "6"),
            null,
            null,
        ),
        RadioGroupSettingItem.RadioGroupOption(
            R.id.sort_order_top_last_nine_month,
            context.getString(R.string.time_frame_last_months_format, "9"),
            null,
            null,
        ),
        RadioGroupSettingItem.RadioGroupOption(
            R.id.sort_order_top_year,
            context.getString(R.string.time_frame_this_year),
            null,
            null,
        ),
        RadioGroupSettingItem.RadioGroupOption(
            R.id.sort_order_top_all_time,
            context.getString(R.string.time_frame_all_time),
            null,
            null,
        ),
    )

@Singleton
class LoggingSettings @Inject constructor(
    @ApplicationContext private val context: Context,
) : SearchableSettings {

    val useFirebase = OnOffSettingItem(
        null,
        context.getString(R.string.use_firebase),
        context.getString(R.string.use_firebase_desc),
    )

    override val parents: List<KClass<out SearchableSettings>> = listOf(
        MainSettings::class,
    )

    override val allSettings: List<SettingItem> = listOf(
        useFirebase,
    )
}

@Singleton
class HistorySettings @Inject constructor(
    @ApplicationContext private val context: Context,
) : SearchableSettings {

    val recordBrowsingHistory = OnOffSettingItem(
        null,
        context.getString(R.string.record_browsing_history),
        context.getString(R.string.record_browsing_history_desc),
    )

    override val parents: List<KClass<out SearchableSettings>> = listOf(
        MainSettings::class,
    )

    override val allSettings: List<SettingItem> = listOf(
        recordBrowsingHistory,
    )
}

@Singleton
class NavigationSettings @Inject constructor(
    @ApplicationContext private val context: Context,
) : SearchableSettings {

    val navBarDestOptions =
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
    )

    val navBarDest1 = RadioGroupSettingItem(
        null,
        context.getString(R.string.nav_bar_option_format, "1"),
        null,
        navBarDestOptions,
    )

    val navBarDest2 = RadioGroupSettingItem(
        null,
        context.getString(R.string.nav_bar_option_format, "2"),
        null,
        navBarDestOptions,
    )

    val navBarDest3 = RadioGroupSettingItem(
        null,
        context.getString(R.string.nav_bar_option_format, "3"),
        null,
        navBarDestOptions,
    )

    val navBarDest4 = RadioGroupSettingItem(
        null,
        context.getString(R.string.nav_bar_option_format, "4"),
        null,
        navBarDestOptions,
    )

    val navBarDest5 = RadioGroupSettingItem(
        null,
        context.getString(R.string.nav_bar_option_format, "5"),
        null,
        navBarDestOptions,
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
    )
}
