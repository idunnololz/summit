package com.idunnololz.summit.lemmy.person

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.idunnololz.summit.R
import com.idunnololz.summit.accountUi.PreAuthDialogFragment
import com.idunnololz.summit.accountUi.SignInNavigator
import com.idunnololz.summit.alert.OldAlertDialogFragment
import com.idunnololz.summit.databinding.FragmentPersonPostsBinding
import com.idunnololz.summit.lemmy.community.PostListEngineItem
import com.idunnololz.summit.lemmy.community.PostListAdapter
import com.idunnololz.summit.lemmy.postListView.PostListViewBuilder
import com.idunnololz.summit.lemmy.postListView.showMorePostOptions
import com.idunnololz.summit.lemmy.utils.actions.MoreActionsHelper
import com.idunnololz.summit.lemmy.utils.setupDecoratorsForPostList
import com.idunnololz.summit.lemmy.utils.showMoreVideoOptions
import com.idunnololz.summit.links.onLinkClick
import com.idunnololz.summit.nsfwMode.NsfwModeManager
import com.idunnololz.summit.preferences.Preferences
import com.idunnololz.summit.util.AnimationsHelper
import com.idunnololz.summit.util.BaseFragment
import com.idunnololz.summit.util.StatefulData
import com.idunnololz.summit.util.ext.setup
import com.idunnololz.summit.util.showMoreLinkOptions
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class PersonPostsFragment : BaseFragment<FragmentPersonPostsBinding>(), SignInNavigator {

    private var adapter: PostListAdapter? = null

    @Inject
    lateinit var postListViewBuilder: PostListViewBuilder

    @Inject
    lateinit var preferences: Preferences

    @Inject
    lateinit var moreActionsHelper: MoreActionsHelper

    @Inject
    lateinit var animationsHelper: AnimationsHelper

    @Inject
    lateinit var nsfwModeManager: NsfwModeManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val parentFragment = parentFragment as PersonTabbedFragment
        val viewModel = parentFragment.viewModel

        adapter = PostListAdapter(
            postListViewBuilder = postListViewBuilder,
            context = requireContext(),
            postListEngine = viewModel.postListEngine,
            onNextClick = {
//                viewModel.fetchNextPage(clearPagePosition = true)
            },
            onPrevClick = {
//                viewModel.fetchPrevPage()
            },
            onSignInRequired = {
                PreAuthDialogFragment.newInstance()
                    .show(childFragmentManager, "asdf")
            },
            onInstanceMismatch = { accountInstance, apiInstance ->
                OldAlertDialogFragment.Builder()
                    .setTitle(R.string.error_account_instance_mismatch_title)
                    .setMessage(
                        getString(
                            R.string.error_account_instance_mismatch,
                            accountInstance,
                            apiInstance,
                        ),
                    )
                    .setNegativeButton(R.string.go_to_account_instance)
                    .createAndShow(childFragmentManager, "onInstanceMismatch")
            },
            onImageClick = { _, postView, sharedElementView, url ->
                val altUrl = if (url == postView.post.thumbnail_url) {
                    postView.post.url
                } else {
                    null
                }

                getMainActivity()?.openImage(
                    sharedElement = sharedElementView,
                    appBar = parentFragment.binding.appBar,
                    title = postView.post.name,
                    url = url,
                    mimeType = null,
                    urlAlt = altUrl,
                )
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
            onPageClick = { accountId, pageRef ->
                getMainActivity()?.launchPage(pageRef)
            },
            onItemClick = {
                    accountId,
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
                    accountId = accountId,
                    videoState = videoState,
                )
            },
            onShowMoreActions = { accountId, postView ->
                showMorePostOptions(
                    instance = parentFragment.viewModel.instance,
                    accountId = accountId,
                    postView = postView,
                    moreActionsHelper = parentFragment.moreActionsHelper,
                    fragmentManager = childFragmentManager,
                )
            },
            onPostRead = { _, _ -> },
            onLoadPage = {
                viewModel.fetchPage(it)
            },
            onLinkClick = { accountId, url, text, linkType ->
                onLinkClick(url, text, linkType)
            },
            onLinkLongClick = { accountId, url, text ->
                getMainActivity()?.showMoreLinkOptions(url, text)
            },
        ).apply {
            alwaysRenderAsUnread = true

            updateWithPreferences(preferences)
            updateNsfwMode(nsfwModeManager)
        }
        onSelectedLayoutChanged()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        super.onCreateView(inflater, container, savedInstanceState)

        setBinding(FragmentPersonPostsBinding.inflate(inflater, container, false))

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val parentFragment = parentFragment as PersonTabbedFragment

        val layoutManager = LinearLayoutManager(context)

        fun fetchPageIfLoadItem(position: Int) {
            (adapter?.items?.getOrNull(position) as? PostListEngineItem.AutoLoadItem)
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

        parentFragment.viewModel.postsState.observe(viewLifecycleOwner) {
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

                    adapter?.onItemsChanged()

                    binding.recyclerView.post {
                        checkIfFetchNeeded()

                        if (it.data.isReset) {
                            layoutManager.scrollToPositionWithOffset(0, 0)
                        }
                    }
                }
            }
        }

        with(binding) {
            recyclerView.layoutManager = layoutManager

            binding.recyclerView.addOnScrollListener(
                object : RecyclerView.OnScrollListener() {
                    override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                        super.onScrolled(recyclerView, dx, dy)

                        checkIfFetchNeeded()
                    }
                },
            )

            swipeRefreshLayout.setOnRefreshListener {
                parentFragment.viewModel.fetchPage(0, true, true)
            }
        }

        runAfterLayout {
            setupView()
        }
    }

    override fun onResume() {
        super.onResume()
        onSelectedLayoutChanged()
    }

    private fun onSelectedLayoutChanged() {
        val newPostUiConfig = preferences.getPostInListUiConfig()
        val didUiConfigChange = postListViewBuilder.postUiConfig != newPostUiConfig
        val didLayoutChange = adapter?.layout != preferences.getPostsLayout()

        if (didLayoutChange) {
            if (isBindingAvailable()) {
                updateDecorator(binding.recyclerView)
            }
        }

        if (didUiConfigChange) {
            postListViewBuilder.postUiConfig = newPostUiConfig
        }

        if (didLayoutChange || didUiConfigChange) {
            adapter?.layout = preferences.getPostsLayout()
        }
    }

    private fun updateDecorator(recyclerView: RecyclerView) {
        recyclerView.setupDecoratorsForPostList(preferences)
    }

    private fun setupView() {
        if (!isBindingAvailable()) return

        with(binding) {
            adapter?.apply {
                contentMaxWidth = recyclerView.width
                contentPreferredHeight = recyclerView.height
                viewLifecycleOwner = this@PersonPostsFragment.viewLifecycleOwner
            }

            recyclerView.setup(animationsHelper)
            recyclerView.adapter = adapter
            recyclerView.setHasFixedSize(true)
            recyclerView.setupDecoratorsForPostList(preferences)
        }
    }

    override fun navigateToSignInScreen() {
        (parentFragment as? SignInNavigator)?.navigateToSignInScreen()
    }

    override fun proceedAnyways(tag: Int) {
        (parentFragment as? SignInNavigator)?.proceedAnyways(tag)
    }
}
