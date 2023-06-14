package com.idunnololz.summit.auth

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import com.idunnololz.summit.R
import com.idunnololz.summit.databinding.DialogFragmentPreAuthBinding
import com.idunnololz.summit.util.BaseDialogFragment

class PreAuthDialogFragment : BaseDialogFragment<DialogFragmentPreAuthBinding>() {

    companion object {
        fun newInstance(): PreAuthDialogFragment = PreAuthDialogFragment()
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val context = requireContext()
        val builder: AlertDialog.Builder = AlertDialog.Builder(context)

        builder.setMessage(R.string.auth_required_title)

        val inflater = LayoutInflater.from(context)
        val rootView = inflater.inflate(R.layout.dialog_fragment_pre_auth, null)
        val textView = rootView.findViewById<TextView>(R.id.text)

        textView.setText(R.string.auth_required_body)

        builder.setView(rootView)

        builder.setPositiveButton(R.string.log_in) { _, _ ->
            RedditAuthManager.instance.showSignInIfNeeded()
        }
        builder.setNegativeButton(R.string.cancel) { _, _ -> }

        return builder.create().also { dialog ->
        }
    }
}