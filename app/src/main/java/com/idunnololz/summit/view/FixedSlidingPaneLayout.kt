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

  private val mDragHelper: Field?
  private val mSlideEnabled = true
  private val mSlideOffsetField: Field?
  private val mSlideableViewField: Field?
  private val updateObscuredViewsVisibilityMethod: Method?
  private val dispatchOnPanelOpenedMethod: Method?
  private val dispatchOnPanelClosedMethod: Method?
  private val mPreservedOpenStateField: Field?
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

    mSlideOffsetField = try {
      SlidingPaneLayout::class.java.getDeclaredField("mSlideOffset")
    } catch (_: Exception) {
      null
    }
    mSlideableViewField = try {
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
    mPreservedOpenStateField = try {
      SlidingPaneLayout::class.java.getDeclaredField("mPreservedOpenState")
    } catch (_: Exception) {
      null
    }
    parallaxOtherViewsMethod = try {
      SlidingPaneLayout::class.java.getDeclaredMethod("parallaxOtherViews", Float::class.javaPrimitiveType)
    } catch (_: Exception) {
      null
    }
    mDragHelper = try {
      SlidingPaneLayout::class.java.getDeclaredField("mDragHelper")
    } catch (_: Exception) {
      null
    }

    try {
      mSlideOffsetField?.isAccessible = true
      mSlideableViewField?.isAccessible = true
      updateObscuredViewsVisibilityMethod?.isAccessible = true
      dispatchOnPanelOpenedMethod?.isAccessible = true
      dispatchOnPanelClosedMethod?.isAccessible = true
      mPreservedOpenStateField?.isAccessible = true
      parallaxOtherViewsMethod?.isAccessible = true
      mDragHelper?.isAccessible = true
    } catch (_: Exception) {
    }

//    addPanelSlideListener(
//      object : PanelSlideListener {
//        override fun onPanelSlide(p0: View, p1: Float) {
//          (mDragHelper?.get(this@FixedSlidingPaneLayout) as? ViewDragHelper)?.abort()
//        }
//
//        override fun onPanelOpened(p0: View) {
//        }
//
//        override fun onPanelClosed(p0: View) {
//        }
//
//      },
//    )
  }

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
    (mDragHelper?.get(this@FixedSlidingPaneLayout) as? ViewDragHelper)?.abort()
  }

  fun closePaneNoAnimation() {
    closePane()
//    mSlideOffsetField?.set(this, 0.0f)
//    requestLayout()
//    invalidate()

    post {

      (mDragHelper?.get(this@FixedSlidingPaneLayout) as? ViewDragHelper)?.abort()
    }

//    try {
//      val slideableView = mSlideableViewField?.get(this) as View?
//      mSlideOffsetField?.set(this, 0.0f)
//      requestLayout()
//      invalidate()
//      updateObscuredViewsVisibilityMethod?.invoke(this, slideableView)
//      dispatchOnPanelClosedMethod?.invoke(this, slideableView)
//      mPreservedOpenStateField?.set(this, false)
//    } catch (_: Exception) {
//      closePane()
//    }
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
