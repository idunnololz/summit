package com.idunnololz.summit.lemmy.report

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.navArgs
import arrow.core.Either
import com.idunnololz.summit.R
import com.idunnololz.summit.databinding.DialogFragmentReportContentBinding
import com.idunnololz.summit.error.ErrorDialogFragment
import com.idunnololz.summit.lemmy.CommentRef
import com.idunnololz.summit.lemmy.PostRef
import com.idunnololz.summit.util.BackPressHandler
import com.idunnololz.summit.util.BaseDialogFragment
import com.idunnololz.summit.util.StatefulData
import com.idunnololz.summit.util.ext.showAllowingStateLoss
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ReportContentDialogFragment :
    BaseDialogFragment<DialogFragmentReportContentBinding>(),
    BackPressHandler {

    companion object {
        fun show(
            fragmentManager: FragmentManager,
            postRef: PostRef?,
            commentRef: CommentRef?,
        ) {
            ReportContentDialogFragment()
                .apply {
                    arguments = ReportContentDialogFragmentArgs(postRef, commentRef).toBundle()
                }
                .showAllowingStateLoss(fragmentManager, "ReportContentDialogFragment")
        }
    }

    private val args by navArgs<ReportContentDialogFragmentArgs>()

    private val viewModel: ReportContentViewModel by viewModels()

    override fun onStart() {
        super.onStart()
        val dialog = dialog
        if (dialog != null) {
            val width = ViewGroup.LayoutParams.MATCH_PARENT
            val height = ViewGroup.LayoutParams.WRAP_CONTENT

            val window = checkNotNull(dialog.window)
            window.setLayout(width, height)
        }
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

        if (commentRef == null && postRef == null) {
            dismiss()
            return
        }
        val target = if (postRef != null) {
            Either.Left(postRef)
        } else {
            Either.Right(requireNotNull(commentRef))
        }

        binding.report.setOnClickListener {
            viewModel.sendReport(
                target,
                binding.reasonEditText.text.toString(),
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
                }
                is StatefulData.Loading -> {
                    onSendReportStateChange(isSending = true)
                }
                is StatefulData.NotStarted -> {
                    onSendReportStateChange(isSending = false)
                }
                is StatefulData.Success -> {
                    onSendReportStateChange(isSending = false)
                    dismiss()
                }
            }
        }
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

    override fun onBackPressed(): Boolean {
        if (viewModel.reportState.isLoading) {
            viewModel.cancelSendReport()
            return true
        }

        return false
    }
}
