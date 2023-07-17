package com.idunnololz.summit.settings.dialogs

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.viewModels
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.idunnololz.summit.R
import com.idunnololz.summit.alert.AlertDialogFragment
import com.idunnololz.summit.databinding.DialogFragmentRichTextValueBinding
import com.idunnololz.summit.databinding.DialogFragmentTextValueBinding
import com.idunnololz.summit.lemmy.comment.AddOrEditCommentViewModel
import com.idunnololz.summit.lemmy.comment.PreviewCommentDialogFragment
import com.idunnololz.summit.lemmy.comment.PreviewCommentDialogFragmentArgs
import com.idunnololz.summit.lemmy.utils.TextFormatterHelper
import com.idunnololz.summit.util.BaseDialogFragment
import com.idunnololz.summit.util.StatefulData
import com.idunnololz.summit.util.ext.showAllowingStateLoss
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class RichTextValueDialogFragment : BaseDialogFragment<DialogFragmentRichTextValueBinding>() {

    companion object {
        private const val ARG_TITLE = "ARG_TITLE"
        private const val ARG_KEY_ID = "ARG_KEY_ID"
        private const val ARG_CURRENT_VALUE = "ARG_CURRENT_VALUE"

        fun newInstance(title: String, key: Int, currentValue: String?) =
            RichTextValueDialogFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_TITLE, title)
                    putInt(ARG_KEY_ID, key)
                    putString(ARG_CURRENT_VALUE, currentValue)
                }
            }
    }

    private val viewModel: RichTextValueViewModel by viewModels()

    private val textFormatterHelper = TextFormatterHelper()
    private val imagePickerLauncher = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        // Callback is invoked after the user selects a media item or closes the
        // photo picker.
        if (uri != null) {
            viewModel.uploadImage(uri)
        }
    }

    override fun onStart() {
        super.onStart()
        val dialog = dialog
        if (dialog != null) {
            val width = ViewGroup.LayoutParams.MATCH_PARENT
            val height = ViewGroup.LayoutParams.MATCH_PARENT

            val window = checkNotNull(dialog.window)
            window.setLayout(width, height)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        super.onCreateView(inflater, container, savedInstanceState)

        setBinding(DialogFragmentRichTextValueBinding.inflate(inflater, container, false))

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        with(binding) {
            textEditor.hint = requireArguments().getString(ARG_TITLE)
            textEditor.setText(requireArguments().getString(ARG_CURRENT_VALUE))

            textFormatterHelper.setupTextFormatterToolbar(
                textFormatToolbar,
                textEditor,
                imagePickerLauncher,
                onPreviewClick = {
                    PreviewCommentDialogFragment()
                        .apply {
                            arguments = PreviewCommentDialogFragmentArgs(
                                viewModel.instance,
                                textEditor.text.toString()
                            ).toBundle()
                        }
                        .showAllowingStateLoss(childFragmentManager, "AA")
                }
            )

            viewModel.uploadImageEvent.observe(viewLifecycleOwner) {
                when (it) {
                    is StatefulData.Error -> {
                        loadingView.hideAll()
                        AlertDialogFragment.Builder()
                            .setMessage(getString(
                                R.string.error_unable_to_send_post,
                                it.error::class.qualifiedName,
                                it.error.message))
                            .createAndShow(childFragmentManager, "ASDS")
                    }
                    is StatefulData.Loading -> {
                        loadingView.showProgressBar()
                    }
                    is StatefulData.NotStarted -> {}
                    is StatefulData.Success -> {
                        loadingView.hideAll()
                        viewModel.uploadImageEvent.clear()

                        textFormatterHelper.onImageUploaded(it.data.url)
                    }
                }
            }

            positiveButton.setOnClickListener {
                dismiss()
                (parentFragment as SettingValueUpdateCallback).updateValue(
                    requireArguments().getInt(ARG_KEY_ID),
                    textEditor.text?.toString(),
                )
            }
            negativeButton.setOnClickListener {
                dismiss()
            }
        }
    }
}