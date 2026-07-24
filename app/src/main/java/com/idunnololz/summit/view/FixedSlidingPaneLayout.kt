package com.idunnololz.summit.view

import android.content.Context
import android.os.Build
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat
import androidx.customview.widget.ViewDragHelper
import androidx.slidingpanelayout.widget.SlidingPaneLayout
import java.lang.reflect.Field
import java.lang.reflect.Method
import kotlin.math.max

class FixedSlidingPaneLayout : SlidingPaneLayout {

  companion object {
    private val sEdgeSizeUsingSystemGestureInsets = Build.VERSION.SDK_INT >= 29
  }

  private val dragHelperField: Field?
  private val slideOffsetField: Field?
  private val slideableViewField: Field?
  private val updateObscuredViewsVisibilityMethod: Method?
  private val dispatchOnPanelOpenedMethod: Method?
  private val dispatchOnPanelClosedMethod: Method?
  private val preservedOpenStateField: Field?
  private val parallaxOtherViewsMethod: Method?

  var isSwipeEnabled: Boolean = true
  private val smoothSlideToFn: Method?
  private val onPanelDragged: Method?

  // We only construct a drag helper to get the width of the drag region.
  private val dragHelper = ViewDragHelper.create(
    this,
    0.5f,
    object : ViewDragHelper.Callback() {
      override fun tryCaptureView(child: View, pointerId: Int): Boolean = false
    },
  )

  constructor(context: Context) : super(context)
  constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
  constructor(context: Context, attrs: AttributeSet?, defStyle: Int) : super(
    context,
    attrs,
    defStyle,
  )

  init {
    smoothSlideToFn = try {
      val method = SlidingPaneLayout::class.java.getDeclaredMethod(
        "smoothSlideTo",
        Float::class.javaPrimitiveType,
        Int::class.javaPrimitiveType,
      )
      method.isAccessible = true
      method
    } catch (_: Exception) {
      null
    }
    onPanelDragged = try {
      val method = SlidingPaneLayout::class.java.getDeclaredMethod(
        "onPanelDragged",
        Int::class.javaPrimitiveType,
      )
      method.isAccessible = true
      method
    } catch (_: Exception) {
      null
    }

    slideOffsetField = try {
      SlidingPaneLayout::class.java.getDeclaredField("mSlideOffset")
    } catch (_: Exception) {
      null
    }
    slideableViewField = try {
      SlidingPaneLayout::class.java.getDeclaredField("mSlideableView")
    } catch (_: Exception) {
      null
    }
    updateObscuredViewsVisibilityMethod = try {
      SlidingPaneLayout::class.java.getDeclaredMethod(
        "updateObscuredViewsVisibility",
        View::class.java,
      )
    } catch (_: Exception) {
      null
    }
    dispatchOnPanelClosedMethod = try {
      SlidingPaneLayout::class.java.getDeclaredMethod("dispatchOnPanelClosed", View::class.java)
    } catch (_: Exception) {
      null
    }
    dispatchOnPanelOpenedMethod = try {
      SlidingPaneLayout::class.java.getDeclaredMethod("dispatchOnPanelOpened", View::class.java)
    } catch (_: Exception) {
      null
    }
    preservedOpenStateField = try {
      SlidingPaneLayout::class.java.getDeclaredField("mPreservedOpenState")
    } catch (_: Exception) {
      null
    }
    parallaxOtherViewsMethod = try {
      SlidingPaneLayout::class.java.getDeclaredMethod(
        "parallaxOtherViews",
        Float::class.javaPrimitiveType,
      )
    } catch (_: Exception) {
      null
    }
    dragHelperField = try {
      SlidingPaneLayout::class.java.getDeclaredField("mDragHelper")
    } catch (_: Exception) {
      null
    }

    try {
      slideOffsetField?.isAccessible = true
      slideableViewField?.isAccessible = true
      updateObscuredViewsVisibilityMethod?.isAccessible = true
      dispatchOnPanelOpenedMethod?.isAccessible = true
      dispatchOnPanelClosedMethod?.isAccessible = true
      preservedOpenStateField?.isAccessible = true
      parallaxOtherViewsMethod?.isAccessible = true
      dragHelperField?.isAccessible = true
    } catch (_: Exception) {
    }
  }

  fun getSlideOffset(): Float? = slideOffsetField?.get(this) as? Float

  override fun onInterceptTouchEvent(ev: MotionEvent?): Boolean {
    if (!isSwipeEnabled) return false

    val intercept = super.onInterceptTouchEvent(ev)

    if (ev == null) {
      return intercept
    }

    if (!intercept) {
      return false
    }

    if (!isOpen || !isSlideable) {
      return true
    }

    val gestureInsets = getSystemGestureInsets()
      ?: return true

    val edgeSize = max(dragHelper.defaultEdgeSize, gestureInsets.left)

    val isLayoutRtl = isLayoutRtlSupport()

    if (isLayoutRtl) {
      if (ev.x > edgeSize) {
        return false
      }
    } else {
      if (ev.x < edgeSize) {
        return false
      }
    }

    return true
  }

  override fun onTouchEvent(ev: MotionEvent?): Boolean {
    if (!isSwipeEnabled) {
      // Careful here, view might be null
      getChildAt(1).dispatchTouchEvent(ev)
      return true
    }
    return super.onTouchEvent(ev)
  }

  fun smoothSlideTo(offset: Float) {
    smoothSlideToFn?.invoke(this, offset, 0)
  }

  fun onPanelDragged(offset: Int) {
    onPanelDragged?.invoke(this, offset)
  }

  fun openPaneNoAnimation() {
    openPane()
    (dragHelperField?.get(this@FixedSlidingPaneLayout) as? ViewDragHelper)?.abort()
  }

  fun closePaneNoAnimation() {
    closePane()

    post {
      (dragHelperField?.get(this@FixedSlidingPaneLayout) as? ViewDragHelper)?.abort()
    }
  }

  private fun getSystemGestureInsets(): Insets? {
    var gestureInsets: Insets? = null
    if (sEdgeSizeUsingSystemGestureInsets) {
      val rootInsetsCompat = ViewCompat.getRootWindowInsets(this)
      if (rootInsetsCompat != null) {
        gestureInsets = rootInsetsCompat.systemGestureInsets
      }
    }
    return gestureInsets
  }

  private fun isLayoutRtlSupport(): Boolean = this.layoutDirection == View.LAYOUT_DIRECTION_RTL
}
