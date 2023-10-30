package com.idunnololz.summit.settings.postList

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import com.idunnololz.summit.R
import com.idunnololz.summit.databinding.FragmentSettingsContentBinding
import com.idunnololz.summit.filterLists.ContentTypes
import com.idunnololz.summit.filterLists.FilterTypes
import com.idunnololz.summit.lemmy.idToSortOrder
import com.idunnololz.summit.lemmy.toApiSortOrder
import com.idunnololz.summit.lemmy.toId
import com.idunnololz.summit.preferences.Preferences
import com.idunnololz.summit.settings.PostListSettings
import com.idunnololz.summit.settings.SettingPath.getPageName
import com.idunnololz.summit.settings.SettingsFragment
import com.idunnololz.summit.settings.cache.SettingCacheFragment
import com.idunnololz.summit.settings.dialogs.MultipleChoiceDialogFragment
import com.idunnololz.summit.settings.dialogs.SettingValueUpdateCallback
import com.idunnololz.summit.settings.util.bindTo
import com.idunnololz.summit.util.BaseFragment
import com.idunnololz.summit.util.ext.navigateSafe
import com.idunnololz.summit.util.ext.showAllowingStateLoss
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class SettingsPostListFragment :
    BaseFragment<FragmentSettingsContentBinding>(),
    SettingValueUpdateCallback {

    @Inject
    lateinit var preferences: Preferences

    @Inject
    lateinit var settings: PostListSettings

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        super.onCreateView(inflater, container, savedInstanceState)

        requireMainActivity().apply {
            setupForFragment<SettingCacheFragment>()
        }

        setBinding(FragmentSettingsContentBinding.inflate(inflater, container, false))

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val context = requireContext()

        requireMainActivity().apply {
            setupForFragment<SettingsFragment>()
            insetViewExceptTopAutomaticallyByPadding(viewLifecycleOwner, binding.scrollView)
            insetViewExceptBottomAutomaticallyByMargins(viewLifecycleOwner, binding.toolbar)

            setSupportActionBar(binding.toolbar)

            supportActionBar?.setDisplayShowHomeEnabled(true)
            supportActionBar?.setDisplayHomeAsUpEnabled(true)
            supportActionBar?.title = settings.getPageName(context)
        }

        updateRendering()
    }

    private fun updateRendering() {
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
        settings.hidePostScores.bindTo(
            binding.hidePostScores,
            { preferences.hidePostScores },
            {
                preferences.hidePostScores = it
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

        settings.keywordFilters.bindTo(binding.keywordFilters) {
            val direction = SettingsPostListFragmentDirections
                .actionSettingsContentFragmentToSettingsFilterListFragment(
                    ContentTypes.PostListType,
                    FilterTypes.KeywordFilter,
                    getString(R.string.keyword_filters),
                )
            findNavController().navigateSafe(direction)
        }
        settings.instanceFilters.bindTo(binding.instanceFilters) {
            val direction = SettingsPostListFragmentDirections
                .actionSettingsContentFragmentToSettingsFilterListFragment(
                    ContentTypes.PostListType,
                    FilterTypes.InstanceFilter,
                    getString(R.string.instance_filters),
                )
            findNavController().navigateSafe(direction)
        }
        settings.communityFilters.bindTo(binding.communityFilters) {
            val direction = SettingsPostListFragmentDirections
                .actionSettingsContentFragmentToSettingsFilterListFragment(
                    ContentTypes.PostListType,
                    FilterTypes.CommunityFilter,
                    getString(R.string.community_filters),
                )
            findNavController().navigateSafe(direction)
        }
        settings.userFilters.bindTo(binding.userFilters) {
            val direction = SettingsPostListFragmentDirections
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

        settings.compatibilityMode.bindTo(
            binding.compatibilityMode,
            { preferences.compatibilityMode },
            {
                preferences.compatibilityMode = it
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
    }

    override fun updateValue(key: Int, value: Any?) {
        when (key) {
            settings.defaultCommunitySortOrder.id -> {
                preferences.defaultCommunitySortOrder = idToSortOrder(value as Int)
            }
        }

        updateRendering()
    }
}
