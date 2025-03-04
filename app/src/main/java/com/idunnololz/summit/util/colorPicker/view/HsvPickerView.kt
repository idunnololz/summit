package com.idunnololz.summit.util.colorPicker.view

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.TextView
import com.google.android.material.slider.Slider
import com.idunnololz.summit.R
import com.idunnololz.summit.databinding.ColorpickerLayoutHsvPickerBinding
import com.idunnololz.summit.util.colorPicker.ColorUtils
import java.util.Locale

class HsvPickerView : PickerView {
    private lateinit var hue: Slider
    private lateinit var saturation: Slider
    private lateinit var brightness: Slider
    private lateinit var hueInt: TextView
    private lateinit var saturationInt: TextView
    private lateinit var brightnessInt: TextView

    constructor(context: Context?) : super(context)

    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)

    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr,
    )

    constructor(
        context: Context?,
        attrs: AttributeSet?,
        defStyleAttr: Int,
        defStyleRes: Int,
    ) : super(context, attrs, defStyleAttr, defStyleRes)

    override fun init() {
        val b = ColorpickerLayoutHsvPickerBinding.inflate(
            LayoutInflater.from(context),
            this,
            true,
        )
        hue = b.hue
        hueInt = b.hueInt
        saturation = b.saturation
        saturationInt = b.saturationInt
        brightness = b.brightness
        brightnessInt = b.brightnessInt

        val listener = Slider.OnChangeListener { slider, value, fromUser ->
            when (slider.id) {
                R.id.hue -> hueInt.text = String.format("%s", (value).toInt())
                R.id.saturation ->
                    saturationInt.text = String.format(
                        Locale.getDefault(), "%.2f", value / 255f,
                    )

                R.id.brightness ->
                    brightnessInt.text = String.format(
                        Locale.getDefault(), "%.2f", value / 255f,
                    )
            }
            if (fromUser) {
                onColorPicked()
            }
        }

        hue.addOnChangeListener(listener)
        saturation.addOnChangeListener(listener)
        brightness.addOnChangeListener(listener)
    }

    override fun setColor(color: Int, animate: Boolean) {
        super.setColor(color, animate)

        val bars = arrayOf(hue, saturation, brightness)
        val values = FloatArray(3)
        Color.colorToHSV(color, values)
        values[1] *= 255f
        values[2] *= 255f

        for (i in bars.indices) {
            bars[i].value = values[i]
        }

        updateProgressBars()
    }

    override var color: Int
        get() {
            val color = Color.HSVToColor(
                floatArrayOf(
                    hue.value,
                    saturation.value / 255f,
                    brightness.value / 255f,
                ),
            )
            return (colorAlpha shl 24) or (color and 0x00ffffff)
        }
        set(value) {
            setColor(value, animate = false)
        }

    override var name: String = context.getString(R.string.colorPickerDialog_hsv)

    override fun onColorPicked() {
        super.onColorPicked()
        updateProgressBars()
    }

    private fun updateProgressBars() {
        hue.setProgressBarDrawable(
            GradientDrawable(
                GradientDrawable.Orientation.LEFT_RIGHT,
                ColorUtils.getColorWheelArr(
                    saturation.value / 255f,
                    brightness.value / 255f,
                ),
            ),
        )

        saturation.setProgressBarDrawable(
            GradientDrawable(
                GradientDrawable.Orientation.LEFT_RIGHT,
                intArrayOf(
                    Color.HSVToColor(
                        floatArrayOf(
                            hue.value,
                            0f,
                            brightness.value / 255f,
                        ),
                    ),
                    Color.HSVToColor(
                        floatArrayOf(
                            hue.value,
                            1f,
                            brightness.value / 255f,
                        ),
                    ),
                ),
            ),
        )

        brightness.setProgressBarDrawable(
            GradientDrawable(
                GradientDrawable.Orientation.LEFT_RIGHT,
                intArrayOf(
                    Color.HSVToColor(
                        floatArrayOf(
                            hue.value,
                            saturation.value / 255f,
                            0f,
                        ),
                    ),
                    Color.HSVToColor(
                        floatArrayOf(
                            hue.value,
                            saturation.value / 255f,
                            1f,
                        ),
                    ),
                ),
            ),
        )
    }
}
