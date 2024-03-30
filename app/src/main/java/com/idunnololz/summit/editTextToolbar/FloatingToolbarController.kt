package com.idunnololz.summit.editTextToolbar

import android.graphics.Point
import android.graphics.Rect
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.widget.EditText
import android.widget.FrameLayout
import androidx.core.view.updateLayoutParams
import androidx.core.widget.NestedScrollView
import androidx.lifecycle.LifecycleOwner
import com.idunnololz.summit.main.MainActivity
import com.idunnololz.summit.util.insetViewExceptTopAutomaticallyByMargins

class FloatingToolbarController(
    private val floatingPlaceholder: View,
    private val floatingViews: List<View>,
    private val anchoredPlaceholder: View,
    private val anchoredViews: List<View>,
    private val toolbarContainer: View,
    private val floatingToolbarContainer: NestedScrollView,
    private val textField: EditText,
    private val lifecycleOwner: LifecycleOwner,
    private val mainActivityProvider: () -> MainActivity?,
) {

    private val floatingLocation = Point()
    private var isImeOpen: Boolean = false
    private val outLocation = IntArray(2)
    var hiding = false
    var showing = true

    fun setup(rootView: View) {
        rootView.viewTreeObserver.addOnPreDrawListener(
            object : ViewTreeObserver.OnPreDrawListener {
                override fun onPreDraw(): Boolean {
                    rootView.viewTreeObserver.removeOnPreDrawListener(this)

                    floatingPlaceholder.updateLayoutParams<ViewGroup.LayoutParams> {
                        height = toolbarContainer.height
                    }
                    anchoredPlaceholder.updateLayoutParams<ViewGroup.LayoutParams> {
                        height = toolbarContainer.height
                    }

                    rootView.post {
                        onScrollUpdated()
                    }

                    return false // discard frame
                }
            },
        )
        mainActivityProvider()?.insets?.observe(lifecycleOwner) { insets ->
            val isImeOpen = (insets?.imeHeight ?: 0) > 0

            rootView.post {
                onImeChange(isImeOpen)
            }
        }

        floatingToolbarContainer.setOnScrollChangeListener(
            NestedScrollView.OnScrollChangeListener { _, _, _, _, _ ->
                onScrollUpdated()
            },
        )

        mainActivityProvider()?.insetViewExceptTopAutomaticallyByMargins(
            lifecycleOwner,
            toolbarContainer,
        )
    }

    private fun onImeChange(isImeOpen: Boolean) {
        this.isImeOpen = isImeOpen

        updateToolbar()
    }

    private fun updateToolbar() {
        if (isImeOpen) {
            floatingPlaceholder.visibility = View.GONE
            floatingViews.forEach { it.visibility = View.GONE }
            anchoredPlaceholder.visibility = View.VISIBLE
            anchoredViews.forEach { it.visibility = View.VISIBLE }
            toolbarContainer.updateLayoutParams<FrameLayout.LayoutParams> {
                gravity = Gravity.BOTTOM
            }
            toolbarContainer.translationY = 0f

            showPostToolbar()
        } else {
            floatingPlaceholder.visibility = View.VISIBLE
            floatingViews.forEach { it.visibility = View.VISIBLE }
            anchoredPlaceholder.visibility = View.GONE
            anchoredViews.forEach { it.visibility = View.GONE }
            toolbarContainer.updateLayoutParams<FrameLayout.LayoutParams> {
                gravity = Gravity.TOP or Gravity.LEFT
            }

            onScrollUpdated()
        }
    }

    private fun onPositionChanged() {
        if (isImeOpen) {
            return
        }

        val scrollBounds = Rect()
        floatingToolbarContainer.getHitRect(scrollBounds)
        val anyPartVisible = floatingPlaceholder.getLocalVisibleRect(scrollBounds)
        val visiblePercent = scrollBounds.height().toFloat() / floatingPlaceholder.height

        if (anyPartVisible && visiblePercent > 0.9f) {
            showPostToolbar()
        } else {
            hidePostToolbar()
        }

        toolbarContainer.translationY = floatingLocation.y.toFloat()
    }

    private fun hidePostToolbar() {
        if (hiding) {
            return
        }

        hiding = true
        showing = false

        toolbarContainer.clearAnimation()
        toolbarContainer.animate()
            .alpha(0f)
    }

    private fun showPostToolbar() {
        if (showing) {
            return
        }

        hiding = false
        showing = true

        toolbarContainer.clearAnimation()
        toolbarContainer.animate()
            .alpha(1f)
    }

    private fun onScrollUpdated() {
        floatingPlaceholder.getLocationOnScreen(outLocation)

        floatingLocation.y = outLocation[1]

        onPositionChanged()
    }
}
