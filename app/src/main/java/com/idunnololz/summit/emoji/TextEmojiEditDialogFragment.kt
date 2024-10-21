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
    ): Parcelable

    private val args by navArgs<TextEmojiEditDialogFragmentArgs>()

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

        with(binding) {
            toolbar.apply {
                setTitle(getString(R.string.edit_text_emoji))
                setNavigationIcon(R.drawable.baseline_close_24)
                setNavigationOnClickListener {
                    dismiss()
                }
            }

            textEmojiEditText.setText(args.textEmoji)

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
                        )
                    )
                )
                dismiss()
            }
        }

    }
}