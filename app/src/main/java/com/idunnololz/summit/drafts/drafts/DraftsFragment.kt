package com.idunnololz.summit.drafts.drafts

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.idunnololz.summit.R
import com.idunnololz.summit.alert.newAlertDialogLauncher
import com.idunnololz.summit.databinding.FragmentDraftsBinding
import com.idunnololz.summit.drafts.DraftData
import com.idunnololz.summit.drafts.DraftEntry
import com.idunnololz.summit.drafts.DraftTypes
import com.idunnololz.summit.drafts.DraftsDialogFragment
import com.idunnololz.summit.drafts.DraftsDialogFragmentArgs
import com.idunnololz.summit.drafts.DraftsManager
import com.idunnololz.summit.drafts.DraftsTabbedFragment
import com.idunnololz.summit.lemmy.comment.AddOrEditCommentFragment
import com.idunnololz.summit.lemmy.comment.AddOrEditCommentFragmentArgs
import com.idunnololz.summit.lemmy.createOrEditPost.AddOrEditPostFragment
import com.idunnololz.summit.util.AnimationsHelper
import com.idunnololz.summit.util.BaseFragment
import com.idunnololz.summit.util.FullscreenDialogFragment
import com.idunnololz.summit.util.PrettyPrintUtils
import com.idunnololz.summit.util.ext.setup
import com.idunnololz.summit.util.ext.showAllowingStateLoss
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class DraftsFragment :
  BaseFragment<FragmentDraftsBinding>(),
  FullscreenDialogFragment {

  companion object {
    const val REQUEST_KEY = "DraftsDialogFragment_req_key"
    const val REQUEST_KEY_RESULT = "result"

    fun show(fragmentManager: FragmentManager, draftType: Int) {
      DraftsDialogFragment().apply {
        arguments = DraftsDialogFragmentArgs(draftType).toBundle()
      }.showAllowingStateLoss(fragmentManager, "DraftsDialogFragment")
    }
  }

  private val args by navArgs<DraftsFragmentArgs>()

  private val viewModel: DraftsViewModel by viewModels()

  @Inject
  lateinit var draftsManager: DraftsManager

  @Inject
  lateinit var animationsHelper: AnimationsHelper

  private val deleteDraftDialogLauncher = newAlertDialogLauncher("delete_draft") {
    if (it.isOk) {
      it.extras?.getLong("draft_id")?.let { draftId ->
        viewModel.deleteDraft(draftId)
      }
    }
  }

  private val deleteSelectedDialogLauncher = newAlertDialogLauncher("delete_selected") {
    if (it.isOk) {
      viewModel.deleteAllSelectedDrafts()
    }
  }
  private val deleteAllDialogLauncher = newAlertDialogLauncher("delete_all") {
    if (it.isOk) {
      viewModel.deleteAll(args.draftType)
    }
  }

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?,
  ): View {
    super.onCreateView(inflater, container, savedInstanceState)

    setBinding(FragmentDraftsBinding.inflate(inflater, container, false))

    return binding.root
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)

    val context = requireContext()

    viewModel.draftType = args.draftType

    val onBackPressedCallback = object : OnBackPressedCallback(
      enabled = viewModel.model.value?.isInSelectMode == true
    ) {
      override fun handleOnBackPressed() {
        viewModel.isInSelectMode = false
      }
    }

    requireSummitActivity().onBackPressedDispatcher
      .addCallback(
        viewLifecycleOwner,
        onBackPressedCallback,
      )

    with(binding) {
      val adapter = DraftsAdapter(
        onDraftClick = {
          openDraft(it)
        },
        onDeleteClick = {
          deleteDraftDialogLauncher.launchDialog {
            messageResId = R.string.warn_delete_draft
            positionButtonResId = R.string.delete
            negativeButtonResId = R.string.cancel
            extras.putLong("draft_id", it.id)
          }
        },
        onStartSelectionMode = {
          viewModel.isInSelectMode = true
        },
        onItemSelected = { draftEntry, isSelected ->
          viewModel.markItemAsSelected(draftEntry.id, isSelected)
        }
      )
      val layoutManager = LinearLayoutManager(context)
      recyclerView.adapter = adapter
      recyclerView.layoutManager = layoutManager
      recyclerView.setup(animationsHelper)
      recyclerView.setHasFixedSize(true)

      fun fetchPageIfLoadItem(position: Int) {
        (adapter.model.items.getOrNull(position) as? ViewModelItem.LoadingItem)
          ?.let {
            viewModel.loadMoreDrafts()
          }
      }

      fun checkIfFetchNeeded() {
        val firstPos = layoutManager.findFirstVisibleItemPosition()
        val lastPos = layoutManager.findLastVisibleItemPosition()

        for (i in (firstPos - 1)..(lastPos + 1)) {
          fetchPageIfLoadItem(i)
        }
      }

      viewModel.model.observe(viewLifecycleOwner) {
        swipeRefreshLayout.isRefreshing = false

        adapter.setModel(it) {
          checkIfFetchNeeded()
        }

        onBackPressedCallback.isEnabled = it.isInSelectMode

        if (it.isInSelectMode) {
          if (addFab.isShown || !deleteFab.isShown) {
            addFab.hide()
            deleteFab.show()
          }
        } else {
          if (!addFab.isShown || deleteFab.isShown) {
            addFab.show()
            deleteFab.hide()
          }
        }
      }

      recyclerView.addOnScrollListener(
        object : RecyclerView.OnScrollListener() {
          override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
            super.onScrolled(recyclerView, dx, dy)

            checkIfFetchNeeded()
          }
        },
      )

      swipeRefreshLayout.setOnRefreshListener {
        viewModel.loadMoreDrafts(force = true)
      }

      addFab.setOnClickListener {
        val currentAccount = viewModel.currentAccount ?: return@setOnClickListener

        viewLifecycleOwner.lifecycleScope.launch {
          val draftData = when (viewModel.draftType) {
            DraftTypes.Post ->
              DraftData.PostDraftData(
                null,
                null,
                null,
                null,
                false,
                currentAccount.id,
                currentAccount.instance,
                "",
              )
            DraftTypes.Comment ->
              DraftData.CommentDraftData(
                originalComment = null,
                postRef = null,
                parentCommentId = null,
                content = "",
                accountId = currentAccount.id,
                accountInstance = currentAccount.instance,
              )
            else -> return@launch
          }

          val id = draftsManager.saveDraft(
            draftData = draftData,
            showToast = false,
          )
          val draftEntry = draftsManager.getDraft(id)

          draftEntry.firstOrNull()?.let {
            openDraft(it)
          }
        }
      }

      deleteFab.setOnClickListener {
        deleteSelectedDialogLauncher.launchDialog {
          message = resources.getQuantityString(
            R.plurals.warn_delete_drafts_format,
            viewModel.selectedItemsCount,
            PrettyPrintUtils.defaultDecimalFormat.format(viewModel.selectedItemsCount),
          )
          positionButtonResId = R.string.delete
          negativeButtonResId = R.string.cancel
        }
      }
    }
  }

  fun onSelected() {
  }

  private fun openDraft(draftEntry: DraftEntry) {
    when (draftEntry.data) {
      is DraftData.CommentDraftData -> {
        AddOrEditCommentFragment()
          .apply {
            arguments =
              AddOrEditCommentFragmentArgs(
                instance = viewModel.apiInstance,
                commentView = null,
                postView = null,
                editCommentView = null,
                draft = draftEntry,
              ).toBundle()
          }
          .showAllowingStateLoss(
            childFragmentManager,
            "AddOrEditCommentFragment",
          )
      }
      is DraftData.PostDraftData -> {
        AddOrEditPostFragment.show(
          fragmentManager = childFragmentManager,
          instance = viewModel.apiInstance,
          communityName = draftEntry.data.targetCommunityFullName,
          draft = draftEntry,
        )
      }
      is DraftData.MessageDraftData -> {}
      null -> {}
    }
  }

  fun onToolbarItemSelected(itemId: Int): Boolean {
    when (itemId) {
      R.id.delete_all -> {
        deleteAllDialogLauncher.launchDialog {
          messageResId = R.string.warn_delete_all_drafts
          positionButtonResId = R.string.delete_all
          negativeButtonResId = R.string.cancel
        }
      }
    }
    return true
  }
}
