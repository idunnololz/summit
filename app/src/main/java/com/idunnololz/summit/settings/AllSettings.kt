package com.idunnololz.summit.settings

import android.content.Context
import com.idunnololz.summit.R
import com.idunnololz.summit.preferences.ColorSchemes
import com.idunnololz.summit.preferences.CommentGestureAction
import com.idunnololz.summit.preferences.CommentsThreadStyle
import com.idunnololz.summit.preferences.PostGestureAction
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MainSettings @Inject constructor(
    @ApplicationContext private val context: Context,
) {
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
    val settingHistory = BasicSettingItem(
        R.drawable.baseline_history_24,
        context.getString(R.string.history),
        context.getString(R.string.history_setting_desc),
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
}

@Singleton
class LemmyWebSettings @Inject constructor(
    @ApplicationContext private val context: Context,
) {

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
}

@Singleton
class GestureSettings @Inject constructor(
    @ApplicationContext private val context: Context,
) {
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
}

@Singleton
class PostListSettings @Inject constructor(
    @ApplicationContext private val context: Context,
) {
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
}

@Singleton
class CommentListSettings @Inject constructor(
    @ApplicationContext private val context: Context,
) {
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
}

@Singleton
class PostAndCommentsSettings @Inject constructor(
    @ApplicationContext private val context: Context,
) {

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
}

@Singleton
class ThemeSettings @Inject constructor(
    @ApplicationContext private val context: Context,
) {

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

    val fontSize = TextOnlySettingItem(
        context.getString(R.string.font_size),
        "",
    )

    val fontColor = TextOnlySettingItem(
        context.getString(R.string.font_color),
        "",
    )
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
