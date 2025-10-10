package com.idunnololz.summit.lemmy.person

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.HapticFeedbackConstantsCompat
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.idunnololz.summit.R
import com.idunnololz.summit.account.AccountManager
import com.idunnololz.summit.accountUi.PreAuthDialogFragment
import com.idunnololz.summit.accountUi.SignInNavigator
import com.idunnololz.summit.alert.launchAlertDialog
import com.idunnololz.summit.databinding.FragmentPersonCommentsBinding
import com.idunnololz.summit.lemmy.LemmyTextHelper
import com.idunnololz.summit.lemmy.PostRef
import com.idunnololz.summit.lemmy.postAndCommentView.PostAndCommentViewBuilder
import com.idunnololz.summit.lemmy.postAndCommentView.createCommentActionHandler
import com.idunnololz.summit.lemmy.utils.CommentListAdapter
import com.idunnololz.summit.lemmy.utils.actions.MoreActionsHelper
import com.idunnololz.summit.links.onLinkClick
import com.idunnololz.summit.preferences.Preferences
import com.idunnololz.summit.util.AnimationsHelper
import com.idunnololz.summit.util.BaseFragment
import com.idunnololz.summit.util.CustomDividerItemDecoration
import com.idunnololz.summit.util.StatefulData
import com.idunnololz.summit.util.ext.performHapticFeedbackCompat
import com.idunnololz.summit.util.ext.setup
import com.idunnololz.summit.util.getParcelableCompat
import com.idunnololz.summit.util.showMoreLinkOptions
import com.idunnololz.summit.util.showProgressBarIfNeeded
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class PersonCommentsFragment :
  BaseFragment<FragmentPersonCommentsBinding>(),
  SignInNavigator {

  private var adapter: CommentListAdapter? = null

  @Inject
  lateinit var moreActionsHelper: MoreActionsHelper

  @Inject
  lateinit var postAndCommentViewBuilder: PostAndCommentViewBuilder

  @Inject
  lateinit var accountManager: AccountManager

  @Inject
  lateinit var preferences: Preferences

  @Inject
  lateinit var animationsHelper: AnimationsHelper

  @Inject
  lateinit var lemmyTextHelper: LemmyTextHelper

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    val parentFragment = parentFragment as PersonTabbedFragment

    adapter = CommentListAdapter(
      context = requireContext(),
      postAndCommentViewBuilder = postAndCommentViewBuilder,
      lemmyTextHelper = lemmyTextHelper,
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
      onCommentActionClick = { view, commentView, actionId ->
        createCommentActionHandler(
          commentView = commentView,
          postRef = PostRef(parentFragment.viewModel.instance, commentView.post.id),
          moreActionsHelper = moreActionsHelper,
          fragmentManager = childFragmentManager,
        )(actionId)

        if (preferences.hapticsOnActions) {
          view.performHapticFeedbackCompat(HapticFeedbackConstantsCompat.CONFIRM)
        }
      },
      onImageClick = { view, url ->
        getMainActivity()?.openImage(
          sharedElement = view,
          appBar = parentFragment.binding.appBar,
          title = null,
          url = url,
          mimeType = null,
        )
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
        parentFragment.viewModel.fetchPage(it, false)
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

    setBinding(FragmentPersonCommentsBinding.inflate(inflater, container, false))

    return binding.root
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)

    with(binding) {
      val parentFragment = parentFragment as PersonTabbedFragment

      val layoutManager = LinearLayoutManager(context)

      fun fetchPageIfLoadItem(position: Int) {
        (adapter?.items?.getOrNull(position) as? CommentListAdapter.Item.AutoLoadItem)
          ?.pageToLoad
          ?.let {
            parentFragment.viewModel.fetchPage(it)
          }
      }

      fun checkIfFetchNeeded() {
        val firstPos = layoutManager.findFirstVisibleItemPosition()
        val lastPos = layoutManager.findLastVisibleItemPosition()

        for (i in (firstPos - 1)..(lastPos + 1)) {
          fetchPageIfLoadItem(i)
        }
      }

      parentFragment.viewModel.commentsState.observe(viewLifecycleOwner) {
        when (it) {
          is StatefulData.Error -> {
            binding.swipeRefreshLayout.isRefreshing = false
            binding.loadingView.showDefaultErrorMessageFor(it.error)
          }

          is StatefulData.Loading ->
            loadingView.showProgressBarIfNeeded(swipeRefreshLayout)

          is StatefulData.NotStarted -> {}
          is StatefulData.Success -> {
            binding.swipeRefreshLayout.isRefreshing = false
            binding.loadingView.hideAll()

            adapter?.setData(parentFragment.viewModel.commentListEngine.commentPages)

            binding.root.post {
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
  }

  override fun onResume() {
    super.onResume()

    postAndCommentViewBuilder.onPreferencesChanged()
  }

  private fun setupView() {
    if (!isBindingAvailable()) return

    with(binding) {
      adapter?.apply {
        viewLifecycleOwner = this@PersonCommentsFragment.viewLifecycleOwner
      }

      recyclerView.setup(animationsHelper)
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

  override fun navigateToSignInScreen() {
    (parentFragment as? SignInNavigator)?.navigateToSignInScreen()
  }

  override fun proceedAnyways(tag: Int) {
    (parentFragment as? SignInNavigator)?.proceedAnyways(tag)
  }
}
