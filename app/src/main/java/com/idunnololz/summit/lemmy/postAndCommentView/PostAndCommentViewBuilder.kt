package com.idunnololz.summit.lemmy.postAndCommentView

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.RectF
import android.graphics.Typeface
import android.text.Spannable
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.view.Gravity
import android.view.View
import android.view.View.LAYOUT_DIRECTION_LTR
import android.view.ViewGroup
import android.view.ViewGroup.MarginLayoutParams
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Space
import android.widget.TextView
import androidx.annotation.IdRes
import androidx.constraintlayout.widget.Barrier
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.text.buildSpannedString
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePaddingRelative
import androidx.core.widget.TextViewCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.RecyclerView.LAYOUT_DIRECTION_RTL
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import arrow.core.Either
import com.google.android.material.divider.MaterialDivider
import com.idunnololz.summit.R
import com.idunnololz.summit.account.Account
import com.idunnololz.summit.account.AccountActionsManager
import com.idunnololz.summit.account.AccountManager
import com.idunnololz.summit.api.dto.CommentView
import com.idunnololz.summit.api.dto.PersonId
import com.idunnololz.summit.api.dto.PostView
import com.idunnololz.summit.api.utils.instance
import com.idunnololz.summit.avatar.AvatarHelper
import com.idunnololz.summit.coroutine.CoroutineScopeFactory
import com.idunnololz.summit.databinding.InboxListItemBinding
import com.idunnololz.summit.databinding.PostCommentCollapsedItemBinding
import com.idunnololz.summit.databinding.PostCommentExpandedCompactItemBinding
import com.idunnololz.summit.databinding.PostCommentExpandedItemBinding
import com.idunnololz.summit.databinding.PostCommentFilteredItemBinding
import com.idunnololz.summit.databinding.PostHeaderItemBinding
import com.idunnololz.summit.databinding.PostMissingCommentItemBinding
import com.idunnololz.summit.databinding.PostMoreCommentsItemBinding
import com.idunnololz.summit.databinding.PostPendingCommentCollapsedItemBinding
import com.idunnololz.summit.databinding.PostPendingCommentExpandedItemBinding
import com.idunnololz.summit.lemmy.LemmyContentHelper
import com.idunnololz.summit.lemmy.LemmyHeaderHelper
import com.idunnololz.summit.lemmy.LemmyTextHelper
import com.idunnololz.summit.lemmy.LemmyUtils
import com.idunnololz.summit.lemmy.PageRef
import com.idunnololz.summit.lemmy.inbox.CommentBackedItem
import com.idunnololz.summit.lemmy.inbox.InboxItem
import com.idunnololz.summit.lemmy.inbox.ReportItem
import com.idunnololz.summit.lemmy.post.QueryMatchHelper.HighlightTextData
import com.idunnololz.summit.lemmy.post.ThreadLinesData
import com.idunnololz.summit.lemmy.postListView.CommentUiConfig
import com.idunnololz.summit.lemmy.postListView.PostAndCommentsUiConfig
import com.idunnololz.summit.lemmy.postListView.PostInListUiConfig
import com.idunnololz.summit.lemmy.postListView.PostUiConfig
import com.idunnololz.summit.lemmy.screenshotMode.ScreenshotModeViewModel
import com.idunnololz.summit.lemmy.toCommunityRef
import com.idunnololz.summit.lemmy.toPersonRef
import com.idunnololz.summit.lemmy.utils.VotableRef
import com.idunnololz.summit.lemmy.utils.bind
import com.idunnololz.summit.lemmy.utils.makeUpAndDownVoteButtons
import com.idunnololz.summit.links.LinkContext
import com.idunnololz.summit.links.LinkResolver
import com.idunnololz.summit.offline.OfflineManager
import com.idunnololz.summit.preferences.CommentHeaderLayoutId
import com.idunnololz.summit.preferences.CommentQuickActionIds
import com.idunnololz.summit.preferences.CommentQuickActionsSettings
import com.idunnololz.summit.preferences.GlobalFontSizeId
import com.idunnololz.summit.preferences.PostQuickActionIds
import com.idunnololz.summit.preferences.PostQuickActionsSettings
import com.idunnololz.summit.preferences.PreferenceManager
import com.idunnololz.summit.preview.VideoType
import com.idunnololz.summit.util.CustomLinkMovementMethod
import com.idunnololz.summit.util.DefaultLinkLongClickListener
import com.idunnololz.summit.util.LinkUtils
import com.idunnololz.summit.util.RecycledState
import com.idunnololz.summit.util.Size
import com.idunnololz.summit.util.Utils
import com.idunnololz.summit.util.ViewRecycler
import com.idunnololz.summit.util.abbrevNumber
import com.idunnololz.summit.util.dateStringToPretty
import com.idunnololz.summit.util.ext.appendLink
import com.idunnololz.summit.util.ext.getColorCompat
import com.idunnololz.summit.util.ext.getColorFromAttribute
import com.idunnololz.summit.util.ext.getDimen
import com.idunnololz.summit.util.ext.getDrawableCompat
import com.idunnololz.summit.util.ext.getResIdFromAttribute
import com.idunnololz.summit.video.ExoPlayerManager
import com.idunnololz.summit.video.VideoState
import com.idunnololz.summit.view.AutoHorizontalScrollView
import com.idunnololz.summit.view.LemmyHeaderView
import com.idunnololz.summit.view.LemmyHeaderView.Companion.DEFAULT_ICON_SIZE_DP
import dagger.hilt.android.qualifiers.ActivityContext
import dagger.hilt.android.scopes.ActivityScoped
import javax.inject.Inject
import kotlinx.coroutines.launch

@ActivityScoped
class PostAndCommentViewBuilder @Inject constructor(
    private val activity: FragmentActivity,
    @ActivityContext private val context: Context,
    private val offlineManager: OfflineManager,
    private val accountActionsManager: AccountActionsManager,
    private val preferenceManager: PreferenceManager,
    private val accountManager: AccountManager,
    private val coroutineScopeFactory: CoroutineScopeFactory,
    private val avatarHelper: AvatarHelper,
) {

    private val coroutineScope = coroutineScopeFactory.create()

    private var preferences = preferenceManager.currentPreferences

    var postInListUiConfig: PostInListUiConfig = preferences.getPostInListUiConfig()
    var uiConfig: PostAndCommentsUiConfig = preferences.getPostAndCommentsUiConfig()
        set(value) {
            field = value

            lemmyContentHelper.config = uiConfig.postUiConfig.fullContentConfig

            postUiConfig = uiConfig.postUiConfig
            commentUiConfig = uiConfig.commentUiConfig
        }

    var hideCommentActions = preferences.hideCommentActions
        private set

    var tapCommentToCollapse = preferences.tapCommentToCollapse
        private set

    private var postUiConfig: PostUiConfig = uiConfig.postUiConfig
    var commentUiConfig: CommentUiConfig = uiConfig.commentUiConfig
    private var globalFontSizeMultiplier: Float =
        GlobalFontSizeId.getFontSizeMultiplier(preferences.globalFontSize)

    val lemmyHeaderHelper = LemmyHeaderHelper(context)
    private val lemmyContentHelper = LemmyContentHelper(
        context,
        offlineManager,
        ExoPlayerManager.get(activity),
    ).also {
        it.globalFontSizeMultiplier = globalFontSizeMultiplier
    }
    val voteUiHandler = accountActionsManager.voteUiHandler

    private var upvoteColor = preferences.upvoteColor
    private var downvoteColor = preferences.downvoteColor
    private val normalTextColor = ContextCompat.getColor(context, R.color.colorText)
    private val unimportantTextColor = ContextCompat.getColor(context, R.color.colorTextFaint)
    private var showUpAndDownVotes: Boolean = preferences.showUpAndDownVotes
    private var displayInstanceStyle = preferences.displayInstanceStyle
    private var leftHandMode = preferences.leftHandMode
    private var showPostUpvotePercentage: Boolean = preferences.showPostUpvotePercentage
    private var showCommentUpvotePercentage: Boolean = preferences.showCommentUpvotePercentage
    private var useMultilinePostHeaders: Boolean = preferences.useMultilinePostHeaders
    private var indicateCurrentUser: Boolean = preferences.indicatePostsAndCommentsCreatedByCurrentUser
    private var showProfileIcons: Boolean = preferences.showProfileIcons
    private var commentHeaderLayout: Int = preferences.commentHeaderLayout
    private var commentQuickActions: CommentQuickActionsSettings = preferences.commentQuickActions
        ?: CommentQuickActionsSettings()
    private var commentsShowInlineMediaAsLinks: Boolean = preferences.commentsShowInlineMediaAsLinks
    private var postQuickActions: PostQuickActionsSettings = preferences.postQuickActions
        ?: PostQuickActionsSettings()
    private var showEditedDate: Boolean = preferences.showEditedDate
    private var autoPlayVideos: Boolean = preferences.autoPlayVideos
    private var useCondensedTypefaceForCommentHeaders: Boolean = preferences.useCondensedTypefaceForCommentHeaders

    private val viewRecycler: ViewRecycler<View> = ViewRecycler()

    private val tempSize = Size()

    private val selectableItemBackgroundBorderless =
        context.getResIdFromAttribute(androidx.appcompat.R.attr.selectableItemBackgroundBorderless)

    private val paddingHalf = context.getDimen(R.dimen.padding_half)
    private val paddingFull = context.getDimen(R.dimen.padding)

    private val paddingIcon = Utils.convertDpToPixel(12f).toInt()

    private var currentUser: Account? = null

    init {
        lemmyContentHelper.config = uiConfig.postUiConfig.fullContentConfig

        coroutineScope.launch {
            accountManager.currentAccount.collect {
                val account = it as? Account
                currentUser = account

                preferences = preferenceManager.getComposedPreferencesForAccount(account)

                onPreferencesChanged()
            }
        }
    }

    fun onPreferencesChanged() {
        uiConfig = preferences.getPostAndCommentsUiConfig()
        hideCommentActions = preferences.hideCommentActions
        tapCommentToCollapse = preferences.tapCommentToCollapse
        globalFontSizeMultiplier = GlobalFontSizeId.getFontSizeMultiplier(preferences.globalFontSize)
        lemmyContentHelper.globalFontSizeMultiplier = globalFontSizeMultiplier
        lemmyContentHelper.alwaysShowLinkBelowPost = preferences.alwaysShowLinkButtonBelowPost
        displayInstanceStyle = preferences.displayInstanceStyle
        showPostUpvotePercentage = preferences.showPostUpvotePercentage
        showCommentUpvotePercentage = preferences.showCommentUpvotePercentage
        useMultilinePostHeaders = preferences.useMultilinePostHeaders
        indicateCurrentUser = preferences.indicatePostsAndCommentsCreatedByCurrentUser
        showProfileIcons = preferences.showProfileIcons
        showEditedDate = preferences.showEditedDate
        autoPlayVideos = preferences.autoPlayVideos
        useCondensedTypefaceForCommentHeaders = preferences.useCondensedTypefaceForCommentHeaders

        upvoteColor = preferences.upvoteColor
        downvoteColor = preferences.downvoteColor
        showUpAndDownVotes = preferences.showUpAndDownVotes
        leftHandMode = preferences.leftHandMode
        commentHeaderLayout = preferences.commentHeaderLayout
        commentQuickActions = preferences.commentQuickActions
            ?: CommentQuickActionsSettings()
        commentsShowInlineMediaAsLinks = preferences.commentsShowInlineMediaAsLinks
        postQuickActions = preferences.postQuickActions
            ?: PostQuickActionsSettings()

        postInListUiConfig = preferences.getPostInListUiConfig()
    }

    class PostViewHolder(
        val root: ViewGroup,
        val commentButton: View,
        val startGuideline: View? = null,
        override val quickActionsTopBarrier: View,
        override val quickActionsStartBarrier: View? = null,
        override val quickActionsEndBarrier: View? = null,
        override var upvoteButton: View? = null,
        override var downvoteButton: View? = null,
        override var quickActionsBar: ViewGroup? = null,
        override var qaScoreCount: TextView? = null,
        override var qaUpvoteCount: TextView? = null,
        override var qaDownvoteCount: TextView? = null,
        override var actionButtons: List<ImageView> = listOf(),
    ) : QuickActionsViewHolder

    fun bindPostView(
        binding: PostHeaderItemBinding,
        container: View,
        postView: PostView,
        accountId: Long?,
        instance: String,
        isRevealed: Boolean,
        contentMaxWidth: Int,
        viewLifecycleOwner: LifecycleOwner,
        videoState: VideoState?,
        updateContent: Boolean,
        highlightTextData: HighlightTextData?,
        screenshotConfig: ScreenshotModeViewModel.ScreenshotConfig? = null,
        onRevealContentClickedFn: () -> Unit,
        onPostActionClick: (PostView, actionId: Int) -> Unit,
        onImageClick: (Either<PostView, CommentView>, View?, String) -> Unit,
        onVideoClick: (url: String, videoType: VideoType, videoState: VideoState?) -> Unit,
        onVideoLongClickListener: (url: String) -> Unit,
        onPageClick: (PageRef) -> Unit,
        onAddCommentClick: (Either<PostView, CommentView>) -> Unit,
        onLinkClick: (url: String, text: String?, linkContext: LinkContext) -> Unit,
        onLinkLongClick: (url: String, text: String?) -> Unit,
        onSignInRequired: () -> Unit,
        onInstanceMismatch: (String, String) -> Unit,
    ) = with(binding) {
        val showCommunityIcon = postInListUiConfig.showCommunityIcon
        val useMultilineHeader = showCommunityIcon

        val viewHolder =
            root.getTag(R.id.view_holder) as? PostViewHolder
                ?: run {
                    val vh = PostViewHolder(
                        root = root,
                        commentButton = commentButton,
                        startGuideline = startGuideline,
                        quickActionsTopBarrier = bottomBarrier,
                        quickActionsStartBarrier = startBarrier,
                        quickActionsEndBarrier = endBarrier,
                    )
                    this.root.setTag(R.id.view_holder, vh)
                    vh
                }

        ensureContent(viewHolder)

        if (screenshotConfig != null) {
            viewHolder.ensureActionButtons(
                root = root,
                leftHandMode = leftHandMode,
                showUpAndDownVotes = showUpAndDownVotes,
                actions = listOf(PostQuickActionIds.Voting),
                isSaved = postView.saved,
                fullWidth = false,
            )
        } else {
            viewHolder.ensureActionButtons(
                root = root,
                leftHandMode = leftHandMode,
                showUpAndDownVotes = showUpAndDownVotes,
                actions = postQuickActions.actions + PostQuickActionIds.More,
                isSaved = postView.saved,
                fullWidth = false,
            )
        }

        scaleTextSizes()
        viewHolder.scaleTextSizes()

        lemmyHeaderHelper.populateHeaderSpan(
            headerContainer = headerContainer,
            postView = postView,
            instance = instance,
            onPageClick = onPageClick,
            onLinkClick = onLinkClick,
            onLinkLongClick = onLinkLongClick,
            displayInstanceStyle = displayInstanceStyle,
            listAuthor = true,
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
            headerContainer.iconSize = Utils.convertDpToPixel(DEFAULT_ICON_SIZE_DP).toInt()
            val iconImageView = headerContainer.getIconImageView()
            avatarHelper.loadIcon(iconImageView, postView.community)
            iconImageView.setOnClickListener {
                onPageClick(postView.community.toCommunityRef())
            }
        }

        if ((title.getTag(R.id.show_community_icon) as? Boolean) != showCommunityIcon) {
            if (showCommunityIcon) {
                title.updateLayoutParams<MarginLayoutParams> {
                    topMargin = context.resources.getDimensionPixelSize(R.dimen.padding)
                }
            } else {
                title.updateLayoutParams<MarginLayoutParams> {
                    topMargin = context.resources.getDimensionPixelSize(R.dimen.padding_half)
                }
            }

            title.setTag(R.id.show_community_icon, showCommunityIcon)
        }

        LemmyTextHelper.bindText(
            title,
            postView.post.name,
            instance,
            highlight = if (highlightTextData?.targetSubtype == 0) {
                highlightTextData
            } else {
                highlightTextData?.copy(matchIndex = null)
            },
            onImageClick = {
                onImageClick(Either.Left(postView), null, it)
            },
            onVideoClick = { url ->
                onVideoClick(url, VideoType.Unknown, null)
            },
            onPageClick = onPageClick,
            onLinkClick = onLinkClick,
            onLinkLongClick = onLinkLongClick,
        )


        if (postView.counts.comments == postView.unread_comments ||
            postView.unread_comments <= 0) {

            commentButton.text = LemmyUtils.abbrevNumber(postView.counts.comments.toLong())
        } else {
            commentButton.text =
                context.getString(
                    R.string.comments_with_new_comments_format,
                    LemmyUtils.abbrevNumber(postView.counts.comments.toLong()),
                    LemmyUtils.abbrevNumber(postView.unread_comments.toLong()),
                )
        }

        commentButton.isEnabled = !postView.post.locked
        commentButton.setOnClickListener {
            onAddCommentClick(Either.Left(postView))
        }

        viewHolder.actionButtons.forEach {
            it.setOnClickListener {
                onPostActionClick(postView, it.id)
            }
            if (it.id == R.id.pa_reply) {
                it.isEnabled = !postView.post.locked
            }
        }

        lemmyContentHelper.setupFullContent(
            reveal = isRevealed,
            tempSize = tempSize,
            videoViewMaxHeight = (
                container.height -
                    Utils.convertDpToPixel(56f) -
                    Utils.convertDpToPixel(16f)
                ).toInt(),
            contentMaxWidth = contentMaxWidth,
            fullImageViewTransitionName = "post_image",
            postView = postView,
            instance = instance,
            rootView = root,
            fullContentContainerView = fullContent,
            lazyUpdate = !updateContent,
            videoState = videoState,
            autoPlayVideos = autoPlayVideos,
            highlight = if (highlightTextData?.targetSubtype == 1) {
                highlightTextData
            } else {
                highlightTextData?.copy(matchIndex = null)
            },
            screenshotConfig = screenshotConfig,
            onFullImageViewClickListener = { view, url ->
                onImageClick(Either.Left(postView), view, url)
            },
            onImageClickListener = { url ->
                onImageClick(Either.Left(postView), null, url)
            },
            onVideoClickListener = onVideoClick,
            onVideoLongClickListener = onVideoLongClickListener,
            onRevealContentClickedFn = onRevealContentClickedFn,
            onItemClickListener = {},
            onLemmyUrlClick = onPageClick,
            onLinkClick = onLinkClick,
            onLinkLongClick = onLinkLongClick,
        )

        val scoreCount: TextView? = viewHolder.qaScoreCount
            ?: viewHolder.qaUpvoteCount
        if (scoreCount != null) {
            val upvoteCount: TextView?
            val downvoteCount: TextView?

            if (viewHolder.qaDownvoteCount != null) {
                upvoteCount = viewHolder.qaUpvoteCount
                downvoteCount = viewHolder.qaDownvoteCount
            } else {
                upvoteCount = null
                downvoteCount = null
            }

            voteUiHandler.bind(
                lifecycleOwner = viewLifecycleOwner,
                instance = instance,
                postView = postView,
                upVoteView = viewHolder.upvoteButton,
                downVoteView = viewHolder.downvoteButton,
                scoreView = scoreCount,
                upvoteCount = upvoteCount,
                downvoteCount = downvoteCount,
                accountId = accountId,
                onUpdate = null,
                onSignInRequired = onSignInRequired,
                onInstanceMismatch = onInstanceMismatch,
            )
        }

        root.tag = postView

        when (screenshotConfig?.postViewType) {
            ScreenshotModeViewModel.PostViewType.Full -> {
                title.visibility = View.VISIBLE
                headerContainer.visibility = View.VISIBLE
                fullContent.visibility = View.VISIBLE
            }
            ScreenshotModeViewModel.PostViewType.ImageOnly -> {
                title.visibility = View.GONE
                headerContainer.visibility = View.GONE
                fullContent.visibility = View.VISIBLE
            }
            ScreenshotModeViewModel.PostViewType.TextOnly -> {
                title.visibility = View.VISIBLE
                headerContainer.visibility = View.VISIBLE
                fullContent.visibility = View.VISIBLE
            }
            ScreenshotModeViewModel.PostViewType.TitleOnly -> {
                fullContent.visibility = View.GONE

                title.visibility = View.VISIBLE
                headerContainer.visibility = View.VISIBLE
            }
            ScreenshotModeViewModel.PostViewType.TitleAndImageOnly -> {
                title.visibility = View.VISIBLE
                fullContent.visibility = View.VISIBLE
                headerContainer.visibility = View.VISIBLE
            }
            ScreenshotModeViewModel.PostViewType.Compact -> {
                title.visibility = View.VISIBLE
                headerContainer.visibility = View.VISIBLE
                fullContent.visibility = View.VISIBLE
            }
            null -> {
            }
        }
    }

    private fun ensureContent(vh: PostViewHolder) = with(vh) {
        val currentLeftHandMode = root.getTag(R.id.left_hand_mode) as? Boolean

        if (leftHandMode == currentLeftHandMode) {
            return@with
        }

        root.setTag(R.id.left_hand_mode, leftHandMode)

        commentButton.updateLayoutParams<ConstraintLayout.LayoutParams> {
            if (leftHandMode) {
                endToEnd = ConstraintLayout.LayoutParams.PARENT_ID
                startToStart = ConstraintLayout.LayoutParams.UNSET
            } else {
                startToStart = startGuideline?.id ?: ConstraintLayout.LayoutParams.PARENT_ID
                endToEnd = ConstraintLayout.LayoutParams.UNSET
            }
        }
        (quickActionsStartBarrier as? Barrier)?.apply {
            referencedIds = if (leftHandMode) {
                intArrayOf(ConstraintLayout.LayoutParams.PARENT_ID)
            } else {
                intArrayOf(commentButton.id)
            }
        }
        (quickActionsEndBarrier as? Barrier)?.apply {
            referencedIds = if (leftHandMode) {
                intArrayOf(commentButton.id)
            } else {
                intArrayOf(ConstraintLayout.LayoutParams.PARENT_ID)
            }
        }
    }

    fun bindCommentViewExpanded(
        h: ViewHolder,
        holder: CommentExpandedViewHolder,
        baseDepth: Int,
        depth: Int,
        maxDepth: Int,
        commentView: CommentView,
        accountId: Long?,
        isDeleting: Boolean,
        isRemoved: Boolean,
        content: String,
        instance: String,
        isPostLocked: Boolean,
        isUpdating: Boolean,
        highlight: Boolean,
        highlightForever: Boolean,
        viewLifecycleOwner: LifecycleOwner,
        isActionsExpanded: Boolean,
        highlightTextData: HighlightTextData?,
        onImageClick: (Either<PostView, CommentView>, View?, String) -> Unit,
        onVideoClick: (url: String, videoType: VideoType, videoState: VideoState?) -> Unit,
        onPageClick: (PageRef) -> Unit,
        collapseSection: (position: Int) -> Unit,
        toggleActionsExpanded: () -> Unit,
        onCommentActionClick: (CommentView, actionId: Int) -> Unit,
        onLinkClick: (url: String, text: String, linkContext: LinkContext) -> Unit,
        onLinkLongClick: (url: String, text: String) -> Unit,
        onSignInRequired: () -> Unit,
        onInstanceMismatch: (String, String) -> Unit,
    ) = with(holder) {
        val isCompactView = this.rawBinding is PostCommentExpandedCompactItemBinding
        val useMultilineHeader =
            showProfileIcons || commentHeaderLayout == CommentHeaderLayoutId.Multiline

        with(holder) {
            if (holder.state.preferUpAndDownVotes != showUpAndDownVotes) {
                if (showUpAndDownVotes) {
                    holder.headerView.textView3.visibility = View.VISIBLE

                    holder.upvoteCount = holder.headerView.textView2
                    holder.downvoteCount = holder.headerView.textView3
                } else {
                    holder.headerView.textView3.visibility = View.GONE

                    holder.upvoteCount = null
                    holder.downvoteCount = null
                }

                if (useMultilineHeader) {
                    // We are displaying the score on it's own line so no padding needed.
                    headerView.textView2.updatePaddingRelative(
                        start = 0,
                    )
                    headerView.updatePaddingRelative(
                        bottom = Utils.convertDpToPixel(8f).toInt(),
                    )
                    collapseSectionButton.updateLayoutParams<ConstraintLayout.LayoutParams> {
                        bottomMargin = Utils.convertDpToPixel(8f).toInt()
                    }
                } else {
                    headerView.textView2.updatePaddingRelative(
                        start = Utils.convertDpToPixel(8f).toInt(),
                    )
                }

                when (val rb = rawBinding) {
                    is PostCommentExpandedItemBinding -> {
                        ensureCommentsActionButtons(
                            vh = this,
                            root = rb.root,
                            isSaved = commentView.saved,
                            fullWidth = false,
                        )
                    }
                    is PostCommentExpandedCompactItemBinding -> {
                        if (isActionsExpanded) {
                            ensureCommentsActionButtons(
                                vh = this,
                                root = root,
                                isSaved = commentView.saved,
                                fullWidth = false,
                            )
                        } else {
                            ensureCommentsActionButtons(
                                vh = this,
                                root = root,
                                isSaved = commentView.saved,
                                removeOnly = true,
                            )
                        }
                    }
                }
            }
        }

        scaleTextSizes()

        val upvoteButton = upvoteButton
        val downvoteButton = downvoteButton

        threadLinesSpacer.updateThreadSpacer(depth, baseDepth, maxDepth)
        lemmyHeaderHelper.populateHeaderSpan(
            headerContainer = headerView,
            commentView = commentView,
            instance = instance,
            score = null,
            onPageClick = onPageClick,
            onLinkClick = onLinkClick,
            onLinkLongClick = onLinkLongClick,
            displayInstanceStyle = displayInstanceStyle,
            showUpvotePercentage = showCommentUpvotePercentage,
            useMultilineHeader = useMultilineHeader,
            useCondensedTypeface = useCondensedTypefaceForCommentHeaders,
            wrapHeader = commentHeaderLayout == CommentHeaderLayoutId.Wrap,
            isCurrentUser = if (indicateCurrentUser) {
                currentUser?.id == commentView.creator.id &&
                    currentUser?.instance == commentView.creator.instance
            } else {
                false
            },
            showEditedDate = showEditedDate,
        )

        if (showProfileIcons) {
            val iconImageView = headerView.getIconImageView()
            avatarHelper.loadAvatar(iconImageView, commentView.creator)
            iconImageView.setOnClickListener {
                onPageClick(commentView.creator.toPersonRef())
            }
        }

        if (commentView.comment.deleted || isDeleting) {
            LemmyTextHelper.bindText(
                textView = text,
                text = context.getString(R.string.deleted_special),
                instance = instance,
                highlight = highlightTextData,
                onImageClick = {
                    onImageClick(Either.Right(commentView), null, it)
                },
                onVideoClick = { url ->
                    onVideoClick(url, VideoType.Unknown, null)
                },
                onPageClick = onPageClick,
                onLinkClick = onLinkClick,
                onLinkLongClick = onLinkLongClick,
            )
        } else if (commentView.comment.removed || isRemoved) {
            LemmyTextHelper.bindText(
                textView = text,
                text = context.getString(R.string.removed_special),
                instance = instance,
                highlight = highlightTextData,
                onImageClick = {
                    onImageClick(Either.Right(commentView), null, it)
                },
                onVideoClick = { url ->
                    onVideoClick(url, VideoType.Unknown, null)
                },
                onPageClick = onPageClick,
                onLinkClick = onLinkClick,
                onLinkLongClick = onLinkLongClick,
            )
        } else {
            LemmyTextHelper.bindText(
                textView = text,
                text = content,
                instance = instance,
                highlight = highlightTextData,
                onImageClick = {
                    onImageClick(Either.Right(commentView), null, it)
                },
                onVideoClick = { url ->
                    onVideoClick(url, VideoType.Unknown, null)
                },
                onPageClick = onPageClick,
                onLinkClick = onLinkClick,
                onLinkLongClick = onLinkLongClick,
                showMediaAsLinks = commentsShowInlineMediaAsLinks,
            )
        }

        if (mediaContainer.childCount > 0) {
            mediaContainer.removeAllViews()
        }
        mediaContainer.visibility = View.GONE

        collapseSectionButton.setOnClickListener {
            collapseSection(h.bindingAdapterPosition)
        }
        topHotspot.setOnClickListener {
            collapseSection(h.bindingAdapterPosition)
        }
        leftHotspot.setOnClickListener {
            collapseSection(h.bindingAdapterPosition)
        }
        headerView.setOnClickListener {
            collapseSection(h.absoluteAdapterPosition)
        }

        actionButtons.forEach {
            it.setOnClickListener {
                onCommentActionClick(commentView, it.id)
            }
            if (it.id == R.id.ca_reply) {
                it.isEnabled = !isPostLocked
            }
        }

        if (commentView.comment.distinguished) {
            overlay.visibility = View.VISIBLE
            overlay.setBackgroundResource(R.drawable.locked_overlay)
        } else {
            overlay.visibility = View.GONE
        }

        val scoreCount = scoreCount
        if (scoreCount != null) {
            val scoreCount2 = qaScoreCount
            val upvoteCount2 = qaUpvoteCount
            val downvoteCount2 = qaDownvoteCount

            voteUiHandler.bind(
                lifecycleOwner = viewLifecycleOwner,
                instance = instance,
                commentView = commentView,
                upVoteView = upvoteButton,
                downVoteView = downvoteButton,
                scoreView = scoreCount,
                upvoteCount = upvoteCount,
                downvoteCount = downvoteCount,
                accountId = accountId,
                onUpdate = { vote, totalScore, upvotes, downvotes ->
                    if (vote < 0) {
                        if (downvoteCount2 == null || upvoteCount2 == null) {
                            scoreCount2?.setTextColor(downvoteColor)
                        } else {
                            downvoteCount2.setTextColor(downvoteColor)
                            upvoteCount2.setTextColor(context.getColorCompat(R.color.colorText))
                        }
                    } else if (vote > 0) {
                        if (downvoteCount2 == null || upvoteCount2 == null) {
                            scoreCount2?.setTextColor(upvoteColor)
                        } else {
                            downvoteCount2.setTextColor(context.getColorCompat(R.color.colorText))
                            upvoteCount2.setTextColor(upvoteColor)
                        }
                    } else {
                        if (downvoteCount2 == null || upvoteCount2 == null) {
                            scoreCount2?.setTextColor(context.getColorCompat(R.color.colorText))
                        } else {
                            downvoteCount2.setTextColor(context.getColorCompat(R.color.colorText))
                            upvoteCount2.setTextColor(context.getColorCompat(R.color.colorText))
                        }
                    }

                    scoreCount2?.text = LemmyUtils.abbrevNumber(totalScore?.toLong())
                    upvoteCount2?.text = LemmyUtils.abbrevNumber(upvotes?.toLong())
                    downvoteCount2?.text = LemmyUtils.abbrevNumber(downvotes?.toLong())

                    scoreCount2?.invalidate()
                    downvoteCount2?.invalidate()
                    upvoteCount2?.invalidate()
                },
                onSignInRequired = onSignInRequired,
                onInstanceMismatch = onInstanceMismatch,
            )
        }

        highlightComment(highlight, highlightForever, highlightBg)

        if (tapCommentToCollapse) {
            holder.root.setOnClickListener {
                collapseSection(h.bindingAdapterPosition)
            }
        }

        if (isCompactView) {
            if (tapCommentToCollapse) {
                holder.root.setOnLongClickListener {
                    toggleActionsExpanded()
                    true
                }
            } else {
                holder.root.setOnClickListener {
                    toggleActionsExpanded()
                }
                holder.root.setOnLongClickListener {
                    toggleActionsExpanded()
                    true
                }
            }

            collapseSectionButton.setOnLongClickListener {
                toggleActionsExpanded()
                true
            }
            topHotspot.setOnLongClickListener {
                toggleActionsExpanded()
                true
            }
            leftHotspot.setOnLongClickListener {
                toggleActionsExpanded()
                true
            }
            headerView.setOnLongClickListener {
                toggleActionsExpanded()
                true
            }
        }

        if (isUpdating) {
            progressBar.visibility = View.VISIBLE
        } else {
            progressBar.visibility = View.GONE
        }

        root.tag = ThreadLinesData(
            depth = depth,
            baseDepth = baseDepth,
            maxDepth = maxDepth,
            indentationPerLevel = commentUiConfig.indentationPerLevelDp,
        )
    }

    fun bindCommentViewCollapsed(
        h: ViewHolder,
        binding: PostCommentCollapsedItemBinding,
        baseDepth: Int,
        depth: Int,
        maxDepth: Int,
        childrenCount: Int,
        highlight: Boolean,
        highlightForever: Boolean,
        isUpdating: Boolean,
        commentView: CommentView,
        instance: String,
        accountId: Long?,
        viewLifecycleOwner: LifecycleOwner,
        expandSection: (position: Int) -> Unit,
        onPageClick: (PageRef) -> Unit,
        onLinkClick: (url: String, text: String, linkContext: LinkContext) -> Unit,
        onLinkLongClick: (url: String, text: String) -> Unit,
    ) = with(binding) {
        scaleTextSizes()

        fun updateText(scoreColor: Int? = null) {
            lemmyHeaderHelper.populateHeaderSpan(
                headerContainer = headerView,
                commentView = commentView,
                instance = instance,
                score = accountActionsManager.getScore(
                    VotableRef.CommentRef(commentView.comment.id),
                ),
                onPageClick = onPageClick,
                detailed = true,
                childrenCount = childrenCount,
                onLinkClick = onLinkClick,
                onLinkLongClick = onLinkLongClick,
                displayInstanceStyle = displayInstanceStyle,
                showUpvotePercentage = showCommentUpvotePercentage,
                useMultilineHeader = false,
                isCurrentUser = if (indicateCurrentUser) {
                    currentUser?.id == commentView.creator.id &&
                        currentUser?.instance == commentView.creator.instance
                } else {
                    false
                },
                showEditedDate = showEditedDate,
                useCondensedTypeface = useCondensedTypefaceForCommentHeaders,
                wrapHeader = true,
                scoreColor = scoreColor,
            )
        }

        threadLinesSpacer.updateThreadSpacer(depth, baseDepth, maxDepth)
        updateText()

        expandSectionButton.setOnClickListener {
            expandSection(h.absoluteAdapterPosition)
        }
        topHotspot.setOnClickListener {
            expandSection(h.absoluteAdapterPosition)
        }
        leftHotspot.setOnClickListener {
            expandSection(h.absoluteAdapterPosition)
        }
        headerView.setOnClickListener {
            expandSection(h.absoluteAdapterPosition)
        }
        if (commentView.comment.distinguished) {
            overlay.visibility = View.VISIBLE
            overlay.setBackgroundResource(R.drawable.locked_overlay)
        } else {
            overlay.visibility = View.GONE
        }

        highlightComment(highlight, highlightForever, highlightBg)

        if (isUpdating) {
            progressBar.visibility = View.VISIBLE
        } else {
            progressBar.visibility = View.GONE
        }

        root.tag = ThreadLinesData(
            depth = depth,
            baseDepth = baseDepth,
            maxDepth = maxDepth,
            indentationPerLevel = commentUiConfig.indentationPerLevelDp,
        )

        voteUiHandler.bind(
            lifecycleOwner = viewLifecycleOwner,
            instance = instance,
            commentView = commentView,
            upVoteView = null,
            downVoteView = null,
            scoreView = dummyTextView,
            upvoteCount = null,
            downvoteCount = null,
            accountId = accountId,
            onUpdate = { vote, totalScore, upvotes, downvotes ->
                updateText(
                    scoreColor = if (vote < 0) {
                        downvoteColor
                    } else if (vote > 0) {
                        upvoteColor
                    } else {
                        null
                    },
                )
            },
            onSignInRequired = {},
            onInstanceMismatch = { _, _ -> },
        )
    }

    fun bindPendingCommentViewExpanded(
        h: ViewHolder,
        binding: PostPendingCommentExpandedItemBinding,
        baseDepth: Int,
        depth: Int,
        maxDepth: Int,
        content: String,
        instance: String,
        author: String?,
        highlight: Boolean,
        highlightForever: Boolean,
        onImageClick: (Either<PostView, CommentView>?, View?, String) -> Unit,
        onVideoClick: (url: String, videoType: VideoType, videoState: VideoState?) -> Unit,
        onPageClick: (PageRef) -> Unit,
        onLinkClick: (url: String, text: String, linkContext: LinkContext) -> Unit,
        onLinkLongClick: (url: String, text: String) -> Unit,
        collapseSection: (position: Int) -> Unit,
    ) = with(binding) {
        scaleTextSizes()

        val context = binding.root.context

        threadLinesSpacer.updateThreadSpacer(depth, baseDepth, maxDepth)
        headerContainer.setTextFirstPart(author ?: context.getString(R.string.unknown_special))
        headerContainer.setTextSecondPart("")

        LemmyTextHelper.bindText(
            textView = text,
            text = content,
            instance = instance,
            onImageClick = {
                onImageClick(null, null, it)
            },
            onVideoClick = { url ->
                onVideoClick(url, VideoType.Unknown, null)
            },
            onPageClick = onPageClick,
            onLinkClick = onLinkClick,
            onLinkLongClick = onLinkLongClick,
        )

        collapseSectionButton.setOnClickListener {
            collapseSection(h.bindingAdapterPosition)
        }
        topHotspot.setOnClickListener {
            collapseSection(h.bindingAdapterPosition)
        }

        highlightComment(highlight, highlightForever, highlightBg)

        root.tag = ThreadLinesData(
            depth = depth,
            baseDepth = baseDepth,
            maxDepth = maxDepth,
            indentationPerLevel = commentUiConfig.indentationPerLevelDp,
        )
    }

    fun bindPendingCommentViewCollapsed(
        holder: ViewHolder,
        binding: PostPendingCommentCollapsedItemBinding,
        baseDepth: Int,
        depth: Int,
        maxDepth: Int,
        author: String?,
        highlight: Boolean,
        highlightForever: Boolean,
        expandSection: (position: Int) -> Unit,
    ) = with(binding) {
        scaleTextSizes()

        val context = holder.itemView.context
        threadLinesSpacer.updateThreadSpacer(depth, baseDepth, maxDepth)
        headerContainer.setTextFirstPart(author ?: context.getString(R.string.unknown_special))
        headerContainer.setTextSecondPart("")

        expandSectionButton.setOnClickListener {
            expandSection(holder.absoluteAdapterPosition)
        }
        topHotspot.setOnClickListener {
            expandSection(holder.absoluteAdapterPosition)
        }
        overlay.visibility = View.GONE

        highlightComment(highlight, highlightForever, highlightBg)

        root.tag = ThreadLinesData(
            depth = depth,
            baseDepth = baseDepth,
            maxDepth = maxDepth,
            indentationPerLevel = commentUiConfig.indentationPerLevelDp,
        )
    }

    fun bindMessage(
        viewLifecycleOwner: LifecycleOwner,
        b: InboxListItemBinding,
        instance: String,
        accountId: PersonId?,
        item: InboxItem,
        onImageClick: (String) -> Unit,
        onVideoClick: (url: String, videoType: VideoType, videoState: VideoState?) -> Unit,
        onPageClick: (PageRef) -> Unit,
        onMarkAsRead: (InboxItem, Boolean) -> Unit,
        onMessageClick: (InboxItem) -> Unit,
        onAddCommentClick: (InboxItem) -> Unit,
        onOverflowMenuClick: (InboxItem) -> Unit,
        onLinkClick: (url: String, text: String, linkContext: LinkContext) -> Unit,
        onLinkLongClick: (url: String, text: String) -> Unit,
        onSignInRequired: () -> Unit,
        onInstanceMismatch: (String, String) -> Unit,
    ) = with(b) {
        val leftHandMode = leftHandMode
        if ((b.root.getTag(R.id.left_hand_mode) as? Boolean) != leftHandMode) {
            if (leftHandMode) {
                b.actionsContainer.updateLayoutParams<ConstraintLayout.LayoutParams> {
                    endToEnd = ConstraintLayout.LayoutParams.UNSET
                    startToStart = ConstraintLayout.LayoutParams.PARENT_ID
                }
            } else {
                b.actionsContainer.updateLayoutParams<ConstraintLayout.LayoutParams> {
                    endToEnd = ConstraintLayout.LayoutParams.PARENT_ID
                    startToStart = ConstraintLayout.LayoutParams.UNSET
                }
            }
            b.root.setTag(R.id.left_hand_mode, leftHandMode)
        }

        val isMessageItem = item is InboxItem.MessageInboxItem

        b.author.text = buildSpannedString {
            run {
                val s = length
                appendLink(
                    if (isMessageItem) {
                        context.getString(R.string.message_from_format, item.authorName)
                    } else {
                        item.authorName
                    },
                    LinkUtils.getLinkForPerson(item.authorInstance, item.authorName),
                    underline = false,
                )
                val e = length
                setSpan(
                    ForegroundColorSpan(normalTextColor),
                    s,
                    e,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE,
                )
                setSpan(
                    StyleSpan(Typeface.BOLD),
                    s,
                    e,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE,
                )
            }
        }
        b.author.movementMethod = CustomLinkMovementMethod().apply {
            onLinkLongClickListener = DefaultLinkLongClickListener(context, onLinkLongClick)
            onLinkClickListener = object : CustomLinkMovementMethod.OnLinkClickListener {
                override fun onClick(
                    textView: TextView,
                    url: String,
                    text: String,
                    rect: RectF,
                ): Boolean {
                    val pageRef = LinkResolver.parseUrl(url, instance)

                    return if (pageRef != null) {
                        onPageClick(pageRef)
                        true
                    } else {
                        false
                    }
                }
            }
        }

        when (item) {
            is CommentBackedItem -> {
                val scoreDrawable = context.getDrawableCompat(R.drawable.baseline_arrow_upward_24)
                scoreDrawable?.setBounds(
                    0,
                    0,
                    Utils.convertDpToPixel(16f).toInt(),
                    Utils.convertDpToPixel(16f).toInt(),
                )
                b.score.setCompoundDrawablesRelative(
                    scoreDrawable,
                    null,
                    null,
                    null,
                )
                b.score.text = LemmyUtils.abbrevNumber(item.score.toLong())
                b.score.visibility = View.VISIBLE

                voteUiHandler.bind(
                    lifecycleOwner = viewLifecycleOwner,
                    instance = instance,
                    inboxItem = item,
                    upVoteView = upvoteButton,
                    downVoteView = downvoteButton,
                    scoreView = b.score,
                    upvoteCount = null,
                    downvoteCount = null,
                    accountId = accountId,
                    onUpdate = null,
                    onSignInRequired = onSignInRequired,
                    onInstanceMismatch = onInstanceMismatch,
                )
                upvoteButton.isEnabled = true
                downvoteButton.isEnabled = true
                b.reply.isEnabled = true
            }

            is ReportItem -> {
                voteUiHandler.unbindVoteUi(b.score)
                b.score.visibility = View.GONE

                upvoteButton.isEnabled = false
                downvoteButton.isEnabled = false
                b.reply.isEnabled = false
            }

            else -> {
                voteUiHandler.unbindVoteUi(b.score)
                b.score.visibility = View.GONE

                upvoteButton.isEnabled = false
                downvoteButton.isEnabled = false
                b.reply.isEnabled = true
            }
        }

        val drawable = when (item) {
            is InboxItem.MentionInboxItem -> {
                TextViewCompat.setCompoundDrawableTintList(
                    b.author,
                    ColorStateList.valueOf(context.getColorCompat(R.color.colorTextTitle)),
                )
                context.getDrawableCompat(R.drawable.baseline_at_24)
            }
            is InboxItem.MessageInboxItem -> {
                TextViewCompat.setCompoundDrawableTintList(
                    b.author,
                    ColorStateList.valueOf(context.getColorCompat(R.color.colorTextTitle)),
                )
                context.getDrawableCompat(R.drawable.baseline_email_24)
            }
            is InboxItem.ReplyInboxItem -> {
                TextViewCompat.setCompoundDrawableTintList(
                    b.author,
                    ColorStateList.valueOf(context.getColorCompat(R.color.colorTextTitle)),
                )
                context.getDrawableCompat(R.drawable.baseline_reply_24)
            }
            is InboxItem.ReportMessageInboxItem,
            is InboxItem.ReportPostInboxItem,
            is InboxItem.ReportCommentInboxItem,
            -> {
                TextViewCompat.setCompoundDrawableTintList(
                    b.author,
                    ColorStateList.valueOf(context.getColorCompat(R.color.style_red)),
                )
                context.getDrawableCompat(R.drawable.baseline_outlined_flag_24)
            }
        }
        drawable?.setBounds(
            0,
            0,
            Utils.convertDpToPixel(16f).toInt(),
            Utils.convertDpToPixel(16f).toInt(),
        )
        val faintTextColor = context.getColorCompat(R.color.colorTextFaint)

        b.author.setCompoundDrawablesRelative(
            drawable,
            null,
            null,
            null,
        )
        b.date.text = dateStringToPretty(context, item.lastUpdate)

        val title = if (item is InboxItem.MessageInboxItem) {
            context.getString(R.string.message_to_format, item.targetUserName)
        } else {
            item.title
        }

        LemmyTextHelper.bindText(
            textView = b.title,
            text = title,
            instance = instance,
            onImageClick = {
                onImageClick(it)
            },
            onVideoClick = { url ->
                onVideoClick(url, VideoType.Unknown, null)
            },
            onPageClick = onPageClick,
            onLinkClick = onLinkClick,
            onLinkLongClick = onLinkLongClick,
            showMediaAsLinks = true,
        )

        if (item.isDeleted) {
            b.content.text = buildSpannedString {
                append(context.getString(R.string.deleted_special))
                setSpan(StyleSpan(Typeface.ITALIC), 0, length, 0)
            }
        } else if (item.isRemoved) {
            b.content.text = buildSpannedString {
                append(context.getString(R.string.removed_special))
                setSpan(StyleSpan(Typeface.ITALIC), 0, length, 0)
            }
        } else {
            LemmyTextHelper.bindText(
                textView = b.content,
                text = item.content,
                instance = instance,
                showMediaAsLinks = true,
                onImageClick = {
                    onImageClick(it)
                },
                onVideoClick = { url ->
                    onVideoClick(url, VideoType.Unknown, null)
                },
                onPageClick = onPageClick,
                onLinkClick = onLinkClick,
                onLinkLongClick = onLinkLongClick,
            )
        }

        if (item.isRead) {
            b.author.setTextColor(faintTextColor)
            b.title.setTextColor(faintTextColor)
            b.content.setTextColor(faintTextColor)

            b.markAsRead.imageTintList =
                ColorStateList.valueOf(context.getColorCompat(R.color.style_green))
            b.markAsRead.setOnClickListener {
                onMarkAsRead(item, false)
            }
            b.author.alpha = .5f
        } else {
            b.author.setTextColor(context.getColorCompat(R.color.colorText))
            b.author.alpha = 1f
            b.title.setTextColor(context.getColorCompat(R.color.colorTextTitle))
            b.content.setTextColor(context.getColorCompat(R.color.colorText))
            b.markAsRead.imageTintList =
                ColorStateList.valueOf(
                    context.getColorFromAttribute(androidx.appcompat.R.attr.colorControlNormal),
                )
            b.markAsRead.setOnClickListener {
                onMarkAsRead(item, true)
            }
        }

        b.reply.setOnClickListener {
            onAddCommentClick(item)
        }
        b.moreButton.setOnClickListener {
            onOverflowMenuClick(item)
        }

        root.setOnClickListener {
            onMessageClick(item)
        }

        if (item is InboxItem.MessageInboxItem &&
            item.authorId == accountId
        ) {
            b.root.setTag(R.id.swipe_enabled, false)
        } else {
            b.root.setTag(R.id.swipe_enabled, true)
            b.markAsRead.isEnabled = true
        }

        if (item.isRead) {
            b.root.setTag(R.id.swipe_enabled, false)
        }
    }

    private fun makeCommentActionButton(@IdRes idRes: Int) = ImageView(
        context,
    ).apply {
        id = idRes
        layoutParams = LinearLayout.LayoutParams(
            ConstraintLayout.LayoutParams.WRAP_CONTENT,
            ConstraintLayout.LayoutParams.WRAP_CONTENT,
        )
        setPadding(
            paddingIcon,
            paddingHalf,
            paddingIcon,
            paddingHalf,
        )
        setBackgroundResource(selectableItemBackgroundBorderless)
        imageTintList = ColorStateList.valueOf(normalTextColor)
    }

    fun ensureCommentsActionButtons(
        vh: QuickActionsViewHolder,
        root: ViewGroup,
        isSaved: Boolean,
        removeOnly: Boolean = false,
        fullWidth: Boolean = true,
    ) {
        vh.ensureActionButtons(
            root = root,
            leftHandMode = leftHandMode,
            showUpAndDownVotes = showUpAndDownVotes,
            actions = commentQuickActions.actions + CommentQuickActionIds.More,
            removeOnly = removeOnly,
            isSaved = isSaved,
            fullWidth = fullWidth,
        )
    }

    private fun QuickActionsViewHolder.ensureActionButtons(
        root: ViewGroup,
        leftHandMode: Boolean,
        showUpAndDownVotes: Boolean,
        actions: List<Int>,
        isSaved: Boolean,
        removeOnly: Boolean = false,
        fullWidth: Boolean = true,
    ) {
        root.removeView(quickActionsBar)

        if (removeOnly) return

        val quickActionsBarContainer = AutoHorizontalScrollView(context)
            .also {
                quickActionsBar = it
            }
        val quickActionsBar = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(
                0,
                Utils.convertDpToPixel(8f).toInt(),
                0,
                Utils.convertDpToPixel(8f).toInt(),
            )
        }
        if (!leftHandMode) {
            quickActionsBarContainer.layoutDirection = LAYOUT_DIRECTION_RTL
            quickActionsBar.layoutDirection = LAYOUT_DIRECTION_LTR
        }

        val indices: IntProgression
        if (leftHandMode) {
            quickActionsBar.gravity = Gravity.START or Gravity.CENTER_VERTICAL
            indices = actions.indices
        } else {
            quickActionsBar.gravity = Gravity.END or Gravity.CENTER_VERTICAL
            indices = actions.indices.reversed()
        }

        val actionButtons = mutableListOf<ImageView>()

        for (actionIndex in indices) {
            when (val action = actions[actionIndex]) {
                PostQuickActionIds.Voting,
                CommentQuickActionIds.Voting,
                -> {
                    if (showUpAndDownVotes) {
                        val buttons = makeUpAndDownVoteButtons(context) {
                            LinearLayout.LayoutParams(
                                ConstraintLayout.LayoutParams.WRAP_CONTENT,
                                Utils.convertDpToPixel(48f).toInt(),
                            )
                        }

                        if (leftHandMode) {
                            buttons.upvoteButton.updateLayoutParams<LinearLayout.LayoutParams> {
                                marginStart = paddingFull
                            }
                            buttons.downvoteButton.updateLayoutParams<LinearLayout.LayoutParams> {
                                marginEnd = paddingHalf
                            }
                        } else {
                            buttons.downvoteButton.updateLayoutParams<LinearLayout.LayoutParams> {
                                marginEnd = paddingFull
                            }
                            buttons.upvoteButton.updateLayoutParams<LinearLayout.LayoutParams> {
                                marginStart = paddingHalf
                            }
                        }
                        quickActionsBar.addView(buttons.upvoteButton)
                        quickActionsBar.addView(buttons.downvoteButton)

                        qaScoreCount = buttons.upvoteButton
                        upvoteButton = buttons.upvoteButton
                        qaUpvoteCount = buttons.upvoteButton
                        downvoteButton = buttons.downvoteButton
                        qaDownvoteCount = buttons.downvoteButton
                    } else {
                        val button1 = ImageView(
                            context,
                        ).apply {
                            id = View.generateViewId()
                            layoutParams = LinearLayout.LayoutParams(
                                ConstraintLayout.LayoutParams.WRAP_CONTENT,
                                ConstraintLayout.LayoutParams.WRAP_CONTENT,
                            ).apply {
                                if (leftHandMode) {
                                    marginStart = context.getDimen(R.dimen.padding_quarter)
                                } else {
                                    marginEnd = context.getDimen(R.dimen.padding_quarter)
                                }
                            }
                            setPaddingRelative(
                                paddingHalf,
                                paddingHalf,
                                paddingHalf,
                                paddingHalf,
                            )
                            setBackgroundResource(selectableItemBackgroundBorderless)
                            imageTintList = ColorStateList.valueOf(normalTextColor)
                        }

                        val scoreCount = TextView(
                            context,
                        ).apply {
                            id = View.generateViewId()
                            layoutParams = LinearLayout.LayoutParams(
                                ConstraintLayout.LayoutParams.WRAP_CONTENT,
                                ConstraintLayout.LayoutParams.WRAP_CONTENT,
                            ).apply {
                                gravity = Gravity.CENTER_VERTICAL
                            }
                        }.also {
                            qaScoreCount = it
                        }

                        val button2 = ImageView(
                            context,
                        ).apply {
                            id = View.generateViewId()
                            layoutParams = LinearLayout.LayoutParams(
                                ConstraintLayout.LayoutParams.WRAP_CONTENT,
                                ConstraintLayout.LayoutParams.WRAP_CONTENT,
                            )
                            setPaddingRelative(
                                paddingIcon,
                                paddingHalf,
                                paddingHalf,
                                paddingHalf,
                            )
                            setBackgroundResource(selectableItemBackgroundBorderless)
                            imageTintList = ColorStateList.valueOf(normalTextColor)
                        }

                        val finalUpvoteButton: ImageView = button2
                        val finalDownvoteButton: ImageView = button1

                        finalUpvoteButton.apply {
                            setImageResource(R.drawable.baseline_arrow_upward_24)
                        }
                        finalDownvoteButton.apply {
                            setImageResource(R.drawable.baseline_arrow_downward_24)
                        }

                        upvoteButton = finalUpvoteButton
                        downvoteButton = finalDownvoteButton

                        quickActionsBar.addView(button2)
                        quickActionsBar.addView(scoreCount)
                        quickActionsBar.addView(button1)
                    }
                }
                PostQuickActionIds.Reply -> {
                    makeCommentActionButton(R.id.pa_reply).apply {
                        setImageResource(R.drawable.baseline_add_comment_24)
                        quickActionsBar.addView(this)
                        actionButtons.add(this)
                    }
                }
                CommentQuickActionIds.Reply -> {
                    makeCommentActionButton(R.id.ca_reply).apply {
                        setImageResource(R.drawable.baseline_add_comment_24)
                        quickActionsBar.addView(this)
                        actionButtons.add(this)
                    }
                }
                PostQuickActionIds.Save -> {
                    makeCommentActionButton(R.id.pa_save_toggle).apply {
                        if (isSaved) {
                            setImageResource(R.drawable.baseline_bookmark_24)
                        } else {
                            setImageResource(R.drawable.outline_bookmark_border_24)
                        }
                        quickActionsBar.addView(this)
                        actionButtons.add(this)
                    }
                }
                CommentQuickActionIds.Save -> {
                    makeCommentActionButton(R.id.ca_save_toggle).apply {
                        if (isSaved) {
                            setImageResource(R.drawable.baseline_bookmark_24)
                        } else {
                            setImageResource(R.drawable.outline_bookmark_border_24)
                        }
                        quickActionsBar.addView(this)
                        actionButtons.add(this)
                    }
                }
                CommentQuickActionIds.Share -> {
                    makeCommentActionButton(R.id.ca_share).apply {
                        setImageResource(R.drawable.baseline_share_24)
                        quickActionsBar.addView(this)
                        actionButtons.add(this)
                    }
                }
                CommentQuickActionIds.TakeScreenshot -> {
                    makeCommentActionButton(R.id.ca_screenshot).apply {
                        setImageResource(R.drawable.baseline_screenshot_24)
                        quickActionsBar.addView(this)
                        actionButtons.add(this)
                    }
                }
                CommentQuickActionIds.ShareSource -> {
                    makeCommentActionButton(R.id.ca_share_fediverse_link).apply {
                        setImageResource(R.drawable.ic_fediverse_24)
                        quickActionsBar.addView(this)
                        actionButtons.add(this)
                    }
                }
                CommentQuickActionIds.OpenComment -> {
                    makeCommentActionButton(R.id.ca_open_comment_in_new_screen).apply {
                        setImageResource(R.drawable.baseline_open_in_new_24)
                        quickActionsBar.addView(this)
                        actionButtons.add(this)
                    }
                }
                CommentQuickActionIds.ViewSource -> {
                    makeCommentActionButton(R.id.ca_view_source).apply {
                        setImageResource(R.drawable.baseline_code_24)
                        quickActionsBar.addView(this)
                        actionButtons.add(this)
                    }
                }
                CommentQuickActionIds.DetailedView -> {
                    makeCommentActionButton(R.id.ca_detailed_view).apply {
                        setImageResource(R.drawable.baseline_open_in_full_24)
                        quickActionsBar.addView(this)
                        actionButtons.add(this)
                    }
                }
                PostQuickActionIds.More -> {
                    makeCommentActionButton(R.id.pa_more).apply {
                        setImageResource(R.drawable.baseline_more_horiz_24)
                        quickActionsBar.addView(this)
                        actionButtons.add(this)
                    }
                }
                CommentQuickActionIds.More -> {
                    makeCommentActionButton(R.id.ca_more).apply {
                        setImageResource(R.drawable.baseline_more_horiz_24)
                        quickActionsBar.addView(this)
                        actionButtons.add(this)
                    }
                }
                PostQuickActionIds.TakeScreenshot -> {
                    makeCommentActionButton(R.id.pa_screenshot).apply {
                        setImageResource(R.drawable.baseline_screenshot_24)
                        quickActionsBar.addView(this)
                        actionButtons.add(this)
                    }
                }
                PostQuickActionIds.CrossPost -> {
                    makeCommentActionButton(R.id.pa_cross_post).apply {
                        setImageResource(R.drawable.baseline_content_copy_24)
                        quickActionsBar.addView(this)
                        actionButtons.add(this)
                    }
                }
                PostQuickActionIds.ShareSourceLink -> {
                    makeCommentActionButton(R.id.pa_share_fediverse_link).apply {
                        setImageResource(R.drawable.ic_fediverse_24)
                        quickActionsBar.addView(this)
                        actionButtons.add(this)
                    }
                }
                PostQuickActionIds.CommunityInfo -> {
                    makeCommentActionButton(R.id.pa_community_info).apply {
                        setImageResource(R.drawable.ic_community_24)
                        quickActionsBar.addView(this)
                        actionButtons.add(this)
                    }
                }
                PostQuickActionIds.ViewSource -> {
                    makeCommentActionButton(R.id.pa_view_source).apply {
                        setImageResource(R.drawable.baseline_code_24)
                        quickActionsBar.addView(this)
                        actionButtons.add(this)
                    }
                }
                PostQuickActionIds.DetailedView -> {
                    makeCommentActionButton(R.id.pa_detailed_view).apply {
                        setImageResource(R.drawable.baseline_open_in_full_24)
                        quickActionsBar.addView(this)
                        actionButtons.add(this)
                    }
                }
            }

            this.actionButtons = actionButtons

            if (actionIndex != indices.last) {
                val actionsDivider1 = MaterialDivider(
                    context,
                ).apply {
                    id = View.generateViewId()
                    layoutParams = LinearLayout.LayoutParams(
                        Utils.convertDpToPixel(1f).toInt(),
                        LinearLayout.LayoutParams.MATCH_PARENT,
                    ).apply {
                        topMargin = paddingHalf
                        bottomMargin = paddingHalf
                    }
                }
                quickActionsBar.addView(actionsDivider1)
            }
        }

        quickActionsBarContainer.addView(quickActionsBar)
        quickActionsBarContainer.isHorizontalScrollBarEnabled = false
        quickActionsBarContainer.layoutParams = ConstraintLayout.LayoutParams(
            0,
            ConstraintLayout.LayoutParams.WRAP_CONTENT,
        ).apply {
            val startBarrier = quickActionsStartBarrier
            val endBarrier = quickActionsEndBarrier

            topToBottom = quickActionsTopBarrier.id

            if (leftHandMode) {
                if (startBarrier != null) {
                    startToEnd = startBarrier.id
                } else {
                    startToStart = ConstraintLayout.LayoutParams.PARENT_ID
                }

                if (endBarrier != null) {
                    endToStart = endBarrier.id
                } else {
                    endToEnd = ConstraintLayout.LayoutParams.PARENT_ID
                }
            } else {
                if (fullWidth) {
                    endToEnd = ConstraintLayout.LayoutParams.PARENT_ID
                } else {
                    if (startBarrier != null) {
                        endToStart = startBarrier.id
                    } else {
                        endToEnd = ConstraintLayout.LayoutParams.PARENT_ID
                    }

                    marginEnd = Utils.convertDpToPixel(16f).toInt()
                }

                if (endBarrier != null) {
                    startToEnd = endBarrier.id
                } else {
                    startToStart = ConstraintLayout.LayoutParams.PARENT_ID
                }
            }
        }
        root.addView(quickActionsBarContainer, root.childCount - 2)
    }

    fun bindCommentFilteredItem(
        b: PostCommentFilteredItemBinding,
        depth: Int,
        baseDepth: Int,
        maxDepth: Int,
        onTap: () -> Unit,
    ) = with(b) {
        threadLinesSpacer.updateThreadSpacer(depth, baseDepth, maxDepth)

        b.root.setOnClickListener {
            onTap()
        }

        root.tag = ThreadLinesData(
            depth = depth,
            baseDepth = baseDepth,
            maxDepth = maxDepth,
            indentationPerLevel = commentUiConfig.indentationPerLevelDp,
        )
    }

    fun bindMoreCommentsItem(
        b: PostMoreCommentsItemBinding,
        depth: Int,
        baseDepth: Int,
        maxDepth: Int,
    ) = with(b) {
        threadLinesSpacer.updateThreadSpacer(depth, baseDepth, maxDepth)

        b.root.tag = ThreadLinesData(
            depth = depth,
            baseDepth = baseDepth,
            maxDepth = maxDepth,
            indentationPerLevel = commentUiConfig.indentationPerLevelDp,
        )
    }

    fun bindMissingCommentItem(
        b: PostMissingCommentItemBinding,
        depth: Int,
        baseDepth: Int,
        maxDepth: Int,
        isExpanded: Boolean,
        onToggleClick: () -> Unit,
    ) {
        b.text.disableTouch = true
        b.text.text = buildSpannedString {
            append(context.getString(R.string.missing_comment_special))
            setSpan(StyleSpan(Typeface.ITALIC), 0, length, 0)
        }
        b.threadLinesSpacer.updateThreadSpacer(depth, baseDepth, maxDepth)
        b.root.tag = ThreadLinesData(
            depth = depth,
            baseDepth = baseDepth,
            maxDepth = maxDepth,
            indentationPerLevel = commentUiConfig.indentationPerLevelDp,
        )

        if (isExpanded) {
            b.state.setImageResource(R.drawable.baseline_expand_less_18)
        } else {
            b.state.setImageResource(R.drawable.baseline_expand_more_18)
        }

        b.root.setOnClickListener {
            onToggleClick()
        }
        b.state.setOnClickListener {
            onToggleClick()
        }
    }

    fun recycle(b: PostHeaderItemBinding): RecycledState {
        val recycledState = lemmyContentHelper.recycleFullContent(b.fullContent)
        offlineManager.cancelFetch(b.root)
        (b.root.getTag(R.id.view_holder) as? PostViewHolder)?.qaUpvoteCount?.let {
            voteUiHandler.unbindVoteUi(it)
        }
        return recycledState
    }

    fun recycle(b: CommentExpandedViewHolder) {
        b.scoreCount?.let {
            voteUiHandler.unbindVoteUi(it)
        }
        b.actionsContainer?.let {
            if (it.childCount > 0) {
                val actionsView = it.getChildAt(0)
                it.removeAllViews()
                viewRecycler.addRecycledView(actionsView, R.layout.comment_actions_view)
            }
        }
    }

    fun populateHeaderSpan(
        headerContainer: LemmyHeaderView,
        commentView: CommentView,
        instance: String,
        onPageClick: (PageRef) -> Unit,
        onLinkClick: (url: String, text: String, linkContext: LinkContext) -> Unit,
        onLinkLongClick: (url: String, text: String) -> Unit,
    ) {
        lemmyHeaderHelper.populateHeaderSpan(
            headerContainer = headerContainer,
            commentView = commentView,
            instance = instance,
            score = null,
            onPageClick = onPageClick,
            onLinkClick = onLinkClick,
            onLinkLongClick = onLinkLongClick,
            displayInstanceStyle = displayInstanceStyle,
            showUpvotePercentage = showCommentUpvotePercentage,
            useMultilineHeader = false,
            useCondensedTypeface = useCondensedTypefaceForCommentHeaders,
            isCurrentUser = if (indicateCurrentUser) {
                currentUser?.id == commentView.creator.id &&
                    currentUser?.instance == commentView.creator.instance
            } else {
                false
            },
            showEditedDate = showEditedDate,
        )
    }

    private fun highlightComment(
        isCommentHighlighted: Boolean,
        highlightForever: Boolean,
        bg: View,
    ) {
        if (highlightForever) {
            bg.visibility = View.VISIBLE
            bg.clearAnimation()
        } else if (isCommentHighlighted) {
            bg.visibility = View.VISIBLE

            val animation = AlphaAnimation(0f, 0.9f)
            animation.repeatCount = 5
            animation.repeatMode = Animation.REVERSE
            animation.duration = 300
            animation.fillAfter = true

            bg.startAnimation(animation)
        } else {
            bg.visibility = View.GONE
            bg.clearAnimation()
        }
    }

    private fun PostHeaderItemBinding.scaleTextSizes() {
        headerContainer.textSize = postUiConfig.headerTextSizeSp.toPostTextSize()
        title.textSize = postUiConfig.titleTextSizeSp.toPostTextSize()
        commentButton.textSize = postUiConfig.footerTextSizeSp.toPostTextSize()
    }

    private fun PostViewHolder.scaleTextSizes() {
        qaUpvoteCount?.textSize = postUiConfig.footerTextSizeSp.toPostTextSize()
        qaDownvoteCount?.textSize = postUiConfig.footerTextSizeSp.toPostTextSize()
        qaScoreCount?.textSize = postUiConfig.footerTextSizeSp.toPostTextSize()
    }

    private fun CommentExpandedViewHolder.scaleTextSizes() {
        headerView.textSize = if (useCondensedTypefaceForCommentHeaders) {
            commentUiConfig.headerTextSizeSp.toCommentTextSize() * 0.9f
        } else {
            commentUiConfig.headerTextSizeSp.toCommentTextSize()
        }
        text.textSize = commentUiConfig.contentTextSizeSp.toCommentTextSize()
        scoreCount?.textSize = commentUiConfig.footerTextSizeSp.toCommentTextSize()
    }

    private fun PostCommentCollapsedItemBinding.scaleTextSizes() {
        headerView.textSize = if (useCondensedTypefaceForCommentHeaders) {
            commentUiConfig.headerTextSizeSp.toCommentTextSize() * 0.9f
        } else {
            commentUiConfig.headerTextSizeSp.toCommentTextSize()
        }
    }

    private fun PostPendingCommentExpandedItemBinding.scaleTextSizes() {
        headerContainer.textSize = postUiConfig.headerTextSizeSp.toCommentTextSize()
        text.textSize = commentUiConfig.contentTextSizeSp.toCommentTextSize()
    }

    private fun PostPendingCommentCollapsedItemBinding.scaleTextSizes() {
        headerContainer.textSize = postUiConfig.headerTextSizeSp.toCommentTextSize()
    }

    private fun Space.updateThreadSpacer(depth: Int, baseDepth: Int, maxDepth: Int) {
        val absoluteDepth = (depth - baseDepth).coerceAtMost(maxDepth)

        updateLayoutParams {
            width = if (absoluteDepth == 0) {
                0
            } else {
                Utils.convertDpToPixel(
                    (commentUiConfig.indentationPerLevelDp * (absoluteDepth - 1)).toFloat() + 16f,
                ).toInt()
            }
        }
    }

    private fun Float.toPostTextSize(): Float =
        this * postUiConfig.textSizeMultiplier * globalFontSizeMultiplier

    private fun Float.toCommentTextSize(): Float =
        this * commentUiConfig.textSizeMultiplier * globalFontSizeMultiplier
}
