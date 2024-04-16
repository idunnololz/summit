package com.idunnololz.summit.editTextToolbar

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.idunnololz.summit.R

enum class TextFieldToolbarOption(
    @DrawableRes val icon: Int,
    @StringRes val title: Int,
) {
    Preview(
        R.drawable.baseline_preview_24,
        R.string.preview,
    ),
    Drafts(
        R.drawable.baseline_note_24,
        R.string.drafts,
    ),
    TextEmojisField(
        R.drawable.baseline_emoji_emotions_24,
        R.string.text_emojis,
    ),
    Spoiler(
        R.drawable.baseline_spoiler_24,
        R.string.spoiler_text,
    ),
    Bold(
        R.drawable.baseline_format_bold_24,
        R.string.bold,
    ),
    Italic(
        R.drawable.baseline_format_italic_24,
        R.string.italic,
    ),
    Strikethrough(
        R.drawable.baseline_format_strikethrough_24,
        R.string.strikethrough,
    ),
    Quote(
        R.drawable.baseline_format_quote_24,
        R.string.quote,
    ),
    Link(
        R.drawable.baseline_link_24,
        R.string.insert_link,
    ),
    BulletedList(
        R.drawable.baseline_format_list_bulleted_24,
        R.string.bulleted_list,
    ),
    NumberedList(
        R.drawable.baseline_format_list_numbered_24,
        R.string.numbered_list,
    ),
    Sarcasm(
        R.drawable.ic_troll,
        R.string.sarcasm,
    ),
    Image(
        R.drawable.baseline_image_24,
        R.string.add_image,
    ),
    LinkApp(
        R.drawable.ic_app_link,
        R.string.add_link_to_app,
    ),
}
