package com.idunnololz.summit.settings.dialogs

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.idunnololz.summit.R
import com.idunnololz.summit.account_ui.PreAuthDialogFragment
import com.idunnololz.summit.account_ui.SignInNavigator
import com.idunnololz.summit.databinding.DialogFragmentTextValueBinding
import com.idunnololz.summit.util.BaseDialogFragment

class TextValueDialogFragment : BaseDialogFragment<DialogFragmentTextValueBinding>() {

    companion object {
        private const val ARG_TITLE = "ARG_TITLE"
        private const val ARG_KEY_ID = "ARG_KEY_ID"
        private const val ARG_HINT = "ARG_HINT"
        private const val ARG_CURRENT_VALUE = "ARG_CURRENT_VALUE"

        fun newInstance(title: String, key: Int, hint: String?, currentValue: String?) =
            TextValueDialogFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_TITLE, title)
                    putInt(ARG_KEY_ID, key)
                    putString(ARG_HINT, hint)
                    putString(ARG_CURRENT_VALUE, currentValue)
                }
            }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val context = requireContext()
        val builder: AlertDialog.Builder = MaterialAlertDialogBuilder(context)

        val b = DialogFragmentTextValueBinding.inflate(layoutInflater)

        builder.setView(b.root)

        b.inputText.hint = requireArguments().getString(ARG_HINT)
            ?: requireArguments().getString(ARG_TITLE)
        b.inputText.editText?.setText(requireArguments().getString(ARG_CURRENT_VALUE))

        builder
            .setPositiveButton(android.R.string.ok) { _, _ ->
                dismiss()
                (parentFragment as SettingValueUpdateCallback).updateValue(
                    requireArguments().getInt(ARG_KEY_ID),
                    b.inputText.editText?.text?.toString(),
                )
            }
            .setNegativeButton(android.R.string.cancel) { _, _ ->
                dismiss()
            }

        return builder.create()
    }
}