package com.idunnololz.summit.go_to

import android.app.Dialog
import android.content.ClipDescription.MIMETYPE_TEXT_PLAIN
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.util.Patterns
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageButton
import androidx.appcompat.app.AlertDialog
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.idunnololz.summit.R
import com.idunnololz.summit.databinding.DialogFragmentGoToBinding
import com.idunnololz.summit.reddit.LemmyUtils
import com.idunnololz.summit.util.BaseDialogFragment

class GoToDialogFragment : BaseDialogFragment<DialogFragmentGoToBinding>() {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val context = requireContext()
        val builder: AlertDialog.Builder = AlertDialog.Builder(context)

        val clipboardManager =
            context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager

        builder.setMessage(R.string.go_to_description)

        val inflater = LayoutInflater.from(context)
        val rootView = inflater.inflate(R.layout.dialog_fragment_go_to, null)
        val textField = rootView.findViewById<TextInputLayout>(R.id.outlinedTextField)
        val pasteButton = rootView.findViewById<ImageButton>(R.id.paste)
        val editText = rootView.findViewById<TextInputEditText>(R.id.editText)

        builder.setView(rootView)

        builder.setPositiveButton(android.R.string.ok) { _, _ -> }
        builder.setNegativeButton(android.R.string.cancel) { dialog, _ -> dialog.cancel() }

        val enablePasteButton = when {
            !clipboardManager.hasPrimaryClip() -> {
                false
            }
            !(clipboardManager.primaryClipDescription?.hasMimeType(MIMETYPE_TEXT_PLAIN)
                ?: false) -> {
                // This disables the paste menu item, since the clipboard has data but it is not plain text
                false
            }
            else -> {
                // This enables the paste menu item, since the clipboard contains plain text.
                true
            }
        }
        if (enablePasteButton) {
            pasteButton.visibility = View.VISIBLE
            pasteButton.setOnClickListener {
                editText.setText(clipboardManager.primaryClip?.getItemAt(0)?.text ?: "")
            }
        } else {
            pasteButton.visibility = View.GONE
        }

        return builder.create().also { dialog ->
            dialog.setOnShowListener {
                // Set the positive button click listener so we can intercept and override the auto
                // dismiss
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                    val url = textField.editText?.text.toString().trim()
                    if (Patterns.WEB_URL.matcher(url).matches()) {
                        LemmyUtils.openRedditUrl(requireContext(), url)
                        dismiss()
                    } else {
                        textField.error = getString(R.string.invalid_link)
                    }
                }
            }
        }
    }
}