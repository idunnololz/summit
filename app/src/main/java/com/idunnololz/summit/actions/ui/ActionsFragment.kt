package com.idunnololz.summit.actions.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.idunnololz.summit.databinding.FragmentActionsBinding
import com.idunnololz.summit.lemmy.LemmyTextHelper
import com.idunnololz.summit.links.onLinkClick
import com.idunnololz.summit.util.AnimationsHelper
import com.idunnololz.summit.util.BaseFragment
import com.idunnololz.summit.util.StatefulData
import com.idunnololz.summit.util.ext.setup
import com.idunnololz.summit.util.showMoreLinkOptions
import com.idunnololz.summit.util.showProgressBarIfNeeded
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class ActionsFragment : BaseFragment<FragmentActionsBinding>() {
  private val args by navArgs<ActionsFragmentArgs>()

  enum class ActionType {
    Completed,
    Pending,
    Failed,
  }

  @Inject
  lateinit var animationsHelper: AnimationsHelper

  @Inject
  lateinit var lemmyTextHelper: LemmyTextHelper

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?,
  ): View {
    super.onCreateView(inflater, container, savedInstanceState)

    setBinding(FragmentActionsBinding.inflate(inflater, container, false))

    return binding.root
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)

    val context = requireContext()

    with(binding) {
      val parentFragment = parentFragment as ActionsTabbedFragment
      val viewModel = parentFragment.viewModel
      val adapter = ActionsAdapter(
        context = context,
        lemmyTextHelper = lemmyTextHelper,
        onImageClick = { postView, sharedElementView, url ->
          getMainActivity()?.openImage(
            sharedElement = sharedElementView,
            appBar = null,
            title = null,
            url = url,
            mimeType = null,
          )
        },
        onVideoClick = { url, videoType, state ->
          getMainActivity()?.openVideo(url, videoType, state)
        },
        onPageClick = { url, pageRef ->
          getMainActivity()?.launchPage(pageRef, url = url)
        },
        onLinkClick = { url, text, linkType ->
          onLinkClick(url, text, linkType)
        },
        onLinkLongClick = { url, text ->
          getMainActivity()?.showMoreLinkOptions(url, text)
        },
        onActionClick = {
          viewModel.markActionAsSeen(it)
          parentFragment.openActionDetails(it)
//                ActionDetailsDialogFragment.show(
//                    fragmentManager = parentFragmentManager,
//                    action = it,
//                )
        },
      ).apply {
        stateRestorationPolicy = RecyclerView.Adapter.StateRestorationPolicy.PREVENT_WHEN_EMPTY
      }

      viewModel.actionsDataLiveData.observe(viewLifecycleOwner) {
        when (it) {
          is StatefulData.Error -> {
            binding.swipeRefreshLayout.isRefreshing = false
            binding.loadingView.showDefaultErrorMessageFor(it.error)
          }

          is StatefulData.Loading -> {
            binding.loadingView.showProgressBarIfNeeded(swipeRefreshLayout)
          }

          is StatefulData.NotStarted -> {}
          is StatefulData.Success -> {
            binding.swipeRefreshLayout.isRefreshing = false
            binding.loadingView.hideAll()

            adapter.accountDictionary = it.data.accountDictionary

            when (args.actionType) {
              ActionType.Completed -> {
                adapter.actions = it.data.completedActions
              }

              ActionType.Pending -> {
                adapter.actions = it.data.pendingActions
              }

              ActionType.Failed -> {
                adapter.actions = it.data.failedActions
              }
            }
          }
        }
      }

      swipeRefreshLayout.setOnRefreshListener {
        viewModel.loadActions()
      }

      recyclerView.adapter = adapter
      recyclerView.layoutManager = LinearLayoutManager(context)
      recyclerView.setup(animationsHelper)
      recyclerView.setHasFixedSize(true)
      fastScroller.setRecyclerView(binding.recyclerView)
    }
  }
}
