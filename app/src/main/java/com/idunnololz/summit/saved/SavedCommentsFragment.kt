package com.idunnololz.summit.saved

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.idunnololz.summit.R
import com.idunnololz.summit.account.AccountManager
import com.idunnololz.summit.accountUi.PreAuthDialogFragment
import com.idunnololz.summit.accountUi.SignInNavigator
import com.idunnololz.summit.alert.AlertDialogFragment
import com.idunnololz.summit.api.NotAuthenticatedException
import com.idunnololz.summit.databinding.FragmentSavedCommentsBinding
import com.idunnololz.summit.lemmy.PostRef
import com.idunnololz.summit.lemmy.postAndCommentView.PostAndCommentViewBuilder
import com.idunnololz.summit.lemmy.postAndCommentView.createCommentActionHandler
import com.idunnololz.summit.lemmy.utils.CommentListAdapter
import com.idunnololz.summit.lemmy.utils.actions.MoreActionsHelper
import com.idunnololz.summit.links.onLinkClick
import com.idunnololz.summit.util.BaseFragment
import com.idunnololz.summit.util.CustomDividerItemDecoration
import com.idunnololz.summit.util.StatefulData
import com.idunnololz.summit.util.ext.navigateSafe
import com.idunnololz.summit.util.getParcelableCompat
import com.idunnololz.summit.util.showMoreLinkOptions
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class SavedCommentsFragment :
    BaseFragment<FragmentSavedCommentsBinding>(),
    AlertDialogFragment.AlertDialogFragmentListener,
    SignInNavigator {

    companion object {
        private const val CONFIRM_DELETE_COMMENT_TAG = "CONFIRM_DELETE_COMMENT_TAG"
        private const val EXTRA_POST_REF = "EXTRA_POST_REF"
        private const val EXTRA_COMMENT_ID = "EXTRA_COMMENT_ID"
    }

    private var adapter: CommentListAdapter? = null

    @Inject
    lateinit var moreActionsHelper: MoreActionsHelper

    @Inject
    lateinit var postAndCommentViewBuilder: PostAndCommentViewBuilder

    @Inject
    lateinit var accountManager: AccountManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val parentFragment = parentFragment as SavedTabbedFragment

        adapter = CommentListAdapter(
            context = requireContext(),
            postAndCommentViewBuilder = postAndCommentViewBuilder,
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
            onImageClick = { view, url ->
                getMainActivity()?.openImage(view, parentFragment.binding.appBar, null, url, null)
            },
            onVideoClick = { url, videoType, state ->
                getMainActivity()?.openVideo(url, videoType, state)
            },
            onPageClick = {
                getMainActivity()?.launchPage(it)
            },
            onCommentClick = {
                parentFragment.slidingPaneController?.openComment(
                    it.instance,
                    it.id,
                )
            },
            onLoadPage = {
                parentFragment.viewModel.fetchCommentPage(it, false)
            },
            onCommentActionClick = { commentView, actionId ->
                createCommentActionHandler(
                    instance = parentFragment.viewModel.instance,
                    commentView = commentView,
                    moreActionsHelper = moreActionsHelper,
                    fragmentManager = childFragmentManager,
                )(actionId)
            },
            onLinkClick = { url, text, linkType ->
                onLinkClick(url, text, linkType)
            },
            onLinkLongClick = { url, text ->
                getMainActivity()?.showMoreLinkOptions(url, text)
            },
        ).apply {
            stateRestorationPolicy = RecyclerView.Adapter.StateRestorationPolicy.PREVENT_WHEN_EMPTY
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        super.onCreateView(inflater, container, savedInstanceState)

        setBinding(FragmentSavedCommentsBinding.inflate(inflater, container, false))

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val parentFragment = parentFragment as SavedTabbedFragment

        val viewModel = parentFragment.viewModel

        val layoutManager = LinearLayoutManager(context)

        fun fetchPageIfLoadItem(position: Int) {
            (adapter?.items?.getOrNull(position) as? CommentListAdapter.Item.AutoLoadItem)
                ?.pageToLoad
                ?.let {
                    viewModel.fetchCommentPage(it)
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

        viewModel.commentsState.observe(viewLifecycleOwner) {
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
                            viewModel.fetchCommentPage(0, true)
                        }
                    }
                }
                is StatefulData.Loading ->
                    binding.loadingView.showProgressBar()
                is StatefulData.NotStarted -> {}
                is StatefulData.Success -> {
                    binding.swipeRefreshLayout.isRefreshing = false
                    binding.loadingView.hideAll()

                    adapter?.setData(viewModel.commentListEngine.commentPages)

                    binding.root.post {
                        checkIfFetchNeeded()
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
                viewModel.fetchCommentPage(0, true)
            }
        }

        viewModel.lastSelectedItemLiveData.observe(viewLifecycleOwner) { lastSelectedPost ->
            if (lastSelectedPost != null) {
                lastSelectedPost.fold(
                    {
                        adapter?.onHighlightComplete()
                    },
                    {
                        adapter?.highlightForever(it)
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

        postAndCommentViewBuilder.onPreferencesChanged()
    }

    private fun setupView() {
        if (!isBindingAvailable()) return

        with(binding) {
            adapter?.apply {
                viewLifecycleOwner = this@SavedCommentsFragment.viewLifecycleOwner
            }

            recyclerView.adapter = adapter
            recyclerView.setHasFixedSize(true)
            recyclerView.addItemDecoration(
                CustomDividerItemDecoration(
                    recyclerView.context,
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
        }
    }

    override fun onPositiveClick(dialog: AlertDialogFragment, tag: String?) {
        when (tag) {
            CONFIRM_DELETE_COMMENT_TAG -> {
                val commentId = dialog.getExtra(EXTRA_COMMENT_ID)
                val postRef = dialog.arguments?.getParcelableCompat<PostRef>(EXTRA_POST_REF)
                if (commentId != null && postRef != null) {
                    moreActionsHelper.deleteComment(postRef, commentId.toInt())
                }
            }
        }
    }

    override fun onNegativeClick(dialog: AlertDialogFragment, tag: String?) {
    }

    override fun navigateToSignInScreen() {
        (parentFragment as? SignInNavigator)?.navigateToSignInScreen()
    }

    override fun proceedAnyways(tag: Int) {
        (parentFragment as? SignInNavigator)?.proceedAnyways(tag)
    }
}
