package com.idunnololz.summit.settings

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
import com.idunnololz.summit.api.dto.Community
import com.idunnololz.summit.api.dto.Person
import com.idunnololz.summit.api.dto.Post
import com.idunnololz.summit.api.dto.PostAggregates
import com.idunnololz.summit.api.dto.PostView
import com.idunnololz.summit.api.dto.SubscribedType
import com.idunnololz.summit.databinding.FragmentSettingViewTypeBinding
import com.idunnololz.summit.databinding.ListingItemCardBinding
import com.idunnololz.summit.databinding.ListingItemCompactBinding
import com.idunnololz.summit.databinding.ListingItemFullBinding
import com.idunnololz.summit.databinding.ListingItemListBinding
import com.idunnololz.summit.lemmy.community.CommunityLayout
import com.idunnololz.summit.lemmy.post_view.ListingItemViewHolder
import com.idunnololz.summit.lemmy.post_view.PostViewBuilder
import com.idunnololz.summit.preferences.Preferences
import com.idunnololz.summit.util.BaseFragment
import com.idunnololz.summit.util.Utils.ANIMATION_DURATION_MS
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class SettingViewTypeFragment : BaseFragment<FragmentSettingViewTypeBinding>(),
AlertDialogFragment.AlertDialogFragmentListener {

    private val fakePostView = PostView(
        post = Post(
            id = 10,
            name = "Example post",
            url = "https://lol-catalyst-data.s3.amazonaws.com/manual_uploads/sencha.jpg",
            body = "This is my cat Sencha. Isn't he cute? :D",
            creator_id = 1,
            community_id = 1,
            removed = false,
            locked = false,
            published = "2023-06-30T07:11:18Z",
            updated = null,
            deleted = false,
            nsfw = false,
            embed_title = null,
            embed_description = null,
            thumbnail_url = null,
            ap_id = "http://meme.idunnololz.com",
            local = true,
            embed_video_url = null,
            language_id = 1,
            featured_community = false,
            featured_local = false,
        ),
        creator = Person(
            id = 1,
            name = "idunnololz",
            display_name = "idunnololz",
            avatar = "null",
            banned = false,
            published = "2023-06-30T07:11:18Z",
            updated = null,
            actor_id = "http://meme.idunnololz.com",
            bio = null,
            local = false,
            banner = null,
            deleted = false,
            matrix_user_id = null,
            admin = false,
            bot_account = false,
            ban_expires = null,
            instance_id = 1
        ),
        community = Community(
            id = 0,
            name = "summit",
            title = "summit",
            description = null,
            removed = false,
            published = "2023-06-30T07:11:18Z",
            updated = null,
            deleted = false,
            nsfw = false,
            actor_id = "asdf",
            local = true,
            icon = null,
            banner = null,
            hidden = false,
            posting_restricted_to_mods = false,
            instance_id = 1,
        ),
        creator_banned_from_community = false,
        counts = PostAggregates(
            id = 1,
            post_id = 1,
            comments = 1,
            score = 420,
            upvotes = 489,
            downvotes = 69,
            published = "2023-06-30T07:11:18Z",
            newest_comment_time_necro = "2023-06-30T07:11:18Z",
            newest_comment_time = "2023-06-30T07:11:18Z",
            featured_community = false,
            featured_local = false,
        ),
        subscribed = SubscribedType.NotSubscribed,
        saved = true,
        read = true,
        creator_blocked = false,
        my_vote = null,
        unread_comments = 0,
    )

    private val viewModel: SettingViewTypeViewModel by viewModels()

    @Inject
    lateinit var preferences: Preferences

    @Inject
    lateinit var postViewBuilder: PostViewBuilder

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
            insetViewAutomaticallyByPadding(viewLifecycleOwner, binding.root)

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
            R.id.setting_base_view_type,
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
            R.id.setting_text_scaling,
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
            R.id.setting_prefer_image_at_the_end,
            getString(R.string.prefer_image_at_the_end),
            null,
            false,
        ).bindTo(
            binding.preferImageAtTheEnd,
            { viewModel.currentPostUiConfig.preferImagesAtEnd },
            {
                viewModel.currentPostUiConfig =
                    viewModel.currentPostUiConfig.copy(preferImagesAtEnd = it)

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

        postViewBuilder.postUiConfig = viewModel.currentPostUiConfig

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
            postViewBuilder.recycle(lastH)
        }
        binding.demoViewContainer.removeAllViews()
        binding.demoViewContainer.addView(h.root)

        postViewBuilder.bind(
            holder = h,
            container = binding.demoViewContainer,
            postView = fakePostView,
            instance = "https://fake.instance",
            isRevealed = true,
            contentMaxWidth = binding.demoViewContainer.width,
            viewLifecycleOwner = viewLifecycleOwner,
            isExpanded = false,
            isActionsExpanded = false,
            updateContent = true,
            highlight = false,
            highlightForever = false,
            onRevealContentClickedFn = {},
            onImageClick = {},
            onVideoClick = { _, _, _ -> },
            onPageClick = {},
            onItemClick = { _, _, _, _, _, _, _ -> },
            onShowMoreOptions = {},
            toggleItem = { _, _ -> },
            toggleActions = { _, _ -> },
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