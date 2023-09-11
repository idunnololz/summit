package com.idunnololz.summit.lemmy.comment

import android.graphics.Typeface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.navArgs
import com.idunnololz.summit.databinding.DialogFragmentPreviewCommentBinding
import com.idunnololz.summit.lemmy.LemmyTextHelper
import com.idunnololz.summit.util.BaseDialogFragment

class PreviewCommentDialogFragment : BaseDialogFragment<DialogFragmentPreviewCommentBinding>() {

    private val args by navArgs<PreviewCommentDialogFragmentArgs>()

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

        setBinding(DialogFragmentPreviewCommentBinding.inflate(inflater, container, false))

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (args.showRaw) {
            binding.previewContent.text = args.content
            binding.previewContent.setTextIsSelectable(true)
            binding.previewContent.typeface = Typeface.MONOSPACE
        } else {
            LemmyTextHelper.bindText(
                textView = binding.previewContent,
                text = args.content,
                instance = args.instance,
                onImageClick = {},
                onVideoClick = {},
                onPageClick = {},
                onLinkClick = { _, _, _ -> },
                onLinkLongClick = { _, _ -> },
            )
        }
    }
}
