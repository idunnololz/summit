package com.idunnololz.summit.lemmy.post

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver.OnPreDrawListener
import androidx.activity.OnBackPressedCallback
import androidx.core.view.MenuProvider
import androidx.core.view.doOnPreDraw
import androidx.fragment.app.setFragmentResultListener
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import arrow.core.Either
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
import com.idunnololz.summit.lemmy.MoreActionsViewModel
import com.idunnololz.summit.lemmy.PageRef
import com.idunnololz.summit.lemmy.PostRef
import com.idunnololz.summit.lemmy.comment.AddOrEditCommentFragment
import com.idunnololz.summit.lemmy.comment.AddOrEditCommentFragmentArgs
import com.idunnololz.summit.lemmy.community.CommunityFragment
import com.idunnololz.summit.lemmy.createOrEditPost.CreateOrEditPostFragment
import com.idunnololz.summit.lemmy.createOrEditPost.CreateOrEditPostFragmentArgs
import com.idunnololz.summit.lemmy.post.PostFragment.Item.CommentItem
import com.idunnololz.summit.lemmy.post.PostFragment.Item.FooterItem
import com.idunnololz.summit.lemmy.post.PostFragment.Item.HeaderItem
import com.idunnololz.summit.lemmy.post.PostFragment.Item.MoreCommentsItem
import com.idunnololz.summit.lemmy.post.PostFragment.Item.PendingCommentItem
import com.idunnololz.summit.lemmy.post.PostFragment.Item.ProgressOrErrorItem
import com.idunnololz.summit.lemmy.post.PostViewModel.Companion.HIGHLIGHT_COMMENT_MS
import com.idunnololz.summit.lemmy.postAndCommentView.PostAndCommentViewBuilder
import com.idunnololz.summit.offline.OfflineManager
import com.idunnololz.summit.preview.VideoType
import com.idunnololz.summit.reddit.CommentsSortOrder
import com.idunnololz.summit.reddit.getLocalizedName
import com.idunnololz.summit.util.BaseFragment
import com.idunnololz.summit.util.BottomMenu
import com.idunnololz.summit.util.LinkUtils
import com.idunnololz.summit.util.SharedElementTransition
import com.idunnololz.summit.util.StatefulData
import com.idunnololz.summit.util.Utils
import com.idunnololz.summit.util.ext.navigateSafe
import com.idunnololz.summit.util.ext.showAllowingStateLoss
import com.idunnololz.summit.util.getParcelableCompat
import com.idunnololz.summit.util.recyclerView.ViewBindingViewHolder
import com.idunnololz.summit.util.recyclerView.getBinding
import com.idunnololz.summit.util.recyclerView.isBinding
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

    @Inject
    lateinit var postAndCommentViewBuilder: PostAndCommentViewBuilder

    private var mainListingItemSeen = false

    private var actionsViewModel: MoreActionsViewModel? = null

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
            postAndCommentViewBuilder = postAndCommentViewBuilder,
            context,
            args.instance,
            args.reveal,
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

                AddOrEditCommentFragment().apply {
                    arguments =
                        AddOrEditCommentFragmentArgs(
                            args.instance,null, null, it).toBundle()
                }.show(childFragmentManager, "asdf")

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
            onVideoClick = { url, videoType, state ->
                getMainActivity()?.openVideo(url, videoType, state)
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

        setFragmentResultListener(AddOrEditCommentFragment.REQUEST_KEY) { _, bundle ->
//            val result = bundle.getParcelableCompat<AddOrEditCommentFragment.Result>(AddOrEditCommentFragment.REQUEST_KEY_RESULT)
//
//            if (result != null) {
//                viewModel.fetchPostData(args.instance, args.id)
//            }
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
            actionsViewModel = (parentFragment as? CommunityFragment)?.actionsViewModel
        } else {
            // do things if this is a single page
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
                hideBottomNav(animate = true)
                disableCustomAppBar(animate = true)
                lockUiOpenness = true
            }
        }
    }

    override fun navigateToSignInScreen() {
        if (args.isSinglePage) {
            val direction = PostFragmentDirections.actionPostFragmentToLogin()
            findNavController().navigateSafe(direction)
        } else {
            (parentFragment as? SignInNavigator)?.navigateToSignInScreen()
        }
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

            if (actionsViewModel != null) {
                addItemWithIcon(
                    R.id.block_community,
                    getString(R.string.block_this_community_format, postView.community.name),
                    R.drawable.baseline_block_24
                )
                addItemWithIcon(
                    R.id.block_user,
                    getString(R.string.block_this_user, postView.creator.name),
                    R.drawable.baseline_person_off_24
                )
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
                    R.id.block_community -> {
                        actionsViewModel?.blockCommunity(postView.community.id)
                    }
                    R.id.block_user -> {
                        actionsViewModel?.blockPerson(postView.creator.id)
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
        private val postAndCommentViewBuilder: PostAndCommentViewBuilder,
        private val context: Context,
        private val instance: String,
        private val revealAll: Boolean,
        private val onRefreshClickCb: () -> Unit,
        private val onSignInRequired: () -> Unit,
        private val onInstanceMismatch: (String, String) -> Unit,
        private val onAddCommentClick: (Either<PostView, CommentView>) -> Unit,
        private val onEditCommentClick: (CommentView) -> Unit,
        private val onDeleteCommentClick: (CommentView) -> Unit,
        private val onImageClick: (String) -> Unit,
        private val onVideoClick: (String, VideoType, VideoState?) -> Unit,
        private val onPageClick: (PageRef) -> Unit,
        private val onPostMoreClick: (PostView) -> Unit,
    ) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

        private val inflater = LayoutInflater.from(context)

        private var items: List<Item> = listOf()

        private var parentHeight: Int = 0

        /**
         * Set of items that is hidden by default but is reveals (ie. nsfw or spoiler tagged)
         */
        private var revealedItems = mutableSetOf<String>()

        private var rawData: PostViewModel.PostData? = null

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

                        postAndCommentViewBuilder.bindPostView(
                            binding = b,
                            container = binding.recyclerView,
                            postView = item.postView,
                            instance = instance,
                            isRevealed = revealAll || revealedItems.contains(postKey),
                            contentMaxWidth = contentMaxWidth,
                            viewLifecycleOwner = viewLifecycleOwner,
                            videoState = item.videoState,
                            updateContent = false,
                            onRevealContentClickedFn = {
                                revealedItems.add(postKey)
                                notifyItemChanged(holder.absoluteAdapterPosition)
                            },
                            onImageClick = onImageClick,
                            onVideoClick = onVideoClick,
                            onPageClick = {
                                requireMainActivity().launchPage(it)
                            },
                            onAddCommentClick = onAddCommentClick,
                            onPostMoreClick = onPostMoreClick,
                            onSignInRequired = onSignInRequired,
                            onInstanceMismatch = onInstanceMismatch,
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

                    postAndCommentViewBuilder.bindPostView(
                        binding = b,
                        container = binding.recyclerView,
                        postView = item.postView,
                        instance = instance,
                        isRevealed = revealAll || revealedItems.contains(postKey),
                        contentMaxWidth = contentMaxWidth,
                        viewLifecycleOwner = viewLifecycleOwner,
                        videoState = item.videoState,
                        updateContent = true,
                        onRevealContentClickedFn = {
                            revealedItems.add(postKey)
                            notifyItemChanged(holder.absoluteAdapterPosition)
                        },
                        onImageClick = onImageClick,
                        onVideoClick = onVideoClick,
                        onPageClick = {
                            requireMainActivity().launchPage(it)
                        },
                        onAddCommentClick = onAddCommentClick,
                        onPostMoreClick = onPostMoreClick,
                        onSignInRequired = onSignInRequired,
                        onInstanceMismatch = onInstanceMismatch,
                    )
                }
                is CommentItem -> {
                    if (item.isExpanded) {
                        val b = holder.getBinding<PostCommentExpandedItemBinding>()
                        val highlight = highlightedComment == item.commentId

                        postAndCommentViewBuilder.bindCommentViewExpanded(
                            holder,
                            b,
                            item.baseDepth,
                            item.depth,
                            item.comment,
                            item.isDeleting,
                            item.content,
                            instance,
                            item.isPostLocked,
                            item.isUpdating,
                            highlight,
                            viewLifecycleOwner,
                            accountManager.currentAccount.value?.id,
                            onImageClick,
                            onPageClick,
                            {
                                collapseSection(holder.bindingAdapterPosition)
                            },
                            onAddCommentClick,
                            onEditCommentClick,
                            onDeleteCommentClick,
                            onSignInRequired,
                            onInstanceMismatch,
                        )
                    } else {
                        // collapsed
                        val b = holder.getBinding<PostCommentCollapsedItemBinding>()
                        val highlight = highlightedComment == item.commentId

                        postAndCommentViewBuilder.bindCommentViewCollapsed(
                            holder,
                            b,
                            item.baseDepth,
                            item.depth,
                            item.childrenCount,
                            highlight,
                            item.isUpdating,
                            item.comment,
                            ::expandSection,
                        )
                    }
                }
                is PendingCommentItem -> {
                    if (item.isExpanded) {
                        val b = holder.getBinding<PostPendingCommentExpandedItemBinding>()
                        val highlight = highlightedComment == item.commentId

                        postAndCommentViewBuilder.bindPendingCommentViewExpanded(
                            holder,
                            b,
                            item.baseDepth,
                            item.depth,
                            item.content,
                            instance,
                            item.author,
                            highlight,
                            onImageClick,
                            onPageClick,
                            ::collapseSection,
                        )
                    } else {
                        // collapsed
                        val b = holder.getBinding<PostPendingCommentCollapsedItemBinding>()
                        val highlight = highlightedComment == item.commentId

                        postAndCommentViewBuilder.bindPendingCommentViewCollapsed(
                            holder,
                            b,
                            item.baseDepth,
                            item.depth,
                            item.author,
                            highlight,
                            ::collapseSection,
                        )
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

                    postAndCommentViewBuilder.threadLinesHelper.populateThreadLines(
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

        override fun onViewRecycled(holder: RecyclerView.ViewHolder) {
            super.onViewRecycled(holder)

            val item = if (holder.absoluteAdapterPosition >= 0) {
                items[holder.absoluteAdapterPosition]
            } else {
                null
            }

            if (holder.isBinding<PostHeaderItemBinding>()) {
                val b = holder.getBinding<PostHeaderItemBinding>()
                val state = postAndCommentViewBuilder.recycle(b)

                (item as? HeaderItem)?.videoState = state.videoState
            }
        }

        override fun getItemCount(): Int = items.size

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
