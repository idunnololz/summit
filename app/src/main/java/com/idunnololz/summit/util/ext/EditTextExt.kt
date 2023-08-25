package com.idunnololz.summit.util.ext

import android.widget.EditText

fun EditText.getSelectedText(): String =
    try {
        val startSelection: Int = selectionStart
        val endSelection: Int = selectionEnd

        text.toString().substring(startSelection, endSelection)
    } catch (e: Exception) {
        ""
    }