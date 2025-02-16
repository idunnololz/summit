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
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.Adapter
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import arrow.core.Either
import com.idunnololz.summit.R
import com.idunnololz.summit.account.AccountManager
import com.idunnololz.summit.accountUi.PreAuthDialogFragment
import com.idunnololz.summit.alert.OldAlertDialogFragment
import com.idunnololz.summit.api.dto.CommentView
import com.idunnololz.summit.api.dto.PostView
import com.idunnololz.summit.api.utils.getUniqueKey
import com.idunnololz.summit.api.utils.instance
import com.idunnololz.summit.avatar.AvatarHelper
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
import com.idunnololz.summit.lemmy.PageRef
import com.idunnololz.summit.lemmy.appendSeparator
import com.idunnololz.summit.lemmy.comment.AddOrEditCommentFragment
import com.idunnololz.summit.lemmy.postAndCommentView.GeneralQuickActionsViewHolder
import com.idunnololz.summit.lemmy.postAndCommentView.PostAndCommentViewBuilder
import com.idunnololz.summit.lemmy.postAndCommentView.createCommentActionHandler
import com.idunnololz.summit.lemmy.postListView.ListingItemViewHolder
import com.idunnololz.summit.lemmy.postListView.PostListViewBuilder
import com.idunnololz.summit.lemmy.postListView.createPostActionHandler
import com.idunnololz.summit.lemmy.toCommunityRef
import com.idunnololz.summit.lemmy.toPersonRef
import com.idunnololz.summit.lemmy.utils.actions.MoreActionsHelper
import com.idunnololz.summit.lemmy.utils.bind
import com.idunnololz.summit.lemmy.utils.showMoreVideoOptions
import com.idunnololz.summit.links.LinkContext
import com.idunnololz.summit.links.LinkResolver
import com.idunnololz.summit.links.onLinkClick
import com.idunnololz.summit.offline.OfflineManager
import com.idunnololz.summit.preview.VideoType
import com.idunnololz.summit.util.AnimationsHelper
import com.idunnololz.summit.util.BaseFragment
import com.idunnololz.summit.util.CustomDividerItemDecoration
import com.idunnololz.summit.util.CustomLinkMovementMethod
import com.idunnololz.summit.util.DefaultLinkLongClickListener
import com.idunnololz.summit.util.LinkUtils
import com.idunnololz.summit.util.StatefulData
import com.idunnololz.summit.util.ext.appendLink
import com.idunnololz.summit.util.ext.setup
import com.idunnololz.summit.util.isLoading
import com.idunnololz.summit.util.recyclerView.AdapterHelper
import com.idunnololz.summit.util.showMoreLinkOptions
import com.idunnololz.summit.video.VideoState
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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

    @Inject
    lateinit var moreActionsHelper: MoreActionsHelper

    @Inject
    lateinit var animationsHelper: AnimationsHelper

    @Inject
    lateinit var avatarHelper: AvatarHelper

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
                context = context,
                instance = parentFragment.viewModel.instance,
                viewLifecycleOwner = viewLifecycleOwner,
                offlineManager = offlineManager,
                postAndCommentViewBuilder = postAndCommentViewBuilder,
                postListViewBuilder = postListViewBuilder,
                avatarHelper = avatarHelper,
                onLoadPage = {
                    queryEngine?.performQuery(it, force = false)
                },
                onImageClick = { view, url ->
                    getMainActivity()?.openImage(view, parentFragment.binding.appBar, null, url, null)
                },
                onPostImageClick = { _, _, view, url ->
                    getMainActivity()?.openImage(view, null, null, url, null)
                },
                onVideoClick = { url, videoType, state ->
                    getMainActivity()?.openVideo(url, videoType, state)
                },
                onVideoLongClickListener = { url ->
                    showMoreVideoOptions(
                        url = url,
                        originalUrl = url,
                        moreActionsHelper = moreActionsHelper,
                        fragmentManager = childFragmentManager,
                    )
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

                    AddOrEditCommentFragment.showReplyDialog(
                        instance = parentFragment.viewModel.instance,
                        postOrCommentView = postOrComment,
                        fragmentManager = childFragmentManager,
                        accountId = null,
                    )
                },
                onPostActionClick = { postView, actionId ->
                    createPostActionHandler(
                        instance = parentFragment.viewModel.instance,
                        accountId = null,
                        postView = postView,
                        moreActionsHelper = moreActionsHelper,
                        fragmentManager = childFragmentManager,
                    )(actionId)
                },
                onCommentActionClick = { commentView, actionId ->
                    createCommentActionHandler(
                        apiInstance = parentFragment.viewModel.instance,
                        commentView = commentView,
                        moreActionsHelper = moreActionsHelper,
                        fragmentManager = childFragmentManager,
                    )(actionId)
                },
                onSignInRequired = {
                    PreAuthDialogFragment.newInstance()
                        .show(childFragmentManager, "asdf")
                },
                onInstanceMismatch = { accountInstance, apiInstance ->
                    if (!isBindingAvailable()) return@SearchResultAdapter

                    OldAlertDialogFragment.Builder()
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
                onLinkClick = { url, text, linkType ->
                    onLinkClick(url, text, linkType)
                },
                onLinkLongClick = { url, text ->
                    getMainActivity()?.showMoreLinkOptions(url, text)
                },
                onCommentClick = {
                    parentFragment.slidingPaneController?.openComment(
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

                    parentFragment.slidingPaneController?.openPost(
                        instance = instance,
                        id = id,
                        reveal = reveal,
                        post = post,
                        jumpToComments = jumpToComments,
                        currentCommunity = currentCommunity,
                        accountId = null,
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

            recyclerView.setup(animationsHelper)
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
                            if (queryEngine.pageCount == 1 && !queryEngine.currentState.value.isLoading()) {
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
                        withContext(Dispatchers.Main) a@{
                            if (!isBindingAvailable()) return@a

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
        private val avatarHelper: AvatarHelper,
        private val onLoadPage: (Int) -> Unit,
        private val onImageClick: (View?, String) -> Unit,
        private val onPostImageClick: (accountId: Long?, PostView, View?, String) -> Unit,
        private val onVideoClick: (
            url: String,
            videoType: VideoType,
            videoState: VideoState?,
        ) -> Unit,
        private val onVideoLongClickListener: (url: String) -> Unit,
        private val onPageClick: (PageRef) -> Unit,
        private val onAddCommentClick: (Either<PostView, CommentView>) -> Unit,
        private val onPostActionClick: (PostView, actionId: Int) -> Unit,
        private val onCommentActionClick: (CommentView, actionId: Int) -> Unit,
        private val onSignInRequired: () -> Unit,
        private val onInstanceMismatch: (String, String) -> Unit,
        private val onLinkClick: (url: String, text: String?, linkContext: LinkContext) -> Unit,
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
                        old.fetchedPost.postView.post.id ==
                            (new as Item.PostItem).fetchedPost.postView.post.id
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

            addItemType(Item.AutoLoadItem::class, AutoLoadItemBinding::inflate) { _, b, _ ->
                b.loadingView.showProgressBar()
            }
            addItemType(
                clazz = Item.CommentItem::class,
                inflateFn = CommentListCommentItemBinding::inflate,
            ) { item, b, _ ->
                val post = item.commentView.post
                val viewHolder = b.root.getTag(R.id.view_holder) as? GeneralQuickActionsViewHolder
                    ?: run {
                        val vh = GeneralQuickActionsViewHolder(
                            root = b.root,
                            quickActionsTopBarrier = b.text,
                        )
                        b.root.setTag(R.id.view_holder, vh)
                        vh
                    }

                b.highlightBg.visibility = View.GONE
                postAndCommentViewBuilder.ensureCommentsActionButtons(
                    vh = viewHolder,
                    root = viewHolder.root,
                    isSaved = item.commentView.saved,
                )

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
                postAndCommentViewBuilder.populateHeaderSpan(
                    headerContainer = b.headerContainer,
                    commentView = item.commentView,
                    instance = item.instance,
                    onPageClick = onPageClick,
                    onLinkClick = onLinkClick,
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
                        onVideoClick(it, VideoType.Unknown, null)
                    },
                    onLinkClick = onLinkClick,
                    onLinkLongClick = onLinkLongClick,
                )

                val scoreCount: TextView? = viewHolder.qaScoreCount
                val upvoteCount: TextView?
                val downvoteCount: TextView?

                if (scoreCount != null) {
                    if (viewHolder.qaDownvoteCount != null) {
                        upvoteCount = viewHolder.qaUpvoteCount
                        downvoteCount = viewHolder.qaDownvoteCount
                    } else {
                        upvoteCount = null
                        downvoteCount = null
                    }
                    postAndCommentViewBuilder.voteUiHandler.bind(
                        lifecycleOwner = viewLifecycleOwner,
                        instance = item.instance,
                        commentView = item.commentView,
                        upVoteView = viewHolder.upvoteButton,
                        downVoteView = viewHolder.downvoteButton,
                        scoreView = scoreCount,
                        upvoteCount = upvoteCount,
                        downvoteCount = downvoteCount,
                        accountId = null,
                        onUpdate = null,
                        onSignInRequired = onSignInRequired,
                        onInstanceMismatch = onInstanceMismatch,
                    )
                }

                viewHolder.actionButtons.forEach {
                    it.setOnClickListener {
                        onCommentActionClick(item.commentView, it.id)
                    }
                    if (it.id == R.id.ca_reply) {
                        it.isEnabled = !item.commentView.post.locked
                    }
                }
                b.root.setOnClickListener {
                    onCommentClick(CommentRef(item.instance, item.commentView.comment.id))
                }
                b.root.setOnLongClickListener {
                    onCommentActionClick(item.commentView, R.id.ca_more)
                    true
                }
            }
            addItemType(Item.CommunityItem::class, CommunityItemBinding::inflate) { item, b, h ->
                val community = item.communityView

                avatarHelper.loadCommunityIcon(b.icon, community.community)

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
            addItemType(Item.PostItem::class, SearchResultPostItemBinding::inflate) { item, b, _ ->
                val h = b.root.getTag(R.id.view_holder) as? ListingItemViewHolder ?: run {
                    ListingItemViewHolder.fromBinding(b).also {
                        b.root.setTag(R.id.view_holder, it)
                    }
                }
                val isRevealed = revealedItems.contains(item.fetchedPost.postView.getUniqueKey())
                val isActionsExpanded = true
                val isExpanded = false

                h.root.setTag(R.id.fetched_post, item.fetchedPost)
                h.root.setTag(R.id.swipeable, true)

                postListViewBuilder.bind(
                    holder = h,
                    fetchedPost = item.fetchedPost,
                    instance = item.instance,
                    isRevealed = isRevealed,
                    contentMaxWidth = contentMaxWidth,
                    contentPreferredHeight = contentPreferredHeight,
                    viewLifecycleOwner = viewLifecycleOwner,
                    isExpanded = isExpanded,
                    isActionsExpanded = isActionsExpanded,
                    alwaysRenderAsUnread = alwaysRenderAsUnread,
                    updateContent = true,
                    highlight = false,
                    highlightForever = false,
                    themeColor = null,
                    isDuplicatePost = false,
                    onRevealContentClickedFn = {
                        revealedItems.add(item.fetchedPost.postView.getUniqueKey())
                        notifyItemChanged(h.absoluteAdapterPosition)
                    },
                    onImageClick = onPostImageClick,
                    onShowMoreOptions = { _, postView ->
                        onPostActionClick(postView, R.id.pa_more)
                    },
                    onVideoClick = onVideoClick,
                    onVideoLongClickListener = onVideoLongClickListener,
                    onPageClick = { accountId, pageRef ->
                        onPageClick(pageRef)
                    },
                    onItemClick = { accountId: Long?, instance: String, id: Int, currentCommunity: CommunityRef?, post: PostView, jumpToComments: Boolean, reveal: Boolean, videoState: VideoState? ->
                        onItemClick(
                            instance,
                            id,
                            currentCommunity,
                            post,
                            jumpToComments,
                            reveal,
                            videoState,
                        )
                    },
                    toggleItem = {},
                    toggleActions = {},
                    onSignInRequired = onSignInRequired,
                    onInstanceMismatch = onInstanceMismatch,
                    onHighlightComplete = {},
                    onLinkClick = { accountId, url, text, linkContext ->
                        onLinkClick(url, text, linkContext)
                    },
                    onLinkLongClick = { accountId, url, text ->
                        onLinkLongClick(url, text)
                    },
                )
            }
            addItemType(Item.UserItem::class, UserItemBinding::inflate) { item, b, _ ->
                val person = item.personView.person

                avatarHelper.loadAvatar(b.icon, person)

                b.name.text = person.name
                b.instance.text = person.instance

                b.root.setOnClickListener {
                    onPageClick(person.toPersonRef())
                }
            }
            addItemType(Item.ErrorItem::class, LoadingViewItemBinding::inflate) { item, b, _ ->
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

        override fun getItemViewType(position: Int): Int = adapterHelper.getItemViewType(position)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
            adapterHelper.onCreateViewHolder(parent, viewType)

        override fun getItemCount(): Int = adapterHelper.itemCount

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
