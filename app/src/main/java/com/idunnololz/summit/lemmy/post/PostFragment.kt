package com.idunnololz.summit.lemmy.post

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
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.idunnololz.summit.R
import com.idunnololz.summit.account.AccountManager
import com.idunnololz.summit.account_ui.PreAuthDialogFragment
import com.idunnololz.summit.account_ui.SignInNavigator
import com.idunnololz.summit.alert.AlertDialogFragment
import com.idunnololz.summit.api.dto.PostView
import com.idunnololz.summit.api.utils.getUrl
import com.idunnololz.summit.databinding.FragmentPostBinding
import com.idunnololz.summit.history.HistoryManager
import com.idunnololz.summit.history.HistorySaveReason
import com.idunnololz.summit.lemmy.MoreActionsViewModel
import com.idunnololz.summit.lemmy.PostRef
import com.idunnololz.summit.lemmy.comment.AddOrEditCommentFragment
import com.idunnololz.summit.lemmy.comment.AddOrEditCommentFragmentArgs
import com.idunnololz.summit.lemmy.community.CommunityFragment
import com.idunnololz.summit.lemmy.createOrEditPost.CreateOrEditPostFragment
import com.idunnololz.summit.lemmy.createOrEditPost.CreateOrEditPostFragmentArgs
import com.idunnololz.summit.lemmy.post.PostViewModel.Companion.HIGHLIGHT_COMMENT_MS
import com.idunnololz.summit.lemmy.postAndCommentView.PostAndCommentViewBuilder
import com.idunnololz.summit.offline.OfflineManager
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

    private var adapter: PostsAdapter? = null

    private var hasConsumedJumpToComments: Boolean = false

    @Inject
    lateinit var historyManager: HistoryManager

    @Inject
    lateinit var offlineManager: OfflineManager

    @Inject
    lateinit var accountManager: AccountManager

    @Inject
    lateinit var postAndCommentViewBuilder: PostAndCommentViewBuilder

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

        val context = requireContext()
        if (adapter == null) {
            adapter = PostsAdapter(
                postAndCommentViewBuilder = postAndCommentViewBuilder,
                context,
                binding.recyclerView,
                this,
                args.instance,
                args.reveal,
                accountManager.currentAccount.value?.id,
                args.videoState,
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
                            getString(
                                R.string.error_account_instance_mismatch,
                                accountInstance,
                                apiInstance
                            )
                        )
                        .createAndShow(childFragmentManager, "aa")
                },
                onAddCommentClick = { postOrComment ->
                    if (accountManager.currentAccount.value == null) {
                        PreAuthDialogFragment.newInstance(R.id.action_add_comment)
                            .show(childFragmentManager, "asdf")
                        return@PostsAdapter
                    }

                    AddOrEditCommentFragment().apply {
                        arguments = postOrComment.fold({
                            AddOrEditCommentFragmentArgs(
                                args.instance, null, it, null
                            )
                        }, {
                            AddOrEditCommentFragmentArgs(
                                args.instance, it, null, null
                            )
                        }).toBundle()
                    }.show(childFragmentManager, "asdf")
                },
                onEditCommentClick = {
                    if (accountManager.currentAccount.value == null) {
                        PreAuthDialogFragment.newInstance(R.id.action_edit_comment)
                            .show(childFragmentManager, "asdf")
                        return@PostsAdapter
                    }

                    AddOrEditCommentFragment().apply {
                        arguments =
                            AddOrEditCommentFragmentArgs(
                                args.instance, null, null, it
                            ).toBundle()
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
                onImageClick = { view, url ->
                    getMainActivity()?.openImage(view, binding.appBar, null, url, null)
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
                onFetchComments = {
                    viewModel.fetchMoreComments(it)
                }
            ).apply {
                stateRestorationPolicy =
                    RecyclerView.Adapter.StateRestorationPolicy.PREVENT_WHEN_EMPTY
            }
        }

        binding.recyclerView.viewTreeObserver.addOnPreDrawListener(object : OnPreDrawListener {
            override fun onPreDraw(): Boolean {
                if (isBindingAvailable()) {
                    binding.recyclerView.viewTreeObserver.removeOnPreDrawListener(this)

                    adapter?.contentMaxWidth = binding.recyclerView.width

                    setup()
                }

                return false
            }
        })
    }

    private fun setup() {
        if (!isBindingAvailable()) {
            return
        }

        val adapter = adapter ?: return

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
                    onMainListingItemRetrieved(it.data.postView.post)

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

        actionsViewModel?.deletePostResult?.observe(viewLifecycleOwner) {
            when (it) {
                is StatefulData.Error -> {
                    AlertDialogFragment.Builder()
                        .setMessage(getString(
                            R.string.error_unable_to_send_post,
                            it.error::class.qualifiedName,
                            it.error.message))
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

    override fun proceedAnyways(tag: Int) {
        when (tag) {
            R.id.action_add_comment -> {
                val postView = viewModel.postData.valueOrNull?.postView ?: return
                AddOrEditCommentFragment().apply {
                    arguments =
                        AddOrEditCommentFragmentArgs(
                            args.instance, null, postView.post, null)
                            .toBundle()
                }.show(childFragmentManager, "asdf")
            }
            R.id.action_edit_comment -> {
                val postView = viewModel.postData.valueOrNull?.postView ?: return
                AddOrEditCommentFragment().apply {
                    arguments =
                        AddOrEditCommentFragmentArgs(
                            args.instance, null, postView.post, null)
                            .toBundle()
                }.show(childFragmentManager, "asdf")
            }
        }
    }

    private fun forceRefresh() {
        viewModel.fetchPostData(args.instance, args.id, force = true)
    }

    private fun onMainListingItemRetrieved(post: PostView) {
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
                        actionsViewModel?.deletePost(postView.post)
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
}
