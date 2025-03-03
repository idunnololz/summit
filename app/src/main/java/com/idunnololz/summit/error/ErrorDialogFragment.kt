package com.idunnololz.summit.error

import android.os.Bundle
import android.transition.TransitionManager
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.navArgs
import com.idunnololz.summit.R
import com.idunnololz.summit.databinding.DialogFragmentErrorBinding
import com.idunnololz.summit.util.BaseDialogFragment
import com.idunnololz.summit.util.Utils
import com.idunnololz.summit.util.ext.setSizeDynamically
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

            Log.d("ErrorDialogFragment", "Displaying error dialog for error.", error)

            ErrorDialogFragment()
                .apply {
                    arguments = ErrorDialogFragmentArgs(
                        message = message,
                        errorMessage = error.message ?: stackTrace,
                        errorType = error::class.simpleName ?: "UNKNOWN",
                    ).toBundle()
                }
                .showAllowingStateLoss(fm, "ErrorDialogFragment")
        }
    }

    private val args by navArgs<ErrorDialogFragmentArgs>()

    private val viewModel: ErrorDialogViewModel by viewModels()

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

        setBinding(DialogFragmentErrorBinding.inflate(inflater, container, false))

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val context = requireContext()

        with(binding) {
            title.text = args.message
            message.text = args.errorMessage

            // Show the details card if we want to show even more info in the future
            detailsCard.visibility = View.GONE
            detailsCard.setOnClickListener {
                viewModel.isExpanded = !viewModel.isExpanded

                updateErrorDetails()
            }
            updateErrorDetails()

            ok.setOnClickListener {
                dismiss()
            }
            copy.setOnClickListener {
                Utils.copyToClipboard(context, args.errorMessage)
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
