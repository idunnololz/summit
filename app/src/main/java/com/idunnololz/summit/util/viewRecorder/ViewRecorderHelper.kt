package com.idunnololz.summit.util.viewRecorder

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.media.MediaRecorder
import android.net.Uri
import android.os.SystemClock
import android.util.Log
import android.view.View
import com.idunnololz.summit.coroutine.CoroutineScopeFactory
import com.idunnololz.summit.fileprovider.FileProviderHelper
import com.idunnololz.summit.util.gif.AnimatedGifEncoder
import com.idunnololz.summit.util.viewRecorder.RecordingType.*
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.BufferedOutputStream
import java.io.File
import java.io.OutputStream
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.runInterruptible
import kotlinx.coroutines.withContext

class ViewRecorderHelper @Inject constructor(
    @ApplicationContext private val context: Context,
    private val coroutineScopeFactory: CoroutineScopeFactory,
) {

    companion object {
        private const val TAG = "ViewRecorderHelper"
    }

    private val coroutineScope = coroutineScopeFactory.create()

    sealed interface ViewRecordingState {
        data class RecordingState(
            val progress: Double,
        ) : ViewRecordingState

        data class Encoding(
            val progress: Double,
        ) : ViewRecordingState

        data class Complete(
            val fileUri: Uri,
            val recordingStats: RecordingStats,
        ) : ViewRecordingState

        data class Error(
            val error: Throwable,
        ) : ViewRecordingState
    }

    fun recordView(
        view: View,
        name: String,
        config: RecordScreenshotConfig,
    ): Flow<ViewRecordingState> = flow {
        val statsBuilder = RecordingStatsBuilder()
        var fileWithUri = FileProviderHelper(context)
            .getTempFile("Summit_$name.mp4")

        recordViewAsVideo(
            view = view,
            outputFile = fileWithUri.file,
            recordingType = config.recordingType,
            fps = config.maxFps.toInt(),
            durationMs = config.recordingLengthMs,
            resolutionMultiplier = config.resolutionFactor,
            qualityFactor = config.qualityFactor,
            stats = statsBuilder,
        ).collect {
            emit(it)
        }

        Log.d(TAG, "File size: ${fileWithUri.file.length()}")

        if (fileWithUri.file.length() == 0L) {
            return@flow
        }

        if (config.recordingType == Gif) {
            val newFileWithUri = FileProviderHelper(context).getTempFile("Summit_$name.gif")

            newFileWithUri.file.outputStream().use {
                convertMp4ToGif(fileWithUri.file, it, config.maxFps.toInt()).collect {
                    emit(it)
                }
            }

            fileWithUri = newFileWithUri
        }

        statsBuilder.endTime = SystemClock.elapsedRealtime()
        statsBuilder.fileSize = fileWithUri.file.length()

        emit(ViewRecordingState.Complete(fileWithUri.uri, statsBuilder.build()))
    }

    private fun createViewRecorderAndStart(
        view: View,
        surfaceW: Int,
        surfaceH: Int,
        outputFile: File,
        recordingType: RecordingType,
        fps: Int,
        resolutionMultiplier: Double,
        qualityFactor: Double,
    ): Result<ViewRecorder> {
        val viewRecorder = ViewRecorder()

        val result = run {
            // ORDERING HERE IS IMPORTANT!!!

            // 1
            viewRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE)

            // 2
            viewRecorder.setOutputFormat(
                when (recordingType) {
                    Mp4 -> MediaRecorder.OutputFormat.MPEG_4
                    Webm -> MediaRecorder.OutputFormat.WEBM
                    Gif -> MediaRecorder.OutputFormat.MPEG_4
                },
            )

            // 3
            val recordW = (surfaceW * resolutionMultiplier).toInt()
            val recordH = (surfaceH * resolutionMultiplier).toInt()

            val scaledW = if (recordW % 2 == 1) {
                recordW + 1
            } else {
                recordW
            }
            val scaledH = if (recordH % 2 == 1) {
                recordH + 1
            } else {
                recordH
            }

            viewRecorder.setVideoSize(scaledW, scaledH)
            viewRecorder.setVideoEncoder(
                when (recordingType) {
                    Mp4 -> MediaRecorder.VideoEncoder.H264
                    Webm -> MediaRecorder.VideoEncoder.VP8
                    Gif -> MediaRecorder.VideoEncoder.H264
                },
            )
            viewRecorder.setOutputFile(outputFile.path)
//            viewRecorder.setVideoSize(720, 1280)
            viewRecorder.setVideoFrameRate(fps)
            viewRecorder.setVideoEncodingBitRate(
                (calculateBitRate(recordW, recordH) * qualityFactor).toInt(),
            )

            viewRecorder.setOnErrorListener { _, what, _ -> Log.d(TAG, "Error: $what") }

            viewRecorder.setRecordedView(view)
            try {
                viewRecorder.prepare()
                viewRecorder.start()

                Result.success(Unit)
            } catch (e: Exception) {
                Log.e(TAG, "startRecord failed", e)

                Result.failure(e)
            }
        }

        return result.fold(
            {
                Result.success(viewRecorder)
            },
            {
                Result.failure(it)
            },
        )
    }

    private suspend fun recordViewAsVideo(
        view: View,
        outputFile: File,
        recordingType: RecordingType,
        fps: Int,
        durationMs: Long,
        resolutionMultiplier: Double,
        qualityFactor: Double,
        stats: RecordingStatsBuilder,
    ): Flow<ViewRecordingState> = flow {
        emit(ViewRecordingState.RecordingState(0.0))

        val viewRecorderResult = createViewRecorderAndStart(
            view = view,
            surfaceW = view.measuredWidth,
            surfaceH = view.measuredHeight,
            outputFile = outputFile,
            recordingType = recordingType,
            fps = fps,
            resolutionMultiplier = resolutionMultiplier,
            qualityFactor = qualityFactor,
        )

        if (viewRecorderResult.isFailure) {
            emit(ViewRecordingState.Error(requireNotNull(viewRecorderResult.exceptionOrNull())))
            return@flow
        }

        val viewRecorder = viewRecorderResult.getOrThrow()

        val startTime = System.currentTimeMillis()
        while (durationMs > System.currentTimeMillis() - startTime) {
            delay(300)
            emit(
                ViewRecordingState.RecordingState(
                    (System.currentTimeMillis() - startTime).toDouble() / durationMs,
                ),
            )
        }

        emit(ViewRecordingState.RecordingState(1.0))

        withContext(Dispatchers.Main) {
            try {
                viewRecorder.stop()
                viewRecorder.reset()
                viewRecorder.release()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        stats.surfaceRecorderStats = viewRecorder.lastRecordingStats
    }.flowOn(Dispatchers.Default)

    private suspend fun convertMp4ToGif(
        outputFile: File,
        outputStream: OutputStream,
        fps: Int,
    ): Flow<ViewRecordingState> = flow {
        val frameRate = fps.toFloat()
        val gifEncoder = AnimatedGifEncoder()
        val os = BufferedOutputStream(outputStream)

        gifEncoder.setFrameRate(frameRate)
        gifEncoder.start(os)

        val mmr = MediaMetadataRetriever()
            .apply {
                setDataSource(outputFile.path)
            }
        val duration = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            ?.let { Integer.parseInt(it) }

        if (duration == null) {
            emit(ViewRecordingState.Error(RuntimeException("Duration is null")))
            return@flow
        }

        val increment = (1000 / frameRate).toLong()
        val maxFrames = duration / increment

        Log.d(TAG, "Encoding $maxFrames frames...")

        var counter = 0L
        var i = 0
        while (counter < duration) {
            val b: Bitmap? = mmr.getFrameAtTime(
                counter * 1000,
                MediaMetadataRetriever.OPTION_CLOSEST,
            )

            if (b == null) {
                Log.d(TAG, "Bitmap was null!")
            } else {
                gifEncoder.addFrame(b)
                b.recycle()
            }

            counter += increment

            emit(ViewRecordingState.Encoding(i.toDouble() / maxFrames))

            Log.d(TAG, "Encoded ${++i} / $maxFrames frames...")
        }

        gifEncoder.finish()

        runInterruptible {
            os.flush()
        }
    }

    private fun calculateBitRate(w: Int, h: Int): Int {
        val resolution = w * h

        // bitrates from https://developer.android.com/media/optimize/sharing
        return when {
            resolution < 306176 /* SD */ -> {
                2_000_000
            }
            resolution < 921600 /* 720p */ -> {
                8_000_000
            }
            resolution < 2073600 /* 1080p */ -> {
                16_000_000
            }
            else -> /* 4k or above */ {
                20_000_000
            }
        }
    }

    private fun RecordingStatsBuilder.build() = RecordingStats(
        surfaceRecorderStats?.effectiveFrameRate,
        surfaceRecorderStats?.frameTimeMs,
        endTime?.let {
            it - startTime
        },
        fileSize,
    )

    private data class RecordingStatsBuilder(
        var surfaceRecorderStats: SurfaceRecorderStats? = null,
        val startTime: Long = SystemClock.elapsedRealtime(),
        var endTime: Long? = null,
        var fileSize: Long? = null,
    )

    data class RecordingStats(
        val effectiveFrameRate: Double?,
        /**
         * How long it took to draw a single frame. Excludes [delay()].
         */
        val frameTimeMs: Double?,
        val totalTimeSpent: Long?,
        var fileSize: Long?,
    )
}
