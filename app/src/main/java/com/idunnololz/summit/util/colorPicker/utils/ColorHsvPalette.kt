/*
 * Designed and developed by 2017 skydoves (Jaewoong Eum)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.idunnololz.summit.util.colorPicker.utils

import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.RadialGradient
import android.graphics.Shader
import android.graphics.SweepGradient
import android.graphics.drawable.BitmapDrawable
import kotlin.math.min

/**
 * ColorHsvPalette is a default drawable palette built by HSV (hue, saturation, value) color model
 * for alternating representations of the RGB color model.
 */
class ColorHsvPalette(resources: Resources?, bitmap: Bitmap?) :
    BitmapDrawable(resources, bitmap) {
    private val huePaint =
        Paint(Paint.ANTI_ALIAS_FLAG)
    private val saturationPaint =
        Paint(Paint.ANTI_ALIAS_FLAG)

    override fun draw(canvas: Canvas) {
        val width = bounds.width()
        val height = bounds.height()
        val centerX = width * 0.5f
        val centerY = height * 0.5f
        val radius = (min(width.toDouble(), height.toDouble()) * 0.5f).toFloat()

        val sweepShader: Shader =
            SweepGradient(
                centerX,
                centerY,
                intArrayOf(
                    Color.RED,
                    Color.MAGENTA,
                    Color.BLUE,
                    Color.CYAN,
                    Color.GREEN,
                    Color.YELLOW,
                    Color.RED,
                ),
                floatArrayOf(0.000f, 0.166f, 0.333f, 0.499f, 0.666f, 0.833f, 0.999f),
            )

        huePaint.setShader(sweepShader)

        val saturationShader: Shader =
            RadialGradient(
                centerX,
                centerY,
                radius,
                Color.WHITE,
                0x00FFFFFF,
                Shader.TileMode.CLAMP,
            )
        saturationPaint.setShader(saturationShader)

        canvas.drawCircle(centerX, centerY, radius, huePaint)
        canvas.drawCircle(centerX, centerY, radius, saturationPaint)
    }

    override fun setAlpha(alpha: Int) {
        huePaint.alpha = alpha
    }

    override fun setColorFilter(colorFilter: ColorFilter?) {
        huePaint.setColorFilter(colorFilter)
    }

    override fun getOpacity(): Int {
        return PixelFormat.OPAQUE
    }
}
