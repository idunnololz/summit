package com.idunnololz.summit.util.shimmer

import android.animation.ValueAnimator
import android.graphics.Color
import android.graphics.RectF
import androidx.annotation.ColorInt
import androidx.annotation.FloatRange
import androidx.annotation.IntDef
import androidx.annotation.Px
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

/**
 * A Shimmer is an object detailing all of the configuration options available for [ ]
 */
class Shimmer internal constructor() {
    /** The shape of the shimmer's highlight. By default LINEAR is used.  */
    @Retention(AnnotationRetention.SOURCE)
    @IntDef(Shape.LINEAR, Shape.RADIAL)
    annotation class Shape {
        companion object {
            /** Linear gives a ray reflection effect.  */
            const val LINEAR = 0

            /** Radial gives a spotlight effect.  */
            const val RADIAL = 1
        }
    }

    /** Direction of the shimmer's sweep.  */
    @Retention(AnnotationRetention.SOURCE)
    @IntDef(
        Direction.LEFT_TO_RIGHT,
        Direction.TOP_TO_BOTTOM,
        Direction.RIGHT_TO_LEFT,
        Direction.BOTTOM_TO_TOP,
    )
    annotation class Direction {
        companion object {
            const val LEFT_TO_RIGHT = 0
            const val TOP_TO_BOTTOM = 1
            const val RIGHT_TO_LEFT = 2
            const val BOTTOM_TO_TOP = 3
        }
    }

    val positions = FloatArray(COMPONENT_COUNT)
    val colors = IntArray(COMPONENT_COUNT)
    private val bounds: RectF = RectF()

    @Direction
    var direction = Direction.LEFT_TO_RIGHT

    @ColorInt
    var highlightColor = Color.WHITE

    @ColorInt
    var baseColor = 0x4cffffff

    @Shape
    var shape = Shape.LINEAR
    var fixedWidth = 0
    var fixedHeight = 0
    var widthRatio = 1f
    var heightRatio = 1f
    var intensity = 0f
    var dropoff = 0.5f
    var tilt = 20f
    var clipToChildren = true
    var autoStart = true
    var alphaShimmer = true
    var repeatCount: Int = ValueAnimator.INFINITE
    var repeatMode: Int = ValueAnimator.RESTART
    var animationDuration = 1000L
    var repeatDelay: Long = 0
    var startDelay: Long = 0

    fun width(width: Int): Int {
        return if (fixedWidth > 0) fixedWidth else Math.round(widthRatio * width)
    }

    fun height(height: Int): Int {
        return if (fixedHeight > 0) fixedHeight else Math.round(heightRatio * height)
    }

    fun updateColors() {
        when (shape) {
            Shape.LINEAR -> {
                colors[0] = baseColor
                colors[1] = highlightColor
                colors[2] = highlightColor
                colors[3] = baseColor
            }

            Shape.RADIAL -> {
                colors[0] = highlightColor
                colors[1] = highlightColor
                colors[2] = baseColor
                colors[3] = baseColor
            }

            else -> {
                colors[0] = baseColor
                colors[1] = highlightColor
                colors[2] = highlightColor
                colors[3] = baseColor
            }
        }
    }

    fun updatePositions() {
        when (shape) {
            Shape.LINEAR -> {
                positions[0] = max(((1f - intensity - dropoff) / 2f).toDouble(), 0.0)
                    .toFloat()
                positions[1] = max(((1f - intensity - 0.001f) / 2f).toDouble(), 0.0)
                    .toFloat()
                positions[2] = min(((1f + intensity + 0.001f) / 2f).toDouble(), 1.0)
                    .toFloat()
                positions[3] = min(((1f + intensity + dropoff) / 2f).toDouble(), 1.0)
                    .toFloat()
            }

            Shape.RADIAL -> {
                positions[0] = 0f
                positions[1] = min(intensity.toDouble(), 1.0).toFloat()
                positions[2] = min((intensity + dropoff).toDouble(), 1.0).toFloat()
                positions[3] = 1f
            }

            else -> {
                positions[0] = max(((1f - intensity - dropoff) / 2f).toDouble(), 0.0)
                    .toFloat()
                positions[1] = max(((1f - intensity - 0.001f) / 2f).toDouble(), 0.0)
                    .toFloat()
                positions[2] = min(((1f + intensity + 0.001f) / 2f).toDouble(), 1.0)
                    .toFloat()
                positions[3] = min(((1f + intensity + dropoff) / 2f).toDouble(), 1.0)
                    .toFloat()
            }
        }
    }

    fun updateBounds(viewWidth: Int, viewHeight: Int) {
        val magnitude = max(viewWidth.toDouble(), viewHeight.toDouble()).toInt()
        val rad = Math.PI / 2f - Math.toRadians((tilt % 90f).toDouble())
        val hyp = magnitude / sin(rad)
        val padding = 3 * Math.round((hyp - magnitude).toFloat() / 2f)
        bounds.set(
            -padding.toFloat(),
            -padding.toFloat(),
            (width(viewWidth) + padding).toFloat(),
            (height(viewHeight) + padding).toFloat(),
        )
    }

    abstract class Builder<T : Builder<T>?> {
        val shimmer = Shimmer()
        protected abstract val thisBuilder: T

        /** Copies the configuration of an already built Shimmer to this builder  */
        fun copyFrom(other: Shimmer): T {
            setDirection(other.direction)
            setShape(other.shape)
            setFixedWidth(other.fixedWidth)
            setFixedHeight(other.fixedHeight)
            setWidthRatio(other.widthRatio)
            setHeightRatio(other.heightRatio)
            setIntensity(other.intensity)
            setDropoff(other.dropoff)
            setTilt(other.tilt)
            setClipToChildren(other.clipToChildren)
            setAutoStart(other.autoStart)
            setRepeatCount(other.repeatCount)
            setRepeatMode(other.repeatMode)
            setRepeatDelay(other.repeatDelay)
            setStartDelay(other.startDelay)
            setDuration(other.animationDuration)
            shimmer.baseColor = other.baseColor
            shimmer.highlightColor = other.highlightColor
            return thisBuilder
        }

        /** Sets the direction of the shimmer's sweep. See [Direction].  */
        fun setDirection(@Direction direction: Int): T {
            shimmer.direction = direction
            return thisBuilder
        }

        /** Sets the shape of the shimmer. See [Shape].  */
        fun setShape(@Shape shape: Int): T {
            shimmer.shape = shape
            return thisBuilder
        }

        /** Sets the fixed width of the shimmer, in pixels.  */
        fun setFixedWidth(@Px fixedWidth: Int): T {
            require(!(fixedWidth < 0)) { "Given invalid width: $fixedWidth" }
            shimmer.fixedWidth = fixedWidth
            return thisBuilder
        }

        /** Sets the fixed height of the shimmer, in pixels.  */
        fun setFixedHeight(@Px fixedHeight: Int): T {
            if (fixedHeight < 0) {
                throw IllegalArgumentException("Given invalid height: $fixedHeight")
            }
            shimmer.fixedHeight = fixedHeight
            return thisBuilder
        }

        /** Sets the width ratio of the shimmer, multiplied against the total width of the layout.  */
        fun setWidthRatio(widthRatio: Float): T {
            if (widthRatio < 0f) {
                throw IllegalArgumentException("Given invalid width ratio: $widthRatio")
            }
            shimmer.widthRatio = widthRatio
            return thisBuilder
        }

        /** Sets the height ratio of the shimmer, multiplied against the total height of the layout.  */
        fun setHeightRatio(heightRatio: Float): T {
            if (heightRatio < 0f) {
                throw IllegalArgumentException("Given invalid height ratio: $heightRatio")
            }
            shimmer.heightRatio = heightRatio
            return thisBuilder
        }

        /** Sets the intensity of the shimmer. A larger value causes the shimmer to be larger.  */
        fun setIntensity(intensity: Float): T {
            if (intensity < 0f) {
                throw IllegalArgumentException("Given invalid intensity value: $intensity")
            }
            shimmer.intensity = intensity
            return thisBuilder
        }

        /**
         * Sets how quickly the shimmer's gradient drops-off. A larger value causes a sharper drop-off.
         */
        fun setDropoff(dropoff: Float): T {
            if (dropoff < 0f) {
                throw IllegalArgumentException("Given invalid dropoff value: $dropoff")
            }
            shimmer.dropoff = dropoff
            return thisBuilder
        }

        /** Sets the tilt angle of the shimmer in degrees.  */
        fun setTilt(tilt: Float): T {
            shimmer.tilt = tilt
            return thisBuilder
        }

        /**
         * Sets the base alpha, which is the alpha of the underlying children, amount in the range [0,
         * 1].
         */
        fun setBaseAlpha(@FloatRange(from = 0.0, to = 1.0) alpha: Float): T {
            val intAlpha = (clamp(0f, 1f, alpha) * 255f).toInt()
            shimmer.baseColor = intAlpha shl 24 or (shimmer.baseColor and 0x00FFFFFF)
            return thisBuilder
        }

        /** Sets the shimmer alpha amount in the range [0, 1].  */
        fun setHighlightAlpha(@FloatRange(from = 0.0, to = 1.0) alpha: Float): T {
            val intAlpha = (clamp(0f, 1f, alpha) * 255f).toInt()
            shimmer.highlightColor = intAlpha shl 24 or (shimmer.highlightColor and 0x00FFFFFF)
            return thisBuilder
        }

        /**
         * Sets whether the shimmer will clip to the childrens' contents, or if it will opaquely draw on
         * top of the children.
         */
        fun setClipToChildren(status: Boolean): T {
            shimmer.clipToChildren = status
            return thisBuilder
        }

        /** Sets whether the shimmering animation will start automatically.  */
        fun setAutoStart(status: Boolean): T {
            shimmer.autoStart = status
            return thisBuilder
        }

        /**
         * Sets how often the shimmering animation will repeat. See [ ][ValueAnimator.setRepeatCount].
         */
        fun setRepeatCount(repeatCount: Int): T {
            shimmer.repeatCount = repeatCount
            return thisBuilder
        }

        /**
         * Sets how the shimmering animation will repeat. See [ ][ValueAnimator.setRepeatMode].
         */
        fun setRepeatMode(mode: Int): T {
            shimmer.repeatMode = mode
            return thisBuilder
        }

        /** Sets how long to wait in between repeats of the shimmering animation.  */
        fun setRepeatDelay(millis: Long): T {
            if (millis < 0) {
                throw IllegalArgumentException("Given a negative repeat delay: $millis")
            }
            shimmer.repeatDelay = millis
            return thisBuilder
        }

        /** Sets how long to wait for starting the shimmering animation.  */
        fun setStartDelay(millis: Long): T {
            if (millis < 0) {
                throw IllegalArgumentException("Given a negative start delay: $millis")
            }
            shimmer.startDelay = millis
            return thisBuilder
        }

        /** Sets how long the shimmering animation takes to do one full sweep.  */
        fun setDuration(millis: Long): T {
            if (millis < 0) {
                throw IllegalArgumentException("Given a negative duration: $millis")
            }
            shimmer.animationDuration = millis
            return thisBuilder
        }

        fun build(): Shimmer {
            shimmer.updateColors()
            shimmer.updatePositions()
            return shimmer
        }

        companion object {
            private fun clamp(min: Float, max: Float, value: Float): Float {
                return min(max.toDouble(), max(min.toDouble(), value.toDouble()))
                    .toFloat()
            }
        }
    }

    class AlphaHighlightBuilder : Builder<AlphaHighlightBuilder>() {
        init {
            shimmer.alphaShimmer = true
        }

        override val thisBuilder: AlphaHighlightBuilder
            get() = this
    }

    class ColorHighlightBuilder : Builder<ColorHighlightBuilder>() {
        init {
            shimmer.alphaShimmer = false
        }

        /** Sets the highlight color for the shimmer.  */
        fun setHighlightColor(@ColorInt color: Int): ColorHighlightBuilder {
            shimmer.highlightColor = color
            return thisBuilder
        }

        /** Sets the base color for the shimmer.  */
        fun setBaseColor(@ColorInt color: Int): ColorHighlightBuilder {
            shimmer.baseColor = color
            return thisBuilder
        }

        override val thisBuilder: ColorHighlightBuilder
            get() = this
    }

    companion object {
        private const val COMPONENT_COUNT = 4
    }
}
