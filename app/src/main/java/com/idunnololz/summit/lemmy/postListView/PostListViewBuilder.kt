package com.idunnololz.summit.lemmy.postListView

import android.content.Context
import android.view.View
import android.view.ViewGroup.MarginLayoutParams
import androidx.constraintlayout.widget.Barrier
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.updateLayoutParams
import androidx.core.widget.TextViewCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.LifecycleOwner
import coil.load
import com.commit451.coiltransformations.BlurTransformation
import com.google.android.material.imageview.ShapeableImageView
import com.idunnololz.summit.R
import com.idunnololz.summit.account.AccountActionsManager
import com.idunnololz.summit.api.dto.PostView
import com.idunnololz.summit.api.utils.PostType
import com.idunnololz.summit.api.utils.getType
import com.idunnololz.summit.databinding.ListingItemCard2Binding
import com.idunnololz.summit.databinding.ListingItemCard3Binding
import com.idunnololz.summit.databinding.ListingItemCardBinding
import com.idunnololz.summit.databinding.ListingItemCompactBinding
import com.idunnololz.summit.databinding.ListingItemLargeListBinding
import com.idunnololz.summit.databinding.ListingItemListBinding
import com.idunnololz.summit.lemmy.CommunityRef
import com.idunnololz.summit.lemmy.LemmyContentHelper
import com.idunnololz.summit.lemmy.LemmyHeaderHelper
import com.idunnololz.summit.lemmy.LemmyTextHelper
import com.idunnololz.summit.lemmy.LemmyUtils
import com.idunnololz.summit.lemmy.PageRef
import com.idunnololz.summit.lemmy.toCommunityRef
import com.idunnololz.summit.lemmy.utils.bind
import com.idunnololz.summit.offline.OfflineManager
import com.idunnololz.summit.preferences.GlobalFontSizeId
import com.idunnololz.summit.preferences.Preferences
import com.idunnololz.summit.preferences.ThemeManager
import com.idunnololz.summit.preview.VideoType
import com.idunnololz.summit.util.ContentUtils
import com.idunnololz.summit.util.Size
import com.idunnololz.summit.util.Utils
import com.idunnololz.summit.util.ext.getDimen
import com.idunnololz.summit.util.ext.getResIdFromAttribute
import com.idunnololz.summit.video.ExoPlayerManager
import com.idunnololz.summit.video.VideoState
import dagger.hilt.android.qualifiers.ActivityContext
import dagger.hilt.android.scopes.ActivityScoped
import javax.inject.Inject

@ActivityScoped
class PostListViewBuilder @Inject constructor(
    private val activity: FragmentActivity,
    @ActivityContext private val context: Context,
    private val offlineManager: OfflineManager,
    private val accountActionsManager: AccountActionsManager,
    private val preferences: Preferences,
    private val themeManager: ThemeManager,
) {

    var postUiConfig: PostInListUiConfig = preferences.getPostInListUiConfig()
        set(value) {
            field = value

            onPostUiConfigUpdated()
        }

    private var globalFontSizeMultiplier: Float =
        GlobalFontSizeId.getFontSizeMultiplier(preferences.globalFontSize)

    private val lemmyHeaderHelper = LemmyHeaderHelper(context)
    private val lemmyContentHelper = LemmyContentHelper(
        context,
        offlineManager,
        ExoPlayerManager.get(activity),
    ).also {
        it.globalFontSizeMultiplier = globalFontSizeMultiplier
    }

    private val padding = context.getDimen(R.dimen.padding)
    private val paddingHalf = context.getDimen(R.dimen.padding_half)

    private val voteUiHandler = accountActionsManager.voteUiHandler
    private var textSizeMultiplier: Float = postUiConfig.textSizeMultiplier
    private var singleTapToViewImage: Boolean = preferences.postListViewImageOnSingleTap

    private val tempSize = Size()

    init {
        onPostUiConfigUpdated()
    }

    private fun onPostUiConfigUpdated() {
        lemmyContentHelper.config = postUiConfig.fullContentConfig

        textSizeMultiplier = postUiConfig.textSizeMultiplier

        globalFontSizeMultiplier =
            GlobalFontSizeId.getFontSizeMultiplier(preferences.globalFontSize)
        lemmyContentHelper.globalFontSizeMultiplier = globalFontSizeMultiplier
        lemmyContentHelper.alwaysShowLinkBelowPost = preferences.alwaysShowLinkButtonBelowPost
        singleTapToViewImage = preferences.postListViewImageOnSingleTap
    }

    /**
     * @param updateContent False for a fast update (also removes flickering)
     */
    fun bind(
        holder: ListingItemViewHolder,
        postView: PostView,
        instance: String,
        isRevealed: Boolean,
        contentMaxWidth: Int,
        contentPreferredHeight: Int,
        viewLifecycleOwner: LifecycleOwner,
        isExpanded: Boolean,
        isActionsExpanded: Boolean,
        alwaysRenderAsUnread: Boolean,
        updateContent: Boolean,
        highlight: Boolean,
        highlightForever: Boolean,
        onRevealContentClickedFn: () -> Unit,
        onImageClick: (PostView, sharedElementView: View?, String) -> Unit,
        onVideoClick: (url: String, videoType: VideoType, videoState: VideoState?) -> Unit,
        onPageClick: (PageRef) -> Unit,
        onItemClick: (
            instance: String,
            id: Int,
            currentCommunity: CommunityRef?,
            post: PostView,
            jumpToComments: Boolean,
            reveal: Boolean,
            videoState: VideoState?,
        ) -> Unit,
        onShowMoreOptions: (PostView) -> Unit,
        toggleItem: (postView: PostView) -> Unit,
        toggleActions: (postView: PostView) -> Unit,
        onSignInRequired: () -> Unit,
        onInstanceMismatch: (String, String) -> Unit,
        onHighlightComplete: () -> Unit,
        onLinkLongClick: (url: String, text: String) -> Unit,
    ) = with(holder) {
        if (holder.state.preferTitleText != postUiConfig.preferTitleText) {
            if (postUiConfig.preferTitleText) {
                when (val rb = rawBinding) {
                    is ListingItemListBinding -> {
                        TextViewCompat.setTextAppearance(
                            rb.title,
                            context.getResIdFromAttribute(
                                com.google.android.material.R.attr.textAppearanceTitleMedium,
                            ),
                        )
                    }
                    is ListingItemCompactBinding -> {
                        TextViewCompat.setTextAppearance(
                            rb.title,
                            context.getResIdFromAttribute(
                                com.google.android.material.R.attr.textAppearanceTitleMedium,
                            ),
                        )
                    }
                }
            } else {
                when (val rb = rawBinding) {
                    is ListingItemListBinding -> {
                        TextViewCompat.setTextAppearance(
                            rb.title,
                            context.getResIdFromAttribute(
                                com.google.android.material.R.attr.textAppearanceBodyMedium,
                            ),
                        )
                    }
                    is ListingItemCompactBinding -> {
                        TextViewCompat.setTextAppearance(
                            rb.title,
                            context.getResIdFromAttribute(
                                com.google.android.material.R.attr.textAppearanceBodyMedium,
                            ),
                        )
                    }
                }
            }

            holder.state.preferTitleText = postUiConfig.preferTitleText
        }

        scaleTextSizes()

        if (holder.state.preferImagesAtEnd != postUiConfig.preferImagesAtEnd) {
            if (postUiConfig.preferImagesAtEnd) {
                when (val rb = rawBinding) {
                    is ListingItemListBinding -> {
                        rb.image.updateLayoutParams<ConstraintLayout.LayoutParams> {
                            this.endToEnd = ConstraintLayout.LayoutParams.PARENT_ID
                            this.startToStart = ConstraintLayout.LayoutParams.UNSET
                            this.marginEnd = padding
                        }
                        rb.iconImage.updateLayoutParams<ConstraintLayout.LayoutParams> {
                            this.endToEnd = ConstraintLayout.LayoutParams.PARENT_ID
                            this.startToStart = ConstraintLayout.LayoutParams.UNSET
                            this.marginEnd = padding
                        }
                        rb.iconGoneSpacer.updateLayoutParams<ConstraintLayout.LayoutParams> {
                            this.endToEnd = ConstraintLayout.LayoutParams.PARENT_ID
                            this.startToStart = ConstraintLayout.LayoutParams.UNSET
                        }
                        rb.leftBarrier.type = Barrier.START
                        rb.title.updateLayoutParams<ConstraintLayout.LayoutParams> {
                            this.startToStart = ConstraintLayout.LayoutParams.PARENT_ID
                            this.endToStart = rb.leftBarrier.id
                            this.marginStart = padding
                            this.marginEnd = paddingHalf
                        }
                    }

                    is ListingItemCompactBinding -> {
                        rb.image.updateLayoutParams<ConstraintLayout.LayoutParams> {
                            this.endToEnd = ConstraintLayout.LayoutParams.PARENT_ID
                            this.startToStart = ConstraintLayout.LayoutParams.UNSET
                            this.marginEnd = padding
                        }
                        rb.iconImage.updateLayoutParams<ConstraintLayout.LayoutParams> {
                            this.endToEnd = ConstraintLayout.LayoutParams.PARENT_ID
                            this.startToStart = ConstraintLayout.LayoutParams.UNSET
                            this.marginEnd = padding
                        }
                        rb.iconGoneSpacer.updateLayoutParams<ConstraintLayout.LayoutParams> {
                            this.endToEnd = ConstraintLayout.LayoutParams.PARENT_ID
                            this.startToStart = ConstraintLayout.LayoutParams.UNSET
                        }
                        rb.leftBarrier.type = Barrier.START
                        rb.headerContainer.updateLayoutParams<ConstraintLayout.LayoutParams> {
                            this.startToStart = ConstraintLayout.LayoutParams.PARENT_ID
                            this.endToStart = rb.leftBarrier.id
                            this.marginStart = padding
                            this.marginEnd = paddingHalf
                        }
                        rb.title.updateLayoutParams<ConstraintLayout.LayoutParams> {
                            this.startToStart = ConstraintLayout.LayoutParams.PARENT_ID
                            this.endToStart = rb.leftBarrier.id
                            this.marginStart = padding
                            this.marginEnd = paddingHalf
                        }
                        rb.commentText.updateLayoutParams<ConstraintLayout.LayoutParams> {
                            this.startToStart = ConstraintLayout.LayoutParams.PARENT_ID
                            this.endToStart = rb.leftBarrier.id
                            this.marginStart = padding
                            this.marginEnd = paddingHalf
                        }
                    }

                    is ListingItemCardBinding -> {
                        rb.image.updateLayoutParams<ConstraintLayout.LayoutParams> {
                            this.topToTop = ConstraintLayout.LayoutParams.UNSET
                            this.topToBottom = rb.bottomBarrier.id
                        }
                        rb.headerView.updateLayoutParams<ConstraintLayout.LayoutParams> {
                            this.topToTop = ConstraintLayout.LayoutParams.PARENT_ID
                        }
                    }
                }
            }

            holder.state.preferImagesAtEnd = postUiConfig.preferImagesAtEnd
        }

        if (holder.state.preferFullSizeImages != postUiConfig.preferFullSizeImages) {
            if (postUiConfig.preferFullSizeImages) {
                when (val rb = rawBinding) {
                    is ListingItemCardBinding -> {
                        rb.image.updateLayoutParams<ConstraintLayout.LayoutParams> {
                            this.dimensionRatio = null
                        }
                    }
                    is ListingItemCard2Binding -> {
                        rb.image.updateLayoutParams<ConstraintLayout.LayoutParams> {
                            this.dimensionRatio = null
                        }
                    }
                    is ListingItemCard3Binding -> {
                        rb.image.updateLayoutParams<ConstraintLayout.LayoutParams> {
                            this.dimensionRatio = null
                        }
                    }

                    is ListingItemLargeListBinding -> {
                        rb.image.updateLayoutParams<ConstraintLayout.LayoutParams> {
                            this.dimensionRatio = null
                        }
                    }
                }
            } else {
                when (val rb = rawBinding) {
                    is ListingItemCardBinding -> {
                        rb.image.updateLayoutParams<ConstraintLayout.LayoutParams> {
                            this.dimensionRatio = "H,16:9"
                        }
                    }
                    is ListingItemCard2Binding -> {
                        rb.image.updateLayoutParams<ConstraintLayout.LayoutParams> {
                            this.dimensionRatio = "H,16:9"
                        }
                    }
                    is ListingItemCard3Binding -> {
                        rb.image.updateLayoutParams<ConstraintLayout.LayoutParams> {
                            this.dimensionRatio = "H,16:9"
                        }
                    }
                    is ListingItemLargeListBinding -> {
                        rb.image.updateLayoutParams<ConstraintLayout.LayoutParams> {
                            this.dimensionRatio = "H,16:9"
                        }
                    }
                }
            }

            holder.state.preferFullSizeImages = postUiConfig.preferFullSizeImages
        }

        val finalContentMaxWidth =
            when (val rb = rawBinding) {
                is ListingItemCardBinding -> {
                    val lp = rb.root.layoutParams as MarginLayoutParams
                    contentMaxWidth - lp.marginStart - lp.marginEnd
                }
                is ListingItemCard2Binding -> {
                    val lp = rb.root.layoutParams as MarginLayoutParams
                    contentMaxWidth - lp.marginStart - lp.marginEnd
                }
                is ListingItemCard3Binding -> {
                    val lp = rb.root.layoutParams as MarginLayoutParams
                    contentMaxWidth - lp.marginStart - lp.marginEnd
                }
                is ListingItemLargeListBinding -> {
                    val lp = rb.image.layoutParams as MarginLayoutParams
                    contentMaxWidth - lp.marginStart - lp.marginEnd
                }
                else -> contentMaxWidth
            }
        val postImageWidth = (postUiConfig.imageWidthPercent * finalContentMaxWidth).toInt()

        fun onItemClick() {
            val videoState = if (fullContentContainerView == null) {
                null
            } else {
                lemmyContentHelper.getState(fullContentContainerView).videoState?.let {
                    it.copy(currentTime = it.currentTime - ExoPlayerManager.CONVENIENCE_REWIND_TIME_MS)
                }
            }
            onItemClick(
                instance,
                postView.post.id,
                postView.community.toCommunityRef(),
                postView,
                false,
                isRevealed,
                videoState,
            )
        }

        if (updateContent) {
            lemmyHeaderHelper.populateHeaderSpan(
                headerContainer = headerContainer,
                postView = postView,
                instance = instance,
                onPageClick = onPageClick,
                onLinkLongClick = onLinkLongClick,
            )

            val thumbnailUrl = postView.post.thumbnail_url

            fun showDefaultImage() {
                image?.visibility = View.GONE
                iconImage?.visibility = View.VISIBLE
                iconImage?.setImageResource(R.drawable.baseline_article_24)
            }

            fun loadAndShowImage() {
                image ?: return

                val thumbnailImageUrl = if (thumbnailUrl != null &&
                    ContentUtils.isUrlImage(thumbnailUrl)
                ) {
                    thumbnailUrl
                } else {
                    null
                }

                val url = thumbnailImageUrl ?: postView.post.url

                if (url == "default") {
                    showDefaultImage()
                    return
                }

                if (url.isNullOrBlank()) {
                    image.visibility = View.GONE
                    return
                }

                if (!ContentUtils.isUrlImage(url)) {
                    image.visibility = View.GONE
                    return
                }

                image.visibility = View.VISIBLE
                iconImage?.visibility = View.GONE

                image.load(R.drawable.thumbnail_placeholder) {
                    if (image is ShapeableImageView) {
                        allowHardware(false)
                    }
                }

                offlineManager.fetchImageWithError(itemView, url, {
                    offlineManager.calculateImageMaxSizeIfNeeded(it)
                    offlineManager.getMaxImageSizeHint(it, tempSize)

                    image.load(it) {
                        if (image is ShapeableImageView) {
                            allowHardware(false)
                        }
                        fallback(R.drawable.thumbnail_placeholder)

                        if (!isRevealed && postView.post.nsfw) {
                            val sampling = (contentMaxWidth * 0.04f).coerceAtLeast(10f)

                            transformations(BlurTransformation(context, sampling = sampling))
                        }
                    }
                }, {
                    image.visibility = View.GONE
                },)
                image.transitionName = "image_$absoluteAdapterPosition"
                image.setOnClickListener {
                    if (fullContentContainerView != null) {
                        if (singleTapToViewImage) {
                            onImageClick(postView, image, url)
                        } else {
                            toggleItem(postView)
                        }
                    } else {
                        onImageClick(postView, image, url)
                    }
                }

                if (fullContentContainerView != null) {
                    image.setOnLongClickListener {
                        if (singleTapToViewImage) {
                            toggleItem(postView)
                        } else {
                            onImageClick(postView, image, url)
                        }
                        true
                    }
                }
            }

            linkTypeImage?.visibility = View.GONE
            iconImage?.visibility = View.GONE
            iconImage?.setOnClickListener(null)
            iconImage?.isClickable = false
            image?.setOnClickListener(null)
            image?.isClickable = false

            fun showFullContent() {
                fullContentContainerView ?: return

                lemmyContentHelper.setupFullContent(
                    reveal = isRevealed,
                    tempSize = tempSize,
                    videoViewMaxHeight = contentPreferredHeight,
                    contentMaxWidth = contentMaxWidth,
                    fullImageViewTransitionName = "full_image_$absoluteAdapterPosition",
                    postView = postView,
                    instance = instance,
                    rootView = itemView,
                    fullContentContainerView = fullContentContainerView,
                    onFullImageViewClickListener = { v, url ->
                        onImageClick(postView, v, url)
                    },
                    onImageClickListener = {
                        onImageClick(postView, null, it)
                    },
                    onVideoClickListener = onVideoClick,
                    onItemClickListener = {
                        onItemClick()
                    },
                    onRevealContentClickedFn = {
                        onRevealContentClickedFn()
                    },
                    onLemmyUrlClick = onPageClick,
                    onLinkLongClick = onLinkLongClick,
                )
            }

            when (postView.getType()) {
                PostType.Image -> {
                    loadAndShowImage()

                    if (isExpanded) {
                        showFullContent()
                    }
                }

                PostType.Video -> {
                    loadAndShowImage()

                    iconImage?.visibility = View.VISIBLE
                    iconImage?.setImageResource(R.drawable.baseline_play_circle_filled_24)
                    iconImage?.setOnClickListener {
                        if (fullContentContainerView != null) {
                            toggleItem(postView)
                        } else {
                            onItemClick()
                        }
                    }

                    if (isExpanded) {
                        showFullContent()
                    }
                }

                PostType.Link,
                PostType.Text,
                -> {
                    if (thumbnailUrl == null) {
                        image?.visibility = View.GONE

                        // see if this text post has additional content
                        val hasAdditionalContent =
                            !postView.post.body.isNullOrBlank() ||
                                !postView.post.url.isNullOrBlank()

                        if (hasAdditionalContent) {
                            showDefaultImage()
                            iconImage?.setOnClickListener {
                                if (fullContentContainerView != null) {
                                    toggleItem(postView)
                                } else {
                                    onItemClick()
                                }
                            }
                        }
                    } else {
                        loadAndShowImage()
                    }

                    if (isExpanded) {
                        showFullContent()
                    }

                    linkTypeImage?.visibility = View.GONE
                    linkTypeImage?.setImageResource(R.drawable.baseline_open_in_new_24)
                }
            }

            image?.updateLayoutParams {
                width = postImageWidth
            }
            iconImage?.updateLayoutParams {
                width = postImageWidth
            }

            if (layoutShowsFullContent) {
                showFullContent()
            }
        }

        LemmyTextHelper.bindText(
            title,
            postView.post.name,
            instance,
            onImageClick = {
                onImageClick(postView, null, it)
            },
            onPageClick = onPageClick,
            onLinkLongClick = onLinkLongClick,
        )

        if (postView.read && !alwaysRenderAsUnread) {
            if (themeManager.isLightTheme) {
                title.alpha = 0.41f
            } else {
                title.alpha = 0.66f
            }
        } else {
            title.alpha = 1f
        }

        commentText.text =
            LemmyUtils.abbrevNumber(postView.counts.comments.toLong())

        itemView.setOnClickListener {
            onItemClick()
        }
        commentButton?.setOnClickListener {
            onItemClick(
                instance,
                postView.post.id,
                postView.community.toCommunityRef(),
                postView,
                true,
                isRevealed,
                null,
            )
        }
        commentButton?.isEnabled = !postView.post.locked

        voteUiHandler.bind(
            viewLifecycleOwner,
            instance,
            postView,
            upvoteButton,
            downvoteButton,
            upvoteCount,
            null,
            onSignInRequired = onSignInRequired,
            onInstanceMismatch,
        )

        if (highlightForever) {
            highlightBg.visibility = View.VISIBLE
            highlightBg.alpha = 1f
        } else if (highlight) {
            highlightBg.visibility = View.VISIBLE
            highlightBg.animate()
                .alpha(0f)
                .apply {
                    duration = 350
                }
                .withEndAction {
                    highlightBg.visibility = View.GONE

                    onHighlightComplete()
                }
        } else {
            highlightBg.visibility = View.GONE
        }

        moreButton?.setOnClickListener {
            onShowMoreOptions(postView)
        }

        if (rawBinding is ListingItemCardBinding ||
            rawBinding is ListingItemCard2Binding ||
            rawBinding is ListingItemCard3Binding) {
            postView.post.url?.let { url ->
                openLinkButton?.visibility = View.VISIBLE
                openLinkButton?.setOnClickListener {
                    Utils.openExternalLink(context, url)
                }
            }
        } else {
            openLinkButton?.visibility = View.GONE
        }
        when (val rb = rawBinding) {
            is ListingItemCompactBinding -> {
                if (isActionsExpanded) {
                    upvoteButton?.visibility = View.VISIBLE
                    downvoteButton?.visibility = View.VISIBLE
                    createCommentButton?.visibility = View.VISIBLE
                    moreButton?.visibility = View.VISIBLE
                } else {
                    upvoteButton?.visibility = View.GONE
                    downvoteButton?.visibility = View.GONE
                    createCommentButton?.visibility = View.GONE
                    moreButton?.visibility = View.GONE
                }

                rb.root.setOnLongClickListener {
                    toggleActions(postView)
                    true
                }
            }
            else -> {
                rb.root.setOnLongClickListener {
                    onShowMoreOptions(postView)
                    true
                }
            }
        }
    }

    private fun ListingItemViewHolder.scaleTextSizes() {
        headerContainer.textSize = postUiConfig.headerTextSizeSp.toTextSize()
        title.textSize = postUiConfig.titleTextSizeSp.toTextSize()
        commentText.textSize = postUiConfig.footerTextSizeSp.toTextSize()
        upvoteCount.textSize = postUiConfig.footerTextSizeSp.toTextSize()
    }

    private fun Float.toTextSize(): Float =
        this * textSizeMultiplier * globalFontSizeMultiplier

    fun recycle(holder: ListingItemViewHolder) {
        if (holder.fullContentContainerView != null) {
            lemmyContentHelper.recycleFullContent(holder.fullContentContainerView)
        }
        offlineManager.cancelFetch(holder.itemView)
        voteUiHandler.unbindVoteUi(holder.upvoteCount)
    }
}
