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
import com.idunnololz.summit.settings.BasicSettingItem
import com.idunnololz.summit.settings.OnOffSettingItem
import com.idunnololz.summit.settings.PostListSettings
import com.idunnololz.summit.settings.SettingsFragment
import com.idunnololz.summit.settings.cache.SettingCacheFragment
import com.idunnololz.summit.settings.dialogs.MultipleChoiceDialogFragment
import com.idunnololz.summit.settings.dialogs.SettingValueUpdateCallback
import com.idunnololz.summit.settings.ui.bindTo
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
    lateinit var postListSettings: PostListSettings

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
            supportActionBar?.title = context.getString(R.string.post_list)
        }

        updateRendering()
    }

    private fun updateRendering() {
        val context = requireContext()

        OnOffSettingItem(
            null,
            getString(R.string.infinity),
            getString(R.string.no_your_limits),
        ).bindTo(
            binding.infinity,
            { preferences.infinity },
            {
                preferences.infinity = it

                updateRendering()
            },
        )
        OnOffSettingItem(
            null,
            getString(R.string.mark_posts_as_read_on_scroll),
            getString(R.string.mark_posts_as_read_on_scroll_desc),
        ).bindTo(
            binding.markPostsAsReadOnScroll,
            { preferences.markPostsAsReadOnScroll },
            {
                preferences.markPostsAsReadOnScroll = it

                updateRendering()
            },
        )
        OnOffSettingItem(
            null,
            getString(R.string.blur_nsfw_posts),
            null,
        ).bindTo(
            binding.blurNsfwPosts,
            { preferences.blurNsfwPosts },
            {
                preferences.blurNsfwPosts = it

                updateRendering()
            },
        )
        postListSettings.defaultCommunitySortOrder.bindTo(
            binding.defaultCommunitySortOrder,
            {
                preferences.defaultCommunitySortOrder?.toApiSortOrder()?.toId()
                    ?: R.id.community_sort_order_default
            },
            {
                MultipleChoiceDialogFragment.newInstance(it)
                    .showAllowingStateLoss(childFragmentManager, "aaaaaaa")
            },
        )

        BasicSettingItem(
            null,
            getString(R.string.keyword_filters),
            getString(R.string.keyword_filters_desc),
        ).bindTo(binding.keywordFilters) {
            val direction = SettingsPostListFragmentDirections
                .actionSettingsContentFragmentToSettingsFilterListFragment(
                    ContentTypes.PostListType,
                    FilterTypes.KeywordFilter,
                    getString(R.string.keyword_filters),
                )
            findNavController().navigateSafe(direction)
        }
        BasicSettingItem(
            null,
            getString(R.string.instance_filters),
            getString(R.string.instance_filters_desc),
        ).bindTo(binding.instanceFilters) {
            val direction = SettingsPostListFragmentDirections
                .actionSettingsContentFragmentToSettingsFilterListFragment(
                    ContentTypes.PostListType,
                    FilterTypes.InstanceFilter,
                    getString(R.string.instance_filters),
                )
            findNavController().navigateSafe(direction)
        }
        BasicSettingItem(
            null,
            getString(R.string.community_filters),
            getString(R.string.community_filters_desc),
        ).bindTo(binding.communityFilters) {
            val direction = SettingsPostListFragmentDirections
                .actionSettingsContentFragmentToSettingsFilterListFragment(
                    ContentTypes.PostListType,
                    FilterTypes.CommunityFilter,
                    getString(R.string.community_filters),
                )
            findNavController().navigateSafe(direction)
        }
        BasicSettingItem(
            null,
            getString(R.string.user_filters),
            getString(R.string.user_filters_desc),
        ).bindTo(binding.userFilters) {
            val direction = SettingsPostListFragmentDirections
                .actionSettingsContentFragmentToSettingsFilterListFragment(
                    ContentTypes.PostListType,
                    FilterTypes.UserFilter,
                    getString(R.string.user_filters),
                )
            findNavController().navigateSafe(direction)
        }

        OnOffSettingItem(
            R.drawable.baseline_link_24,
            getString(R.string.show_link_posts),
            null,
        ).bindTo(
            binding.showLinkPosts,
            { preferences.showLinkPosts },
            {
                preferences.showLinkPosts = it

                updateRendering()
            },
        )
        OnOffSettingItem(
            R.drawable.baseline_image_24,
            getString(R.string.show_image_posts),
            null,
        ).bindTo(
            binding.showImagePosts,
            { preferences.showImagePosts },
            {
                preferences.showImagePosts = it

                updateRendering()
            },
        )
        OnOffSettingItem(
            R.drawable.baseline_videocam_24,
            getString(R.string.show_video_posts),
            null,
        ).bindTo(
            binding.showVideoPosts,
            { preferences.showVideoPosts },
            {
                preferences.showVideoPosts = it

                updateRendering()
            },
        )
        OnOffSettingItem(
            R.drawable.baseline_text_fields_24,
            getString(R.string.show_text_posts),
            null,
        ).bindTo(
            binding.showTextPosts,
            { preferences.showTextPosts },
            {
                preferences.showTextPosts = it

                updateRendering()
            },
        )
        OnOffSettingItem(
            R.drawable.ic_nsfw_24,
            getString(R.string.show_nsfw_posts),
            null,
        ).bindTo(
            binding.showNsfwPosts,
            { preferences.showNsfwPosts },
            {
                preferences.showNsfwPosts = it

                updateRendering()
            },
        )

        postListSettings.viewImageOnSingleTap.bindTo(
            binding.viewImageOnSingleTap,
            { preferences.postListViewImageOnSingleTap },
            {
                preferences.postListViewImageOnSingleTap = it
            },
        )

        postListSettings.compatibilityMode.bindTo(
            binding.compatibilityMode,
            { preferences.compatibilityMode },
            {
                preferences.compatibilityMode = it
            }
        )
    }

    override fun updateValue(key: Int, value: Any?) {
        when (key) {
            postListSettings.defaultCommunitySortOrder.id -> {
                preferences.defaultCommunitySortOrder = idToSortOrder(value as Int)
            }
        }

        updateRendering()
    }
}
