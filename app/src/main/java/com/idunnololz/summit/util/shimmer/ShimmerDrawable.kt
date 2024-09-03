package com.idunnololz.summit.util.shimmer

import android.animation.ValueAnimator
import android.animation.ValueAnimator.AnimatorUpdateListener
import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.RadialGradient
import android.graphics.Rect
import android.graphics.Shader
import android.graphics.drawable.Drawable
import android.view.animation.LinearInterpolator
import kotlin.math.max
import kotlin.math.sqrt
import kotlin.math.tan

class ShimmerDrawable : Drawable() {
    private val updateListener = AnimatorUpdateListener { invalidateSelf() }
    private val shimmerPaint = Paint().apply {
        isAntiAlias = true
    }
    private val drawRect = Rect()
    private val shaderMatrix = Matrix()
    private var valueAnimator: ValueAnimator? = null

    var shimmer: Shimmer? = null
        set(value) {
            field = value

            updateShader()
            updateValueAnimator()
            invalidateSelf()
        }

    public override fun onBoundsChange(bounds: Rect) {
        super.onBoundsChange(bounds)
        drawRect.set(bounds)
        updateShader()
        maybeStartShimmer()
    }

    override fun draw(canvas: Canvas) {
        val shimmer = shimmer
            ?: return
        val valueAnimator = valueAnimator

        if (shimmerPaint.shader == null) {
            return
        }

        val tiltTan = tan(Math.toRadians(shimmer.tilt.toDouble()))
            .toFloat()
        val translateHeight = drawRect.height() + tiltTan * drawRect.width()
        val translateWidth = drawRect.width() + tiltTan * drawRect.height()
        val dx: Float
        val dy: Float
        val animatedValue: Float = if (valueAnimator != null) {
            valueAnimator.animatedValue as Float
        } else {
            0f
        }

        when (shimmer.direction) {
            Shimmer.Direction.LEFT_TO_RIGHT -> {
                dx = offset(-translateWidth, translateWidth, animatedValue)
                dy = 0f
            }

            Shimmer.Direction.RIGHT_TO_LEFT -> {
                dx = offset(translateWidth, -translateWidth, animatedValue)
                dy = 0f
            }

            Shimmer.Direction.TOP_TO_BOTTOM -> {
                dx = 0f
                dy = offset(-translateHeight, translateHeight, animatedValue)
            }

            Shimmer.Direction.BOTTOM_TO_TOP -> {
                dx = 0f
                dy = offset(translateHeight, -translateHeight, animatedValue)
            }

            else -> {
                dx = offset(-translateWidth, translateWidth, animatedValue)
                dy = 0f
            }
        }

        shaderMatrix.reset()
        shaderMatrix.setRotate(shimmer.tilt, drawRect.width() / 2f, drawRect.height() / 2f)
        shaderMatrix.preTranslate(dx, dy)
        shimmerPaint.shader.setLocalMatrix(shaderMatrix)

        canvas.drawRect(drawRect, shimmerPaint)
    }

    override fun setAlpha(alpha: Int) {
        // No-op, modify the Shimmer object you pass in instead
    }

    override fun setColorFilter(colorFilter: ColorFilter?) {
        // No-op, modify the Shimmer object you pass in instead
    }

    @Deprecated("Deprecated in Java", ReplaceWith("No longer used since APi 29."))
    override fun getOpacity(): Int {
        val shimmer = shimmer

        return if (shimmer != null && (shimmer.clipToChildren || shimmer.alphaShimmer)) {
            PixelFormat.TRANSLUCENT
        } else {
            PixelFormat.OPAQUE
        }
    }

    override fun getIntrinsicHeight(): Int {
        return shimmer?.fixedHeight ?: -1
    }

    override fun getIntrinsicWidth(): Int {
        return shimmer?.fixedWidth ?: -1
    }

    fun setSize(width: Int, height: Int) {
        shimmer?.fixedWidth = width
        shimmer?.fixedHeight = height
    }

    private fun offset(start: Float, end: Float, percent: Float): Float {
        return start + (end - start) * percent
    }

    private fun updateValueAnimator() {
        val shimmer = shimmer ?: return
        val wasAnimatorStarted = valueAnimator?.isStarted == true

        valueAnimator?.let {
            it.cancel()
            it.removeAllUpdateListeners()
        }

        valueAnimator = ValueAnimator.ofFloat(
            0f,
            1f + (shimmer.repeatDelay / shimmer.animationDuration).toFloat(),
        ).apply {
            interpolator = LinearInterpolator()
            repeatMode = shimmer.repeatMode
            startDelay = shimmer.startDelay
            repeatCount = shimmer.repeatCount
            duration = shimmer.animationDuration + shimmer.repeatDelay
            addUpdateListener(updateListener)

            if (wasAnimatorStarted) {
                start()
            }
        }
    }

    private fun maybeStartShimmer() {
        val valueAnimator = valueAnimator
        val shimmer = shimmer

        if (valueAnimator != null &&
            !valueAnimator.isStarted &&
            shimmer != null &&
            shimmer.autoStart &&
            callback != null
        ) {
            valueAnimator.start()
        }
    }

    private fun updateShader() {
        val bounds = bounds
        val boundsWidth = bounds.width()
        val boundsHeight = bounds.height()
        val shimmer = shimmer
            ?: return

        if (boundsWidth == 0 || boundsHeight == 0) {
            return
        }

        val width = shimmer.width(boundsWidth)
        val height = shimmer.height(boundsHeight)

        val shader: Shader = when (shimmer.shape) {
            Shimmer.Shape.LINEAR -> {
                val vertical = (
                    shimmer.direction == Shimmer.Direction.TOP_TO_BOTTOM ||
                        shimmer.direction == Shimmer.Direction.BOTTOM_TO_TOP
                    )
                val endX = if (vertical) 0 else width
                val endY = if (vertical) height else 0
                LinearGradient(
                    0f,
                    0f,
                    endX.toFloat(),
                    endY.toFloat(),
                    shimmer.colors,
                    shimmer.positions,
                    Shader.TileMode.CLAMP,
                )
            }
            Shimmer.Shape.RADIAL ->
                RadialGradient(
                    width / 2f,
                    height / 2f,
                    (
                        max(
                            width.toDouble(),
                            height.toDouble(),
                        ) / sqrt(2.0)
                        ).toFloat(),
                    shimmer.colors,
                    shimmer.positions,
                    Shader.TileMode.CLAMP,
                )

            else -> {
                val vertical = (
                    shimmer.direction == Shimmer.Direction.TOP_TO_BOTTOM ||
                        shimmer.direction == Shimmer.Direction.BOTTOM_TO_TOP
                    )
                val endX = if (vertical) 0 else width
                val endY = if (vertical) height else 0

                LinearGradient(
                    0f,
                    0f,
                    endX.toFloat(),
                    endY.toFloat(),
                    shimmer.colors,
                    shimmer.positions,
                    Shader.TileMode.CLAMP,
                )
            }
        }
        shimmerPaint.setShader(shader)
    }
}
