package com.idunnololz.summit.lemmy.postAndCommentView

import android.content.Context
import android.graphics.Typeface
import android.text.style.StyleSpan
import android.view.View
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import androidx.appcompat.widget.PopupMenu
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.text.buildSpannedString
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import arrow.core.Either
import com.facebook.drawee.backends.pipeline.Fresco
import com.facebook.drawee.drawable.ScalingUtils
import com.facebook.drawee.interfaces.DraweeController
import com.facebook.drawee.view.SimpleDraweeView
import com.idunnololz.summit.R
import com.idunnololz.summit.account.AccountActionsManager
import com.idunnololz.summit.api.dto.CommentView
import com.idunnololz.summit.api.dto.PersonId
import com.idunnololz.summit.api.dto.PostView
import com.idunnololz.summit.databinding.PostCommentCollapsedItemBinding
import com.idunnololz.summit.databinding.PostCommentExpandedItemBinding
import com.idunnololz.summit.databinding.PostHeaderItemBinding
import com.idunnololz.summit.databinding.PostPendingCommentCollapsedItemBinding
import com.idunnololz.summit.databinding.PostPendingCommentExpandedItemBinding
import com.idunnololz.summit.lemmy.LemmyContentHelper
import com.idunnololz.summit.lemmy.LemmyHeaderHelper
import com.idunnololz.summit.lemmy.LemmyTextHelper
import com.idunnololz.summit.lemmy.PageRef
import com.idunnololz.summit.lemmy.post.ThreadLinesDecoration
import com.idunnololz.summit.lemmy.postListView.CommentUiConfig
import com.idunnololz.summit.lemmy.postListView.PostAndCommentsUiConfig
import com.idunnololz.summit.lemmy.postListView.PostUiConfig
import com.idunnololz.summit.lemmy.utils.bind
import com.idunnololz.summit.lemmy.utils.getFormattedAuthor
import com.idunnololz.summit.lemmy.utils.getFormattedTitle
import com.idunnololz.summit.offline.OfflineManager
import com.idunnololz.summit.preferences.Preferences
import com.idunnololz.summit.preview.VideoType
import com.idunnololz.summit.reddit.LemmyUtils
import com.idunnololz.summit.util.RecycledState
import com.idunnololz.summit.util.Size
import com.idunnololz.summit.util.ThreadLinesHelper
import com.idunnololz.summit.util.Utils
import com.idunnololz.summit.util.abbrevNumber
import com.idunnololz.summit.video.ExoPlayerManager
import com.idunnololz.summit.video.VideoState
import dagger.hilt.android.qualifiers.ActivityContext
import dagger.hilt.android.scopes.ActivityScoped
import javax.inject.Inject

@ActivityScoped
class PostAndCommentViewBuilder @Inject constructor(
    private val activity: FragmentActivity,
    @ActivityContext private val context: Context,
    private val offlineManager: OfflineManager,
    private val accountActionsManager: AccountActionsManager,
    private val preferences: Preferences,
) {

    var uiConfig: PostAndCommentsUiConfig = preferences.getPostAndCommentsUiConfig()
        set(value) {
            field = value

            lemmyContentHelper.config = uiConfig.postUiConfig.fullContentConfig

            postUiConfig = uiConfig.postUiConfig
            commentUiConfig = uiConfig.commentUiConfig
        }

    private var postUiConfig: PostUiConfig = uiConfig.postUiConfig
    private var commentUiConfig: CommentUiConfig = uiConfig.commentUiConfig

    private val lemmyHeaderHelper = LemmyHeaderHelper(context)
    private val lemmyContentHelper = LemmyContentHelper(
        context,
        offlineManager,
        ExoPlayerManager.get(activity),
    )
    private val voteUiHandler = accountActionsManager.voteUiHandler
    val threadLinesHelper = ThreadLinesHelper(context)

    private val tempSize = Size()

    init {
        lemmyContentHelper.config = uiConfig.postUiConfig.fullContentConfig
    }

    fun bindPostView(
        binding: PostHeaderItemBinding,
        container: View,
        postView: PostView,
        instance: String,
        isRevealed: Boolean,
        contentMaxWidth: Int,
        viewLifecycleOwner: LifecycleOwner,
        videoState: VideoState?,
        updateContent: Boolean,
        onRevealContentClickedFn: () -> Unit,
        onImageClick: (String) -> Unit,
        onVideoClick: (url: String, videoType: VideoType, videoState: VideoState?) -> Unit,
        onPageClick: (PageRef) -> Unit,
        onAddCommentClick: (Either<PostView, CommentView>) -> Unit,
        onPostMoreClick: (PostView) -> Unit,
        onSignInRequired: () -> Unit,
        onInstanceMismatch: (String, String) -> Unit,
    ) = with(binding) {

        scaleTextSizes()

        lemmyHeaderHelper.populateHeaderSpan(
            headerContainer = headerContainer,
            postView = postView,
            instance = instance,
            onPageClick = onPageClick,
            listAuthor = false
        )

        title.text = postView.getFormattedTitle()
        author.text = postView.getFormattedAuthor()

        commentButton.text = abbrevNumber(postView.counts.comments.toLong())
        commentButton.isEnabled = !postView.post.locked
        commentButton.setOnClickListener {
            onAddCommentClick(Either.Left(postView))
        }
        addCommentButton.isEnabled = !postView.post.locked
        addCommentButton.setOnClickListener {
            onAddCommentClick(Either.Left(postView))
        }

        moreButton.setOnClickListener {
            onPostMoreClick(postView)
        }

        lemmyContentHelper.setupFullContent(
            reveal = isRevealed,
            tempSize = tempSize,
            videoViewMaxHeight = (container.height
                    - Utils.convertDpToPixel(56f)
                    - Utils.convertDpToPixel(16f)
                    ).toInt(),
            contentMaxWidth = contentMaxWidth,
            fullImageViewTransitionName = "post_image",
            postView = postView,
            instance = instance,
            rootView = root,
            fullContentContainerView = fullContent,
            lazyUpdate = !updateContent,
            videoState = videoState,
            onFullImageViewClickListener = { _, url ->
                onImageClick(url)
            },
            onImageClickListener = { url ->
                onImageClick(url)
            },
            onVideoClickListener = onVideoClick,
            onRevealContentClickedFn = onRevealContentClickedFn,
            onItemClickListener = {},
            onLemmyUrlClick = onPageClick
        )

        voteUiHandler.bind(
            viewLifecycleOwner,
            instance,
            postView,
            upvoteButton,
            downvoteButton,
            upvoteCount,
            onSignInRequired,
            onInstanceMismatch,
        )
    }

    fun bindCommentViewExpanded(
        h: ViewHolder,
        binding: PostCommentExpandedItemBinding,
        baseDepth: Int,
        depth: Int,
        commentView: CommentView,
        isDeleting: Boolean,
        content: String,
        instance: String,
        isPostLocked: Boolean,
        isUpdating: Boolean,
        highlight: Boolean,
        viewLifecycleOwner: LifecycleOwner,
        currentAccountId: PersonId?,
        onImageClick: (String) -> Unit,
        onPageClick: (PageRef) -> Unit,
        collapseSection: (position: Int) -> Unit,
        onAddCommentClick: (Either<PostView, CommentView>) -> Unit,
        onEditCommentClick: (CommentView) -> Unit,
        onDeleteCommentClick: (CommentView) -> Unit,
        onSignInRequired: () -> Unit,
        onInstanceMismatch: (String, String) -> Unit,
    ) = with(binding) {

        scaleTextSizes()

        threadLinesHelper.populateThreadLines(
            threadLinesContainer, depth, baseDepth
        )
        lemmyHeaderHelper.populateHeaderSpan(headerContainer, commentView)

        if (commentView.comment.deleted || isDeleting) {
            text.text = buildSpannedString {
                append(context.getString(R.string.deleted))
                setSpan(StyleSpan(Typeface.ITALIC), 0, length, 0);
            }
        } else if (commentView.comment.removed) {
            text.text = buildSpannedString {
                append(context.getString(R.string.removed))
                setSpan(StyleSpan(Typeface.ITALIC), 0, length, 0);
            }
        } else {
            LemmyTextHelper.bindText(
                textView = text,
                text = content,
                instance = instance,
                onImageClickListener = onImageClick,
                onPageClick = onPageClick,
            )
        }

        val giphyLinks = LemmyUtils.findGiphyLinks(content)
        if (giphyLinks.isNotEmpty()) {
            var lastViewId = 0
            giphyLinks.withIndex().forEach { (index, giphyKey) ->
                mediaContainer.visibility = View.VISIBLE
                val viewId = View.generateViewId()
                val imageView = SimpleDraweeView(context).apply {
                    layoutParams = ConstraintLayout.LayoutParams(
                        ConstraintLayout.LayoutParams.MATCH_CONSTRAINT,
                        ConstraintLayout.LayoutParams.MATCH_CONSTRAINT,
                    ).apply {
                        if (index == 0) {
                            this.topToTop = ConstraintLayout.LayoutParams.PARENT_ID
                        } else {
                            this.topToBottom = lastViewId
                        }
                        this.startToStart = ConstraintLayout.LayoutParams.PARENT_ID
                        this.endToEnd = ConstraintLayout.LayoutParams.PARENT_ID
                        this.dimensionRatio = "H,16:9"
                    }
                    id = viewId
                    hierarchy.actualImageScaleType = ScalingUtils.ScaleType.FIT_CENTER
                }
                mediaContainer.addView(imageView)

                val processedGiphyKey: String = if (giphyKey.contains('|')) {
                    giphyKey.split('|')[0]
                } else {
                    giphyKey
                }

                val fullUrl = "https://i.giphy.com/media/${processedGiphyKey}/giphy.webp"
                val controller: DraweeController = Fresco.newDraweeControllerBuilder()
                    .setUri(fullUrl)
                    .setAutoPlayAnimations(true)
                    .build()
                imageView.controller = controller

                imageView.setOnClickListener {
                    onImageClick(fullUrl)
                }

                lastViewId = viewId
            }
        } else {
            mediaContainer.removeAllViews()
            mediaContainer.visibility = View.GONE
        }

        collapseSectionButton.setOnClickListener {
            collapseSection(h.bindingAdapterPosition)
        }
        topHotspot.setOnClickListener {
            collapseSection(h.bindingAdapterPosition)
        }

        commentButton.isEnabled = !isPostLocked
        commentButton.setOnClickListener {
            onAddCommentClick(Either.Right(commentView))
        }
        moreButton.setOnClickListener {
            PopupMenu(context, moreButton).apply {
                inflate(R.menu.menu_comment_item)
                if (commentView.creator.id !=
                    currentAccountId) {
                    menu.setGroupVisible(R.id.mod_post_actions, false)
                    //menu.findItem(R.id.edit_comment).isVisible = false
                }

                setOnMenuItemClickListener {
                    when (it.itemId) {
                        // fix this later or something
//                        R.id.raw_comment -> {
//                            val action =
//                                PostFragmentDirections.actionPostFragmentToCommentRawDialogFragment(
//                                    commentItemStr = Utils.gson.toJson(commentView)
//                                )
//                            findNavController().navigate(action)
//                        }
                        R.id.edit_comment -> {
                            onEditCommentClick(commentView)
                        }
                        R.id.delete_comment -> {
                            onDeleteCommentClick(commentView)
                        }
                    }
                    true
                }
            }.show()
        }
        if (commentView.comment.distinguished) {
            overlay.visibility = View.VISIBLE
            overlay.setBackgroundResource(R.drawable.locked_overlay)
        } else {
            overlay.visibility = View.GONE
        }

        voteUiHandler.bind(
            viewLifecycleOwner,
            instance,
            commentView,
            upvoteButton,
            downvoteButton,
            upvoteCount,
            onSignInRequired,
            onInstanceMismatch,
        )

        highlightComment(highlight, highlightBg)

        if (isUpdating) {
            progressBar.visibility = View.VISIBLE
        } else {
            progressBar.visibility = View.GONE
        }

        root.tag = ThreadLinesDecoration.ThreadLinesData(
            depth, baseDepth
        )
    }

    fun bindCommentViewCollapsed(
        h: ViewHolder,
        binding: PostCommentCollapsedItemBinding,
        baseDepth: Int,
        depth: Int,
        childrenCount: Int,
        highlight: Boolean,
        isUpdating: Boolean,
        commentView: CommentView,
        expandSection: (position: Int) -> Unit,
    ) = with(binding) {

        scaleTextSizes()

        threadLinesHelper.populateThreadLines(
            threadLinesContainer, depth, baseDepth
        )
        lemmyHeaderHelper.populateHeaderSpan(
            headerContainer = headerContainer,
            item = commentView,
            detailed = true,
            childrenCount = childrenCount
        )

        expandSectionButton.setOnClickListener {
            expandSection(h.absoluteAdapterPosition)
        }
        topHotspot.setOnClickListener {
            expandSection(h.absoluteAdapterPosition)
        }
        if (commentView.comment.distinguished) {
            overlay.visibility = View.VISIBLE
            overlay.setBackgroundResource(R.drawable.locked_overlay)
        } else {
            overlay.visibility = View.GONE
        }

        highlightComment(highlight, highlightBg)

        if (isUpdating) {
            progressBar.visibility = View.VISIBLE
        } else {
            progressBar.visibility = View.GONE
        }

        root.tag = ThreadLinesDecoration.ThreadLinesData(
            depth, baseDepth
        )
    }

    fun bindPendingCommentViewExpanded(
        h: ViewHolder,
        binding: PostPendingCommentExpandedItemBinding,
        baseDepth: Int,
        depth: Int,
        content: String,
        instance: String,
        author: String?,
        highlight: Boolean,
        onImageClick: (String) -> Unit,
        onPageClick: (PageRef) -> Unit,
        collapseSection: (position: Int) -> Unit,
    ) = with(binding) {

        scaleTextSizes()

        val context = binding.root.context

        threadLinesHelper.populateThreadLines(
            threadLinesContainer, depth, baseDepth
        )
        headerContainer.setTextFirstPart(author ?: context.getString(R.string.unknown))
        headerContainer.setTextSecondPart("")

        LemmyTextHelper.bindText(
            textView = text,
            text = content,
            instance = instance,
            onImageClickListener = onImageClick,
            onPageClick = onPageClick,
        )

        val giphyLinks = LemmyUtils.findGiphyLinks(content)
        if (giphyLinks.isNotEmpty()) {
            var lastViewId = 0
            giphyLinks.withIndex().forEach { (index, giphyKey) ->
                mediaContainer.visibility = View.VISIBLE
                val viewId = View.generateViewId()
                val imageView = SimpleDraweeView(context).apply {
                    layoutParams = ConstraintLayout.LayoutParams(
                        ConstraintLayout.LayoutParams.MATCH_CONSTRAINT,
                        ConstraintLayout.LayoutParams.MATCH_CONSTRAINT,
                    ).apply {
                        if (index == 0) {
                            this.topToTop = ConstraintLayout.LayoutParams.PARENT_ID
                        } else {
                            this.topToBottom = lastViewId
                        }
                        this.startToStart = ConstraintLayout.LayoutParams.PARENT_ID
                        this.endToEnd = ConstraintLayout.LayoutParams.PARENT_ID
                        this.dimensionRatio = "H,16:9"
                    }
                    id = viewId
                    hierarchy.actualImageScaleType = ScalingUtils.ScaleType.FIT_CENTER
                }
                mediaContainer.addView(imageView)

                val processedGiphyKey: String = if (giphyKey.contains('|')) {
                    giphyKey.split('|')[0]
                } else {
                    giphyKey
                }

                val fullUrl = "https://i.giphy.com/media/${processedGiphyKey}/giphy.webp"
                val controller: DraweeController = Fresco.newDraweeControllerBuilder()
                    .setUri(fullUrl)
                    .setAutoPlayAnimations(true)
                    .build()
                imageView.controller = controller

                imageView.setOnClickListener {
                    onImageClick(fullUrl)
                }

                lastViewId = viewId
            }
        } else {
            mediaContainer.removeAllViews()
            mediaContainer.visibility = View.GONE
        }

        collapseSectionButton.setOnClickListener {
            collapseSection(h.bindingAdapterPosition)
        }
        topHotspot.setOnClickListener {
            collapseSection(h.bindingAdapterPosition)
        }

        highlightComment(highlight, highlightBg)

        root.tag = ThreadLinesDecoration.ThreadLinesData(
            depth, baseDepth
        )
    }

    fun bindPendingCommentViewCollapsed(
        holder: ViewHolder,
        binding: PostPendingCommentCollapsedItemBinding,
        baseDepth: Int,
        depth: Int,
        author: String?,
        highlight: Boolean,
        expandSection: (position: Int) -> Unit,
    ) = with(binding) {

        scaleTextSizes()

        val context = holder.itemView.context
        threadLinesHelper.populateThreadLines(
            threadLinesContainer, depth, baseDepth
        )
        headerContainer.setTextFirstPart(author ?: context.getString(R.string.unknown))
        headerContainer.setTextSecondPart("")

        expandSectionButton.setOnClickListener {
            expandSection(holder.absoluteAdapterPosition)
        }
        topHotspot.setOnClickListener {
            expandSection(holder.absoluteAdapterPosition)
        }
        overlay.visibility = View.GONE

        highlightComment(highlight, highlightBg)

        root.tag = ThreadLinesDecoration.ThreadLinesData(
            depth, baseDepth
        )
    }

    fun recycle(b: PostHeaderItemBinding): RecycledState {
        val recycledState = lemmyContentHelper.recycleFullContent(b.fullContent)
        offlineManager.cancelFetch(b.root)
        voteUiHandler.unbindVoteUi(b.upvoteCount)
        return recycledState
    }


    private fun highlightComment(isCommentHighlighted: Boolean, bg: View) {
        if (isCommentHighlighted) {
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
        upvoteCount.textSize = postUiConfig.footerTextSizeSp.toPostTextSize()
        author.textSize = postUiConfig.footerTextSizeSp.toPostTextSize()
    }

    private fun PostCommentExpandedItemBinding.scaleTextSizes() {
        headerContainer.textSize = postUiConfig.headerTextSizeSp.toCommentTextSize()
        text.textSize = commentUiConfig.contentTextSizeSp.toCommentTextSize()
        upvoteCount.textSize = postUiConfig.footerTextSizeSp.toCommentTextSize()
    }

    private fun PostCommentCollapsedItemBinding.scaleTextSizes() {
        headerContainer.textSize = postUiConfig.headerTextSizeSp.toCommentTextSize()
    }

    private fun PostPendingCommentExpandedItemBinding.scaleTextSizes() {
        headerContainer.textSize = postUiConfig.headerTextSizeSp.toCommentTextSize()
        text.textSize = commentUiConfig.contentTextSizeSp.toCommentTextSize()
    }

    private fun PostPendingCommentCollapsedItemBinding.scaleTextSizes() {
        headerContainer.textSize = postUiConfig.headerTextSizeSp.toCommentTextSize()
    }

    private fun Float.toPostTextSize(): Float =
        this * postUiConfig.textSizeMultiplier

    private fun Float.toCommentTextSize(): Float =
        this * commentUiConfig.textSizeMultiplier
}