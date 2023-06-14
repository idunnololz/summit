package com.idunnololz.summit.comment

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import com.idunnololz.summit.R
import com.idunnololz.summit.databinding.DialogFragmentCommentFeatureBinding
import com.idunnololz.summit.util.BaseDialogFragment

class CommentFeatureDialogFragment : BaseDialogFragment<DialogFragmentCommentFeatureBinding>() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val context = requireContext()
        val builder: AlertDialog.Builder = AlertDialog.Builder(context)

        //builder.setMessage(R.string.raw_comment)

        val inflater = LayoutInflater.from(context)
        val rootView = inflater.inflate(R.layout.dialog_fragment_comment_feature, null)
        val textView = rootView.findViewById<TextView>(R.id.text)

        builder.setView(rootView)

        builder.setPositiveButton(android.R.string.ok) { _, _ -> }


        textView.setText(R.string.comments_coming_soon)

        return builder.create().also { dialog ->
        }
    }
}