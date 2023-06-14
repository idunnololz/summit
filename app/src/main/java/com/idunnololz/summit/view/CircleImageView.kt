package com.idunnololz.summit.view

import android.animation.ArgbEvaluator
import android.animation.ValueAnimator
import android.annotation.TargetApi
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.*
import android.graphics.drawable.*
import android.graphics.drawable.shapes.OvalShape
import android.net.Uri
import android.os.Build
import androidx.annotation.ColorInt
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.core.content.ContextCompat
import androidx.appcompat.widget.AppCompatImageView
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.ViewOutlineProvider
import com.idunnololz.summit.R
import com.idunnololz.summit.util.Utils

class CircleImageView : AppCompatImageView {

    companion object {

        private val SCALE_TYPE = ScaleType.CENTER_CROP

        private val BITMAP_CONFIG = Bitmap.Config.ARGB_8888
        private const val COLORDRAWABLE_DIMENSION = 2

        private const val DEFAULT_BORDER_WIDTH = 0
        private const val DEFAULT_BORDER_COLOR = Color.BLACK
        private const val DEFAULT_FILL_COLOR = Color.TRANSPARENT
        private const val DEFAULT_BORDER_OVERLAY = false
        private const val ANIMATION_DURATION = 300
    }

    private val drawableRect = RectF()
    private val borderRect = RectF()

    private val shaderMatrix = Matrix()
    private val bitmapPaint = Paint()
    private val borderPaint = Paint()
    private val fillPaint = Paint()

    @ColorInt
    private var borderColor = DEFAULT_BORDER_COLOR
    var borderWidth = DEFAULT_BORDER_WIDTH
        set(newValue) {
            if (field == newValue) {
                return
            }

            field = newValue
            setup()
        }

    private var bitmap: Bitmap? = null
    private var bitmapShader: BitmapShader? = null
    private var bitmapWidth: Int = 0
    private var bitmapHeight: Int = 0

    private var drawableRadius: Float = 0.toFloat()
    private var borderRadius: Float = 0.toFloat()

    private var colorFilter: ColorFilter? = null

    private var ready: Boolean = false
    private var setupPending: Boolean = false
    private var borderOverlay: Boolean = false
    private var isDisableCircularTransformation: Boolean = false
        set(disableCircularTransformation) {
            if (isDisableCircularTransformation == disableCircularTransformation) {
                return
            }

            field = disableCircularTransformation
            initializeBitmap()
        }

    private val isRipple = Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP
    private var drawOverlay = false
    private val paint: Paint

    var inset: Int = 0
        set(newInset) {
            field = newInset
            setup()
        }
    private var rippleDrawable: Drawable? = null
    private var clippingPath: Path = Path()
    private val captionPaint: Paint
    private val bgPaint: Paint
    private var captionMaxHeight: Int = 0
    private var captionMarginTop: Int = 0
    private var captionMarginBottom: Int = 0

    private var colorAnimator: ValueAnimator? = null

    /**
     * This group of variables are for the "is free" custom ui
     */
    var isFree: Boolean = false
        set(b) {
            field = b
            invalidate()
        }
    private var shapeSize: Float = 0.toFloat()
    private val freeBgPaint: Paint
    private val freeFgPaint: Paint
    private val freeShapePath = Path()
    private val freeShapePath2 = Path()

    private var _width: Int = 0
    private var _height: Int = 0

    var isLoading = false
        set(b) {
            field = b
            setup()
        }
    private val loadingDrawable: Drawable?

    /**
     * The color drawn behind the circle-shaped drawable.
     */
    var fillColor: Int = DEFAULT_FILL_COLOR
        set(@ColorInt fillColor) {
            if (fillColor == field) {
                return
            }

            field = fillColor
            fillPaint.color = fillColor
            invalidate()
        }

    constructor(context: Context) : super(context)

    @JvmOverloads
    constructor(context: Context, attrs: AttributeSet, defStyle: Int = 0) : super(
        context,
        attrs,
        defStyle
    ) {
        val a = context.obtainStyledAttributes(attrs, R.styleable.CircleImageView, defStyle, 0)

        borderWidth =
            a.getDimensionPixelSize(R.styleable.CircleImageView_border_width, DEFAULT_BORDER_WIDTH)
        borderColor = a.getColor(R.styleable.CircleImageView_border_color, DEFAULT_BORDER_COLOR)
        borderOverlay =
            a.getBoolean(R.styleable.CircleImageView_border_overlay, DEFAULT_BORDER_OVERLAY)
        fillColor = a.getColor(R.styleable.CircleImageView_fill_color, DEFAULT_FILL_COLOR)
        inset = a.getDimensionPixelSize(R.styleable.CircleImageView_inset, 0)

        a.recycle()
    }

    init {
        super.setScaleType(SCALE_TYPE)
        ready = true

        loadingDrawable =
            null//ContextCompat.getDrawable(context, R.drawable.ic_image_black_24dp)!!.mutate()

        @Suppress("NewApi") // isRipple will ensure the that the API level req is met
        if (isRipple) {
            val circle = ShapeDrawable(OvalShape())
            val color = ColorStateList.valueOf(ContextCompat.getColor(context, R.color.white97))
            rippleDrawable = RippleDrawable(color, null, circle)
            rippleDrawable?.callback = this

            val viewOutlineProvider = object : ViewOutlineProvider() {
                override fun getOutline(view: View, outline: Outline) {
                    outline.setOval(0, 0, borderRect.right.toInt(), borderRect.bottom.toInt())
                }
            }
            outlineProvider = viewOutlineProvider
        }

        paint = Paint().apply {
            color = ContextCompat.getColor(context, R.color.white50)
        }

        captionPaint = Paint()
        captionPaint.textAlign = Paint.Align.CENTER
        captionPaint.textSize = Utils.convertDpToPixel(12f)

        bgPaint = Paint()
        bgPaint.color = ContextCompat.getColor(context, R.color.black50)

        captionMarginTop = Utils.convertDpToPixel(5f).toInt()
        captionMarginBottom = Utils.convertDpToPixel(7f).toInt()

        val textSize = Rect()
        captionPaint.getTextBounds("ABC", 0, 3, textSize)
        captionMaxHeight = textSize.height()

        freeBgPaint = Paint()
        freeBgPaint.color = ContextCompat.getColor(context, R.color.style_green)
        freeFgPaint = Paint()
        freeFgPaint.color = ContextCompat.getColor(context, R.color.white)

        if (setupPending) {
            setup()
            setupPending = false
        }
    }

    override fun invalidateDrawable(dr: Drawable) {
        if (dr === rippleDrawable) {
            invalidate()
        } else {
            super.invalidateDrawable(dr)
        }
    }

    override fun drawableStateChanged() {
        super.drawableStateChanged()

        if (isRipple) {
            val drawable = rippleDrawable
            if (drawable != null && drawable.isStateful && drawable.setState(drawableState)) {
                invalidateDrawable(drawable)
            }
        }
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    override fun dispatchTouchEvent(event: MotionEvent): Boolean {
        if (isClickable && drawable != null) {
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    if (isRipple) {
                        rippleDrawable?.setHotspot(event.x, event.y)
                        rippleDrawable?.state =
                            intArrayOf(android.R.attr.state_pressed, android.R.attr.state_enabled)
                    } else {
                        drawOverlay = true
                    }
                    invalidate()
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    Log.d("On", "destroy")
                    if (isRipple) {
                        rippleDrawable?.state = intArrayOf()
                    } else {
                        drawOverlay = false
                    }
                    invalidate()
                }
            }
        }

        return super.dispatchTouchEvent(event)
    }

    override fun getScaleType(): ScaleType {
        return SCALE_TYPE
    }

    override fun setScaleType(scaleType: ScaleType) {
        //        if (scaleType != SCALE_TYPE) {
        //            throw new IllegalArgumentException(String.format("ScaleType %s not supported.", scaleType));
        //        }
    }

    override fun setAdjustViewBounds(adjustViewBounds: Boolean) {
        require(!adjustViewBounds) { "adjustViewBounds not supported." }
    }

    override fun onDraw(canvas: Canvas) {
        if (isDisableCircularTransformation) {
            super.onDraw(canvas)
            return
        }

        if (bitmap == null && !isLoading) {
            return
        }

        if (isFree) {
            canvas.drawPath(freeShapePath, freeBgPaint)
        }

        if (fillColor != Color.TRANSPARENT) {
            canvas.drawCircle(
                drawableRect.centerX(),
                drawableRect.centerY(),
                drawableRadius,
                fillPaint
            )
        }

        if (isLoading && loadingDrawable != null) {
            val l = (_height - loadingDrawable.intrinsicWidth) / 2
            val t = (_height - loadingDrawable.intrinsicHeight) / 2
            loadingDrawable.setBounds(
                l,
                t,
                l + loadingDrawable.intrinsicWidth,
                t + loadingDrawable.intrinsicHeight
            )
            loadingDrawable.draw(canvas)
        } else if (bitmap != null) {
            canvas.drawCircle(
                drawableRect.centerX(),
                drawableRect.centerY(),
                drawableRadius,
                bitmapPaint
            )
        }

        if (drawOverlay) {
            canvas.drawCircle(drawableRect.centerX(), drawableRect.centerY(), drawableRadius, paint)
        } else if (isRipple && Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT && isAttachedToWindow) {
            rippleDrawable!!.draw(canvas)
        }

        if (borderWidth > 0) {
            canvas.drawCircle(borderRect.centerX(), borderRect.centerY(), borderRadius, borderPaint)
        }

        if (isFree) {
            canvas.drawPath(freeShapePath2, freeFgPaint)
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)

        _width = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            measuredWidth.coerceAtMost(maxWidth)
        } else {
            measuredWidth
        }
        _height = _width

        setMeasuredDimension(_width, _height)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        setup()
    }

    override fun setImageBitmap(bm: Bitmap) {
        super.setImageBitmap(bm)
        initializeBitmap()
    }

    override fun setImageDrawable(drawable: Drawable?) {
        super.setImageDrawable(drawable)
        initializeBitmap()
    }

    override fun setImageResource(@DrawableRes resId: Int) {
        super.setImageResource(resId)
        initializeBitmap()
    }

    override fun setImageURI(uri: Uri?) {
        super.setImageURI(uri)
        initializeBitmap()
    }

    override fun setColorFilter(cf: ColorFilter) {
        if (cf === colorFilter) {
            return
        }

        colorFilter = cf
        applyColorFilter()
        invalidate()
    }

    override fun getColorFilter(): ColorFilter? {
        return colorFilter
    }

    private fun applyColorFilter() {
        bitmapPaint.colorFilter = colorFilter
    }

    fun setBorderColor(@ColorInt borderColor: Int, animate: Boolean = false) {
        if (this.borderColor == borderColor) {
            return
        }

        if (animate) {
            colorAnimator?.cancel()

            val colorAnimator = colorAnimator ?: ValueAnimator.ofObject(
                ArgbEvaluator(),
                this.borderColor,
                borderColor
            ).apply {
                addUpdateListener { animator ->
                    val c = animator.animatedValue as Int
                    borderPaint.color = c
                    captionPaint.color = c
                    freeBgPaint.color = c
                    invalidate()
                }
            }

            colorAnimator.setObjectValues(this.borderColor, borderColor)
            colorAnimator.duration = ANIMATION_DURATION.toLong()
            colorAnimator.start()

            this.borderColor = borderColor
        } else {
            this.borderColor = borderColor
            borderPaint.color = this.borderColor
            captionPaint.color = this.borderColor
            freeBgPaint.color = this.borderColor
            invalidate()
        }
    }

    @Deprecated("Use {@link #setBorderColor(int)} instead")
    fun setBorderColorResource(@ColorRes borderColorRes: Int) {
        borderColor = ContextCompat.getColor(context, borderColorRes)
    }

    /**
     * Set a color to be drawn behind the circle-shaped drawable. Note that
     * this has no effect if the drawable is opaque or no drawable is set.
     *
     * @param fillColorRes The color resource to be resolved to a color and
     * drawn behind the drawable
     */
    fun setFillColorResource(@ColorRes fillColorRes: Int) {
        fillColor = ContextCompat.getColor(context, fillColorRes)
    }

    private fun getBitmapFromDrawable(drawable: Drawable?): Bitmap? {
        if (drawable == null) {
            return null
        }

        if (drawable is BitmapDrawable) {
            return drawable.bitmap
        }

        return try {
            val bitmap: Bitmap = if (drawable is ColorDrawable) {
                Bitmap.createBitmap(COLORDRAWABLE_DIMENSION, COLORDRAWABLE_DIMENSION, BITMAP_CONFIG)
            } else {
                Bitmap.createBitmap(
                    drawable.intrinsicWidth,
                    drawable.intrinsicHeight,
                    BITMAP_CONFIG
                )
            }

            val canvas = Canvas(bitmap)
            drawable.setBounds(0, 0, canvas.width, canvas.height)
            drawable.draw(canvas)
            bitmap
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun initializeBitmap() {
        if (isDisableCircularTransformation) {
            bitmap = null
        } else {
            isLoading = false
            bitmap = getBitmapFromDrawable(drawable)
        }
        setup()
    }

    private fun setup() {
        if (!ready) {
            setupPending = true
            return
        }

        if (width == 0 && height == 0) {
            return
        }

        bitmap?.let {
            bitmapHeight = it.height
            bitmapWidth = it.width
            bitmapShader = BitmapShader(it, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
        }

        bitmapPaint.isAntiAlias = true
        bitmapPaint.shader = bitmapShader

        borderPaint.style = Paint.Style.STROKE
        borderPaint.isAntiAlias = true
        borderPaint.color = borderColor
        borderPaint.strokeWidth = borderWidth.toFloat()

        captionPaint.color = borderColor

        fillPaint.style = Paint.Style.FILL
        fillPaint.isAntiAlias = true
        fillPaint.color = fillColor

        borderRect.set(calculateBounds())
        borderRadius =
            ((borderRect.height() - borderWidth) / 2.0f).coerceAtMost((borderRect.width() - borderWidth) / 2.0f)

        drawableRect.set(borderRect)
        if (!borderOverlay && borderWidth > 0) {
            drawableRect.inset(borderWidth - 1.0f, borderWidth - 1.0f)
        }
        drawableRect.inset(inset - 1.0f, inset - 1.0f)
        drawableRadius =
            (drawableRect.height() / 2.0f).coerceAtMost(drawableRect.width() / 2.0f) + if (inset < 0) inset else 0

        if (isRipple) {
            rippleDrawable?.setBounds(
                borderRect.left.toInt(),
                borderRect.top.toInt(),
                borderRect.right.toInt(),
                borderRect.bottom.toInt()
            )
        }

        clippingPath = Path()
        clippingPath.addCircle(
            drawableRect.centerX(),
            drawableRect.centerY(),
            borderRadius,
            Path.Direction.CW
        )

        setupFreeUi()

        applyColorFilter()
        updateShaderMatrix()
        invalidate()
    }

    private fun setupFreeUi() {
        shapeSize = width / 2f

        val w = width

        freeShapePath.rewind()
        freeShapePath.moveTo(w.toFloat(), 0f)
        freeShapePath.lineTo(w.toFloat(), shapeSize)
        freeShapePath.lineTo(w - shapeSize, 0f)
        freeShapePath.close()

        val wi = shapeSize / 20f
        val len = wi * 3.5f
        freeShapePath2.rewind()
        freeShapePath2.moveTo(0f, 0f)
        freeShapePath2.lineTo(wi, 0f)
        freeShapePath2.lineTo(wi, len)
        freeShapePath2.lineTo(0f, len)
        freeShapePath2.close()

        freeShapePath2.moveTo(0f, len + wi)
        freeShapePath2.lineTo(wi, len + wi)
        freeShapePath2.lineTo(wi, len + wi + wi)
        freeShapePath2.lineTo(0f, len + wi + wi)
        freeShapePath2.close()
        freeShapePath2.offset(w.toFloat() - wi - shapeSize / 7f, shapeSize / 8f)
    }

    private fun calculateBounds(): RectF {
        val availableWidth = width - paddingLeft - paddingRight
        val availableHeight = height - paddingTop - paddingBottom

        val sideLength = availableWidth.coerceAtMost(availableHeight)

        val left = paddingLeft + (availableWidth - sideLength) / 2f
        val top = paddingTop + (availableHeight - sideLength) / 2f

        return RectF(left, top, left + sideLength, top + sideLength)
    }

    private fun updateShaderMatrix() {
        bitmap ?: return

        val scale: Float
        var dx = 0f
        var dy = 0f

        shaderMatrix.set(null)

        if (bitmapWidth * drawableRect.height() > drawableRect.width() * bitmapHeight) {
            scale = drawableRect.height() / bitmapHeight.toFloat()
            dx = (drawableRect.width() - bitmapWidth * scale) * 0.5f
        } else {
            scale = drawableRect.width() / bitmapWidth.toFloat()
            dy = (drawableRect.height() - bitmapHeight * scale) * 0.5f
        }

        shaderMatrix.setScale(scale, scale)
        shaderMatrix.postTranslate(
            (dx + 0.5f).toInt() + drawableRect.left,
            (dy + 0.5f).toInt() + drawableRect.top
        )

        bitmapShader?.setLocalMatrix(shaderMatrix)
    }

}