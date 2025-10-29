package com.idunnololz.summit.settings.postAppearance

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewTreeObserver
import androidx.fragment.app.viewModels
import androidx.transition.TransitionManager
import com.idunnololz.summit.R
import com.idunnololz.summit.alert.newAlertDialogLauncher
import com.idunnololz.summit.databinding.ListingItemCard2Binding
import com.idunnololz.summit.databinding.ListingItemCard3Binding
import com.idunnololz.summit.databinding.ListingItemCardBinding
import com.idunnololz.summit.databinding.ListingItemCompactBinding
import com.idunnololz.summit.databinding.ListingItemFullBinding
import com.idunnololz.summit.databinding.ListingItemFullWithCardsBinding
import com.idunnololz.summit.databinding.ListingItemLargeListBinding
import com.idunnololz.summit.databinding.ListingItemListBinding
import com.idunnololz.summit.databinding.ListingItemListWithCardsBinding
import com.idunnololz.summit.databinding.PostAppearanceDemoCardBinding
import com.idunnololz.summit.lemmy.community.CommunityLayout
import com.idunnololz.summit.lemmy.postListView.ListingItemViewHolder
import com.idunnololz.summit.lemmy.postListView.PostListViewBuilder
import com.idunnololz.summit.lemmy.postListView.defaultDimReadPosts
import com.idunnololz.summit.lemmy.toPostHeaderInfo
import com.idunnololz.summit.settings.BaseSettingsFragment
import com.idunnololz.summit.settings.BasicSettingItem
import com.idunnololz.summit.settings.LemmyFakeModels
import com.idunnololz.summit.settings.PostsFeedAppearanceSettings
import com.idunnololz.summit.settings.SettingModelItem
import com.idunnololz.summit.settings.util.asCustomItem
import com.idunnololz.summit.settings.util.asCustomViewSettingsItem
import com.idunnololz.summit.settings.util.asOnOffSwitch
import com.idunnololz.summit.settings.util.asSingleChoiceSelectorItem
import com.idunnololz.summit.settings.util.asSliderItem
import com.idunnololz.summit.util.makeTransition
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class SettingsViewTypeFragment : BaseSettingsFragment() {

  private val viewModel: SettingsViewTypeViewModel by viewModels()

  @Inject
  lateinit var postListViewBuilder: PostListViewBuilder

  @Inject
  override lateinit var settings: PostsFeedAppearanceSettings

  private val resetDialogLauncher = newAlertDialogLauncher("reset_view_to_default_styles") {
    if (it.isOk) {
      viewModel.resetPostUiConfig()
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

  private fun setup() {
    if (!isBindingAvailable()) return

    updateRendering()

    viewModel.onPostUiChanged.observe(viewLifecycleOwner) {
      updateRendering()
      refresh()
    }
  }

  override fun generateData(): List<SettingModelItem> = listOf(
    BasicSettingItem(
      icon = null,
      title = getString(R.string.preview),
      description = null,
      id = R.id.post_appearance_preview,
    ).asCustomViewSettingsItem(
      typeId = R.id.post_appearance_preview,
      payload = null,
      inflateFn = PostAppearanceDemoCardBinding::inflate,
      bindViewHolder = { item, b, h ->
        b.demoViewContainer.setTag(R.id.binding, b)
        updateRendering(b)
      },
    ),
    settings.baseViewType.asSingleChoiceSelectorItem(
      {
        preferences.getPostsLayout().ordinal
      },
      {
        viewModel.onLayoutChanging()
        preferences.setPostsLayout(CommunityLayout.entries[it])
        viewModel.onLayoutChanged()

        updateRendering()
      },
    ),
    SettingModelItem.DividerItem(R.id.divider0),
    settings.reset.asCustomItem {
      resetDialogLauncher.launchDialog {
        messageResId = R.string.reset_view_to_default_styles
        positionButtonResId = android.R.string.ok
        negativeButtonResId = R.string.cancel
      }
    },
    SettingModelItem.DividerItem(R.id.divider1),
    settings.fontSize.asSliderItem(
      { viewModel.currentPostUiConfig.textSizeMultiplier },
      {
        viewModel.currentPostUiConfig =
          viewModel.currentPostUiConfig.updateTextSizeMultiplier(it)

        updateRendering()
      },
    ),
    settings.imageSize.asSliderItem(
      { viewModel.currentPostUiConfig.imageWidthPercent },
      {
        viewModel.currentPostUiConfig =
          viewModel.currentPostUiConfig.copy(
            imageWidthPercent = it,
          )

        updateRendering()
      },
    ),
    *when (preferences.getPostsLayout()) {
      CommunityLayout.Card,
      CommunityLayout.Card2,
      CommunityLayout.Card3,
      CommunityLayout.ListWithCards,
      CommunityLayout.FullWithCards,
      -> {
        arrayOf(
          settings.horizontalMarginSize.asSliderItem(
            { viewModel.currentPostUiConfig.horizontalMarginDp ?: 16f },
            {
              viewModel.currentPostUiConfig =
                viewModel.currentPostUiConfig.copy(
                  horizontalMarginDp = it,
                )

              updateRendering()
            },
          ),
          settings.verticalMarginSize.asSliderItem(
            { viewModel.currentPostUiConfig.verticalMarginDp ?: 0f },
            {
              viewModel.currentPostUiConfig =
                viewModel.currentPostUiConfig.copy(
                  verticalMarginDp = it,
                )

              updateRendering()
            },
          ),
        )
      }
      else -> {
        arrayOf()
      }
    },
    settings.preferImageAtEnd.asOnOffSwitch(
      { viewModel.currentPostUiConfig.preferImagesAtEnd },
      {
        viewModel.currentPostUiConfig =
          viewModel.currentPostUiConfig.copy(preferImagesAtEnd = it)

        updateRendering()
      },
    ),
    settings.preferFullImage.asOnOffSwitch(
      { viewModel.currentPostUiConfig.preferFullSizeImages },
      {
        viewModel.currentPostUiConfig =
          viewModel.currentPostUiConfig.copy(preferFullSizeImages = it)

        updateRendering()
      },
    ),
    settings.preferTitleText.asOnOffSwitch(
      { viewModel.currentPostUiConfig.preferTitleText },
      {
        viewModel.currentPostUiConfig =
          viewModel.currentPostUiConfig.copy(preferTitleText = it)

        updateRendering()
      },
    ),
    settings.preferCommunityIcon.asOnOffSwitch(
      { viewModel.currentPostUiConfig.showCommunityIcon },
      {
        viewModel.currentPostUiConfig =
          viewModel.currentPostUiConfig.copy(showCommunityIcon = it)

        updateRendering()
      },
    ),
    settings.contentMaxLines.asSingleChoiceSelectorItem(
      {
        viewModel.currentPostUiConfig.contentMaxLines
      },
      {
        viewModel.currentPostUiConfig =
          viewModel.currentPostUiConfig.copy(contentMaxLines = it)

        updateRendering()
      },
    ),
    settings.contentMaxHeight.asSingleChoiceSelectorItem(
      { viewModel.currentPostUiConfig.contentMaxHeightDp },
      {
        viewModel.currentPostUiConfig =
          viewModel.currentPostUiConfig.copy(contentMaxHeightDp = it)

        updateRendering()
      },
    ),
    settings.dimReadPosts.asOnOffSwitch(
      {
        viewModel.currentPostUiConfig.dimReadPosts
          ?: preferences.getPostsLayout().defaultDimReadPosts
      },
      {
        viewModel.currentPostUiConfig =
          viewModel.currentPostUiConfig.copy(dimReadPosts = it)

        updateRendering()
      },
    ),
    settings.preferTextPreviewIcon.asOnOffSwitch(
      {
        viewModel.currentPostUiConfig.showTextPreviewIcon
          ?: true
      },
      {
        viewModel.currentPostUiConfig =
          viewModel.currentPostUiConfig.copy(showTextPreviewIcon = it)

        updateRendering()
      },
    ),
    settings.showUrlDomain.asOnOffSwitch(
      {
        viewModel.currentPostUiConfig.showUrlDomain()
      },
      {
        viewModel.currentPostUiConfig =
          viewModel.currentPostUiConfig.copy(showUrlDomain = it)

        updateRendering()
      }
    )
  )

  private var lastH = listOf<ListingItemViewHolder>()
  private fun updateRendering(b: PostAppearanceDemoCardBinding? = null) {
    if (!isBindingAvailable()) return

    val binding = b ?: binding.recyclerView.findViewById<View>(R.id.demo_view_container)
      ?.getTag(R.id.binding) as? PostAppearanceDemoCardBinding

    if (binding == null) {
      return
    }

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

    lastH.forEach {
      postListViewBuilder.recycle(it)
    }

    fun newH(id: Int): ListingItemViewHolder {
      val h = when (preferences.getPostsLayout()) {
        CommunityLayout.Compact ->
          ListingItemViewHolder.fromBinding(
            ListingItemCompactBinding.inflate(inflater, binding.demoViewContainer, false),
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
        CommunityLayout.SmartList ->
          ListingItemViewHolder.fromBinding(
            ListingItemListBinding.inflate(inflater, binding.demoViewContainer, false),
          )
      }

      val fetchedPost = LemmyFakeModels.fakeFetchedPost
      postListViewBuilder.bind(
        holder = h,
        fetchedPost = fetchedPost,
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
        postHeaderInfo = fetchedPost.postView.toPostHeaderInfo(context),
        onRevealContentClickedFn = {},
        onImageClick = { _, _, _, _ -> },
        onVideoClick = { _, _, _ -> },
        onVideoLongClickListener = { _ -> },
        onPageClick = { _, _, _ -> },
        onItemClick = { _, _, _, _, _, _, _, _ -> },
        onShowMoreOptions = { _, _ -> },
        toggleItem = {},
        toggleActions = {},
        onSignInRequired = {},
        onInstanceMismatch = { _, _ -> },
        onHighlightComplete = {},
        onLinkClick = { _, _, _, _ -> },
        onLinkLongClick = { _, _, _ -> },
        onPostActionClick = { _, _ -> },
      )

      return h.apply {
        this.root.id = id
      }
    }

    binding.demoViewContainer.removeAllViews()

    lastH = listOf(
      newH(R.id.fake_post_1),
    ).onEach {
      binding.demoViewContainer.addView(it.root)
    }
  }
}
