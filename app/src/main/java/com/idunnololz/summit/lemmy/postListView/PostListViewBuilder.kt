package com.idunnololz.summit.lemmy.postListView

import android.content.Context
import android.view.View
import androidx.constraintlayout.widget.Barrier
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.LifecycleOwner
import coil.load
import com.commit451.coiltransformations.BlurTransformation
import com.idunnololz.summit.R
import com.idunnololz.summit.account.AccountActionsManager
import com.idunnololz.summit.api.dto.PostView
import com.idunnololz.summit.api.utils.PostType
import com.idunnololz.summit.api.utils.getType
import com.idunnololz.summit.databinding.ListingItemCompactBinding
import com.idunnololz.summit.databinding.ListingItemListBinding
import com.idunnololz.summit.lemmy.CommunityRef
import com.idunnololz.summit.lemmy.LemmyContentHelper
import com.idunnololz.summit.lemmy.LemmyHeaderHelper
import com.idunnololz.summit.lemmy.LemmyTextHelper
import com.idunnololz.summit.lemmy.PageRef
import com.idunnololz.summit.lemmy.toCommunityRef
import com.idunnololz.summit.lemmy.utils.bind
import com.idunnololz.summit.lemmy.utils.getFormattedTitle
import com.idunnololz.summit.offline.OfflineManager
import com.idunnololz.summit.preferences.Preferences
import com.idunnololz.summit.preview.VideoType
import com.idunnololz.summit.reddit.LemmyUtils
import com.idunnololz.summit.util.Size
import com.idunnololz.summit.util.Utils
import com.idunnololz.summit.util.ext.getDimen
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
) {

    var postUiConfig: PostInListUiConfig = preferences.getPostInListUiConfig()
        set(value) {
            field = value

            lemmyContentHelper.config = value.fullContentConfig

            postImageWidth = (Utils.getScreenWidth(context) * postUiConfig.imageWidthPercent).toInt()
            textSizeMultiplier = postUiConfig.textSizeMultiplier
        }

    private val lemmyHeaderHelper = LemmyHeaderHelper(context)
    private val lemmyContentHelper = LemmyContentHelper(
        context,
        offlineManager,
        ExoPlayerManager.get(activity),
    )

    private val padding = context.getDimen(R.dimen.padding)
    private val paddingHalf = context.getDimen(R.dimen.padding_half)

    private val voteUiHandler = accountActionsManager.voteUiHandler
    private var postImageWidth: Int = (Utils.getScreenWidth(context) * postUiConfig.imageWidthPercent).toInt()
    private var textSizeMultiplier: Float = postUiConfig.textSizeMultiplier

    private val tempSize = Size()

    /**
     * @param updateContent False for a fast update (also removes flickering)
     */
    fun bind(
        holder: ListingItemViewHolder,
        container: View,
        postView: PostView,
        instance: String,
        isRevealed: Boolean,
        contentMaxWidth: Int,
        viewLifecycleOwner: LifecycleOwner,
        isExpanded: Boolean,
        isActionsExpanded: Boolean,
        updateContent: Boolean,
        highlight: Boolean,
        highlightForever: Boolean,
        onRevealContentClickedFn: () -> Unit,
        onImageClick: (sharedElementView: View?, String) -> Unit,
        onVideoClick: (url: String, videoType: VideoType, videoState: VideoState?) -> Unit,
        onPageClick: (PageRef) -> Unit,
        onItemClick: (
            instance: String,
            id: Int,
            currentCommunity: CommunityRef?,
            post: PostView,
            jumpToComments: Boolean,
            reveal: Boolean,
            videoState: VideoState?
        ) -> Unit,
        onShowMoreOptions: (PostView) -> Unit,
        toggleItem: (postView: PostView) -> Unit,
        toggleActions: (postView: PostView) -> Unit,
        onSignInRequired: () -> Unit,
        onInstanceMismatch: (String, String) -> Unit,
        onHighlightComplete: () -> Unit,
    ) = with(holder) {

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
                }
            }

            holder.state.preferImagesAtEnd = postUiConfig.preferImagesAtEnd
        }


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
                videoState
            )
        }

        if (updateContent) {
            lemmyHeaderHelper.populateHeaderSpan(
                headerContainer = headerContainer,
                postView = postView,
                instance = instance,
                onPageClick = onPageClick,
            )

            val thumbnailUrl = postView.post.thumbnail_url

            fun showDefaultImage() {
                image?.visibility = View.GONE
                iconImage?.visibility = View.VISIBLE
                iconImage?.setImageResource(R.drawable.baseline_article_24)
            }

            fun loadAndShowImage() {
                image ?: return

                val url = thumbnailUrl ?: postView.post.url

                if (url == "default") {
                    showDefaultImage()
                    return
                }

                if (url.isNullOrBlank()) {
                    image.visibility = View.GONE
                    return
                }

                image.visibility = View.VISIBLE
                iconImage?.visibility = View.GONE

                image.load(R.drawable.thumbnail_placeholder)

                offlineManager.fetchImage(itemView, url) {
                    image.load(it) {
                        placeholder(R.drawable.thumbnail_placeholder)

                        if (!isRevealed && postView.post.nsfw) {
                            transformations(BlurTransformation(context, sampling = 10f))
                        }
                    }
                }
                image.transitionName = "image_$absoluteAdapterPosition"
                image.setOnClickListener {
                    if (fullContentContainerView != null) {
                        toggleItem(postView)
                    } else {
                        onImageClick(image, url)
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
                    videoViewMaxHeight = (container.height
                            - Utils.convertDpToPixel(56f)
                            - Utils.convertDpToPixel(16f)
                            ).toInt(),
                    contentMaxWidth = contentMaxWidth,
                    fullImageViewTransitionName = "full_image_$absoluteAdapterPosition",
                    postView = postView,
                    instance = instance,
                    rootView = itemView,
                    fullContentContainerView = fullContentContainerView,
                    onFullImageViewClickListener = { v, url ->
                        onImageClick(v, url)
                    },
                    onImageClickListener = {
                        onImageClick(null, it)
                    },
                    onVideoClickListener = onVideoClick,
                    onItemClickListener = {
                        onItemClick()
                    },
                    onRevealContentClickedFn = {
                        onRevealContentClickedFn()
                    },
                    onLemmyUrlClick = onPageClick,
                )
            }

            when (postView.getType()) {
                PostType.ImageUrl,
                PostType.Image -> {
                    loadAndShowImage()

                    if (isExpanded) {
                        showFullContent()
                    }
                }

                PostType.Video -> {
                    loadAndShowImage()

                    iconImage?.visibility = View.VISIBLE
                    iconImage?.setImageResource(R.drawable.baseline_play_circle_filled_black_24)

                    if (isExpanded) {
                        showFullContent()
                    }
                }

                PostType.Text -> {
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
                    linkTypeImage?.setImageResource(R.drawable.baseline_open_in_new_black_18)
                }
            }

            image?.layoutParams = image?.layoutParams?.apply {
                width = postImageWidth
            }
            iconImage?.layoutParams = iconImage?.layoutParams?.apply {
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
                onImageClick(null, it)
            },
            onPageClick = onPageClick
        )

        if (postView.read) {
            title.alpha = 0.66f
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
                null
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
        this * textSizeMultiplier

    fun recycle(holder: ListingItemViewHolder) {
        if (holder.fullContentContainerView != null) {
            lemmyContentHelper.recycleFullContent(holder.fullContentContainerView)
        }
        offlineManager.cancelFetch(holder.itemView)
        voteUiHandler.unbindVoteUi(holder.upvoteCount)
    }
}