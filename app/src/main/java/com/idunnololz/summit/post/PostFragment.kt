package com.idunnololz.summit.post

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.os.Bundle
import android.text.Editable
import android.text.SpannableString
import android.text.TextWatcher
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.widget.PopupMenu
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavOptions
import androidx.navigation.fragment.FragmentNavigatorExtras
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.transition.*
import com.facebook.drawee.backends.pipeline.Fresco
import com.facebook.drawee.drawable.ScalingUtils
import com.facebook.drawee.interfaces.DraweeController
import com.facebook.drawee.view.SimpleDraweeView
import com.google.android.material.button.MaterialButton
import com.google.android.material.snackbar.Snackbar
import com.idunnololz.summit.R
import com.idunnololz.summit.alert.AlertDialogFragment
import com.idunnololz.summit.auth.RedditAuthManager
import com.idunnololz.summit.databinding.FragmentPostBinding
import com.idunnololz.summit.databinding.PostCommentExpandedItemBinding
import com.idunnololz.summit.history.HistoryManager
import com.idunnololz.summit.history.HistorySaveReason
import com.idunnololz.summit.main.MainActivity
import com.idunnololz.summit.offline.OfflineManager
import com.idunnololz.summit.post.PostFragment.Item.*
import com.idunnololz.summit.reddit.*
import com.idunnololz.summit.reddit.ext.PostItem
import com.idunnololz.summit.reddit.ext.flattenPostData
import com.idunnololz.summit.reddit.ext.getFormattedTitle
import com.idunnololz.summit.reddit_actions.ActionInfo
import com.idunnololz.summit.reddit_actions.RedditAction
import com.idunnololz.summit.reddit_objects.*
import com.idunnololz.summit.util.*
import com.idunnololz.summit.util.ext.addDefaultAnim
import com.idunnololz.summit.util.ext.getColorCompat
import com.idunnololz.summit.video.ExoPlayerManager
import com.idunnololz.summit.video.VideoState
import com.idunnololz.summit.view.LoadingView
import com.idunnololz.summit.view.RedditHeaderView


class PostFragment : BaseFragment<FragmentPostBinding>(), AlertDialogFragment.AlertDialogFragmentListener {
    companion object {
        private val TAG = PostFragment::class.java.canonicalName

        private const val CONFIRM_DELETE_COMMENT_TAG = "CONFIRM_DELETE_COMMENT_TAG"
        private const val EXTRA_COMMENT_ID = "EXTRA_COMMENT_ID"
    }

    private val args: PostFragmentArgs by navArgs()

    private lateinit var postViewModel: PostViewModel

    private lateinit var adapter: RedditObjectAdapter

    private lateinit var jsonUrl: String

    private var offlineManager = OfflineManager.instance

    private var hasConsumedJumpToComments: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val context = requireContext()

        postponeEnterTransition()

        postViewModel = ViewModelProvider(this).get(PostViewModel::class.java)
        adapter = RedditObjectAdapter(context, args.reveal) {
            postViewModel.fetchPostData(RedditUtils.toJsonUrl(args.url), force = true)
        }

        sharedElementEnterTransition = SharedElementTransition()
        sharedElementReturnTransition = SharedElementTransition()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        requireMainActivity().apply {
            setupForFragment<PostFragment>()
        }

        setBinding(FragmentPostBinding.inflate(inflater, container, false))

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        requireMainActivity().apply {
            insetRootViewAutomatically(viewLifecycleOwner, view)
            getCustomAppBarController().setSubreddit(args.currentSubreddit ?: "")
            getCustomAppBarController().clearPageIndex()
        }

        val context = requireContext()
        val swipeRefreshLayout = binding.swipeRefreshLayout

        jsonUrl = RedditUtils.toJsonUrl(args.url)

        (activity as MainActivity).apply {
            hideActionBar()

            headerOffset.observe(viewLifecycleOwner) {
                if (it != null)
                    getView()?.translationY = it.toFloat()
            }

            getCustomAppBarController().setup(
                { controller, url ->
                    val action =
                        PostFragmentDirections.actionPostFragmentToSubredditFragment(url = url)
                    findNavController().navigate(action)
                    Utils.hideKeyboard(activity)
                    controller.hide()
                },
                {
                    PopupMenu(context, it).apply {
                        inflate(R.menu.menu_fragment_post)

                        val selectedItem = when (postViewModel.commentsSortOrder) {
                            CommentsSortOrder.CONFIDENCE -> menu.findItem(R.id.comments_order_confidence)
                            CommentsSortOrder.TOP -> menu.findItem(R.id.comments_order_top)
                            CommentsSortOrder.NEW -> menu.findItem(R.id.comments_order_new)
                            CommentsSortOrder.CONTROVERSIAL -> menu.findItem(R.id.comments_order_controversial)
                            CommentsSortOrder.OLD -> menu.findItem(R.id.comments_order_old)
                            CommentsSortOrder.RANDOM -> menu.findItem(R.id.comments_order_random)
                            CommentsSortOrder.QA -> menu.findItem(R.id.comments_order_qa)
                            null -> menu.findItem(R.id.comments_order_confidence)
                        }

                        selectedItem?.let { selectedItem ->
                            selectedItem.title = SpannableString(selectedItem.title).apply {
                                setSpan(
                                    ForegroundColorSpan(context.getColorCompat(R.color.colorPrimary)),
                                    0,
                                    length,
                                    0
                                )
                                setSpan(StyleSpan(Typeface.BOLD), 0, length, 0)
                            }
                        }

                        fun setCommentsSortOrderAndRefresh(sortOrder: CommentsSortOrder) {
                            swipeRefreshLayout.isRefreshing = true
                            postViewModel.setCommentsSortOrder(sortOrder)
                            postViewModel.fetchPostData(RedditUtils.toJsonUrl(args.url), force = true)
                        }

                        setOnMenuItemClickListener {
                            when (it.itemId) {
                                R.id.share -> {
                                    val sendIntent: Intent = Intent().apply {
                                        action = Intent.ACTION_SEND
                                        putExtra(
                                            Intent.EXTRA_TEXT,
                                            RedditUtils.toSharedLink(args.url)
                                        )
                                        type = "text/plain"
                                    }

                                    val shareIntent = Intent.createChooser(sendIntent, null)
                                    startActivity(shareIntent)
                                    true
                                }
                                R.id.go_to -> {
                                    findNavController().navigate(R.id.goToDialogFragment)
                                    true
                                }
                                R.id.comments_order_confidence -> {
                                    setCommentsSortOrderAndRefresh(CommentsSortOrder.CONFIDENCE)
                                    true
                                }
                                R.id.comments_order_top -> {
                                    setCommentsSortOrderAndRefresh(CommentsSortOrder.TOP)
                                    true
                                }
                                R.id.comments_order_new -> {
                                    setCommentsSortOrderAndRefresh(CommentsSortOrder.NEW)
                                    true
                                }
                                R.id.comments_order_controversial -> {
                                    setCommentsSortOrderAndRefresh(CommentsSortOrder.CONTROVERSIAL)
                                    true
                                }
                                R.id.comments_order_old -> {
                                    setCommentsSortOrderAndRefresh(CommentsSortOrder.OLD)
                                    true
                                }
                                R.id.comments_order_random -> {
                                    setCommentsSortOrderAndRefresh(CommentsSortOrder.RANDOM)
                                    true
                                }
                                R.id.comments_order_qa -> {
                                    setCommentsSortOrderAndRefresh(CommentsSortOrder.QA)
                                    true
                                }
                                else -> false
                            }
                        }
                    }.show()
                }
            )
        }

        swipeRefreshLayout.setOnRefreshListener {
            forceRefresh()
        }

        if (postViewModel.redditObjects.value?.status != Status.SUCCESS) {
            args.originalPost?.let { originalPost ->
                adapter.setStartingData(listOf(ListingItemObject(originalPost)))
                onMainListingItemRetreived(originalPost)
            } ?: binding.loadingView.showProgressBar()
        }
        postViewModel.fetchPostData(jsonUrl)

        HistoryManager.instance.recordVisit(
            jsonUrl = jsonUrl,
            saveReason = HistorySaveReason.LOADING,
            originalPost = args.originalPost
        )

        postViewModel.redditObjects.observe(viewLifecycleOwner,
            Observer {
                when (it.status) {
                    Status.LOADING -> {
                        adapter.isLoaded = false
                    }
                    Status.SUCCESS -> {
                        swipeRefreshLayout.isRefreshing = false

                        binding.loadingView.hideAll()

                        adapter.setData(it.data)

                        if (!hasConsumedJumpToComments && args.jumpToComments) {
                            hasConsumedJumpToComments = true

                            (binding.recyclerView.layoutManager as LinearLayoutManager).scrollToPositionWithOffset(
                                1,
                                0
                            )
                        }
                    }
                    Status.FAILED -> {
                        adapter.setError(it.requireError())
                        swipeRefreshLayout.isRefreshing = false
                    }
                }
            }
        )

        postViewModel.redditMoreComments.observe(viewLifecycleOwner) {
            adapter.setMoreCommentData(it)
        }

        binding.recyclerView.adapter = adapter
        binding.recyclerView.setHasFixedSize(true)
        binding.recyclerView.layoutManager = LinearLayoutManager(context)
        binding.recyclerView.addItemDecoration(ThreadLinesDecoration(context))
        binding.recyclerView.addItemDecoration(PostDividerDecoration(context))
        binding.fastScroller.setRecyclerView(binding.recyclerView)

        if (!hasConsumedJumpToComments && args.jumpToComments) {
            (binding.recyclerView.layoutManager as LinearLayoutManager).scrollToPositionWithOffset(1, 0)
        }

        binding.recyclerView.post {
            startPostponedEnterTransition()
        }
    }

    private fun forceRefresh() {
        postViewModel.fetchPostData(jsonUrl, force = true)
    }

    fun onMainListingItemRetreived(listingItem: ListingItem) {
        (activity as? MainActivity)?.apply {
            getCustomAppBarController().setSubreddit(args.currentSubreddit ?: listingItem.subredditNamePrefixed)
            getCustomAppBarController().clearPageIndex()
        }

        HistoryManager.instance.recordVisit(
            jsonUrl = jsonUrl,
            saveReason = HistorySaveReason.LOADED,
            originalPost = listingItem
        )
    }

    override fun onPositiveClick(dialog: AlertDialogFragment, tag: String?) {
        when (tag) {
            CONFIRM_DELETE_COMMENT_TAG -> {
                val commentId = dialog.getExtra(EXTRA_COMMENT_ID)
                if (commentId != null)
                    PendingActionsManager.instance.deleteComment(commentId, this) {
                        forceRefresh()
                    }
            }
        }
    }

    override fun onNegativeClick(dialog: AlertDialogFragment, tag: String?) {
        // do nothing
    }

    private class PostDividerDecoration(
        private val context: Context
    ) : RecyclerView.ItemDecoration() {

        private val linePaint = Paint().apply {
            color = ContextCompat.getColor(context, R.color.colorDivider)
            strokeWidth = Utils.convertDpToPixel(1f)
        }

        override fun onDraw(c: Canvas, parent: RecyclerView, state: RecyclerView.State) {
            val childCount = parent.childCount

            for (i in 1 until childCount) {
                val previousView = parent.getChildAt(i - 1)
                val view = parent.getChildAt(i)
                val previousTag = previousView.tag

                val drawDivider = previousTag is HeaderItem

                if (drawDivider) {
                    c.drawLine(
                        view.left.toFloat(),
                        view.top.toFloat(),
                        view.right.toFloat(),
                        view.top.toFloat(),
                        linePaint
                    )
                }
            }
        }
    }

    private class ThreadLinesDecoration(
        private val context: Context
    ) : RecyclerView.ItemDecoration() {

        val distanceBetweenLines =
            context.resources.getDimensionPixelSize(R.dimen.thread_line_total_size)
        val startingPadding =
            context.resources.getDimensionPixelSize(R.dimen.reddit_content_horizontal_padding)
        val topOverdraw = context.resources.getDimensionPixelSize(R.dimen.comment_top_overdraw)

        private val linePaint = Paint().apply {
            color = ContextCompat.getColor(context, R.color.colorThreadLines)
            strokeWidth = Utils.convertDpToPixel(2f)
        }

        override fun onDraw(c: Canvas, parent: RecyclerView, state: RecyclerView.State) {
            val childCount = parent.childCount

            for (i in 0 until childCount) {
                val view = parent.getChildAt(i)
                val lastTag = if (i == 0) {
                    null
                } else {
                    parent.getChildAt(i - 1).tag
                }
                val tag = view.tag
                val translationX = view.translationX
                val translationY = view.translationY
                var topOverdraw = topOverdraw
                val totalDepth = if (tag is CommentItem) {
                    tag.depth - tag.baseDepth
                } else if (tag is MoreCommentsItem) {
                    tag.depth - tag.baseDepth
                } else {
                    -1
                }

                if (lastTag is ComposeCommentItem || lastTag is EditCommentItem) {
                    topOverdraw = 0
                }

                if (totalDepth == -1) continue

                for (lineIndex in 0 until totalDepth) {
                    val x =
                        view.left + (lineIndex.toFloat()) * distanceBetweenLines + startingPadding
                    c.drawLine(
                        x + translationX,
                        view.top.toFloat() - topOverdraw + translationY,
                        x + translationX,
                        view.bottom.toFloat() + translationY,
                        linePaint
                    )
                }
            }
        }
    }

    sealed class Item(
        val type: Int,
        open val id: String
    ) {
        companion object {
            const val TYPE_LISTING_ITEM = 1
            const val TYPE_COMMENT_EXPANDED_ITEM = 2
            const val TYPE_COMMENT_COLLAPSED_ITEM = 3
            const val TYPE_MORE_COMMENTS_ITEM = 4
            const val TYPE_PROGRESS = 5
            const val TYPE_COMPOSE_COMMENT_ITEM = 6
            const val TYPE_EDIT_COMMENT_ITEM = 7
        }

        data class HeaderItem(
            val listingItem: ListingItem,
            var videoState: VideoState?
        ) : Item(TYPE_LISTING_ITEM, listingItem.name)

        data class CommentItem(
            val commentItem: RedditCommentItem,
            val depth: Int,
            val baseDepth: Int,
            val isExpanded: Boolean,
            val isPending: Boolean
        ) : Item(
            if (isExpanded) TYPE_COMMENT_EXPANDED_ITEM else TYPE_COMMENT_COLLAPSED_ITEM,
            commentItem.name
        )

        data class MoreCommentsItem(
            val moreItem: MoreItem,
            val depth: Int,
            val baseDepth: Int,
            val linkId: String
        ) : Item(TYPE_MORE_COMMENTS_ITEM, moreItem.name)

        data class ComposeCommentItem(
            override val id: String,
            val parentId: String,
            val depth: Int,
            val baseDepth: Int,
            var comment: CharSequence,
            var isPosting: Boolean = false,
            var error: Throwable? = null
        ) : Item(TYPE_COMPOSE_COMMENT_ITEM, id)

        data class EditCommentItem(
            override val id: String,
            val commentName: String,
            val depth: Int,
            val baseDepth: Int,
            var comment: CharSequence,
            var isPosting: Boolean = false,
            var error: Throwable? = null
        ) : Item(TYPE_EDIT_COMMENT_ITEM, id)

        class ProgressItem() : Item(TYPE_PROGRESS, "wew_pls_no_progress")
    }

    class HeaderViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val headerContainer: RedditHeaderView = view.findViewById(R.id.headerContainer)
        val titleTextView: TextView = view.findViewById(R.id.title)
        val authorTextView: TextView = view.findViewById(R.id.author)
        val commentButton: MaterialButton = itemView.findViewById(R.id.commentButton)
        val upvoteCount: TextView = itemView.findViewById(R.id.upvoteCount)
        val fullContentContainerView: ViewGroup = itemView.findViewById(R.id.fullContent)
        val upvoteButton: ImageView = itemView.findViewById(R.id.upvoteButton)
        val downvoteButton: ImageView = itemView.findViewById(R.id.downvoteButton)
    }

    class CommentExpandedViewHolder(
        val binding: PostCommentExpandedItemBinding
    ) : RecyclerView.ViewHolder(binding.root)

    class CommentCollapsedViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val headerView: RedditHeaderView = view.findViewById(R.id.headerContainer)
        val threadLinesContainer: ViewGroup = view.findViewById(R.id.threadLinesContainer)
        val expandSectionButton: ImageButton = itemView.findViewById(R.id.expandSectionButton)
        val topHotspot: View = itemView.findViewById(R.id.topHotspot)
        val overlay: View = itemView.findViewById(R.id.overlay)
    }

    class MoreCommentsViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val threadLinesContainer: ViewGroup = view.findViewById(R.id.threadLinesContainer)
        val moreButton: Button = itemView.findViewById(R.id.moreButton)
    }

    class LoadingViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val loadingView: LoadingView = view.findViewById(R.id.loadingView)
    }

    class ComposeCommentViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val rootView: ConstraintLayout = view as ConstraintLayout
        val editText: EditText = view.findViewById(R.id.editText)
        val addComment: Button = view.findViewById(R.id.addComment)
        val cancelButton: Button = view.findViewById(R.id.cancelButton)
        val loadingView: LoadingView = view.findViewById(R.id.loadingView)

        var boundTextWatcher: TextWatcher? = null

        val postingConstraintSet: ConstraintSet = ConstraintSet()
        val normalConstraintSet: ConstraintSet = ConstraintSet()

        init {
            normalConstraintSet.clone(rootView)
            postingConstraintSet.clone(view.context, R.layout.compose_comment_item_posting)
        }
    }

    class EditCommentViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val rootView: ConstraintLayout = view as ConstraintLayout
        val editText: EditText = view.findViewById(R.id.editText)
        val saveEditsButton: Button = view.findViewById(R.id.saveEditsButton)
        val cancelButton: Button = view.findViewById(R.id.cancelButton)
        val loadingView: LoadingView = view.findViewById(R.id.loadingView)

        var boundTextWatcher: TextWatcher? = null

        val postingConstraintSet: ConstraintSet = ConstraintSet()
        val normalConstraintSet: ConstraintSet = ConstraintSet()

        init {
            normalConstraintSet.clone(rootView)
            postingConstraintSet.clone(view.context, R.layout.edit_comment_item_posting)
        }
    }

    private inner class RedditObjectAdapter(
        private val context: Context,
        private val revealAll: Boolean,
        private val onRefreshClickCb: () -> Unit
    ) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

        private val inflater = LayoutInflater.from(context)

        private var items: List<Item> = listOf()

        private val redditHeaderHelper = RedditHeaderHelper(context)
        private val redditThreadLinesHelper = RedditThreadLinesHelper(context)
        private val redditContentHelper = RedditContentHelper(
            context, this@PostFragment, offlineManager, ExoPlayerManager.get(this@PostFragment)
        )

        private var parentHeight: Int = 0

        /**
         * Set of items that is hidden by default but is reveals (ie. nsfw or spoiler tagged)
         */
        private var revealedItems = mutableSetOf<String>()
        private var moreCommentsMap: Map<String, List<CommentItemObject>> = mapOf()
        private var composeCommentItems: HashMap<String, ComposeCommentItem> = hashMapOf()
        private var editCommentItems: HashMap<String, EditCommentItem> = hashMapOf()

        private var rawData: List<RedditObject>? = null

        private var tempSize = Size()

        var isFirstLoad: Boolean = true
        var isLoaded: Boolean = false
        private var error: Throwable? = null

        val onRedditActionChangedCallback: (DataWithState<RedditAction>) -> Unit = {
            if (it.status == Status.SUCCESS) {
                val actionInfo = it.data.info
                if (actionInfo is ActionInfo.CommentActionInfo) {
                    removeComposeCommentItemFor(actionInfo.parentId)
                } else if (actionInfo is ActionInfo.EditActionInfo) {
                    removeEditCommentItemFor(actionInfo.thingId)
                }
            } else if (it.status == Status.FAILED) {
                val error = it.requireError() as PendingActionsManager.PendingActionsException
                val actionInfo = error.redditAction.info
                if (actionInfo is ActionInfo.CommentActionInfo) {
                    setComposeCommentItemErrorFor(actionInfo.parentId, error = error.cause)

                    Snackbar.make(
                        requireMainActivity().getSnackbarContainer(),
                        R.string.error_post_comment_failed,
                        Snackbar.LENGTH_LONG
                    ).show()
                } else if (actionInfo is ActionInfo.EditActionInfo) {
                    setEditCommentItemErrorFor(actionInfo.thingId, error = error.cause)

                    Snackbar.make(
                        requireMainActivity().getSnackbarContainer(),
                        R.string.error_edit_comment_failed,
                        Snackbar.LENGTH_LONG
                    ).show()
                }
            }
        }

        override fun getItemViewType(position: Int): Int = when (val item = items[position]) {
            is HeaderItem -> R.layout.post_header_item
            is CommentItem ->
                if (item.isExpanded)
                    R.layout.post_comment_expanded_item
                else
                    R.layout.post_comment_collapsed_item
            is MoreCommentsItem -> R.layout.post_more_comments_item
            is ProgressItem -> R.layout.generic_loading_item
            is ComposeCommentItem -> R.layout.compose_comment_item
            is EditCommentItem -> R.layout.edit_comment_item
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            val v = inflater.inflate(viewType, parent, false)

            parentHeight = parent.height

            return when (viewType) {
                R.layout.post_header_item -> HeaderViewHolder(v)
                R.layout.post_comment_expanded_item ->
                    CommentExpandedViewHolder(PostCommentExpandedItemBinding.bind(v))
                R.layout.post_comment_collapsed_item -> CommentCollapsedViewHolder(v)
                R.layout.post_more_comments_item -> MoreCommentsViewHolder(v)
                R.layout.generic_loading_item -> LoadingViewHolder(v)
                R.layout.compose_comment_item -> ComposeCommentViewHolder(v)
                R.layout.edit_comment_item -> EditCommentViewHolder(v)
                else -> throw RuntimeException("ViewType: $viewType")
            }
        }

        override fun onBindViewHolder(
            holder: RecyclerView.ViewHolder,
            position: Int,
            payloads: MutableList<Any>
        ) {
            when (val item = items[position]) {
                is HeaderItem -> {
                    if (payloads.isEmpty()) {
                        super.onBindViewHolder(holder, position, payloads)
                    } else {
                        // this is an incremental update... Only update the stats, do not update content...
                        val h = holder as HeaderViewHolder

                        redditHeaderHelper.populateHeaderSpan(
                            h.headerContainer, item.listingItem, listAuthor = false
                        )

                        h.titleTextView.text = item.listingItem.getFormattedTitle()
                        h.authorTextView.text = RedditUtils.formatAuthor(item.listingItem.author)

                        h.commentButton.text =
                            RedditUtils.abbrevNumber(item.listingItem.numComments.toLong())
                        h.upvoteCount.text = RedditUtils.getUpvoteText(item.listingItem)
                        h.commentButton.setOnClickListener {
                            handleCommentClick(item.listingItem.name)
                        }

                        h.commentButton.isEnabled = !item.listingItem.locked

                        redditContentHelper.setupFullContent(
                            reveal = revealAll || revealedItems.contains(item.listingItem.name),
                            tempSize = tempSize,
                            videoViewMaxHeight = (binding.recyclerView.height - Utils.convertDpToPixel(16f)).toInt(),
                            fullImageViewTransitionName = "post_image",
                            listingItem = item.listingItem,
                            rootView = h.itemView,
                            fullContentContainerView = h.fullContentContainerView,
                            onFullImageViewClickListener = { v, url ->
                                val action =
                                    PostFragmentDirections.actionPostFragmentToImageViewerFragment(
                                        null,
                                        url,
                                        null
                                    )

                                if (v != null) {
                                    val extras = FragmentNavigatorExtras(
                                        v to "image_view"
                                    )

                                    findNavController().navigate(action, extras)
                                } else {
                                    findNavController().navigate(
                                        action, NavOptions.Builder()
                                            .addDefaultAnim()
                                            .build()
                                    )
                                }
                            },
                            onRevealContentClickedFn = {
                                revealedItems.add(item.listingItem.name)
                                notifyItemChanged(h.adapterPosition)
                            },
                            lazyUpdate = true,
                            videoState = item.videoState
                        )

                        UserActionsHelper.setupActions(
                            item.listingItem.name,
                            item.listingItem.getLikesWithLikesManager(),
                            viewLifecycleOwner,
                            childFragmentManager,
                            h.upvoteButton,
                            h.downvoteButton
                        ) a@{ onVote ->
                            Log.d(TAG, "onVote(): $onVote")
                            if (h.bindingAdapterPosition < 0) {
                                return@a
                            }
                            item.listingItem.likes = when {
                                onVote > 0 -> true
                                onVote < 0 -> false
                                else -> null
                            }
                            (items[h.bindingAdapterPosition] as HeaderItem).listingItem.likes =
                                item.listingItem.likes
                            notifyItemChanged(h.bindingAdapterPosition, Unit)
                            Log.d(TAG, "onVote(): $onVote. new state: ${item.listingItem.likes}")
                        }
                    }
                }
                is ComposeCommentItem -> {
                    if (payloads.isEmpty()) {
                        super.onBindViewHolder(holder, position, payloads)
                    } else {
                        val h = holder as ComposeCommentViewHolder

                        if (!item.isPosting) {
                            h.postingConstraintSet.applyTo(h.rootView)
                        }
                        h.itemView.tag = item
                    }
                }
                is EditCommentItem -> {
                    if (payloads.isEmpty()) {
                        super.onBindViewHolder(holder, position, payloads)
                    } else {
                        val h = holder as EditCommentViewHolder

                        if (!item.isPosting) {
                            h.postingConstraintSet.applyTo(h.rootView)
                        }
                        h.itemView.tag = item
                    }
                }
                else -> super.onBindViewHolder(holder, position, payloads)
            }
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            when (getItemViewType(position)) {
                R.layout.post_header_item -> {
                    Log.d(TAG, "Post item is bound!")
                    val h = holder as HeaderViewHolder
                    val item = items[position] as HeaderItem

                    setupTransitionAnimation(h.titleTextView)

                    ViewCompat.setTransitionName(h.titleTextView, "title")

                    redditHeaderHelper.populateHeaderSpan(
                        h.headerContainer, item.listingItem, listAuthor = false
                    )

                    h.titleTextView.text = item.listingItem.getFormattedTitle()
                    h.authorTextView.text = RedditUtils.formatAuthor(item.listingItem.author)

                    h.commentButton.text =
                        RedditUtils.abbrevNumber(item.listingItem.numComments.toLong())
                    h.upvoteCount.text = RedditUtils.getUpvoteText(item.listingItem)

                    h.commentButton.isEnabled = !item.listingItem.locked
                    h.commentButton.setOnClickListener {
                        handleCommentClick(item.listingItem.name)
                    }

                    redditContentHelper.setupFullContent(
                        reveal = revealAll || revealedItems.contains(item.listingItem.name),
                        tempSize = tempSize,
                        videoViewMaxHeight = (binding.recyclerView.height - Utils.convertDpToPixel(16f)).toInt(),
                        fullImageViewTransitionName = "post_image",
                        listingItem = item.listingItem,
                        rootView = h.itemView,
                        fullContentContainerView = h.fullContentContainerView,
                        onFullImageViewClickListener = { v, url ->
                            val action =
                                PostFragmentDirections.actionPostFragmentToImageViewerFragment(
                                    null,
                                    url,
                                    null
                                )

                            if (v != null) {
                                val extras = FragmentNavigatorExtras(
                                    v to "image_view"
                                )

                                findNavController().navigate(action, extras)
                            } else {
                                findNavController().navigate(
                                    action, NavOptions.Builder()
                                        .addDefaultAnim()
                                        .build()
                                )
                            }
                        },
                        onRevealContentClickedFn = {
                            revealedItems.add(item.listingItem.name)
                            notifyItemChanged(h.adapterPosition)
                        },
                        videoState = item.videoState
                    )

                    Log.d(TAG, "header vote state: ${item.listingItem.likes}")
                    UserActionsHelper.setupActions(
                        item.listingItem.name,
                        item.listingItem.getLikesWithLikesManager(),
                        viewLifecycleOwner,
                        childFragmentManager,
                        h.upvoteButton,
                        h.downvoteButton
                    ) a@{ onVote ->
                        Log.d(TAG, "onVote(): $onVote")
                        if (h.bindingAdapterPosition < 0) {
                            return@a
                        }
                        item.listingItem.likes = when {
                            onVote > 0 -> true
                            onVote < 0 -> false
                            else -> null
                        }
                        (items[h.bindingAdapterPosition] as HeaderItem).listingItem.likes =
                            item.listingItem.likes
                        notifyItemChanged(h.bindingAdapterPosition, Unit)
                        Log.d(TAG, "onVote(): $onVote. new state: ${item.listingItem.likes}")
                    }

                    h.itemView.tag = item
                }
                R.layout.post_comment_expanded_item -> {
                    val h = holder as CommentExpandedViewHolder
                    val b = h.binding
                    val item = items[position] as CommentItem
                    val body = item.commentItem.body

                    redditThreadLinesHelper.populateThreadLines(
                        b.threadLinesContainer, item.depth, item.baseDepth
                    )
                    redditHeaderHelper.populateHeaderSpan(b.headerContainer, item.commentItem)

                    RedditUtils.bindRedditText(b.text, body)
                    b.text.movementMethod = CustomLinkMovementMethod().apply {
                        onLinkLongClickListener = DefaultLinkLongClickListener(context)
                    }

                    val giphyLinks = RedditUtils.findGiphyLinks(body)
                    if (giphyLinks.isNotEmpty()) {
                        var lastViewId = 0
                        giphyLinks.withIndex().forEach { (index, giphyKey) ->
                            b.mediaContainer.visibility = View.VISIBLE
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
                            b.mediaContainer.addView(imageView)

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
                                val action =
                                    PostFragmentDirections.actionPostFragmentToImageViewerFragment(
                                        title = null,
                                        url = fullUrl,
                                        mimeType = null
                                    )
                                findNavController().navigate(
                                    action, NavOptions.Builder()
                                        .addDefaultAnim()
                                        .build()
                                )
                            }

                            lastViewId = viewId
                        }
                    } else {
                        b.mediaContainer.removeAllViews()
                        b.mediaContainer.visibility = View.GONE
                    }

                    b.upvoteCount.text = RedditUtils.getUpvoteText(item.commentItem)

                    b.collapseSectionButton.setOnClickListener {
                        collapseSection(h.bindingAdapterPosition)
                    }
                    b.topHotspot.setOnClickListener {
                        collapseSection(h.bindingAdapterPosition)
                    }
                    b.commentButton.setOnClickListener {
                        handleCommentClick(item.commentItem.name)
                    }
                    b.moreButton.setOnClickListener {
                        Log.d(TAG, "Item: $item")
                        Log.d(TAG, "Body: ${Utils.fromHtml(item.commentItem.bodyHtml)}")

                        RedditUtils.bindRedditText(b.text, body)
                        redditHeaderHelper.populateHeaderSpan(b.headerContainer, item.commentItem)

                        PopupMenu(context, b.moreButton).apply {
                            inflate(R.menu.menu_comment_item)

                            if (!item.commentItem.canModPost) {
                                menu.setGroupVisible(R.id.mod_post_actions, false)
                                //menu.findItem(R.id.edit_comment).isVisible = false
                            }

                            setOnMenuItemClickListener {
                                when (it.itemId) {
                                    R.id.raw_comment -> {
                                        val action =
                                            PostFragmentDirections.actionPostFragmentToCommentRawDialogFragment(
                                                commentItemStr = Utils.gson.toJson(item.commentItem)
                                            )
                                        findNavController().navigate(action)
                                    }
                                    R.id.edit_comment -> {
                                        handleEditClick(item.commentItem)
                                    }
                                    R.id.delete_comment -> {
                                        AlertDialogFragment.Builder()
                                            .setMessage(R.string.delete_comment_confirm)
                                            .setPositiveButton(android.R.string.yes)
                                            .setNegativeButton(android.R.string.no)
                                            .setExtra(EXTRA_COMMENT_ID, item.commentItem.name)
                                            .createAndShow(
                                                childFragmentManager,
                                                CONFIRM_DELETE_COMMENT_TAG
                                            )
                                    }
                                }
                                true
                            }
                        }.show()
                    }
                    if (item.commentItem.locked) {
                        b.overlay.visibility = View.VISIBLE
                        b.overlay.setBackgroundResource(R.drawable.locked_overlay)
                    } else {
                        b.overlay.visibility = View.GONE
                    }

                    UserActionsHelper.setupActions(
                        item.commentItem.name,
                        item.commentItem.likes,
                        viewLifecycleOwner,
                        childFragmentManager,
                        b.upvoteButton,
                        b.downvoteButton
                    ) { onVote ->
                        item.commentItem.likes = when {
                            onVote > 0 -> true
                            onVote < 0 -> false
                            else -> null
                        }
                        notifyItemChanged(h.adapterPosition)
                    }

                    h.itemView.tag = item
                }
                R.layout.post_comment_collapsed_item -> {
                    val h = holder as CommentCollapsedViewHolder
                    val item = items[position] as CommentItem

                    redditThreadLinesHelper.populateThreadLines(
                        h.threadLinesContainer, item.depth, item.baseDepth
                    )
                    redditHeaderHelper.populateHeaderSpan(
                        h.headerView, item.commentItem, detailed = true
                    )

                    h.expandSectionButton.setOnClickListener {
                        expandSection(h.adapterPosition)
                    }
                    h.topHotspot.setOnClickListener {
                        expandSection(h.adapterPosition)
                    }
                    if (item.commentItem.locked) {
                        h.overlay.visibility = View.VISIBLE
                        h.overlay.setBackgroundResource(R.drawable.locked_overlay)
                    } else {
                        h.overlay.visibility = View.GONE
                    }

                    h.itemView.tag = item
                }
                R.layout.post_more_comments_item -> {
                    val h = holder as MoreCommentsViewHolder
                    val item = items[position] as MoreCommentsItem

                    redditThreadLinesHelper.populateThreadLines(
                        h.threadLinesContainer, item.depth, item.baseDepth
                    )
                    h.moreButton.text = context.resources.getQuantityString(
                        R.plurals.replies_format, item.moreItem.count, item.moreItem.count
                    )

                    h.moreButton.setOnClickListener {
                        val url = LinkUtils.getLinkForMoreChildren(
                            item.linkId,
                            item.moreItem.children,
                            postViewModel.commentsSortOrder?.key
                        )
                        Log.d(TAG, "Url $url")

                        postViewModel.fetchMoreComments(url, item.moreItem.parentId)
                    }

                    h.itemView.tag = item
                }
                R.layout.generic_loading_item -> {
                    val h = holder as LoadingViewHolder
                    val error = error
                    if (error != null) {
                        h.loadingView.showDefaultErrorMessageFor(error)
                    } else if (!isLoaded) {
                        h.loadingView.showProgressBar()
                    }
                    h.loadingView.setOnRefreshClickListener {
                        onRefreshClickCb()
                    }
                }
                R.layout.compose_comment_item -> {
                    val h = holder as ComposeCommentViewHolder
                    val item = items[position] as ComposeCommentItem

                    val constraintSet = if (item.isPosting) {
                        h.postingConstraintSet
                    } else {
                        h.normalConstraintSet
                    }
                    constraintSet.applyTo(h.rootView)

                    bindComposeCommentView(h, item, item.isPosting)

                    h.addComment.setOnClickListener a@{
                        if (h.adapterPosition < 0) return@a

                        val autoTransition = AutoTransition()

                        val item = items[h.adapterPosition] as ComposeCommentItem
                        if (item.isPosting) return@a

                        item.isPosting = true

                        autoTransition.ordering = AutoTransition.ORDERING_TOGETHER
                        autoTransition.duration = 250
                        TransitionManager.beginDelayedTransition(h.rootView, autoTransition)
                        h.postingConstraintSet.applyTo(h.rootView)

                        notifyItemChanged(h.adapterPosition, Unit)

                        bindComposeCommentView(h, item, item.isPosting)

                        PendingActionsManager.instance.comment(
                            item.parentId,
                            item.comment.toString(),
                            this@PostFragment,
                            onRedditActionChangedCallback
                        )
                    }
                }
                R.layout.edit_comment_item -> {
                    val h = holder as EditCommentViewHolder
                    val item = items[position] as EditCommentItem

                    val constraintSet = if (item.isPosting) {
                        h.postingConstraintSet
                    } else {
                        h.normalConstraintSet
                    }
                    constraintSet.applyTo(h.rootView)

                    bindEditCommentView(h, item, item.isPosting)

                    h.saveEditsButton.setOnClickListener a@{
                        if (h.adapterPosition < 0) return@a

                        val autoTransition = AutoTransition()

                        val item = items[h.adapterPosition] as EditCommentItem
                        if (item.isPosting) return@a

                        item.isPosting = true

                        autoTransition.ordering = AutoTransition.ORDERING_TOGETHER
                        autoTransition.duration = 250
                        TransitionManager.beginDelayedTransition(h.rootView, autoTransition)
                        h.postingConstraintSet.applyTo(h.rootView)

                        notifyItemChanged(h.adapterPosition, Unit)

                        bindEditCommentView(h, item, item.isPosting)

                        PendingActionsManager.instance.edit(
                            item.commentName,
                            item.comment.toString(),
                            this@PostFragment,
                            onRedditActionChangedCallback
                        )
                    }
                }
            }
        }

        private fun bindComposeCommentView(
            h: ComposeCommentViewHolder,
            item: ComposeCommentItem,
            isPosting: Boolean
        ) {
            h.cancelButton.setOnClickListener {
                removeComposeCommentItemFor(item.parentId)
            }
            h.editText.setText(item.comment)

            if (h.boundTextWatcher != null) {
                h.editText.removeTextChangedListener(h.boundTextWatcher)
            }

            val textWatcher = object : TextWatcher {
                override fun afterTextChanged(s: Editable?) {}

                override fun beforeTextChanged(
                    s: CharSequence?,
                    start: Int,
                    count: Int,
                    after: Int
                ) {
                }

                override fun onTextChanged(
                    s: CharSequence?,
                    start: Int,
                    before: Int,
                    count: Int
                ) {
                    item.comment = s ?: ""
                }
            }
            h.boundTextWatcher = textWatcher
            h.editText.addTextChangedListener(textWatcher)

            val error = item.error
            if (isPosting) {
                h.loadingView.showProgressBarWithMessage(R.string.posting_comment)
            }

            h.itemView.tag = item
        }

        private fun removeComposeCommentItemFor(parentId: String) {
            composeCommentItems.remove(parentId)
            refreshItems(refreshHeader = false)
        }

        private fun setComposeCommentItemErrorFor(parentId: String, error: Throwable) {
            composeCommentItems[parentId]?.let {
                composeCommentItems[parentId] = it.copy(error = error, isPosting = false)
            }
            refreshItems(refreshHeader = false)
        }

        private fun bindEditCommentView(
            h: EditCommentViewHolder,
            item: EditCommentItem,
            isPosting: Boolean
        ) {
            h.cancelButton.setOnClickListener {
                removeEditCommentItemFor(item.commentName)
            }
            h.editText.setText(item.comment)

            if (h.boundTextWatcher != null) {
                h.editText.removeTextChangedListener(h.boundTextWatcher)
            }

            val textWatcher = object : TextWatcher {
                override fun afterTextChanged(s: Editable?) {}

                override fun beforeTextChanged(
                    s: CharSequence?,
                    start: Int,
                    count: Int,
                    after: Int
                ) {
                }

                override fun onTextChanged(
                    s: CharSequence?,
                    start: Int,
                    before: Int,
                    count: Int
                ) {
                    item.comment = s ?: ""
                }
            }
            h.boundTextWatcher = textWatcher
            h.editText.addTextChangedListener(textWatcher)

            val error = item.error
            if (isPosting) {
                h.loadingView.showProgressBarWithMessage(R.string.posting_comment)
            }

            h.itemView.tag = item
        }

        private fun removeEditCommentItemFor(commentName: String) {
            editCommentItems.remove(commentName)
            refreshItems(refreshHeader = false)
        }

        private fun setEditCommentItemErrorFor(commentName: String, error: Throwable) {
            editCommentItems[commentName]?.let {
                editCommentItems[commentName] = it.copy(error = error, isPosting = false)
            }
            refreshItems(refreshHeader = false)
        }

        override fun onViewRecycled(holder: RecyclerView.ViewHolder) {
            super.onViewRecycled(holder)

            val item = if (holder.adapterPosition >= 0) {
                items[holder.adapterPosition]
            } else {
                null
            }

            if (holder is HeaderViewHolder) {
                val state = redditContentHelper.recycleFullContent(holder.fullContentContainerView)

                (item as? HeaderItem)?.videoState = state.videoState
            } else if (holder is ComposeCommentViewHolder) {
                holder.editText.removeTextChangedListener(holder.boundTextWatcher)
                holder.boundTextWatcher = null
            }
        }

        override fun getItemCount(): Int = items.size

        private fun setupTransitionAnimation(titleTextView: TextView) {
            val transitionSet = TransitionSet()
            transitionSet.ordering = TransitionSet.ORDERING_TOGETHER
            transitionSet.addTransition(ChangeBounds())
            transitionSet.addTransition(ChangeTransform())
            transitionSet.addTransition(TextResize().addTarget(titleTextView))

            sharedElementEnterTransition = transitionSet
            sharedElementReturnTransition = transitionSet
        }

        private fun handleCommentClick(id: String) {
            val notSignedIn = RedditAuthManager.instance.showPreSignInIfNeeded(childFragmentManager)

            if (notSignedIn) return

            composeCommentItems[id] = ComposeCommentItem(
                id = "$id:comment",
                parentId = id,
                depth = 0,
                baseDepth = 0,
                comment = ""
            )
            refreshItems(refreshHeader = false)
        }

        private fun handleEditClick(commentItem: RedditCommentItem) {
            val notSignedIn = RedditAuthManager.instance.showPreSignInIfNeeded(childFragmentManager)

            if (notSignedIn) return

            editCommentItems[commentItem.name] = EditCommentItem(
                id = commentItem.name,
                commentName = commentItem.name,
                depth = 0,
                baseDepth = 0,
                comment = commentItem.body
            )
            refreshItems(refreshHeader = false)
        }

        /**
         * @param refreshHeader Pass false to not always refresh header. Useful for web view headers
         * as refreshing them might cause a relayout causing janky animations
         */
        private fun refreshItems(refreshHeader: Boolean = true) {
            val rawData = rawData ?: return
            val oldItems = items
            var mainListingItemSeen = false
            val newItems =
                if (error == null) {
                    val redditObjects = rawData.flattenPostData(moreCommentsMap)
                    val finalItems = arrayListOf<Item>()

                    redditObjects.forEach { redditObject ->
                        when (redditObject) {
                            is PostItem.ContentItem -> {
                                if (!mainListingItemSeen) {
                                    mainListingItemSeen = true
                                    onMainListingItemRetreived(redditObject.listingItem)
                                }

                                finalItems += HeaderItem(redditObject.listingItem, args.videoState)

                                composeCommentItems[redditObject.listingItem.name]?.let {
                                    finalItems += it
                                }
                            }
                            is PostItem.CommentItem -> {
                                editCommentItems[redditObject.commentItem.name]?.let {
                                    finalItems += it
                                } ?: run {
                                    finalItems += CommentItem(
                                        redditObject.commentItem,
                                        redditObject.depth,
                                        0,
                                        !redditObject.commentItem.collapsed,
                                        redditObject.isPending
                                    )
                                }

                                composeCommentItems[redditObject.commentItem.name]?.let {
                                    finalItems += it
                                }
                            }
                            is PostItem.MoreCommentsItem -> {
                                finalItems += MoreCommentsItem(
                                    redditObject.moreItem,
                                    redditObject.depth,
                                    0,
                                    redditObject.linkId
                                )
                            }
                        }
                    }

                    if (!isLoaded) {
                        finalItems += listOf(ProgressItem())
                    }

                    finalItems
                } else {
                    listOf(ProgressItem())
                }

            val diff = DiffUtil.calculateDiff(object : DiffUtil.Callback() {
                override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                    return oldItems[oldItemPosition].id == newItems[newItemPosition].id
                }

                override fun getOldListSize(): Int = oldItems.size

                override fun getNewListSize(): Int = newItems.size

                override fun areContentsTheSame(
                    oldItemPosition: Int,
                    newItemPosition: Int
                ): Boolean {
                    val oldItem = oldItems[oldItemPosition]
                    val newItem = newItems[newItemPosition]
                    return oldItem.type == newItem.type && areItemsTheSame(
                        oldItemPosition,
                        newItemPosition
                    )
                            && when (oldItem) {
                        is ComposeCommentItem -> {
                            oldItem.isPosting == (newItem as ComposeCommentItem).isPosting
                        }
                        is EditCommentItem -> {
                            oldItem.isPosting == (newItem as EditCommentItem).isPosting
                        }
                        else -> true
                    }
                }

            })
            this.items = newItems
            diff.dispatchUpdatesTo(this)
            if (refreshHeader) {
                notifyItemChanged(0, Unit)
            }
        }

        fun setStartingData(data: List<RedditObject>) {
            rawData = data

            refreshItems()
        }

        fun setData(data: List<RedditObject>) {
            if (!isLoaded) {
                isLoaded = true
            }

            error = null
            rawData = data

            refreshItems()
        }

        private fun collapseSection(position: Int) {
            if (position < 0) return

            val commentItem = (items[position] as CommentItem).commentItem
            commentItem.collapsed = true

            refreshItems(refreshHeader = false)
        }

        private fun expandSection(position: Int) {
            if (position < 0) return

            val commentItem = (items[position] as CommentItem).commentItem
            commentItem.collapsed = false

            refreshItems(refreshHeader = false)
        }

        fun setMoreCommentData(moreCommentMap: HashMap<String, List<CommentItemObject>>?) {
            this.moreCommentsMap = moreCommentMap ?: return

            refreshItems()
        }

        fun setError(error: Throwable) {
            this.error = error

            refreshItems()
        }

    }
}
