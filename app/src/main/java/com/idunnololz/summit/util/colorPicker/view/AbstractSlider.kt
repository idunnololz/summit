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

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Point
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.Gravity
import android.view.MotionEvent
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.annotation.ColorInt
import androidx.annotation.ColorRes
import androidx.annotation.DimenRes
import androidx.annotation.DrawableRes
import androidx.annotation.FloatRange
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import com.idunnololz.summit.R
import com.idunnololz.summit.util.colorPicker.utils.ActionMode
import kotlin.math.min

/** AbstractSlider is the abstract class for implementing sliders.  */
abstract class AbstractSlider : FrameLayout {
    var colorPickerView: ColorPickerView? = null
    protected var colorPaint: Paint? = null
    protected var borderPaint: Paint? = null
    var selectorPosition: Float = 1.0f
        set(value) {
            field = min(value.toDouble(), 1.0).toFloat()
            val x = (width * value) - selectorSize - borderHalfSize
            selectedX = getBoundaryX(x).toInt()
            selector!!.x = selectedX.toFloat()
        }

    /**
     * gets selected x coordinate.
     *
     * @return selected x coordinate.
     */
    var selectedX: Int = 0
        protected set
    protected var selectorDrawable: Drawable? = null
        set(value) {
            removeView(selector)
            field = value
            selector!!.setImageDrawable(value)
            val thumbParams =
                LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            thumbParams.gravity = Gravity.CENTER
            addView(selector, thumbParams)
        }
    protected var borderSize: Int = 2
        set(value) {
            field = value
            borderPaint!!.strokeWidth = value.toFloat()
            invalidate()
        }
    protected var borderColor: Int = Color.BLACK
        set(value) {
            field = value
            borderPaint!!.color = value
            invalidate()
        }

    /**
     * gets assembled color
     *
     * @return color
     */
    var color: Int = Color.WHITE
        protected set
    protected var selector: ImageView? = null

    constructor(context: Context) : super(context) {
        onCreate()
    }

    override fun setEnabled(enabled: Boolean) {
        super.setEnabled(enabled)
        selector!!.visibility =
            if (enabled) VISIBLE else INVISIBLE
        this.isClickable = enabled
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        onCreate()
    }

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ) {
        onCreate()
    }

    constructor(
        context: Context,
        attrs: AttributeSet?,
        defStyleAttr: Int,
        defStyleRes: Int
    ) : super(context, attrs, defStyleAttr, defStyleRes) {
        onCreate()
    }

    /** update paint color whenever the triggered colors are changed.  */
    protected abstract fun updatePaint(colorPaint: Paint)

    /**
     * assembles about the selected color.
     *
     * @return assembled color.
     */
    @ColorInt
    abstract fun assembleColor(): Int

    private fun onCreate() {
        this.colorPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        this.borderPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        borderPaint!!.style = Paint.Style.STROKE
        borderPaint!!.strokeWidth = borderSize.toFloat()
        borderPaint!!.color = borderColor
        this.setBackgroundColor(Color.WHITE)

        selector = ImageView(context)

        selectorDrawable = AppCompatResources.getDrawable(context, R.drawable.colorpickerview_wheel)

        initializeSelector()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val width = width.toFloat()
        val height = measuredHeight.toFloat()
        canvas.drawRect(0f, 0f, width, height, colorPaint!!)
        canvas.drawRect(0f, 0f, width, height, borderPaint!!)
    }

    /** called by [ColorPickerView] whenever [ColorPickerView] is triggered.  */
    fun notifyColor() {
        color = colorPickerView?.pureColor ?: return
        updatePaint(colorPaint!!)
        invalidate()
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (!this.isEnabled) {
            return false
        }

        if (colorPickerView != null) {
            when (event.actionMasked) {
                MotionEvent.ACTION_UP, MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                    selector!!.isPressed = true
                    if (event.x > width || event.x < 0) {
                        return false
                    } else {
                        onTouchReceived(event)
                        return true
                    }
                }

                else -> {
                    selector!!.isPressed = false
                    return false
                }
            }
        } else {
            return false
        }
    }

    private fun onTouchReceived(event: MotionEvent) {
        val colorPickerView = colorPickerView!!
        var eventX = event.x
        val left = selector!!.width / 2f
        val right = width - left
        if (eventX > right) eventX = right
        selectorPosition = (eventX - left) / (right - left)
        if (selectorPosition < 0) selectorPosition = 0f
        if (selectorPosition > 1.0f) selectorPosition = 1.0f
        val snapPoint = Point(event.x.toInt(), event.y.toInt())
        selectedX = getBoundaryX(snapPoint.x.toFloat()).toInt()
        selector!!.x = selectedX.toFloat()
        if (colorPickerView.getActionMode() === ActionMode.LAST) {
            if (event.action == MotionEvent.ACTION_UP) {
                colorPickerView.fireColorListener(assembleColor(), true)
            }
        } else {
            colorPickerView.fireColorListener(assembleColor(), true)
        }

        val maxPos = width - selector!!.width
        if (selector!!.x >= maxPos) selector!!.x = maxPos.toFloat()
        if (selector!!.x <= 0) selector!!.x = 0f
    }

    fun updateSelectorX(x: Int) {
        val left = selector!!.width / 2f
        val right = width - left
        selectorPosition = (x - left) / (right - left)
        if (selectorPosition < 0) selectorPosition = 0f
        if (selectorPosition > 1.0f) selectorPosition = 1.0f
        selectedX = getBoundaryX(x.toFloat()).toInt()
        selector!!.x = selectedX.toFloat()
        colorPickerView?.fireColorListener(assembleColor(), false)
    }

    fun setSelectorByHalfSelectorPosition(
        @FloatRange(from = 0.0, to = 1.0) selectorPosition: Float
    ) {
        this.selectorPosition = min(selectorPosition.toDouble(), 1.0).toFloat()
        val x = (width * selectorPosition) - (selectorSize * 0.5f) - borderHalfSize
        selectedX = getBoundaryX(x).toInt()
        selector!!.x = selectedX.toFloat()
    }

    private fun getBoundaryX(x: Float): Float {
        val maxPos = width - selector!!.width / 2
        if (x >= maxPos) return maxPos.toFloat()
        if (x <= selectorSize / 2f) return 0f
        return x - selectorSize / 2f
    }

    protected val selectorSize: Int
        get() = (selector!!.width)

    protected val borderHalfSize: Int
        get() = (borderSize * 0.5f).toInt()

    private fun initializeSelector() {
        viewTreeObserver
            .addOnGlobalLayoutListener(
                object : ViewTreeObserver.OnGlobalLayoutListener {
                    override fun onGlobalLayout() {
                        viewTreeObserver.removeOnGlobalLayoutListener(this)
                        onInflateFinished()
                    }
                })
    }

    /**
     * sets a drawable resource of the selector.
     *
     * @param resource a drawable resource of the selector.
     */
    fun setSelectorDrawableRes(@DrawableRes resource: Int) {
        val drawable = ResourcesCompat.getDrawable(
            context.resources, resource, null
        )
        selectorDrawable = drawable
    }

    /**
     * sets a color resource of the slider border.
     *
     * @param resource color resource of the slider border.
     */
    fun setBorderColorRes(@ColorRes resource: Int) {
        borderColor = ContextCompat.getColor(context, resource)
    }

    /**
     * sets a size of the slide border using dimension resource.
     *
     * @param resource a size of the slide border.
     */
    fun setBorderSizeRes(@DimenRes resource: Int) {
        borderSize = context.resources.getDimension(resource).toInt()
    }

    /** called when the inflating finished.  */
    abstract fun onInflateFinished()

    /**
     * attaches [ColorPickerView] to slider.
     *
     * @param colorPickerView [ColorPickerView].
     */
    fun attachColorPickerView(colorPickerView: ColorPickerView?) {
        this.colorPickerView = colorPickerView
    }
}
