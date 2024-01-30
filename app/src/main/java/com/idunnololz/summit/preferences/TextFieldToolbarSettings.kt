package com.idunnololz.summit.preferences

import com.idunnololz.summit.editTextToolbar.TextFieldToolbarOption
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class TextFieldToolbarSettings(
    val useCustomToolbar: Boolean = false,
    val toolbarOptions: List<TextFieldToolbarOption> = listOf(
        TextFieldToolbarOption.Preview,
        TextFieldToolbarOption.Drafts,
        TextFieldToolbarOption.TextEmojisField,
        TextFieldToolbarOption.Spoiler,
        TextFieldToolbarOption.Bold,
        TextFieldToolbarOption.Italic,
        TextFieldToolbarOption.Strikethrough,
        TextFieldToolbarOption.Quote,
        TextFieldToolbarOption.Link,
        TextFieldToolbarOption.BulletedList,
        TextFieldToolbarOption.NumberedList,
        TextFieldToolbarOption.Sarcasm,
        TextFieldToolbarOption.Image,
        TextFieldToolbarOption.LinkApp,
    ),
)
