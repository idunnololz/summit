package com.idunnololz.summit.util.ext

import android.content.Context
import android.text.TextWatcher
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import androidx.core.widget.doOnTextChanged
import com.idunnololz.summit.R

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

fun EditText.moveCursorToEnd() {
  this.requestFocus()
  this.setSelection(text.length)
}

fun EditText.setTextIfChanged(value: String, listener: (String) -> Unit) {
  if (this.text.toString() != value) {
    setText(value)
    removeTextChangedListener(getTag(R.id.text_change_listener) as? TextWatcher)
    val tw = doOnTextChanged { text: CharSequence?, start: Int, before: Int, count: Int ->
      listener(text?.toString() ?: "")
    }
    setTag(R.id.text_change_listener, tw)
  }
}