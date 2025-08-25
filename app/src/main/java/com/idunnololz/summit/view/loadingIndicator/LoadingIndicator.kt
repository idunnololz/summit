/*
 * Copyright (C) 2024 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.idunnololz.summit.view.loadingIndicator

import android.content.Context
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.View
import android.widget.ProgressBar
import androidx.annotation.AttrRes
import androidx.annotation.ColorInt
import androidx.annotation.Px
import com.google.android.material.R
import com.google.android.material.color.MaterialColors
import com.google.android.material.theme.overlay.MaterialThemeOverlay
import com.idunnololz.summit.view.loadingIndicator.LoadingIndicatorDrawable.Companion.create
import kotlin.math.min

/** This class implements the loading indicators.  */
class LoadingIndicator @JvmOverloads constructor(
  context: Context,
  attrs: AttributeSet? = null,
  @AttrRes defStyleAttr: Int = R.attr.loadingIndicatorStyle,
) : View(
  MaterialThemeOverlay.wrap(context, attrs, defStyleAttr, DEF_STYLE_RES),
  attrs,
  defStyleAttr,
),
  Drawable.Callback {

  /** Returns the [LoadingIndicatorDrawable] object used in this loading indicator.  */
  val drawable: LoadingIndicatorDrawable
  private val specs: LoadingIndicatorSpec
  var playing: Boolean = true
    set(value) {
      if (field == value) return

      field = value

      drawable.setVisible(visibleToUser(), false, value)
    }

  init {
    var context = context
    // Ensures that we are using the correctly themed context rather than the context that was
    // passed in.
    context = getContext()

    drawable = create(context, LoadingIndicatorSpec(context, attrs, defStyleAttr))
    drawable.callback = this

    specs = drawable.drawingDelegate.specs
    setAnimatorDurationScaleProvider(AnimatorDurationScaleProvider())
  }

  fun setProgress(progress: Float) {
    if (playing) return

    drawable.setProgress(progress)
  }

  override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
    var widthMeasureSpec = widthMeasureSpec
    var heightMeasureSpec = heightMeasureSpec
    val widthMode = MeasureSpec.getMode(widthMeasureSpec)
    val heightMode = MeasureSpec.getMode(heightMeasureSpec)
    val widthSize = MeasureSpec.getSize(widthMeasureSpec)
    val heightSize = MeasureSpec.getSize(heightMeasureSpec)

    val drawingDelegate = drawable.drawingDelegate

    val preferredWidth =
      drawingDelegate.getPreferredWidth() + getPaddingLeft() + getPaddingRight()
    val preferredHeight =
      drawingDelegate.getPreferredHeight() + getPaddingTop() + getPaddingBottom()

    if (widthMode == MeasureSpec.AT_MOST) {
      widthMeasureSpec =
        MeasureSpec.makeMeasureSpec(min(widthSize, preferredWidth), MeasureSpec.EXACTLY)
    } else if (widthMode == MeasureSpec.UNSPECIFIED) {
      widthMeasureSpec = MeasureSpec.makeMeasureSpec(preferredWidth, MeasureSpec.EXACTLY)
    }
    if (heightMode == MeasureSpec.AT_MOST) {
      heightMeasureSpec =
        MeasureSpec.makeMeasureSpec(min(heightSize, preferredHeight), MeasureSpec.EXACTLY)
    } else if (heightMode == MeasureSpec.UNSPECIFIED) {
      heightMeasureSpec = MeasureSpec.makeMeasureSpec(preferredHeight, MeasureSpec.EXACTLY)
    }
    super.onMeasure(widthMeasureSpec, heightMeasureSpec)
  }

  override fun onDraw(canvas: Canvas) {
    super.onDraw(canvas)
    val saveCount = canvas.save()
    if (getPaddingLeft() != 0 || getPaddingTop() != 0) {
      canvas.translate(getPaddingLeft().toFloat(), getPaddingTop().toFloat())
    }
    if (getPaddingRight() != 0 || getPaddingBottom() != 0) {
      val w = getWidth() - (getPaddingLeft() + getPaddingRight())
      val h = getHeight() - (getPaddingTop() + getPaddingBottom())
      canvas.clipRect(0, 0, w, h)
    }

    drawable.draw(canvas)

    canvas.restoreToCount(saveCount)
  }

  override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
    super.onSizeChanged(w, h, oldw, oldh)
    drawable.setBounds(0, 0, w, h)
  }

  override fun onVisibilityChanged(changedView: View, visibility: Int) {
    super.onVisibilityChanged(changedView, visibility)
    drawable.setVisible(
      visibleToUser(), /* restart= */
      false, /* animate= */
      visibility == VISIBLE && playing,
    )
  }

  override fun onWindowVisibilityChanged(visibility: Int) {
    super.onWindowVisibilityChanged(visibility)
    drawable.setVisible(
      visibleToUser(), /* restart= */
      false, /* animate= */
      visibility == VISIBLE && playing,
    )
  }

  override fun invalidateDrawable(drawable: Drawable) {
    invalidate()
  }

  override fun getAccessibilityClassName(): CharSequence = ProgressBar::class.java.getName()

  /**
   * Returns whether or not this view is currently displayed in window, based on whether it is
   * attached to a window and whether it and its ancestors are visible.
   */
  fun visibleToUser(): Boolean =
    isAttachedToWindow() && getWindowVisibility() == VISIBLE && this.isEffectivelyVisible

  val isEffectivelyVisible: Boolean
    /**
     * Returns whether or not this view and all of its ancestors are visible (and thus it is
     * effectively visible to the user). This is *very* similar to [.isShown], except that
     * when attached to a visible window, it will treat a null ViewParent in the hierarchy as being
     * visible (whereas [.isShown] treats this case as non-visible).
     *
     *
     * In cases where the return value of this method differs from [.isShown] (which
     * generally only occur through usage of [ViewGroup.detachViewFromParent]
     * by things like [android.widget.ListView] and [android.widget.Spinner]), this view
     * would still be attached to a window (meaning it's mAttachInfo is non-null), but it or one of
     * its ancestors would have had its `mParent` reference directly set to null by the
     * aforementioned method. In correctly written code, this is a transient state, but this transient
     * state often includes things that may trigger a view to re-check its visibility (like re-binding
     * a view for view recycling), and thus [.isShown] can return false negatives.
     *
     *
     * This is necessary as before API 24, it is not guaranteed that Views will ever be notified
     * about their parent changing. Thus, we don't have a proper point to hook in and re-check [ ][.isShown] on parent changes that result from [ ][ViewGroup.attachViewToParent], which *can*
     * change our effective visibility. So this method errs on the side of assuming visibility unless
     * we can conclusively prove otherwise (but may result in some false positives, if this view
     * ends up being attached to a non-visible hierarchy after being detached in a visible state).
     */
    get() {
      var current: View = this
      do {
        if (current.getVisibility() != VISIBLE) {
          return false
        }
        val parent = current.getParent()
        if (parent == null) {
          return getWindowVisibility() == VISIBLE
        }
        if (parent !is View) {
          return true
        }
        current = parent as View
      } while (true)
    }

  // ******************* Getters and Setters *******************

  @get:Px
  var indicatorSize: Int
    /** Returns the indicator size for this loading indicator in px.  */
    get() = specs.indicatorSize

    /**
     * Sets a new indicator size for this loading indicator.
     *
     * @param indicatorSize The new indicator size in px.
     */
    set(indicatorSize) {
      if (specs.indicatorSize != indicatorSize) {
        specs.indicatorSize = indicatorSize
        requestLayout()
        invalidate()
      }
    }

  @get:Px
  var containerWidth: Int
    /** Returns the container width for this loading indicator in px.  */
    get() = specs.containerWidth

    /**
     * Sets a new container width for this loading indicator.
     *
     * @param containerWidth The new container width in px.
     */
    set(containerWidth) {
      if (specs.containerWidth != containerWidth) {
        specs.containerWidth = containerWidth
        requestLayout()
        invalidate()
      }
    }

  @get:Px
  var containerHeight: Int
    /** Returns the container height for this loading indicator in px.  */
    get() = specs.containerHeight

    /**
     * Sets a new container height for this loading indicator.
     *
     * @param containerHeight The new container height in px.
     */
    set(containerHeight) {
      if (specs.containerHeight != containerHeight) {
        specs.containerHeight = containerHeight
        requestLayout()
        invalidate()
      }
    }

  /**
   * Sets a new indicator color (or a sequence of indicator colors) for this loading indicator.
   *
   * @param indicatorColors The new indicator color(s).
   */
  fun setIndicatorColor(@ColorInt vararg indicatorColors: Int) {
    var indicatorColors = indicatorColors
    if (indicatorColors.size == 0) {
      // Uses theme primary color for indicator by default. Indicator color cannot be empty.
      indicatorColors =
        intArrayOf(
          MaterialColors.getColor(
            getContext(),
            androidx.appcompat.R.attr.colorPrimary,
            -1,
          ),
        )
    }
    if (!this.indicatorColor.contentEquals(indicatorColors)) {
      specs.indicatorColors = indicatorColors
      drawable.getAnimatorDelegate().invalidateSpecValues()
      invalidate()
    }
  }

  val indicatorColor: IntArray
    /** Returns the indicator color(s) for this loading indicator in an int array.  */
    get() = specs.indicatorColors

  @get:ColorInt
  var containerColor: Int
    /** Returns the container color for this loading indicator.  */
    get() = specs.containerColor

    /**
     * Sets a new container color for this loading indicator.
     *
     * @param containerColor A new container color.
     */
    set(containerColor) {
      if (specs.containerColor != containerColor) {
        specs.containerColor = containerColor
        invalidate()
      }
    }

  fun setAnimatorDurationScaleProvider(
    animatorDurationScaleProvider: AnimatorDurationScaleProvider,
  ) {
    drawable.animatorDurationScaleProvider = animatorDurationScaleProvider
  }

  companion object {
    val DEF_STYLE_RES: Int = R.style.Widget_Material3_LoadingIndicator
  }
}
