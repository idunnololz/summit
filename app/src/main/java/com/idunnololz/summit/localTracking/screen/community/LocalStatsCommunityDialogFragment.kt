package com.idunnololz.summit.localTracking.screen.community

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.viewModels
import androidx.lifecycle.LifecycleOwner
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.idunnololz.summit.R
import com.idunnololz.summit.accountUi.PreAuthDialogFragment
import com.idunnololz.summit.alert.launchAlertDialog
import com.idunnololz.summit.api.utils.getUniqueKey
import com.idunnololz.summit.api.utils.instance
import com.idunnololz.summit.databinding.DialogFragmentLocalStatsCommunityBinding
import com.idunnololz.summit.databinding.LocalStatsCommunityEmptyItemBinding
import com.idunnololz.summit.databinding.LocalStatsCommunityPostListItemBinding
import com.idunnololz.summit.databinding.LocalStatsCommunitySummaryItemBinding
import com.idunnololz.summit.lemmy.CommunityRef
import com.idunnololz.summit.lemmy.PageRef
import com.idunnololz.summit.lemmy.postListView.ListingItemViewHolder
import com.idunnololz.summit.lemmy.postListView.OnImageClickCallback
import com.idunnololz.summit.lemmy.postListView.PostListViewBuilder
import com.idunnololz.summit.lemmy.postListView.createPostActionHandler
import com.idunnololz.summit.lemmy.toPostHeaderInfo
import com.idunnololz.summit.lemmy.utils.actions.MoreActionsHelper
import com.idunnololz.summit.links.LinkContext
import com.idunnololz.summit.links.onLinkClick
import com.idunnololz.summit.localTracking.screen.community.LocalStatsCommunityModel.PostModelItem
import com.idunnololz.summit.models.PostView
import com.idunnololz.summit.preview.VideoType
import com.idunnololz.summit.util.BaseDialogFragment
import com.idunnololz.summit.util.PrettyPrintUtils
import com.idunnololz.summit.util.StatefulData
import com.idunnololz.summit.util.recyclerView.AdapterHelper
import com.idunnololz.summit.util.setupToolbar
import com.idunnololz.summit.util.showMoreLinkOptions
import com.idunnololz.summit.util.toFileDownloadContext
import com.idunnololz.summit.video.VideoState
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlin.getValue

@AndroidEntryPoint
class LocalStatsCommunityDialogFragment : BaseDialogFragment<DialogFragmentLocalStatsCommunityBinding>() {

  companion object {
    fun show(supportFragmentManager: FragmentManager, communityRef: CommunityRef) {
      LocalStatsCommunityDialogFragment()
        .apply {
          arguments = LocalStatsCommunityDialogFragmentArgs(communityRef).toBundle()
        }
        .show(supportFragmentManager, "LocalStatsCommunityDialogFragment")
    }
  }

  private val args by navArgs<LocalStatsCommunityDialogFragmentArgs>()
  private val viewModel: LocalStatsCommunityViewModel by viewModels()

  @Inject
  lateinit var postListViewBuilder: PostListViewBuilder

  @Inject
  lateinit var moreActionsHelper: MoreActionsHelper

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?,
  ): View {
    super.onCreateView(inflater, container, savedInstanceState)

    setBinding(DialogFragmentLocalStatsCommunityBinding.inflate(inflater, container, false))

    return binding.root
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)

    requireSummitActivity().apply {
      setupToolbar(
        binding.toolbar,
        context.getString(R.string.local_community_stats),
      )
    }

    viewModel.generateModel(communityRef = args.communityRef, force = false)

    val context = requireContext()
    val adapter = CommunityLocalStatItemsAdapter(
      context = context,
      viewLifecycleOwner = viewLifecycleOwner,
      postListViewBuilder = postListViewBuilder,
      fetchPost = { postId, force -> viewModel.fetchPost(postId, force) },
      onPostImageClick = { _, postView, view, url, altUrl, peek ->
        getMainActivity()?.openImage(
          sharedElement = view,
          appBar = null,
          title = null,
          url = url,
          mimeType = null,
          downloadContext = postView.toFileDownloadContext(),
          peek = peek,
          urlAlt = altUrl,
        )
      },
      onPostActionClick = { postView, actionId ->
        createPostActionHandler(
          accountId = null,
          postView = postView,
          moreActionsHelper = moreActionsHelper,
          fragmentManager = childFragmentManager,
          getMainActivity = { getMainActivity() },
        )(actionId)
      },
      onVideoClick = { url, videoType, state ->
//        getMainActivity()?.openVideo(
//          url = url,
//          videoType = videoType,
//          videoState = state,
//          downloadContext = postView.toFileDownloadContext(),
//        )
      },
      onVideoLongClickListener = { url ->
//        showMoreVideoOptions(
//          url = url,
//          originalUrl = url,
//          moreActionsHelper = moreActionsHelper,
//          fragmentManager = childFragmentManager,
//        )
      },
      onPageClick = { url, pageRef ->
        getMainActivity()?.launchPage(pageRef, url = url)
      },
      onItemClick = {
          instance,
          id,
          currentCommunity,
          post,
          jumpToComments,
          reveal,
          videoState,
        ->

//        parentFragment.slidingPaneController?.openPost(
//          instance = instance,
//          id = id,
//          reveal = reveal,
//          post = post,
//          jumpToComments = jumpToComments,
//          currentCommunity = currentCommunity,
//          accountId = accountId,
//          videoState = videoState,
//        )
      },
      onSignInRequired = {
        PreAuthDialogFragment.newInstance()
          .show(childFragmentManager, "asdf")
      },
      onInstanceMismatch = { accountInstance, apiInstance ->
        launchAlertDialog("instance_mismatch") {
          titleResId = R.string.error_account_instance_mismatch_title
          message = getString(
            R.string.error_account_instance_mismatch,
            accountInstance,
            apiInstance,
          )
        }
      },
      onLinkClick = { url, text, linkType ->
        onLinkClick(url, text, linkType)
      },
      onLinkLongClick = { postView, url, text ->
        getMainActivity()?.showMoreLinkOptions(url, text, postView.toFileDownloadContext())
      },
    )

    binding.recyclerView.setHasFixedSize(true)
    binding.recyclerView.layoutManager = LinearLayoutManager(context)
    binding.recyclerView.adapter = adapter

    viewModel.data.observe(viewLifecycleOwner) {
      when (it) {
        is StatefulData.Error -> {
          binding.loadingView.showDefaultErrorMessageFor(it.error)
        }
        is StatefulData.Loading -> {
          binding.loadingView.showProgressBar()
        }
        is StatefulData.NotStarted -> {
          binding.loadingView.hideAll()
        }
        is StatefulData.Success -> {
          binding.loadingView.hideAll()

          adapter.data = it.data

        }
      }
    }

    binding.root.post {
      adapter.contentMaxWidth = binding.recyclerView.width
    }
  }

  private class CommunityLocalStatItemsAdapter(
    private val context: Context,
    private val viewLifecycleOwner: LifecycleOwner,
    private val postListViewBuilder: PostListViewBuilder,
    private val fetchPost: (Long, Boolean) -> Unit,
    private val onPostImageClick: OnImageClickCallback,
    private val onPostActionClick: (PostView, actionId: Int) -> Unit,
    private val onVideoClick: (
      url: String,
      videoType: VideoType,
      videoState: VideoState?,
    ) -> Unit,
    private val onVideoLongClickListener: (url: String) -> Unit,
    private val onPageClick: (url: String, PageRef) -> Unit,
    private val onItemClick: (
      instance: String,
      id: Int,
      currentCommunity: CommunityRef?,
      post: PostView,
      jumpToComments: Boolean,
      reveal: Boolean,
      videoState: VideoState?,
    ) -> Unit,
    private val onSignInRequired: () -> Unit,
    private val onInstanceMismatch: (String, String) -> Unit,
    private val onLinkClick: (url: String, text: String?, linkContext: LinkContext) -> Unit,
    private val onLinkLongClick: (post: PostView, url: String, text: String?) -> Unit,
  ) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    /**
     * Set of items that is hidden by default but is reveals (ie. nsfw or spoiler tagged)
     */
    private var revealedItems = mutableSetOf<String>()

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

    var data: LocalStatsCommunityModel? = null
      set(value) {
        field = value

        refreshItems()
      }

    private val adapterHelper = AdapterHelper<LocalStatsCommunityModel.Item>(
      { old, new ->
        old::class == new::class && when (old) {
          is LocalStatsCommunityModel.CommunityStatsSummaryItem ->
            true
          is PostModelItem ->
            old.postId == (new as PostModelItem).postId
          LocalStatsCommunityModel.NoPostsModelItem ->
            true
        }
      }
    ).apply {
      addItemType(
        clazz = LocalStatsCommunityModel.CommunityStatsSummaryItem::class,
        inflateFn = LocalStatsCommunitySummaryItemBinding::inflate
      ) { item, b, h ->
        @Suppress("SetTextI18n") // positive number sign
        b.score.text =
          if (item.totalScore > 0) {
            "+"
          } else {
            ""
          } + PrettyPrintUtils.defaultDecimalFormat.format(item.totalScore)
        val totalVotes = item.upvotes + item.downvotes
        val upvotePercent = if (totalVotes == 0) {
          0f
        } else {
          item.upvotes.toFloat() / totalVotes
        }
        b.scorePercent.text = PrettyPrintUtils.defaultPercentFormat.format(upvotePercent)

        @Suppress("SetTextI18n") // positive number sign
        b.scoreRecent.text =
          if (item.recentTotalScore > 0) {
            "+"
          } else {
            ""
          } + PrettyPrintUtils.defaultDecimalFormat.format(item.recentTotalScore)
        val recentTotalVotes = item.recentUpvotes + item.recentDownvotes
        val recentUpvotePercent = if (recentTotalVotes == 0) {
          0f
        } else {
          item.recentUpvotes.toFloat() / recentTotalVotes
        }
        b.scoreRecentPercent.text = PrettyPrintUtils.defaultPercentFormat.format(recentUpvotePercent)

        b.views.text = PrettyPrintUtils.defaultDecimalFormat.format(item.totalViewsCount)
        b.recentViews.text = context.getString(
          R.string.last_7_days_format,
          PrettyPrintUtils.defaultDecimalFormat.format(item.recentViewsCount)
        )
      }
      addItemType(
        clazz = LocalStatsCommunityModel.NoPostsModelItem::class,
        inflateFn = LocalStatsCommunityEmptyItemBinding::inflate
      ) { item, b, h ->
      }
      addItemType(
        clazz = PostModelItem::class,
        inflateFn = LocalStatsCommunityPostListItemBinding::inflate
      ) { item, b, h ->
        val h = b.root.getTag(R.id.view_holder) as? ListingItemViewHolder ?: run {
          ListingItemViewHolder.fromBinding(b.content).also {
            b.root.setTag(R.id.view_holder, it)
          }
        }

        val fetchedPost = item.fetchedPost?.getOrNull()

        if (item.postId == null) {
          b.content.root.visibility = View.GONE
          b.loadingView.showErrorWithRetry(R.string.error_unknown_post)
        } else if (item.fetchedPost == null) {
          b.content.root.visibility = View.GONE
          fetchPost(item.postId, false)
          b.loadingView.showProgressBar()
        } else if (fetchedPost != null) {
          b.content.root.visibility = View.VISIBLE
          b.loadingView.hideAll(makeViewGone = true)

          val postView = fetchedPost.postView
          val isRevealed = revealedItems.contains(postView.getUniqueKey())
          val isActionsExpanded = true
          val isExpanded = false

          h.root.setTag(R.id.fetched_post, fetchedPost)
          h.root.setTag(R.id.swipeable, true)

          postListViewBuilder.bind(
            holder = h,
            currentCommunity = null,
            fetchedPost = fetchedPost,
            instance = postView.instance,
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
            postHeaderInfo = postView.toPostHeaderInfo(context),
            onRevealContentClickedFn = {
              revealedItems.add(postView.getUniqueKey())
              notifyItemChanged(h.absoluteAdapterPosition)
            },
            onImageClick = onPostImageClick,
            onShowMoreOptions = { _, postView ->
              onPostActionClick(postView, R.id.pa_more)
            },
            onVideoClick = onVideoClick,
            onVideoLongClickListener = onVideoLongClickListener,
            onPageClick = { accountId, url, pageRef ->
              onPageClick(url, pageRef)
            },
            onItemClick = {
                _: Long?,
                instance: String,
                id: Int,
                currentCommunity: CommunityRef?,
                post: PostView,
                jumpToComments: Boolean,
                reveal: Boolean,
                videoState: VideoState?,
              ->

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
              onLinkLongClick(postView, url, text)
            },
            onPostActionClick = onPostActionClick,
          )
        } else {
          b.content.root.visibility = View.GONE
          b.loadingView.showDefaultErrorMessageFor(
            item.fetchedPost.exceptionOrNull() ?: RuntimeException()
          )
          b.loadingView.setOnRefreshClickListener {
            fetchPost(item.postId, true)
          }
        }
      }
    }

    override fun getItemViewType(position: Int): Int =
      adapterHelper.getItemViewType(position)

    override fun onCreateViewHolder(
      parent: ViewGroup,
      viewType: Int,
    ): RecyclerView.ViewHolder =
      adapterHelper.onCreateViewHolder(parent, viewType)

    override fun onBindViewHolder(
      holder: RecyclerView.ViewHolder,
      position: Int,
    ) =
      adapterHelper.onBindViewHolder(holder, position)

    override fun getItemCount(): Int =
      adapterHelper.itemCount

    private fun refreshItems() {
      adapterHelper.setItems(newItems = data?.items ?: listOf(), adapter = this)
    }
  }
}