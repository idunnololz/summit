package com.idunnololz.summit.emoji

import android.os.Bundle
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.setFragmentResult
import androidx.navigation.fragment.navArgs
import com.idunnololz.summit.R
import com.idunnololz.summit.databinding.DialogFragmentTextEmojiEditBinding
import com.idunnololz.summit.util.BaseDialogFragment
import com.idunnololz.summit.util.ext.getColorFromAttribute
import com.idunnololz.summit.util.ext.getPlainTextFromClipboard
import com.idunnololz.summit.util.ext.setSizeDynamically
import com.idunnololz.summit.util.getParcelableCompat
import kotlinx.parcelize.Parcelize

class TextEmojiEditDialogFragment : BaseDialogFragment<DialogFragmentTextEmojiEditBinding>() {

    companion object {

        const val REQUEST_KEY = "TextEmojiEditDialogFragment_req"

        private const val KEY_RESULT = "result"

        fun show(fragmentManager: FragmentManager, textEmoji: String, id: Long) =
            TextEmojiEditDialogFragment()
                .apply {
                    arguments = TextEmojiEditDialogFragmentArgs(
                        textEmoji = textEmoji,
                        id = id,
                    ).toBundle()
                }
                .show(fragmentManager, "TextEmojiEditDialogFragment")

        fun getResult(bundle: Bundle): Result? =
            bundle.getParcelableCompat(KEY_RESULT)
    }

    @Parcelize
    data class Result(
        val textEmoji: String,
        val id: Long,
        val delete: Boolean,
    ): Parcelable

    private val args by navArgs<TextEmojiEditDialogFragmentArgs>()

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

        setBinding(
            DialogFragmentTextEmojiEditBinding.inflate(inflater, container, false))

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val context = requireContext()

        with(binding) {
            toolbar.apply {
                title = getString(R.string.edit_text_emoji)
                setNavigationIcon(R.drawable.baseline_close_24)
                setNavigationOnClickListener {
                    dismiss()
                }
                setNavigationIconTint(
                    context.getColorFromAttribute(io.noties.markwon.R.attr.colorControlNormal),
                )
            }

            textEmojiEditText.setText(args.textEmoji)
            textEmojiInputLayout.setEndIconOnClickListener {
                val pasteData: String? = context.getPlainTextFromClipboard()
                if (pasteData != null) {
                    textEmojiEditText.setText(pasteData)
                }
            }

            if (args.id != 0L) {
                neutralButton.setOnClickListener {
                    setFragmentResult(
                        requestKey = REQUEST_KEY,
                        result = bundleOf(
                            KEY_RESULT to Result(
                                textEmoji = textEmojiEditText.text.toString(),
                                id = args.id,
                                delete = true,
                            )
                        )
                    )
                    dismiss()
                }
            } else {
                neutralButton.visibility = View.GONE
            }
            negativeButton.setOnClickListener {
                dismiss()
            }
            positiveButton.setOnClickListener {
                setFragmentResult(
                    requestKey = REQUEST_KEY,
                    result = bundleOf(
                        KEY_RESULT to Result(
                            textEmoji = textEmojiEditText.text.toString(),
                            id = args.id,
                            delete = false,
                        )
                    )
                )
                dismiss()
            }
        }

    }
}