package com.idunnololz.summit.util.colorPicker.view

import android.content.Context
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import android.util.AttributeSet
import androidx.annotation.ColorInt
import androidx.appcompat.content.res.AppCompatResources
import com.idunnololz.summit.R

/**
 * BrightnessSlideBar extends [AbstractSlider] and more being specific to implement brightness
 * slide.
 */
class BrightnessSlideBar : AbstractSlider {
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

    override fun updatePaint(colorPaint: Paint) {
        val hsv = FloatArray(3)
        Color.colorToHSV(color, hsv)
        hsv[2] = 0f
        val startColor = Color.HSVToColor(hsv)
        hsv[2] = 1f
        val endColor = Color.HSVToColor(hsv)
        val shader: Shader =
            LinearGradient(
                0f,
                0f,
                width.toFloat(),
                height.toFloat(),
                startColor,
                endColor,
                Shader.TileMode.CLAMP
            )
        colorPaint.setShader(shader)
    }

    override fun onInflateFinished() {
        selector!!.post {
            val defaultPosition = width - selector!!.width
            selector!!.x = defaultPosition.toFloat()
        }
    }

    @ColorInt
    override fun assembleColor(): Int {
        val hsv = FloatArray(3)
        Color.colorToHSV(color, hsv)
        hsv[2] = selectorPosition
        if (colorPickerView != null && colorPickerView!!.alphaSlideBar != null) {
            val alpha = (colorPickerView!!.alphaSlideBar!!.selectorPosition * 255).toInt()
            return Color.HSVToColor(alpha, hsv)
        }
        return Color.HSVToColor(hsv)
    }
}
