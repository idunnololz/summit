package com.idunnololz.summit.util.colorPicker.view

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Point
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Handler
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.widget.ImageView
import androidx.annotation.ColorInt
import androidx.annotation.ColorRes
import androidx.annotation.FloatRange
import androidx.annotation.MainThread
import androidx.annotation.Px
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.PARENT_ID
import androidx.core.content.ContextCompat
import androidx.core.view.doOnLayout
import androidx.core.view.doOnNextLayout
import androidx.core.view.doOnPreDraw
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.idunnololz.summit.R
import com.idunnololz.summit.databinding.ColorWheelPickerBinding
import com.idunnololz.summit.util.colorPicker.OnColorPickedListener
import com.idunnololz.summit.util.colorPicker.utils.ActionMode
import com.idunnololz.summit.util.colorPicker.utils.ColorEnvelope
import com.idunnololz.summit.util.colorPicker.utils.ColorHsvPalette
import com.idunnololz.summit.util.colorPicker.listeners.ColorEnvelopeListener
import com.idunnololz.summit.util.colorPicker.listeners.ColorListener
import com.idunnololz.summit.util.colorPicker.listeners.ColorPickerViewListener
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * ColorPickerView implements getting HSV colors, ARGB values, Hex color codes from any image
 * drawables.
 *
 *
 * [ColorPickerViewListener] will be invoked whenever ColorPickerView is triggered by
 * [ActionMode] rules.
 *
 *
 * Implements [FlagView], [AlphaSlideBar] and [BrightnessSlideBar] optional.
 */
@Suppress("unused")
class ColorPickerView : ConstraintLayout, LifecycleObserver, ColorPicker, ColorPickerContainer {
    /**
     * gets the selected pure color without alpha and brightness.
     *
     * @return the selected pure color.
     */
    /**
     * sets the pure color.
     *
     * @param color the pure color.
     */
    @get:ColorInt
    @ColorInt
    var pureColor: Int = 0

    private var queuedColor: Int? = null
    override fun setColor(color: Int, animate: Boolean) {
        if (!palette.isLaidOut) {
            queuedColor = color
        } else {
            selectByHsvColor(color)
        }
    }

    override fun setListener(listener: OnColorPickedListener?) {
        _listener = listener
    }

    override val view: View
        get() = this

    override var isAlphaEnabled: Boolean = true

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
        get() = "Wheel"
    override val isTrackingTouch: Boolean
        get() = false

    /**
     * gets a selector's selected coordinate.
     *
     * @return a selected coordinate [Point].
     */
    var selectedPoint: Point? = null
        private set
    lateinit var palette: ImageView

    /**
     * gets a selector.
     *
     * @return selector.
     */
    lateinit var selector: ImageView
        private set
    private var paletteDrawable: Drawable? = null
    private var selectorDrawable: Drawable? = null

    /**
     * gets an [AlphaSlideBar].
     *
     * @return [AlphaSlideBar].
     */
    var alphaSlideBar: AlphaSlideBar? = null
        private set

    /**
     * gets an [BrightnessSlideBar].
     *
     * @return [BrightnessSlideBar].
     */
    var brightnessSlider: BrightnessSlideBar? = null
        private set
    var _listener: OnColorPickedListener? = null
    var colorListener: ColorPickerViewListener = object : ColorListener {
        override fun onColorSelected(color: Int, fromUser: Boolean) {
            if (fromUser) {
                _listener?.onColorPicked(this@ColorPickerView, color)
            }
        }
    }
    /**
     * gets a debounce duration.
     *
     *
     * only emit a color to the listener if a particular timespan has passed without it emitting
     * another value.
     *
     * @return debounceDuration.
     */
    /**
     * sets a debounce duration.
     *
     *
     * only emit a color to the listener if a particular timespan has passed without it emitting
     * another value.
     *
     * @param debounceDuration intervals.
     */
    private var debounceDuration: Long = 0
    private val debounceHandler = Handler()

    private var actionMode: ActionMode? = ActionMode.ALWAYS

    @FloatRange(from = 0.0, to = 1.0)
    private var selectorAlpha = 1.0f

    @FloatRange(from = 0.0, to = 1.0)
    private var flagAlpha = 1.0f

    private val flagIsFlipAble = true

    @Px
    private var selectorSize = 0

    private var visibleFlag = false
    private var handleTouch = true

    constructor(context: Context) : super(context) {
        onCreate()
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

    private fun onCreate() {
        val b = ColorWheelPickerBinding.inflate(
            LayoutInflater.from(context), this)
        palette = b.palette

        pureColor = Color.WHITE
        notifyToSlideBars()

        selector = b.selector
        if (selectorDrawable != null) {
            selector.setImageDrawable(selectorDrawable)
        } else {
            selector.setImageDrawable(
                ContextCompat.getDrawable(
                    context,
                    R.drawable.colorpickerview_wheel
                )
            )
        }

        selector.alpha = selectorAlpha

        if (!visibleFlag) {
            visibleFlag = true
            selectorAlpha = selector.alpha
            selector.alpha = 0.0f
        }

        attachAlphaSlider(b.alphaSlideBar)
        attachBrightnessSlider(b.brightnessSlideBar)

        viewTreeObserver
            .addOnGlobalLayoutListener(
                object : ViewTreeObserver.OnGlobalLayoutListener {
                    override fun onGlobalLayout() {
                        viewTreeObserver.removeOnGlobalLayoutListener(this)
                        onFinishInflated()
                    }
                })
        palette.doOnPreDraw {
            if (palette.drawable == null && palette.measuredWidth > 0 && palette.measuredHeight > 0) {
                val bitmap = Bitmap.createBitmap(
                    palette.measuredWidth, palette.measuredHeight, Bitmap.Config.ARGB_8888)
                palette.setImageDrawable(ColorHsvPalette(resources, bitmap))

                if (queuedColor != null) {
                    this.selectByHsvColor(queuedColor!!)
                    queuedColor = null
                }
            }
        }
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

        this.pureColor = pixelColor
        this.color = pixelColor
        this.selectedPoint =
            PointMapper.getColorPoint(this, Point(snapPoint.x, snapPoint.y))
        setCoordinate(snapPoint.x, snapPoint.y)

        if (actionMode === ActionMode.LAST) {
            if (event.action == MotionEvent.ACTION_UP) {
                notifyColorChanged()
            }
        } else {
            notifyColorChanged()
        }
        return true
    }

    val isHuePalette: Boolean = true

    /**
     * notifies color changes to [ColorListener], [FlagView]. [AlphaSlideBar],
     * [BrightnessSlideBar] with the debounce duration.
     */
    private fun notifyColorChanged() {
        debounceHandler.removeCallbacksAndMessages(null)
        val debounceRunnable =
            Runnable {
                fireColorListener(color, true)
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
    fun getColorFromBitmap(x: Float, y: Float): Int {
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
                val radius = (min(
                    palette.width.toDouble(),
                    palette.height.toDouble()
                ) * 0.5f).toFloat()
                val hsv = floatArrayOf(0f, 0f, 1f)
                hsv[0] = (atan2(y.toDouble(), -x.toDouble()) / Math.PI * 180f).toFloat() + 180
                hsv[1] =
                    max(0.0, min(1.0, (r / radius).toFloat().toDouble())).toFloat()
                return Color.HSVToColor(hsv)
            } else {
                val rect = palette.drawable.bounds
                val scaleX = mappedPoints[0] / rect.width()
                val x1 = (scaleX * (palette.drawable as BitmapDrawable).bitmap.width).toInt()
                val scaleY = mappedPoints[1] / rect.height()
                val y1 = (scaleY * (palette.drawable as BitmapDrawable).bitmap.height).toInt()
                return (palette.drawable as BitmapDrawable).bitmap.getPixel(x1, y1)
            }
        }
        return 0
    }

    /**
     * invokes [ColorListener] or [ColorEnvelopeListener] with a color value.
     *
     * @param color    color.
     * @param fromUser triggered by user or not.
     */
    fun fireColorListener(@ColorInt color: Int, fromUser: Boolean) {
        this.color = color
        if (alphaSlideBar != null) {
            alphaSlideBar!!.notifyColor()
            this.color = alphaSlideBar!!.assembleColor()
        }
        if (brightnessSlider != null) {
            brightnessSlider!!.notifyColor()
            this.color = brightnessSlider!!.assembleColor()
        }

        if (colorListener is ColorListener) {
            (colorListener as ColorListener).onColorSelected(this.color, fromUser)
        } else if (colorListener is ColorEnvelopeListener) {
            val envelope = ColorEnvelope(this.color)
            (colorListener as ColorEnvelopeListener).onColorSelected(envelope, fromUser)
        }

        if (visibleFlag) {
            visibleFlag = false
            selector.alpha = selectorAlpha
        }
    }

    /**
     * notify to sliders about a new trigger.
     */
    private fun notifyToSlideBars() {
        if (alphaSlideBar != null) alphaSlideBar!!.notifyColor()
        if (brightnessSlider != null) {
            brightnessSlider!!.notifyColor()

            if (brightnessSlider!!.assembleColor() != Color.WHITE) {
                color = brightnessSlider!!.assembleColor()
            } else if (alphaSlideBar != null) color = alphaSlideBar!!.assembleColor()
        }
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

    val colorEnvelope: ColorEnvelope
        /**
         * gets the [ColorEnvelope] of the selected color.
         *
         * @return [ColorEnvelope].
         */
        get() = ColorEnvelope(color)

    /**
     * gets center coordinate of the selector.
     *
     * @param x coordinate x.
     * @param y coordinate y.
     * @return the center coordinate of the selector.
     */
    private fun getCenterPoint(x: Int, y: Int): Point {
        return Point(x - (selector.width / 2), y - (selector.measuredHeight / 2))
    }

    val selectorX: Float
        /**
         * gets a selector's selected coordinate x.
         *
         * @return a selected coordinate x.
         */
        get() = selector.x - (selector.width * 0.5f)

    val selectorY: Float
        /**
         * gets a selector's selected coordinate y.
         *
         * @return a selected coordinate y.
         */
        get() = selector.y - (selector.measuredHeight * 0.5f)

    /**
     * changes selector's selected point with notifies about changes manually.
     *
     * @param x coordinate x of the selector.
     * @param y coordinate y of the selector.
     */
    fun setSelectorPoint(x: Int, y: Int) {
        val mappedPoint: Point = PointMapper.getColorPoint(this, Point(x, y))
        val color = getColorFromBitmap(mappedPoint.x.toFloat(), mappedPoint.y.toFloat())
        pureColor = color
        this.color = color
        selectedPoint = Point(mappedPoint.x, mappedPoint.y)
        setCoordinate(mappedPoint.x, mappedPoint.y)
        fireColorListener(this.color, false)
    }

    /**
     * moves selector's selected point with notifies about changes manually.
     *
     * @param x coordinate x of the selector.
     * @param y coordinate y of the selector.
     */
    fun moveSelectorPoint(x: Int, y: Int, @ColorInt color: Int) {
        pureColor = color
        this.color = color
        selectedPoint = Point(x, y)
        setCoordinate(x, y)
        fireColorListener(this.color, false)
    }

    /**
     * changes selector's selected point without notifies.
     *
     * @param x coordinate x of the selector.
     * @param y coordinate y of the selector.
     */
    fun setCoordinate(x: Int, y: Int) {
        selector.x = x - (selector.width * 0.5f)
        selector.y = y - (selector.measuredHeight * 0.5f)
    }

    /**
     * select a point by a specific color. this method will not work if the default palette drawable
     * is not [ColorHsvPalette].
     *
     * @param color a starting color.
     */
    fun setInitialColor(@ColorInt color: Int) {
        post {
            try {
                selectByHsvColor(color)
            } catch (e: IllegalAccessException) {
                e.printStackTrace()
            }
        }
    }

    /**
     * select a point by a specific color resource. this method will not work if the default palette
     * drawable is not [ColorHsvPalette].
     *
     * @param colorRes a starting color resource.
     */
    fun setInitialColorRes(@ColorRes colorRes: Int) {
        setInitialColor(ContextCompat.getColor(context, colorRes))
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
            val radius = (hsv[1] * min(
                palette.width,
                palette.height,
            ) * 0.5f)
            val pointX = (radius * cos(Math.toRadians(hsv[0].toDouble())) + centerX).toInt()
            val pointY = (-radius * sin(Math.toRadians(hsv[0].toDouble())) + centerY).toInt()

            val mappedPoint: Point = PointMapper.getColorPoint(this, Point(pointX, pointY))
            pureColor = color
            this.color = color
            selectedPoint = Point(mappedPoint.x, mappedPoint.y)
            if (alphaSlideBar != null) {
                alphaSlideBar!!.setSelectorByHalfSelectorPosition(alpha)
            }
            if (brightnessSlider != null) {
                brightnessSlider!!.setSelectorByHalfSelectorPosition(hsv[2])
            }
            setCoordinate(mappedPoint.x, mappedPoint.y)
            fireColorListener(this.color, false)
        } else {
//            throw IllegalAccessException(
//                "selectByHsvColor(@ColorInt int color) can be called only "
//                        + "when the palette is an instance of ColorHsvPalette. Use setHsvPaletteDrawable();"
//            )
        }
    }

    /**
     * changes selector's selected point by a specific color resource.
     *
     *
     * It may not work properly if change the default palette drawable.
     *
     * @param resource a color resource.
     */
    @Throws(IllegalAccessException::class)
    fun selectByHsvColorRes(@ColorRes resource: Int) {
        selectByHsvColor(ContextCompat.getColor(context, resource))
    }

    /**
     * changes selector drawable manually.
     *
     * @param drawable selector drawable.
     */
    fun setSelectorDrawable(drawable: Drawable?) {
        selector.setImageDrawable(drawable)
    }

    /**
     * selects the center of the palette manually.
     */
    fun selectCenter() {
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

        if (alphaSlideBar != null) {
            alphaSlideBar!!.isEnabled = enabled
        }

        if (brightnessSlider != null) {
            brightnessSlider!!.isEnabled = enabled
        }

        if (enabled) {
            palette.clearColorFilter()
        } else {
            val color = Color.argb(70, 255, 255, 255)
            palette.setColorFilter(color)
        }
    }

    /**
     * gets an [ActionMode].
     *
     * @return [ActionMode].
     */
    fun getActionMode(): ActionMode? {
        return this.actionMode
    }

    /**
     * sets an [ActionMode].
     *
     * @param actionMode [ActionMode].
     */
    fun setActionMode(actionMode: ActionMode?) {
        this.actionMode = actionMode
    }

    /**
     * linking an [AlphaSlideBar] on the [ColorPickerView].
     *
     * @param alphaSlideBar [AlphaSlideBar].
     */
    fun attachAlphaSlider(alphaSlideBar: AlphaSlideBar) {
        this.alphaSlideBar = alphaSlideBar
        alphaSlideBar.attachColorPickerView(this)
        alphaSlideBar.notifyColor()
    }

    /**
     * linking an [BrightnessSlideBar] on the [ColorPickerView].
     *
     * @param brightnessSlider [BrightnessSlideBar].
     */
    fun attachBrightnessSlider(brightnessSlider: BrightnessSlideBar) {
        this.brightnessSlider = brightnessSlider
        brightnessSlider.attachColorPickerView(this)
        brightnessSlider.notifyColor()
    }

    /**
     * sets the [LifecycleOwner].
     *
     * @param lifecycleOwner [LifecycleOwner].
     */
    fun setLifecycleOwner(lifecycleOwner: LifecycleOwner) {
        lifecycleOwner.lifecycle.addObserver(this)
    }

    /**
     * removes this color picker observer from the the [LifecycleOwner].
     *
     * @param lifecycleOwner [LifecycleOwner].
     */
    fun removeLifecycleOwner(lifecycleOwner: LifecycleOwner) {
        lifecycleOwner.lifecycle.removeObserver(this)
    }

    private fun makePaletteLayoutParams() =
        LayoutParams(0, 0).apply {
            startToStart = PARENT_ID
            endToEnd = PARENT_ID
            topToTop = PARENT_ID
            dimensionRatio = "1:1"
        }

    override val colorPicker: ColorPicker
        get() = this
    override val rootView2: View
        get() = this
}