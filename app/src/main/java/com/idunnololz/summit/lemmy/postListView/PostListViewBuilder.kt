package com.idunnololz.summit.lemmy.postListView

import android.content.Context
import android.content.res.ColorStateList
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
import androidx.core.view.ViewCompat
import androidx.core.view.setPadding
import androidx.core.view.updateLayoutParams
import androidx.core.widget.TextViewCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.LifecycleOwner
import androidx.viewbinding.ViewBinding
import coil.dispose
import coil.load
import com.commit451.coiltransformations.BlurTransformation
import com.google.android.material.button.MaterialButton
import com.google.android.material.imageview.ShapeableImageView
import com.idunnololz.summit.R
import com.idunnololz.summit.account.Account
import com.idunnololz.summit.account.AccountActionsManager
import com.idunnololz.summit.account.AccountManager
import com.idunnololz.summit.account.asAccount
import com.idunnololz.summit.api.dto.PostView
import com.idunnololz.summit.api.utils.PostType
import com.idunnololz.summit.api.utils.getType
import com.idunnololz.summit.api.utils.instance
import com.idunnololz.summit.avatar.AvatarHelper
import com.idunnololz.summit.coroutine.CoroutineScopeFactory
import com.idunnololz.summit.databinding.ListingItemCard2Binding
import com.idunnololz.summit.databinding.ListingItemCard3Binding
import com.idunnololz.summit.databinding.ListingItemCardBinding
import com.idunnololz.summit.databinding.ListingItemCompactBinding
import com.idunnololz.summit.databinding.ListingItemFullBinding
import com.idunnololz.summit.databinding.ListingItemLargeListBinding
import com.idunnololz.summit.databinding.ListingItemListBinding
import com.idunnololz.summit.databinding.ListingItemListWithCardsBinding
import com.idunnololz.summit.databinding.SearchResultPostItemBinding
import com.idunnololz.summit.lemmy.CommunityRef
import com.idunnololz.summit.lemmy.LemmyContentHelper
import com.idunnololz.summit.lemmy.LemmyHeaderHelper
import com.idunnololz.summit.lemmy.LemmyTextHelper
import com.idunnololz.summit.lemmy.LemmyUtils
import com.idunnololz.summit.lemmy.PageRef
import com.idunnololz.summit.lemmy.community.CommunityLayout
import com.idunnololz.summit.lemmy.multicommunity.FetchedPost
import com.idunnololz.summit.lemmy.multicommunity.Source
import com.idunnololz.summit.lemmy.multicommunity.accountId
import com.idunnololz.summit.lemmy.toCommunityRef
import com.idunnololz.summit.lemmy.utils.bind
import com.idunnololz.summit.lemmy.utils.makeUpAndDownVoteButtons
import com.idunnololz.summit.links.LinkContext
import com.idunnololz.summit.offline.OfflineManager
import com.idunnololz.summit.offline.TaskFailedListener
import com.idunnololz.summit.preferences.GlobalFontSizeId
import com.idunnololz.summit.preferences.PreferenceManager
import com.idunnololz.summit.preferences.ThemeManager
import com.idunnololz.summit.preview.VideoType
import com.idunnololz.summit.util.ContentUtils
import com.idunnololz.summit.util.Size
import com.idunnololz.summit.util.Utils
import com.idunnololz.summit.util.coil.VideoWatermarkTransformation
import com.idunnololz.summit.util.ext.getDimen
import com.idunnololz.summit.util.ext.getResIdFromAttribute
import com.idunnololz.summit.util.ext.getSize
import com.idunnololz.summit.util.shimmer.newShimmerDrawable16to9
import com.idunnololz.summit.video.ExoPlayerManager
import com.idunnololz.summit.video.VideoState
import com.idunnololz.summit.view.LemmyHeaderView.Companion.DEFAULT_ICON_SIZE_DP
import dagger.hilt.android.qualifiers.ActivityContext
import dagger.hilt.android.scopes.ActivityScoped
import javax.inject.Inject
import kotlinx.coroutines.launch

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
    private val avatarHelper: AvatarHelper,
    private val lemmyHeaderHelperFactory: LemmyHeaderHelper.Factory,
) {

    private val coroutineScope = coroutineScopeFactory.create()

    companion object {
        private const val TAG = "PostListViewBuilder"
    }

    private var preferences = preferenceManager.getComposedPreferencesForAccount(
        accountManager.currentAccount.asAccount,
    )

    var postUiConfig: PostInListUiConfig = preferences.getPostInListUiConfig()
        set(value) {
            field = value

            onPostUiConfigUpdated()
        }

    private var globalFontSizeMultiplier: Float =
        GlobalFontSizeId.getFontSizeMultiplier(preferences.globalFontSize)

    private val lemmyHeaderHelper = lemmyHeaderHelperFactory.create(context)
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
    private var showUpAndDownVotes: Boolean = preferences.postShowUpAndDownVotes
    private var displayInstanceStyle = preferences.displayInstanceStyle
    private var leftHandMode: Boolean = preferences.leftHandMode
    private var showPostUpvotePercentage: Boolean = preferences.showPostUpvotePercentage
    private var useMultilinePostHeaders: Boolean = preferences.useMultilinePostHeaders
    private var indicateCurrentUser: Boolean =
        preferences.indicatePostsAndCommentsCreatedByCurrentUser
    private var showEditedDate: Boolean = preferences.showEditedDate
    private var dimReadPosts: Boolean? = postUiConfig.dimReadPosts
    private var autoPlayVideos: Boolean = preferences.autoPlayVideos
    private var parseMarkdownInPostTitles: Boolean = preferences.parseMarkdownInPostTitles

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
                val account = it as? Account
                currentUser = account

                preferences = preferenceManager.getComposedPreferencesForAccount(account)

                onPostUiConfigUpdated()
            }
        }
    }

    fun onPostUiConfigUpdated() {
        lemmyContentHelper.config = postUiConfig.fullContentConfig
        textSizeMultiplier = postUiConfig.textSizeMultiplier
        contentMaxLines = postUiConfig.contentMaxLines
        dimReadPosts = postUiConfig.dimReadPosts

        globalFontSizeMultiplier =
            GlobalFontSizeId.getFontSizeMultiplier(preferences.globalFontSize)
        lemmyContentHelper.globalFontSizeMultiplier = globalFontSizeMultiplier
        lemmyContentHelper.alwaysShowLinkBelowPost = preferences.alwaysShowLinkButtonBelowPost
        singleTapToViewImage = preferences.postListViewImageOnSingleTap
        showUpAndDownVotes = preferences.postShowUpAndDownVotes
        displayInstanceStyle = preferences.displayInstanceStyle
        leftHandMode = preferences.leftHandMode
        showPostUpvotePercentage = preferences.showPostUpvotePercentage
        useMultilinePostHeaders = preferences.useMultilinePostHeaders
        indicateCurrentUser = preferences.indicatePostsAndCommentsCreatedByCurrentUser
        showEditedDate = preferences.showEditedDate
        autoPlayVideos = preferences.autoPlayVideos
        parseMarkdownInPostTitles = preferences.parseMarkdownInPostTitles
    }

    /**
     * @param updateContent False for a fast update (also removes flickering)
     */
    fun bind(
        holder: ListingItemViewHolder,
        fetchedPost: FetchedPost,
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
        themeColor: Int?,
        onRevealContentClickedFn: () -> Unit,
        onImageClick: (accountId: Long?, PostView, sharedElementView: View?, String) -> Unit,
        onVideoClick: (url: String, videoType: VideoType, videoState: VideoState?) -> Unit,
        onVideoLongClickListener: (url: String) -> Unit,
        onPageClick: (accountId: Long?, PageRef) -> Unit,
        onItemClick: (
            accountId: Long?,
            instance: String,
            id: Int,
            currentCommunity: CommunityRef?,
            post: PostView,
            jumpToComments: Boolean,
            reveal: Boolean,
            videoState: VideoState?,
        ) -> Unit,
        onShowMoreOptions: (accountId: Long?, PostView) -> Unit,
        toggleItem: (fetchedPost: FetchedPost) -> Unit,
        toggleActions: (fetchedPost: FetchedPost) -> Unit,
        onSignInRequired: () -> Unit,
        onInstanceMismatch: (String, String) -> Unit,
        onHighlightComplete: () -> Unit,
        onLinkClick: (
            accountId: Long?,
            url: String,
            text: String?,
            linkContext: LinkContext,
        ) -> Unit,
        onLinkLongClick: (accountId: Long?, url: String, text: String?) -> Unit,
    ) {
        if (!isExpanded && !holder.layoutShowsFullContent) {
            recycle(holder, cancelFetch = false)
        }

        val postView = fetchedPost.postView
        val accountId: Long?
        val accountInstance: String?
        val url = postView.post.url
        val thumbnailUrl = postView.post.thumbnail_url
        val showCommunityIcon = postUiConfig.showCommunityIcon
        val useMultilineHeader = showCommunityIcon

        when (val source = fetchedPost.source) {
            is Source.AccountSource -> {
                accountId = source.accountId
                accountInstance = source.instance
            }
            is Source.StandardSource -> {
                accountId = null
                accountInstance = null
            }
        }

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
                        rb.contentView.addView(downvoteCount)
                    }
                    is ListingItemListBinding -> {
                        ensureActionButtons(rb.contentView, leftHandMode)
                    }
                    is ListingItemListWithCardsBinding -> {
                        ensureActionButtons(rb.constraintLayout, leftHandMode)
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
                            topToBottom = R.id.header_container
                            bottomToBottom = ConstraintLayout.LayoutParams.UNSET
                            bottomToTop = ConstraintLayout.LayoutParams.UNSET
                        }
                    }
                    is ListingItemLargeListBinding -> {
                        ensureActionButtons(rb.contentView, leftHandMode)
                        commentButton!!.updateLayoutParams<ConstraintLayout.LayoutParams> {
                            topToBottom = R.id.image
                            bottomToBottom = ConstraintLayout.LayoutParams.UNSET
                            bottomToTop = ConstraintLayout.LayoutParams.UNSET
                        }
                    }
                    is ListingItemFullBinding -> {
                        ensureActionButtons(rb.contentView, leftHandMode)
                        commentButton!!.updateLayoutParams<ConstraintLayout.LayoutParams> {
                            topToBottom = R.id.bottom_barrier
                            bottomToBottom = ConstraintLayout.LayoutParams.UNSET
                            bottomToTop = ConstraintLayout.LayoutParams.UNSET
                        }
                    }
                    is SearchResultPostItemBinding -> {
                        ensureActionButtons(rb.contentView, leftHandMode)
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
                        is ListingItemListWithCardsBinding -> {
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
                        is ListingItemListWithCardsBinding -> {
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
                        is ListingItemListWithCardsBinding -> {
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
                            rb.headerContainer.updateLayoutParams<ConstraintLayout.LayoutParams> {
                                this.topToTop = ConstraintLayout.LayoutParams.PARENT_ID
                            }
                        }
                    }
                }

                holder.state.preferImagesAtEnd = postUiConfig.preferImagesAtEnd
            }

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

            fun onItemClick() {
                val videoState = if (fullContentContainerView == null) {
                    null
                } else {
                    lemmyContentHelper.getState(fullContentContainerView).videoState?.let {
                        it.copy(
                            currentTime = it.currentTime - ExoPlayerManager.CONVENIENCE_REWIND_TIME_MS,
                        )
                    }
                }
                onItemClick(
                    accountId,
                    accountInstance ?: instance,
                    postView.post.id,
                    postView.community.toCommunityRef(),
                    postView,
                    false,
                    isRevealed,
                    videoState,
                )
            }

            val postType = postView.getType()
            val hasThumbnail = thumbnailUrl != null && ContentUtils.isUrlImage(thumbnailUrl)

            if (rawBinding is ListingItemCardBinding ||
                rawBinding is ListingItemCard2Binding ||
                rawBinding is ListingItemCard3Binding ||
                rawBinding is ListingItemLargeListBinding
            ) {
                if (url == null) {
                    linkText?.visibility = View.GONE
                    linkIcon?.visibility = View.GONE
                    linkOverlay?.visibility = View.GONE
                } else {
                    if ((postType == PostType.Link || postType == PostType.Text) && hasThumbnail) {
                        linkText?.visibility = View.VISIBLE
                        linkIcon?.visibility = View.VISIBLE
                        linkOverlay?.visibility = View.VISIBLE

                        val t = Uri.parse(url).host ?: url
                        linkText?.text = t
                        linkOverlay?.setOnClickListener {
                            onLinkClick(accountId, url, null, LinkContext.Rich)
                        }
                        linkOverlay?.setOnLongClickListener {
                            onLinkLongClick(accountId, url, t)
                            true
                        }
                    } else {
                        linkText?.visibility = View.GONE
                        linkIcon?.visibility = View.GONE
                        linkOverlay?.visibility = View.GONE
                    }
                }
            }

            if (updateContent) {
                lemmyHeaderHelper.populateHeaderSpan(
                    headerContainer = headerContainer,
                    postView = postView,
                    instance = instance,
                    onPageClick = {
                        onPageClick(accountId, it)
                    },
                    onLinkClick = { url, text, linkContext ->
                        onLinkClick(accountId, url, text, linkContext)
                    },
                    onLinkLongClick = { url, text ->
                        onLinkLongClick(accountId, url, text)
                    },
                    displayInstanceStyle = displayInstanceStyle,
                    showUpvotePercentage = showPostUpvotePercentage,
                    useMultilineHeader = useMultilineHeader,
                    wrapHeader = useMultilinePostHeaders && !useMultilineHeader,
                    isCurrentUser = if (indicateCurrentUser) {
                        currentUser?.id == postView.creator.id &&
                            currentUser?.instance == postView.creator.instance
                    } else {
                        false
                    },
                    showEditedDate = showEditedDate,
                )

                if (showCommunityIcon) {
                    headerContainer.iconSize = if (rawBinding is ListingItemCompactBinding) {
                        Utils.convertDpToPixel(24f).toInt()
                    } else {
                        Utils.convertDpToPixel(DEFAULT_ICON_SIZE_DP).toInt()
                    }
                    val iconImageView = headerContainer.getIconImageView()
                    avatarHelper.loadCommunityIcon(iconImageView, postView.community)
                    iconImageView.setOnClickListener {
                        onPageClick(accountId, postView.community.toCommunityRef())
                    }
                }

                fun showDefaultImage() {
                    imageView?.visibility = View.GONE
                    iconImage?.visibility = View.VISIBLE
                    iconImage?.setImageResource(R.drawable.baseline_article_24)
                }

                val finalContentMaxWidth =
                    when (val rb = rawBinding) {
                        is ListingItemCardBinding -> {
                            val lp = rb.cardView.layoutParams as MarginLayoutParams
                            contentMaxWidth - lp.marginStart - lp.marginEnd
                        }
                        is ListingItemCard2Binding -> {
                            val lp = rb.cardView.layoutParams as MarginLayoutParams
                            contentMaxWidth - lp.marginStart - lp.marginEnd
                        }
                        is ListingItemCard3Binding -> {
                            val lp = rb.cardView.layoutParams as MarginLayoutParams
                            contentMaxWidth - lp.marginStart - lp.marginEnd
                        }
                        is ListingItemLargeListBinding -> {
                            val lp = rb.image.layoutParams as MarginLayoutParams
                            contentMaxWidth - lp.marginStart - lp.marginEnd
                        }
                        else -> contentMaxWidth
                    }
                val postImageWidth = (postUiConfig.imageWidthPercent * finalContentMaxWidth).toInt()

                fun loadAndShowImage() {
                    if (postView.post.removed) {
                        // Do not show the image if the post is removed.
                        return
                    }
                    val imageView = imageView ?: return

                    val thumbnailImageUrl = if (hasThumbnail) {
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
                        imageView.visibility = View.GONE
                        return
                    }

                    val isUrlMp4 = ContentUtils.isUrlMp4(imageUrl)

                    if (!ContentUtils.isUrlImage(imageUrl) && !isUrlMp4) {
                        imageView.visibility = View.GONE
                        return
                    }

                    imageView.visibility = View.VISIBLE
                    iconImage?.visibility = View.GONE

                    imageView.dispose()
                    imageView.setImageDrawable(newShimmerDrawable16to9(context))

                    fun loadImage(useBackupUrl: Boolean = false) {
                        val urlToLoad = if (useBackupUrl) {
                            backupImageUrl
                        } else {
                            imageUrl
                        }

                        urlToLoad ?: return

                        loadImage(
                            rootView = itemView,
                            imageView = imageView,
                            imageUrl = urlToLoad,
                            preferFullSizeImage = state.preferFullSizeImages,
                            imageViewWidth = postImageWidth,
                            shouldBlur = !isRevealed && postView.post.nsfw,
                        ) {
                            if (!useBackupUrl && backupImageUrl != null) {
                                loadImage(true)
                            }
                        }
                    }

                    loadImage()

                    fun showImageOrVideo() {
                        if (isUrlMp4) {
                            onVideoClick(imageUrl, VideoType.Mp4, null)
                        } else {
                            onImageClick(accountId, postView, imageView, imageUrl)
                        }
                    }

                    imageView.transitionName = "image_$absoluteAdapterPosition"
                    imageView.setOnClickListener {
                        if (fullContentContainerView != null) {
                            if (singleTapToViewImage) {
                                showImageOrVideo()
                            } else {
                                toggleItem(fetchedPost)
                            }
                        } else {
                            if (url != null && (postType == PostType.Text || postType == PostType.Link)) {
                                onLinkClick(accountId, url, null, LinkContext.Rich)
                            } else {
                                showImageOrVideo()
                            }
                        }
                    }

                    if (fullContentContainerView != null) {
                        imageView.setOnLongClickListener {
                            if (singleTapToViewImage) {
                                toggleItem(fetchedPost)
                            } else {
                                showImageOrVideo()
                            }
                            true
                        }
                    } else {
                        imageView.setOnLongClickListener {
                            onLinkLongClick(accountId, imageUrl, null)
                            true
                        }
                    }
                }

                iconImage?.visibility = View.GONE
                iconImage?.setOnClickListener(null)
                iconImage?.isClickable = false
                imageView?.setOnClickListener(null)
                imageView?.isClickable = false

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
                        autoPlayVideos = autoPlayVideos,
                        onFullImageViewClickListener = { v, url ->
                            onImageClick(accountId, postView, v, url)
                        },
                        onImageClickListener = {
                            onImageClick(accountId, postView, null, it)
                        },
                        onVideoClickListener = onVideoClick,
                        onVideoLongClickListener = onVideoLongClickListener,
                        onItemClickListener = {
                            onItemClick()
                        },
                        onRevealContentClickedFn = {
                            onRevealContentClickedFn()
                        },
                        onLemmyUrlClick = {
                            onPageClick(accountId, it)
                        },
                        onLinkClick = { url, text, linkContext ->
                            onLinkClick(accountId, url, text, linkContext)
                        },
                        onLinkLongClick = { url, text ->
                            onLinkLongClick(accountId, url, text)
                        },
                    )
                }

                Log.d(TAG, "postType: $postType")

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
                                toggleItem(fetchedPost)
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
                            imageView?.visibility = View.GONE

                            // see if this text post has additional content
                            val hasAdditionalContent =
                                !postView.post.body.isNullOrBlank() ||
                                    !url.isNullOrBlank()

                            if (hasAdditionalContent) {
                                showDefaultImage()
                                iconImage?.setOnClickListener {
                                    if (fullContentContainerView != null) {
                                        toggleItem(fetchedPost)
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

                if (imageView != null && imageView.width != postImageWidth) {
                    imageView.updateLayoutParams {
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

            if (parseMarkdownInPostTitles) {
                LemmyTextHelper.bindText(
                    title,
                    postView.post.name,
                    instance,
                    showMediaAsLinks = true,
                    onImageClick = {
                        onImageClick(accountId, postView, null, it)
                    },
                    onVideoClick = {
                        onVideoClick(it, VideoType.Unknown, null)
                    },
                    onPageClick = {
                        onPageClick(accountId, it)
                    },
                    onLinkClick = { url, text, linkContext ->
                        onLinkClick(accountId, url, text, linkContext)
                    },
                    onLinkLongClick = { url, text ->
                        onLinkLongClick(accountId, url, text)
                    },
                )
            } else {
                title.text = postView.post.name
            }

            val renderAsRead = postView.read && !alwaysRenderAsUnread
            val dimReadPosts = dimReadPosts
                ?: rawBinding.communityLayout?.defaultDimReadPosts
                ?: true
            if (dimReadPosts) {
                if (renderAsRead) {
                    if (themeManager.isLightTheme) {
                        contentView.alpha = 0.5f
                    } else {
                        contentView.alpha = 0.6f
                    }
                } else {
                    contentView.alpha = 1f
                }
            } else {
                if (renderAsRead) {
                    if (themeManager.isLightTheme) {
                        title.alpha = 0.41f
                    } else {
                        title.alpha = 0.66f
                    }
                } else {
                    title.alpha = 1f
                }
            }

            if (postView.counts.comments == postView.unread_comments ||
                postView.unread_comments <= 0
            ) {
                commentText?.text = LemmyUtils.abbrevNumber(postView.counts.comments.toLong())
            } else {
                commentText?.text =
                    context.getString(
                        R.string.comments_with_new_comments_format,
                        LemmyUtils.abbrevNumber(postView.counts.comments.toLong()),
                        LemmyUtils.abbrevNumber(postView.unread_comments.toLong()),
                    )
            }

            itemView.setOnClickListener {
                onItemClick()
            }
            commentButton?.setOnClickListener {
                onItemClick(
                    accountId,
                    accountInstance ?: instance,
                    postView.post.id,
                    postView.community.toCommunityRef(),
                    postView,
                    true,
                    isRevealed,
                    null,
                )
            }

            commentButton?.isEnabled = !postView.post.locked

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
                    lifecycleOwner = viewLifecycleOwner,
                    instance = accountInstance ?: instance,
                    postView = postView,
                    upVoteView = upvoteButton,
                    downVoteView = downvoteButton,
                    scoreView = scoreCount,
                    upvoteCount = upvoteCount,
                    downvoteCount = downvoteCount,
                    accountId = accountId,
                    onUpdate = null,
                    onSignInRequired = onSignInRequired,
                    onInstanceMismatch = onInstanceMismatch,
                )
            }

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
                onShowMoreOptions(accountId, postView)
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
                        toggleActions(fetchedPost)
                        true
                    }
                }
                else -> {
                    rb.root.setOnLongClickListener {
                        onShowMoreOptions(accountId, postView)
                        true
                    }
                }
            }

            if (themeColor != null) {
                themeColorBar.visibility = View.VISIBLE
                themeColorBar.setBackgroundResource(R.drawable.post_color_bar)
                ViewCompat.setBackgroundTintList(
                    themeColorBar,
                    ColorStateList.valueOf(themeColor),
                )
//                if (root is MaterialCardView) {
//                    root.setCardBackgroundColor(themeColor)
//                } else {
//                    root.setBackgroundColor(themeColor)
//                }
            } else {
                themeColorBar.visibility = View.GONE
            }
        } // with(holder)
    }

    private fun ListingItemViewHolder.scaleTextSizes() {
        headerContainer.textSize = postUiConfig.headerTextSizeSp.toTextSize() * 0.9f

        title.textSize = postUiConfig.titleTextSizeSp.toTextSize()
        commentText?.textSize = postUiConfig.footerTextSizeSp.toTextSize()
        upvoteCount?.textSize = postUiConfig.footerTextSizeSp.toTextSize()
        downvoteCount?.textSize = postUiConfig.footerTextSizeSp.toTextSize()
    }

    private fun Float.toTextSize(): Float = this * textSizeMultiplier * globalFontSizeMultiplier

    fun recycle(holder: ListingItemViewHolder, cancelFetch: Boolean = true) {
        if (holder.fullContentContainerView != null) {
            lemmyContentHelper.recycleFullContent(holder.fullContentContainerView)
        }
        if (cancelFetch) {
            offlineManager.cancelFetch(holder.itemView)
        }
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
            val commentButton = MaterialButton(
                context,
                null,
                R.attr.summitTextButton,
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
                setIconResource(R.drawable.outline_mode_comment_24)
                compoundDrawablePadding = context.getDimen(R.dimen.padding_half)
                includeFontPadding = false
                setPadding(context.getDimen(R.dimen.padding))
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
            val commentButton = MaterialButton(
                context,
                null,
                R.attr.summitTextButton,
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
                setIconResource(R.drawable.outline_mode_comment_24)
                compoundDrawablePadding = context.getDimen(R.dimen.padding_half)
                includeFontPadding = false
                setPadding(context.getDimen(R.dimen.padding))
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
        imageViewWidth: Int,
        shouldBlur: Boolean,
        errorListener: TaskFailedListener?,
    ) {
        val isUrlMp4 = ContentUtils.isUrlMp4(imageUrl)

        offlineManager.getImageSizeHint(imageUrl, tempSize)

        if (preferFullSizeImage) {
            if (tempSize.width > 0 && tempSize.height > 0) {
                val thumbnailMaxHeight =
                    (imageViewWidth * (tempSize.height.toDouble() / tempSize.width)).toInt()

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

        fun loadImage(urlOrFile: Any, size: Size) {
            Log.d(TAG, "loadImage() size: $size")
            var w: Int? = null
            var h: Int? = null

            if (size.height > 0 && size.width > 0) {
                val heightToWidthRatio = size.height / size.width

                if (heightToWidthRatio > 10) {
                    // shrink the image if needed
                    w = size.width
                    h = size.height
                }
            }

            imageView.load(urlOrFile) {
                if (imageView is ShapeableImageView) {
                    allowHardware(false)
                }

                if (w != null && h != null) {
                    this.size(w, h)
                }
                placeholder(newShimmerDrawable16to9(context))

                if (shouldBlur) {
                    val sampling = (imageViewWidth * 0.04f).coerceAtLeast(10f)

                    if (isUrlMp4) {
                        transformations(
                            BlurTransformation(context, sampling = sampling),
                            VideoWatermarkTransformation(context, sampling = 1f / sampling),
                        )
                    } else {
                        transformations(BlurTransformation(context, sampling = sampling))
                    }
                }

                listener { _, result ->
                    result.drawable.getSize(size)

                    if (size.width > 0 && size.height > 0) {
                        offlineManager.setImageSizeHint(
                            imageUrl,
                            size.width,
                            size.height,
                        )
                    }
                }
            }
        }

        if (isUrlMp4) {
            offlineManager.getMaxImageSizeHint(imageUrl, tempSize)

            loadImage(
                urlOrFile = imageUrl,
                size = tempSize,
            )
        } else {
            offlineManager.fetchImageWithError(
                rootView = rootView,
                url = imageUrl,
                listener = {
                    offlineManager.getMaxImageSizeHint(it, tempSize)

                    loadImage(
                        urlOrFile = it,
                        size = tempSize,
                    )
                },
                errorListener = errorListener,
            )
        }
    }
}

val ViewBinding.communityLayout: CommunityLayout?
    get() =
        when (this) {
            is ListingItemCompactBinding -> CommunityLayout.Compact
            is ListingItemListBinding -> CommunityLayout.List
            is ListingItemListWithCardsBinding -> CommunityLayout.ListWithCards
            is ListingItemCardBinding -> CommunityLayout.Card
            is ListingItemCard2Binding -> CommunityLayout.Card2
            is ListingItemCard3Binding -> CommunityLayout.Card3
            is ListingItemLargeListBinding -> CommunityLayout.LargeList
            is ListingItemFullBinding -> CommunityLayout.Full
            is SearchResultPostItemBinding -> null
            else -> null
        }
