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
package com.idunnololz.summit.util.colorPicker.view

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import android.util.AttributeSet
import androidx.annotation.ColorInt
import com.idunnololz.summit.R

/**
 * AlphaSlideBar extends [AbstractSlider] and more being specific to implement alpha slide.
 */
class AlphaSlideBar : AbstractSlider {
    private var backgroundBitmap: Bitmap? = null
    private val drawable = AlphaTileDrawable()

    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    )

    constructor(
        context: Context,
        attrs: AttributeSet?,
        defStyleAttr: Int,
        defStyleRes: Int
    ) : super(context, attrs, defStyleAttr, defStyleRes)

    override fun onSizeChanged(width: Int, height: Int, oldWidth: Int, oldHeight: Int) {
        super.onSizeChanged(width, height, oldWidth, oldHeight)
        if (width > 0 && height > 0) {
            backgroundBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val backgroundCanvas = Canvas(backgroundBitmap!!)
            drawable.setBounds(0, 0, backgroundCanvas.width, backgroundCanvas.height)
            drawable.draw(backgroundCanvas)
        }
    }

    override fun updatePaint(colorPaint: Paint) {
        val hsv = FloatArray(3)
        Color.colorToHSV(color, hsv)
        val startColor = Color.HSVToColor(0, hsv)
        val endColor = Color.HSVToColor(255, hsv)
        val shader: Shader =
            LinearGradient(
                0f,
                0f,
                width.toFloat(),
                measuredHeight.toFloat(),
                startColor,
                endColor,
                Shader.TileMode.CLAMP
            )
        colorPaint.setShader(shader)
    }

    override fun onInflateFinished() {
        val defaultPosition = width - selector!!.width
        selector!!.x = defaultPosition.toFloat()
    }

    override fun onDraw(canvas: Canvas) {
        if (backgroundBitmap != null) {
            canvas.drawBitmap(backgroundBitmap!!, 0f, 0f, null)
        }
        super.onDraw(canvas)
    }

    @ColorInt
    override fun assembleColor(): Int {
        val hsv = FloatArray(3)
        Color.colorToHSV(color, hsv)
        val alpha = (selectorPosition * 255).toInt()
        return Color.HSVToColor(alpha, hsv)
    }
}
