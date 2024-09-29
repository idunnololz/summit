package com.idunnololz.summit.util.ext

import android.content.Context
import android.view.inputmethod.InputMethodManager
import android.widget.EditText

fun EditText.getSelectedText(): String = try {
    val startSelection: Int = selectionStart
    val endSelection: Int = selectionEnd

    text.toString().substring(startSelection, endSelection)
} catch (e: Exception) {
    ""
}

fun EditText.requestFocusAndShowKeyboard() {
    this.requestFocus()

    val inputMethodManager = context.getSystemService(Context.INPUT_METHOD_SERVICE)
        as InputMethodManager?
    inputMethodManager?.showSoftInput(this, InputMethodManager.SHOW_IMPLICIT)
}