package com.idunnololz.summit.saved

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.idunnololz.summit.R
import com.idunnololz.summit.accountUi.PreAuthDialogFragment
import com.idunnololz.summit.accountUi.SignInNavigator
import com.idunnololz.summit.alert.AlertDialogFragment
import com.idunnololz.summit.api.NotAuthenticatedException
import com.idunnololz.summit.databinding.FragmentSavedPostsBinding
import com.idunnololz.summit.lemmy.community.Item
import com.idunnololz.summit.lemmy.community.ListingItemAdapter
import com.idunnololz.summit.lemmy.postListView.PostListViewBuilder
import com.idunnololz.summit.lemmy.postListView.showMorePostOptions
import com.idunnololz.summit.lemmy.utils.actions.MoreActionsHelper
import com.idunnololz.summit.lemmy.utils.setupDecoratorsForPostList
import com.idunnololz.summit.lemmy.utils.showMoreVideoOptions
import com.idunnololz.summit.links.onLinkClick
import com.idunnololz.summit.preferences.Preferences
import com.idunnololz.summit.util.BaseFragment
import com.idunnololz.summit.util.StatefulData
import com.idunnololz.summit.util.ext.navigateSafe
import com.idunnololz.summit.util.showMoreLinkOptions
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class SavedPostsFragment : BaseFragment<FragmentSavedPostsBinding>(), SignInNavigator {

    private var adapter: ListingItemAdapter? = null

    @Inject
    lateinit var postListViewBuilder: PostListViewBuilder

    @Inject
    lateinit var preferences: Preferences

    @Inject
    lateinit var moreActionsHelper: MoreActionsHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val parentFragment = parentFragment as SavedTabbedFragment
        val viewModel = parentFragment.viewModel

        adapter = ListingItemAdapter(
            postListViewBuilder,
            requireContext(),
            viewModel.postListEngine,
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
                AlertDialogFragment.Builder()
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
                getMainActivity()?.openImage(
                    sharedElement = sharedElementView,
                    appBar = parentFragment.binding.appBar,
                    title = postView.post.name,
                    url = url,
                    mimeType = null,
                )
            },
            onVideoClick = { url, videoType, state ->
                getMainActivity()?.openVideo(url, videoType, state)
            },
            onVideoLongClickListener = { url ->
                showMoreVideoOptions(url, moreActionsHelper, childFragmentManager)
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
                    accountId = null,
                    postView = postView,
                    moreActionsHelper = parentFragment.moreActionsHelper,
                    fragmentManager = childFragmentManager,
                )
            },
            onPostRead = { _, _ -> },
            onLoadPage = {
                viewModel.fetchPostPage(it, false)
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
        }
        onSelectedLayoutChanged()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        super.onCreateView(inflater, container, savedInstanceState)

        setBinding(FragmentSavedPostsBinding.inflate(inflater, container, false))

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val parentFragment = parentFragment as SavedTabbedFragment

        val layoutManager = LinearLayoutManager(context)

        val viewModel = parentFragment.viewModel

        fun fetchPageIfLoadItem(position: Int) {
            (adapter?.items?.getOrNull(position) as? Item.AutoLoadItem)
                ?.pageToLoad
                ?.let {
                    viewModel.fetchPostPage(it, false)
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

        viewModel.postsState.observe(viewLifecycleOwner) {
            when (it) {
                is StatefulData.Error -> {
                    binding.swipeRefreshLayout.isRefreshing = false

                    if (it.error is NotAuthenticatedException) {
                        binding.loadingView.showErrorWithRetry(
                            getString(R.string.please_login_to_view_your_inbox),
                            getString(R.string.login),
                        )
                        binding.loadingView.setOnRefreshClickListener {
                            val direction = SavedTabbedFragmentDirections.actionGlobalLogin()
                            findNavController().navigateSafe(direction)
                        }
                    } else {
                        binding.loadingView.showDefaultErrorMessageFor(it.error)
                        binding.loadingView.setOnRefreshClickListener {
                            viewModel.fetchPostPage(0, true)
                        }
                    }
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
                    }
                }
            }
        }

        with(binding) {
            recyclerView.layoutManager = layoutManager
            recyclerView.addOnScrollListener(
                object : RecyclerView.OnScrollListener() {
                    override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                        super.onScrolled(recyclerView, dx, dy)

                        checkIfFetchNeeded()
                    }
                },
            )

            swipeRefreshLayout.setOnRefreshListener {
                viewModel.fetchPostPage(0, true)
            }
        }

        viewModel.lastSelectedItemLiveData.observe(viewLifecycleOwner) { lastSelectedPost ->
            if (lastSelectedPost != null) {
                lastSelectedPost.fold(
                    {
                        adapter?.highlightPostForever(it)
                    },
                    {
                        adapter?.clearHighlight()
                    },
                )
            } else {
                adapter?.endHighlightForever()
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
                viewLifecycleOwner = this@SavedPostsFragment.viewLifecycleOwner
            }

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
