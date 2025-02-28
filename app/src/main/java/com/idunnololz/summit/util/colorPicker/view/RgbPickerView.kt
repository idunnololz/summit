package com.idunnololz.summit.util.colorPicker.view

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.TextView
import com.google.android.material.slider.Slider
import com.idunnololz.summit.R
import com.idunnololz.summit.databinding.ColorpickerLayoutRgbPickerBinding

class RgbPickerView : PickerView, ColorPickerContainer {
    private lateinit var red: Slider
    private lateinit var green: Slider
    private lateinit var blue: Slider
    private lateinit var redInt: TextView
    private lateinit var greenInt: TextView
    private lateinit var blueInt: TextView

    constructor(context: Context?) : super(context)

    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)

    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    )

    constructor(
        context: Context?,
        attrs: AttributeSet?,
        defStyleAttr: Int,
        defStyleRes: Int
    ) : super(context, attrs, defStyleAttr, defStyleRes)

    override fun init() {
        val b = ColorpickerLayoutRgbPickerBinding.inflate(
            LayoutInflater.from(context), this, true)
        red = b.red
        redInt = b.redInt
        green = b.green
        greenInt = b.greenInt
        blue = b.blue
        blueInt = b.blueInt

        val listener = Slider.OnChangeListener { slider, value, fromUser ->
            when (slider.id) {
                R.id.red -> redInt.text = String.format("%s", value)
                R.id.green -> greenInt.text = String.format("%s", value)
                R.id.blue -> blueInt.text = String.format("%s", value)
            }
            if (fromUser) {
                onColorPicked()
            }
        }

        red.addOnChangeListener(listener)
        green.addOnChangeListener(listener)
        blue.addOnChangeListener(listener)

        red.setProgressBarDrawable(
            GradientDrawable(
                GradientDrawable.Orientation.LEFT_RIGHT,
                intArrayOf(Color.WHITE, Color.RED)
            ),
        )
        green.setProgressBarDrawable(
            GradientDrawable(
                GradientDrawable.Orientation.LEFT_RIGHT,
                intArrayOf(Color.WHITE, Color.GREEN)
            ),
        )
        blue.setProgressBarDrawable(
            GradientDrawable(
                GradientDrawable.Orientation.LEFT_RIGHT,
                intArrayOf(Color.WHITE, Color.BLUE)
            ),
        )
    }

    override fun setColor(color: Int, animate: Boolean) {
        super.setColor(color, animate)
        val bars = arrayOf<Slider?>(red, green, blue)
        val offsets = intArrayOf(16, 8, 0)
        for (i in bars.indices) {
            val value = (color shr offsets[i]) and 0xFF
            bars[i]!!.value = value.toFloat()
        }
    }

    override var color: Int
        get() = Color.argb(
            colorAlpha,
            red.value.toInt(),
            green.value.toInt(),
            blue.value.toInt(),
        )
        set(value) {
            setColor(value, animate = false)
        }

    override val name: String
        get() = context.getString(R.string.colorPickerDialog_rgb)
}