package com.idunnololz.summit.settings.postAndCommentsAppearance

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
import androidx.transition.TransitionManager
import com.idunnololz.summit.R
import com.idunnololz.summit.alert.OldAlertDialogFragment
import com.idunnololz.summit.api.dto.CommentView
import com.idunnololz.summit.api.dto.PostView
import com.idunnololz.summit.databinding.FragmentSettingsPostAndCommentsAppearanceBinding
import com.idunnololz.summit.databinding.PostCommentExpandedItemBinding
import com.idunnololz.summit.databinding.PostHeaderItemBinding
import com.idunnololz.summit.lemmy.postAndCommentView.CommentExpandedViewHolder
import com.idunnololz.summit.lemmy.postAndCommentView.PostAndCommentViewBuilder
import com.idunnololz.summit.lemmy.postAndCommentView.setupForPostAndComments
import com.idunnololz.summit.preferences.Preferences
import com.idunnololz.summit.settings.LemmyFakeModels
import com.idunnololz.summit.settings.PostAndCommentsAppearanceSettings
import com.idunnololz.summit.settings.SettingPath.getPageName
import com.idunnololz.summit.settings.SettingsFragment
import com.idunnololz.summit.settings.dialogs.MultipleChoiceDialogFragment
import com.idunnololz.summit.settings.dialogs.SettingValueUpdateCallback
import com.idunnololz.summit.settings.util.bindTo
import com.idunnololz.summit.util.AnimationsHelper
import com.idunnololz.summit.util.BaseFragment
import com.idunnololz.summit.util.Utils
import com.idunnololz.summit.util.ext.setup
import com.idunnololz.summit.util.ext.showAllowingStateLoss
import com.idunnololz.summit.util.insetViewExceptBottomAutomaticallyByMargins
import com.idunnololz.summit.util.insetViewExceptTopAutomaticallyByPadding
import com.idunnololz.summit.util.makeTransition
import com.idunnololz.summit.util.recyclerView.AdapterHelper
import com.idunnololz.summit.util.setupForFragment
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class SettingsPostAndCommentsAppearanceFragment :
    BaseFragment<FragmentSettingsPostAndCommentsAppearanceBinding>(),
    OldAlertDialogFragment.AlertDialogFragmentListener,
    SettingValueUpdateCallback {

    private val viewModel: SettingsPostAndCommentsAppearanceViewModel by viewModels()

    @Inject
    lateinit var postAndCommentViewBuilder: PostAndCommentViewBuilder

    @Inject
    lateinit var settings: PostAndCommentsAppearanceSettings

    @Inject
    lateinit var animationsHelper: AnimationsHelper

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        super.onCreateView(inflater, container, savedInstanceState)

        setBinding(
            FragmentSettingsPostAndCommentsAppearanceBinding.inflate(
                inflater,
                container,
                false,
            ),
        )

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

    fun setup() {
        if (!isBindingAvailable()) return

        val adapter = FakePostAndCommentsAdapter(
            binding.demoViewContainer,
            postAndCommentViewBuilder,
            binding.demoViewContainer.width,
            binding.demoViewContainer.height,
            viewLifecycleOwner,
            viewModel.preferences,
        )

        val context = requireContext()

        settings.resetPostStyles.bindTo(binding.resetPostStyles) {
            OldAlertDialogFragment.Builder()
                .setMessage(R.string.reset_view_to_default_styles)
                .setPositiveButton(android.R.string.ok)
                .setNegativeButton(R.string.cancel)
                .createAndShow(childFragmentManager, "reset_post_to_default_styles")
        }

        settings.resetCommentStyles.bindTo(binding.resetCommentStyles) {
            OldAlertDialogFragment.Builder()
                .setMessage(R.string.reset_view_to_default_styles)
                .setPositiveButton(android.R.string.ok)
                .setNegativeButton(R.string.cancel)
                .createAndShow(childFragmentManager, "reset_comment_to_default_styles")
        }

        binding.demoViewContainer.setup(animationsHelper)
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
        settings.postFontSize.bindTo(
            binding.textScalingSetting1,
            { viewModel.currentPostAndCommentUiConfig.postUiConfig.textSizeMultiplier },
            {
                viewModel.currentPostAndCommentUiConfig =
                    viewModel.currentPostAndCommentUiConfig.copy(
                        postUiConfig = viewModel.currentPostAndCommentUiConfig
                            .postUiConfig
                            .updateTextSizeMultiplier(it),
                    )

                updateRendering()
            },
        )
        settings.fullBleedImage.bindTo(
            binding.fullBleedImage,
            { viewModel.preferences.postFullBleedImage },
            {
                viewModel.preferences.postFullBleedImage = it

                updateRendering()
            },
        )
        settings.commentFontSize.bindTo(
            binding.textScalingSetting2,
            { viewModel.currentPostAndCommentUiConfig.commentUiConfig.textSizeMultiplier },
            {
                viewModel.currentPostAndCommentUiConfig =
                    viewModel.currentPostAndCommentUiConfig.copy(
                        commentUiConfig = viewModel.currentPostAndCommentUiConfig
                            .commentUiConfig
                            .updateTextSizeMultiplier(it),
                    )

                updateRendering()
            },
        )
        settings.commentIndentationLevel.bindTo(
            binding.indentationPerLevel,
            { viewModel.currentPostAndCommentUiConfig.commentUiConfig.indentationPerLevelDp.toFloat() },
            {
                viewModel.currentPostAndCommentUiConfig =
                    viewModel.currentPostAndCommentUiConfig.copy(
                        commentUiConfig = viewModel.currentPostAndCommentUiConfig
                            .commentUiConfig
                            .updateIndentationPerLevelDp(it),
                    )

                updateRendering()
            },
        )

        settings.showCommentActions.bindTo(
            binding.showCommentActions,
            { !viewModel.preferences.hideCommentActions },
            {
                viewModel.preferences.hideCommentActions = !it

                updateRendering()
            },
        )

        settings.tapCommentToCollapse.bindTo(
            binding.tapCommentToCollapse,
            { viewModel.preferences.tapCommentToCollapse },
            {
                viewModel.preferences.tapCommentToCollapse = it

                updateRendering()
            },
        )

        settings.commentsThreadStyle.bindTo(
            binding.commentThreadStyle,
            { viewModel.preferences.commentThreadStyle },
            { setting, currentValue ->
                MultipleChoiceDialogFragment.newInstance(setting, currentValue)
                    .showAllowingStateLoss(childFragmentManager, "aaaaaaa")
            },
        )

        settings.alwaysShowLinkBelowPost.bindTo(
            binding.alwaysShowLinkBelowPost,
            { viewModel.preferences.alwaysShowLinkButtonBelowPost },
            {
                viewModel.preferences.alwaysShowLinkButtonBelowPost = it

                updateRendering()
            },
        )
        settings.useCondensedTypefaceForCommentHeader.bindTo(
            binding.useCondensedTypefaceForCommentHeader,
            { viewModel.preferences.useCondensedTypefaceForCommentHeaders },
            {
                viewModel.preferences.useCondensedTypefaceForCommentHeaders = it

                updateRendering()
            },
        )
    }

    private fun updateRendering() {
        if (!isBindingAvailable()) return

        TransitionManager.beginDelayedTransition(binding.demoViewContainer, makeTransition())

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
        private val contentMaxHeight: Int,
        private val viewLifecycleOwner: LifecycleOwner,
        private val preferences: Preferences,
    ) : Adapter<ViewHolder>() {

        private val items = listOf(
            LemmyFakeModels.fakePostView,
            LemmyFakeModels.fakeCommentView1,
            LemmyFakeModels.fakeCommentView2,
            LemmyFakeModels.fakeCommentView3,
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
            },
        )

        init {
            refresh()
            adapterHelper.setItems(items, this)
        }

        fun refresh() {
            adapterHelper.resetItemTypes()
            adapterHelper.addItemType(
                clazz = PostView::class,
                inflateFn = PostHeaderItemBinding::inflate,
            ) { item, b, _ ->

                postAndCommentViewBuilder.bindPostView(
                    b,
                    postView = item,
                    instance = "https://fake.instance",
                    accountId = null,
                    isRevealed = true,
                    contentMaxWidth = contentMaxWidth,
                    contentMaxHeight = contentMaxHeight,
                    viewLifecycleOwner = viewLifecycleOwner,
                    updateContent = true,
                    highlightTextData = null,
                    contentSpannable = null,
                    onRevealContentClickedFn = {},
                    onImageClick = { _, _, _ -> },
                    onVideoClick = { _, _, _ -> },
                    onVideoLongClickListener = { _ -> },
                    onPageClick = {},
                    onSignInRequired = {},
                    onInstanceMismatch = { _, _ -> },
                    videoState = null,
                    onAddCommentClick = {},
                    onPostActionClick = { _, _ -> },
                    onLinkClick = { _, _, _ -> },
                    onLinkLongClick = { _, _ -> },
                    onTextBound = {},
                )
            }
            adapterHelper.addItemType(
                clazz = CommentView::class,
                inflateFn = PostCommentExpandedItemBinding::inflate,
            ) { item, b, h ->
                b.headerView.textView2.setCompoundDrawablesRelativeWithIntrinsicBounds(
                    R.drawable.baseline_arrow_upward_16,
                    0,
                    0,
                    0,
                )
                b.headerView.textView2.compoundDrawablePadding =
                    Utils.convertDpToPixel(4f).toInt()
                b.headerView.textView2.updatePaddingRelative(
                    start = Utils.convertDpToPixel(8f).toInt(),
                )

                postAndCommentViewBuilder.bindCommentViewExpanded(
                    h = h,
                    holder = CommentExpandedViewHolder.fromBinding(b),
                    baseDepth = 0,
                    depth = when (item.comment.id) {
                        LemmyFakeModels.fakeCommentView1.comment.id -> {
                            0
                        }
                        LemmyFakeModels.fakeCommentView2.comment.id -> {
                            1
                        }
                        else -> {
                            2
                        }
                    },
                    maxDepth = Integer.MAX_VALUE,
                    commentView = item,
                    accountId = null,
                    isDeleting = false,
                    isRemoved = false,
                    content = item.comment.content,
                    contentSpannable = null,
                    instance = "https://fake.instance",
                    isPostLocked = false,
                    isUpdating = false,
                    highlight = false,
                    highlightForever = false,
                    viewLifecycleOwner = viewLifecycleOwner,
                    isActionsExpanded = false,
                    highlightTextData = null,
                    onImageClick = { _, _, _ -> },
                    onVideoClick = { _, _, _ -> },
                    onPageClick = {},
                    collapseSection = {},
                    toggleActionsExpanded = {},
                    onSignInRequired = {},
                    onInstanceMismatch = { _, _ -> },
                    onCommentActionClick = { _, _ -> },
                    onLinkClick = { _, _, _ -> },
                    onLinkLongClick = { _, _ -> },
                )
            }
        }

        override fun getItemViewType(position: Int): Int = adapterHelper.getItemViewType(position)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
            adapterHelper.onCreateViewHolder(parent, viewType)

        override fun getItemCount(): Int = adapterHelper.itemCount

        override fun onBindViewHolder(holder: ViewHolder, position: Int) =
            adapterHelper.onBindViewHolder(holder, position)
    }

    override fun onPositiveClick(dialog: OldAlertDialogFragment, tag: String?) {
        if (tag == "reset_post_to_default_styles") {
            viewModel.resetPostUiConfig()
        } else if (tag == "reset_comment_to_default_styles") {
            viewModel.resetCommentUiConfig()
        }
    }

    override fun onNegativeClick(dialog: OldAlertDialogFragment, tag: String?) {
    }
}
