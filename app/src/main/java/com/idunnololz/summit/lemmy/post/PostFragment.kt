package com.idunnololz.summit.lemmy.post

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.text.style.StyleSpan
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.widget.PopupMenu
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.content.ContextCompat
import androidx.core.text.buildSpannedString
import androidx.core.view.MenuProvider
import androidx.core.view.ViewCompat
import androidx.core.view.doOnPreDraw
import androidx.fragment.app.viewModels
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
import com.idunnololz.summit.BuildConfig
import com.idunnololz.summit.R
import com.idunnololz.summit.account_ui.PreAuthDialogFragment
import com.idunnololz.summit.account_ui.SignInNavigator
import com.idunnololz.summit.alert.AlertDialogFragment
import com.idunnololz.summit.api.dto.CommentView
import com.idunnololz.summit.api.dto.PostView
import com.idunnololz.summit.databinding.FragmentPostBinding
import com.idunnololz.summit.databinding.PostCommentExpandedItemBinding
import com.idunnololz.summit.history.HistoryManager
import com.idunnololz.summit.history.HistorySaveReason
import com.idunnololz.summit.lemmy.LemmyContentHelper
import com.idunnololz.summit.lemmy.LemmyHeaderHelper
import com.idunnololz.summit.lemmy.utils.getFormattedAuthor
import com.idunnololz.summit.lemmy.utils.getFormattedTitle
import com.idunnololz.summit.lemmy.utils.getUpvoteText
import com.idunnololz.summit.offline.OfflineManager
import com.idunnololz.summit.lemmy.post.PostFragment.Item.*
import com.idunnololz.summit.lemmy.utils.VoteUiHandler
import com.idunnololz.summit.lemmy.utils.bind
import com.idunnololz.summit.reddit.*
import com.idunnololz.summit.util.*
import com.idunnololz.summit.util.ext.addDefaultAnim
import com.idunnololz.summit.util.ext.navigateSafe
import com.idunnololz.summit.video.ExoPlayerManager
import com.idunnololz.summit.video.VideoState
import com.idunnololz.summit.view.LoadingView
import com.idunnololz.summit.view.RedditHeaderView
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class PostFragment : BaseFragment<FragmentPostBinding>(),
    AlertDialogFragment.AlertDialogFragmentListener,
    SignInNavigator {
    companion object {
        private val TAG = PostFragment::class.java.canonicalName

        private const val CONFIRM_DELETE_COMMENT_TAG = "CONFIRM_DELETE_COMMENT_TAG"
        private const val EXTRA_COMMENT_ID = "EXTRA_COMMENT_ID"
    }

    private val args: PostFragmentArgs by navArgs()

    private val viewModel: PostViewModel by viewModels()

    private lateinit var adapter: PostsAdapter

    private var offlineManager = OfflineManager.instance

    private var hasConsumedJumpToComments: Boolean = false

    @Inject
    lateinit var historyManager: HistoryManager

    private val _sortByMenu: BottomMenu by lazy {
        BottomMenu(requireContext()).apply {
            addItem(R.id.sort_order_hot, R.string.sort_order_hot)
            addItem(R.id.sort_order_top, R.string.sort_order_top)
            addItem(R.id.sort_order_new, R.string.sort_order_new)
            addItem(R.id.sort_order_old, R.string.sort_order_old)
            setTitle(R.string.sort_by)

            setOnMenuItemClickListener { menuItem ->
                when(menuItem.id) {
                    R.id.sort_order_hot ->
                        viewModel.setCommentsSortOrder(CommentsSortOrder.Hot)
                    R.id.sort_order_top ->
                        viewModel.setCommentsSortOrder(CommentsSortOrder.Top)
                    R.id.sort_order_new ->
                        viewModel.setCommentsSortOrder(CommentsSortOrder.New)
                    R.id.sort_order_old ->
                        viewModel.setCommentsSortOrder(CommentsSortOrder.Old)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val context = requireContext()

        postponeEnterTransition()

        adapter = PostsAdapter(
            context,
            args.instance,
            args.reveal,
            viewModel.voteUiHandler,
            onRefreshClickCb = {
                forceRefresh()
            },
            onSignInRequired = {
                PreAuthDialogFragment.newInstance()
                    .show(childFragmentManager, "asdf")
            },
            onInstanceMismatch = { accountInstance, apiInstance ->
                AlertDialogFragment.Builder()
                    .setTitle(R.string.error_account_instance_mismatch_title)
                    .setMessage(
                        getString(R.string.error_account_instance_mismatch,
                            accountInstance,
                            apiInstance)
                    )
                    .createAndShow(childFragmentManager, "aa")
            },
        )

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

        val context = requireContext()

        requireMainActivity().apply {
            insetViewExceptTopAutomaticallyByPadding(viewLifecycleOwner, binding.recyclerView)
            insetViewExceptBottomAutomaticallyByMargins(viewLifecycleOwner, binding.toolbar)

            setupActionBar(
                "",
                showUp = false,
                animateActionBarIn = false,
            )

            setSupportActionBar(binding.toolbar)

            supportActionBar?.setDisplayShowHomeEnabled(true)
            supportActionBar?.setDisplayHomeAsUpEnabled(true)
            supportActionBar?.title =
                viewModel.commentsSortOrderLiveData.value?.getLocalizedName(context) ?: ""
        }

        val swipeRefreshLayout = binding.swipeRefreshLayout

        swipeRefreshLayout.setOnRefreshListener {
            forceRefresh()
        }
        binding.loadingView.setOnRefreshClickListener {
            forceRefresh()
        }

        args.post?.let { post ->
            adapter.setStartingData(
                PostViewModel.PostData(
                    PostViewModel.ListView.PostListView(post),
                    listOf(),
            ))
            onMainListingItemRetrieved(post)
        } ?: binding.loadingView.showProgressBar()

        viewModel.postData.observe(viewLifecycleOwner) {
            when (it) {
                is StatefulData.Error -> {
                    binding.swipeRefreshLayout.isRefreshing = false
                    binding.loadingView.hideAll()
                    adapter.setError(it.error)
                }
                is StatefulData.Loading -> {
                    if (!adapter.hasStartingData()) {
                        binding.loadingView.showProgressBar()
                    }
                }
                is StatefulData.NotStarted -> {}
                is StatefulData.Success -> {
                    binding.swipeRefreshLayout.isRefreshing = false
                    binding.loadingView.hideAll()
                    adapter.setData(it.data)
                }
            }
        }

        viewModel.fetchPostData(args.instance, args.id)

        args.post?.getUrl()?.let { url ->
            historyManager.recordVisit(
                jsonUrl = url,
                saveReason = HistorySaveReason.LOADING,
                post = args.post
            )
        }

        binding.recyclerView.adapter = adapter
        binding.recyclerView.setHasFixedSize(true)
        binding.recyclerView.layoutManager = LinearLayoutManager(context)
        binding.recyclerView.addItemDecoration(ThreadLinesDecoration(context))
        binding.recyclerView.addItemDecoration(PostDividerDecoration(context))
        binding.fastScroller.setRecyclerView(binding.recyclerView)

        if (!hasConsumedJumpToComments && args.jumpToComments) {
            (binding.recyclerView.layoutManager as LinearLayoutManager)
                .scrollToPositionWithOffset(1, 0)
        }

        binding.recyclerView.post {
            startPostponedEnterTransition()
        }

        viewModel.commentsSortOrderLiveData.observe(viewLifecycleOwner) {
            getMainActivity()?.supportActionBar?.title =
                viewModel.commentsSortOrderLiveData.value?.getLocalizedName(context) ?: ""
        }

        binding.root.doOnPreDraw {
            adapter.contentMaxWidth = binding.recyclerView.width
        }

        addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.menu_fragment_post, menu)
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean =
                when (menuItem.itemId) {
                    R.id.share -> {
                        val sendIntent: Intent = Intent().apply {
                            action = Intent.ACTION_SEND
                            putExtra(
                                Intent.EXTRA_TEXT,
                                LinkUtils.postIdToLink(viewModel.instance, args.id)
                            )
                            type = "text/plain"
                        }

                        val shareIntent = Intent.createChooser(sendIntent, null)
                        startActivity(shareIntent)
                        true
                    }

                    R.id.sort_comments_by -> {
                        getMainActivity()?.showBottomMenu(getSortByMenu())
                        true
                    }
                    else -> false
                }

        })
    }

    override fun navigateToSignInScreen() {
        val direction = PostFragmentDirections.actionPostFragmentToLogin()
        findNavController().navigateSafe(direction)
    }

    private fun forceRefresh() {
        viewModel.fetchPostData(args.instance, args.id, force = true)
    }

    fun onMainListingItemRetrieved(post: PostView) {
        post.getUrl()?.let { url ->
            historyManager.recordVisit(
                jsonUrl = url,
                saveReason = HistorySaveReason.LOADED,
                post = post,
            )
        }
    }

    private fun getSortByMenu(): BottomMenu {
        when (viewModel.commentsSortOrderLiveData.value) {
            CommentsSortOrder.Hot -> _sortByMenu.setChecked(R.id.sort_order_hot)
            CommentsSortOrder.Top -> _sortByMenu.setChecked(R.id.sort_order_top)
            CommentsSortOrder.New -> _sortByMenu.setChecked(R.id.sort_order_new)
            CommentsSortOrder.Old -> _sortByMenu.setChecked(R.id.sort_order_old)
            else -> {}
        }

        return _sortByMenu
    }

    override fun onPositiveClick(dialog: AlertDialogFragment, tag: String?) {
        when (tag) {
            CONFIRM_DELETE_COMMENT_TAG -> {
                val commentId = dialog.getExtra(EXTRA_COMMENT_ID)
                if (commentId != null) {
                    viewModel.deleteComment(commentId)
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
                val totalDepth = when (tag) {
                    is CommentItem -> {
                        tag.depth - tag.baseDepth
                    }
                    else -> {
                        -1
                    }
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
            val postView: PostView,
            var videoState: VideoState?
        ) : Item(TYPE_LISTING_ITEM, postView.getUniqueKey())

        data class CommentItem(
            val comment: CommentView,
            val depth: Int,
            val baseDepth: Int,
            val isExpanded: Boolean,
            val isPending: Boolean,
            val view: PostViewModel.ListView.CommentListView,
            val childrenCount: Int,
        ) : Item(
            if (isExpanded) TYPE_COMMENT_EXPANDED_ITEM else TYPE_COMMENT_COLLAPSED_ITEM,
            "comment_${comment.comment.id}"
        )

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

        class ProgressOrErrorItem(
            val error: Throwable? = null
        ) : Item(TYPE_PROGRESS, "wew_pls_no_progress")
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

    private inner class PostsAdapter(
        private val context: Context,
        private val instance: String,
        private val revealAll: Boolean,
        private val voteUiHandler: VoteUiHandler,
        private val onRefreshClickCb: () -> Unit,
        private val onSignInRequired: () -> Unit,
        private val onInstanceMismatch: (String, String) -> Unit,
    ) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

        private val inflater = LayoutInflater.from(context)

        private var items: List<Item> = listOf()

        private val lemmyHeaderHelper = LemmyHeaderHelper(context)
        private val lemmyContentHelper = LemmyContentHelper(
            context, this@PostFragment, offlineManager, ExoPlayerManager.get(this@PostFragment)
        )
        private val threadLinesHelper = ThreadLinesHelper(context)

        private var parentHeight: Int = 0

        /**
         * Set of items that is hidden by default but is reveals (ie. nsfw or spoiler tagged)
         */
        private var revealedItems = mutableSetOf<String>()
        private var composeCommentItems: HashMap<String, ComposeCommentItem> = hashMapOf()
        private var editCommentItems: HashMap<String, EditCommentItem> = hashMapOf()

        private var rawData: PostViewModel.PostData? = null

        private var tempSize = Size()

        var isFirstLoad: Boolean = true
        var isLoaded: Boolean = false
        private var error: Throwable? = null

        var contentMaxWidth = 0
            set(value) {
                if (value == field) {
                    return
                }
                field = value

                for (i in 0.. items.lastIndex) {
                    if (items[i] is Item.HeaderItem) {
                        notifyItemChanged(i)
                    }
                }
            }

//        val onRedditActionChangedCallback: (StatefulData<RedditAction>) -> Unit = {
//            if (it is StatefulData.Success) {
//                val actionInfo = it.data.info
//                if (actionInfo is ActionInfo.CommentActionInfo) {
//                    removeComposeCommentItemFor(actionInfo.parentId)
//                } else if (actionInfo is ActionInfo.EditActionInfo) {
//                    removeEditCommentItemFor(actionInfo.thingId)
//                }
//            } else if (it is StatefulData.Error) {
//                val error = it.error as PendingActionsManager.PendingActionsException
//                val actionInfo = error
//                if (actionInfo is ActionInfo.CommentActionInfo) {
//                    setComposeCommentItemErrorFor(actionInfo.parentId, error = error.cause)
//
//                    Snackbar.make(
//                        requireMainActivity().getSnackbarContainer(),
//                        R.string.error_post_comment_failed,
//                        Snackbar.LENGTH_LONG
//                    ).show()
//                } else if (actionInfo is ActionInfo.EditActionInfo) {
//                    setEditCommentItemErrorFor(actionInfo.thingId, error = error.cause)
//
//                    Snackbar.make(
//                        requireMainActivity().getSnackbarContainer(),
//                        R.string.error_edit_comment_failed,
//                        Snackbar.LENGTH_LONG
//                    ).show()
//                }
//            }
//        }

        override fun getItemViewType(position: Int): Int = when (val item = items[position]) {
            is HeaderItem -> R.layout.post_header_item
            is CommentItem ->
                if (item.isExpanded)
                    R.layout.post_comment_expanded_item
                else
                    R.layout.post_comment_collapsed_item
            is ProgressOrErrorItem -> R.layout.generic_loading_item
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
                        val post = item.postView
                        val postKey = post.getUniqueKey()

                        lemmyHeaderHelper.populateHeaderSpan(
                            h.headerContainer, post, listAuthor = false
                        )

                        h.titleTextView.text = post.getFormattedTitle()
                        h.authorTextView.text = post.getFormattedAuthor()

                        h.commentButton.text = abbrevNumber(item.postView.counts.comments.toLong())
                        h.upvoteCount.text = post.getUpvoteText()
                        h.commentButton.isEnabled = !post.post.locked
                        h.commentButton.setOnClickListener {
                            handleCommentClick(postKey)
                        }

                        lemmyContentHelper.setupFullContent(
                            reveal = revealAll || revealedItems.contains(postKey),
                            tempSize = tempSize,
                            videoViewMaxHeight = (binding.recyclerView.height - Utils.convertDpToPixel(16f)).toInt(),
                            contentMaxWidth = contentMaxWidth,
                            fullImageViewTransitionName = "post_image",
                            postView = post,
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
                            onImageClickListener = { url ->
                                val action =
                                    PostFragmentDirections.actionPostFragmentToImageViewerFragment(
                                        title = null,
                                        url = url,
                                        mimeType = null
                                    )
                                findNavController().navigate(
                                    action, NavOptions.Builder()
                                        .addDefaultAnim()
                                        .build()
                                )
                            },
                            onRevealContentClickedFn = {
                                revealedItems.add(postKey)
                                notifyItemChanged(h.adapterPosition)
                            },
                            lazyUpdate = true,
                            onItemClickListener = {},
                            videoState = item.videoState
                        )

                        voteUiHandler.bind(
                            instance,
                            item.postView,
                            h.upvoteButton,
                            h.downvoteButton,
                            onSignInRequired,
                            onInstanceMismatch,
                        )
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
                    val post = item.postView
                    val postKey = post.getUniqueKey()

                    setupTransitionAnimation(h.titleTextView)

                    ViewCompat.setTransitionName(h.titleTextView, "title")

                    lemmyHeaderHelper.populateHeaderSpan(
                        h.headerContainer, item.postView, listAuthor = false
                    )

                    h.titleTextView.text = post.getFormattedTitle()
                    h.authorTextView.text = post.getFormattedAuthor()

                    h.commentButton.text = abbrevNumber(item.postView.counts.comments.toLong())
                    h.upvoteCount.text = post.getUpvoteText()
                    h.commentButton.isEnabled = !post.post.locked
                    h.commentButton.setOnClickListener {
                        handleCommentClick(postKey)
                    }

                    lemmyContentHelper.setupFullContent(
                        reveal = revealAll || revealedItems.contains(postKey),
                        tempSize = tempSize,
                        videoViewMaxHeight = (binding.recyclerView.height - Utils.convertDpToPixel(16f)).toInt(),
                        contentMaxWidth = contentMaxWidth,
                        fullImageViewTransitionName = "post_image",
                        postView = post,
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
                        onImageClickListener = { url ->
                            val action =
                                PostFragmentDirections.actionPostFragmentToImageViewerFragment(
                                    title = null,
                                    url = url,
                                    mimeType = null
                                )
                            findNavController().navigate(
                                action, NavOptions.Builder()
                                    .addDefaultAnim()
                                    .build()
                            )
                        },
                        onRevealContentClickedFn = {
                            revealedItems.add(postKey)
                            notifyItemChanged(h.absoluteAdapterPosition)
                        },
                        onItemClickListener = {},
                        videoState = item.videoState
                    )

                    Log.d(TAG, "header vote state: ${item.postView.counts.upvotes}")

                    voteUiHandler.bind(
                        instance,
                        item.postView,
                        h.upvoteButton,
                        h.downvoteButton,
                        onSignInRequired,
                        onInstanceMismatch,
                    )

                    h.itemView.tag = item
                }
                R.layout.post_comment_expanded_item -> {
                    val h = holder as CommentExpandedViewHolder
                    val b = h.binding
                    val item = items[position] as CommentItem
                    val comment = item.comment
                    val body = comment.comment.content

                    threadLinesHelper.populateThreadLines(
                        b.threadLinesContainer, item.depth, item.baseDepth
                    )
                    lemmyHeaderHelper.populateHeaderSpan(b.headerContainer, item.comment)

                    if (comment.comment.deleted) {
                        b.text.text = buildSpannedString {
                            append(context.getString(R.string.deleted))
                            setSpan(StyleSpan(Typeface.ITALIC), 0, length, 0);
                        }
                    } else if (comment.comment.removed) {
                        b.text.text = buildSpannedString {
                            append(context.getString(R.string.removed))
                            setSpan(StyleSpan(Typeface.ITALIC), 0, length, 0);
                        }
                    } else {
                        RedditUtils.bindRedditText(b.text, body)
                    }

                    b.text.movementMethod = CustomLinkMovementMethod().apply {
                        onLinkLongClickListener = DefaultLinkLongClickListener(context)
                        onImageClickListener = { url ->
                            val action =
                                PostFragmentDirections.actionPostFragmentToImageViewerFragment(
                                    title = null,
                                    url = url,
                                    mimeType = null
                                )
                            findNavController().navigate(
                                action, NavOptions.Builder()
                                    .addDefaultAnim()
                                    .build()
                            )
                        }
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

                    b.upvoteCount.text = item.comment.getUpvoteText()

                    b.collapseSectionButton.setOnClickListener {
                        collapseSection(h.bindingAdapterPosition)
                    }
                    b.topHotspot.setOnClickListener {
                        collapseSection(h.bindingAdapterPosition)
                    }
                    b.commentButton.setOnClickListener {


                        AlertDialogFragment.Builder()
                            .setMessage(R.string.coming_soon)
                            .setPositiveButton(android.R.string.ok)
                            .createAndShow(
                                childFragmentManager,
                                CONFIRM_DELETE_COMMENT_TAG
                            )
//                        handleCommentClick(item.comment.getUniqueKey())
                    }
                    b.moreButton.setOnClickListener {

                        PopupMenu(context, b.moreButton).apply {
                            inflate(R.menu.menu_comment_item)

                            if (BuildConfig.DEBUG) {
                                menu.findItem(R.id.raw_comment).isVisible = true
                            }

//                            if (item.comment.creator.id) {
//                                menu.setGroupVisible(R.id.mod_post_actions, false)
//                                //menu.findItem(R.id.edit_comment).isVisible = false
//                            }

                            setOnMenuItemClickListener {
                                when (it.itemId) {
                                    R.id.raw_comment -> {
                                        val action =
                                            PostFragmentDirections.actionPostFragmentToCommentRawDialogFragment(
                                                commentItemStr = Utils.gson.toJson(item.comment)
                                            )
                                        findNavController().navigate(action)
                                    }
                                    R.id.edit_comment -> {

                                        AlertDialogFragment.Builder()
                                            .setMessage(R.string.coming_soon)
                                            .setPositiveButton(android.R.string.ok)
                                            .createAndShow(
                                                childFragmentManager,
                                                CONFIRM_DELETE_COMMENT_TAG
                                            )

//                                        handleEditClick(item.comment)
                                    }
                                    R.id.delete_comment -> {
//                                        AlertDialogFragment.Builder()
//                                            .setMessage(R.string.delete_comment_confirm)
//                                            .setPositiveButton(android.R.string.yes)
//                                            .setNegativeButton(android.R.string.no)
//                                            .setExtra(EXTRA_COMMENT_ID, item.comment.comment.id.toString())
//                                            .createAndShow(
//                                                childFragmentManager,
//                                                CONFIRM_DELETE_COMMENT_TAG
//                                            )


                                        AlertDialogFragment.Builder()
                                            .setMessage(R.string.coming_soon)
                                            .setPositiveButton(android.R.string.ok)
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
                    if (item.comment.comment.distinguished) {
                        b.overlay.visibility = View.VISIBLE
                        b.overlay.setBackgroundResource(R.drawable.locked_overlay)
                    } else {
                        b.overlay.visibility = View.GONE
                    }

                    voteUiHandler.bind(
                        instance,
                        item.comment,
                        b.upvoteButton,
                        b.downvoteButton,
                        onSignInRequired,
                        onInstanceMismatch,
                    )

                    h.itemView.tag = item
                }
                R.layout.post_comment_collapsed_item -> {
                    val h = holder as CommentCollapsedViewHolder
                    val item = items[position] as CommentItem

                    threadLinesHelper.populateThreadLines(
                        h.threadLinesContainer, item.depth, item.baseDepth
                    )
                    lemmyHeaderHelper.populateHeaderSpan(
                        headerContainer = h.headerView,
                        item = item.comment,
                        detailed = true,
                        childrenCount = item.childrenCount
                    )

                    h.expandSectionButton.setOnClickListener {
                        expandSection(h.absoluteAdapterPosition)
                    }
                    h.topHotspot.setOnClickListener {
                        expandSection(h.absoluteAdapterPosition)
                    }
                    if (item.comment.comment.distinguished) {
                        h.overlay.visibility = View.VISIBLE
                        h.overlay.setBackgroundResource(R.drawable.locked_overlay)
                    } else {
                        h.overlay.visibility = View.GONE
                    }

                    h.itemView.tag = item
                }
                R.layout.generic_loading_item -> {
                    val h = holder as LoadingViewHolder
                    val item = items[position] as ProgressOrErrorItem

                    if (item.error != null) {
                        h.loadingView.showDefaultErrorMessageFor(item.error)
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
                        if (h.absoluteAdapterPosition < 0) return@a

                        val autoTransition = AutoTransition()

                        val item = items[h.absoluteAdapterPosition] as ComposeCommentItem
                        if (item.isPosting) return@a

                        item.isPosting = true

                        autoTransition.ordering = AutoTransition.ORDERING_TOGETHER
                        autoTransition.duration = 250
                        TransitionManager.beginDelayedTransition(h.rootView, autoTransition)
                        h.postingConstraintSet.applyTo(h.rootView)

                        notifyItemChanged(h.adapterPosition, Unit)

                        bindComposeCommentView(h, item, item.isPosting)

//                        PendingActionsManager.instance.comment(
//                            item.parentId,
//                            item.comment.toString(),
//                            this@PostFragment,
//                            onRedditActionChangedCallback
//                        )
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
                        if (h.absoluteAdapterPosition < 0) return@a

                        val autoTransition = AutoTransition()

                        val item = items[h.absoluteAdapterPosition] as EditCommentItem
                        if (item.isPosting) return@a

                        item.isPosting = true

                        autoTransition.ordering = AutoTransition.ORDERING_TOGETHER
                        autoTransition.duration = 250
                        TransitionManager.beginDelayedTransition(h.rootView, autoTransition)
                        h.postingConstraintSet.applyTo(h.rootView)

                        notifyItemChanged(h.adapterPosition, Unit)

                        bindEditCommentView(h, item, item.isPosting)

//                        PendingActionsManager.instance.edit(
//                            item.commentName,
//                            item.comment.toString(),
//                            this@PostFragment,
//                            onRedditActionChangedCallback
//                        )
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

            val item = if (holder.absoluteAdapterPosition >= 0) {
                items[holder.absoluteAdapterPosition]
            } else {
                null
            }

            if (holder is HeaderViewHolder) {
                val state = lemmyContentHelper.recycleFullContent(holder.fullContentContainerView)

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
//            val notSignedIn = RedditAuthManager.instance.showPreSignInIfNeeded(childFragmentManager)
//
//            if (notSignedIn) return
//
//            composeCommentItems[id] = ComposeCommentItem(
//                id = "$id:comment",
//                parentId = id,
//                depth = 0,
//                baseDepth = 0,
//                comment = ""
//            )
//            refreshItems(refreshHeader = false)
        }

        private fun handleEditClick(commentItem: CommentView) {
//            val notSignedIn = RedditAuthManager.instance.showPreSignInIfNeeded(childFragmentManager)
//
//            if (notSignedIn) return
//
//            editCommentItems[commentItem.getUniqueKey()] = EditCommentItem(
//                id = commentItem.getUniqueKey(),
//                commentName = commentItem.getUniqueKey(),
//                depth = 0,
//                baseDepth = 0,
//                comment = commentItem.comment.content
//            )
//            refreshItems(refreshHeader = false)
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
                    val finalItems = mutableListOf<Item>()

                    rawData.postView.let {
                        if (!mainListingItemSeen) {
                            mainListingItemSeen = true
                            onMainListingItemRetrieved(it.post)
                        }

                        finalItems += HeaderItem(it.post, args.videoState)

                        composeCommentItems[it.post.getUniqueKey()]?.let {
                            finalItems += it
                        }
                    }

                    rawData.commentTree.flatten().forEach {
                        finalItems += CommentItem(
                            it.commentView.comment,
                            it.commentView.comment.getDepth(),
                            0,
                            !it.commentView.isCollapsed,
                            false,
                            view = it.commentView,
                            it.children?.size ?: 0,
                        )
                    }


//
//                    redditObjects.forEach { redditObject ->
//                        when (redditObject) {
//                            is PostItem.ContentItem -> {
//                                if (!mainListingItemSeen) {
//                                    mainListingItemSeen = true
//                                    onMainListingItemRetreived(redditObject.listingItem)
//                                }
//
//                                finalItems += HeaderItem(redditObject.listingItem, args.videoState)
//
//                                composeCommentItems[redditObject.listingItem.name]?.let {
//                                    finalItems += it
//                                }
//                            }
//                            is PostItem.CommentItem -> {
//                                editCommentItems[redditObject.commentItem.name]?.let {
//                                    finalItems += it
//                                } ?: run {
//                                    finalItems += CommentItem(
//                                        redditObject.commentItem,
//                                        redditObject.depth,
//                                        0,
//                                        !redditObject.commentItem.collapsed,
//                                        redditObject.isPending
//                                    )
//                                }
//
//                                composeCommentItems[redditObject.commentItem.name]?.let {
//                                    finalItems += it
//                                }
//                            }
//                            is PostItem.MoreCommentsItem -> {
//                                finalItems += MoreCommentsItem(
//                                    redditObject.moreItem,
//                                    redditObject.depth,
//                                    0,
//                                    redditObject.linkId
//                                )
//                            }
//                        }
//                    }

                    if (!isLoaded) {
                        finalItems += listOf(ProgressOrErrorItem())
                    }

                    finalItems
                } else {
                    listOf(ProgressOrErrorItem(error))
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
                    return when (oldItem) {
                        is ComposeCommentItem -> {
                            oldItem.isPosting == (newItem as ComposeCommentItem).isPosting
                        }
                        is EditCommentItem -> {
                            oldItem.isPosting == (newItem as EditCommentItem).isPosting
                        }

                        is HeaderItem -> true
                        else -> oldItem == newItem
                    }
                }

            })
            this.items = newItems
            diff.dispatchUpdatesTo(this)
            if (refreshHeader) {
                notifyItemChanged(0, Unit)
            }
        }

        fun setStartingData(data: PostViewModel.PostData) {
            rawData = data

            refreshItems()
        }

        fun hasStartingData(): Boolean = rawData?.postView != null

        fun setData(data: PostViewModel.PostData) {
            if (!isLoaded) {
                isLoaded = true
            }

            error = null
            rawData = data

            refreshItems()
        }

        private fun collapseSection(position: Int) {
            if (position < 0) return

            val commentItem = (items[position] as CommentItem).view
            commentItem.isCollapsed = true

            refreshItems(refreshHeader = false)
        }

        private fun expandSection(position: Int) {
            if (position < 0) return

            val commentItem = (items[position] as CommentItem).view
            commentItem.isCollapsed = false

            refreshItems(refreshHeader = false)
        }

        fun setError(error: Throwable) {
            this.error = error

            refreshItems()
        }

    }
}
