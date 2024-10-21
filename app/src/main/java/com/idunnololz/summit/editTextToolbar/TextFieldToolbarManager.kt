package com.idunnololz.summit.editTextToolbar

import android.content.Context
import android.os.Build
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.children
import androidx.core.view.isVisible
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.MutableLiveData
import com.idunnololz.summit.R
import com.idunnololz.summit.databinding.TextFieldToolbarItemBinding
import com.idunnololz.summit.databinding.TextFormatToolbarBinding
import com.idunnololz.summit.emoji.EmojiPopupWindow
import com.idunnololz.summit.preferences.Preferences
import com.idunnololz.summit.preferences.TextFieldToolbarSettings
import com.idunnololz.summit.util.BottomMenu
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TextFieldToolbarManager @Inject constructor(
    private val preferences: Preferences,
    private val emojiPopupWindowFactory: EmojiPopupWindow.Factory
) {

    val textFieldToolbarSettings = MutableLiveData(preferences.textFieldToolbarSettings)

    fun updateToolbarSettings(settings: TextFieldToolbarSettings?) {
        preferences.textFieldToolbarSettings = settings
        textFieldToolbarSettings.value = settings
    }

    fun createTextFormatterToolbar(
        context: Context,
        parentView: ViewGroup,
    ): TextFormatToolbarViewHolder {
        val textFieldToolbarSettings = textFieldToolbarSettings.value

        if (textFieldToolbarSettings?.useCustomToolbar == true) {
            val container = LinearLayout(context)
            val layoutInflater = LayoutInflater.from(context)

            var preview: View? = null
            var drafts: View? = null
            var textEmojis: View? = null
            var spoiler: View? = null
            var bold: View? = null
            var italic: View? = null
            var strikethrough: View? = null
            var quote: View? = null
            var link: View? = null
            var bulletedList: View? = null
            var numberedList: View? = null
            var spongebob: View? = null
            var image: View? = null
            var linkApp: View? = null

            container.orientation = LinearLayout.HORIZONTAL
            for (option in textFieldToolbarSettings.toolbarOptions) {
                val b = TextFieldToolbarItemBinding
                    .inflate(layoutInflater, container, true)
                b.image.setImageResource(option.icon)
                b.image.contentDescription = context.getString(option.title)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    b.image.tooltipText = context.getString(option.title)
                }

                when (option) {
                    TextFieldToolbarOption.Preview ->
                        preview = b.image
                    TextFieldToolbarOption.Drafts ->
                        drafts = b.image
                    TextFieldToolbarOption.TextEmojisField ->
                        textEmojis = b.image
                    TextFieldToolbarOption.Spoiler ->
                        spoiler = b.image
                    TextFieldToolbarOption.Bold ->
                        bold = b.image
                    TextFieldToolbarOption.Italic ->
                        italic = b.image
                    TextFieldToolbarOption.Strikethrough ->
                        strikethrough = b.image
                    TextFieldToolbarOption.Quote ->
                        quote = b.image
                    TextFieldToolbarOption.Link ->
                        link = b.image
                    TextFieldToolbarOption.BulletedList ->
                        bulletedList = b.image
                    TextFieldToolbarOption.NumberedList ->
                        numberedList = b.image
                    TextFieldToolbarOption.Sarcasm ->
                        spongebob = b.image
                    TextFieldToolbarOption.Image ->
                        image = b.image
                    TextFieldToolbarOption.LinkApp ->
                        linkApp = b.image
                }
            }

            val b = TextFieldToolbarItemBinding
                .inflate(layoutInflater, container, true)
            b.image.setImageResource(R.drawable.baseline_settings_24)
            b.image.contentDescription = context.getString(R.string.settings)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                b.image.tooltipText = context.getString(R.string.settings)
            }
            val settings: View = b.image

            parentView.addView(container)

            return TextFormatToolbarViewHolder(
                preview = preview,
                drafts = drafts,
                textEmojis = textEmojis,
                spoiler = spoiler,
                bold = bold,
                italic = italic,
                strikethrough = strikethrough,
                quote = quote,
                link = link,
                bulletedList = bulletedList,
                numberedList = numberedList,
                spongebob = spongebob,
                image = image,
                linkApp = linkApp,
                settings = settings,
                emojiPopupWindowFactory = emojiPopupWindowFactory,
            )
        }

        val b = TextFormatToolbarBinding.inflate(
            LayoutInflater.from(context),
            parentView,
            true,
        )

        return TextFormatToolbarViewHolder(b, emojiPopupWindowFactory)
    }
}

class TextFormatToolbarViewHolder(
    val preview: View?,
    val drafts: View?,
    val textEmojis: View?,
    val spoiler: View?,
    val bold: View?,
    val italic: View?,
    val strikethrough: View?,
    val quote: View?,
    val link: View?,
    val bulletedList: View?,
    val numberedList: View?,
    val spongebob: View?,
    val image: View?,
    val linkApp: View?,
    val settings: View,
    private val emojiPopupWindowFactory: EmojiPopupWindow.Factory,
) {

    private var editText: EditText? = null

    constructor(
        b: TextFormatToolbarBinding,
        emojiPopupWindowFactory: EmojiPopupWindow.Factory,
    ) : this(
        b.preview,
        b.drafts,
        b.textEmojis,
        b.spoiler,
        b.bold,
        b.italic,
        b.strikethrough,
        b.quote,
        b.link,
        b.bulletedList,
        b.numberedList,
        b.spongebob,
        b.image,
        b.linkApp,
        b.settings,
        emojiPopupWindowFactory,
    )

    var isEnabled: Boolean = true
        set(value) {
            field = value

            preview?.isEnabled = value
            drafts?.isEnabled = value
            textEmojis?.isEnabled = value
            spoiler?.isEnabled = value
            bold?.isEnabled = value
            italic?.isEnabled = value
            strikethrough?.isEnabled = value
            quote?.isEnabled = value
            link?.isEnabled = value
            bulletedList?.isEnabled = value
            numberedList?.isEnabled = value
            spongebob?.isEnabled = value
            image?.isEnabled = value
            linkApp?.isEnabled = value
            settings.isEnabled = value
        }

    /**
     * @param referenceTextView A [TextView] where content is shown. For instance the message being
     * replied to. This is used for certain comment actions such as quoting.
     */
    fun setupTextFormatterToolbar(
        editText: EditText,
        referenceTextView: TextView?,
        lifecycleOwner: LifecycleOwner,
        fragmentManager: FragmentManager,
        onChooseImageClick: (() -> Unit)? = null,
        onAddLinkClick: (() -> Unit)? = null,
        onPreviewClick: (() -> Unit)? = null,
        onDraftsClick: (() -> Unit)? = null,
        onSettingsClick: (() -> Unit)? = null,
    ) {
        this.editText = editText

        fun getSelectedText(): String? {
            if (referenceTextView != null) {
                val start = referenceTextView.selectionStart
                val end = referenceTextView.selectionEnd

                if (start != -1 && end != -1) {
                    return referenceTextView.text.toString().substring(start, end)
                }
            }

            val start = editText.selectionStart
            val end = editText.selectionEnd

            if (start != -1 && end != -1) {
                return editText.text.toString().substring(start, end)
            }

            return null
        }

        if (preview != null) {
            if (onPreviewClick != null) {
                preview.visibility = View.VISIBLE
                preview.setOnClickListener {
                    onPreviewClick()
                }
            } else {
                preview.visibility = View.GONE
            }
        }
        if (drafts != null) {
            if (onDraftsClick != null) {
                drafts.visibility = View.VISIBLE
                drafts.setOnClickListener {
                    onDraftsClick()
                }
            } else {
                drafts.visibility = View.GONE
            }
        }
        textEmojis?.setOnClickListener {
//            PopupMenu(it.context, it).apply {
//                menu.apply {
//                    TEXT_EMOJIS.withIndex().forEach { (index, str) ->
//                        add(0, index, 0, str)
//                    }
//                }
//                setOnMenuItemClickListener {
//                    editText.replaceTextAtCursor(TEXT_EMOJIS[it.itemId])
//                    true
//                }
//            }.show()
            emojiPopupWindowFactory.create(
                context = it.context,
                lifecycleOwner = lifecycleOwner,
                fragmentManager = fragmentManager,
                onEmojiSelected = {
                    editText.replaceTextAtCursor(it)
                }
            ).showOnView(it)
//                .show(fragmentManager, "EmojiDialog")
        }
        spoiler?.setOnClickListener {
            editText.wrapTextAtCursor(
                startText = "::: spoiler spoiler\n",
                endText = "\n:::",
            )
        }
        bold?.setOnClickListener {
            editText.wrapTextAtCursor(
                startText = "**",
                endText = "**",
            )
        }
        italic?.setOnClickListener {
            editText.wrapTextAtCursor(
                startText = "*",
                endText = "*",
            )
        }
        strikethrough?.setOnClickListener {
            editText.wrapTextAtCursor(
                startText = "~~",
                endText = "~~",
            )
        }
        quote?.setOnClickListener {
            val start = editText.selectionStart.coerceAtLeast(0)
            val end = editText.selectionEnd.coerceAtLeast(0)

            val text = getSelectedText() ?: ""
            val newText = ">" + text.split("\n").joinToString(separator = "\n>")

            editText.text.replace(start, end, newText)

            val finalCursorPos = Integer.min(start, end) + newText.length

            editText.setSelection(finalCursorPos)
            editText.requestFocus()
        }
        link?.setOnClickListener {
            onAddLinkClick?.invoke()
        }
        bulletedList?.setOnClickListener {
            editText.wrapTextAtCursor(
                startText = "* ",
                endText = "",
                autoLineBreak = true,
            )
        }
        numberedList?.setOnClickListener {
            editText.wrapTextAtCursor(
                startText = "1. ",
                endText = "",
                autoLineBreak = true,
            )
        }
        spongebob?.setOnClickListener {
            val startSelection: Int = editText.selectionStart
            val endSelection: Int = editText.selectionEnd

            val selectedText: String =
                editText.text.toString().substring(startSelection, endSelection)
            var index = 0

            val newString = buildString {
                selectedText.lowercase().forEach a@{
                    if (it.uppercase() == it.toString()) {
                        append(it)
                        return@a
                    }

                    if (index % 2 == 0) {
                        append(it.uppercase())
                    } else {
                        append(it)
                    }

                    index++
                }
            }
            editText.replaceTextAtCursor(newString)
        }

        if (image != null) {
            if (onChooseImageClick != null) {
                image.visibility = View.VISIBLE
                image.setOnClickListener {
                    onChooseImageClick()
                }
            } else {
                image.visibility = View.GONE
            }
        }

        linkApp?.setOnClickListener {
            editText.replaceTextAtCursor(
                "Play store link: [Summit for Lemmy](https://play.google.com/store/apps/details?id=com.idunnololz.summit)",
            )
        }

        if (onSettingsClick != null) {
            settings.visibility = View.VISIBLE
            settings.setOnClickListener {
                onSettingsClick.invoke()
            }
        } else {
            settings.visibility = View.GONE
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
            text.length,
        )
    }

    private fun EditText.wrapTextAtCursor(
        startText: String,
        endText: String,
        autoLineBreak: Boolean = false,
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

    private fun generateBottomSheetFromBar(viewGroup: ViewGroup): BottomMenu {
        val bottomMenu = BottomMenu(viewGroup.context)

        for (view in viewGroup.children) {
            if (view !is ImageView) {
                continue
            }
            if (!view.isVisible) {
                continue
            }

            bottomMenu.addItemWithIcon(
                view.id,
                view.contentDescription.toString(),
                view.drawable,
            )
        }

        return bottomMenu
    }

    fun onImageUploaded(url: String) {
        editText?.replaceTextAtCursor("![]($url)")
    }

    fun onLinkAdded(text: String, url: String) {
        editText?.replaceTextAtCursor("[$text]($url)")
    }
}
