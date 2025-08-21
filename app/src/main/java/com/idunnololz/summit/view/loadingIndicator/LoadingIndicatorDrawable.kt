/*
 * Copyright (C) 2024 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
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
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.Rect
import android.graphics.drawable.Drawable
import androidx.annotation.IntRange
import androidx.annotation.RestrictTo
import androidx.annotation.VisibleForTesting
import androidx.vectordrawable.graphics.drawable.VectorDrawableCompat
import com.google.android.material.R
import com.google.errorprone.annotations.CanIgnoreReturnValue

/** This class draws the graphics for a loading indicator.  */
class LoadingIndicatorDrawable internal constructor(
  private val context: Context,
  private val specs: LoadingIndicatorSpec,
  var drawingDelegate: LoadingIndicatorDrawingDelegate,
  private var animatorDelegate: LoadingIndicatorAnimatorDelegate
) : Drawable(), Drawable.Callback {
  var animatorDurationScaleProvider: AnimatorDurationScaleProvider? =
    AnimatorDurationScaleProvider()

  var paint: Paint

  @IntRange(from = 0, to = 255)
  var _alpha: Int = 0

  /**
   * Returns the drawable that will be used when the system animator is disabled.
   *
   * @hide
   */
  /**
   * Sets the drawable that will be used when the system animator is disabled.
   *
   * @hide
   */
  @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
  @set:VisibleForTesting
  @set:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
  var staticDummyDrawable: Drawable? = null

  init {

    this.paint = Paint()

    animatorDelegate.registerDrawable(this)
    setAlpha(255)
  }

  // ******************* Overridden methods *******************
  override fun getIntrinsicWidth(): Int {
    return drawingDelegate.getPreferredWidth()
  }

  override fun getIntrinsicHeight(): Int {
    return drawingDelegate.getPreferredHeight()
  }

  override fun draw(canvas: Canvas) {
    val clipBounds = Rect()
    val bounds = getBounds()

    if (bounds.isEmpty() || !isVisible() || !canvas.getClipBounds(clipBounds)) {
      // Escape if bounds are empty, clip bounds are empty, or currently hidden.
      return
    }

    if (this.isSystemAnimatorDisabled && staticDummyDrawable != null) {
      staticDummyDrawable!!.setBounds(bounds)
      staticDummyDrawable!!.setTint(specs.indicatorColors[0])
      staticDummyDrawable!!.draw(canvas)
      return
    }

    canvas.save()
    drawingDelegate.adjustCanvas(canvas, bounds)
    drawingDelegate.drawContainer(canvas, paint, specs.containerColor, getAlpha())
    drawingDelegate.drawIndicator(canvas, paint, animatorDelegate.indicatorState, getAlpha())
    canvas.restore()
  }

  @CanIgnoreReturnValue
  override fun setVisible(visible: Boolean, restart: Boolean): Boolean {
    return setVisible(visible, restart,  /* animate= */visible)
  }

  /**
   * Changes the visibility with/without triggering the animation callbacks.
   *
   * @param visible Whether to make the drawable visible.
   * @param restart Whether to force starting the animation from the beginning.
   * @param animate Whether to change the visibility with animation.
   * @return `true`, if the visibility changes or will change after the animation; `false`, otherwise.
   * @see .setVisible
   */
  @CanIgnoreReturnValue
  fun setVisible(visible: Boolean, restart: Boolean, animate: Boolean): Boolean {
    val changed = super.setVisible(visible, restart)
    animatorDelegate.cancelAnimatorImmediately()
    // Restarts the main animator if it's visible and needs to be animated.
    if (visible && animate && !this.isSystemAnimatorDisabled) {
      animatorDelegate.startAnimator()
    }
    return changed
  }

  override fun setAlpha(alpha: Int) {
    if (this._alpha != alpha) {
      this._alpha = alpha
      invalidateSelf()
    }
  }

  override fun getAlpha(): Int {
    return _alpha
  }

  override fun setColorFilter(colorFilter: ColorFilter?) {
    paint.setColorFilter(colorFilter)
    invalidateSelf()
  }

  override fun getOpacity(): Int {
    return PixelFormat.TRANSLUCENT
  }

  override fun invalidateDrawable(drawable: Drawable) {
    val callback = getCallback()
    if (callback != null) {
      callback.invalidateDrawable(this)
    }
  }

  override fun scheduleDrawable(who: Drawable, what: Runnable, `when`: Long) {
    val callback = getCallback()
    if (callback != null) {
      callback.scheduleDrawable(this, what, `when`)
    }
  }

  override fun unscheduleDrawable(who: Drawable, what: Runnable) {
    val callback = getCallback()
    if (callback != null) {
      callback.unscheduleDrawable(this, what)
    }
  }

  private val isSystemAnimatorDisabled: Boolean
    // ******************* Utility functions *******************
    get() {
      if (animatorDurationScaleProvider != null) {
        val systemAnimatorDurationScale =
          animatorDurationScaleProvider!!.getSystemAnimatorDurationScale(
            context.getContentResolver()
          )
        return systemAnimatorDurationScale == 0f
      }
      return false
    }

  // ******************* Setter and getter *******************

  fun getAnimatorDelegate(): LoadingIndicatorAnimatorDelegate {
    return animatorDelegate
  }

  fun setAnimatorDelegate(animatorDelegate: LoadingIndicatorAnimatorDelegate) {
    this.animatorDelegate = animatorDelegate
    animatorDelegate.registerDrawable(this)
  }

  fun setProgress(progress: Float) {
    animatorDelegate.indicatorState.rotationDegree += 360 * progress
    animatorDelegate.setMorphFactor(1f)
    invalidateSelf()
  }

  companion object {
    @JvmStatic
    fun create(
      context: Context, specs: LoadingIndicatorSpec
    ): LoadingIndicatorDrawable {
      val loadingIndicatorDrawable =
        LoadingIndicatorDrawable(
          context,
          specs,
          LoadingIndicatorDrawingDelegate(specs),
          LoadingIndicatorAnimatorDelegate(specs)
        )
      loadingIndicatorDrawable.staticDummyDrawable =
        VectorDrawableCompat.create(
          context.getResources(),
          R.drawable.ic_mtrl_arrow_circle,
          null
        )
      return loadingIndicatorDrawable
    }
  }
}
