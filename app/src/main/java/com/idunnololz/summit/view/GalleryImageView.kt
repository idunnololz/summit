package com.idunnololz.summit.view

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Matrix
import android.graphics.PointF
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.util.Log
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.animation.DecelerateInterpolator
import androidx.appcompat.widget.AppCompatImageView
import androidx.core.view.GestureDetectorCompat
import androidx.core.view.ScaleGestureDetectorCompat
import com.google.common.math.Quantiles.scale
import com.idunnololz.summit.util.Utils
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min


class GalleryImageView : AppCompatImageView {

    companion object {

        private const val TAG = "GalleryImageView"

        private const val ANIMATION_DURATION = 250L
        private const val MAX_ZOOM_MULTIPLIER = 4f
    }

    interface Callback {
        fun togggleUi()
        fun showUi()
        fun hideUi()
        fun overScroll(offX: Float, offY: Float, curZoom: Float)

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
    private var maxZoom = 3f // see RELATIVE_MAX_ZOOM
    private val zoomIncrement = 1f

    private var curZoom = 1f

    private var startZoom = 0f
    private var isQuickScaling = false
    private var isZooming = false
    private var quickScaleLastDistance: Float = 0f
    private val quickScaleCenter = PointF(0f, 0f)
    private var quickScaleMoved = false
    private val quickScaleVLastPoint = PointF(0f, 0f)
//    quickScaleSCenter = viewToSourceCoord(vCenterStart)
//    quickScaleVStart = PointF(e.x, e.y)
//    quickScaleVLastPoint = PointF(quickScaleSCenter.x, quickScaleSCenter.y)

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

    private var outputSrcCoords = PointF(0f, 0f)

    private var quickScaleThreshold = Utils.convertDpToPixel(20f)

    private var flingAnimation: ValueAnimator? = null

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr,
    )

    init {
        scaleDetector =
            ScaleGestureDetector(
                context,
                object : ScaleGestureDetector.OnScaleGestureListener {

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
                                curZoom * detector.scaleFactor,
                            )
                        }
                        return true
                    }
                },
            )
        ScaleGestureDetectorCompat.setQuickScaleEnabled(scaleDetector, false)

        detector = GestureDetectorCompat(
            context,
            object : GestureDetector.OnGestureListener {
                override fun onShowPress(e: MotionEvent) {
                }

                override fun onSingleTapUp(e: MotionEvent): Boolean {
                    return false
                }

                override fun onDown(e: MotionEvent): Boolean {
                    return false
                }

                override fun onFling(
                    e1: MotionEvent?,
                    e2: MotionEvent,
                    velocityX: Float,
                    velocityY: Float,
                ): Boolean {
                    if (e1 != null && (abs(
                            e1.x - e2.x,
                        ) > 50 || abs(e1.y - e2.y) > 50) && (abs(velocityX) > 500 || Math.abs(
                            velocityY,
                        ) > 500) && !isZooming
                    ) {
                        val scrollSpeedX: Float = -velocityX * 0.2f
                        val scrollSpeedY: Float = -velocityY * 0.2f

                        var lastValue = 0f

                        flingAnimation?.cancel()
                        flingAnimation = ValueAnimator.ofFloat(0f, 1f).apply {
                            duration = 1000
                            interpolator = DecelerateInterpolator(2f)
                            addUpdateListener {
                                val delta = (it.animatedFraction - lastValue)
                                scrollByAndCommit(
                                    scrollSpeedX * delta,
                                    scrollSpeedY * delta,
                                )

                                lastValue = it.animatedFraction
                            }
                        }.also {
                            it.start()
                        }
                        return true
                    }
                    return false
                }

                override fun onScroll(
                    e1: MotionEvent?,
                    e2: MotionEvent,
                    distanceX: Float,
                    distanceY: Float,
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
            },
        )

        detector.setOnDoubleTapListener(
            object : GestureDetector.OnDoubleTapListener {
                override fun onDoubleTap(e: MotionEvent): Boolean {
                    e.let {
                        // Store quick scale params. This will become either a double tap zoom or a
                        // quick scale depending on whether the user swipes.

                        // Store quick scale params. This will become either a double tap zoom or a
                        // quick scale depending on whether the user swipes.
//                        vCenterStart = PointF(e.x, e.y)
//                        vTranslateStart = PointF(vTranslate.x, vTranslate.y)
                        startZoom = curZoom
                        isQuickScaling = true
                        isZooming = true
                        quickScaleLastDistance = -1f
                        quickScaleCenter.set(it.x, it.y)
//                        quickScaleSCenter = viewToSourceCoord(vCenterStart)
//                        quickScaleVStart = PointF(e.x, e.y)
                        quickScaleVLastPoint.set(quickScaleCenter.x, quickScaleCenter.y)
                        quickScaleMoved = false
                        // We need to get events in onTouchEvent after this.
//                        zoomInToAnimated(it.x, it.y)
                    }
                    // We need to get events in onTouchEvent after this.
                    return false
                }

                override fun onDoubleTapEvent(e: MotionEvent): Boolean {
                    return true
                }

                override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
                    callback?.togggleUi()
                    return true
                }
            },
        )

        scaleType = ScaleType.MATRIX
    }

    private fun updateOverScrollBy(offX: Float, offY: Float) {
        overScrollX -= offX / curZoom
        overScrollY -= offY / curZoom
        callback?.overScroll(overScrollX, overScrollY, curZoom)
        updateMatrix()
    }

    private fun updateOverScroll(newOverScrollX: Float, newOverScrollY: Float) {
        overScrollX = newOverScrollX
        overScrollY = newOverScrollY
        callback?.overScroll(overScrollX, overScrollY, curZoom)
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

            if (isQuickScaling) {
                isQuickScaling = false
                if (!quickScaleMoved) {
                    zoomInToAnimated(event.x, event.y)
                }
            }
            isZooming = false
        } else if (event.actionMasked == MotionEvent.ACTION_MOVE) {
            if (isQuickScaling) {
                // One finger zoom
                // Stole Google's Magical Formula™ to make sure it feels the exact same
                var dist: Float = Math.abs(quickScaleCenter.y - event.y) * 2 + quickScaleThreshold
                if (quickScaleLastDistance == -1f) {
                    quickScaleLastDistance = dist
                }
                val isUpwards: Boolean = event.y > quickScaleVLastPoint.y
                quickScaleVLastPoint.set(0f, event.y)
                val spanDiff = abs(1 - dist / quickScaleLastDistance) * 0.5f
                if (spanDiff > 0.03f || quickScaleMoved) {
                    quickScaleMoved = true
                    var multiplier = 1f
                    if (quickScaleLastDistance > 0) {
                        multiplier = if (isUpwards) 1 + spanDiff else 1 - spanDiff
                    }
                    val newZoom = minZoom.coerceAtLeast(maxZoom.coerceAtMost(curZoom * multiplier))

                    convertViewToSourceCoord(quickScaleCenter.x, quickScaleCenter.y)
                    zoomInToAbs(outputSrcCoords.x, outputSrcCoords.y, newZoom)
//                    if (panEnabled) {
//                        val vLeftStart: Float = vCenterStart.x - vTranslateStart.x
//                        val vTopStart: Float = vCenterStart.y - vTranslateStart.y
//                        val vLeftNow: Float = vLeftStart * (scale / scaleStart)
//                        val vTopNow: Float = vTopStart * (scale / scaleStart)
//                        vTranslate.x = vCenterStart.x - vLeftNow
//                        vTranslate.y = vCenterStart.y - vTopNow
//                        if (previousScale * sHeight() < height && scale * sHeight() >= height || previousScale * sWidth() < width && scale * sWidth() >= width) {
//                            fitToBounds(true)
//                            vCenterStart.set(sourceToViewCoord(quickScaleSCenter))
//                            vTranslateStart.set(vTranslate)
//                            scaleStart = scale
//                            dist = 0f
//                        }
//                    } else if (sRequestedCenter != null) {
//                        // With a center specified from code, zoom around that point.
//                        vTranslate.x = width / 2 - scale * sRequestedCenter.x
//                        vTranslate.y = height / 2 - scale * sRequestedCenter.y
//                    } else {
//                        // With no requested center, scale around the image center.
//                        vTranslate.x = width / 2 - scale * (sWidth() / 2)
//                        vTranslate.y = height / 2 - scale * (sHeight() / 2)
//                    }
                }
                quickScaleLastDistance = dist
//                fitToBounds(true)
//                refreshRequiredTiles(eagerLoadingEnabled)
//                consumed = true
            }
        } else if(event.actionMasked == MotionEvent.ACTION_DOWN) {
            flingAnimation?.cancel()
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
        maxZoom = minZoom * MAX_ZOOM_MULTIPLIER

        imageW = d.intrinsicWidth.toFloat()
        imageH = d.intrinsicHeight.toFloat()

        drawableDirty = false
    }

    override fun setImageDrawable(drawable: Drawable?) {
        drawableDirty = true
        super.setImageDrawable(drawable)
    }

    private fun convertViewToSourceCoord(x: Float, y: Float) {
        val absX = x / curZoom - offX
        val absY = y / curZoom - offY

        outputSrcCoords.set(absX, absY)
    }

    private fun zoomInToAnimated(x: Float, y: Float) {
        var lastValue = 0f
        val absX = x / curZoom - offX
        val absY = y / curZoom - offY
        val zoomDelta = if (curZoom < maxZoom - 0.1f)
            curZoom
        else
            minZoom - curZoom
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
        Log.d(TAG, "absX $absX absY $absY w $width h $height curZoom $curZoom")
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
            -(-offY * prevZoom + absY / imageH * diffH) / curZoom,
        )

        Log.d(TAG, "zoom: $curZoom")

        updateMatrix()
    }

    fun scrollByAndCommit(deltaX: Float, deltaY: Float): Boolean {
        val result = scrollByAbsolute(
            deltaX / curZoom,
            deltaY / curZoom,
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
