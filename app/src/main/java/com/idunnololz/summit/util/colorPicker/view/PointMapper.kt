package com.idunnololz.summit.util.colorPicker.view

import android.graphics.Color
import android.graphics.Point
import androidx.constraintlayout.widget.ConstraintLayout
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.sqrt

internal object PointMapper {
    internal fun getColorPoint(colorPickerView: ColorPickerView, point: Point): Point {
        return getHuePoint(colorPickerView, point)
    }

    private fun getHuePoint(colorPickerView: ColorPickerView, point: Point): Point {
        val lp = colorPickerView.palette.layoutParams as ConstraintLayout.LayoutParams
        val centerX = colorPickerView.palette.width * 0.5f + lp.marginStart
        val centerY = colorPickerView.palette.height * 0.5f + lp.topMargin
        var x = point.x - centerX
        var y = point.y - centerY
        val radius = min(
            colorPickerView.palette.width * 0.5f,
            colorPickerView.palette.height * 0.5f,
        )
        val r = sqrt((x * x + y * y).toDouble())
        if (r > radius) {
            x *= (radius / r).toFloat()
            y *= (radius / r).toFloat()
        }
        return Point((x + centerX).toInt(), (y + centerY).toInt())
    }
}
