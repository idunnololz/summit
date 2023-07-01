package com.idunnololz.summit.lemmy.utils

import android.net.Uri
import android.view.View
import android.widget.EditText
import android.widget.PopupMenu
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.navigation.fragment.findNavController
import com.idunnololz.summit.databinding.TextFormatToolbarBinding
import com.idunnololz.summit.lemmy.comment.AddOrEditCommentFragment
import com.idunnololz.summit.lemmy.comment.AddOrEditCommentFragmentDirections
import com.idunnololz.summit.util.ext.navigateSafe

class TextFormatterHelper {

    companion object {
        private val TEXT_EMOJIS = listOf(
            "( ͡° ͜ʖ ͡° )",
            "ಠ_ಠ",
            "(╯°□°）╯︵ ┻━┻",
            "┬─┬ノ( º _ ºノ)",
            "¯\\_(ツ)_/¯",
            "༼ つ ◕_◕ ༽つ",
            "ᕕ( ᐛ )ᕗ",
            "(•_•) ( •_•)>⌐■-■ (⌐■_■)"
        )
    }

    private var editText: EditText? = null

    fun setupTextFormatterToolbar(
        textFormatToolbarBinding: TextFormatToolbarBinding,
        editText: EditText,
        imagePickerLauncher: ActivityResultLauncher<PickVisualMediaRequest>? = null,
        onPreviewClick: () -> Unit,
    ) {
        this.editText = editText
        with(textFormatToolbarBinding) {
            preview.setOnClickListener {
                onPreviewClick()
            }

            textEmojis.setOnClickListener {
                PopupMenu(it.context, it).apply {
                    menu.apply {
                        TEXT_EMOJIS.withIndex().forEach { (index, str) ->
                            add(0, index, 0, str)
                        }
                    }
                    setOnMenuItemClickListener {
                        editText.replaceTextAtCursor(TEXT_EMOJIS[it.itemId])

                        true
                    }
                }.show()
            }

            spoiler.setOnClickListener {
                editText.wrapTextAtCursor(
                    startText = "::: spoiler spoiler\n",
                    endText = "\n:::"
                )
            }
            bold.setOnClickListener {
                editText.wrapTextAtCursor(
                    startText = "**",
                    endText = "**"
                )
            }
            italic.setOnClickListener {
                editText.wrapTextAtCursor(
                    startText = "*",
                    endText = "*"
                )
            }
            strikethrough.setOnClickListener {
                editText.wrapTextAtCursor(
                    startText = "~~",
                    endText = "~~"
                )
            }
            quote.setOnClickListener {
                editText.wrapTextAtCursor(
                    startText = "> ",
                    endText = ""
                )
            }
            bulletedList.setOnClickListener {
                editText.wrapTextAtCursor(
                    startText = "* ",
                    endText = "",
                    autoLineBreak = true,
                )
            }
            numberedList.setOnClickListener {
                editText.wrapTextAtCursor(
                    startText = "1. ",
                    endText = "",
                    autoLineBreak = true,
                )
            }

            if (imagePickerLauncher != null) {
                image.visibility = View.VISIBLE
                image.setOnClickListener {
                    imagePickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                }
            } else {
                image.visibility = View.GONE
            }

            linkApp.setOnClickListener {
                editText.replaceTextAtCursor(
                    "Play store link: [Summit - Lemmy Reader](https://play.google.com/store/apps/details?id=com.idunnololz.summit)"
                )
            }
        }
    }

    private fun EditText.replaceTextAtCursor(text: String) {
        val editText = this
        val start = editText.selectionStart.coerceAtLeast(0)
        val end = editText.selectionEnd.coerceAtLeast(0)
        editText.text.replace(
            start.coerceAtMost(end),
            start.coerceAtLeast(end),
            text,
            0,
            text.length
        )
    }

    private fun EditText.wrapTextAtCursor(
        startText: String,
        endText: String,
        autoLineBreak: Boolean = false
    ) {
        val editText = this
        val start = editText.selectionStart.coerceAtLeast(0)
        val end = editText.selectionEnd.coerceAtLeast(0)

        val prevChar = editText.text.toString().getOrNull(Integer.min(start - 1, end - 1))

        var finalCursorPos = Integer.min(start, end) + startText.length

        editText.text.insert(Integer.max(start, end), endText)
        editText.text.insert(Integer.min(start, end), startText)

        if (prevChar != null && prevChar != '\n' && autoLineBreak) {
            editText.text.insert(Integer.min(start, end), "\n")
            finalCursorPos++
        }

        editText.setSelection(finalCursorPos)
    }

    fun onImageUploaded(url: String) {
        editText?.replaceTextAtCursor("![]($url)")
    }
}