package com.idunnololz.summit.lemmy.comment

import android.os.Bundle
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.setFragmentResult
import androidx.navigation.fragment.navArgs
import com.idunnololz.summit.databinding.DialogFragmentAddLinkBinding
import com.idunnololz.summit.util.BaseDialogFragment
import com.idunnololz.summit.util.ext.getPlainTextFromClipboard
import com.idunnololz.summit.util.ext.showAllowingStateLoss
import kotlinx.parcelize.Parcelize

class AddLinkDialogFragment : BaseDialogFragment<DialogFragmentAddLinkBinding>() {

    companion object {
        const val REQUEST_KEY = "AddLinkDialogFragment_req_key"
        const val REQUEST_KEY_RESULT = "result"

        fun show(prefillText: String?, fragmentManager: FragmentManager) {
            AddLinkDialogFragment()
                .apply {
                    arguments = AddLinkDialogFragmentArgs(prefillText)
                        .toBundle()
                }
                .showAllowingStateLoss(fragmentManager, "AddLinkDialogFragment")
        }
    }

    private val args by navArgs<AddLinkDialogFragmentArgs>()

    @Parcelize
    data class AddLinkResult(
        val text: String,
        val url: String,
    ) : Parcelable

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

        setBinding(DialogFragmentAddLinkBinding.inflate(inflater, container, false))

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val context = requireContext()

        with(binding) {
            if (savedInstanceState == null) {
                args.prefillText?.let {
                    binding.textEditText.setText(it)
                }
            }

            binding.link.setEndIconOnClickListener {
                val pasteData: String? = context.getPlainTextFromClipboard()
                if (pasteData != null) {
                    binding.linkEditText.setText(pasteData)
                }
            }

            positiveButton.setOnClickListener {
                setFragmentResult(
                    REQUEST_KEY,
                    bundleOf(
                        REQUEST_KEY_RESULT to AddLinkResult(
                            textEditText.text.toString(),
                            linkEditText.text.toString(),
                        ),
                    ),
                )
                dismiss()
            }
            negativeButton.setOnClickListener {
                dismiss()
            }
        }
    }
}
