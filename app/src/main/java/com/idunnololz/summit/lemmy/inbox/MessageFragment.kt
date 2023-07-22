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
import com.idunnololz.summit.account_ui.PreAuthDialogFragment
import com.idunnololz.summit.alert.AlertDialogFragment
import com.idunnololz.summit.api.dto.PostView
import com.idunnololz.summit.databinding.FragmentMessageBinding
import com.idunnololz.summit.lemmy.LemmyTextHelper
import com.idunnololz.summit.lemmy.MoreActionsViewModel
import com.idunnololz.summit.lemmy.PersonRef
import com.idunnololz.summit.lemmy.PostRef
import com.idunnololz.summit.lemmy.comment.AddOrEditCommentFragment
import com.idunnololz.summit.lemmy.comment.AddOrEditCommentFragmentArgs
import com.idunnololz.summit.lemmy.createOrEditPost.CreateOrEditPostFragment
import com.idunnololz.summit.lemmy.createOrEditPost.CreateOrEditPostFragmentArgs
import com.idunnololz.summit.lemmy.post.PostViewModel
import com.idunnololz.summit.lemmy.post.PostsAdapter
import com.idunnololz.summit.lemmy.post.OldThreadLinesDecoration
import com.idunnololz.summit.lemmy.postAndCommentView.PostAndCommentViewBuilder
import com.idunnololz.summit.lemmy.postAndCommentView.showMoreCommentOptions
import com.idunnololz.summit.lemmy.postListView.showMorePostOptions
import com.idunnololz.summit.lemmy.utils.VotableRef
import com.idunnololz.summit.lemmy.utils.bind
import com.idunnololz.summit.util.BaseFragment
import com.idunnololz.summit.util.BottomMenu
import com.idunnololz.summit.util.StatefulData
import com.idunnololz.summit.util.ext.getColorFromAttribute
import com.idunnololz.summit.util.ext.showAllowingStateLoss
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

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        super.onCreateView(inflater, container, savedInstanceState)

        setBinding(FragmentMessageBinding.inflate(inflater, container, false))

        requireMainActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    (parentFragment as? InboxTabbedFragment)?.closeMessage()
                }
            })

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
            com.google.android.material.R.drawable.ic_arrow_back_black_24)
        binding.toolbar.setNavigationIconTint(
            context.getColorFromAttribute(androidx.appcompat.R.attr.colorControlNormal))

        binding.toolbar.setNavigationOnClickListener {
            (parentFragment as? InboxTabbedFragment)?.closeMessage()
        }

        val inboxItem = args.inboxItem

        LemmyTextHelper.bindText(
            binding.title,
            inboxItem.title,
            args.instance,
            onImageClick = { url ->
                getMainActivity()?.openImage(null, binding.appBar, null, url, null)
            },
            onPageClick = {
                getMainActivity()?.launchPage(it)
            },
            onLinkLongClick = { url, text ->
                getMainActivity()?.showBottomMenuForLink(url, text)
            },
        )
        LemmyTextHelper.bindText(
            binding.content,
            inboxItem.content,
            args.instance,
            onImageClick = { url ->
                getMainActivity()?.openImage(null, binding.appBar, null, url, null)
            },
            onPageClick = {
                getMainActivity()?.launchPage(it)
            },
            onLinkLongClick = { url, text ->
                getMainActivity()?.showBottomMenuForLink(url, text)
            },
        )

        binding.author.text = getString(R.string.from_format, args.inboxItem.authorName)
        binding.author.setOnClickListener {
            getMainActivity()?.launchPage(
                PersonRef.PersonRefByName(
                    args.inboxItem.authorName, args.inboxItem.authorInstance
                )
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

        binding.fab.setOnClickListener {
            if (accountManager.currentAccount.value == null) {
                PreAuthDialogFragment.newInstance(R.id.action_add_comment)
                    .show(childFragmentManager, "asdf")
                return@setOnClickListener
            }

            AddOrEditCommentFragment().apply {
                arguments =
                    AddOrEditCommentFragmentArgs(
                        args.instance, null, null, null, inboxItem
                    ).toBundle()
            }.show(childFragmentManager, "asdf")
        }

        val upvoteColor = ContextCompat.getColor(context, R.color.upvoteColor)
        val downvoteColor = ContextCompat.getColor(context, R.color.downvoteColor)
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
                    {
                        if (it > 0) {
                            binding.score.setTextColor(upvoteColor)
                        } else if (it == 0) {
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
                                getString(R.string.error_account_instance_mismatch,
                                    accountInstance,
                                    apiInstance)
                            )
                            .createAndShow(childFragmentManager, "aa")
                    },
                )

                binding.goToPost.setOnClickListener {
                    getMainActivity()?.launchPage(
                        PostRef(args.instance, inboxItem.postId)
                    )
                }
            }
            is InboxItem.MessageInboxItem -> {
                binding.score.visibility = View.GONE
                binding.bottomAppBar.menu.findItem(R.id.upvote).isEnabled = false
                binding.bottomAppBar.menu.findItem(R.id.downvote).isEnabled = false
            }

            is InboxItem.MentionInboxItem,
            is InboxItem.ReplyInboxItem -> error("THIS SHOULDNT HAPPEN!!!")
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
        updateContextState()
    }

    private fun updateContextState() {
        with(binding) {
            if (args.inboxItem !is CommentBackedItem) {
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
        }
    }


    private fun getPostMoreMenu(postView: PostView): BottomMenu {
        val bottomMenu = BottomMenu(requireContext()).apply {
            if (postView.post.creator_id == actionsViewModel.accountManager.currentAccount.value?.id) {
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
                    getString(R.string.block_this_user_format, postView.creator.name),
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
                                    instance = args.instance,
                                    post = postView.post,
                                    communityName = null,
                                ).toBundle()
                            }
                            .showAllowingStateLoss(childFragmentManager, "CreateOrEditPostFragment")
                    }
                    R.id.delete -> {
                        actionsViewModel.deletePost(postView.post)
                    }
                    R.id.block_community -> {
                        actionsViewModel.blockCommunity(postView.community.id)
                    }
                    R.id.block_user -> {
                        actionsViewModel.blockPerson(postView.creator.id)
                    }
                }
            }
        }

        return bottomMenu
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
                currentAccountId = viewModel.accountManager.currentAccount.value?.id,
                videoState = null,
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
                            getString(R.string.error_account_instance_mismatch,
                                accountInstance,
                                apiInstance)
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
                                args.instance, null, it, null)
                        }, {
                            AddOrEditCommentFragmentArgs(
                                args.instance, it, null, null)
                        }).toBundle()
                    }.show(childFragmentManager, "asdf")
                },
                onImageClick = { postOrCommentView, view, url ->
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
                    showMorePostOptions(viewModel.apiInstance, it, actionsViewModel)
                },
                onCommentMoreClick = {
                    showMoreCommentOptions(viewModel.apiInstance, it, actionsViewModel)
                },
                onFetchComments = {
                    val postId = when (val inboxItem = args.inboxItem) {
                        is InboxItem.MentionInboxItem -> inboxItem.postId
                        is InboxItem.MessageInboxItem -> return@PostsAdapter
                        is InboxItem.ReplyInboxItem -> inboxItem.postId
                    }

                    getMainActivity()?.launchPage(
                        PostRef(args.instance, postId)
                    )
                },
                onLoadPost = {},
                onLinkLongClick = { url, text ->
                    getMainActivity()?.showBottomMenuForLink(url, text)
                },
            ).apply {
                stateRestorationPolicy = RecyclerView.Adapter.StateRestorationPolicy.PREVENT_WHEN_EMPTY
            }

            adapter.contentMaxWidth = recyclerView.width

            recyclerView.layoutManager = LinearLayoutManager(context)
            recyclerView.adapter = adapter
            recyclerView.addItemDecoration(OldThreadLinesDecoration(
                context, postAndCommentViewBuilder.hideCommentActions))

            adapter.setData(PostViewModel.PostData(
                PostViewModel.ListView.PostListView(data.post),
                listOf(data.commentTree),
                null,
                null,
                false,
            ))

            val commentId = args.inboxItem.commentId
            if (commentId != null) {
                adapter.highlightCommentForever(commentId)
            }
        }
    }
}