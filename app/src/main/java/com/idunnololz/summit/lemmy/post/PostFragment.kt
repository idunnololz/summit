package com.idunnololz.summit.lemmy.post

import android.content.Context
import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.text.style.StyleSpan
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver.OnPreDrawListener
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.widget.*
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.widget.PopupMenu
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.text.buildSpannedString
import androidx.core.view.MenuProvider
import androidx.core.view.ViewCompat
import androidx.core.view.doOnPreDraw
import androidx.fragment.app.setFragmentResultListener
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.transition.*
import arrow.core.Either
import com.facebook.drawee.backends.pipeline.Fresco
import com.facebook.drawee.drawable.ScalingUtils
import com.facebook.drawee.interfaces.DraweeController
import com.facebook.drawee.view.SimpleDraweeView
import com.idunnololz.summit.BuildConfig
import com.idunnololz.summit.R
import com.idunnololz.summit.account.AccountManager
import com.idunnololz.summit.account_ui.PreAuthDialogFragment
import com.idunnololz.summit.account_ui.SignInNavigator
import com.idunnololz.summit.alert.AlertDialogFragment
import com.idunnololz.summit.api.dto.CommentId
import com.idunnololz.summit.api.dto.CommentView
import com.idunnololz.summit.api.dto.PostView
import com.idunnololz.summit.api.utils.getDepth
import com.idunnololz.summit.api.utils.getUniqueKey
import com.idunnololz.summit.api.utils.getUrl
import com.idunnololz.summit.databinding.FragmentPostBinding
import com.idunnololz.summit.databinding.GenericFooterItemBinding
import com.idunnololz.summit.databinding.GenericLoadingItemBinding
import com.idunnololz.summit.databinding.PostCommentCollapsedItemBinding
import com.idunnololz.summit.databinding.PostCommentExpandedItemBinding
import com.idunnololz.summit.databinding.PostHeaderItemBinding
import com.idunnololz.summit.databinding.PostMoreCommentsItemBinding
import com.idunnololz.summit.databinding.PostPendingCommentCollapsedItemBinding
import com.idunnololz.summit.databinding.PostPendingCommentExpandedItemBinding
import com.idunnololz.summit.history.HistoryManager
import com.idunnololz.summit.history.HistorySaveReason
import com.idunnololz.summit.lemmy.LemmyContentHelper
import com.idunnololz.summit.lemmy.LemmyHeaderHelper
import com.idunnololz.summit.lemmy.LemmyTextHelper
import com.idunnololz.summit.lemmy.PageRef
import com.idunnololz.summit.lemmy.PostRef
import com.idunnololz.summit.lemmy.comment.AddOrEditCommentFragment
import com.idunnololz.summit.lemmy.comment.AddOrEditCommentFragmentArgs
import com.idunnololz.summit.lemmy.community.CommunityFragment
import com.idunnololz.summit.lemmy.createOrEditPost.CreateOrEditPostFragment
import com.idunnololz.summit.lemmy.createOrEditPost.CreateOrEditPostFragmentArgs
import com.idunnololz.summit.lemmy.utils.getFormattedAuthor
import com.idunnololz.summit.lemmy.utils.getFormattedTitle
import com.idunnololz.summit.offline.OfflineManager
import com.idunnololz.summit.lemmy.post.PostFragment.Item.*
import com.idunnololz.summit.lemmy.post.PostViewModel.Companion.HIGHLIGHT_COMMENT_MS
import com.idunnololz.summit.lemmy.utils.VoteUiHandler
import com.idunnololz.summit.lemmy.utils.bind
import com.idunnololz.summit.main.MainFragment
import com.idunnololz.summit.reddit.*
import com.idunnololz.summit.util.*
import com.idunnololz.summit.util.ext.addDefaultAnim
import com.idunnololz.summit.util.ext.getColorCompat
import com.idunnololz.summit.util.ext.navigateSafe
import com.idunnololz.summit.util.ext.showAllowingStateLoss
import com.idunnololz.summit.util.recyclerView.ViewBindingViewHolder
import com.idunnololz.summit.util.recyclerView.getBinding
import com.idunnololz.summit.util.recyclerView.isBinding
import com.idunnololz.summit.video.ExoPlayerManager
import com.idunnololz.summit.video.VideoState
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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

    private var hasConsumedJumpToComments: Boolean = false

    @Inject
    lateinit var historyManager: HistoryManager

    @Inject
    lateinit var offlineManager: OfflineManager

    @Inject
    lateinit var accountManager: AccountManager

    private var mainListingItemSeen = false

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
            onAddCommentClick = { postOrComment ->
                if (accountManager.currentAccount.value == null) {
                    PreAuthDialogFragment.newInstance()
                        .show(childFragmentManager, "asdf")
                    return@PostsAdapter
                }

                AddOrEditCommentFragment().apply {
                    arguments = postOrComment.fold({
                        AddOrEditCommentFragmentArgs(
                            args.instance, null, it, null)
                    }, {
                        AddOrEditCommentFragmentArgs(
                            args.instance, it, null, null)
                    }).toBundle()
                }.show(childFragmentManager, "asdf")
            },
            onEditCommentClick = {
                if (accountManager.currentAccount.value == null) {
                    PreAuthDialogFragment.newInstance()
                        .show(childFragmentManager, "asdf")
                    return@PostsAdapter
                }

                val directions =
                    PostFragmentDirections
                        .actionPostFragmentToAddOrEditCommentFragment(
                            args.instance, null, null, it)

                findNavController().navigateSafe(directions)

            },
            onDeleteCommentClick = {
                AlertDialogFragment.Builder()
                    .setMessage(R.string.delete_comment_confirm)
                    .setPositiveButton(android.R.string.ok)
                    .setNegativeButton(android.R.string.cancel)
                    .setExtra(EXTRA_COMMENT_ID, it.comment.id.toString())
                    .createAndShow(
                        childFragmentManager,
                        CONFIRM_DELETE_COMMENT_TAG
                    )

            },
            onImageClick = { url ->
                getMainActivity()?.openImage(null, url, null)
            },
            onPageClick = {
                getMainActivity()?.launchPage(it)
            },
            onPostMoreClick = {
                getMainActivity()?.showBottomMenu(getPostMoreMenu(it))
            },
        ).apply {
            stateRestorationPolicy = RecyclerView.Adapter.StateRestorationPolicy.PREVENT_WHEN_EMPTY
        }

        sharedElementEnterTransition = SharedElementTransition()
        sharedElementReturnTransition = SharedElementTransition()
//        enterTransition = TransitionInflater.from(requireContext())
//            .inflateTransition(android.R.transition.fade)

        setFragmentResultListener(AddOrEditCommentFragment.REQUEST_KEY) { _, bundle ->
            val result = bundle.getParcelableCompat<AddOrEditCommentFragment.Result>(AddOrEditCommentFragment.REQUEST_KEY_RESULT)

            if (result != null) {
//                viewModel.fetchPostData(args.instance, args.id)
            }
        }

        requireActivity().supportFragmentManager.setFragmentResultListener(
            CreateOrEditPostFragment.REQUEST_KEY,
            this
        ) { _, bundle ->
            val result = bundle.getParcelableCompat<PostView>(
                CreateOrEditPostFragment.REQUEST_KEY_RESULT)

            if (result != null) {
                viewModel.fetchPostData(args.instance, args.id, force = true)
                (parentFragment as? CommunityFragment)?.onPostUpdated()
            }
        }

        if (!args.isSinglePage) {
            requireMainActivity().onBackPressedDispatcher
                .addCallback(this, object : OnBackPressedCallback(true) {
                    override fun handleOnBackPressed() {
                        goBack()
                    }
                })
        }
    }

    private fun goBack() {
        (requireParentFragment() as CommunityFragment)
            .closePost(this@PostFragment)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        setBinding(FragmentPostBinding.inflate(inflater, container, false))

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.recyclerView.viewTreeObserver.addOnPreDrawListener(object : OnPreDrawListener {
            override fun onPreDraw(): Boolean {
                binding.recyclerView.viewTreeObserver.removeOnPreDrawListener(this)

                adapter.contentMaxWidth = binding.recyclerView.width

                setup()
                return false
            }

        })

//        view.doOnPreDraw {
//        }

    }

    private fun setup() {
        if (!isBindingAvailable()) {
            return
        }

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
                    null,
                ))
            onMainListingItemRetrieved(post)
        } ?: binding.loadingView.showProgressBar()

        viewModel.postData.observe(viewLifecycleOwner) {
            when (it) {
                is StatefulData.Error -> {
                    binding.swipeRefreshLayout.isRefreshing = false
                    binding.loadingView.hideAll()
                    adapter.error = it.error
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

                    val newlyPostedCommentId = it.data.newlyPostedCommentId
                    if (newlyPostedCommentId != null) {
                        val pos = adapter.getPositionOfComment(newlyPostedCommentId)

                        viewModel.resetNewlyPostedComment()

                        binding.recyclerView.post {
                            if (!isBindingAvailable()) {
                                return@post
                            }

                            if (pos >= 0) {
                                (binding.recyclerView.layoutManager as? LinearLayoutManager)
                                    ?.scrollToPositionWithOffset(
                                        pos, (requireMainActivity().lastInsets.topInset + Utils.convertDpToPixel(56f)).toInt()
                                    )
                                adapter.highlightComment(newlyPostedCommentId)

                                lifecycleScope.launch {
                                    withContext(Dispatchers.IO) {
                                        delay(HIGHLIGHT_COMMENT_MS)
                                    }

                                    withContext(Dispatchers.Main) {
                                        adapter.clearHighlightComment()
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        viewModel.deletePostResult.observe(viewLifecycleOwner) {
            when (it) {
                is StatefulData.Error -> {
                    AlertDialogFragment.Builder()
                        .setMessage(getString(
                            R.string.error_unable_to_send_post,
                            it.error::class.qualifiedName,
                            it.error.message))
                        .createAndShow(childFragmentManager, "ASDS")
                }
                is StatefulData.Loading -> {}
                is StatefulData.NotStarted -> {}
                is StatefulData.Success -> {
                    viewModel.fetchPostData(args.instance, args.id, force = true)
                    (parentFragment as? CommunityFragment)?.onPostUpdated()
                }
            }
        }

        if (viewModel.postData.valueOrNull == null) {
            lifecycleScope.launch(Dispatchers.Default) {
                delay(400)

                withContext(Dispatchers.Main) {
                    viewModel.fetchPostData(args.instance, args.id)
                }
            }
        }

        args.post?.getUrl(args.instance)?.let { url ->
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
        binding.fastScroller.setRecyclerView(binding.recyclerView)

        if (!hasConsumedJumpToComments && args.jumpToComments) {
            hasConsumedJumpToComments = true
            (binding.recyclerView.layoutManager as LinearLayoutManager)
                .scrollToPositionWithOffset(1, (Utils.convertDpToPixel(48f)).toInt())
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
                    android.R.id.home -> {
                        if (args.isSinglePage) {
                            false
                        } else {
                            goBack()
                            true
                        }
                    }

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

                    R.id.refresh -> {
                        viewModel.fetchPostData(args.instance, args.id, force = true)
                        true
                    }

                    else -> false
                }

        })
    }

    override fun onResume() {
        super.onResume()

        if (args.isSinglePage) {
            requireMainActivity().apply {
                setupForFragment<PostFragment>()
                hideBottomNav()
                disableCustomAppBar()
                lockUiOpenness = true
            }
        }
    }

    override fun navigateToSignInScreen() {
        val direction = PostFragmentDirections.actionPostFragmentToLogin()
        findNavController().navigateSafe(direction)
    }

    private fun forceRefresh() {
        viewModel.fetchPostData(args.instance, args.id, force = true)
    }

    fun onMainListingItemRetrieved(post: PostView) {
        post.getUrl(args.instance).let { url ->
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

    private fun getPostMoreMenu(postView: PostView): BottomMenu {
        val bottomMenu = BottomMenu(requireContext()).apply {
            if (postView.post.creator_id == accountManager.currentAccount.value?.id) {
                addItemWithIcon(R.id.edit_post, R.string.edit_post, R.drawable.baseline_edit_24)
                addItemWithIcon(R.id.delete, R.string.delete_post, R.drawable.baseline_delete_24)
            }

            if (this.itemsCount() == 0) {
                addItem(io.noties.markwon.R.id.none, R.string.no_options)
            }

            setTitle(R.string.post_options)

            setOnMenuItemClickListener {
                when (it.id) {
                    R.id.edit_post -> {
                        CreateOrEditPostFragment()
                            .apply {
                                arguments = CreateOrEditPostFragmentArgs(
                                    instance = viewModel.instance,
                                    post = postView.post,
                                    communityName = null,
                                ).toBundle()
                            }
                            .showAllowingStateLoss(childFragmentManager, "CreateOrEditPostFragment")
                    }
                    R.id.delete -> {
                        viewModel.deletePost(postView.post)
                    }
                }
            }
        }

        return bottomMenu
    }

    override fun onPositiveClick(dialog: AlertDialogFragment, tag: String?) {
        when (tag) {
            CONFIRM_DELETE_COMMENT_TAG -> {
                val commentId = dialog.getExtra(EXTRA_COMMENT_ID)
                if (commentId != null) {
                    viewModel.deleteComment(PostRef(args.instance, args.id), commentId.toInt())
                }
            }
        }
    }

    override fun onNegativeClick(dialog: AlertDialogFragment, tag: String?) {
        // do nothing
    }

    sealed class Item(
        open val id: String
    ) {

        data class HeaderItem(
            val postView: PostView,
            var videoState: VideoState?
        ) : Item(postView.getUniqueKey())

        data class CommentItem(
            val commentId: CommentId,
            val content: String,
            val comment: CommentView,
            val depth: Int,
            val baseDepth: Int,
            val isExpanded: Boolean,
            val isPending: Boolean,
            val view: PostViewModel.ListView.CommentListView,
            val childrenCount: Int,
            val isPostLocked: Boolean,
            val isUpdating: Boolean,
            val isDeleting: Boolean,
        ) : Item(
            "comment_${comment.comment.id}"
        )

        data class PendingCommentItem(
            val commentId: CommentId?,
            val content: String,
            val author: String?,
            val depth: Int,
            val baseDepth: Int,
            val isExpanded: Boolean,
            val isPending: Boolean,
            val view: PostViewModel.ListView.PendingCommentListView,
            val childrenCount: Int,
        ) : Item(
            "pending_comment_${view.pendingCommentView.id}"
        )

        data class MoreCommentsItem(
            val parentId: CommentId?,
            val moreCount: Int,
            val depth: Int,
            val baseDepth: Int,
        ) : Item(
            "more_comments_${parentId}"
        )

        class ProgressOrErrorItem(
            val error: Throwable? = null
        ) : Item("wew_pls_no_progress")

        object FooterItem : Item("footer")
    }

    private inner class PostsAdapter(
        private val context: Context,
        private val instance: String,
        private val revealAll: Boolean,
        private val voteUiHandler: VoteUiHandler,
        private val onRefreshClickCb: () -> Unit,
        private val onSignInRequired: () -> Unit,
        private val onInstanceMismatch: (String, String) -> Unit,
        private val onAddCommentClick: (Either<PostView, CommentView>) -> Unit,
        private val onEditCommentClick: (CommentView) -> Unit,
        private val onDeleteCommentClick: (CommentView) -> Unit,
        private val onImageClick: (String) -> Unit,
        private val onPageClick: (PageRef) -> Unit,
        private val onPostMoreClick: (PostView) -> Unit,
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

        private var rawData: PostViewModel.PostData? = null

        private var tempSize = Size()

        private var highlightedComment: CommentId = -1
        var isLoaded: Boolean = false
        var error: Throwable? = null
            set(value) {
                field = value

                refreshItems()
            }

        var contentMaxWidth = 0
            set(value) {
                if (value == field) {
                    return
                }
                field = value

                for (i in 0.. items.lastIndex) {
                    if (items[i] is HeaderItem) {
                        notifyItemChanged(i)
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
            is PendingCommentItem ->
                if (item.isExpanded)
                    R.layout.post_pending_comment_expanded_item
                else
                    R.layout.post_pending_comment_collapsed_item
            is ProgressOrErrorItem -> R.layout.generic_loading_item
            is MoreCommentsItem -> R.layout.post_more_comments_item
            is FooterItem -> R.layout.generic_footer_item
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            val v = inflater.inflate(viewType, parent, false)

            parentHeight = parent.height

            return when (viewType) {
                R.layout.post_header_item -> ViewBindingViewHolder(PostHeaderItemBinding.bind(v))
                R.layout.post_comment_expanded_item ->
                    ViewBindingViewHolder(PostCommentExpandedItemBinding.bind(v))
                R.layout.post_comment_collapsed_item ->
                    ViewBindingViewHolder(PostCommentCollapsedItemBinding.bind(v))
                R.layout.post_pending_comment_expanded_item ->
                    ViewBindingViewHolder(PostPendingCommentExpandedItemBinding.bind(v))
                R.layout.post_pending_comment_collapsed_item ->
                    ViewBindingViewHolder(PostPendingCommentCollapsedItemBinding.bind(v))
                R.layout.post_more_comments_item ->
                    ViewBindingViewHolder(PostMoreCommentsItemBinding.bind(v))
                R.layout.generic_loading_item ->
                    ViewBindingViewHolder(GenericLoadingItemBinding.bind(v))
                R.layout.generic_footer_item ->
                    ViewBindingViewHolder(GenericFooterItemBinding.bind(v))
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
                        val b = holder.getBinding<PostHeaderItemBinding>()
                        val post = item.postView
                        val postKey = post.getUniqueKey()

                        ViewCompat.setTransitionName(b.title, "title")

                        lemmyHeaderHelper.populateHeaderSpan(
                            headerContainer = b.headerContainer,
                            postView = post,
                            instance = instance,
                            onPageClick = {
                                requireMainActivity().launchPage(it)
                            },
                            listAuthor = false
                        )

                        b.title.text = post.getFormattedTitle()
                        b.author.text = post.getFormattedAuthor()

                        b.commentButton.text = abbrevNumber(item.postView.counts.comments.toLong())
                        b.commentButton.isEnabled = !post.post.locked
                        b.commentButton.setOnClickListener {
                            onAddCommentClick(Either.Left(item.postView))
                        }
                        b.addCommentButton.isEnabled = !post.post.locked
                        b.addCommentButton.setOnClickListener {
                            onAddCommentClick(Either.Left(item.postView))
                        }

                        b.moreButton.setOnClickListener {
                            onPostMoreClick(item.postView)
                        }

                        lemmyContentHelper.setupFullContent(
                            reveal = revealAll || revealedItems.contains(postKey),
                            tempSize = tempSize,
                            videoViewMaxHeight = (binding.recyclerView.height - Utils.convertDpToPixel(16f)).toInt(),
                            contentMaxWidth = contentMaxWidth,
                            fullImageViewTransitionName = "post_image",
                            postView = post,
                            instance = instance,
                            rootView = b.root,
                            fullContentContainerView = b.fullContent,
                            lazyUpdate = true,
                            videoState = item.videoState,
                            onFullImageViewClickListener = { v, url ->
                                onImageClick(url)
                            },
                            onImageClickListener = { url ->
                                onImageClick(url)
                            },
                            onRevealContentClickedFn = {
                                revealedItems.add(postKey)
                                notifyItemChanged(holder.absoluteAdapterPosition)
                            },
                            onItemClickListener = {},
                            onLemmyUrlClick = onPageClick
                        )

                        voteUiHandler.bind(
                            viewLifecycleOwner,
                            instance,
                            item.postView,
                            b.upvoteButton,
                            b.downvoteButton,
                            b.upvoteCount,
                            onSignInRequired,
                            onInstanceMismatch,
                        )
                    }
                }
                else -> super.onBindViewHolder(holder, position, payloads)
            }
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            when (val item = items[position]) {
                is HeaderItem -> {
                    val b = holder.getBinding<PostHeaderItemBinding>()
                    val post = item.postView
                    val postKey = post.getUniqueKey()

                    setupTransitionAnimation(b.title)

                    ViewCompat.setTransitionName(b.title, "title")

                    lemmyHeaderHelper.populateHeaderSpan(
                        headerContainer = b.headerContainer,
                        postView = item.postView,
                        instance = instance,
                        onPageClick = {
                            requireMainActivity().launchPage(it)
                        },
                        listAuthor = false
                    )

                    b.title.text = post.getFormattedTitle()
                    b.author.text = post.getFormattedAuthor()

                    b.commentButton.text = abbrevNumber(item.postView.counts.comments.toLong())
                    b.commentButton.isEnabled = !post.post.locked
                    b.commentButton.setOnClickListener {
                        onAddCommentClick(Either.Left(item.postView))
                    }
                    b.addCommentButton.isEnabled = !post.post.locked
                    b.addCommentButton.setOnClickListener {
                        onAddCommentClick(Either.Left(item.postView))
                    }

                    b.moreButton.setOnClickListener {
                        onPostMoreClick(item.postView)
                    }

                    lemmyContentHelper.setupFullContent(
                        reveal = revealAll || revealedItems.contains(postKey),
                        tempSize = tempSize,
                        videoViewMaxHeight = (binding.recyclerView.height - Utils.convertDpToPixel(16f)).toInt(),
                        contentMaxWidth = contentMaxWidth,
                        fullImageViewTransitionName = "post_image",
                        postView = post,
                        instance = instance,
                        rootView = b.root,
                        fullContentContainerView = b.fullContent,
                        videoState = item.videoState,
                        onFullImageViewClickListener = { v, url ->
                            onImageClick(url)
                        },
                        onImageClickListener = onImageClick,
                        onRevealContentClickedFn = {
                            revealedItems.add(postKey)
                            notifyItemChanged(holder.absoluteAdapterPosition)
                        },
                        onItemClickListener = {},
                        onLemmyUrlClick = onPageClick,
                    )

                    Log.d(TAG, "header vote state: ${item.postView.counts.upvotes}")

                    voteUiHandler.bind(
                        viewLifecycleOwner,
                        instance,
                        item.postView,
                        b.upvoteButton,
                        b.downvoteButton,
                        b.upvoteCount,
                        onSignInRequired,
                        onInstanceMismatch,
                    )

                    b.root.tag = item
                }
                is CommentItem -> {
                    if (item.isExpanded) {
                        val b = holder.getBinding<PostCommentExpandedItemBinding>()
                        val comment = item.comment
                        val body = item.content

                        threadLinesHelper.populateThreadLines(
                            b.threadLinesContainer, item.depth, item.baseDepth
                        )
                        lemmyHeaderHelper.populateHeaderSpan(b.headerContainer, item.comment)

                        if (comment.comment.deleted || item.isDeleting) {
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
                            LemmyTextHelper.bindText(
                                textView = b.text,
                                text = body,
                                instance = instance,
                                onImageClickListener = onImageClick,
                                onPageClick = onPageClick,
                            )
                        }

                        val giphyLinks = LemmyUtils.findGiphyLinks(body)
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
                                    onImageClick(fullUrl)
                                }

                                lastViewId = viewId
                            }
                        } else {
                            b.mediaContainer.removeAllViews()
                            b.mediaContainer.visibility = View.GONE
                        }

                        b.collapseSectionButton.setOnClickListener {
                            collapseSection(holder.bindingAdapterPosition)
                        }
                        b.topHotspot.setOnClickListener {
                            collapseSection(holder.bindingAdapterPosition)
                        }

                        b.commentButton.isEnabled = !item.isPostLocked
                        b.commentButton.setOnClickListener {
                            onAddCommentClick(Either.Right(item.comment))
                        }
                        b.moreButton.setOnClickListener {

                            PopupMenu(context, b.moreButton).apply {
                                inflate(R.menu.menu_comment_item)

                                if (BuildConfig.DEBUG) {
                                    menu.findItem(R.id.raw_comment).isVisible = true
                                }

                                if (item.comment.creator.id !=
                                    accountManager.currentAccount.value?.id) {
                                    menu.setGroupVisible(R.id.mod_post_actions, false)
                                    //menu.findItem(R.id.edit_comment).isVisible = false
                                }

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
                                            onEditCommentClick(item.comment)
                                        }
                                        R.id.delete_comment -> {
                                            onDeleteCommentClick(item.comment)
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
                            viewLifecycleOwner,
                            instance,
                            item.comment,
                            b.upvoteButton,
                            b.downvoteButton,
                            b.upvoteCount,
                            onSignInRequired,
                            onInstanceMismatch,
                        )

                        highlightComment(item.commentId, b.highlightBg)

                        if (item.isUpdating) {
                            b.progressBar.visibility = View.VISIBLE
                        } else {
                            b.progressBar.visibility = View.GONE
                        }

                        b.root.tag = item
                    } else {
                        // collapsed
                        val b = holder.getBinding<PostCommentCollapsedItemBinding>()
                        threadLinesHelper.populateThreadLines(
                            b.threadLinesContainer, item.depth, item.baseDepth
                        )
                        lemmyHeaderHelper.populateHeaderSpan(
                            headerContainer = b.headerContainer,
                            item = item.comment,
                            detailed = true,
                            childrenCount = item.childrenCount
                        )

                        b.expandSectionButton.setOnClickListener {
                            expandSection(holder.absoluteAdapterPosition)
                        }
                        b.topHotspot.setOnClickListener {
                            expandSection(holder.absoluteAdapterPosition)
                        }
                        if (item.comment.comment.distinguished) {
                            b.overlay.visibility = View.VISIBLE
                            b.overlay.setBackgroundResource(R.drawable.locked_overlay)
                        } else {
                            b.overlay.visibility = View.GONE
                        }

                        highlightComment(item.commentId, b.highlightBg)

                        if (item.isUpdating) {
                            b.progressBar.visibility = View.VISIBLE
                        } else {
                            b.progressBar.visibility = View.GONE
                        }

                        b.root.tag = item
                    }
                }
                is PendingCommentItem -> {
                    if (item.isExpanded) {
                        val b = holder.getBinding<PostPendingCommentExpandedItemBinding>()
                        val body = item.content

                        threadLinesHelper.populateThreadLines(
                            b.threadLinesContainer, item.depth, item.baseDepth
                        )
                        b.headerContainer.setTextFirstPart(item.author ?: getString(R.string.unknown))
                        b.headerContainer.setTextSecondPart("")

                        LemmyTextHelper.bindText(
                            textView = b.text,
                            text = body,
                            instance = instance,
                            onImageClickListener = onImageClick,
                            onPageClick = onPageClick,
                        )

                        val giphyLinks = LemmyUtils.findGiphyLinks(body)
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
                                    onImageClick(fullUrl)
                                }

                                lastViewId = viewId
                            }
                        } else {
                            b.mediaContainer.removeAllViews()
                            b.mediaContainer.visibility = View.GONE
                        }

                        b.collapseSectionButton.setOnClickListener {
                            collapseSection(holder.bindingAdapterPosition)
                        }
                        b.topHotspot.setOnClickListener {
                            collapseSection(holder.bindingAdapterPosition)
                        }

                        highlightComment(item.commentId, b.highlightBg)

                        b.root.tag = item
                    } else {
                        // collapsed
                        val b = holder.getBinding<PostPendingCommentCollapsedItemBinding>()
                        threadLinesHelper.populateThreadLines(
                            b.threadLinesContainer, item.depth, item.baseDepth
                        )
                        b.headerContainer.setTextFirstPart(item.author ?: getString(R.string.unknown))
                        b.headerContainer.setTextSecondPart("")

                        b.expandSectionButton.setOnClickListener {
                            expandSection(holder.absoluteAdapterPosition)
                        }
                        b.topHotspot.setOnClickListener {
                            expandSection(holder.absoluteAdapterPosition)
                        }
                        b.overlay.visibility = View.GONE

                        b.root.tag = item

                        highlightComment(item.commentId, b.highlightBg)
                    }
                }
                is ProgressOrErrorItem -> {
                    val b = holder.getBinding<GenericLoadingItemBinding>()
                    if (item.error != null) {
                        b.loadingView.showDefaultErrorMessageFor(item.error)
                    } else if (!isLoaded) {
                        b.loadingView.showProgressBar()
                    }
                    b.loadingView.setOnRefreshClickListener {
                        onRefreshClickCb()
                    }
                }
                is MoreCommentsItem -> {
                    val b = holder.getBinding<PostMoreCommentsItemBinding>()

                    threadLinesHelper.populateThreadLines(
                        b.threadLinesContainer, item.depth, item.baseDepth
                    )
                    b.moreButton.text = context.resources.getQuantityString(
                        R.plurals.replies_format, item.moreCount, item.moreCount
                    )

                    b.moreButton.setOnClickListener {
                        if (item.parentId != null) {
                            viewModel.fetchMoreComments(item.parentId)
                        }
                    }

                    b.root.tag = item
                }

                FooterItem -> {}
            }
        }

        private fun highlightComment(commentId: CommentId?, bg: View) {
            commentId ?: return

            if (highlightedComment == commentId) {
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

        override fun onViewRecycled(holder: RecyclerView.ViewHolder) {
            super.onViewRecycled(holder)

            val item = if (holder.absoluteAdapterPosition >= 0) {
                items[holder.absoluteAdapterPosition]
            } else {
                null
            }

            if (holder.isBinding<PostHeaderItemBinding>()) {
                val b = holder.getBinding<PostHeaderItemBinding>()
                val state = lemmyContentHelper.recycleFullContent(b.fullContent)

                (item as? HeaderItem)?.videoState = state.videoState
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

        /**
         * @param refreshHeader Pass false to not always refresh header. Useful for web view headers
         * as refreshing them might cause a relayout causing janky animations
         */
        private fun refreshItems(refreshHeader: Boolean = true) {
            val rawData = rawData
            val oldItems = items

            val newItems =
                if (error == null) {
                    rawData ?: return

                    val finalItems = mutableListOf<Item>()

                    rawData.postView.let {
                        if (!mainListingItemSeen) {
                            mainListingItemSeen = true
                            onMainListingItemRetrieved(it.post)
                        }

                        finalItems += HeaderItem(it.post, args.videoState)
                    }

                    rawData.commentTree.flatten().forEach {
                        when (val commentView = it.commentView) {
                            is PostViewModel.ListView.CommentListView -> {
                                val isDeleting =
                                    commentView.pendingCommentView?.isActionDelete == true
                                finalItems += CommentItem(
                                    commentId = commentView.comment.comment.id,
                                    content =
                                        commentView.pendingCommentView?.content
                                            ?: commentView.comment.comment.content,
                                    comment = commentView.comment,
                                    depth = commentView.comment.getDepth(),
                                    baseDepth = 0,
                                    isExpanded = !commentView.isCollapsed,
                                    isPending = false,
                                    view = commentView,
                                    childrenCount = it.children.size,
                                    isPostLocked = commentView.comment.post.locked,
                                    isUpdating = commentView.pendingCommentView != null,
                                    isDeleting = isDeleting,
                                )
                            }
                            is PostViewModel.ListView.PendingCommentListView -> {
                                finalItems += PendingCommentItem(
                                    commentId = commentView.pendingCommentView.commentId,
                                    content = commentView.pendingCommentView.content,
                                    author = commentView.author,
                                    depth = it.depth,
                                    baseDepth = 0,
                                    isExpanded = !commentView.isCollapsed,
                                    isPending = false,
                                    view = commentView,
                                    childrenCount = it.children.size,
                                )
                            }
                            is PostViewModel.ListView.PostListView -> {
                                // should never happen
                            }

                            is PostViewModel.ListView.MoreCommentsItem -> {
                                if (commentView.parentCommentId != null) {
                                    finalItems += MoreCommentsItem(
                                        parentId = commentView.parentCommentId,
                                        moreCount = commentView.moreCount,
                                        depth = it.depth,
                                        baseDepth = 0,
                                    )
                                }
                            }
                        }
                    }

                    if (!isLoaded) {
                        finalItems += listOf(ProgressOrErrorItem())
                    }

                    finalItems += FooterItem

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
                        is HeaderItem ->
                            oldItem.postView.post == (newItem as HeaderItem).postView.post
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

        fun getPositionOfComment(commentId: CommentId): Int =
            items.indexOfFirst {
                when (it) {
                    is CommentItem -> it.commentId == commentId
                    is HeaderItem -> false
                    is MoreCommentsItem -> false
                    is PendingCommentItem -> it.commentId == commentId
                    is ProgressOrErrorItem -> false
                    FooterItem -> false
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

        fun highlightComment(commentId: CommentId) {
            val pos = getPositionOfComment(commentId)
            if (pos == -1) {
                return
            }

            highlightedComment = commentId
            notifyItemChanged(getPositionOfComment(commentId), Unit)
        }

        fun clearHighlightComment() {
            val pos = getPositionOfComment(highlightedComment)
            if (pos == -1) {
                return
            }

            highlightedComment = -1
            notifyItemChanged(pos, Unit)
        }

        private fun collapseSection(position: Int) {
            if (position < 0) return

            (items[position] as? CommentItem)?.view?.isCollapsed = true
            (items[position] as? PendingCommentItem)?.view?.isCollapsed = true

            refreshItems(refreshHeader = false)
        }

        private fun expandSection(position: Int) {
            if (position < 0) return

            (items[position] as? CommentItem)?.view?.isCollapsed = false
            (items[position] as? PendingCommentItem)?.view?.isCollapsed = false

            refreshItems(refreshHeader = false)
        }

    }
}
