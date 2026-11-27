package com.idunnololz.animatedavif

import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.SystemClock
import android.util.Log
import android.view.View
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.findViewTreeLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import androidx.vectordrawable.graphics.drawable.Animatable2Compat
import java.io.IOException
import java.lang.ref.WeakReference
import java.nio.ByteBuffer
import kotlin.math.roundToLong
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.aomedia.avif.android.AvifDecoder as LibAvifDecoder

class AnimatedAvifDrawable internal constructor(
  private val encoded: ByteBuffer,
  firstFrame: Bitmap,
  private val durations: DoubleArray,
  repetitionCount: Int,
) : Drawable(),
  Animatable2Compat {

  private val paint = Paint(Paint.FILTER_BITMAP_FLAG)
  private var frame = firstFrame
  private var frameIndex = 0
  private var repeatsRemaining = repetitionCount
  private var started = false
  private var finished = false
  private var playback: Job? = null
  private var hostView: WeakReference<View>? = null
  private val callbacks = mutableListOf<Animatable2Compat.AnimationCallback>()

  fun setHostView(view: View) {
    if (hostView?.get() === view) return
    hostView = WeakReference(view)
    playback?.cancel()
  }

  fun getFirstFrameAsDrawable(resources: Resources): Drawable = BitmapDrawable(resources, frame)

  override fun draw(canvas: Canvas) {
    canvas.drawBitmap(frame, null, bounds, paint)
    if (!isRunning || playback != null || callback == null) return

    val host = hostView?.get() ?: generateSequence(callback) { (it as? Drawable)?.callback }
      .filterIsInstance<View>()
      .firstOrNull() ?: return
    if (host.isAttachedToWindow) playIn(host)
  }

  override fun start() {
    if (finished) return
    started = true

    for (index in callbacks.indices.reversed()) {
      callbacks[index].onAnimationStart(this)
    }

    invalidateSelf()
  }

  override fun stop() {
    started = false
    playback?.cancel()

    for (index in callbacks.indices.reversed()) {
      callbacks[index].onAnimationEnd(this)
    }
  }

  override fun isRunning(): Boolean = started && isVisible && !finished

  override fun setVisible(visible: Boolean, restart: Boolean): Boolean {
    val changed = super.setVisible(visible, restart)
    if (!visible) playback?.cancel()
    return changed
  }

  override fun getIntrinsicWidth(): Int = frame.width

  override fun getIntrinsicHeight(): Int = frame.height

  override fun setAlpha(alpha: Int) {
    paint.alpha = alpha
  }

  override fun getAlpha(): Int = paint.alpha

  override fun setColorFilter(colorFilter: ColorFilter?) {
    paint.colorFilter = colorFilter
    invalidateSelf()
  }

  @Deprecated("Deprecated in Java")
  override fun getOpacity(): Int = PixelFormat.TRANSLUCENT

  private fun playIn(host: View) {
    playback = CoroutineScope(Dispatchers.Main).launch {
      val job = coroutineContext.job
      val listener = object : View.OnAttachStateChangeListener {
        override fun onViewAttachedToWindow(view: View) = Unit

        override fun onViewDetachedFromWindow(view: View) = job.cancel()
      }
      host.addOnAttachStateChangeListener(listener)
      try {
        if (!host.isAttachedToWindow || callback == null) return@launch

        val lifecycle = host.findViewTreeLifecycleOwner()?.lifecycle
        if (lifecycle == null) {
          playFrames()
        } else {
          lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            playFrames()
            job.cancel()
          }
        }
      } finally {
        host.removeOnAttachStateChangeListener(listener)
        playback = null
        if (isRunning) invalidateSelf()
      }
    }
  }

  private suspend fun playFrames() {
    val width = intrinsicWidth
    val height = intrinsicHeight
    var index = frameIndex
    var remaining = repeatsRemaining
    var displayedAt = SystemClock.uptimeMillis()

    try {
      withContext(Dispatchers.Default) {
        // The drawable owns the direct buffer. Release the native decoder on the same worker
        // after any in-flight decode returns, including when playback is cancelled.
        val decoder = LibAvifDecoder.create(encoded) ?: throw IOException("Invalid AVIF sequence")
        try {
          while (true) {
            ensureActive()
            val duration = frameDurationMillis(durations[index])
            val lastFrame = index == durations.lastIndex
            val nextFrame = if (lastFrame && remaining == 0) {
              null
            } else {
              val nextIndex = if (lastFrame) 0 else index + 1
              // The rendering thread may still be using the previous bitmap.
              Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).also {
                decoder.decodeFrame(nextIndex, it)
              }
            }

            delay(duration - (SystemClock.uptimeMillis() - displayedAt))
            val keepPlaying = withContext(Dispatchers.Main.immediate) {
              if (callback == null || !isRunning) return@withContext false
              if (nextFrame == null) {
                finished = true
                return@withContext false
              }
              if (lastFrame && remaining > 0) remaining--
              index = if (lastFrame) 0 else index + 1
              frame = nextFrame
              frameIndex = index
              repeatsRemaining = remaining
              displayedAt = SystemClock.uptimeMillis()
              invalidateSelf()
              true
            }
            if (!keepPlaying) break
          }
        } finally {
          decoder.release()
        }
      }
    } catch (e: IOException) {
      finished = true
      Log.w("AnimatedAvifDrawable", "AVIF playback stopped after a decode error", e)
    }
  }

  override fun registerAnimationCallback(callback: Animatable2Compat.AnimationCallback) {
    callbacks.add(callback)
  }

  override fun unregisterAnimationCallback(
    callback: Animatable2Compat.AnimationCallback,
  ): Boolean = callbacks.remove(callback)

  override fun clearAnimationCallbacks() {
    callbacks.clear()
  }
}

private fun frameDurationMillis(seconds: Double): Long = if (seconds.isFinite() && seconds > 0) {
  (seconds * 1000).roundToLong().coerceAtLeast(1L)
} else {
  100L
}
