package com.idunnololz.summit.settings.post_and_comments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import androidx.core.view.updatePaddingRelative
import androidx.fragment.app.viewModels
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView.Adapter
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import androidx.transition.ChangeBounds
import androidx.transition.ChangeClipBounds
import androidx.transition.ChangeImageTransform
import androidx.transition.Fade
import androidx.transition.Transition
import androidx.transition.TransitionManager
import androidx.transition.TransitionSet
import com.idunnololz.summit.R
import com.idunnololz.summit.alert.AlertDialogFragment
import com.idunnololz.summit.api.dto.CommentView
import com.idunnololz.summit.api.dto.PostView
import com.idunnololz.summit.databinding.FragmentSettingPostAndCommentsBinding
import com.idunnololz.summit.databinding.PostCommentExpandedCompactItemBinding
import com.idunnololz.summit.databinding.PostCommentExpandedItemBinding
import com.idunnololz.summit.databinding.PostHeaderItemBinding
import com.idunnololz.summit.lemmy.post.OldThreadLinesDecoration
import com.idunnololz.summit.lemmy.postAndCommentView.CommentExpandedViewHolder
import com.idunnololz.summit.lemmy.postAndCommentView.PostAndCommentViewBuilder
import com.idunnololz.summit.lemmy.postAndCommentView.setupForPostAndComments
import com.idunnololz.summit.lemmy.utils.setupDecoratorsForPostList
import com.idunnololz.summit.preferences.Preferences
import com.idunnololz.summit.settings.LemmyFakeModels
import com.idunnololz.summit.settings.PostAndCommentsSettings
import com.idunnololz.summit.settings.SettingsFragment
import com.idunnololz.summit.settings.SliderSettingItem
import com.idunnololz.summit.settings.dialogs.MultipleChoiceDialogFragment
import com.idunnololz.summit.settings.dialogs.SettingValueUpdateCallback
import com.idunnololz.summit.settings.ui.bindTo
import com.idunnololz.summit.util.BaseFragment
import com.idunnololz.summit.util.PreferenceUtil
import com.idunnololz.summit.util.Utils
import com.idunnololz.summit.util.ext.showAllowingStateLoss
import com.idunnololz.summit.util.recyclerView.AdapterHelper
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class SettingPostAndCommentsFragment : BaseFragment<FragmentSettingPostAndCommentsBinding>(),
    AlertDialogFragment.AlertDialogFragmentListener,
    SettingValueUpdateCallback {

    private val viewModel: SettingPostAndCommentsViewModel by viewModels()

    @Inject
    lateinit var postAndCommentViewBuilder: PostAndCommentViewBuilder

    @Inject
    lateinit var settings: PostAndCommentsSettings

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        super.onCreateView(inflater, container, savedInstanceState)

        setBinding(FragmentSettingPostAndCommentsBinding.inflate(inflater, container, false))

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
            supportActionBar?.title = context.getString(R.string.post_and_comments)
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

    fun setup() {
        if (!isBindingAvailable()) return

        val adapter = FakePostAndCommentsAdapter(
            binding.demoViewContainer,
            postAndCommentViewBuilder,
            binding.demoViewContainer.width,
            viewLifecycleOwner,
            viewModel.preferences,
        )

        val context = requireContext()

        binding.resetPostStyles.setOnClickListener {
            AlertDialogFragment.Builder()
                .setMessage(R.string.reset_view_to_default_styles)
                .setPositiveButton(android.R.string.ok)
                .setNegativeButton(R.string.cancel)
                .createAndShow(childFragmentManager, "reset_post_to_default_styles")
        }

        binding.resetCommentStyles.setOnClickListener {
            AlertDialogFragment.Builder()
                .setMessage(R.string.reset_view_to_default_styles)
                .setPositiveButton(android.R.string.ok)
                .setNegativeButton(R.string.cancel)
                .createAndShow(childFragmentManager, "reset_comment_to_default_styles")
        }

        binding.demoViewContainer.adapter = adapter
        binding.demoViewContainer.setHasFixedSize(true)
        binding.demoViewContainer.layoutManager = LinearLayoutManager(context)

        viewModel.onPostUiChanged.observe(viewLifecycleOwner) {
            updateRendering()
            bindPostUiSettings()
        }
        bindPostUiSettings()
        updateRendering()
    }

    private fun bindPostUiSettings() {
        SliderSettingItem(
            getString(R.string.font_size),
            0.2f,
            3f,
        ).bindTo(
            binding.textScalingSetting1,
            { viewModel.currentPostAndCommentUiConfig.postUiConfig.textSizeMultiplier },
            {
                viewModel.currentPostAndCommentUiConfig =
                    viewModel.currentPostAndCommentUiConfig.copy(
                        postUiConfig = viewModel.currentPostAndCommentUiConfig
                            .postUiConfig
                            .updateTextSizeMultiplier(it)
                    )

                updateRendering()
            }
        )
        SliderSettingItem(
            getString(R.string.font_size),
            0.2f,
            3f,
        ).bindTo(
            binding.textScalingSetting2,
            { viewModel.currentPostAndCommentUiConfig.commentUiConfig.textSizeMultiplier },
            {
                viewModel.currentPostAndCommentUiConfig =
                    viewModel.currentPostAndCommentUiConfig.copy(
                        commentUiConfig = viewModel.currentPostAndCommentUiConfig
                            .commentUiConfig
                            .updateTextSizeMultiplier(it)
                    )

                updateRendering()
            }
        )

        settings.showCommentActions.bindTo(
            binding.showCommentActions,
            { !viewModel.preferences.hideCommentActions },
            {
                viewModel.preferences.hideCommentActions = !it

                updateRendering()
            }
        )

        settings.commentsThreadStyle.bindTo(
            binding.commentThreadStyle,
            { viewModel.preferences.commentThreadStyle },
            {
                MultipleChoiceDialogFragment.newInstance(it)
                    .showAllowingStateLoss(childFragmentManager, "aaaaaaa")
            }
        )
    }

    private fun updateRendering() {
        if (!isBindingAvailable()) return

        val set: Transition = TransitionSet()
            .addTransition(Fade(Fade.IN or Fade.OUT))
            .addTransition(ChangeBounds())
            .addTransition(ChangeClipBounds())
            .addTransition(ChangeImageTransform())
            .setOrdering(TransitionSet.ORDERING_TOGETHER)
            .setDuration(Utils.ANIMATION_DURATION_MS)
        TransitionManager.beginDelayedTransition(binding.demoViewContainer, set)

        binding.demoViewContainer.itemAnimator = null
        postAndCommentViewBuilder.onPreferencesChanged()
        postAndCommentViewBuilder.uiConfig = viewModel.currentPostAndCommentUiConfig
        (binding.demoViewContainer.adapter as? FakePostAndCommentsAdapter)?.refresh()
        binding.demoViewContainer.adapter?.notifyDataSetChanged()
        binding.demoViewContainer.setupForPostAndComments(viewModel.preferences)
    }

    override fun updateValue(key: Int, value: Any?) {
        when (key) {
            settings.commentsThreadStyle.id -> {
                viewModel.preferences.commentThreadStyle = value as Int
            }
        }

        bindPostUiSettings()
        updateRendering()
    }

    private class FakePostAndCommentsAdapter(
        private val container: View,
        private val postAndCommentViewBuilder: PostAndCommentViewBuilder,
        private val contentMaxWidth: Int,
        private val viewLifecycleOwner: LifecycleOwner,
        private val preferences: Preferences,
    ) : Adapter<ViewHolder>() {

        private val items = listOf(
            LemmyFakeModels.fakePostView,
            LemmyFakeModels.fakeCommentView1,
            LemmyFakeModels.fakeCommentView2,
        )

        private val adapterHelper = AdapterHelper<Any>(
            areItemsTheSame = { old, new ->
                old::class == new::class && when (old) {
                    is PostView -> true
                    is CommentView ->
                        old.comment.id == (new as CommentView).comment.id
                    else ->
                        false
                }
            }
        )

        init {
            refresh()
            adapterHelper.setItems(items, this)
        }

        fun refresh() {
            adapterHelper.resetItemTypes()
            adapterHelper.addItemType(PostView::class, PostHeaderItemBinding::inflate) { item, b, h ->
                postAndCommentViewBuilder.bindPostView(
                    b,
                    container = container,
                    postView = item,
                    instance = "https://fake.instance",
                    isRevealed = true,
                    contentMaxWidth = contentMaxWidth,
                    viewLifecycleOwner = viewLifecycleOwner,
                    updateContent = true,
                    onRevealContentClickedFn = {},
                    onImageClick = { _, _, _ -> },
                    onVideoClick = { _, _, _ -> },
                    onPageClick = {},
                    onSignInRequired = {},
                    onInstanceMismatch = { _, _ -> },
                    videoState = null,
                    onAddCommentClick = {},
                    onPostMoreClick = {},
                )
            }
            if (preferences.hideCommentActions) {
                adapterHelper.addItemType(CommentView::class, PostCommentExpandedCompactItemBinding::inflate) { item, b, h ->

                    b.headerView.textView2.setCompoundDrawablesRelativeWithIntrinsicBounds(
                        R.drawable.baseline_arrow_upward_16, 0, 0, 0)
                    b.headerView.textView2.compoundDrawablePadding =
                        Utils.convertDpToPixel(4f).toInt()
                    b.headerView.textView2.updatePaddingRelative(
                        start = Utils.convertDpToPixel(8f).toInt())

                    postAndCommentViewBuilder.bindCommentViewExpanded(
                        h = h,
                        binding = CommentExpandedViewHolder.fromBinding(b),
                        baseDepth = 0,
                        depth = if (item.comment.id == LemmyFakeModels.fakeCommentView1.comment.id) {
                            0
                        } else {
                            1
                        },
                        commentView = item,
                        isDeleting = false,
                        content = item.comment.content,
                        instance = "https://fake.instance",
                        isPostLocked = false,
                        isUpdating = false,
                        highlight = false,
                        highlightForever = false,
                        viewLifecycleOwner = viewLifecycleOwner,
                        currentAccountId = item.creator.id,
                        isActionsExpanded = false,
                        onImageClick = { _, _, _ -> },
                        onPageClick = {},
                        collapseSection = {},
                        toggleActionsExpanded = {},
                        onAddCommentClick = {},
                        onEditCommentClick = {},
                        onDeleteCommentClick = {},
                        onSignInRequired = {},
                        onInstanceMismatch = { _, _ -> }
                    )
                }
            } else {
                adapterHelper.addItemType(CommentView::class, PostCommentExpandedItemBinding::inflate) { item, b, h ->
                    postAndCommentViewBuilder.bindCommentViewExpanded(
                        h = h,
                        binding = CommentExpandedViewHolder.fromBinding(b),
                        baseDepth = 0,
                        depth = if (item.comment.id == LemmyFakeModels.fakeCommentView1.comment.id) {
                            0
                        } else {
                            1
                        },
                        commentView = item,
                        isDeleting = false,
                        content = item.comment.content,
                        instance = "https://fake.instance",
                        isPostLocked = false,
                        isUpdating = false,
                        highlight = false,
                        highlightForever = false,
                        viewLifecycleOwner = viewLifecycleOwner,
                        currentAccountId = item.creator.id,
                        isActionsExpanded = false,
                        onImageClick = { _, _, _ -> },
                        onPageClick = {},
                        collapseSection = {},
                        toggleActionsExpanded = {},
                        onAddCommentClick = {},
                        onEditCommentClick = {},
                        onDeleteCommentClick = {},
                        onSignInRequired = {},
                        onInstanceMismatch = { _, _ -> }
                    )
                }
            }
        }

        override fun getItemViewType(position: Int): Int =
            adapterHelper.getItemViewType(position)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
            adapterHelper.onCreateViewHolder(parent, viewType)

        override fun getItemCount(): Int = adapterHelper.itemCount

        override fun onBindViewHolder(holder: ViewHolder, position: Int) =
            adapterHelper.onBindViewHolder(holder, position)
    }

    override fun onPositiveClick(dialog: AlertDialogFragment, tag: String?) {
        if (tag == "reset_post_to_default_styles") {
            viewModel.resetPostUiConfig()
        } else if (tag == "reset_comment_to_default_styles") {
            viewModel.preferences.reset(PreferenceUtil.KEY_COMMENT_THREAD_STYLE)
            viewModel.preferences.reset(PreferenceUtil.KEY_HIDE_COMMENT_ACTIONS)
            viewModel.resetCommentUiConfig()
        }
    }

    override fun onNegativeClick(dialog: AlertDialogFragment, tag: String?) {

    }
}