package com.idunnololz.summit.lemmy.person

import android.content.Context
import android.graphics.RectF
import android.graphics.Typeface
import android.os.Bundle
import android.text.Spannable
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.widget.PopupMenu
import androidx.core.content.ContextCompat
import androidx.core.text.buildSpannedString
import androidx.fragment.app.viewModels
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.Adapter
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import arrow.core.Either
import com.idunnololz.summit.R
import com.idunnololz.summit.account.AccountManager
import com.idunnololz.summit.account_ui.PreAuthDialogFragment
import com.idunnololz.summit.alert.AlertDialogFragment
import com.idunnololz.summit.api.dto.CommentView
import com.idunnololz.summit.api.dto.PersonId
import com.idunnololz.summit.api.dto.PostView
import com.idunnololz.summit.api.utils.instance
import com.idunnololz.summit.databinding.AutoLoadItemBinding
import com.idunnololz.summit.databinding.CommentListCommentItemBinding
import com.idunnololz.summit.databinding.CommentListEndItemBinding
import com.idunnololz.summit.databinding.FragmentPersonCommentsBinding
import com.idunnololz.summit.databinding.LoadingViewItemBinding
import com.idunnololz.summit.lemmy.CommentRef
import com.idunnololz.summit.lemmy.CommunityRef
import com.idunnololz.summit.lemmy.LemmyHeaderHelper
import com.idunnololz.summit.lemmy.LemmyTextHelper
import com.idunnololz.summit.lemmy.LinkResolver
import com.idunnololz.summit.lemmy.MoreActionsViewModel
import com.idunnololz.summit.lemmy.PageRef
import com.idunnololz.summit.lemmy.PostRef
import com.idunnololz.summit.lemmy.appendSeparator
import com.idunnololz.summit.lemmy.comment.AddOrEditCommentFragment
import com.idunnololz.summit.lemmy.comment.AddOrEditCommentFragmentArgs
import com.idunnololz.summit.lemmy.postAndCommentView.PostAndCommentViewBuilder
import com.idunnololz.summit.lemmy.toCommunityRef
import com.idunnololz.summit.lemmy.utils.bind
import com.idunnololz.summit.lemmy.utils.setupDecoratorsForPostList
import com.idunnololz.summit.util.BaseFragment
import com.idunnololz.summit.util.CustomDividerItemDecoration
import com.idunnololz.summit.util.CustomLinkMovementMethod
import com.idunnololz.summit.util.DefaultLinkLongClickListener
import com.idunnololz.summit.util.LinkUtils
import com.idunnololz.summit.util.StatefulData
import com.idunnololz.summit.util.ext.appendLink
import com.idunnololz.summit.util.getParcelableCompat
import com.idunnololz.summit.util.recyclerView.AdapterHelper
import com.idunnololz.summit.util.recyclerView.getBinding
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class PersonCommentsFragment : BaseFragment<FragmentPersonCommentsBinding>(),
    AlertDialogFragment.AlertDialogFragmentListener {

    companion object {
        private const val CONFIRM_DELETE_COMMENT_TAG = "CONFIRM_DELETE_COMMENT_TAG"
        private const val EXTRA_POST_REF = "EXTRA_POST_REF"
        private const val EXTRA_COMMENT_ID = "EXTRA_COMMENT_ID"
    }

    private var adapter: CommentsAdapter? = null
    private val actionsViewModel: MoreActionsViewModel by viewModels()

    @Inject
    lateinit var postAndCommentViewBuilder: PostAndCommentViewBuilder

    @Inject
    lateinit var accountManager: AccountManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val parentFragment = parentFragment as PersonTabbedFragment

        adapter = CommentsAdapter(
            context = requireContext(),
            postAndCommentViewBuilder = postAndCommentViewBuilder,
            currentPersonId = accountManager.currentAccount.value?.id,
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
                            apiInstance)
                    )
                    .createAndShow(childFragmentManager, "aa")
            },
            onAddCommentClick = { postOrComment ->
                if (accountManager.currentAccount.value == null) {
                    PreAuthDialogFragment.newInstance(R.id.action_add_comment)
                        .show(childFragmentManager, "asdf")
                    return@CommentsAdapter
                }

                AddOrEditCommentFragment().apply {
                    arguments = postOrComment.fold({
                        AddOrEditCommentFragmentArgs(
                            parentFragment.viewModel.instance, null, it, null)
                    }, {
                        AddOrEditCommentFragmentArgs(
                            parentFragment.viewModel.instance, it, null, null)
                    }).toBundle()
                }.show(childFragmentManager, "asdf")
            },
            onEditCommentClick = {
                if (accountManager.currentAccount.value == null) {
                    PreAuthDialogFragment.newInstance(R.id.action_edit_comment)
                        .show(childFragmentManager, "asdf")
                    return@CommentsAdapter
                }

                AddOrEditCommentFragment().apply {
                    arguments =
                        AddOrEditCommentFragmentArgs(
                            parentFragment.viewModel.instance,null, null, it).toBundle()
                }.show(childFragmentManager, "asdf")

            },
            onDeleteCommentClick = {
                AlertDialogFragment.Builder()
                    .setMessage(R.string.delete_comment_confirm)
                    .setPositiveButton(android.R.string.ok)
                    .setNegativeButton(android.R.string.cancel)
                    .extras {
                        putParcelable(EXTRA_POST_REF, PostRef(parentFragment.viewModel.instance, it.post.id))
                        putString(EXTRA_COMMENT_ID, it.comment.id.toString())
                    }
                    .createAndShow(
                        childFragmentManager,
                        CONFIRM_DELETE_COMMENT_TAG
                    )

            },
            onImageClick = { view, url ->
                getMainActivity()?.openImage(view, parentFragment.binding.appBar, null, url, null)
            },
            onPageClick = {
                getMainActivity()?.launchPage(it)
            },
            onLoadPage = {
                parentFragment.viewModel.fetchPage(it, false)
            }
        ).apply {
            stateRestorationPolicy = RecyclerView.Adapter.StateRestorationPolicy.PREVENT_WHEN_EMPTY
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        super.onCreateView(inflater, container, savedInstanceState)

        setBinding(FragmentPersonCommentsBinding.inflate(inflater, container, false))

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val parentFragment = parentFragment as PersonTabbedFragment

        val layoutManager = LinearLayoutManager(context)

        fun fetchPageIfLoadItem(position: Int) {
            (adapter?.items?.getOrNull(position) as? CommentsAdapter.Item.AutoLoadItem)
                ?.pageToLoad
                ?.let {
                    parentFragment.viewModel.fetchPage(it)
                }
        }

        fun checkIfFetchNeeded() {
            val firstPos = layoutManager.findFirstVisibleItemPosition()
            val lastPos = layoutManager.findLastVisibleItemPosition()

            fetchPageIfLoadItem(firstPos)
            fetchPageIfLoadItem(firstPos - 1)
            fetchPageIfLoadItem(lastPos)
            fetchPageIfLoadItem(lastPos + 1)
        }

        parentFragment.viewModel.commentsState.observe(viewLifecycleOwner) {
            when (it) {
                is StatefulData.Error -> {
                    binding.swipeRefreshLayout.isRefreshing = false
                    binding.loadingView.showDefaultErrorMessageFor(it.error)
                }
                is StatefulData.Loading ->
                    binding.loadingView.showProgressBar()
                is StatefulData.NotStarted -> {}
                is StatefulData.Success -> {
                    binding.swipeRefreshLayout.isRefreshing = false
                    binding.loadingView.hideAll()

                    adapter?.setData(parentFragment.viewModel.commentPages)

                    binding.root.post {
                        checkIfFetchNeeded()
                    }
                }
            }
        }

        with(binding) {
            recyclerView.layoutManager = layoutManager

            binding.recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    Log.d("HAHA", "asdsafsdfasdfsadf")
                    super.onScrolled(recyclerView, dx, dy)

                    checkIfFetchNeeded()
                }
            })

            swipeRefreshLayout.setOnRefreshListener {
                parentFragment.viewModel.fetchPage(0, true, true)
            }
        }

        runAfterLayout {
            setupView()
        }
    }

    private fun setupView() {
        if (!isBindingAvailable()) return

        with(binding) {
            adapter?.apply {
                viewLifecycleOwner = this@PersonCommentsFragment.viewLifecycleOwner
            }

            recyclerView.adapter = adapter
            recyclerView.setHasFixedSize(true)
            recyclerView.addItemDecoration(
                CustomDividerItemDecoration(
                    recyclerView.context,
                    DividerItemDecoration.VERTICAL
                ).apply {
                    setDrawable(
                        checkNotNull(
                            ContextCompat.getDrawable(
                                context,
                                R.drawable.vertical_divider
                            )
                        )
                    )
                }
            )
        }
    }

    override fun onPositiveClick(dialog: AlertDialogFragment, tag: String?) {
        when (tag) {
            CONFIRM_DELETE_COMMENT_TAG -> {
                val commentId = dialog.getExtra(EXTRA_COMMENT_ID)
                val postRef = dialog.arguments?.getParcelableCompat<PostRef>(EXTRA_POST_REF)
                if (commentId != null && postRef != null) {
                    actionsViewModel.deleteComment(postRef, commentId.toInt())
                }
            }
        }
    }

    override fun onNegativeClick(dialog: AlertDialogFragment, tag: String?) {
    }

    private class CommentsAdapter(
        private val context: Context,
        private val postAndCommentViewBuilder: PostAndCommentViewBuilder,
        var currentPersonId: PersonId?,
        private val onLoadPage: (Int) -> Unit,
        private val onImageClick: (View?, String) -> Unit,
        private val onPageClick: (PageRef) -> Unit,
        private val onAddCommentClick: (Either<PostView, CommentView>) -> Unit,
        private val onEditCommentClick: (CommentView) -> Unit,
        private val onDeleteCommentClick: (CommentView) -> Unit,
        private val onSignInRequired: () -> Unit,
        private val onInstanceMismatch: (String, String) -> Unit,
    ) : Adapter<ViewHolder>() {

        sealed interface Item {
            data class CommentItem(
                val commentView: CommentView,
                val instance: String,
                val pageIndex: Int,
            ) : Item

            data class AutoLoadItem(val pageToLoad: Int) : Item

            data class ErrorItem(val error: Throwable, val pageToLoad: Int) : Item

            object EndItem : Item
        }

        private val regularColor: Int = ContextCompat.getColor(context, R.color.colorText)

        var viewLifecycleOwner: LifecycleOwner? = null
        val items: List<Item>
            get() = adapterHelper.items

        private var commentPages: List<PersonTabbedViewModel.CommentPageResult> = listOf()

        private val adapterHelper = AdapterHelper<Item>(
            areItemsTheSame = { old, new ->
                old::class == new::class && when (old) {
                    is Item.AutoLoadItem ->
                        old.pageToLoad == (new as Item.AutoLoadItem).pageToLoad
                    is Item.CommentItem ->
                        old.commentView.comment.id == (new as Item.CommentItem).commentView.comment.id
                    Item.EndItem -> true
                    is Item.ErrorItem ->
                        old.pageToLoad == (new as Item.ErrorItem).pageToLoad
                }
            }
        ).apply {
            addItemType(Item.AutoLoadItem::class, AutoLoadItemBinding::inflate) { _, b, _ ->
                b.loadingView.showProgressBar()
            }
            addItemType(Item.CommentItem::class, CommentListCommentItemBinding::inflate) { item, b, _ ->
                val post = item.commentView.post
                b.postInfo.text = buildSpannedString {
                    appendLink(
                        item.commentView.community.name,
                        LinkUtils.getLinkForCommunity(
                            CommunityRef.CommunityRefByName(
                                item.commentView.community.name,
                                item.commentView.community.instance))
                    )
                    appendSeparator()

                    val s = length
                    appendLink(
                        post.name,
                        LinkUtils.getLinkForPost(item.instance, post.id),
                        underline = false
                    )
                    val e = length
                    setSpan(
                        ForegroundColorSpan(regularColor),
                        s,
                        e,
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                    setSpan(
                        StyleSpan(Typeface.BOLD),
                        s,
                        e,
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                }
                b.postInfo.movementMethod = CustomLinkMovementMethod().apply {
                    onLinkLongClickListener = DefaultLinkLongClickListener(context)
                    onLinkClickListener = object : CustomLinkMovementMethod.OnLinkClickListener {
                        override fun onClick(
                            textView: TextView,
                            url: String,
                            text: String,
                            rect: RectF
                        ): Boolean {
                            val pageRef = LinkResolver.parseUrl(url, item.instance)

                            return if (pageRef != null) {
                                onPageClick(pageRef)
                                true
                            } else {
                                false
                            }
                        }
                    }
                }
                postAndCommentViewBuilder.lemmyHeaderHelper
                    .populateHeaderSpan(
                        headerContainer = b.headerContainer,
                        item = item.commentView,
                        instance = item.instance,
                        onPageClick = onPageClick
                    )
                LemmyTextHelper.bindText(
                    textView = b.text,
                    text = item.commentView.comment.content,
                    instance = item.instance,
                    onImageClick = {
                        onImageClick(null, it)
                    },
                    onPageClick = onPageClick,
                )

                postAndCommentViewBuilder.voteUiHandler.bind(
                    requireNotNull(viewLifecycleOwner),
                    item.instance,
                    item.commentView,
                    b.upvoteButton,
                    b.downvoteButton,
                    b.upvoteCount,
                    null,
                    onSignInRequired,
                    onInstanceMismatch,
                )

                b.commentButton.isEnabled = !post.locked
                b.commentButton.setOnClickListener {
                    onAddCommentClick(Either.Right(item.commentView))
                }
                b.moreButton.setOnClickListener {
                    PopupMenu(context, it).apply {
                        inflate(R.menu.menu_comment_item)
                        if (item.commentView.creator.id !=
                            currentPersonId) {
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
                                    onEditCommentClick(item.commentView)
                                }
                                R.id.delete_comment -> {
                                    onDeleteCommentClick(item.commentView)
                                }
                            }
                            true
                        }
                    }.show()
                }
                b.root.setOnClickListener {
                    onPageClick(CommentRef(item.instance, item.commentView.comment.id))
                }
            }
            addItemType(Item.EndItem::class, CommentListEndItemBinding::inflate) { _, _, _ -> }
            addItemType(Item.ErrorItem::class, LoadingViewItemBinding::inflate) { item, b, h ->
                b.loadingView.showDefaultErrorMessageFor(item.error)
                b.loadingView.setOnRefreshClickListener {
                    onLoadPage(item.pageToLoad)
                }
            }
        }

        override fun getItemViewType(position: Int): Int =
            adapterHelper.getItemViewType(position)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
            adapterHelper.onCreateViewHolder(parent, viewType)

        override fun getItemCount(): Int = adapterHelper.itemCount

        override fun onBindViewHolder(holder: ViewHolder, position: Int) =
            adapterHelper.onBindViewHolder(holder, position)

        fun setData(commentPages: List<PersonTabbedViewModel.CommentPageResult>) {
            this.commentPages = commentPages

            refreshItems()
        }

        private fun refreshItems() {
            val commentPages = commentPages

            if (commentPages.isEmpty()) return

            val newItems = mutableListOf<Item>()

            for (page in commentPages) {
                for (comment in page.comments) {
                    newItems.add(Item.CommentItem(
                        comment,
                        page.instance,
                        page.pageIndex,
                    ))
                }
            }

            val lastPage = commentPages.last()
            if (lastPage.error != null) {
                newItems.add(Item.ErrorItem(lastPage.error, lastPage.pageIndex + 1))
            } else if (lastPage.hasMore) {
                newItems.add(Item.AutoLoadItem(lastPage.pageIndex + 1))
            } else {
                newItems.add(Item.EndItem)
            }

            adapterHelper.setItems(newItems, this)
        }

    }

}