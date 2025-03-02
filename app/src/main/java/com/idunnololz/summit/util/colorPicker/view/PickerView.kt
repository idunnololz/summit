package com.idunnololz.summit.util.colorPicker.view

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.graphics.drawable.InsetDrawable
import android.graphics.drawable.LayerDrawable
import android.util.AttributeSet
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.ColorInt
import com.google.android.material.slider.Slider
import com.idunnololz.summit.R
import com.idunnololz.summit.util.colorPicker.OnColorPickedListener
import com.idunnololz.summit.util.colorPicker.SeekBarBackgroundDrawable
import com.idunnololz.summit.util.colorPicker.utils.AlphaTileDrawable
import com.idunnololz.summit.util.colorPicker.utils.ColorPicker
import com.idunnololz.summit.util.colorPicker.utils.ColorPickerContainer
import com.idunnololz.summit.util.ext.getDimen
import java.util.Locale

abstract class PickerView :
    LinearLayout, OnColorPickedListener, ColorPicker, ColorPickerContainer {
    private var listener: OnColorPickedListener? = null

    private var alphaInt: TextView? = null
    private var alpha: Slider? = null
    private var alphaLayout: View? = null
    private var alphaTileDrawable: AlphaTileDrawable? = null

    constructor(context: Context?) : super(context) {
        init()
        postInit()
    }

    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs) {
        init()
        postInit()
    }

    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ) {
        init()
        postInit()
    }

    constructor(
        context: Context?,
        attrs: AttributeSet?,
        defStyleAttr: Int,
        defStyleRes: Int
    ) : super(context, attrs, defStyleAttr, defStyleRes) {
        init()
        postInit()
    }

    abstract fun init()

    /**
     * Called after `init()`, used to set up child views that should be
     * present in *most* pickers, such as the "alpha" slider.
     */
    private fun postInit() {
        val alphaInt = findViewById<TextView>(R.id.alphaInt).also {
            alphaInt = it
        }
        alpha = findViewById(R.id.alpha)
        alphaLayout = findViewById(R.id.alphaLayout)

        if (alpha != null) {
            alpha?.addOnChangeListener { slider, value, fromUser ->
                alphaInt.text = String.format(Locale.getDefault(), "%.2f", value)
                onColorPicked()
            }
        }

        alphaTileDrawable = AlphaTileDrawable(color)
        alpha?.background = alphaTileDrawable

        updateProgressBar()
    }

    /**
     * Set the picker's color.
     *
     * @param color         The picker's color.
     * @param animate       Whether to animate changes in values.
     */
    override fun setColor(@ColorInt color: Int, animate: Boolean) {
        setColorAlpha(Color.alpha(color), animate)
        updateProgressBar()
    }

    @get:ColorInt
    abstract override var color: Int

    /**
     * Get the "name" of the type of picker view. For example, an RGBPickerView
     * would return the string "RGB".
     *
     * @return The "name" of the type of picker.
     */
    abstract override val name: String

    /**
     * Set the color's alpha, between 0-1 (inclusive).
     *
     * @param alpha         The color's alpha, between 0-1 (inclusive).
     * @param animate       Whether to animate the change in values.
     */
    fun setColorAlpha(alpha: Int, animate: Boolean) {
        if (this.alpha == null) return

        this.alpha!!.value = alpha.toFloat()
    }

    var colorAlpha: Int
        /**
         * Gets the color's alpha, from 0-255.
         *
         * @return The color's alpha, from 0-255.
         */
        get() = if (alpha != null) alpha!!.value.toInt() else 255
        /**
         * Set the color's alpha, from 0-255. Change in values
         * will not be animated.
         *
         * @param alpha         The color's alpha, from 0-255.
         */
        set(alpha) {
            setColorAlpha(alpha, false)
        }

    /**
     * Set an interface to receive updates to color values. This may
     * be called multiple times in succession if a slider is dragged
     * or animated; be wary of performance.
     *
     * @param listener      An interface to receive color updates.
     */
    override fun setListener(listener: OnColorPickedListener?) {
        this.listener = listener
    }

    protected open fun onColorPicked() {
        onColorPicked(this, color)

        updateProgressBar()
    }

    private fun updateProgressBar() {
        alphaTileDrawable?.gradientColor = color
        alphaTileDrawable?.invalidateSelf()
        alpha?.invalidate()
    }

    override fun onColorPicked(pickerView: ColorPicker?, color: Int) {
        if (listener != null) listener!!.onColorPicked(this, this.color)
    }

    override val view: View
        get() = this

    override val colorPicker: ColorPicker
        get() = this

    override val rootView2: View
        get() = this

    fun Slider.setProgressBarDrawable(drawable: Drawable) {
        val background: Drawable = SeekBarBackgroundDrawable(
            drawable.mutate().constantState!!.newDrawable(),
        )

        this.background = background
    }
}