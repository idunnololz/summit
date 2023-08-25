package com.idunnololz.summit.lemmy.search

import android.content.Context
import android.graphics.RectF
import android.graphics.Typeface
import android.os.Bundle
import android.text.Spannable
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.text.buildSpannedString
import androidx.fragment.app.viewModels
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.Adapter
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import arrow.core.Either
import coil.load
import com.idunnololz.summit.R
import com.idunnololz.summit.account.AccountManager
import com.idunnololz.summit.accountUi.PreAuthDialogFragment
import com.idunnololz.summit.alert.AlertDialogFragment
import com.idunnololz.summit.api.dto.CommentView
import com.idunnololz.summit.api.dto.PostView
import com.idunnololz.summit.api.utils.getUniqueKey
import com.idunnololz.summit.api.utils.instance
import com.idunnololz.summit.databinding.AutoLoadItemBinding
import com.idunnololz.summit.databinding.CommentListCommentItemBinding
import com.idunnololz.summit.databinding.CommentListEndItemBinding
import com.idunnololz.summit.databinding.CommunityItemBinding
import com.idunnololz.summit.databinding.FragmentSearchResultsBinding
import com.idunnololz.summit.databinding.GenericSpaceFooterItemBinding
import com.idunnololz.summit.databinding.LoadingViewItemBinding
import com.idunnololz.summit.databinding.SearchResultPostItemBinding
import com.idunnololz.summit.databinding.UserItemBinding
import com.idunnololz.summit.lemmy.CommentRef
import com.idunnololz.summit.lemmy.CommunityRef
import com.idunnololz.summit.lemmy.LemmyTextHelper
import com.idunnololz.summit.lemmy.LemmyUtils
import com.idunnololz.summit.lemmy.LinkResolver
import com.idunnololz.summit.lemmy.MoreActionsViewModel
import com.idunnololz.summit.lemmy.PageRef
import com.idunnololz.summit.lemmy.PersonRef
import com.idunnololz.summit.lemmy.appendSeparator
import com.idunnololz.summit.lemmy.comment.AddOrEditCommentFragment
import com.idunnololz.summit.lemmy.comment.AddOrEditCommentFragmentArgs
import com.idunnololz.summit.lemmy.postAndCommentView.PostAndCommentViewBuilder
import com.idunnololz.summit.lemmy.postAndCommentView.showMoreCommentOptions
import com.idunnololz.summit.lemmy.postListView.ListingItemViewHolder
import com.idunnololz.summit.lemmy.postListView.PostListViewBuilder
import com.idunnololz.summit.lemmy.postListView.showMorePostOptions
import com.idunnololz.summit.lemmy.toCommunityRef
import com.idunnololz.summit.lemmy.utils.bind
import com.idunnololz.summit.offline.OfflineManager
import com.idunnololz.summit.preview.VideoType
import com.idunnololz.summit.util.BaseFragment
import com.idunnololz.summit.util.CustomDividerItemDecoration
import com.idunnololz.summit.util.CustomLinkMovementMethod
import com.idunnololz.summit.util.DefaultLinkLongClickListener
import com.idunnololz.summit.util.LinkUtils
import com.idunnololz.summit.util.StatefulData
import com.idunnololz.summit.util.ext.appendLink
import com.idunnololz.summit.util.recyclerView.AdapterHelper
import com.idunnololz.summit.util.showBottomMenuForLink
import com.idunnololz.summit.video.VideoState
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@AndroidEntryPoint
class SearchResultsFragment : BaseFragment<FragmentSearchResultsBinding>() {
    private val args by navArgs<SearchResultsFragmentArgs>()

    @Inject
    lateinit var offlineManager: OfflineManager

    @Inject
    lateinit var postAndCommentViewBuilder: PostAndCommentViewBuilder

    @Inject
    lateinit var postListViewBuilder: PostListViewBuilder

    @Inject
    lateinit var accountManager: AccountManager

    val actionsViewModel: MoreActionsViewModel by viewModels()

    private var adapter: SearchResultAdapter? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        super.onCreateView(inflater, container, savedInstanceState)

        setBinding(FragmentSearchResultsBinding.inflate(inflater, container, false))

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val context = requireContext()
        val parentFragment = parentFragment as SearchTabbedFragment
        val queryEngine = parentFragment.viewModel.queryEnginesByType[args.searchType]

        val layoutManager = LinearLayoutManager(context)

        with(binding) {
            adapter = SearchResultAdapter(
                context,
                parentFragment.viewModel.instance,
                viewLifecycleOwner,
                offlineManager,
                postAndCommentViewBuilder,
                postListViewBuilder,
                onLoadPage = {
                    queryEngine?.performQuery(it, force = false)
                },
                onImageClick = { view, url ->
                    getMainActivity()?.openImage(view, parentFragment.binding.appBar, null, url, null)
                },
                onPostImageClick = { postOrCommentView, view, url ->
                    getMainActivity()?.openImage(view, null, null, url, null)
                },
                onVideoClick = { url, videoType, state ->
                    getMainActivity()?.openVideo(url, videoType, state)
                },
                onPageClick = {
                    getMainActivity()?.launchPage(it)
                },
                onAddCommentClick = { postOrComment ->
                    if (accountManager.currentAccount.value == null) {
                        PreAuthDialogFragment.newInstance(R.id.action_add_comment)
                            .show(childFragmentManager, "asdf")
                        return@SearchResultAdapter
                    }

                    AddOrEditCommentFragment().apply {
                        arguments = postOrComment.fold({
                            AddOrEditCommentFragmentArgs(
                                parentFragment.viewModel.instance,
                                null,
                                it,
                                null,
                            )
                        }, {
                            AddOrEditCommentFragmentArgs(
                                parentFragment.viewModel.instance,
                                it,
                                null,
                                null,
                            )
                        },).toBundle()
                    }.show(childFragmentManager, "asdf")
                },
                onCommentMoreClick = {
                    showMoreCommentOptions(
                        instance = parentFragment.viewModel.instance,
                        commentView = it,
                        actionsViewModel = actionsViewModel,
                        fragmentManager = childFragmentManager,
                    )
                },
                onPostMoreClick = {
                    showMorePostOptions(
                        instance = parentFragment.viewModel.instance,
                        postView = it,
                        actionsViewModel = actionsViewModel,
                        fragmentManager = childFragmentManager,
                    )
                },
                onSignInRequired = {
                    PreAuthDialogFragment.newInstance()
                        .show(childFragmentManager, "asdf")
                },
                onInstanceMismatch = { accountInstance, apiInstance ->
                    if (!isBindingAvailable()) return@SearchResultAdapter

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
                onLinkLongClick = { url, text ->
                    getMainActivity()?.showBottomMenuForLink(url, text)
                },
                onCommentClick = {
                    parentFragment.viewPagerController?.openComment(
                        it.instance,
                        it.id,
                    )
                },
                onItemClick = {
                        instance,
                        id,
                        currentCommunity,
                        post,
                        jumpToComments,
                        reveal,
                        videoState, ->

                    parentFragment.viewPagerController?.openPost(
                        instance = instance,
                        id = id,
                        reveal = reveal,
                        post = post,
                        jumpToComments = jumpToComments,
                        currentCommunity = currentCommunity,
                        videoState = videoState,
                    )
                },
            ).apply {
                stateRestorationPolicy = Adapter.StateRestorationPolicy.PREVENT_WHEN_EMPTY
            }

            fun fetchPageIfLoadItem(position: Int) {
                (adapter?.data?.getOrNull(position) as? Item.AutoLoadItem)
                    ?.pageToLoad
                    ?.let {
                        parentFragment.viewModel.loadPage(it)
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

            recyclerView.adapter = adapter
            recyclerView.setHasFixedSize(true)
            recyclerView.layoutManager = layoutManager
            recyclerView.addOnScrollListener(
                object : RecyclerView.OnScrollListener() {
                    override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                        super.onScrolled(recyclerView, dx, dy)

                        checkIfFetchNeeded()
                    }
                },
            )
            recyclerView.addItemDecoration(
                CustomDividerItemDecoration(
                    context,
                    DividerItemDecoration.VERTICAL,
                ).apply {
                    setDrawable(
                        checkNotNull(
                            ContextCompat.getDrawable(
                                context,
                                R.drawable.vertical_divider,
                            ),
                        ),
                    )
                },
            )

            swipeRefreshLayout.setOnRefreshListener {
                parentFragment.viewModel.loadPage(0, force = true)
            }
        }

        lifecycleScope.launch {
            withContext(Dispatchers.Default) {
                queryEngine
                    ?.onItemsChangeFlow
                    ?.collect {
                        if (!isBindingAvailable()) {
                            return@collect
                        }

                        adapter?.setData(queryEngine.getItems()) {
                            if (queryEngine.pageCount == 1) {
                                binding.recyclerView.scrollToPosition(0)
                            }
                        }
                    }
            }
        }

        lifecycleScope.launch {
            withContext(Dispatchers.Default) {
                queryEngine
                    ?.currentState
                    ?.collect {
                        withContext(Dispatchers.Main) {
                            if (!isBindingAvailable()) return@withContext

                            when (it) {
                                is StatefulData.Error,
                                is StatefulData.NotStarted,
                                is StatefulData.Success,
                                -> {
                                    binding.loadingView.hideAll()
                                    binding.swipeRefreshLayout.isRefreshing = false
                                }
                                is StatefulData.Loading -> {
                                    binding.loadingView.showProgressBar()
                                }
                            }
                        }
                    }
            }
        }

        queryEngine?.getItems()?.let {
            adapter?.setData(it) {}
        }
    }

    private class SearchResultAdapter(
        private val context: Context,
        private val instance: String,
        private val viewLifecycleOwner: LifecycleOwner,
        private val offlineManager: OfflineManager,
        private val postAndCommentViewBuilder: PostAndCommentViewBuilder,
        private val postListViewBuilder: PostListViewBuilder,
        private val onLoadPage: (Int) -> Unit,
        private val onImageClick: (View?, String) -> Unit,
        private val onPostImageClick: (PostView, View?, String) -> Unit,
        private val onVideoClick: (url: String, videoType: VideoType, videoState: VideoState?) -> Unit,
        private val onPageClick: (PageRef) -> Unit,
        private val onAddCommentClick: (Either<PostView, CommentView>) -> Unit,
        private val onCommentMoreClick: (CommentView) -> Unit,
        private val onPostMoreClick: (PostView) -> Unit,
        private val onSignInRequired: () -> Unit,
        private val onInstanceMismatch: (String, String) -> Unit,
        private val onLinkLongClick: (url: String, text: String?) -> Unit,
        private val onCommentClick: (CommentRef) -> Unit,
        private val onItemClick: (
            instance: String,
            id: Int,
            currentCommunity: CommunityRef?,
            post: PostView,
            jumpToComments: Boolean,
            reveal: Boolean,
            videoState: VideoState?,
        ) -> Unit,
    ) : Adapter<ViewHolder>() {

        var data: List<Item> = listOf()
            private set

        val regularColor: Int = ContextCompat.getColor(context, R.color.colorText)

        var contentMaxWidth: Int = 0
            set(value) {
                if (value == 0 || value == field) {
                    return
                }

                field = value

                @Suppress("NotifyDataSetChanged")
                notifyDataSetChanged()
            }
        var contentPreferredHeight: Int = 0
        val alwaysRenderAsUnread = true

        /**
         * Set of items that is hidden by default but is reveals (ie. nsfw or spoiler tagged)
         */
        private var revealedItems = mutableSetOf<String>()

        val adapterHelper = AdapterHelper<Item>(
            areItemsTheSame = { old, new ->
                old::class == new::class && when (old) {
                    is Item.AutoLoadItem ->
                        old.pageToLoad == (new as Item.AutoLoadItem).pageToLoad
                    is Item.CommentItem ->
                        old.commentView.comment.id ==
                            (new as Item.CommentItem).commentView.comment.id
                    is Item.CommunityItem ->
                        old.communityView.community.id ==
                            (new as Item.CommunityItem).communityView.community.id
                    is Item.PostItem ->
                        old.postView.post.id ==
                            (new as Item.PostItem).postView.post.id
                    is Item.UserItem ->
                        old.personView.person.id ==
                            (new as Item.UserItem).personView.person.id
                    is Item.ErrorItem ->
                        old.pageToLoad == (new as Item.ErrorItem).pageToLoad
                    Item.EndItem,
                    Item.FooterSpacerItem,
                    -> true
                }
            },
        ).apply {
            addItemType(Item.AutoLoadItem::class, AutoLoadItemBinding::inflate) { item, b, h ->
                b.loadingView.showProgressBar()
            }
            addItemType(
                clazz = Item.CommentItem::class,
                inflateFn = CommentListCommentItemBinding::inflate,
            ) { item, b, _ ->
                val post = item.commentView.post
                val viewHolder = if (b.root.tag == null) {
                    val vh =
                        PostAndCommentViewBuilder.CustomViewHolder(b.root, b.commentButton, b.controlsDivider)
                    b.root.setTag(R.id.custom_view_holder, vh)
                    postAndCommentViewBuilder.ensureContent(vh)
                    vh
                } else {
                    b.root.getTag(R.id.custom_view_holder) as PostAndCommentViewBuilder.CustomViewHolder
                }

                b.postInfo.text = buildSpannedString {
                    appendLink(
                        item.commentView.community.name,
                        LinkUtils.getLinkForCommunity(
                            CommunityRef.CommunityRefByName(
                                item.commentView.community.name,
                                item.commentView.community.instance,
                            ),
                        ),
                    )
                    appendSeparator()

                    val s = length
                    appendLink(
                        post.name,
                        LinkUtils.getLinkForPost(item.instance, post.id),
                        underline = false,
                    )
                    val e = length
                    setSpan(
                        ForegroundColorSpan(regularColor),
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
                b.postInfo.movementMethod = CustomLinkMovementMethod().apply {
                    onLinkLongClickListener = DefaultLinkLongClickListener(context, onLinkLongClick)
                    onLinkClickListener = object : CustomLinkMovementMethod.OnLinkClickListener {
                        override fun onClick(
                            textView: TextView,
                            url: String,
                            text: String,
                            rect: RectF,
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
                        onPageClick = onPageClick,
                        onLinkLongClick = onLinkLongClick,
                    )
                LemmyTextHelper.bindText(
                    textView = b.text,
                    text = item.commentView.comment.content,
                    instance = item.instance,
                    onImageClick = {
                        onImageClick(null, it)
                    },
                    onPageClick = onPageClick,
                    onVideoClick = {
                        onVideoClick(it, VideoType.UNKNOWN, null)
                    },
                    onLinkLongClick = onLinkLongClick,
                )

                postAndCommentViewBuilder.voteUiHandler.bind(
                    requireNotNull(viewLifecycleOwner),
                    item.instance,
                    item.commentView,
                    viewHolder.upvoteButton,
                    viewHolder.downvoteButton,
                    viewHolder.upvoteCount!!,
                    viewHolder.upvoteCount,
                    viewHolder.downvoteCount,
                    null,
                    onSignInRequired,
                    onInstanceMismatch,
                )

                b.commentButton.isEnabled = !post.locked
                b.commentButton.setOnClickListener {
                    onAddCommentClick(Either.Right(item.commentView))
                }
                b.moreButton.setOnClickListener {
                    onCommentMoreClick(item.commentView)
                }
                b.root.setOnClickListener {
                    onCommentClick(CommentRef(item.instance, item.commentView.comment.id))
                }
                b.root.setOnLongClickListener {
                    onCommentMoreClick(item.commentView)
                    true
                }
            }
            addItemType(Item.CommunityItem::class, CommunityItemBinding::inflate) { item, b, h ->
                val community = item.communityView

                b.icon.load(R.drawable.ic_subreddit_default)
                offlineManager.fetchImage(h.itemView, community.community.icon) {
                    b.icon.load(it)
                }

                b.title.text = community.community.name
                val mauString =
                    LemmyUtils.abbrevNumber(community.counts.users_active_month.toLong())

                @Suppress("SetTextI18n")
                b.monthlyActives.text = "(${context.getString(R.string.mau_format, mauString)}) "
                b.instance.text = community.community.instance

                h.itemView.setOnClickListener {
                    onPageClick(community.community.toCommunityRef())
                }
            }
            addItemType(Item.PostItem::class, SearchResultPostItemBinding::inflate) { item, b, h ->
                val h: ListingItemViewHolder = ListingItemViewHolder.fromBinding(b)
                val isRevealed = revealedItems.contains(item.postView.getUniqueKey())
                val isActionsExpanded = true
                val isExpanded = false

                h.root.setTag(R.id.post_view, item.postView)
                h.root.setTag(R.id.swipeable, true)

                postListViewBuilder.bind(
                    holder = h,
                    postView = item.postView,
                    instance = item.instance,
                    isRevealed = isRevealed,
                    contentMaxWidth = contentMaxWidth,
                    contentPreferredHeight = contentPreferredHeight,
                    viewLifecycleOwner = requireNotNull(viewLifecycleOwner),
                    isExpanded = isExpanded,
                    isActionsExpanded = isActionsExpanded,
                    alwaysRenderAsUnread = alwaysRenderAsUnread,
                    updateContent = true,
                    highlight = false,
                    highlightForever = false,
                    onRevealContentClickedFn = {
                        revealedItems.add(item.postView.getUniqueKey())
                        notifyItemChanged(h.absoluteAdapterPosition)
                    },
                    onImageClick = onPostImageClick,
                    onShowMoreOptions = {
                        onPostMoreClick(item.postView)
                    },
                    onVideoClick = onVideoClick,
                    onPageClick = onPageClick,
                    onItemClick = onItemClick,
                    toggleItem = {},
                    toggleActions = {},
                    onSignInRequired = onSignInRequired,
                    onInstanceMismatch = onInstanceMismatch,
                    onHighlightComplete = {},
                    onLinkLongClick = onLinkLongClick,
                )
            }
            addItemType(Item.UserItem::class, UserItemBinding::inflate) { item, b, h ->
                val person = item.personView.person
                b.icon.load(person.avatar)
                b.name.text = person.name
                b.instance.text = person.instance

                b.root.setOnClickListener {
                    onPageClick(PersonRef.PersonRefByName(person.name, person.instance))
                }
            }
            addItemType(Item.ErrorItem::class, LoadingViewItemBinding::inflate) { item, b, h ->
                b.loadingView.showDefaultErrorMessageFor(item.error)
                b.loadingView.setOnRefreshClickListener {
                    onLoadPage(item.pageToLoad)
                }
            }

            addItemType(Item.EndItem::class, CommentListEndItemBinding::inflate) { _, _, _ -> }
            addItemType(
                clazz = Item.FooterSpacerItem::class,
                inflateFn = GenericSpaceFooterItemBinding::inflate,
            ) { _, _, _ -> }
        }

        override fun getItemViewType(position: Int): Int =
            adapterHelper.getItemViewType(position)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
            adapterHelper.onCreateViewHolder(parent, viewType)

        override fun getItemCount(): Int =
            adapterHelper.itemCount

        override fun onBindViewHolder(holder: ViewHolder, position: Int) =
            adapterHelper.onBindViewHolder(holder, position)

        fun setData(newData: List<Item>, cb: () -> Unit) {
            data = newData
            refreshItems(cb)
        }

        private fun refreshItems(cb: () -> Unit) {
            adapterHelper.setItems(data, this, cb)
        }
    }
}
