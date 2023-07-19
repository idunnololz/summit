package com.idunnololz.summit.settings.view_type

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
import com.idunnololz.summit.databinding.ListingItemCardBinding
import com.idunnololz.summit.databinding.ListingItemCompactBinding
import com.idunnololz.summit.databinding.ListingItemFullBinding
import com.idunnololz.summit.databinding.ListingItemListBinding
import com.idunnololz.summit.lemmy.community.CommunityLayout
import com.idunnololz.summit.lemmy.postListView.ListingItemViewHolder
import com.idunnololz.summit.lemmy.postListView.PostListViewBuilder
import com.idunnololz.summit.preferences.Preferences
import com.idunnololz.summit.settings.LemmyFakeModels
import com.idunnololz.summit.settings.OnOffSettingItem
import com.idunnololz.summit.settings.SettingsFragment
import com.idunnololz.summit.settings.SliderSettingItem
import com.idunnololz.summit.settings.TextOnlySettingItem
import com.idunnololz.summit.settings.ui.bindTo
import com.idunnololz.summit.util.BaseFragment
import com.idunnololz.summit.util.Utils.ANIMATION_DURATION_MS
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class SettingViewTypeFragment : BaseFragment<FragmentSettingViewTypeBinding>(),
AlertDialogFragment.AlertDialogFragmentListener {

    private val viewModel: SettingViewTypeViewModel by viewModels()

    @Inject
    lateinit var preferences: Preferences

    @Inject
    lateinit var postListViewBuilder: PostListViewBuilder

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
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
            supportActionBar?.title = context.getString(R.string.view_type)
        }

        binding.root.viewTreeObserver.addOnPreDrawListener(object :
            ViewTreeObserver.OnPreDrawListener {
            override fun onPreDraw(): Boolean {
                binding.root.viewTreeObserver.removeOnPreDrawListener(this)

                setup()
                return false
            }

        })

    }

    private fun setup() {
        if (!isBindingAvailable()) return

        val context = requireContext()
        val parentActivity = requireMainActivity()

        updateRendering()

        binding.reset.setOnClickListener {
            AlertDialogFragment.Builder()
                .setMessage(R.string.reset_view_to_default_styles)
                .setPositiveButton(android.R.string.ok)
                .setNegativeButton(R.string.cancel)
                .createAndShow(childFragmentManager, "reset_view_to_default_styles")

        }

        TextOnlySettingItem(
            getString(R.string.base_view_type),
            ""
        ).bindTo(activity = parentActivity,
            b = binding.viewTypeSetting,
            choices = mapOf(
                CommunityLayout.Compact to getString(R.string.compact),
                CommunityLayout.List to getString(R.string.list),
                CommunityLayout.Card to getString(R.string.card),
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
            }
        )

        bindPostUiSettings()

        viewModel.onPostUiChanged.observe(viewLifecycleOwner) {
            updateRendering()
            bindPostUiSettings()
        }
    }

    private fun bindPostUiSettings() {
        SliderSettingItem(
            getString(R.string.font_size),
            0.2f,
            3f,
        ).bindTo(
            binding.textScalingSetting,
            { viewModel.currentPostUiConfig.textSizeMultiplier },
            {
                viewModel.currentPostUiConfig =
                    viewModel.currentPostUiConfig.updateTextSizeMultiplier(it)

                updateRendering()
            }
        )
        OnOffSettingItem(
            getString(R.string.prefer_image_at_the_end),
            null,
        ).bindTo(
            binding.preferImageAtTheEnd,
            { viewModel.currentPostUiConfig.preferImagesAtEnd },
            {
                viewModel.currentPostUiConfig =
                    viewModel.currentPostUiConfig.copy(preferImagesAtEnd = it)

                updateRendering()
            }
        )
        OnOffSettingItem(
            getString(R.string.prefer_full_size_image),
            null,
        ).bindTo(
            binding.preferFullSizeImage,
            { viewModel.currentPostUiConfig.preferFullSizeImages },
            {
                viewModel.currentPostUiConfig =
                    viewModel.currentPostUiConfig.copy(preferFullSizeImages = it)

                updateRendering()
            }
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
            }
        )

        postListViewBuilder.postUiConfig = viewModel.currentPostUiConfig

        val context = requireContext()
        val inflater = LayoutInflater.from(context)

        val h = when (preferences.getPostsLayout()) {
            CommunityLayout.Compact ->
                ListingItemViewHolder.fromBinding(
                    ListingItemCompactBinding.inflate(inflater, binding.demoViewContainer, true)
                )
            CommunityLayout.List ->
                ListingItemViewHolder.fromBinding(
                    ListingItemListBinding.inflate(inflater, binding.demoViewContainer, false)
                )
            CommunityLayout.Card ->
                ListingItemViewHolder.fromBinding(
                    ListingItemCardBinding.inflate(inflater, binding.demoViewContainer, false)
                )
            CommunityLayout.Full ->
                ListingItemViewHolder.fromBinding(
                    ListingItemFullBinding.inflate(inflater, binding.demoViewContainer, false)
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
            onPageClick = {},
            onItemClick = { _, _, _, _, _, _, _ -> },
            onShowMoreOptions = {},
            toggleItem = {},
            toggleActions = {},
            onSignInRequired = {},
            onInstanceMismatch = { _, _ -> },
            onHighlightComplete = {}
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