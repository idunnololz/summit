package com.idunnololz.summit.util.colorPicker.view

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Point
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.os.Handler
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.widget.ImageView
import androidx.annotation.ColorInt
import androidx.annotation.FloatRange
import androidx.annotation.MainThread
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.graphics.alpha
import androidx.core.view.doOnPreDraw
import androidx.lifecycle.LifecycleObserver
import com.google.android.material.slider.Slider
import com.idunnololz.summit.R
import com.idunnololz.summit.databinding.ColorWheelPickerBinding
import com.idunnololz.summit.util.colorPicker.OnColorPickedListener
import com.idunnololz.summit.util.colorPicker.SeekBarBackgroundDrawable
import com.idunnololz.summit.util.colorPicker.listeners.ColorListener
import com.idunnololz.summit.util.colorPicker.listeners.ColorPickerViewListener
import com.idunnololz.summit.util.colorPicker.utils.AlphaTileDrawable
import com.idunnololz.summit.util.colorPicker.utils.ColorHsvPalette
import com.idunnololz.summit.util.colorPicker.utils.ColorPicker
import com.idunnololz.summit.util.colorPicker.utils.ColorPickerContainer
import com.idunnololz.summit.util.colorPicker.utils.PointMapper
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

class ColorPickerView : ConstraintLayout, LifecycleObserver, ColorPicker, ColorPickerContainer {

    override val view: View
        get() = this

    private var selectedWheelColor: Int = 0

    /**
     * gets the selected color.
     *
     * @return the selected color.
     */
    @get:ColorInt
    @ColorInt
    override var color: Int = 0
        private set
    override val name: String
        get() = context.getString(R.string.color_wheel)

    /**
     * gets a selector's selected coordinate.
     *
     * @return a selected coordinate [Point].
     */
    private var selectedPoint: Point? = null
    lateinit var palette: ImageView

    /**
     * gets a selector.
     *
     * @return selector.
     */
    private lateinit var selector: ImageView
    private var paletteDrawable: Drawable? = null

    lateinit var alphaSlideBar: Slider
        private set
    lateinit var brightnessSlider: Slider
        private set

    var _listener: OnColorPickedListener? = null
    private var colorListener: ColorPickerViewListener = object : ColorListener {
        override fun onColorSelected(color: Int, fromUser: Boolean) {
            if (fromUser) {
                _listener?.onColorPicked(this@ColorPickerView, color)
            }
        }
    }
    private var debounceDuration: Long = 0
    private val debounceHandler = Handler()

    private var handleTouch = true

    private var alphaTileDrawable = AlphaTileDrawable()

    constructor(context: Context) : super(context) {
        onCreate()
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        onCreate()
    }

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr,
    ) {
        onCreate()
    }

    constructor(
        context: Context,
        attrs: AttributeSet?,
        defStyleAttr: Int,
        defStyleRes: Int,
    ) : super(context, attrs, defStyleAttr, defStyleRes) {
        onCreate()
    }

    private fun onCreate() {
        val b = ColorWheelPickerBinding.inflate(LayoutInflater.from(context), this)

        palette = b.palette
        alphaSlideBar = b.alphaSlideBar
        brightnessSlider = b.brightnessSlideBar

        updateSliders()

        selector = b.selector
        selector.setImageDrawable(
            ContextCompat.getDrawable(
                context,
                R.drawable.colorpickerview_wheel,
            ),
        )

        selector.alpha = 1f

        alphaSlideBar.addOnChangeListener { slider, value, fromUser ->
            if (fromUser) {
                notifyColorChanged()
            }
        }
        brightnessSlider.addOnChangeListener { slider, value, fromUser ->
            if (fromUser) {
                notifyColorChanged()
            }
        }

        viewTreeObserver
            .addOnGlobalLayoutListener(
                object : ViewTreeObserver.OnGlobalLayoutListener {
                    override fun onGlobalLayout() {
                        viewTreeObserver.removeOnGlobalLayoutListener(this)
                        onFinishInflated()
                    }
                },
            )
        palette.doOnPreDraw {
            if (palette.drawable == null && palette.measuredWidth > 0 && palette.measuredHeight > 0) {
                val bitmap = Bitmap.createBitmap(
                    palette.measuredWidth,
                    palette.measuredHeight,
                    Bitmap.Config.ARGB_8888,
                )
                palette.setImageDrawable(ColorHsvPalette(resources, bitmap))
            }
        }

        updateSliders()
    }

    private fun onFinishInflated() {
        if (parent != null && parent is ViewGroup) {
            (parent as ViewGroup).clipChildren = false
        }

        selectCenter()
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (!this.isEnabled) {
            return false
        }
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                val snapPoint: Point =
                    PointMapper.getColorPoint(this, Point(event.x.toInt(), event.y.toInt()))

                if (snapPoint.x != event.x.toInt() || snapPoint.y != event.y.toInt()) {
                    handleTouch = false
                    return false
                }

                handleTouch = true
                selector.isPressed = true
                return onTouchReceived(event)
            }
            MotionEvent.ACTION_MOVE, MotionEvent.ACTION_UP -> {
                if (!handleTouch) {
                    return false
                }

                selector.isPressed = true
                return onTouchReceived(event)
            }

            else -> {
                selector.isPressed = false
                return false
            }
        }
    }

    /**
     * notify to the other views by the onTouchEvent.
     *
     * @param event [MotionEvent].
     * @return notified or not.
     */
    @MainThread
    private fun onTouchReceived(event: MotionEvent): Boolean {
        val snapPoint: Point =
            PointMapper.getColorPoint(this, Point(event.x.toInt(), event.y.toInt()))
        val pixelColor = getColorFromBitmap(snapPoint.x.toFloat(), snapPoint.y.toFloat())

        this.selectedWheelColor = pixelColor
        this.selectedPoint =
            PointMapper.getColorPoint(this, Point(snapPoint.x, snapPoint.y))
        setCoordinate(snapPoint.x, snapPoint.y)

        notifyColorChanged()
        return true
    }

    private fun notifyColorChanged() {
        debounceHandler.removeCallbacksAndMessages(null)
        val debounceRunnable =
            Runnable {
                fireColorListener(selectedWheelColor, true)
            }
        debounceHandler.postDelayed(debounceRunnable, this.debounceDuration)
    }

    /**
     * gets a pixel color on the specific coordinate from the bitmap.
     *
     * @param x coordinate x.
     * @param y coordinate y.
     * @return selected color.
     */
    private fun getColorFromBitmap(x: Float, y: Float): Int {
        val lp = palette.layoutParams as LayoutParams
        var x = x - lp.marginStart
        var y = y - lp.topMargin
        val invertMatrix = Matrix()
        palette.imageMatrix.invert(invertMatrix)

        val mappedPoints = floatArrayOf(x, y)
        invertMatrix.mapPoints(mappedPoints)

        if (palette.drawable != null &&
            palette.drawable is BitmapDrawable &&
            mappedPoints[0] >= 0 && mappedPoints[1] >= 0 &&
            mappedPoints[0] < palette.drawable.intrinsicWidth &&
            mappedPoints[1] < palette.drawable.intrinsicHeight
        ) {
            invalidate()

            if (palette.drawable is ColorHsvPalette) {
                x = x - palette.width * 0.5f
                y = y - palette.height * 0.5f
                val r = sqrt((x * x + y * y).toDouble())
                val radius = (
                    min(
                        palette.width.toDouble(),
                        palette.height.toDouble(),
                    ) * 0.5f
                    ).toFloat()
                val hsv = floatArrayOf(0f, 0f, 1f)
                hsv[0] = (atan2(y.toDouble(), -x.toDouble()) / Math.PI * 180f).toFloat() + 180
                hsv[1] =
                    max(0.0, min(1.0, (r / radius).toFloat().toDouble())).toFloat()
                return Color.HSVToColor(hsv)
            }
        }
        return 0
    }

    private fun fireColorListener(@ColorInt color: Int, fromUser: Boolean) {
        this.color = color
        val hsv = FloatArray(3)
        Color.colorToHSV(this.color, hsv)
        hsv[2] = brightnessSlider.value / 255f
        val alpha = (alphaSlideBar.value).toInt()
        this.color = Color.HSVToColor(alpha, hsv)
        updateSliders()

        if (colorListener is ColorListener) {
            (colorListener as ColorListener).onColorSelected(this.color, fromUser)
        }
    }

    /**
     * notify to sliders about a new trigger.
     */
    private fun updateSliders() {
        alphaTileDrawable.gradientColor = selectedWheelColor
        alphaSlideBar.background = alphaTileDrawable
        alphaSlideBar.invalidate()
        brightnessSlider.background = SeekBarBackgroundDrawable(
            GradientDrawable(
                GradientDrawable.Orientation.LEFT_RIGHT,
                intArrayOf(Color.BLACK, selectedWheelColor),
            ).mutate().constantState!!.newDrawable(),
        )
    }

    /**
     * gets an alpha value from the selected color.
     *
     * @return alpha from the selected color.
     */
    @FloatRange(from = 0.0, to = 1.0)
    override fun getAlpha(): Float {
        return Color.alpha(color) / 255f
    }

    /**
     * changes selector's selected point with notifies about changes manually.
     *
     * @param x coordinate x of the selector.
     * @param y coordinate y of the selector.
     */
    private fun setSelectorPoint(x: Int, y: Int) {
        val mappedPoint: Point = PointMapper.getColorPoint(this, Point(x, y))
        val color = getColorFromBitmap(mappedPoint.x.toFloat(), mappedPoint.y.toFloat())
        this.color = color
        selectedPoint = Point(mappedPoint.x, mappedPoint.y)
        setCoordinate(mappedPoint.x, mappedPoint.y)
        fireColorListener(this.color, false)
    }

    /**
     * changes selector's selected point without notifies.
     *
     * @param x coordinate x of the selector.
     * @param y coordinate y of the selector.
     */
    private fun setCoordinate(x: Int, y: Int) {
        selector.x = x - (selector.width * 0.5f)
        selector.y = y - (selector.measuredHeight * 0.5f)
    }

    /**
     * changes selector's selected point by a specific color.
     *
     *
     * It will throw an exception if the default palette drawable is not [ColorHsvPalette].
     *
     * @param color color.
     */
    @Throws(IllegalAccessException::class)
    fun selectByHsvColor(@ColorInt color: Int) {
        if (palette.drawable is ColorHsvPalette) {
            val hsv = FloatArray(3)
            Color.colorToHSV(color, hsv)

            val lp = palette.layoutParams as LayoutParams
            val centerX = palette.width * 0.5f + lp.marginStart
            val centerY = palette.height * 0.5f + lp.topMargin
            val radius = (
                hsv[1] * min(
                    palette.width,
                    palette.height,
                ) * 0.5f
                )
            val pointX = (radius * cos(Math.toRadians(hsv[0].toDouble())) + centerX).toInt()
            val pointY = (-radius * sin(Math.toRadians(hsv[0].toDouble())) + centerY).toInt()

            val mappedPoint: Point = PointMapper.getColorPoint(this, Point(pointX, pointY))
            this.color = color
            selectedPoint = Point(mappedPoint.x, mappedPoint.y)
            alphaSlideBar.value = color.alpha.toFloat()
            brightnessSlider.value = hsv[2] * 255f

            setCoordinate(mappedPoint.x, mappedPoint.y)
            updateSliders()
            this.selectedWheelColor = getColorFromBitmap(mappedPoint.x.toFloat(), mappedPoint.y.toFloat())
            fireColorListener(this.color, false)
        }
    }

    /**
     * selects the center of the palette manually.
     */
    private fun selectCenter() {
        setSelectorPoint(width / 2, measuredHeight / 2)
    }

    /**
     * sets enabling or not the ColorPickerView and slide bars.
     *
     * @param enabled true/false flag for making enable or not.
     */
    override fun setEnabled(enabled: Boolean) {
        super.setEnabled(enabled)

        selector.visibility =
            if (enabled) VISIBLE else INVISIBLE

        alphaSlideBar.isEnabled = enabled
        brightnessSlider.isEnabled = enabled

        if (enabled) {
            palette.clearColorFilter()
        } else {
            val color = Color.argb(70, 255, 255, 255)
            palette.setColorFilter(color)
        }
    }

    override fun setColor(color: Int, animate: Boolean) {
        if (!palette.isLaidOut) {
            post {
                selectByHsvColor(color)
            }
        } else {
            selectByHsvColor(color)
        }
    }

    override fun setListener(listener: OnColorPickedListener?) {
        _listener = listener
    }

    override val colorPicker: ColorPicker
        get() = this
    override val rootView2: View
        get() = this
}
