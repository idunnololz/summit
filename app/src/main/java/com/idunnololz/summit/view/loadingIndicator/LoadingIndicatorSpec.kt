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

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.TypedArray
import android.graphics.Color
import android.util.AttributeSet
import android.util.TypedValue
import androidx.annotation.AttrRes
import androidx.annotation.ColorInt
import androidx.annotation.Px
import androidx.annotation.StyleRes
import com.google.android.material.R
import com.google.android.material.color.MaterialColors

/**
 * This class contains the parameters for drawing a loading indicator. The parameters reflect the
 * attributes defined in `R.styleable.LoadingIndicator`.
 */
@SuppressLint("PrivateResource")
class LoadingIndicatorSpec @JvmOverloads constructor(
  context: Context,
  attrs: AttributeSet?,
  @AttrRes defStyleAttr: Int = R.attr.loadingIndicatorStyle,
  @StyleRes defStyleRes: Int = LoadingIndicator.DEF_STYLE_RES,
) {
  @JvmField
  var scaleToFit: Boolean = false

  @JvmField
  @Px
  var indicatorSize: Int

  @JvmField
  @Px
  var containerWidth: Int

  @JvmField
  @Px
  var containerHeight: Int

  @JvmField
  var indicatorColors: IntArray = IntArray(0)

  @ColorInt
  var containerColor: Int

  init {
    // Loads default resources.

    @Px val defaultShapeSize =
      context.getResources().getDimensionPixelSize(R.dimen.m3_loading_indicator_shape_size)

    @Px val defaultContainerSize =
      context.getResources()
        .getDimensionPixelSize(R.dimen.m3_loading_indicator_container_size)

    // Loads attributes.
    val a =
      context.obtainStyledAttributes(
        attrs,
        R.styleable.LoadingIndicator,
        defStyleAttr,
        defStyleRes,
      )
    indicatorSize =
      a.getDimensionPixelSize(R.styleable.LoadingIndicator_indicatorSize, defaultShapeSize)
    containerWidth =
      a.getDimensionPixelSize(
        R.styleable.LoadingIndicator_containerWidth,
        defaultContainerSize,
      )
    containerHeight =
      a.getDimensionPixelSize(
        R.styleable.LoadingIndicator_containerHeight,
        defaultContainerSize,
      )
    loadIndicatorColors(context, a)
    containerColor = a.getColor(R.styleable.LoadingIndicator_containerColor, Color.TRANSPARENT)
    a.recycle()
  }

  private fun loadIndicatorColors(context: Context, typedArray: TypedArray) {
    if (!typedArray.hasValue(R.styleable.LoadingIndicator_indicatorColor)) {
      // Uses theme primary color for indicator if not provided in the attribute set.
      indicatorColors =
        intArrayOf(
          MaterialColors.getColor(context, androidx.appcompat.R.attr.colorPrimary, -1),
        )
      return
    }

    val indicatorColorValue =
      typedArray.peekValue(R.styleable.LoadingIndicator_indicatorColor)

    if (indicatorColorValue.type != TypedValue.TYPE_REFERENCE) {
      indicatorColors =
        intArrayOf(typedArray.getColor(R.styleable.LoadingIndicator_indicatorColor, -1))
      return
    }

    indicatorColors =
      context
        .getResources()
        .getIntArray(
          typedArray.getResourceId(
            R.styleable.LoadingIndicator_indicatorColor,
            -1,
          ),
        )
    require(indicatorColors.isNotEmpty()) {
      "indicatorColors cannot be empty when indicatorColor is not used."
    }
  }

  /**
   * Sets the scale specs to fit the given bound of the [LoadingIndicatorDrawable].
   *
   * @param scaleToFit The new scaleToFit value.
   */
  fun setScaleToFit(scaleToFit: Boolean) {
    this.scaleToFit = scaleToFit
  }
}
