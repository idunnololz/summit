package com.idunnololz.summit.util.viewRecorder

import android.graphics.Canvas
import android.media.MediaRecorder
import android.os.SystemClock
import android.util.Log
import android.view.Surface
import com.idunnololz.summit.util.PrettyPrintUtils
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.newSingleThreadContext
import kotlinx.coroutines.withContext

/**
 * This class extends [MediaRecorder] and manages to compose each video frame for recording.
 * Two extra initialization steps before [.start],
 * <pre>
 * [.setWorkerLooper]
 * [.setVideoFrameDrawer]
</pre> *
 *
 * Also you can use it as same as [MediaRecorder] for other functions.
 *
 *
 *  By the way, one more error type [.MEDIA_RECORDER_ERROR_SURFACE] is defined for surface error.
 */
open class SurfaceMediaRecorder : MediaRecorder() {
    /**
     * Interface defined for user to customize video frame composition
     */
    interface VideoFrameDrawer {
        /**
         * Called when video frame is composing
         *
         * @param canvas the canvas on which content will be drawn
         */
        fun onDraw(canvas: Canvas)
    }

    companion object {
        private const val TAG = "SurfaceMediaRecorder"

        /**
         * Surface error during recording, In this case, the application must release the
         * MediaRecorder object and instantiate a new one.
         *
         * @see android.media.MediaRecorder.OnErrorListener
         */
        const val MEDIA_RECORDER_ERROR_SURFACE: Int = 10000

        /**
         * Surface error when getting for drawing into this [Surface].
         *
         * @see android.media.MediaRecorder.OnErrorListener
         */
        const val MEDIA_RECORDER_ERROR_CODE_LOCK_CANVAS: Int = 1

        /**
         * Surface error when releasing and posting content to [Surface].
         *
         * @see android.media.MediaRecorder.OnErrorListener
         */
        const val MEDIA_RECORDER_ERROR_CODE_UNLOCK_CANVAS: Int = 2

        /**
         * default inter-frame gap
         */
        private const val DEFAULT_INTERFRAME_GAP: Long = 1000L / 32L
    }

    private var videoSource = 0
    private var onErrorListener: OnErrorListener? = null
    private var interframeGap = DEFAULT_INTERFRAME_GAP // 1000 milliseconds as default
    private var recordingSurface: Surface? = null

    var lastRecordingStats: SurfaceRecorderStats? = null

    // if set, this class works same as MediaRecorder
    private var inputSurface: Surface? = null
    private var videoFrameDrawer: VideoFrameDrawer? = null

    // indicate surface composing started or not
    private val started = AtomicBoolean(false)

    // indicate surface composing paused or not
    private val paused = AtomicBoolean(false)

    @OptIn(ExperimentalCoroutinesApi::class, DelicateCoroutinesApi::class)
    private val coroutineContext = newSingleThreadContext("summit_surface_media_recorder")
    private val coroutineScope = CoroutineScope(SupervisorJob() + coroutineContext)
    private var renderJob: Job? = null

    private fun startRendering() {
        fun handlerCanvasError(errorCode: Int) {
            try {
                stop()
            } catch (e: Exception) {
                e.printStackTrace()
            }
            onErrorListener?.onError(
                this@SurfaceMediaRecorder,
                MEDIA_RECORDER_ERROR_SURFACE,
                errorCode,
            )
        }

        renderJob = coroutineScope.launch {
            var framesDrawn = 0
            var totalTimeSpentDrawing = 0L
            val startTime = SystemClock.elapsedRealtime()

            var errorCode: Int? = null

            try {
                while (true) {
                    if (!started.get()) {
                        Log.d(TAG, "Recording is not started! Stopping...")
                        break
                    }

                    if (paused.get()) {
                        delay(interframeGap)
                        continue
                    }

                    val start = SystemClock.elapsedRealtime()

                    val surface = recordingSurface
                        ?: break
                    val videoFrameDrawer = videoFrameDrawer
                        ?: break

                    withContext(Dispatchers.Main) a@{
                        val canvas: Canvas = try {
                            surface.lockCanvas(null)
                        } catch (e: Exception) {
                            errorCode = MEDIA_RECORDER_ERROR_CODE_LOCK_CANVAS
                            Log.d(TAG, "Error recording view", e)
                            return@a
                        }
                        videoFrameDrawer.onDraw(canvas)
                        try {
                            surface.unlockCanvasAndPost(canvas)
                        } catch (e: Exception) {
                            errorCode = MEDIA_RECORDER_ERROR_CODE_UNLOCK_CANVAS
                            Log.d(TAG, "Error recording view", e)
                            return@a
                        }
                    }
                    val frameTimeMs = SystemClock.elapsedRealtime() - start
                    totalTimeSpentDrawing += frameTimeMs

                    framesDrawn++

                    if (!isRecording) {
                        break
                    }

                    if (errorCode != null) {
                        handlerCanvasError(errorCode!!)
                    } else {
                        val delayMs = interframeGap - frameTimeMs - 1
                        if (delayMs > 0) {
                            delay(delayMs)
                        }
                    }
                }
            } catch (e: Exception) {
                // do nothing
                Log.d(TAG, "Error recording view", e)
            }

            if (recordingSurface == null) {
                Log.d(TAG, "Recording stopped because surface was null")
            }
            if (videoFrameDrawer == null) {
                Log.d(TAG, "Recording stopped because videoFrameDrawer was null")
            }

            val totalTimeMs = SystemClock.elapsedRealtime() - startTime
            val fps = (framesDrawn / totalTimeMs.toDouble()) * 1000.0
            val timePerFrame = totalTimeSpentDrawing / framesDrawn.toDouble()
            Log.d(
                TAG,
                "effective fps: ${PrettyPrintUtils.defaultDecimalFormat.format(fps)} " +
                    "timePerFrame: ${PrettyPrintUtils.defaultDecimalFormat.format(timePerFrame)}",
            )

            lastRecordingStats = SurfaceRecorderStats(
                effectiveFrameRate = fps,
                frameTimeMs = timePerFrame,
            )
        }
    }

    @Throws(IllegalStateException::class)
    override fun pause() {
        if (isSurfaceAvailable) {
            paused.set(true)
        }
        super.pause()
    }

    override fun reset() {
        localReset()
        super.reset()
    }

    @Throws(IllegalStateException::class)
    override fun resume() {
        super.resume()
        if (isSurfaceAvailable) {
            paused.set(false)
        }
    }

    override fun setOnErrorListener(l: OnErrorListener) {
        super.setOnErrorListener(l)
        onErrorListener = l
    }

    override fun setInputSurface(surface: Surface) {
        super.setInputSurface(surface)
        inputSurface = surface
    }

    @Throws(IllegalStateException::class)
    override fun setVideoFrameRate(rate: Int) {
        super.setVideoFrameRate(rate)
        interframeGap = (1000 / rate + (if (1000 % rate == 0) 0 else 1)).toLong()
    }

    @Throws(IllegalStateException::class)
    override fun setVideoSource(videoSource: Int) {
        super.setVideoSource(videoSource)
        this.videoSource = videoSource
    }

    @Throws(IllegalStateException::class)
    override fun start() {
        if (isSurfaceAvailable) {
            checkNotNull(videoFrameDrawer) { "video frame drawer is not initialized yet" }
        }

        super.start()
        if (isSurfaceAvailable) {
            recordingSurface = surface
            started.set(true)
            startRendering()
        }
    }

    @Throws(IllegalStateException::class)
    override fun stop() {
        localReset()
        super.stop()
    }

    /**
     * Sets video frame drawer for composing.
     * @param drawer the drawer to compose frame with [Canvas]
     * @throws IllegalStateException if it is called after [.start]
     */
    @Throws(IllegalStateException::class)
    fun setVideoFrameDrawer(drawer: VideoFrameDrawer) {
        check(!isRecording) { "setVideoFrameDrawer called in an invalid state: Recording" }
        videoFrameDrawer = drawer
    }

    protected val isSurfaceAvailable: Boolean
        /**
         * Returns whether Surface is editable
         * @return true if surface editable
         */
        get() = (videoSource == VideoSource.SURFACE) && (inputSurface == null)

    private val isRecording: Boolean
        get() = (started.get() && !paused.get())

    private fun localReset() {
        if (isSurfaceAvailable) {
            started.compareAndSet(true, false)
            paused.compareAndSet(true, false)
            renderJob?.cancel()
        }
        interframeGap = DEFAULT_INTERFRAME_GAP
        inputSurface = null
        onErrorListener = null
        videoFrameDrawer = null
    }
}
