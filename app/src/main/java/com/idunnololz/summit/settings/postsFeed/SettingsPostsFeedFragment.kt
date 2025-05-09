package com.idunnololz.summit.settings.postsFeed

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.google.android.material.snackbar.Snackbar
import com.idunnololz.summit.R
import com.idunnololz.summit.account.fullName
import com.idunnololz.summit.databinding.FragmentSettingsPostsFeedBinding
import com.idunnololz.summit.filterLists.ContentTypes
import com.idunnololz.summit.filterLists.FilterTypes
import com.idunnololz.summit.lemmy.communityPicker.CommunityPickerDialogFragment
import com.idunnololz.summit.lemmy.idToSortOrder
import com.idunnololz.summit.lemmy.toApiSortOrder
import com.idunnololz.summit.lemmy.toId
import com.idunnololz.summit.preferences.Preferences
import com.idunnololz.summit.settings.PostsFeedSettings
import com.idunnololz.summit.settings.PreferencesViewModel
import com.idunnololz.summit.settings.SettingPath.getPageName
import com.idunnololz.summit.settings.SettingsFragment
import com.idunnololz.summit.settings.dialogs.MultipleChoiceDialogFragment
import com.idunnololz.summit.settings.dialogs.SettingValueUpdateCallback
import com.idunnololz.summit.settings.util.bindTo
import com.idunnololz.summit.user.UserCommunitiesManager
import com.idunnololz.summit.util.BaseFragment
import com.idunnololz.summit.util.ext.navigateSafe
import com.idunnololz.summit.util.ext.showAllowingStateLoss
import com.idunnololz.summit.util.getParcelableCompat
import com.idunnololz.summit.util.insetViewExceptBottomAutomaticallyByMargins
import com.idunnololz.summit.util.insetViewExceptTopAutomaticallyByPadding
import com.idunnololz.summit.util.setupForFragment
import com.idunnololz.summit.util.setupToolbar
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.launch

@AndroidEntryPoint
class SettingsPostsFeedFragment :
  BaseFragment<FragmentSettingsPostsFeedBinding>(),
  SettingValueUpdateCallback {

  private val args: SettingsPostsFeedFragmentArgs by navArgs()
  private val preferencesViewModel: PreferencesViewModel by viewModels()

  lateinit var preferences: Preferences

  @Inject
  lateinit var settings: PostsFeedSettings

  @Inject
  lateinit var userCommunitiesManager: UserCommunitiesManager

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    preferences = preferencesViewModel.getPreferences(args.account)
  }

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?,
  ): View {
    super.onCreateView(inflater, container, savedInstanceState)

    requireSummitActivity().apply {
      setupForFragment<SettingsFragment>()
    }

    setBinding(FragmentSettingsPostsFeedBinding.inflate(inflater, container, false))

    return binding.root
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)

    val context = requireContext()

    requireSummitActivity().apply {
      setupForFragment<SettingsFragment>()
      insetViewExceptTopAutomaticallyByPadding(viewLifecycleOwner, binding.scrollView)
      insetViewExceptBottomAutomaticallyByMargins(viewLifecycleOwner, binding.toolbar)

      setupToolbar(binding.toolbar, settings.getPageName(context), args.account?.fullName)

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

            updateRendering()

            Snackbar.make(
              binding.coordinatorLayout,
              R.string.home_page_set,
              Snackbar.LENGTH_LONG,
            ).show()
          }
        }
      }
    }

    updateRendering()
  }

  private fun updateRendering() {
    val context = context ?: return

    settings.settingsPostFeedAppearance.bindTo(
      binding.postsFeedAppearance,
    ) {
      val direction = SettingsPostsFeedFragmentDirections
        .actionSettingsContentFragmentToSettingViewTypeFragment()
      findNavController().navigateSafe(direction)
    }
    settings.infinity.bindTo(
      binding.infinity,
      { preferences.infinity },
      {
        preferences.infinity = it
      },
    )
    settings.markPostsAsReadOnScroll.bindTo(
      binding.markPostsAsReadOnScroll,
      { preferences.markPostsAsReadOnScroll },
      {
        preferences.markPostsAsReadOnScroll = it
      },
    )
    settings.blurNsfwPosts.bindTo(
      binding.blurNsfwPosts,
      { preferences.blurNsfwPosts },
      {
        preferences.blurNsfwPosts = it
      },
    )
    settings.postScores.bindTo(
      binding.postScores,
      {
        if (preferences.hidePostScores) {
          R.id.hide_scores
        } else if (preferences.postShowUpAndDownVotes) {
          R.id.show_up_and_down_votes
        } else {
          R.id.show_scores
        }
      },
      { setting, currentValue ->
        MultipleChoiceDialogFragment.newInstance(setting, currentValue)
          .showAllowingStateLoss(childFragmentManager, "aaaaaaa")
      },
    )
    settings.defaultCommunitySortOrder.bindTo(
      binding.defaultCommunitySortOrder,
      {
        preferences.defaultCommunitySortOrder?.toApiSortOrder()?.toId()
          ?: R.id.community_sort_order_default
      },
      { setting, currentValue ->
        MultipleChoiceDialogFragment.newInstance(setting, currentValue)
          .showAllowingStateLoss(childFragmentManager, "aaaaaaa")
      },
    )

    if (args.account != null) {
      binding.keywordFilters.root.visibility = View.GONE
      binding.instanceFilters.root.visibility = View.GONE
      binding.communityFilters.root.visibility = View.GONE
      binding.userFilters.root.visibility = View.GONE
    } else {
      binding.keywordFilters.root.visibility = View.VISIBLE
      binding.instanceFilters.root.visibility = View.VISIBLE
      binding.communityFilters.root.visibility = View.VISIBLE
      binding.userFilters.root.visibility = View.VISIBLE
    }

    settings.keywordFilters.bindTo(binding.keywordFilters) {
      val direction = SettingsPostsFeedFragmentDirections
        .actionSettingsContentFragmentToSettingsFilterListFragment(
          ContentTypes.PostListType,
          FilterTypes.KeywordFilter,
          getString(R.string.keyword_filters),
        )
      findNavController().navigateSafe(direction)
    }
    settings.instanceFilters.bindTo(binding.instanceFilters) {
      val direction = SettingsPostsFeedFragmentDirections
        .actionSettingsContentFragmentToSettingsFilterListFragment(
          ContentTypes.PostListType,
          FilterTypes.InstanceFilter,
          getString(R.string.instance_filters),
        )
      findNavController().navigateSafe(direction)
    }
    settings.communityFilters.bindTo(binding.communityFilters) {
      val direction = SettingsPostsFeedFragmentDirections
        .actionSettingsContentFragmentToSettingsFilterListFragment(
          ContentTypes.PostListType,
          FilterTypes.CommunityFilter,
          getString(R.string.community_filters),
        )
      findNavController().navigateSafe(direction)
    }
    settings.userFilters.bindTo(binding.userFilters) {
      val direction = SettingsPostsFeedFragmentDirections
        .actionSettingsContentFragmentToSettingsFilterListFragment(
          ContentTypes.PostListType,
          FilterTypes.UserFilter,
          getString(R.string.user_filters),
        )
      findNavController().navigateSafe(direction)
    }

    settings.showLinkPosts.bindTo(
      binding.showLinkPosts,
      { preferences.showLinkPosts },
      {
        preferences.showLinkPosts = it
      },
    )
    settings.showImagePosts.bindTo(
      binding.showImagePosts,
      { preferences.showImagePosts },
      {
        preferences.showImagePosts = it
      },
    )
    settings.showVideoPosts.bindTo(
      binding.showVideoPosts,
      { preferences.showVideoPosts },
      {
        preferences.showVideoPosts = it
      },
    )
    settings.showTextPosts.bindTo(
      binding.showTextPosts,
      { preferences.showTextPosts },
      {
        preferences.showTextPosts = it
      },
    )
    settings.showNsfwPosts.bindTo(
      binding.showNsfwPosts,
      { preferences.showNsfwPosts },
      {
        preferences.showNsfwPosts = it
      },
    )
    settings.viewImageOnSingleTap.bindTo(
      binding.viewImageOnSingleTap,
      { preferences.postListViewImageOnSingleTap },
      {
        preferences.postListViewImageOnSingleTap = it
      },
    )
    settings.lockBottomBar.bindTo(
      binding.lockBottomBar,
      { preferences.lockBottomBar },
      {
        preferences.lockBottomBar = it
      },
    )
    settings.autoLoadMorePosts.bindTo(
      binding.autoLoadMorePosts,
      { preferences.autoLoadMorePosts },
      {
        preferences.autoLoadMorePosts = it
      },
    )
    settings.infinityPageIndicator.bindTo(
      binding.infinityPageIndicator,
      { preferences.infinityPageIndicator },
      {
        preferences.infinityPageIndicator = it
      },
    )
    settings.showPostUpvotePercentage.bindTo(
      binding.showPostUpvotePercentage,
      { preferences.showPostUpvotePercentage },
      {
        preferences.showPostUpvotePercentage = it
      },
    )
    settings.useMultilinePostHeaders.bindTo(
      binding.useMultilinePostHeaders,
      { preferences.useMultilinePostHeaders },
      {
        preferences.useMultilinePostHeaders = it
      },
    )
    settings.showFilteredPosts.bindTo(
      binding.showFilteredPosts,
      { preferences.showFilteredPosts },
      {
        preferences.showFilteredPosts = it
      },
    )
    settings.prefetchPosts.bindTo(
      binding.prefetchPosts,
      { preferences.prefetchPosts },
      {
        preferences.prefetchPosts = it
      },
    )
    settings.homeFabQuickAction.bindTo(
      binding.homeFabQuickAction,
      { preferences.homeFabQuickAction },
      { setting, currentValue ->
        MultipleChoiceDialogFragment.newInstance(setting, currentValue)
          .showAllowingStateLoss(childFragmentManager, "homeFabQuickAction")
      },
    )
    settings.parseMarkdownInPostTitles.bindTo(
      binding.parseMarkdownInPostTitles,
      { preferences.parseMarkdownInPostTitles },
      {
        preferences.parseMarkdownInPostTitles = it
      },
    )
    settings.homePage.bindTo(
      binding.homePage,
      {
        userCommunitiesManager.defaultCommunity.value.getLocalizedFullName(context)
      },
      { setting, currentValue ->
        CommunityPickerDialogFragment.show(childFragmentManager, showFeeds = true)
      },
    )
    settings.postFeedShowScrollBar.bindTo(
      binding.showScrollBar,
      { preferences.postFeedShowScrollBar },
      {
        preferences.postFeedShowScrollBar = it
      },
    )
    settings.hideDuplicatePostsOnRead.bindTo(
      binding.hideDuplicatePostsOnRead,
      { preferences.hideDuplicatePostsOnRead },
      {
        preferences.hideDuplicatePostsOnRead = it
      },
    )
    settings.usePostsFeedHeader.bindTo(
      binding.usePostsFeedHeader,
      { preferences.usePostsFeedHeader },
      {
        preferences.usePostsFeedHeader = it
      },
    )
    settings.customizePostsInFeedQuickActions.bindTo(
      binding.customizePostsInFeedQuickActions,
      {
        val direction = SettingsPostsFeedFragmentDirections
          .actionSettingsContentFragmentToPostsInFeedQuickActionsFragment()
        findNavController().navigateSafe(direction)
      },
    )
    settings.openLinkWhenThumbnailTapped.bindTo(
      binding.openLinkWhenThumbnailTapped,
      { preferences.openLinkWhenThumbnailTapped },
      {
        preferences.openLinkWhenThumbnailTapped = it
      },
    )
    settings.showPostType.bindTo(
      binding.showPostType,
      { preferences.showPostType },
      {
        preferences.showPostType = it
      },
    )
  }

  override fun updateValue(key: Int, value: Any?) {
    when (key) {
      settings.defaultCommunitySortOrder.id -> {
        preferences.defaultCommunitySortOrder = idToSortOrder(value as Int)
      }
      settings.homeFabQuickAction.id -> {
        preferences.homeFabQuickAction = value as Int
      }
      settings.postScores.id -> {
        when (value as Int) {
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
      }
    }

    updateRendering()
  }
}
