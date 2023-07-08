package com.idunnololz.summit.view

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Matrix
import android.graphics.drawable.Drawable
import androidx.core.view.GestureDetectorCompat
import androidx.core.view.ScaleGestureDetectorCompat
import android.util.AttributeSet
import android.util.Log
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import androidx.appcompat.widget.AppCompatImageView
import kotlin.math.max
import kotlin.math.min

class GalleryImageView : AppCompatImageView {

    companion object {

        private const val TAG = "GalleryImageView"

        private const val ANIMATION_DURATION = 250L
    }

    interface Callback {
        fun togggleUi()
        fun showUi()
        fun hideUi()
        fun overScroll(offX: Float, offY: Float)

        /**
         * Called when the user lets go
         * @return true if overscroll should be kept
         */
        fun overScrollEnd(): Boolean
    }

    private var detector: GestureDetectorCompat
    private var scaleDetector: ScaleGestureDetector
    private val _matrix: Matrix = Matrix()
    private val _matrixArr = FloatArray(9)

    /**
     * Minimum zoom possible. This is variable and depends on the size of the image.
     */
    private var minZoom = 0f
    private var maxZoom = 3f
    private val zoomIncrement = 1f

    private var curZoom = 1f

    /**
     * Absolute X offset. This is not affected by the scale/zoom. Multiply with curZoom to get scaled offset.
     */
    private var offX = 0f
    private var offY = 0f

    private var overScrollX = 0f
    private var overScrollY = 0f

    private var imageW = 0f
    private var imageH = 0f

    private var zoomGestureOngoing = false

    var callback: Callback? = null

    private var drawableDirty = true

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    )

    init {
        scaleDetector =
            ScaleGestureDetector(context, object : ScaleGestureDetector.OnScaleGestureListener {

                override fun onScaleBegin(detector: ScaleGestureDetector): Boolean {
                    zoomGestureOngoing = true
                    return true
                }

                override fun onScaleEnd(detector: ScaleGestureDetector) {
                    zoomGestureOngoing = false
                }

                override fun onScale(detector: ScaleGestureDetector): Boolean {
                    detector.let {
                        zoomInToAbs(
                            detector.focusX / curZoom - offX,
                            detector.focusY / curZoom - offY,
                            curZoom * detector.scaleFactor
                        )
                    }
                    return true
                }
            })
        ScaleGestureDetectorCompat.setQuickScaleEnabled(scaleDetector, false)

        detector = GestureDetectorCompat(context, object : GestureDetector.OnGestureListener {
            override fun onShowPress(e: MotionEvent) {
            }

            override fun onSingleTapUp(e: MotionEvent): Boolean {
                return false
            }

            override fun onDown(e: MotionEvent): Boolean {
                return false
            }

            override fun onFling(
                e1: MotionEvent,
                e2: MotionEvent,
                velocityX: Float,
                velocityY: Float
            ): Boolean {
                return false
            }

            override fun onScroll(
                e1: MotionEvent,
                e2: MotionEvent,
                distanceX: Float,
                distanceY: Float
            ): Boolean {
                if (scrollByAndCommit(distanceX, distanceY)) {
                    return true
                } else if (curZoom > minZoom - 0.01 && curZoom <= minZoom + 0.01) {
                    updateOverScrollBy(distanceX, distanceY)
                    return false
                } else {
                    return false
                }
            }

            override fun onLongPress(e: MotionEvent) {
            }
        })

        detector.setOnDoubleTapListener(object : GestureDetector.OnDoubleTapListener {
            override fun onDoubleTap(e: MotionEvent): Boolean {
                e.let {
                    zoomInToAnimated(it.x, it.y)
                }
                return true
            }

            override fun onDoubleTapEvent(e: MotionEvent): Boolean {
                return true
            }

            override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
                callback?.togggleUi()
                return true
            }

        })

        scaleType = ScaleType.MATRIX
    }

    private fun updateOverScrollBy(offX: Float, offY: Float) {
        overScrollX -= offX / curZoom
        overScrollY -= offY / curZoom
        callback?.overScroll(overScrollX, overScrollY)
        updateMatrix()
    }

    private fun updateOverScroll(newOverScrollX: Float, newOverScrollY: Float) {
        overScrollX = newOverScrollX
        overScrollY = newOverScrollY
        callback?.overScroll(overScrollX, overScrollY)
        updateMatrix()
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        // val g1 = !zoomGestureOngoing && detector.onTouchEvent(event)
        val g1 = detector.onTouchEvent(event)
        val g2 = scaleDetector.onTouchEvent(event)
        val gestureDetected = g1 || g2

        if (event.actionMasked == MotionEvent.ACTION_UP || event.actionMasked == MotionEvent.ACTION_CANCEL) {
            val keepOverscroll = callback?.overScrollEnd()
            if (keepOverscroll != true) {
                ValueAnimator.ofFloat(1f, 0f).apply {
                    duration = ANIMATION_DURATION
                    addUpdateListener {
                        val newX = overScrollX - overScrollX * it.animatedFraction
                        val newY = overScrollY - overScrollY * it.animatedFraction

                        updateOverScroll(newX, newY)
                    }
                }.also {
                    it.start()
                }
            }
        }

        if (gestureDetected) {
            return true
        }

        return super.onTouchEvent(event)
    }

    override fun setScaleType(scaleType: ScaleType?) {
//        if (scaleType != ScaleType.MATRIX) {
//            throw UnsupportedOperationException("GalleryImageView only supports Matrix scale type.")
//        }
        super.setScaleType(scaleType)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)

        drawable ?: return

        calculateImageSize()
        scrollByAbsolute(0f, 0f)
        centerIfNeeded()
        updateMatrix()
    }

    private fun calculateImageSize() {
        if (!drawableDirty) return
        val d = drawable ?: return

        // Get image matrix values and place them in an array
        imageMatrix.getValues(_matrixArr)

        val imageRatio = d.intrinsicWidth.toFloat() / d.intrinsicHeight.toFloat()
        val viewRatio = measuredWidth.toFloat() / measuredHeight.toFloat()

        minZoom = if (imageRatio > viewRatio) {
            // constraint by image width
            measuredWidth.toFloat() / d.intrinsicWidth
        } else {
            // constraint by image height
            measuredHeight.toFloat() / d.intrinsicHeight
        }

        curZoom = minZoom
        maxZoom = minZoom + 2f

        imageW = d.intrinsicWidth.toFloat()
        imageH = d.intrinsicHeight.toFloat()

        drawableDirty = false
    }

    override fun setImageDrawable(drawable: Drawable?) {
        drawableDirty = true
        super.setImageDrawable(drawable)
    }

    fun zoomInToAnimated(x: Float, y: Float) {
        var lastValue = 0f
        val absX = x / curZoom - offX
        val absY = y / curZoom - offY
        val zoomDelta = if (curZoom < maxZoom - 0.1f) zoomIncrement else minZoom - curZoom
        ValueAnimator.ofFloat(0f, 1f).apply {
            duration = ANIMATION_DURATION
            addUpdateListener {
                zoomInTo(absX, absY, (it.animatedFraction - lastValue) * zoomDelta)

                lastValue = it.animatedFraction
            }
        }.also {
            it.start()
        }
    }

    private fun zoomInTo(absX: Float, absY: Float, zoomAmount: Float) {
        zoomInToAbs(absX, absY, curZoom + zoomAmount)
    }

    fun zoomInToAbs(absX: Float, absY: Float, zoomAmount: Float) {
        Log.d(TAG, "absX $absX absY $absY w $width h $height")
        val prevZoom = curZoom
        curZoom = max(min(zoomAmount, maxZoom), minZoom)

        if (curZoom == prevZoom) {
            return
        }

        val diffW = imageW * curZoom - imageW * prevZoom
        val diffH = imageH * curZoom - imageH * prevZoom

        // 1. Convert offX/Y from absolute to scaled
        // 2. Convert absX/Y we are trying to zoom into into a normalized ratio [0,1]
        // 3. Multiply by change in W/H. This gives us how much we should pivot left or right.
        // 4. Sum the scaled offX/Y with the pivot amount.
        // 5. Finally scaled offset to absolute offset.
        scrollToAbsolute(
            -(-offX * prevZoom + absX / imageW * diffW) / curZoom,
            -(-offY * prevZoom + absY / imageH * diffH) / curZoom
        )

        Log.d(TAG, "zoom: $curZoom")

        updateMatrix()
    }

    fun scrollByAndCommit(deltaX: Float, deltaY: Float): Boolean {
        val result = scrollByAbsolute(
            deltaX / curZoom,
            deltaY / curZoom
        )
        if (result) {
            updateMatrix()
        }
        return result
    }

    /**
     * @return true if scroll was valid. False if scroll was impossible
     */
    private fun scrollByAbsolute(deltaX: Float, deltaY: Float): Boolean {
        return scrollToAbsolute(offX - deltaX, offY - deltaY)
    }

    /**
     * @return true if scroll was valid. False if scroll was impossible
     */
    private fun scrollToAbsolute(newX: Float, newY: Float): Boolean {
        val oldX = offX
        val oldY = offY

        offX = newX
        offY = newY

        offX = max(min(offX, 0f), width / curZoom - imageW)
        offY = max(min(offY, 0f), height / curZoom - imageH)

        centerIfNeeded()

        Log.d(TAG, "scrollToAbsolute: newX $newX newY $newY")

        return offX != oldX || offY != oldY
    }

    private fun centerIfNeeded() {
        val scaledW = imageW * curZoom
        val scaledH = imageH * curZoom
        val width = measuredWidth
        val height = measuredHeight

        if (scaledH < height) {
            offY = (height - scaledH) / 2f / curZoom
        }
        if (scaledW < width) {
            offX = (width - scaledW) / 2f / curZoom
        }
    }

    private fun updateMatrix() {
        _matrix.reset()
        _matrix.postTranslate(offX + overScrollX, offY + overScrollY)
        _matrix.postScale(curZoom, curZoom)
        imageMatrix = _matrix
    }
}