package com.idunnololz.summit.account_ui

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.idunnololz.summit.R
import com.idunnololz.summit.databinding.DialogFragmentPreAuthBinding
import com.idunnololz.summit.util.BaseDialogFragment

class PreAuthDialogFragment : BaseDialogFragment<DialogFragmentPreAuthBinding>() {

    companion object {
        fun newInstance(): PreAuthDialogFragment = PreAuthDialogFragment()
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val context = requireContext()
        val builder: AlertDialog.Builder = MaterialAlertDialogBuilder(context)

        builder.setTitle(R.string.auth_required_title)

        builder.setMessage(R.string.auth_required_body)

        builder.setPositiveButton(R.string.log_in) { _, _ ->
            dismiss()
            (parentFragment as SignInNavigator).navigateToSignInScreen()
        }
        builder.setNegativeButton(R.string.cancel) { _, _ ->
            dismiss()
        }

        return builder.create().also { dialog ->
        }
    }
}

interface SignInNavigator {
    fun navigateToSignInScreen()
}