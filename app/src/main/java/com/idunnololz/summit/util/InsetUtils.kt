package com.idunnololz.summit.util

import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ScrollView
import androidx.core.view.children
import androidx.core.view.updateLayoutParams
import androidx.core.widget.NestedScrollView
import androidx.lifecycle.LifecycleOwner
import com.google.android.material.textfield.TextInputLayout

fun InsetsProvider.insetViewAutomaticallyByMargins(
  lifecycleOwner: LifecycleOwner,
  rootView: View,
  additionalMarginTop: Int = 0,
) {
  insets.observe(lifecycleOwner) {
    val lp = rootView.layoutParams as ViewGroup.MarginLayoutParams

    lp.topMargin = it.topInset + additionalMarginTop
    lp.bottomMargin = it.bottomInset
    lp.leftMargin = it.leftInset
    lp.rightMargin = it.rightInset
    rootView.requestLayout()
  }
}

fun InsetsProvider.insetViewExceptBottomAutomaticallyByMargins(
  lifecycleOwner: LifecycleOwner,
  view: View,
) {
  insets.observe(lifecycleOwner) {
    view.updateLayoutParams<ViewGroup.MarginLayoutParams> {
      topMargin = it.topInset
      leftMargin = it.leftInset
      rightMargin = it.rightInset
    }
  }
}

fun InsetsProvider.insetViewExceptTopAutomaticallyByPadding(
  lifecycleOwner: LifecycleOwner,
  rootView: View,
  additionalPaddingBottom: Int = 0,
) {
  insets.observe(lifecycleOwner) {
    val insets = it

    rootView.setPadding(
      insets.leftInset,
      0,
      insets.rightInset,
      insets.bottomInset + additionalPaddingBottom,
    )
  }
}

fun InsetsProvider.insetViewExceptBottomAutomaticallyByPadding(
  lifecycleOwner: LifecycleOwner,
  view: View,
) {
  insets.observe(lifecycleOwner) {
    view.setPadding(
      it.leftInset,
      it.topInset,
      it.rightInset,
      0,
    )
  }
}

fun InsetsProvider.insetViewExceptTopAutomaticallyByMargins(
  lifecycleOwner: LifecycleOwner,
  rootView: View,
) {
  insets.observe(lifecycleOwner) {
    rootView.updateLayoutParams<ViewGroup.MarginLayoutParams> {
      bottomMargin = it.bottomInset
      leftMargin = it.leftInset
      rightMargin = it.rightInset
    }
  }
}

fun InsetsProvider.insetViewStartAndEndByPadding(lifecycleOwner: LifecycleOwner, rootView: View) {
  insets.observe(lifecycleOwner) { insets ->
    rootView.setPadding(
      insets.leftInset,
      0,
      insets.rightInset,
      0,
    )
  }
}

fun InsetsProvider.insetViewAutomaticallyByPadding(
  lifecycleOwner: LifecycleOwner,
  rootView: View,
  additionalPaddingBottom: Int = 0,
) {
  insets.observe(lifecycleOwner) { insets ->
    rootView.setPadding(
      insets.leftInset,
      insets.topInset,
      insets.rightInset,
      insets.bottomInset + additionalPaddingBottom,
    )
  }
}

fun InsetsProvider.autoScrollToEditTextCursorOnIme(
  lifecycleOwner: LifecycleOwner,
  scrollView: NestedScrollView,
  textInputs: List<TextInputLayout>
) {
  var lastImeHeight = -1
  insets.observe(lifecycleOwner) {
    if (lastImeHeight == -1) {
      lastImeHeight = it.imeHeight
      return@observe
    }

    if (lastImeHeight != it.imeHeight) {
      if (lastImeHeight < it.imeHeight) {
        lastImeHeight = it.imeHeight

        scrollView.post {
          for (textInput in textInputs) {
            val editText = textInput.editText ?: continue
            val pos = editText.selectionStart

            if (pos >= 0 && editText.isFocused) {
              editText.post(
                {
                  val pos = editText.selectionStart
                  val layout = editText.layout
                  if (pos >= 0 && layout != null) {
                    val line = layout.getLineForOffset(pos)
                    val lineTop = layout.getLineTop(line)

                    scrollView.smoothScrollTo(0, textInput.top + lineTop)
                  }
                }
              )
              break
            }
          }
        }
      } else {
        lastImeHeight = it.imeHeight
      }
    }
  }
}