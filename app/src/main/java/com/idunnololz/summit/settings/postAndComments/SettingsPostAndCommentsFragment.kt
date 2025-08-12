package com.idunnololz.summit.settings.postAndComments

import androidx.annotation.IdRes
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.idunnololz.summit.R
import com.idunnololz.summit.filterLists.ContentTypes
import com.idunnololz.summit.filterLists.FilterTypes
import com.idunnololz.summit.lemmy.idToCommentsSortOrder
import com.idunnololz.summit.lemmy.toApiSortOrder
import com.idunnololz.summit.lemmy.toId
import com.idunnololz.summit.settings.BaseSettingsFragment
import com.idunnololz.summit.settings.PostAndCommentsSettings
import com.idunnololz.summit.settings.SettingModelItem
import com.idunnololz.summit.settings.util.asCustomItem
import com.idunnololz.summit.settings.util.asOnOffSwitch
import com.idunnololz.summit.settings.util.asSingleChoiceSelectorItem
import com.idunnololz.summit.util.ext.navigateSafe
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class SettingsPostAndCommentsFragment : BaseSettingsFragment() {

  private val args: SettingsPostAndCommentsFragmentArgs by navArgs()

  @Inject
  override lateinit var settings: PostAndCommentsSettings

  override fun generateData(): List<SettingModelItem> = listOf(
    settings.settingPostAndCommentsAppearance.asCustomItem {
      val direction = SettingsPostAndCommentsFragmentDirections
        .actionSettingsPostAndCommentsFragmentToSettingsPostAndCommentsAppearanceFragment()
      findNavController().navigateSafe(direction)
    },
    SettingModelItem.DividerItem(R.id.divider0),
    settings.relayStyleNavigation.asOnOffSwitch(
      { preferences.commentsNavigationFab },
      { preferences.commentsNavigationFab = it },
    ),
    settings.swipeBetweenPosts.asOnOffSwitch(
      { preferences.swipeBetweenPosts },
      { preferences.swipeBetweenPosts = it },
    ),
    settings.customizePostQuickActions.asCustomItem {
      val directions = SettingsPostAndCommentsFragmentDirections
        .actionSettingsPostAndCommentsFragmentToPostQuickActionsFragment()
      findNavController().navigateSafe(directions)
    },
    settings.postFabQuickAction.asSingleChoiceSelectorItem(
      { preferences.postFabQuickAction },
      { preferences.postFabQuickAction = it },
    ),
    settings.showCrossPostsInPost.asOnOffSwitch(
      { preferences.showCrossPostsInPost },
      { preferences.showCrossPostsInPost = it },
    ),
    settings.showNavigationBarOnPost.asOnOffSwitch(
      { preferences.showNavigationBarOnPost },
      { preferences.showNavigationBarOnPost = it },
    ),
    SettingModelItem.SubgroupItem(
      getString(R.string.comments),
      listOf(
        settings.defaultCommentsSortOrder.asSingleChoiceSelectorItem(
          {
            preferences.defaultCommentsSortOrder?.toApiSortOrder()?.toId()
              ?: R.id.comments_sort_order_default
          },
          {
            preferences.defaultCommentsSortOrder = idToCommentsSortOrder(it)
          },
        ),
        settings.useVolumeButtonNavigation.asOnOffSwitch(
          { preferences.useVolumeButtonNavigation },
          { preferences.useVolumeButtonNavigation = it },
        ),
        settings.collapseChildCommentsByDefault.asOnOffSwitch(
          { preferences.collapseChildCommentsByDefault },
          { preferences.collapseChildCommentsByDefault = it },
        ),
        settings.autoCollapseCommentThreshold.asSingleChoiceSelectorItem(
          { convertAutoCollapseCommentToOptionId(preferences.autoCollapseCommentThreshold) },
          {
            val threshold = convertOptionIdToAutoCollapseCommentThreshold(it)

            if (threshold != null) {
              preferences.autoCollapseCommentThreshold = threshold
            }
          },
        ),
        settings.showCommentUpvotePercentage.asOnOffSwitch(
          { preferences.showCommentUpvotePercentage },
          {
            preferences.showCommentUpvotePercentage = it
          },
        ),
        settings.showInlineMediaAsLinks.asOnOffSwitch(
          { preferences.commentsShowInlineMediaAsLinks },
          {
            preferences.commentsShowInlineMediaAsLinks = it
          },
        ),
        settings.commentScores.asSingleChoiceSelectorItem(
          {
            if (preferences.hideCommentScores) {
              R.id.hide_scores
            } else if (preferences.commentShowUpAndDownVotes) {
              R.id.show_up_and_down_votes
            } else {
              R.id.show_scores
            }
          },
          {
            when (it) {
              R.id.hide_scores -> {
                preferences.hideCommentScores = true
                preferences.commentShowUpAndDownVotes = false
              }
              R.id.show_up_and_down_votes -> {
                preferences.hideCommentScores = false
                preferences.commentShowUpAndDownVotes = true
              }
              R.id.show_scores -> {
                preferences.hideCommentScores = false
                preferences.commentShowUpAndDownVotes = false
              }
            }
          },
        ),
        settings.opTagStyle.asSingleChoiceSelectorItem(
          { preferences.opTagStyle },
          { preferences.opTagStyle = it },
        ),
        settings.customizeCommentQuickActions.asCustomItem {
          val directions = SettingsPostAndCommentsFragmentDirections
            .actionSettingCommentListFragmentToCustomQuickActionsFragment()
          findNavController().navigateSafe(directions)
        },
      ),
    ),

    SettingModelItem.SubgroupItem(
      getString(R.string.comment_header),
      listOf(
        settings.showProfileIcons.asOnOffSwitch(
          { preferences.showProfileIcons },
          {
            preferences.showProfileIcons = it
          },
        ),
        settings.showDefaultProfileIcons.asOnOffSwitch(
          { preferences.showDefaultProfileIcons },
          {
            preferences.showDefaultProfileIcons = it
          },
        ),
        settings.commentHeaderLayout
          .copy(isEnabled = !preferences.showProfileIcons)
          .asSingleChoiceSelectorItem(
            { preferences.commentHeaderLayout },
            {
              preferences.commentHeaderLayout = it
            },
          ),
      ),
    ),
    *if (args.account != null) {
      arrayOf()
    } else {
      arrayOf<SettingModelItem>(
        SettingModelItem.SubgroupItem(
          getString(R.string.filters),
          listOf(
            settings.keywordFilters.asCustomItem {
              val direction = SettingsPostAndCommentsFragmentDirections
                .actionSettingCommentListFragmentToSettingsFilterListFragment(
                  ContentTypes.CommentListType,
                  FilterTypes.KeywordFilter,
                  getString(R.string.keyword_filters),
                )
              findNavController().navigateSafe(direction)
            },
            settings.instanceFilters.asCustomItem {
              val direction = SettingsPostAndCommentsFragmentDirections
                .actionSettingCommentListFragmentToSettingsFilterListFragment(
                  ContentTypes.CommentListType,
                  FilterTypes.InstanceFilter,
                  getString(R.string.instance_filters),
                )
              findNavController().navigateSafe(direction)
            },
            settings.userFilters.asCustomItem {
              val direction = SettingsPostAndCommentsFragmentDirections
                .actionSettingCommentListFragmentToSettingsFilterListFragment(
                  ContentTypes.CommentListType,
                  FilterTypes.UserFilter,
                  getString(R.string.user_filters),
                )
              findNavController().navigateSafe(direction)
            },
          ),
        ),
      )
    },
  )

  private fun convertAutoCollapseCommentToOptionId(value: Float) = when {
    value >= 0.499f -> {
      R.id.auto_collapse_comment_threshold_50
    }
    value >= 0.399f -> {
      R.id.auto_collapse_comment_threshold_40
    }
    value >= 0.299f -> {
      R.id.auto_collapse_comment_threshold_30
    }
    value >= 0.199f -> {
      R.id.auto_collapse_comment_threshold_20
    }
    value >= 0f -> {
      R.id.auto_collapse_comment_threshold_10
    }
    else -> {
      R.id.auto_collapse_comment_threshold_never_collapse
    }
  }

  private fun convertOptionIdToAutoCollapseCommentThreshold(@IdRes id: Int) = when (id) {
    R.id.auto_collapse_comment_threshold_50 -> 0.5f
    R.id.auto_collapse_comment_threshold_40 -> 0.4f
    R.id.auto_collapse_comment_threshold_30 -> 0.3f
    R.id.auto_collapse_comment_threshold_20 -> 0.2f
    R.id.auto_collapse_comment_threshold_10 -> 0.1f
    R.id.auto_collapse_comment_threshold_never_collapse -> -1f
    else -> null
  }
}
