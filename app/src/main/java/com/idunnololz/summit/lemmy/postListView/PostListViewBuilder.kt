package com.idunnololz.summit.lemmy.postListView

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams
import android.view.ViewGroup.MarginLayoutParams
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.Barrier
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.view.setPadding
import androidx.core.view.updateLayoutParams
import androidx.core.widget.TextViewCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.LifecycleOwner
import coil.load
import com.commit451.coiltransformations.BlurTransformation
import com.google.android.material.imageview.ShapeableImageView
import com.idunnololz.summit.R
import com.idunnololz.summit.account.Account
import com.idunnololz.summit.account.AccountActionsManager
import com.idunnololz.summit.account.AccountManager
import com.idunnololz.summit.api.dto.PostView
import com.idunnololz.summit.api.utils.PostType
import com.idunnololz.summit.api.utils.getType
import com.idunnololz.summit.api.utils.instance
import com.idunnololz.summit.coroutine.CoroutineScopeFactory
import com.idunnololz.summit.databinding.ListingItemCard2Binding
import com.idunnololz.summit.databinding.ListingItemCard3Binding
import com.idunnololz.summit.databinding.ListingItemCardBinding
import com.idunnololz.summit.databinding.ListingItemCompactBinding
import com.idunnololz.summit.databinding.ListingItemFullBinding
import com.idunnololz.summit.databinding.ListingItemLargeListBinding
import com.idunnololz.summit.databinding.ListingItemListBinding
import com.idunnololz.summit.databinding.SearchResultPostItemBinding
import com.idunnololz.summit.lemmy.CommunityRef
import com.idunnololz.summit.lemmy.LemmyContentHelper
import com.idunnololz.summit.lemmy.LemmyHeaderHelper
import com.idunnololz.summit.lemmy.LemmyTextHelper
import com.idunnololz.summit.lemmy.LemmyUtils
import com.idunnololz.summit.lemmy.PageRef
import com.idunnololz.summit.lemmy.toCommunityRef
import com.idunnololz.summit.lemmy.utils.bind
import com.idunnololz.summit.lemmy.utils.makeUpAndDownVoteButtons
import com.idunnololz.summit.links.LinkType
import com.idunnololz.summit.offline.OfflineManager
import com.idunnololz.summit.offline.TaskFailedListener
import com.idunnololz.summit.preferences.GlobalFontSizeId
import com.idunnololz.summit.preferences.PreferenceManager
import com.idunnololz.summit.preferences.Preferences
import com.idunnololz.summit.preferences.ThemeManager
import com.idunnololz.summit.preview.VideoType
import com.idunnololz.summit.util.ContentUtils
import com.idunnololz.summit.util.Size
import com.idunnololz.summit.util.ext.getDimen
import com.idunnololz.summit.util.ext.getResIdFromAttribute
import com.idunnololz.summit.video.ExoPlayerManager
import com.idunnololz.summit.video.VideoState
import dagger.hilt.android.qualifiers.ActivityContext
import dagger.hilt.android.scopes.ActivityScoped
import kotlinx.coroutines.launch
import javax.inject.Inject

@ActivityScoped
class PostListViewBuilder @Inject constructor(
    private val activity: FragmentActivity,
    @ActivityContext private val context: Context,
    private val offlineManager: OfflineManager,
    private val accountActionsManager: AccountActionsManager,
    private val preferenceManager: PreferenceManager,
    private val themeManager: ThemeManager,
    private val accountManager: AccountManager,
    private val coroutineScopeFactory: CoroutineScopeFactory,
) {

    private val coroutineScope = coroutineScopeFactory.create()

    companion object {
        private const val TAG = "PostListViewBuilder"
    }

    private var preferences = preferenceManager.getComposedPreferencesForAccount(
        accountManager.currentAccount.value)

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
    private var contentMaxLines: Int = postUiConfig.contentMaxLines
    private var showUpAndDownVotes: Boolean = preferences.showUpAndDownVotes
    private var displayInstanceStyle = preferences.displayInstanceStyle
    private var leftHandMode: Boolean = preferences.leftHandMode
    private var showPostUpvotePercentage: Boolean = preferences.showPostUpvotePercentage
    private var useMultilinePostHeaders: Boolean = preferences.useMultilinePostHeaders
    private var indicateCurrentUser: Boolean = preferences.indicatePostsAndCommentsCreatedByCurrentUser

    private val normalTextColor = ContextCompat.getColor(context, R.color.colorText)

    private val selectableItemBackground =
        context.getResIdFromAttribute(androidx.appcompat.R.attr.selectableItemBackground)
    private val selectableItemBackgroundBorderless =
        context.getResIdFromAttribute(androidx.appcompat.R.attr.selectableItemBackgroundBorderless)

    private val tempSize = Size()

    private var currentUser: Account? = null

    init {
        onPostUiConfigUpdated()

        coroutineScope.launch {
            accountManager.currentAccount.collect {
                currentUser = it

                preferences = preferenceManager.getComposedPreferencesForAccount(it)

                onPostUiConfigUpdated()
            }
        }
    }

    fun onPostUiConfigUpdated() {
        lemmyContentHelper.config = postUiConfig.fullContentConfig
        textSizeMultiplier = postUiConfig.textSizeMultiplier
        contentMaxLines = postUiConfig.contentMaxLines

        globalFontSizeMultiplier =
            GlobalFontSizeId.getFontSizeMultiplier(preferences.globalFontSize)
        lemmyContentHelper.globalFontSizeMultiplier = globalFontSizeMultiplier
        lemmyContentHelper.alwaysShowLinkBelowPost = preferences.alwaysShowLinkButtonBelowPost
        singleTapToViewImage = preferences.postListViewImageOnSingleTap
        showUpAndDownVotes = preferences.showUpAndDownVotes
        displayInstanceStyle = preferences.displayInstanceStyle
        leftHandMode = preferences.leftHandMode
        showPostUpvotePercentage = preferences.showPostUpvotePercentage
        useMultilinePostHeaders = preferences.useMultilinePostHeaders
        indicateCurrentUser = preferences.indicatePostsAndCommentsCreatedByCurrentUser
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
        onVideoLongClickListener: (url: String) -> Unit,
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
        onLinkClick: (url: String, text: String?, linkType: LinkType) -> Unit,
        onLinkLongClick: (url: String, text: String?) -> Unit,
    ) {
        var start = System.nanoTime()

        val url = postView.post.url
        val thumbnailUrl = postView.post.thumbnail_url
        with(holder) {
            if (holder.state.preferUpAndDownVotes != showUpAndDownVotes) {
                when (val rb = rawBinding) {
                    is ListingItemCompactBinding -> {
                        val downvoteCount = TextView(
                            context,
                        ).apply {
                            id = View.generateViewId()
                            layoutParams = ConstraintLayout.LayoutParams(
                                ConstraintLayout.LayoutParams.WRAP_CONTENT,
                                ConstraintLayout.LayoutParams.WRAP_CONTENT,
                            ).apply {
                                startToEnd = R.id.score_text
                                baselineToBaseline = R.id.comment_text
                                marginStart = context.getDimen(R.dimen.padding_half)
                            }
                            setCompoundDrawablesRelativeWithIntrinsicBounds(
                                R.drawable.baseline_arrow_downward_16,
                                0,
                                0,
                                0,
                            )
                            TextViewCompat.setCompoundDrawableTintList(
                                this,
                                ColorStateList.valueOf(normalTextColor),
                            )
                            includeFontPadding = false
                            setBackgroundResource(selectableItemBackground)
                            gravity = Gravity.CENTER
                        }.also {
                            downvoteCount = it
                        }
                        rb.root.addView(downvoteCount)
                    }
                    is ListingItemListBinding -> {
                        ensureActionButtons(rb.root, leftHandMode)
                    }
                    is ListingItemCardBinding -> {
                        ensureActionButtons(rb.constraintLayout, leftHandMode)
                        rb.bottomBarrier.referencedIds = intArrayOf(commentButton!!.id)
                        commentButton!!.updateLayoutParams<ConstraintLayout.LayoutParams> {
                            topToBottom = R.id.title
                            bottomToBottom = ConstraintLayout.LayoutParams.UNSET
                            bottomToTop = ConstraintLayout.LayoutParams.UNSET
                        }
                    }
                    is ListingItemCard2Binding -> {
                        ensureActionButtons(rb.constraintLayout, leftHandMode)
                        commentButton!!.updateLayoutParams<ConstraintLayout.LayoutParams> {
                            topToBottom = R.id.image
                            bottomToBottom = ConstraintLayout.LayoutParams.UNSET
                            bottomToTop = ConstraintLayout.LayoutParams.UNSET
                        }
                    }
                    is ListingItemCard3Binding -> {
                        ensureActionButtons(rb.constraintLayout, leftHandMode)
                        commentButton!!.updateLayoutParams<ConstraintLayout.LayoutParams> {
                            topToBottom = R.id.header_view
                            bottomToBottom = ConstraintLayout.LayoutParams.UNSET
                            bottomToTop = ConstraintLayout.LayoutParams.UNSET
                        }
                    }
                    is ListingItemLargeListBinding -> {
                        ensureActionButtons(rb.root, leftHandMode)
                        commentButton!!.updateLayoutParams<ConstraintLayout.LayoutParams> {
                            topToBottom = R.id.image
                            bottomToBottom = ConstraintLayout.LayoutParams.UNSET
                            bottomToTop = ConstraintLayout.LayoutParams.UNSET
                        }
                    }
                    is ListingItemFullBinding -> {
                        ensureActionButtons(rb.root, leftHandMode)
                        commentButton!!.updateLayoutParams<ConstraintLayout.LayoutParams> {
                            topToBottom = R.id.bottom_barrier
                            bottomToBottom = ConstraintLayout.LayoutParams.UNSET
                            bottomToTop = ConstraintLayout.LayoutParams.UNSET
                        }
                    }
                    is SearchResultPostItemBinding -> {
                        ensureActionButtons(rb.root, leftHandMode)
                        commentButton!!.updateLayoutParams<ConstraintLayout.LayoutParams> {
                            topToBottom = R.id.bottom_barrier
                            bottomToBottom = ConstraintLayout.LayoutParams.UNSET
                            bottomToTop = ConstraintLayout.LayoutParams.UNSET
                        }
                    }
                }

                holder.state.preferUpAndDownVotes = showUpAndDownVotes
            }

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

            Log.d("HAHA", "s1: ${System.nanoTime() - start}")
            start = System.nanoTime()

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

            Log.d("HAHA", "s2: ${System.nanoTime() - start}")
            start = System.nanoTime()

            val preferFullSizeImages = postUiConfig.preferFullSizeImages &&
                (rawBinding is ListingItemCardBinding) ||
                (rawBinding is ListingItemCard2Binding) ||
                (rawBinding is ListingItemCard3Binding) ||
                (rawBinding is ListingItemLargeListBinding)
            if (holder.state.preferFullSizeImages != preferFullSizeImages) {
                if (preferFullSizeImages) {
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

                holder.state.preferFullSizeImages = preferFullSizeImages
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

            Log.d("HAHA", "s3: ${System.nanoTime() - start}")
            start = System.nanoTime()

            val postType = postView.getType()
            if (updateContent) {
                lemmyHeaderHelper.populateHeaderSpan(
                    headerContainer = headerContainer,
                    postView = postView,
                    instance = instance,
                    onPageClick = onPageClick,
                    onLinkClick = onLinkClick,
                    onLinkLongClick = onLinkLongClick,
                    displayInstanceStyle = displayInstanceStyle,
                    showUpvotePercentage = showPostUpvotePercentage,
                    useMultilineHeader = false,
                    wrapHeader = useMultilinePostHeaders,
                    isCurrentUser = if (indicateCurrentUser) {
                        currentUser?.id == postView.creator.id &&
                            currentUser?.instance == postView.creator.instance
                    } else {
                        false
                    },
                )

                fun showDefaultImage() {
                    image?.visibility = View.GONE
                    iconImage?.visibility = View.VISIBLE
                    iconImage?.setImageResource(R.drawable.baseline_article_24)
                }

                fun loadAndShowImage() {
                    if (postView.post.removed) {
                        // Do not show the image if the post is removed.
                        return
                    }
                    val image = image ?: return

                    val thumbnailImageUrl = if (thumbnailUrl != null &&
                        ContentUtils.isUrlImage(thumbnailUrl)
                    ) {
                        thumbnailUrl
                    } else {
                        null
                    }

                    val imageUrl = thumbnailImageUrl ?: url
                    val backupImageUrl = if (imageUrl != url &&
                        url != null &&
                        ContentUtils.isUrlImage(url)
                    ) {
                        url
                    } else {
                        null
                    }

                    if (imageUrl == "default") {
                        showDefaultImage()
                        return
                    }

                    if (imageUrl.isNullOrBlank()) {
                        image.visibility = View.GONE
                        return
                    }

                    if (!ContentUtils.isUrlImage(imageUrl)) {
                        image.visibility = View.GONE
                        return
                    }

                    image.visibility = View.VISIBLE
                    iconImage?.visibility = View.GONE

                    image.load(R.drawable.thumbnail_placeholder_16_9) {
                        if (image is ShapeableImageView) {
                            allowHardware(false)
                        }
                    }

                    fun loadImage(useBackupUrl: Boolean = false) {
                        val urlToLoad = if (useBackupUrl) {
                            backupImageUrl
                        } else {
                            imageUrl
                        }

                        urlToLoad ?: return

                        loadImage(
                            itemView,
                            image,
                            urlToLoad,
                            state.preferFullSizeImages,
                            contentMaxWidth,
                            !isRevealed && postView.post.nsfw,
                        ) {
                            if (!useBackupUrl && backupImageUrl != null) {
                                loadImage(true)
                            }
                        }
                    }

                    loadImage()

                    image.transitionName = "image_$absoluteAdapterPosition"
                    image.setOnClickListener {
                        if (fullContentContainerView != null) {
                            if (singleTapToViewImage) {
                                onImageClick(postView, image, imageUrl)
                            } else {
                                toggleItem(postView)
                            }
                        } else {
                            if (url != null && (postType == PostType.Text || postType == PostType.Link)) {
                                onLinkClick(url, null, LinkType.Rich)
                            } else {
                                onImageClick(postView, image, imageUrl)
                            }
                        }
                    }

                    if (fullContentContainerView != null) {
                        image.setOnLongClickListener {
                            if (singleTapToViewImage) {
                                toggleItem(postView)
                            } else {
                                onImageClick(postView, image, imageUrl)
                            }
                            true
                        }
                    } else {
                        image.setOnLongClickListener {
                            onImageClick(postView, image, imageUrl)
                            true
                        }
                    }
                }

                Log.d("HAHA", "s4: ${System.nanoTime() - start}")
                start = System.nanoTime()

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
                        contentMaxLines = contentMaxLines,
                        onFullImageViewClickListener = { v, url ->
                            onImageClick(postView, v, url)
                        },
                        onImageClickListener = {
                            onImageClick(postView, null, it)
                        },
                        onVideoClickListener = onVideoClick,
                        onVideoLongClickListener = onVideoLongClickListener,
                        onItemClickListener = {
                            onItemClick()
                        },
                        onRevealContentClickedFn = {
                            onRevealContentClickedFn()
                        },
                        onLemmyUrlClick = onPageClick,
                        onLinkClick = onLinkClick,
                        onLinkLongClick = onLinkLongClick,
                    )
                }

                when (postType) {
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
                                    !url.isNullOrBlank()

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
                    }
                }

                if (image != null && image.width != postImageWidth) {
                    image.updateLayoutParams {
                        width = postImageWidth
                    }
                }
                if (iconImage != null && iconImage.width != postImageWidth) {
                    iconImage.updateLayoutParams {
                        width = postImageWidth
                    }
                }

                if (layoutShowsFullContent) {
                    showFullContent()
                }
            }

            Log.d("HAHA", "s5: ${System.nanoTime() - start}")
            start = System.nanoTime()

            LemmyTextHelper.bindText(
                title,
                postView.post.name,
                instance,
                onImageClick = {
                    onImageClick(postView, null, it)
                },
                onVideoClick = {
                    onVideoClick(it, VideoType.Unknown, null)
                },
                onPageClick = onPageClick,
                onLinkClick = onLinkClick,
                onLinkLongClick = onLinkLongClick,
            )

            Log.d("HAHA", "s6: ${System.nanoTime() - start}")
            start = System.nanoTime()

            if (postView.read && !alwaysRenderAsUnread) {
                if (themeManager.isLightTheme) {
                    title.alpha = 0.41f
                } else {
                    title.alpha = 0.66f
                }
            } else {
                title.alpha = 1f
            }

            commentText?.text =
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

            Log.d("HAHA", "s7: ${System.nanoTime() - start}")
            start = System.nanoTime()
            val scoreCount: TextView? = upvoteCount
            if (scoreCount != null) {
                val upvoteCount: TextView?
                val downvoteCount: TextView?

                if (this.downvoteCount != null) {
                    upvoteCount = this.upvoteCount
                    downvoteCount = this.downvoteCount
                } else {
                    upvoteCount = null
                    downvoteCount = null
                }

                voteUiHandler.bind(
                    viewLifecycleOwner,
                    instance,
                    postView,
                    upvoteButton,
                    downvoteButton,
                    scoreCount,
                    upvoteCount,
                    downvoteCount,
                    onUpdate = null,
                    onSignInRequired = onSignInRequired,
                    onInstanceMismatch,
                )
            }

            Log.d("HAHA", "s8: ${System.nanoTime() - start}")
            start = System.nanoTime()

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
                rawBinding is ListingItemCard3Binding ||
                rawBinding is ListingItemLargeListBinding
            ) {
                if (url == null) {
                    openLinkButton?.visibility = View.GONE
                    linkText?.visibility = View.GONE
                    linkIcon?.visibility = View.GONE
                    linkOverlay?.visibility = View.GONE
                } else {
                    if ((postType == PostType.Link || postType == PostType.Text) && thumbnailUrl != null) {
                        openLinkButton?.visibility = View.GONE
                        linkText?.visibility = View.VISIBLE
                        linkIcon?.visibility = View.VISIBLE
                        linkOverlay?.visibility = View.VISIBLE

                        val t = Uri.parse(url).host ?: url
                        linkText?.text = t
                        linkOverlay?.setOnClickListener {
                            onLinkClick(url, null, LinkType.Rich)
                        }
                        linkOverlay?.setOnLongClickListener {
                            onLinkLongClick(url, t)
                            true
                        }
                    } else {
                        openLinkButton?.visibility = View.VISIBLE
                        linkText?.visibility = View.GONE
                        linkIcon?.visibility = View.GONE
                        linkOverlay?.visibility = View.GONE
                        openLinkButton?.setOnClickListener {
                            onLinkClick(url, null, LinkType.Action)
                        }
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

            Log.d("HAHA", "s9: ${System.nanoTime() - start}")
            start = System.nanoTime()
        }
    }

    private fun ListingItemViewHolder.scaleTextSizes() {
        headerContainer.textSize = postUiConfig.headerTextSizeSp.toTextSize() * 0.9f

        title.textSize = postUiConfig.titleTextSizeSp.toTextSize()
        commentText?.textSize = postUiConfig.footerTextSizeSp.toTextSize()
        upvoteCount?.textSize = postUiConfig.footerTextSizeSp.toTextSize()
        downvoteCount?.textSize = postUiConfig.footerTextSizeSp.toTextSize()
    }

    private fun Float.toTextSize(): Float =
        this * textSizeMultiplier * globalFontSizeMultiplier

    fun recycle(holder: ListingItemViewHolder) {
        if (holder.fullContentContainerView != null) {
            lemmyContentHelper.recycleFullContent(holder.fullContentContainerView)
        }
        offlineManager.cancelFetch(holder.itemView)
        holder.upvoteCount?.let {
            voteUiHandler.unbindVoteUi(it)
        }
    }

    private fun ListingItemViewHolder.ensureActionButtons(root: ViewGroup, leftHandMode: Boolean) {
        root.removeView(commentButton)
        root.removeView(upvoteButton)
        root.removeView(downvoteButton)
        root.removeView(upvoteCount)

        if (showUpAndDownVotes) {
            val commentButton = TextView(
                context,
            ).apply {
                id = View.generateViewId()
                layoutParams = ConstraintLayout.LayoutParams(
                    ConstraintLayout.LayoutParams.WRAP_CONTENT,
                    ConstraintLayout.LayoutParams.WRAP_CONTENT,
                ).apply {
                    if (leftHandMode) {
                        endToEnd = ConstraintLayout.LayoutParams.PARENT_ID
                    } else {
                        startToStart = ConstraintLayout.LayoutParams.PARENT_ID
                    }
                    topToBottom = R.id.bottom_barrier
                }
                setCompoundDrawablesRelativeWithIntrinsicBounds(
                    R.drawable.baseline_comment_18,
                    0,
                    0,
                    0,
                )
                TextViewCompat.setCompoundDrawableTintList(
                    this,
                    ColorStateList.valueOf(normalTextColor),
                )
                compoundDrawablePadding = context.getDimen(R.dimen.padding_half)
                includeFontPadding = false
                setPadding(context.getDimen(R.dimen.padding))
                setBackgroundResource(selectableItemBackground)
            }.also {
                commentButton = it
                commentText = it
            }
            root.addView(commentButton)

            val buttons = makeUpAndDownVoteButtons(context)

            if (leftHandMode) {
                buttons.upvoteButton.updateLayoutParams<ConstraintLayout.LayoutParams> {
                    topToTop = commentButton.id
                    bottomToBottom = commentButton.id

                    startToStart = ConstraintLayout.LayoutParams.PARENT_ID
                    marginStart = context.getDimen(R.dimen.padding)
                }
                buttons.downvoteButton.updateLayoutParams<ConstraintLayout.LayoutParams> {
                    topToTop = commentButton.id
                    bottomToBottom = commentButton.id

                    startToEnd = buttons.upvoteButton.id
                }
            } else {
                buttons.downvoteButton.updateLayoutParams<ConstraintLayout.LayoutParams> {
                    topToTop = commentButton.id
                    bottomToBottom = commentButton.id

                    endToEnd = ConstraintLayout.LayoutParams.PARENT_ID
                    marginEnd = context.getDimen(R.dimen.padding)
                }
                buttons.upvoteButton.updateLayoutParams<ConstraintLayout.LayoutParams> {
                    topToTop = commentButton.id
                    bottomToBottom = commentButton.id

                    endToStart = buttons.downvoteButton.id
                }
            }
            root.addView(buttons.upvoteButton)
            root.addView(buttons.downvoteButton)

            upvoteButton = buttons.upvoteButton
            upvoteCount = buttons.upvoteButton
            downvoteButton = buttons.downvoteButton
            downvoteCount = buttons.downvoteButton
        } else {
            val commentButton = TextView(
                context,
            ).apply {
                id = View.generateViewId()
                layoutParams = ConstraintLayout.LayoutParams(
                    ConstraintLayout.LayoutParams.WRAP_CONTENT,
                    ConstraintLayout.LayoutParams.WRAP_CONTENT,
                ).apply {
                    if (leftHandMode) {
                        endToEnd = ConstraintLayout.LayoutParams.PARENT_ID
                    } else {
                        startToStart = ConstraintLayout.LayoutParams.PARENT_ID
                    }
                    topToBottom = R.id.bottom_barrier
                }
                setCompoundDrawablesRelativeWithIntrinsicBounds(
                    R.drawable.baseline_comment_18,
                    0,
                    0,
                    0,
                )
                TextViewCompat.setCompoundDrawableTintList(
                    this,
                    ColorStateList.valueOf(normalTextColor),
                )
                compoundDrawablePadding = context.getDimen(R.dimen.padding_half)
                includeFontPadding = false
                setPadding(context.getDimen(R.dimen.padding))
                setBackgroundResource(selectableItemBackground)
            }.also {
                commentButton = it
                commentText = it
            }
            root.addView(commentButton)

            val button1 = ImageView(
                context,
            ).apply {
                id = View.generateViewId()
                layoutParams = ConstraintLayout.LayoutParams(
                    ConstraintLayout.LayoutParams.WRAP_CONTENT,
                    ConstraintLayout.LayoutParams.WRAP_CONTENT,
                ).apply {
                    topToTop = commentButton.id
                    bottomToBottom = commentButton.id
                    if (leftHandMode) {
                        startToStart = ConstraintLayout.LayoutParams.PARENT_ID
                        marginStart = context.getDimen(R.dimen.padding_quarter)
                    } else {
                        endToEnd = ConstraintLayout.LayoutParams.PARENT_ID
                        marginEnd = context.getDimen(R.dimen.padding_quarter)
                    }
                }
                setPadding(context.getDimen(R.dimen.padding_half))
                setBackgroundResource(selectableItemBackgroundBorderless)
            }
            root.addView(button1)

            val upvoteCount = TextView(
                context,
            ).apply {
                id = View.generateViewId()
                layoutParams = ConstraintLayout.LayoutParams(
                    ConstraintLayout.LayoutParams.WRAP_CONTENT,
                    ConstraintLayout.LayoutParams.WRAP_CONTENT,
                ).apply {
                    topToTop = commentButton.id
                    bottomToBottom = commentButton.id

                    if (leftHandMode) {
                        startToEnd = button1.id
                    } else {
                        endToStart = button1.id
                    }
                }
            }.also {
                upvoteCount = it
            }
            root.addView(upvoteCount)

            val button2 = ImageView(
                context,
            ).apply {
                id = View.generateViewId()
                layoutParams = ConstraintLayout.LayoutParams(
                    ConstraintLayout.LayoutParams.WRAP_CONTENT,
                    ConstraintLayout.LayoutParams.WRAP_CONTENT,
                ).apply {
                    topToTop = commentButton.id
                    bottomToBottom = commentButton.id

                    if (leftHandMode) {
                        startToEnd = upvoteCount.id
                    } else {
                        endToStart = upvoteCount.id
                    }
                }
                setPadding(context.getDimen(R.dimen.padding_half))
                setBackgroundResource(selectableItemBackgroundBorderless)
            }
            root.addView(button2)

            if (leftHandMode) {
                button1.setImageResource(R.drawable.baseline_arrow_upward_24)
                button2.setImageResource(R.drawable.baseline_arrow_downward_24)

                upvoteButton = button1
                downvoteButton = button2
            } else {
                button2.setImageResource(R.drawable.baseline_arrow_upward_24)
                button1.setImageResource(R.drawable.baseline_arrow_downward_24)

                upvoteButton = button2
                downvoteButton = button1
            }
        }
    }

    fun loadImage(
        rootView: View,
        imageView: ImageView,
        imageUrl: String,
        preferFullSizeImage: Boolean,
        contentMaxWidth: Int,
        shouldBlur: Boolean,
        errorListener: TaskFailedListener?,
    ) {
        val urlToLoad = imageUrl

        offlineManager.getImageSizeHint(imageUrl, tempSize)

        if (preferFullSizeImage) {
            if (tempSize.width > 0 && tempSize.height > 0) {
                val thumbnailMaxHeight =
                    (contentMaxWidth * (tempSize.height.toDouble() / tempSize.width)).toInt()

                Log.d(TAG, "Reserving space for image ${thumbnailMaxHeight}h")
                imageView.updateLayoutParams<LayoutParams> {
                    this.height = thumbnailMaxHeight
                }
            } else {
                imageView.updateLayoutParams<LayoutParams> {
                    this.height = LayoutParams.WRAP_CONTENT
                }
            }
        }

        offlineManager.fetchImageWithError(
            rootView = rootView,
            url = urlToLoad,
            listener = {
                offlineManager.getMaxImageSizeHint(it, tempSize)

                var w: Int? = null
                var h: Int? = null
                if (tempSize.height > 0 && tempSize.width > 0) {
                    val heightToWidthRatio = tempSize.height / tempSize.width

                    if (heightToWidthRatio > 10) {
                        // shrink the image if needed
                        w = tempSize.width
                        h = tempSize.height
                    }
                }

                imageView.load(it) {
                    if (imageView is ShapeableImageView) {
                        allowHardware(false)
                    }

                    if (w != null && h != null) {
                        this.size(w, h)
                    }
                    //                                fallback(R.drawable.thumbnail_placeholder_16_9)
                    //                                placeholder(R.drawable.thumbnail_placeholder_16_9)

                    if (shouldBlur) {
                        val sampling = (contentMaxWidth * 0.04f).coerceAtLeast(10f)

                        transformations(BlurTransformation(context, sampling = sampling))
                    }

                    listener { _, result ->
                        val d = result.drawable
                        if (d is BitmapDrawable) {
                            offlineManager.setImageSizeHint(
                                imageUrl,
                                d.bitmap.width,
                                d.bitmap.height,
                            )
                        }
                    }
                }
            },
            errorListener = errorListener,
        )
    }
}
