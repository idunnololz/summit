package com.idunnololz.summit.editTextToolbar

import android.graphics.Point
import android.graphics.Rect
import android.view.Gravity
import android.view.View
import android.view.ViewTreeObserver.OnPreDrawListener
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePadding
import androidx.core.widget.NestedScrollView
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.LifecycleOwner
import com.idunnololz.summit.util.InsetsProvider
import com.idunnololz.summit.util.Utils

class TextFieldToolbarHelper(
  val root: View,
  val postBodyToolbar: View,
  val postBodyToolbarPlaceholder: View,
  val postBodyToolbarPlaceholder2: View,
  val bodyEditText: View,
  val postTextDivider: View?,
  val scrollView: NestedScrollView,
  val getInsetsProvider: () -> InsetsProvider?,
  val editTextsThatUseToolbar: List<TextView>,
  val lifecycleOwner: LifecycleOwner,
  val toolbarTopMargin: Int = 0,
  val onPositionChange: (isSticky: Boolean) -> Unit = {},
) {

  private var isImeOpen: Boolean = false

  private val outLocation = IntArray(2)

  private val floatingLocation = Point()

  var show: Boolean = true
    set(value) {
      field = value

      updateToolbar()
    }

  init {
    onImeChange(isImeOpen = isImeOpen, force = true)
  }

  fun registerListeners() {
    root.viewTreeObserver.addOnPreDrawListener(
      object : OnPreDrawListener {
        override fun onPreDraw(): Boolean {
          root.viewTreeObserver.removeOnPreDrawListener(this)

          postBodyToolbarPlaceholder.updateLayoutParams<ConstraintLayout.LayoutParams> {
            height = postBodyToolbar.height
          }
          postBodyToolbarPlaceholder2.updateLayoutParams<LinearLayout.LayoutParams> {
            height = postBodyToolbar.height
          }

          root.post {
            onScrollUpdated()
          }

          return false // discard frame
        }
      },
    )

    scrollView.setOnScrollChangeListener(
      NestedScrollView.OnScrollChangeListener { _, _, _, _, _ ->
        onScrollUpdated()
      },
    )

    getInsetsProvider()?.insets?.observe(lifecycleOwner) { insets ->
      val isImeOpen = (insets?.imeHeight ?: 0) > 0

      root.post {
        onImeChange(isImeOpen)
      }
    }

    for (view in editTextsThatUseToolbar) {
      view.addTextChangedListener {
        root.postDelayed(
          {
            onScrollUpdated()
          },
          10,
        )
      }
    }
  }

  /**
   * Call this if an animation has started that will result in the toolbar moving locations.
   */
  fun onTransitionStart() {
    postBodyToolbar.animate()
      .alpha(0f)
  }

  /**
   * Call this if an animation has ended that will result in the toolbar moving locations.
   */
  fun onTransitionEnd() {
    root.post {
      onScrollUpdated()
      postBodyToolbar.animate()
        .alpha(1f)
    }
  }

  private fun onImeChange(isImeOpen: Boolean, force: Boolean = false) {
    if (this.isImeOpen == isImeOpen && !force) {
      return
    }

    this.isImeOpen = isImeOpen

    updateToolbar()
    onPositionChange(isImeOpen)
  }

  private fun updateToolbar() {
    if (!show) {
      hidePostToolbar()
    } else if (isImeOpen) {
      postBodyToolbarPlaceholder.visibility = View.GONE
      postBodyToolbarPlaceholder2.visibility = View.VISIBLE
      postTextDivider?.visibility = View.VISIBLE
      postBodyToolbar.updateLayoutParams<FrameLayout.LayoutParams> {
        gravity = Gravity.BOTTOM
      }
      postBodyToolbar.translationY = 0f

      showPostToolbar()
    } else {
      postBodyToolbarPlaceholder.visibility = View.VISIBLE
      postBodyToolbarPlaceholder2.visibility = View.GONE
      postTextDivider?.visibility = View.GONE
      postBodyToolbar.updateLayoutParams<FrameLayout.LayoutParams> {
        gravity = Gravity.TOP or Gravity.LEFT
      }

      onPositionChanged()
    }
  }

  private fun onPositionChanged() {
    if (isImeOpen) {
      return
    }

    if (!postBodyToolbar.isLaidOut) {
      return
    }

    val scrollBounds = Rect()
    scrollView.getHitRect(scrollBounds)
    val anyPartVisible = postBodyToolbarPlaceholder.getLocalVisibleRect(scrollBounds)
    val visiblePercent = scrollBounds.height().toFloat() / postBodyToolbarPlaceholder.height

    if (anyPartVisible && visiblePercent > 0.9f && show) {
      showPostToolbar()
    } else {
      hidePostToolbar()
    }

    postBodyToolbar.translationY = floatingLocation.y.toFloat() -
      (getInsetsProvider()?.insets?.value?.topInset ?: 0)
  }

  var hiding = false
  var showing = true
  private fun hidePostToolbar() {
    if (hiding) {
      return
    }

    hiding = true
    showing = false

    postBodyToolbar.clearAnimation()
    postBodyToolbar.animate()
      .alpha(0f)
  }

  private fun showPostToolbar() {
    if (showing) {
      return
    }

    hiding = false
    showing = true

    postBodyToolbar.clearAnimation()
    postBodyToolbar.animate()
      .alpha(1f)
  }

  fun onScrollUpdated() {
    bodyEditText.getLocationOnScreen(outLocation)

    floatingLocation.y = outLocation[1] + bodyEditText.height + toolbarTopMargin

    onPositionChanged()
  }
}