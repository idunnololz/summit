package com.idunnololz.summit.lemmy.person

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.idunnololz.summit.R
import com.idunnololz.summit.account_ui.PreAuthDialogFragment
import com.idunnololz.summit.account_ui.SignInNavigator
import com.idunnololz.summit.alert.AlertDialogFragment
import com.idunnololz.summit.databinding.FragmentPersonPostsBinding
import com.idunnololz.summit.lemmy.community.Item
import com.idunnololz.summit.lemmy.community.ListingItemAdapter
import com.idunnololz.summit.lemmy.postListView.PostListViewBuilder
import com.idunnololz.summit.lemmy.postListView.showMorePostOptions
import com.idunnololz.summit.lemmy.utils.setupDecoratorsForPostList
import com.idunnololz.summit.preferences.Preferences
import com.idunnololz.summit.util.BaseFragment
import com.idunnololz.summit.util.StatefulData
import com.idunnololz.summit.util.showBottomMenuForLink
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class PersonPostsFragment : BaseFragment<FragmentPersonPostsBinding>(), SignInNavigator {

    private var adapter: ListingItemAdapter? = null

    @Inject
    lateinit var postListViewBuilder: PostListViewBuilder
    @Inject
    lateinit var preferences: Preferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val parentFragment = parentFragment as PersonTabbedFragment
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
                        getString(R.string.error_account_instance_mismatch,
                            accountInstance,
                            apiInstance)
                    )
                    .setNegativeButton(R.string.go_to_account_instance)
                    .createAndShow(childFragmentManager, "onInstanceMismatch")
            },
            onImageClick = { postView, sharedElementView, url ->
                getMainActivity()?.openImage(
                    sharedElement = sharedElementView,
                    appBar = parentFragment.binding.appBar,
                    title = postView.post.name,
                    url = url,
                    mimeType = null
                )
            },
            onVideoClick = { url, videoType, state ->
                getMainActivity()?.openVideo(url, videoType, state)
            },
            onPageClick = {
                getMainActivity()?.launchPage(it)
            },
            onItemClick = {
                    instance,
                    id,
                    currentCommunity,
                    post,
                    jumpToComments,
                    reveal,
                    videoState ->

                parentFragment.viewPagerController?.openPost(
                    instance = instance,
                    id =  id,
                    reveal = reveal,
                    post = post,
                    jumpToComments = jumpToComments,
                    currentCommunity = currentCommunity,
                    videoState = videoState,
                )
            },
            onShowMoreActions = {
                showMorePostOptions(parentFragment.viewModel.instance, it, parentFragment.actionsViewModel)
            },
            onPostRead = { postView ->
//                viewModel.onPostRead(postView)
            },
            onLoadPage = {
                viewModel.fetchPage(it)
            },
            onLinkLongClick = { url, text ->
                getMainActivity()?.showBottomMenuForLink(url, text)
            },
        )
        onSelectedLayoutChanged()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
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
            (adapter?.items?.getOrNull(position) as? Item.AutoLoadItem)
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
                    }
                }
            }
        }

        with(binding) {
            recyclerView.layoutManager = layoutManager

            binding.recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
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