package com.idunnololz.summit.settings.postAndCommentsAppearance

import android.os.Bundle
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
import com.idunnololz.summit.alert.newAlertDialogLauncher
import com.idunnololz.summit.api.dto.lemmy.CommentView
import com.idunnololz.summit.api.dto.lemmy.PostView
import com.idunnololz.summit.databinding.PostAndCommentAppearanceDemoCardBinding
import com.idunnololz.summit.databinding.PostCommentExpandedItemBinding
import com.idunnololz.summit.databinding.PostHeaderItemBinding
import com.idunnololz.summit.lemmy.postAndCommentView.CommentExpandedViewHolder
import com.idunnololz.summit.lemmy.postAndCommentView.PostAndCommentViewBuilder
import com.idunnololz.summit.lemmy.postAndCommentView.setupForPostAndComments
import com.idunnololz.summit.lemmy.toCommentHeaderInfo
import com.idunnololz.summit.lemmy.toPostHeaderInfo
import com.idunnololz.summit.preferences.Preferences
import com.idunnololz.summit.settings.BaseSettingsFragment
import com.idunnololz.summit.settings.BasicSettingItem
import com.idunnololz.summit.settings.LemmyFakeModels
import com.idunnololz.summit.settings.PostAndCommentsAppearanceSettings
import com.idunnololz.summit.settings.SettingModelItem
import com.idunnololz.summit.settings.util.asCustomItem
import com.idunnololz.summit.settings.util.asCustomViewSettingsItem
import com.idunnololz.summit.settings.util.asOnOffSwitch
import com.idunnololz.summit.settings.util.asSingleChoiceSelectorItem
import com.idunnololz.summit.settings.util.asSliderItem
import com.idunnololz.summit.util.AnimationsHelper
import com.idunnololz.summit.util.Utils
import com.idunnololz.summit.util.ext.setup
import com.idunnololz.summit.util.makeTransition
import com.idunnololz.summit.util.recyclerView.AdapterHelper
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class SettingsPostAndCommentsAppearanceFragment :
  BaseSettingsFragment() {

  private val viewModel: SettingsPostAndCommentsAppearanceViewModel by viewModels()

  @Inject
  lateinit var postAndCommentViewBuilder: PostAndCommentViewBuilder

  @Inject
  override lateinit var settings: PostAndCommentsAppearanceSettings

  @Inject
  lateinit var animationsHelper: AnimationsHelper

  private val resetPostStylesDialogLauncher =
    newAlertDialogLauncher("reset_post_to_default_styles") {
      if (it.isOk) {
        viewModel.resetPostUiConfig()
      }
    }
  private val resetCommentStylesDialogLauncher =
    newAlertDialogLauncher("reset_comment_to_default_styles") {
      if (it.isOk) {
        viewModel.resetCommentUiConfig()
      }
    }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)

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

    viewModel.onPostUiChanged.observe(viewLifecycleOwner) {
      updateRendering()
      refresh()
    }
    updateRendering()
  }

  override fun generateData(): List<SettingModelItem> = listOf(
    BasicSettingItem(
      icon = null,
      title = getString(R.string.preview),
      description = null,
      id = R.id.post_and_comment_appearance_preview,
    ).asCustomViewSettingsItem(
      typeId = R.id.post_and_comment_appearance_preview,
      payload = null,
      inflateFn = PostAndCommentAppearanceDemoCardBinding::inflate,
      bindViewHolder = { item, b, h ->
        b.demoViewContainer.setTag(R.id.binding, b)
        updateRendering(b)
      },
    ),
    SettingModelItem.SubgroupItem(
      getString(R.string.post_settings),
      listOf(
        settings.postFontSize.asSliderItem(
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
        ),
        settings.alwaysShowLinkBelowPost.asOnOffSwitch(
          { viewModel.preferences.alwaysShowLinkButtonBelowPost },
          {
            viewModel.preferences.alwaysShowLinkButtonBelowPost = it

            updateRendering()
          },
        ),
        settings.fullBleedImage.asOnOffSwitch(
          { viewModel.preferences.postFullBleedImage },
          {
            viewModel.preferences.postFullBleedImage = it

            updateRendering()
          },
        ),
        SettingModelItem.DividerItem(R.id.divider0),
        settings.resetPostStyles.asCustomItem {
          resetPostStylesDialogLauncher.launchDialog {
            messageResId = R.string.reset_view_to_default_styles
            positionButtonResId = android.R.string.ok
            negativeButtonResId = R.string.cancel
          }
        },
        SettingModelItem.DividerItem(R.id.divider1),
      ),
    ),
    SettingModelItem.SubgroupItem(
      getString(R.string.comment_settings),
      listOf(
        settings.commentFontSize.asSliderItem(
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
        ),
        settings.commentIndentationLevel.asSliderItem(
          {
            viewModel.currentPostAndCommentUiConfig.commentUiConfig.indentationPerLevelDp.toFloat()
          },
          {
            viewModel.currentPostAndCommentUiConfig =
              viewModel.currentPostAndCommentUiConfig.copy(
                commentUiConfig = viewModel.currentPostAndCommentUiConfig
                  .commentUiConfig
                  .updateIndentationPerLevelDp(it),
              )

            updateRendering()
          },
        ),
        settings.showCommentActions.asOnOffSwitch(
          { !viewModel.preferences.hideCommentActions },
          {
            viewModel.preferences.hideCommentActions = !it

            updateRendering()
          },
        ),
        settings.tapCommentToCollapse.asOnOffSwitch(
          { viewModel.preferences.tapCommentToCollapse },
          {
            viewModel.preferences.tapCommentToCollapse = it

            updateRendering()
          },
        ),
        settings.commentsThreadStyle.asSingleChoiceSelectorItem(
          { viewModel.preferences.commentThreadStyle },
          {
            viewModel.preferences.commentThreadStyle = it

            updateRendering()
          },
        ),
        settings.useCondensedTypefaceForCommentHeader.asOnOffSwitch(
          { viewModel.preferences.useCondensedTypefaceForCommentHeaders },
          {
            viewModel.preferences.useCondensedTypefaceForCommentHeaders = it

            updateRendering()
          },
        ),
        SettingModelItem.DividerItem(R.id.divider2),
        settings.resetCommentStyles.asCustomItem {
          resetCommentStylesDialogLauncher.launchDialog {
            messageResId = R.string.reset_view_to_default_styles
            positionButtonResId = android.R.string.ok
            negativeButtonResId = R.string.cancel
          }
        },
        SettingModelItem.DividerItem(R.id.divider3),
      ),
    ),
  )

  private fun updateRendering(b: PostAndCommentAppearanceDemoCardBinding? = null) {
    if (!isBindingAvailable()) return

    val binding = b ?: binding.recyclerView.findViewById<View>(R.id.demo_view_container)
      ?.getTag(R.id.binding) as? PostAndCommentAppearanceDemoCardBinding

    if (binding == null) {
      return
    }

    val adapter = FakePostAndCommentsAdapter(
      binding.demoViewContainer,
      postAndCommentViewBuilder,
      binding.demoViewContainer.width,
      binding.demoViewContainer.height,
      viewLifecycleOwner,
      viewModel.preferences,
    )

    binding.demoViewContainer.setup(animationsHelper)
    binding.demoViewContainer.adapter = adapter
    binding.demoViewContainer.setHasFixedSize(false)
    binding.demoViewContainer.layoutManager = LinearLayoutManager(context)

    TransitionManager.beginDelayedTransition(binding.demoViewContainer, makeTransition())

    binding.demoViewContainer.itemAnimator = null
    postAndCommentViewBuilder.onPreferencesChanged()
    postAndCommentViewBuilder.uiConfig = viewModel.currentPostAndCommentUiConfig
    (binding.demoViewContainer.adapter as? FakePostAndCommentsAdapter)?.refresh()
    binding.demoViewContainer.adapter?.notifyDataSetChanged()
    binding.demoViewContainer.setupForPostAndComments(viewModel.preferences)
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
        old::class == new::class &&
          when (old) {
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
      val context = container.context

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
          crossPosts = 0,
          postHeaderInfo = item.toPostHeaderInfo(context),
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
          onCrossPostsClick = {},
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
          highlightTintColor = null,
          viewLifecycleOwner = viewLifecycleOwner,
          isActionsExpanded = false,
          highlightTextData = null,
          commentHeaderInfo = item.toCommentHeaderInfo(context),
          onImageClick = { _, _, _ -> },
          onVideoClick = { _, _, _ -> },
          onPageClick = {},
          collapseSection = {},
          toggleActionsExpanded = {},
          onCommentActionClick = { _, _ -> },
          onLinkClick = { _, _, _ -> },
          onLinkLongClick = { _, _ -> },
          onSignInRequired = {},
          onInstanceMismatch = { _, _ -> },
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
}
