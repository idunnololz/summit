package com.idunnololz.summit.accountUi

import android.app.Dialog
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.idunnololz.summit.R
import com.idunnololz.summit.databinding.DialogFragmentPreAuthBinding
import com.idunnololz.summit.util.BaseDialogFragment

class PreAuthDialogFragment : BaseDialogFragment<DialogFragmentPreAuthBinding>() {

    companion object {
        private const val ARG_TAG = "ARG_TAG"

        fun newInstance(tag: Int = 0): PreAuthDialogFragment =
            PreAuthDialogFragment()
                .apply {
                    arguments = Bundle().apply {
                        putInt(ARG_TAG, tag)
                    }
                }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val context = requireContext()
        val builder: AlertDialog.Builder = MaterialAlertDialogBuilder(context)

        builder.setTitle(R.string.auth_required_title)

        builder.setMessage(R.string.auth_required_body)

        builder.setPositiveButton(R.string.login) { _, _ ->
            dismiss()
            (parentFragment as SignInNavigator).navigateToSignInScreen()
        }
        builder.setNegativeButton(R.string.cancel) { _, _ ->
            dismiss()
        }

        val tag = arguments?.getInt(ARG_TAG) ?: 0
        if (tag != 0) {
            builder.setNeutralButton(R.string.proceed_anyways) { _, _ ->
                dismiss()
                (parentFragment as SignInNavigator).proceedAnyways(tag)
            }
        }

        return builder.create()
    }
}

interface SignInNavigator {
    fun navigateToSignInScreen()
    fun proceedAnyways(tag: Int)
}
