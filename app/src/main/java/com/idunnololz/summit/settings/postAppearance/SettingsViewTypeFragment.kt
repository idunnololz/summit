package com.idunnololz.summit.settings.postAppearance

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import androidx.fragment.app.viewModels
import androidx.transition.TransitionManager
import com.idunnololz.summit.R
import com.idunnololz.summit.alert.OldAlertDialogFragment
import com.idunnololz.summit.databinding.FragmentSettingsPostsFeedAppearanceBinding
import com.idunnololz.summit.databinding.ListingItemCard2Binding
import com.idunnololz.summit.databinding.ListingItemCard3Binding
import com.idunnololz.summit.databinding.ListingItemCardBinding
import com.idunnololz.summit.databinding.ListingItemCompactBinding
import com.idunnololz.summit.databinding.ListingItemFullBinding
import com.idunnololz.summit.databinding.ListingItemFullWithCardsBinding
import com.idunnololz.summit.databinding.ListingItemLargeListBinding
import com.idunnololz.summit.databinding.ListingItemListBinding
import com.idunnololz.summit.databinding.ListingItemListWithCardsBinding
import com.idunnololz.summit.lemmy.community.CommunityLayout
import com.idunnololz.summit.lemmy.postListView.ListingItemViewHolder
import com.idunnololz.summit.lemmy.postListView.PostListViewBuilder
import com.idunnololz.summit.lemmy.postListView.defaultDimReadPosts
import com.idunnololz.summit.preferences.Preferences
import com.idunnololz.summit.settings.LemmyFakeModels
import com.idunnololz.summit.settings.PostsFeedAppearanceSettings
import com.idunnololz.summit.settings.SettingPath.getPageName
import com.idunnololz.summit.settings.SettingsFragment
import com.idunnololz.summit.settings.util.bindTo
import com.idunnololz.summit.util.BaseFragment
import com.idunnololz.summit.util.insetViewExceptBottomAutomaticallyByMargins
import com.idunnololz.summit.util.insetViewExceptTopAutomaticallyByPadding
import com.idunnololz.summit.util.makeTransition
import com.idunnololz.summit.util.setupForFragment
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class SettingsViewTypeFragment :
    BaseFragment<FragmentSettingsPostsFeedAppearanceBinding>(),
    OldAlertDialogFragment.AlertDialogFragmentListener {

    private val viewModel: SettingsViewTypeViewModel by viewModels()

    @Inject
    lateinit var preferences: Preferences

    @Inject
    lateinit var postListViewBuilder: PostListViewBuilder

    @Inject
    lateinit var settings: PostsFeedAppearanceSettings

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        super.onCreateView(inflater, container, savedInstanceState)

        setBinding(FragmentSettingsPostsFeedAppearanceBinding.inflate(inflater, container, false))

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val context = requireContext()

        requireSummitActivity().apply {
            setupForFragment<SettingsFragment>()
            insetViewExceptTopAutomaticallyByPadding(viewLifecycleOwner, binding.scrollView)
            insetViewExceptBottomAutomaticallyByMargins(viewLifecycleOwner, binding.toolbar)

            setSupportActionBar(binding.toolbar)

            supportActionBar?.setDisplayShowHomeEnabled(true)
            supportActionBar?.setDisplayHomeAsUpEnabled(true)
            supportActionBar?.title = settings.getPageName(context)
        }

        binding.root.viewTreeObserver.addOnPreDrawListener(
            object :
                ViewTreeObserver.OnPreDrawListener {
                override fun onPreDraw(): Boolean {
                    binding.root.viewTreeObserver.removeOnPreDrawListener(this)

                    setup()
                    return false
                }
            },
        )
    }

    private fun setup() {
        if (!isBindingAvailable()) return

        val parentActivity = requireSummitActivity()

        updateRendering()

        binding.reset.setOnClickListener {
            OldAlertDialogFragment.Builder()
                .setMessage(R.string.reset_view_to_default_styles)
                .setPositiveButton(android.R.string.ok)
                .setNegativeButton(R.string.cancel)
                .createAndShow(childFragmentManager, "reset_view_to_default_styles")
        }

        settings.baseViewType.bindTo(
            activity = parentActivity,
            b = binding.viewTypeSetting,
            choices = mapOf(
                CommunityLayout.Compact to getString(R.string.compact),
                CommunityLayout.List to getString(R.string.list),
                CommunityLayout.LargeList to getString(R.string.large_list),
                CommunityLayout.Card to getString(R.string.card),
                CommunityLayout.Card2 to getString(R.string.card2),
                CommunityLayout.Card3 to getString(R.string.card3),
                CommunityLayout.Full to getString(R.string.full),
                CommunityLayout.ListWithCards to getString(R.string.list_with_cards),
                CommunityLayout.FullWithCards to getString(R.string.full_with_cards),
            ),
            getCurrentChoice = {
                preferences.getPostsLayout()
            },
            onChoiceSelected = {
                viewModel.onLayoutChanging()
                preferences.setPostsLayout(it)
                viewModel.onLayoutChanged()

                updateRendering()
            },
        )

        bindPostUiSettings()

        viewModel.onPostUiChanged.observe(viewLifecycleOwner) {
            updateRendering()
            bindPostUiSettings()
        }
    }

    private fun bindPostUiSettings() {
        settings.fontSize.bindTo(
            binding.textScalingSetting,
            { viewModel.currentPostUiConfig.textSizeMultiplier },
            {
                viewModel.currentPostUiConfig =
                    viewModel.currentPostUiConfig.updateTextSizeMultiplier(it)

                updateRendering()
            },
        )
        settings.imageSize.bindTo(
            binding.imageScalingSetting,
            { viewModel.currentPostUiConfig.imageWidthPercent },
            {
                viewModel.currentPostUiConfig =
                    viewModel.currentPostUiConfig.copy(
                        imageWidthPercent = it,
                    )

                updateRendering()
            },
        )
        when (preferences.getPostsLayout()) {
            CommunityLayout.Card,
            CommunityLayout.Card2,
            CommunityLayout.Card3,
            CommunityLayout.ListWithCards,
            CommunityLayout.FullWithCards,
            -> {
                binding.horizontalMarginSizeSetting.root.visibility = View.VISIBLE
                settings.horizontalMarginSize.bindTo(
                    binding.horizontalMarginSizeSetting,
                    { viewModel.currentPostUiConfig.horizontalMarginDp ?: 16f },
                    {
                        viewModel.currentPostUiConfig =
                            viewModel.currentPostUiConfig.copy(
                                horizontalMarginDp = it,
                            )

                        updateRendering()
                    },
                )
            }
            else -> {
                binding.horizontalMarginSizeSetting.root.visibility = View.GONE
            }
        }
        settings.preferImageAtEnd.bindTo(
            binding.preferImageAtTheEnd,
            { viewModel.currentPostUiConfig.preferImagesAtEnd },
            {
                viewModel.currentPostUiConfig =
                    viewModel.currentPostUiConfig.copy(preferImagesAtEnd = it)

                updateRendering()
            },
        )
        settings.preferFullImage.bindTo(
            binding.preferFullSizeImage,
            { viewModel.currentPostUiConfig.preferFullSizeImages },
            {
                viewModel.currentPostUiConfig =
                    viewModel.currentPostUiConfig.copy(preferFullSizeImages = it)

                updateRendering()
            },
        )
        settings.preferTitleText.bindTo(
            binding.preferTitleText,
            { viewModel.currentPostUiConfig.preferTitleText },
            {
                viewModel.currentPostUiConfig =
                    viewModel.currentPostUiConfig.copy(preferTitleText = it)

                updateRendering()
            },
        )
        settings.preferCommunityIcon.bindTo(
            binding.preferCommunityIcon,
            { viewModel.currentPostUiConfig.showCommunityIcon },
            {
                viewModel.currentPostUiConfig =
                    viewModel.currentPostUiConfig.copy(showCommunityIcon = it)

                updateRendering()
            },
        )
        settings.contentMaxLines.bindTo(
            requireSummitActivity(),
            binding.contentMaxLines,
            choices = mapOf(
                -1 to getString(R.string.no_limit),
                1 to "1",
                2 to "2",
                3 to "3",
                4 to "4",
                5 to "5",
                6 to "6",
                7 to "7",
                8 to "8",
                9 to "9",
            ),
            getCurrentChoice = {
                viewModel.currentPostUiConfig.contentMaxLines
            },
            onChoiceSelected = {
                viewModel.currentPostUiConfig =
                    viewModel.currentPostUiConfig.copy(contentMaxLines = it)

                updateRendering()
            },
        )
        settings.dimReadPosts.bindTo(
            binding.dimReadPosts,
            {
                viewModel.currentPostUiConfig.dimReadPosts
                    ?: preferences.getPostsLayout().defaultDimReadPosts
            },
            {
                viewModel.currentPostUiConfig =
                    viewModel.currentPostUiConfig.copy(dimReadPosts = it)

                updateRendering()
            },
        )
        settings.preferTextPreviewIcon.bindTo(
            binding.preferTextPreviewIcon,
            {
                viewModel.currentPostUiConfig.showTextPreviewIcon
                    ?: true
            },
            {
                viewModel.currentPostUiConfig =
                    viewModel.currentPostUiConfig.copy(showTextPreviewIcon = it)

                updateRendering()
            },
        )
    }

    private val lastH: ListingItemViewHolder? = null
    private fun updateRendering() {
        if (!isBindingAvailable()) return

        TransitionManager.beginDelayedTransition(binding.demoViewContainer, makeTransition())

        binding.demoViewContainer.viewTreeObserver.addOnPreDrawListener(
            object : ViewTreeObserver.OnPreDrawListener {
                override fun onPreDraw(): Boolean {
                    binding.demoViewContainer.viewTreeObserver.removeOnPreDrawListener(this)
                    return true
                }
            },
        )

        postListViewBuilder.postUiConfig = viewModel.currentPostUiConfig

        val context = requireContext()
        val inflater = LayoutInflater.from(context)

        val h = when (preferences.getPostsLayout()) {
            CommunityLayout.Compact ->
                ListingItemViewHolder.fromBinding(
                    ListingItemCompactBinding.inflate(inflater, binding.demoViewContainer, true),
                )
            CommunityLayout.List ->
                ListingItemViewHolder.fromBinding(
                    ListingItemListBinding.inflate(inflater, binding.demoViewContainer, false),
                )
            CommunityLayout.LargeList ->
                ListingItemViewHolder.fromBinding(
                    ListingItemLargeListBinding.inflate(inflater, binding.demoViewContainer, false),
                )
            CommunityLayout.Card ->
                ListingItemViewHolder.fromBinding(
                    ListingItemCardBinding.inflate(inflater, binding.demoViewContainer, false),
                )
            CommunityLayout.Card2 ->
                ListingItemViewHolder.fromBinding(
                    ListingItemCard2Binding.inflate(inflater, binding.demoViewContainer, false),
                )
            CommunityLayout.Card3 ->
                ListingItemViewHolder.fromBinding(
                    ListingItemCard3Binding.inflate(inflater, binding.demoViewContainer, false),
                )
            CommunityLayout.Full ->
                ListingItemViewHolder.fromBinding(
                    ListingItemFullBinding.inflate(inflater, binding.demoViewContainer, false),
                )
            CommunityLayout.ListWithCards ->
                ListingItemViewHolder.fromBinding(
                    ListingItemListWithCardsBinding.inflate(
                        inflater,
                        binding.demoViewContainer,
                        false,
                    ),
                )
            CommunityLayout.FullWithCards ->
                ListingItemViewHolder.fromBinding(
                    ListingItemFullWithCardsBinding.inflate(
                        inflater,
                        binding.demoViewContainer,
                        false,
                    ),
                )
        }

        if (lastH != null) {
            postListViewBuilder.recycle(lastH)
        }
        binding.demoViewContainer.removeAllViews()
        binding.demoViewContainer.addView(h.root)

        postListViewBuilder.bind(
            holder = h,
            fetchedPost = LemmyFakeModels.fakeFetchedPost,
            instance = "https://fake.instance",
            isRevealed = true,
            contentMaxWidth = binding.demoViewContainer.width,
            contentPreferredHeight = binding.demoViewContainer.height,
            viewLifecycleOwner = viewLifecycleOwner,
            isExpanded = false,
            isActionsExpanded = false,
            alwaysRenderAsUnread = true,
            updateContent = true,
            highlight = false,
            highlightForever = false,
            themeColor = null,
            isDuplicatePost = false,
            onRevealContentClickedFn = {},
            onImageClick = { _, _, _, _ -> },
            onVideoClick = { _, _, _ -> },
            onVideoLongClickListener = { _ -> },
            onPageClick = { _, _ -> },
            onItemClick = { _, _, _, _, _, _, _, _ -> },
            onShowMoreOptions = { _, _ -> },
            toggleItem = {},
            toggleActions = {},
            onSignInRequired = {},
            onInstanceMismatch = { _, _ -> },
            onHighlightComplete = {},
            onLinkClick = { _, _, _, _ -> },
            onLinkLongClick = { _, _, _ -> },
        )
    }

    override fun onPositiveClick(dialog: OldAlertDialogFragment, tag: String?) {
        if (tag == "reset_view_to_default_styles") {
            viewModel.resetPostUiConfig()
        }
    }

    override fun onNegativeClick(dialog: OldAlertDialogFragment, tag: String?) {
    }
}
