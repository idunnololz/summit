package com.idunnololz.summit.settings.postsFeed

import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.google.android.material.snackbar.Snackbar
import com.idunnololz.summit.R
import com.idunnololz.summit.filterLists.ContentTypes
import com.idunnololz.summit.filterLists.FilterTypes
import com.idunnololz.summit.lemmy.communityPicker.CommunityPickerDialogFragment
import com.idunnololz.summit.lemmy.idToSortOrder
import com.idunnololz.summit.lemmy.toApiSortOrder
import com.idunnololz.summit.lemmy.toId
import com.idunnololz.summit.settings.BaseSettingsFragment
import com.idunnololz.summit.settings.PostsFeedSettings
import com.idunnololz.summit.settings.PreferencesViewModel
import com.idunnololz.summit.settings.SettingModelItem
import com.idunnololz.summit.settings.util.asCustomItem
import com.idunnololz.summit.settings.util.asOnOffSwitch
import com.idunnololz.summit.settings.util.asSingleChoiceSelectorItem
import com.idunnololz.summit.user.UserCommunitiesManager
import com.idunnololz.summit.util.ext.navigateSafe
import com.idunnololz.summit.util.getParcelableCompat
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.launch

@AndroidEntryPoint
class SettingsPostsFeedFragment :
  BaseSettingsFragment() {

  private val args: SettingsPostsFeedFragmentArgs by navArgs()
  private val preferencesViewModel: PreferencesViewModel by viewModels()

  @Inject
  override lateinit var settings: PostsFeedSettings

  @Inject
  lateinit var userCommunitiesManager: UserCommunitiesManager

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    preferences = preferencesViewModel.getPreferences(args.account)
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)

    childFragmentManager.setFragmentResultListener(
      CommunityPickerDialogFragment.REQUEST_KEY,
      viewLifecycleOwner,
    ) { requestKey, bundle ->
      val result = bundle.getParcelableCompat<CommunityPickerDialogFragment.Result>(
        CommunityPickerDialogFragment.REQUEST_KEY_RESULT,
      )
      if (result != null) {
        viewLifecycleOwner.lifecycleScope.launch {
          userCommunitiesManager.setDefaultPage(result.communityRef)

          refresh()

          Snackbar.make(
            binding.coordinatorLayout,
            R.string.home_page_set,
            Snackbar.LENGTH_LONG,
          ).show()
        }
      }
    }
  }

  override fun generateData(): List<SettingModelItem> {
    val context = context ?: return listOf()

    return listOf(
      settings.settingsPostFeedAppearance.asCustomItem {
        val direction = SettingsPostsFeedFragmentDirections
          .actionSettingsContentFragmentToSettingViewTypeFragment()
        findNavController().navigateSafe(direction)
      },
      SettingModelItem.DividerItem(R.id.settings_posts_feed_divider),
      settings.markPostsAsReadOnScroll.asOnOffSwitch(
        { preferences.markPostsAsReadOnScroll },
        { preferences.markPostsAsReadOnScroll = it },
      ),
      settings.blurNsfwPosts.asOnOffSwitch(
        { preferences.blurNsfwPosts },
        { preferences.blurNsfwPosts = it },
      ),
      settings.doNotBlurNsfwContentInNsfwCommunityFeed.asOnOffSwitch(
        { preferences.doNotBlurNsfwContentInNsfwCommunityFeed },
        { preferences.doNotBlurNsfwContentInNsfwCommunityFeed = it },
      ),
      settings.defaultCommunitySortOrder.asSingleChoiceSelectorItem(
        {
          preferences.defaultCommunitySortOrder?.toApiSortOrder()?.toId()
            ?: R.id.community_sort_order_default
        },
        {
          preferences.defaultCommunitySortOrder = idToSortOrder(it)
        },
      ),

      settings.viewImageOnSingleTap.asOnOffSwitch(
        { preferences.postListViewImageOnSingleTap },
        {
          preferences.postListViewImageOnSingleTap = it
        },
      ),
      settings.postScores.asSingleChoiceSelectorItem(
        {
          if (preferences.hidePostScores) {
            R.id.hide_scores
          } else if (preferences.postShowUpAndDownVotes) {
            R.id.show_up_and_down_votes
          } else {
            R.id.show_scores
          }
        },
        {
          when (it) {
            R.id.hide_scores -> {
              preferences.hidePostScores = true
              preferences.postShowUpAndDownVotes = false
            }
            R.id.show_up_and_down_votes -> {
              preferences.hidePostScores = false
              preferences.postShowUpAndDownVotes = true
            }
            R.id.show_scores -> {
              preferences.hidePostScores = false
              preferences.postShowUpAndDownVotes = false
            }
          }
        },
      ),
      settings.lockBottomBar.asOnOffSwitch(
        { preferences.lockBottomBar },
        {
          preferences.lockBottomBar = it
        },
      ),

      settings.showPostUpvotePercentage.asOnOffSwitch(
        { preferences.showPostUpvotePercentage },
        {
          preferences.showPostUpvotePercentage = it
        },
      ),
      settings.useMultilinePostHeaders.asOnOffSwitch(
        { preferences.useMultilinePostHeaders },
        {
          preferences.useMultilinePostHeaders = it
        },
      ),
      settings.showFilteredPosts.asOnOffSwitch(
        { preferences.showFilteredPosts },
        {
          preferences.showFilteredPosts = it
        },
      ),
      settings.prefetchPosts.asOnOffSwitch(
        { preferences.prefetchPosts },
        {
          preferences.prefetchPosts = it
        },
      ),
      settings.homeFabQuickAction.asSingleChoiceSelectorItem(
        { preferences.homeFabQuickAction },
        {
          preferences.homeFabQuickAction = it
        },
      ),
      settings.parseMarkdownInPostTitles.asOnOffSwitch(
        { preferences.parseMarkdownInPostTitles },
        {
          preferences.parseMarkdownInPostTitles = it
        },
      ),
      settings.homePage.asCustomItem(
        {
          userCommunitiesManager.defaultCommunity.value.getLocalizedFullName(context)
        },
        {
          CommunityPickerDialogFragment.show(childFragmentManager, showFeeds = true)
        },
      ),
      settings.postFeedShowScrollBar.asOnOffSwitch(
        { preferences.postFeedShowScrollBar },
        {
          preferences.postFeedShowScrollBar = it
        },
      ),
      settings.hideDuplicatePostsOnRead.asOnOffSwitch(
        { preferences.hideDuplicatePostsOnRead },
        {
          preferences.hideDuplicatePostsOnRead = it
        },
      ),
      settings.customizePostsInFeedQuickActions.asCustomItem {
        val direction = SettingsPostsFeedFragmentDirections
          .actionSettingsContentFragmentToPostsInFeedQuickActionsFragment()
        findNavController().navigateSafe(direction)
      },
      settings.openLinkWhenThumbnailTapped.asOnOffSwitch(
        { preferences.openLinkWhenThumbnailTapped },
        {
          preferences.openLinkWhenThumbnailTapped = it
        },
      ),
      settings.showPostType.asOnOffSwitch(
        { preferences.showPostType },
        {
          preferences.showPostType = it
        },
      ),

      SettingModelItem.SubgroupItem(
        getString(R.string.posts_feed_header),
        listOf(
          settings.usePostsFeedHeader.asOnOffSwitch(
            { preferences.usePostsFeedHeader },
            {
              preferences.usePostsFeedHeader = it
            },
          ),
          settings.hideHeaderBannerIfNoBanner.asOnOffSwitch(
            { preferences.hideHeaderBannerIfNoBanner },
            {
              preferences.hideHeaderBannerIfNoBanner = it
            },
          ),
        ),
      ),

      SettingModelItem.SubgroupItem(
        getString(R.string.filters),
        listOf(
          *if (args.account != null) {
            arrayOf()
          } else {
            arrayOf(
              settings.keywordFilters.asCustomItem {
                val direction = SettingsPostsFeedFragmentDirections
                  .actionSettingsContentFragmentToSettingsFilterListFragment(
                    ContentTypes.PostListType,
                    FilterTypes.KeywordFilter,
                    getString(R.string.keyword_filters),
                  )
                findNavController().navigateSafe(direction)
              },
              settings.instanceFilters.asCustomItem {
                val direction = SettingsPostsFeedFragmentDirections
                  .actionSettingsContentFragmentToSettingsFilterListFragment(
                    ContentTypes.PostListType,
                    FilterTypes.InstanceFilter,
                    getString(R.string.instance_filters),
                  )
                findNavController().navigateSafe(direction)
              },
              settings.communityFilters.asCustomItem {
                val direction = SettingsPostsFeedFragmentDirections
                  .actionSettingsContentFragmentToSettingsFilterListFragment(
                    ContentTypes.PostListType,
                    FilterTypes.CommunityFilter,
                    getString(R.string.community_filters),
                  )
                findNavController().navigateSafe(direction)
              },
              settings.userFilters.asCustomItem {
                val direction = SettingsPostsFeedFragmentDirections
                  .actionSettingsContentFragmentToSettingsFilterListFragment(
                    ContentTypes.PostListType,
                    FilterTypes.UserFilter,
                    getString(R.string.user_filters),
                  )
                findNavController().navigateSafe(direction)
              },
              settings.urlFilters.asCustomItem {
                val direction = SettingsPostsFeedFragmentDirections
                  .actionSettingsContentFragmentToSettingsFilterListFragment(
                    ContentTypes.PostListType,
                    FilterTypes.UrlFilter,
                    getString(R.string.url_filters),
                  )
                findNavController().navigateSafe(direction)
              },
            )
          },

          settings.showLinkPosts.asOnOffSwitch(
            { preferences.showLinkPosts },
            {
              preferences.showLinkPosts = it
            },
          ),
          settings.showImagePosts.asOnOffSwitch(
            { preferences.showImagePosts },
            {
              preferences.showImagePosts = it
            },
          ),
          settings.showVideoPosts.asOnOffSwitch(
            { preferences.showVideoPosts },
            {
              preferences.showVideoPosts = it
            },
          ),
          settings.showTextPosts.asOnOffSwitch(
            { preferences.showTextPosts },
            {
              preferences.showTextPosts = it
            },
          ),
          settings.showNsfwPosts.asOnOffSwitch(
            { preferences.showNsfwPosts },
            {
              preferences.showNsfwPosts = it
            },
          ),
        ),
      ),

      SettingModelItem.SubgroupItem(
        getString(R.string.infinity),
        listOf(
          settings.infinity.asOnOffSwitch(
            { preferences.infinity },
            {
              preferences.infinity = it
            },
          ),
          settings.autoLoadMorePosts.asOnOffSwitch(
            { preferences.autoLoadMorePosts },
            {
              preferences.autoLoadMorePosts = it
            },
          ),
          settings.infinityPageIndicator.asOnOffSwitch(
            { preferences.infinityPageIndicator },
            {
              preferences.infinityPageIndicator = it
            },
          ),
        ),
      ),
    )
  }
}
