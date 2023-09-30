package com.idunnololz.summit.lemmy.inbox

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.core.content.ContextCompat
import androidx.core.view.updatePadding
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.idunnololz.summit.R
import com.idunnololz.summit.account.AccountActionsManager
import com.idunnololz.summit.account.AccountManager
import com.idunnololz.summit.accountUi.PreAuthDialogFragment
import com.idunnololz.summit.alert.AlertDialogFragment
import com.idunnololz.summit.databinding.FragmentMessageBinding
import com.idunnololz.summit.lemmy.LemmyTextHelper
import com.idunnololz.summit.lemmy.MoreActionsViewModel
import com.idunnololz.summit.lemmy.PersonRef
import com.idunnololz.summit.lemmy.PostRef
import com.idunnololz.summit.lemmy.comment.AddOrEditCommentFragment
import com.idunnololz.summit.lemmy.comment.AddOrEditCommentFragmentArgs
import com.idunnololz.summit.lemmy.post.OldThreadLinesDecoration
import com.idunnololz.summit.lemmy.post.PostViewModel
import com.idunnololz.summit.lemmy.post.PostsAdapter
import com.idunnololz.summit.lemmy.postAndCommentView.PostAndCommentViewBuilder
import com.idunnololz.summit.lemmy.postAndCommentView.showMoreCommentOptions
import com.idunnololz.summit.lemmy.postListView.showMorePostOptions
import com.idunnololz.summit.lemmy.utils.VotableRef
import com.idunnololz.summit.lemmy.utils.bind
import com.idunnololz.summit.links.onLinkClick
import com.idunnololz.summit.preferences.Preferences
import com.idunnololz.summit.preview.VideoType
import com.idunnololz.summit.util.BaseFragment
import com.idunnololz.summit.util.StatefulData
import com.idunnololz.summit.util.ext.getColorFromAttribute
import com.idunnololz.summit.util.showBottomMenuForLink
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MessageFragment : BaseFragment<FragmentMessageBinding>() {

    companion object {
        private const val TAG = "MessageFragment"

        private const val CONFIRM_DELETE_COMMENT_TAG = "CONFIRM_DELETE_COMMENT_TAG"
        private const val EXTRA_COMMENT_ID = "EXTRA_COMMENT_ID"
    }

    private val args by navArgs<MessageFragmentArgs>()

    private val viewModel: MessageViewModel by viewModels()
    val actionsViewModel: MoreActionsViewModel by viewModels()
    val inboxViewModel: InboxViewModel by activityViewModels()

    @Inject
    lateinit var postAndCommentViewBuilder: PostAndCommentViewBuilder

    @Inject
    lateinit var accountActionsManager: AccountActionsManager

    @Inject
    lateinit var accountManager: AccountManager

    @Inject
    lateinit var preferences: Preferences

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        super.onCreateView(inflater, container, savedInstanceState)

        setBinding(FragmentMessageBinding.inflate(inflater, container, false))

        requireMainActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    (parentFragment as? InboxTabbedFragment)?.closeMessage()
                }
            },
        )

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val context = requireContext()

        requireMainActivity().apply {
            setupForFragment<InboxTabbedFragment>()

            insetViewExceptBottomAutomaticallyByMargins(viewLifecycleOwner, binding.appBar)
            insetViewExceptTopAutomaticallyByPadding(viewLifecycleOwner, binding.mainContainer)
            insetViewExceptTopAutomaticallyByPadding(viewLifecycleOwner, binding.bottomAppBar)
            insetsChangedLiveData.observe(viewLifecycleOwner) {
                binding.dummyAppBar.updatePadding(bottom = lastInsets.bottomInset * 2)
            }
        }

        binding.toolbar.setNavigationIcon(
            com.google.android.material.R.drawable.ic_arrow_back_black_24,
        )
        binding.toolbar.setNavigationIconTint(
            context.getColorFromAttribute(androidx.appcompat.R.attr.colorControlNormal),
        )

        binding.toolbar.setNavigationOnClickListener {
            (parentFragment as? InboxTabbedFragment)?.closeMessage()
        }

        val inboxItem = args.inboxItem

        when (inboxItem) {
            is InboxItem.MentionInboxItem,
            is InboxItem.MessageInboxItem,
            is InboxItem.ReplyInboxItem,
            -> {
                LemmyTextHelper.bindText(
                    binding.title,
                    inboxItem.title,
                    args.instance,
                    onImageClick = { url ->
                        getMainActivity()?.openImage(null, binding.appBar, null, url, null)
                    },
                    onVideoClick = { url ->
                        getMainActivity()?.openVideo(url, VideoType.UNKNOWN, null)
                    },
                    onPageClick = {
                        getMainActivity()?.launchPage(it)
                    },
                    onLinkClick = { url, text, linkType ->
                        onLinkClick(url, text, linkType)
                    },
                    onLinkLongClick = { url, text ->
                        getMainActivity()?.showBottomMenuForLink(url, text)
                    },
                )
            }
            is InboxItem.ReportCommentInboxItem -> {
                binding.title.setText(R.string.report_on_comment)
            }
            is InboxItem.ReportMessageInboxItem -> {
                binding.title.setText(R.string.report)
            }
            is InboxItem.ReportPostInboxItem -> {
                binding.title.setText(R.string.report_on_post)
            }
        }
        LemmyTextHelper.bindText(
            binding.content,
            inboxItem.content,
            args.instance,
            onImageClick = { url ->
                getMainActivity()?.openImage(null, binding.appBar, null, url, null)
            },
            onVideoClick = { url ->
                getMainActivity()?.openVideo(url, VideoType.UNKNOWN, null)
            },
            onPageClick = {
                getMainActivity()?.launchPage(it)
            },
            onLinkClick = { url, text, linkType ->
                onLinkClick(url, text, linkType)
            },
            onLinkLongClick = { url, text ->
                getMainActivity()?.showBottomMenuForLink(url, text)
            },
        )

        binding.author.text = getString(R.string.from_format, args.inboxItem.authorName)
        binding.author.setOnClickListener {
            getMainActivity()?.launchPage(
                PersonRef.PersonRefByName(
                    args.inboxItem.authorName,
                    args.inboxItem.authorInstance,
                ),
            )
        }

        viewModel.commentContext.observe(viewLifecycleOwner) {
            when (it) {
                is StatefulData.Error -> {
                    binding.contextLoadingView.showDefaultErrorMessageFor(it.error)
                }
                is StatefulData.Loading -> {
                    binding.contextLoadingView.showProgressBar()
                }
                is StatefulData.NotStarted -> {}
                is StatefulData.Success -> {
                    binding.contextLoadingView.hideAll()
                    loadRecyclerView(it.data)
                }
            }
        }

        binding.bottomAppBar.setOnMenuItemClickListener {
            when (it.itemId) {
                R.id.mark_as_unread -> {
                    inboxViewModel.markAsRead(args.inboxItem, read = false)
                    (parentFragment as? InboxTabbedFragment)?.closeMessage()
                    true
                }
                R.id.upvote -> {
                    val commentId = (args.inboxItem as CommentBackedItem).commentId
                    accountActionsManager.vote(args.instance, VotableRef.CommentRef(commentId), 1)
                    true
                }
                R.id.downvote -> {
                    val commentId = (args.inboxItem as CommentBackedItem).commentId
                    accountActionsManager.vote(args.instance, VotableRef.CommentRef(commentId), -1)
                    true
                }
                else -> {
                    false
                }
            }
        }

        binding.bottomAppBar.menu.apply {
            if (inboxItem is ReportItem) {
                findItem(R.id.upvote).isVisible = false
                findItem(R.id.downvote).isVisible = false
            }
        }

        if (inboxItem is ReportItem) {
            binding.fab.setImageResource(R.drawable.baseline_check_24)
            binding.fab.setOnClickListener {
                inboxViewModel.markAsRead(inboxItem, true)
                (parentFragment as? InboxTabbedFragment)?.closeMessage()
            }
        } else {
            binding.fab.setImageResource(R.drawable.baseline_reply_24)
            binding.fab.setOnClickListener {
                if (accountManager.currentAccount.value == null) {
                    PreAuthDialogFragment.newInstance(R.id.action_add_comment)
                        .show(childFragmentManager, "asdf")
                    return@setOnClickListener
                }

                AddOrEditCommentFragment().apply {
                    arguments =
                        AddOrEditCommentFragmentArgs(
                            args.instance,
                            null,
                            null,
                            null,
                            inboxItem,
                        ).toBundle()
                }.show(childFragmentManager, "asdf")
            }
        }

        val upvoteColor = preferences.upvoteColor
        val downvoteColor = preferences.downvoteColor
        val normalTextColor = ContextCompat.getColor(context, R.color.colorText)
        when (inboxItem) {
            is CommentBackedItem -> {
                postAndCommentViewBuilder.voteUiHandler.bind(
                    viewLifecycleOwner,
                    args.instance,
                    inboxItem,
                    null,
                    null,
                    binding.score,
                    null,
                    null,
                    { vote, _, _, _ ->
                        if (vote > 0) {
                            binding.score.setTextColor(upvoteColor)
                        } else if (vote == 0) {
                            binding.score.setTextColor(normalTextColor)
                        } else {
                            binding.score.setTextColor(downvoteColor)
                        }
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
                                    apiInstance,
                                ),
                            )
                            .createAndShow(childFragmentManager, "aa")
                    },
                )

                binding.goToPost.setOnClickListener {
                    getMainActivity()?.launchPage(
                        PostRef(args.instance, inboxItem.postId),
                    )
                }
            }
            is InboxItem.MessageInboxItem -> {
                binding.score.visibility = View.GONE
                binding.bottomAppBar.menu.findItem(R.id.upvote).isEnabled = false
                binding.bottomAppBar.menu.findItem(R.id.downvote).isEnabled = false
            }

            is InboxItem.ReportCommentInboxItem,
            is InboxItem.ReportPostInboxItem,
            is InboxItem.ReportMessageInboxItem,
            -> {
                binding.score.visibility = View.GONE
                binding.bottomAppBar.menu.findItem(R.id.upvote).isEnabled = false
                binding.bottomAppBar.menu.findItem(R.id.downvote).isEnabled = false
            }

            is InboxItem.MentionInboxItem,
            is InboxItem.ReplyInboxItem,
            -> error("THIS SHOULDNT HAPPEN!!!")
        }

        binding.contextCard.setOnClickListener {
            if (viewModel.isContextShowing) {
                viewModel.isContextShowing = false
            } else {
                viewModel.isContextShowing = true
                loadContext()
            }
            updateContextState()
        }

        if (inboxItem is ReportItem) {
            viewModel.isContextShowing = true
            loadContext()
        }

        updateContextState()
    }

    override fun onResume() {
        super.onResume()

        if (preferences.leftHandMode) {
            binding.bottomAppBar.layoutDirection = View.LAYOUT_DIRECTION_RTL
            binding.fabContainer.layoutDirection = View.LAYOUT_DIRECTION_RTL
        } else {
            binding.bottomAppBar.layoutDirection = View.LAYOUT_DIRECTION_INHERIT
        }
    }

    private fun updateContextState() {
        with(binding) {
            if (args.inboxItem !is CommentBackedItem &&
                args.inboxItem !is InboxItem.ReportPostInboxItem &&
                args.inboxItem !is InboxItem.ReportCommentInboxItem
            ) {
                contextCard.visibility = View.GONE
            } else if (viewModel.isContextShowing) {
                indicator.setImageResource(R.drawable.baseline_expand_less_18)
                contextContainer.visibility = View.VISIBLE
                contextLoadingView.visibility = View.VISIBLE
                goToPost.visibility = View.VISIBLE
            } else {
                indicator.setImageResource(R.drawable.baseline_expand_more_18)
                contextContainer.visibility = View.GONE
                contextLoadingView.visibility = View.GONE
                goToPost.visibility = View.GONE
            }
        }
    }

    private fun loadContext(force: Boolean = false) {
        when (val inboxItem = args.inboxItem) {
            is InboxItem.MentionInboxItem -> {
                Log.d(TAG, "Context: " + inboxItem.commentPath)
                viewModel.fetchCommentContext(inboxItem.postId, inboxItem.commentPath, force)
            }
            is InboxItem.MessageInboxItem -> {}
            is InboxItem.ReplyInboxItem -> {
                Log.d(TAG, "Context: " + inboxItem.commentPath)
                viewModel.fetchCommentContext(inboxItem.postId, inboxItem.commentPath, force)
            }
            is InboxItem.ReportMessageInboxItem -> {
                TODO()
            }
            is InboxItem.ReportCommentInboxItem -> {
                viewModel.fetchCommentContext(inboxItem.postId, inboxItem.reportedCommentPath, force)
            }
            is InboxItem.ReportPostInboxItem -> {
                viewModel.fetchCommentContext(inboxItem.reportedPostId, null, force)
            }
        }
    }

    private fun loadRecyclerView(data: MessageViewModel.CommentContext) {
        if (!isBindingAvailable()) return

        val context = requireContext()

        with(binding) {
            val adapter = PostsAdapter(
                postAndCommentViewBuilder = postAndCommentViewBuilder,
                context = context,
                containerView = binding.recyclerView,
                lifecycleOwner = viewLifecycleOwner,
                instance = args.instance,
                revealAll = false,
                useFooter = false,
                isEmbedded = true,
                videoState = null,
                autoCollapseCommentThreshold = preferences.autoCollapseCommentThreshold,
                onRefreshClickCb = {
                    loadContext(force = true)
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
                                apiInstance,
                            ),
                        )
                        .createAndShow(childFragmentManager, "aa")
                },
                onAddCommentClick = { postOrComment ->
                    if (viewModel.accountManager.currentAccount.value == null) {
                        PreAuthDialogFragment.newInstance(R.id.action_add_comment)
                            .show(childFragmentManager, "asdf")
                        return@PostsAdapter
                    }

                    AddOrEditCommentFragment().apply {
                        arguments = postOrComment.fold({
                            AddOrEditCommentFragmentArgs(
                                args.instance,
                                null,
                                it,
                                null,
                            )
                        }, {
                            AddOrEditCommentFragmentArgs(
                                args.instance,
                                it,
                                null,
                                null,
                            )
                        },).toBundle()
                    }.show(childFragmentManager, "asdf")
                },
                onImageClick = { _, view, url ->
                    getMainActivity()?.openImage(view, binding.appBar, null, url, null)
                },
                onVideoClick = { url, videoType, state ->
                    getMainActivity()?.openVideo(url, videoType, state)
                },
                onPageClick = {
                    getMainActivity()?.launchPage(it)
                },
                onPostMoreClick = {
                    showMorePostOptions(viewModel.apiInstance, it, actionsViewModel, childFragmentManager)
                },
                onCommentMoreClick = {
                    showMoreCommentOptions(viewModel.apiInstance, it, actionsViewModel, childFragmentManager)
                },
                onFetchComments = {
                    val postId = when (val inboxItem = args.inboxItem) {
                        is InboxItem.MentionInboxItem -> inboxItem.postId
                        is InboxItem.MessageInboxItem -> return@PostsAdapter
                        is InboxItem.ReplyInboxItem -> inboxItem.postId
                        is InboxItem.ReportMessageInboxItem -> TODO()
                        is InboxItem.ReportCommentInboxItem -> inboxItem.postId
                        is InboxItem.ReportPostInboxItem -> inboxItem.reportedPostId
                    }

                    getMainActivity()?.launchPage(
                        PostRef(args.instance, postId),
                    )
                },
                onLoadPost = {},
                onLinkClick = { url, text, linkType ->
                    onLinkClick(url, text, linkType)
                },
                onLinkLongClick = { url, text ->
                    getMainActivity()?.showBottomMenuForLink(url, text)
                },
            ).apply {
                stateRestorationPolicy = RecyclerView.Adapter.StateRestorationPolicy.PREVENT_WHEN_EMPTY
            }

            adapter.contentMaxWidth = recyclerView.width

            recyclerView.layoutManager = LinearLayoutManager(context)
            recyclerView.adapter = adapter
            recyclerView.addItemDecoration(
                OldThreadLinesDecoration(
                    context,
                    postAndCommentViewBuilder.hideCommentActions,
                ),
            )

            adapter.setData(
                PostViewModel.PostData(
                    PostViewModel.ListView.PostListView(data.post),
                    listOfNotNull(data.commentTree),
                    null,
                    null,
                    false,
                ),
            )

            val commentId = args.inboxItem.commentId
            if (commentId != null) {
                adapter.highlightCommentForever(commentId)
            }
        }
    }
}
