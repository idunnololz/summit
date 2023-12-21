package com.idunnololz.summit.settings.viewType

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import androidx.fragment.app.viewModels
import androidx.transition.ChangeBounds
import androidx.transition.ChangeClipBounds
import androidx.transition.ChangeImageTransform
import androidx.transition.Fade
import androidx.transition.Fade.IN
import androidx.transition.Fade.OUT
import androidx.transition.Transition
import androidx.transition.TransitionManager
import androidx.transition.TransitionSet
import androidx.transition.TransitionSet.ORDERING_TOGETHER
import com.idunnololz.summit.R
import com.idunnololz.summit.alert.AlertDialogFragment
import com.idunnololz.summit.databinding.FragmentSettingViewTypeBinding
import com.idunnololz.summit.databinding.ListingItemCard2Binding
import com.idunnololz.summit.databinding.ListingItemCard3Binding
import com.idunnololz.summit.databinding.ListingItemCardBinding
import com.idunnololz.summit.databinding.ListingItemCompactBinding
import com.idunnololz.summit.databinding.ListingItemFullBinding
import com.idunnololz.summit.databinding.ListingItemLargeListBinding
import com.idunnololz.summit.databinding.ListingItemListBinding
import com.idunnololz.summit.lemmy.community.CommunityLayout
import com.idunnololz.summit.lemmy.postListView.ListingItemViewHolder
import com.idunnololz.summit.lemmy.postListView.PostListViewBuilder
import com.idunnololz.summit.preferences.Preferences
import com.idunnololz.summit.settings.LemmyFakeModels
import com.idunnololz.summit.settings.SettingPath.getPageName
import com.idunnololz.summit.settings.SettingsFragment
import com.idunnololz.summit.settings.ViewTypeSettings
import com.idunnololz.summit.settings.util.bindTo
import com.idunnololz.summit.util.BaseFragment
import com.idunnololz.summit.util.Utils.ANIMATION_DURATION_MS
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class SettingViewTypeFragment :
    BaseFragment<FragmentSettingViewTypeBinding>(),
    AlertDialogFragment.AlertDialogFragmentListener {

    private val viewModel: SettingViewTypeViewModel by viewModels()

    @Inject
    lateinit var preferences: Preferences

    @Inject
    lateinit var postListViewBuilder: PostListViewBuilder

    @Inject
    lateinit var settings: ViewTypeSettings

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        super.onCreateView(inflater, container, savedInstanceState)

        setBinding(FragmentSettingViewTypeBinding.inflate(inflater, container, false))

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

        val parentActivity = requireMainActivity()

        updateRendering()

        binding.reset.setOnClickListener {
            AlertDialogFragment.Builder()
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
        settings.contentMaxLines.bindTo(
            requireMainActivity(),
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
    }

    private val lastH: ListingItemViewHolder? = null
    private fun updateRendering() {
        if (!isBindingAvailable()) return

        val set: Transition = TransitionSet()
            .addTransition(Fade(IN or OUT))
            .addTransition(ChangeBounds())
            .addTransition(ChangeClipBounds())
            .addTransition(ChangeImageTransform())
            .setOrdering(ORDERING_TOGETHER)
            .setDuration(ANIMATION_DURATION_MS)
        TransitionManager.beginDelayedTransition(binding.demoViewContainer, set)

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
        }

        if (lastH != null) {
            postListViewBuilder.recycle(lastH)
        }
        binding.demoViewContainer.removeAllViews()
        binding.demoViewContainer.addView(h.root)

        postListViewBuilder.bind(
            holder = h,
            postView = LemmyFakeModels.fakePostView,
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
            onRevealContentClickedFn = {},
            onImageClick = { _, _, _ -> },
            onVideoClick = { _, _, _ -> },
            onVideoLongClickListener = { _ -> },
            onPageClick = {},
            onItemClick = { _, _, _, _, _, _, _ -> },
            onShowMoreOptions = {},
            toggleItem = {},
            toggleActions = {},
            onSignInRequired = {},
            onInstanceMismatch = { _, _ -> },
            onHighlightComplete = {},
            onLinkClick = { _, _, _ -> },
            onLinkLongClick = { _, _ -> },
        )
    }

    override fun onPositiveClick(dialog: AlertDialogFragment, tag: String?) {
        if (tag == "reset_view_to_default_styles") {
            viewModel.resetPostUiConfig()
        }
    }

    override fun onNegativeClick(dialog: AlertDialogFragment, tag: String?) {
    }
}
