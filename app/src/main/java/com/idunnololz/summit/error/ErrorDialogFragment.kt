package com.idunnololz.summit.error

import android.os.Bundle
import android.transition.TransitionManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.navArgs
import com.idunnololz.summit.R
import com.idunnololz.summit.databinding.DialogFragmentErrorBinding
import com.idunnololz.summit.util.BaseDialogFragment
import com.idunnololz.summit.util.ext.showAllowingStateLoss
import java.io.PrintWriter
import java.io.StringWriter

class ErrorDialogFragment : BaseDialogFragment<DialogFragmentErrorBinding>() {

    companion object {
        fun show(message: String, error: Throwable, fm: FragmentManager) {
            val sw = StringWriter()
            val pw = PrintWriter(sw)
            error.printStackTrace(pw)
            val stackTrace = sw.toString() // stack trace as a string

            ErrorDialogFragment()
                .apply {
                    arguments = ErrorDialogFragmentArgs(
                        message = message,
                        errorMessage = stackTrace,
                        errorType = error::class.simpleName ?: "UNKNOWN"
                    ).toBundle()
                }
                .showAllowingStateLoss(fm, "ErrorDialogFragment")
        }
    }

    private val args by navArgs<ErrorDialogFragmentArgs>()

    private val viewModel: ErrorDialogViewModel by viewModels()

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
        savedInstanceState: Bundle?
    ): View {
        super.onCreateView(inflater, container, savedInstanceState)

        setBinding(DialogFragmentErrorBinding.inflate(inflater, container, false))

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        with(binding) {
            message.text = args.message

            detailsCard.setOnClickListener {
                viewModel.isExpanded = !viewModel.isExpanded

                updateErrorDetails()
            }
            updateErrorDetails()

            ok.setOnClickListener {
                dismiss()
            }
        }
    }

    private fun updateErrorDetails() {
        if (!isBindingAvailable()) return

        TransitionManager.beginDelayedTransition(binding.root)

        with(binding) {
            if (viewModel.isExpanded) {
                indicator.setImageResource(R.drawable.baseline_expand_less_18)
                contextContainer.visibility = View.VISIBLE

                errorDetails.text = buildString {
                    appendLine(getString(R.string.caused_by_format, args.errorType))
                    append(args.errorMessage)
                }

            } else {
                indicator.setImageResource(R.drawable.baseline_expand_more_18)
                contextContainer.visibility = View.GONE
            }
        }
    }
}