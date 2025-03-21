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
import com.idunnololz.summit.util.ext.setSizeDynamically
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class PreviewCommentDialogFragment : BaseDialogFragment<DialogFragmentPreviewCommentBinding>() {

    private val args by navArgs<PreviewCommentDialogFragmentArgs>()

    @Inject
    lateinit var lemmyTextHelper: LemmyTextHelper

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
            lemmyTextHelper.bindText(
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
