package com.idunnololz.summit.lemmy.report

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.navArgs
import com.idunnololz.summit.R
import com.idunnololz.summit.databinding.DialogFragmentReportContentBinding
import com.idunnololz.summit.error.ErrorDialogFragment
import com.idunnololz.summit.lemmy.CommentRef
import com.idunnololz.summit.lemmy.PostRef
import com.idunnololz.summit.lemmy.inbox.conversation.MessageItem
import com.idunnololz.summit.util.BaseDialogFragment
import com.idunnololz.summit.util.StatefulData
import com.idunnololz.summit.util.ext.setSizeDynamically
import com.idunnololz.summit.util.ext.showAllowingStateLoss
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ReportContentDialogFragment :
  BaseDialogFragment<DialogFragmentReportContentBinding>() {

  companion object {
    fun show(fragmentManager: FragmentManager, postRef: PostRef?, commentRef: CommentRef?) {
      ReportContentDialogFragment()
        .apply {
          arguments = ReportContentDialogFragmentArgs(
            postRef = postRef,
            commentRef = commentRef,
            messageItem = null,
          ).toBundle()
        }
        .showAllowingStateLoss(fragmentManager, "ReportContentDialogFragment")
    }

    fun show(fragmentManager: FragmentManager, messageItem: MessageItem) {
      ReportContentDialogFragment()
        .apply {
          arguments = ReportContentDialogFragmentArgs(
            postRef = null,
            commentRef = null,
            messageItem = messageItem,
          ).toBundle()
        }
        .showAllowingStateLoss(fragmentManager, "ReportContentDialogFragment")
    }
  }

  private val args by navArgs<ReportContentDialogFragmentArgs>()

  private val viewModel: ReportContentViewModel by viewModels()

  private val unsavedChangesBackPressedHandler = object : OnBackPressedCallback(false) {
    override fun handleOnBackPressed() {
      viewModel.cancelSendReport()
    }
  }

  override fun onStart() {
    super.onStart()
    setSizeDynamically(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
  }

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?,
  ): View {
    super.onCreateView(inflater, container, savedInstanceState)

    setBinding(DialogFragmentReportContentBinding.inflate(inflater, container, false))

    return binding.root
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)

    val commentRef = args.commentRef
    val postRef = args.postRef
    val messageItem = args.messageItem

    if (commentRef == null && postRef == null && messageItem == null) {
      dismiss()
      return
    }

    binding.report.setOnClickListener {
      viewModel.sendReport(
        postRef = postRef,
        commentRef = commentRef,
        messageItem = messageItem,
        reason = binding.reasonEditText.text.toString(),
      )
    }
    binding.cancel.setOnClickListener {
      if (viewModel.reportState.isLoading) {
        viewModel.cancelSendReport()
      } else {
        dismiss()
      }
    }

    viewModel.reportState.observe(viewLifecycleOwner) {
      when (it) {
        is StatefulData.Error -> {
          onSendReportStateChange(isSending = false)
          ErrorDialogFragment.show(
            getString(R.string.error_unable_to_send_report),
            it.error,
            childFragmentManager,
          )

          unsavedChangesBackPressedHandler.isEnabled = false
        }
        is StatefulData.Loading -> {
          onSendReportStateChange(isSending = true)

          unsavedChangesBackPressedHandler.isEnabled = true
        }
        is StatefulData.NotStarted -> {
          onSendReportStateChange(isSending = false)

          unsavedChangesBackPressedHandler.isEnabled = false
        }
        is StatefulData.Success -> {
          unsavedChangesBackPressedHandler.isEnabled = false

          onSendReportStateChange(isSending = false)
          dismiss()
        }
      }
    }

    onBackPressedDispatcher.addCallback(
      viewLifecycleOwner,
      unsavedChangesBackPressedHandler,
    )
  }

  private fun onSendReportStateChange(isSending: Boolean) {
    if (!isBindingAvailable()) return

    with(binding) {
      reason.isEnabled = !isSending
      report.isEnabled = !isSending

      if (isSending) {
        loadingView.showProgressBar()
      } else {
        loadingView.hideAll()
      }
    }
  }
}
