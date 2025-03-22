package io.noties.markwon.image

import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.PixelFormat
import android.graphics.Rect
import android.graphics.drawable.Animatable
import android.graphics.drawable.Drawable
import androidx.annotation.IntRange
import androidx.annotation.MainThread

interface OnDimensionsKnownListener {
    fun onDimensionsKnown(drawable: AsyncDrawable)
}

class AsyncDrawable(
    val destination: String,
    private val loader: AsyncDrawableLoader,
    /**
     * @since 4.0.0
     */
    @get:Suppress("unused") val imageSizeResolver: ImageSizeResolver,
    /**
     * @since 4.0.0
     */
    @JvmField val imageSize: ImageSize?,
    val imageText: String?
) : Drawable() {
    // @since 4.5.0

    private val placeholder = loader.placeholder(this)
    private var callback: Callback? = null

    private var onDimensionsKnownListeners = mutableListOf<OnDimensionsKnownListener>()

    var result: Drawable? = null
        set(value) {
            // @since 4.5.0 revert this flag when we have new source

            wasPlayingBefore = false

            // if we have previous one, detach it
            field?.callback = null

            field = value

            initBounds()
        }

    /**
     * @see .hasKnownDimensions
     * @since 4.0.0
     */
    var lastKnownCanvasWidth: Int = 0
        private set

    /**
     * @see .hasKnownDimensions
     * @since 4.0.0
     */
    var lastKnowTextSize: Float = 0f
        private set

    // @since 2.0.1 for use-cases when image is loaded faster than span is drawn and knows canvas width
    private var waitingForDimensions = false

    // @since 4.5.0 in case if result is Animatable and this drawable was detached, we
    //  keep the state to resume when we are going to be attached again (when used in RecyclerView)
    private var wasPlayingBefore = false

    /**
     * @since 1.0.1
     */
    init {
        val placeholder = this.placeholder
        if (placeholder != null) {
            setPlaceholderResult(placeholder)
        }
    }

    /**
     * @since 4.2.1
     */
    @Suppress("unused")
    fun hasKnownDimensions(): Boolean {
        return lastKnownCanvasWidth > 0
    }

    fun hasResult(): Boolean {
        return result != null
    }

    val isAttached: Boolean
        get() = getCallback() != null

    fun setCallback2(cb: Callback?) {
        // @since 4.2.1
        //  wrap callback so invalidation happens to this AsyncDrawable instance
        //  and not for wrapped result/placeholder

        this.callback = if (cb == null)
            null
        else
            WrappedCallback(cb)

        super.setCallback(cb)

        // if not null -> means we are attached
        if (callback != null) {
            // as we have a placeholder now, it's important to check it our placeholder
            // has a proper callback at this point. This is not required in most cases,
            // as placeholder should be static, but if it's not -> it can operate as usual

            if (result != null
                && result!!.callback == null
            ) {
                result!!.callback = callback
            }

            // @since 4.5.0 we trigger loading only if we have no result (and result is not placeholder)
            val shouldLoad = result == null || result === placeholder

            if (result != null) {
                result!!.callback = callback

                // @since 4.5.0
                if (result is Animatable && wasPlayingBefore) {
                    (result as Animatable).start()
                }
            }

            if (shouldLoad) {
                loader.load(this)
            }
        } else {
            if (result != null) {
                result!!.callback = null

                // let's additionally stop if it Animatable
                if (result is Animatable) {
                    val animatable = result as Animatable
                    wasPlayingBefore = animatable.isRunning
                    val isPlaying = wasPlayingBefore
                    if (isPlaying) {
                        animatable.stop()
                    }
                }
            }

            loader.cancel(this)
        }
    }

    /**
     * @since 3.0.1
     */
    protected fun setPlaceholderResult(placeholder: Drawable) {
        // okay, if placeholder has bounds -> use it, otherwise use original imageSize
        // it's important to NOT pass to imageSizeResolver when placeholder has bounds
        // this is done, so actual result and placeholder can have _different_
        // bounds. Assume image is loaded with HTML and has ImageSize width=100%,
        // so, even if placeholder has exact bounds, it will still be scaled up.

        // this condition should not be true for placeholder (at least for now)
        // (right now this method is always called from constructor)

        if (result != null) {
            // but it is, unregister current result
            result!!.callback = null
        }

        val rect = placeholder.bounds

        if (rect.isEmpty) {
            // check for intrinsic bounds
            val intrinsic = DrawableUtils.intrinsicBounds(placeholder)
            if (intrinsic.isEmpty) {
                // @since 4.2.2
                // if intrinsic bounds are empty, use _any_ non-empty bounds,
                // they must be non-empty so when result is obtained - proper invalidation will occur
                // (0, 0, 1, 0) is still considered empty
                placeholder.setBounds(0, 0, 1, 1)
            } else {
                // use them
                placeholder.bounds = intrinsic
            }

            // it is very important (if we have a placeholder) to set own bounds to it (and they must not be empty
            // otherwise result won't be rendered)
            // @since 4.2.2
            bounds = placeholder.bounds
            result = placeholder
        } else {
            // this method is not the same as above, as we do not want to trigger image-size-resolver
            // in case when placeholder has exact bounds

            // placeholder has bounds specified -> use them until we have real result

            this.result = placeholder
            result!!.callback = callback

            // use bounds directly
            bounds = rect

            // just in case -> so we do not update placeholder when we have canvas dimensions
            waitingForDimensions = false
        }
    }

    /**
     * Remove result from this drawable (for example, in case of cancellation)
     *
     * @since 3.0.1
     */
    fun clearResult() {
        val result = this.result

        if (result != null) {
            result.callback = null
            this.result = null

            // clear bounds
            setBounds(0, 0, 0, 0)
        }
    }

    private fun initBounds() {
        if (lastKnownCanvasWidth == 0) {
            // we still have no bounds - wait for them
            waitingForDimensions = true

            // we cannot have empty bounds - otherwise in case if text contains
            //  a single AsyncDrawableSpan, it won't be displayed
            bounds = noDimensionsBounds(result)
            return
        }

        waitingForDimensions = false

        val bounds = resolveBounds()

        result!!.bounds = bounds
        // @since 4.2.1, we set callback after bounds are resolved
        //  to reduce number of invalidations
        result!!.callback = callback

        // so, this method will check if there is previous bounds and call invalidate _BEFORE_
        //  applying new bounds. This is why it is important to have initial bounds empty.
        setBounds(bounds)

        invalidateSelf()
    }

    /**
     * @since 1.0.1
     */
    fun initWithKnownDimensions(width: Int, textSize: Float) {
        this.lastKnownCanvasWidth = width
        this.lastKnowTextSize = textSize

        if (waitingForDimensions) {
            initBounds()
        }
        onDimensionsKnownListeners.forEach { it.onDimensionsKnown(this) }
    }

    override fun draw(canvas: Canvas) {
        if (hasResult()) {
            result!!.draw(canvas)
        }
    }

    override fun setAlpha(@IntRange(from = 0, to = 255) alpha: Int) {
    }

    override fun setColorFilter(colorFilter: ColorFilter?) {
    }

    @Deprecated("Deprecated in Java")
    override fun getOpacity(): Int {
        val opacity = if (hasResult()) {
            result!!.opacity
        } else {
            PixelFormat.TRANSPARENT
        }
        return opacity
    }

    override fun getIntrinsicWidth(): Int {
        val out = if (hasResult()) {
            result!!.intrinsicWidth
        } else {
            // @since 4.0.0, must not be zero in order to receive canvas dimensions
            1
        }
        return out
    }

    override fun getIntrinsicHeight(): Int {
        val out = if (hasResult()) {
            result!!.intrinsicHeight
        } else {
            // @since 4.0.0, must not be zero in order to receive canvas dimensions
            1
        }
        return out
    }

    /**
     * @since 1.0.1
     */
    private fun resolveBounds(): Rect {
        // @since 2.0.0 previously we were checking if image is greater than canvas width here
        //          but as imageSizeResolver won't be null anymore, we should transfer this logic
        //          there
        return imageSizeResolver.resolveImageSize(this)
    }

    override fun toString(): String {
        return "AsyncDrawable{" +
                "destination='" + destination + '\'' +
                ", imageSize=" + imageSize +
                ", result=" + result +
                ", canvasWidth=" + lastKnownCanvasWidth +
                ", textSize=" + lastKnowTextSize +
                ", waitingForDimensions=" + waitingForDimensions +
                '}'
    }

    @MainThread
    fun registerOnDimensionsKnownListener(l: OnDimensionsKnownListener) {
        if (hasKnownDimensions()) {
            l.onDimensionsKnown(this)
            return
        }

        onDimensionsKnownListeners.add(l)
    }

    @MainThread
    fun unregisterOnDimensionsKnownListener(l: OnDimensionsKnownListener) {
        onDimensionsKnownListeners.remove(l)
    }

    // @since 4.2.1
    //  Wrapped callback to trigger invalidation for this AsyncDrawable instance (and not result/placeholder)
    private inner class WrappedCallback(private val callback: Callback) :
        Callback {
        override fun invalidateDrawable(who: Drawable) {
            callback.invalidateDrawable(this@AsyncDrawable)
        }

        override fun scheduleDrawable(who: Drawable, what: Runnable, `when`: Long) {
            callback.scheduleDrawable(this@AsyncDrawable, what, `when`)
        }

        override fun unscheduleDrawable(who: Drawable, what: Runnable) {
            callback.unscheduleDrawable(this@AsyncDrawable, what)
        }
    }

    companion object {
        /**
         * @since 4.3.0
         */
        private fun noDimensionsBounds(result: Drawable?): Rect {
            if (result != null) {
                val bounds = result.bounds
                if (!bounds.isEmpty) {
                    return bounds
                }
                val intrinsicBounds = DrawableUtils.intrinsicBounds(result)
                if (!intrinsicBounds.isEmpty) {
                    return intrinsicBounds
                }
            }
            return Rect(0, 0, 1, 1)
        }
    }
}
