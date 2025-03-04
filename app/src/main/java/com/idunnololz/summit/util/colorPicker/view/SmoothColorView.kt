package com.idunnololz.summit.util.colorPicker.view

import android.annotation.TargetApi
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.TransitionDrawable
import android.util.AttributeSet
import android.view.View
import androidx.annotation.ColorInt
import com.idunnololz.summit.util.colorPicker.AlphaColorDrawable

open class SmoothColorView : View {
    private var previous: AlphaColorDrawable? = null

    constructor(context: Context?) : super(context) {
        init()
    }

    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs) {
        init()
    }

    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr,
    ) {
        init()
    }

    @TargetApi(21)
    constructor(
        context: Context?,
        attrs: AttributeSet?,
        defStyleAttr: Int,
        defStyleRes: Int,
    ) : super(context, attrs, defStyleAttr, defStyleRes) {
        init()
    }

    private fun init() {
        previous = AlphaColorDrawable(Color.BLACK)
        background = previous
    }

    /**
     * Update the displayed color. The change in values will not be animated.
     *
     * @param color The new color to display.
     */
    fun setColor(@ColorInt color: Int) {
        setColor(color, false)
    }

    /**
     * Update the displayed color.
     *
     * @param color         The new color to display.
     * @param animate       Whether to animate the change in values.
     */
    fun setColor(@ColorInt color: Int, animate: Boolean) {
        val current: AlphaColorDrawable = AlphaColorDrawable(color)

        if (previous != null && animate) {
            val transition = TransitionDrawable(arrayOf(previous, current))
            background = transition
            transition.startTransition(100)
        } else {
            background = current
        }

        previous = current
    }
}
