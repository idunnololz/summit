package com.idunnololz.summit.util.coil

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.media.MediaMetadataRetriever
import android.media.MediaMetadataRetriever.METADATA_KEY_DURATION
import android.media.MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT
import android.media.MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION
import android.media.MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH
import android.media.MediaMetadataRetriever.OPTION_CLOSEST_SYNC
import android.os.Build.VERSION.SDK_INT
import androidx.annotation.RequiresApi
import androidx.core.graphics.applyCanvas
import androidx.core.graphics.createBitmap
import androidx.core.graphics.drawable.toDrawable
import coil.ImageLoader
import coil.annotation.ExperimentalCoilApi
import coil.decode.AssetMetadata
import coil.decode.ContentMetadata
import coil.decode.DecodeResult
import coil.decode.DecodeUtils
import coil.decode.Decoder
import coil.decode.ImageSource
import coil.decode.ResourceMetadata
import coil.fetch.SourceResult
import coil.request.Options
import coil.request.videoFrameMicros
import coil.request.videoFrameOption
import coil.request.videoFramePercent
import coil.size.Dimension
import coil.size.Dimension.Pixels
import coil.size.Scale
import coil.size.Size
import coil.size.isOriginal
import coil.size.pxOrElse
import com.idunnololz.summit.R
import com.idunnololz.summit.util.ext.getDrawableCompat
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.roundToLong

class CustomVideoFrameDecoder(
    private val source: ImageSource,
    private val options: Options,
) : Decoder {

    private val overlay by lazy {
        options.context.getDrawableCompat(R.drawable.video_overlay)
    }

    override suspend fun decode() = MediaMetadataRetriever().use { retriever ->
        retriever.setDataSource(source)
        val option = options.parameters.videoFrameOption() ?: OPTION_CLOSEST_SYNC
        val frameMicros = computeFrameMicros(retriever)

        // Resolve the dimensions to decode the video frame at accounting
        // for the source's aspect ratio and the target's size.
        var srcWidth: Int
        var srcHeight: Int
        val rotation = retriever.extractMetadata(METADATA_KEY_VIDEO_ROTATION)?.toIntOrNull() ?: 0
        if (rotation == 90 || rotation == 270) {
            srcWidth = retriever.extractMetadata(METADATA_KEY_VIDEO_HEIGHT)?.toIntOrNull() ?: 0
            srcHeight = retriever.extractMetadata(METADATA_KEY_VIDEO_WIDTH)?.toIntOrNull() ?: 0
        } else {
            srcWidth = retriever.extractMetadata(METADATA_KEY_VIDEO_WIDTH)?.toIntOrNull() ?: 0
            srcHeight = retriever.extractMetadata(METADATA_KEY_VIDEO_HEIGHT)?.toIntOrNull() ?: 0
        }

        val dstSize = if (srcWidth > 0 && srcHeight > 0) {
            val dstWidth = options.size.widthPx(options.scale) { srcWidth }
            val dstHeight = options.size.heightPx(options.scale) { srcHeight }
            val rawScale = DecodeUtils.computeSizeMultiplier(
                srcWidth = srcWidth,
                srcHeight = srcHeight,
                dstWidth = dstWidth,
                dstHeight = dstHeight,
                scale = options.scale,
            )
            val scale = if (options.allowInexactSize) {
                rawScale.coerceAtMost(1.0)
            } else {
                rawScale
            }
            val width = (scale * srcWidth).roundToInt()
            val height = (scale * srcHeight).roundToInt()
            Size(width, height)
        } else {
            // We were unable to decode the video's dimensions.
            // Fall back to decoding the video frame at the original size.
            // We'll scale the resulting bitmap after decoding if necessary.
            Size.ORIGINAL
        }

        val (dstWidth, dstHeight) = dstSize
        val rawBitmap: Bitmap? = if (SDK_INT >= 27 && dstWidth is Pixels && dstHeight is Pixels) {
            retriever.getScaledFrameAtTime(
                frameMicros,
                option,
                dstWidth.px,
                dstHeight.px,
                options.config,
            )
        } else {
            retriever.getFrameAtTime(frameMicros, option, options.config)?.also {
                srcWidth = it.width
                srcHeight = it.height
            }
        }

        // If you encounter this exception make sure your video is encoded in a supported codec.
        // https://developer.android.com/guide/topics/media/media-formats#video-formats
        checkNotNull(rawBitmap) { "Failed to decode frame at $frameMicros microseconds." }

        val bitmap = normalizeBitmap(rawBitmap, dstSize)

        val isSampled = if (srcWidth > 0 && srcHeight > 0) {
            DecodeUtils.computeSizeMultiplier(
                srcWidth = srcWidth,
                srcHeight = srcHeight,
                dstWidth = bitmap.width,
                dstHeight = bitmap.height,
                scale = options.scale,
            ) < 1.0
        } else {
            // We were unable to determine the original size of the video. Assume it is sampled.
            true
        }

        Canvas(bitmap).apply {
            val overlay = overlay
            if (overlay != null) {
                val size = min(bitmap.width, bitmap.height) * 0.33
                val w = size.toInt()
                val h = size.toInt()
                val start = (bitmap.width - w) / 2
                val top = (bitmap.height - h) / 2
                overlay.bounds = Rect(
                    start,
                    top,
                    start + w,
                    top + h,
                )
                overlay.draw(this)
            }
        }

        DecodeResult(
            drawable = bitmap.toDrawable(options.context.resources),
            isSampled = isSampled,
        )
    }

    private fun computeFrameMicros(retriever: MediaMetadataRetriever): Long {
        val frameMicros = options.parameters.videoFrameMicros()
        if (frameMicros != null) {
            return frameMicros
        }

        val framePercent = options.parameters.videoFramePercent()
        if (framePercent != null) {
            val durationMillis = retriever.extractMetadata(METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
            return 1000 * (framePercent * durationMillis).roundToLong()
        }

        return 0
    }

    /** Return [inBitmap] or a copy of [inBitmap] that is valid for the input [options] and [size]. */
    private fun normalizeBitmap(inBitmap: Bitmap, size: Size): Bitmap {
        // Fast path: if the input bitmap is valid, return it.
        if (isConfigValid(inBitmap, options) && isSizeValid(inBitmap, options, size)) {
            return inBitmap
        }

        // Slow path: re-render the bitmap with the correct size + config.
        val scale = DecodeUtils.computeSizeMultiplier(
            srcWidth = inBitmap.width,
            srcHeight = inBitmap.height,
            dstWidth = size.width.pxOrElse { inBitmap.width },
            dstHeight = size.height.pxOrElse { inBitmap.height },
            scale = options.scale,
        ).toFloat()
        val dstWidth = (scale * inBitmap.width).roundToInt()
        val dstHeight = (scale * inBitmap.height).roundToInt()
        val safeConfig = when {
            SDK_INT >= 26 && options.config == Bitmap.Config.HARDWARE -> Bitmap.Config.ARGB_8888
            else -> options.config
        }

        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        val outBitmap = createBitmap(dstWidth, dstHeight, safeConfig)
        outBitmap.applyCanvas {
            scale(scale, scale)
            drawBitmap(inBitmap, 0f, 0f, paint)
        }
        inBitmap.recycle()

        return outBitmap
    }

    private fun isConfigValid(bitmap: Bitmap, options: Options): Boolean {
        return SDK_INT < 26 ||
            bitmap.config != Bitmap.Config.HARDWARE ||
            options.config == Bitmap.Config.HARDWARE
    }

    private fun isSizeValid(bitmap: Bitmap, options: Options, size: Size): Boolean {
        if (options.allowInexactSize) return true
        val multiplier = DecodeUtils.computeSizeMultiplier(
            srcWidth = bitmap.width,
            srcHeight = bitmap.height,
            dstWidth = size.width.pxOrElse { bitmap.width },
            dstHeight = size.height.pxOrElse { bitmap.height },
            scale = options.scale,
        )
        return multiplier == 1.0
    }

    @OptIn(ExperimentalCoilApi::class)
    private fun MediaMetadataRetriever.setDataSource(source: ImageSource) {
        when (val metadata = source.metadata) {
            is AssetMetadata -> {
                options.context.assets.openFd(metadata.filePath).use {
                    setDataSource(it.fileDescriptor, it.startOffset, it.length)
                }
            }
            is ContentMetadata -> {
                setDataSource(options.context, metadata.uri)
            }
            is ResourceMetadata -> {
                setDataSource("android.resource://${metadata.packageName}/${metadata.resId}")
            }
            else -> {
                setDataSource(source.file().toFile().path)
            }
        }
    }

    class Factory : Decoder.Factory {

        override fun create(
            result: SourceResult,
            options: Options,
            imageLoader: ImageLoader,
        ): Decoder? {
            if (!isApplicable(result.mimeType)) return null
            return CustomVideoFrameDecoder(result.source, options)
        }

        private fun isApplicable(mimeType: String?): Boolean {
            return mimeType != null && mimeType.startsWith("video/")
        }

        override fun equals(other: Any?) = other is Factory

        override fun hashCode() = javaClass.hashCode()
    }

    companion object {
        const val VIDEO_FRAME_MICROS_KEY = "coil#video_frame_micros"
        const val VIDEO_FRAME_PERCENT_KEY = "coil#video_frame_percent"
        const val VIDEO_FRAME_OPTION_KEY = "coil#video_frame_option"
    }

    private fun MediaMetadataRetriever.getFrameAtTime(
        timeUs: Long,
        option: Int,
        config: Bitmap.Config,
    ): Bitmap? = if (SDK_INT >= 30) {
        val params = MediaMetadataRetriever.BitmapParams().apply { preferredConfig = config }
        getFrameAtTime(timeUs, option, params)
    } else {
        getFrameAtTime(timeUs, option)
    }

    @RequiresApi(27)
    internal fun MediaMetadataRetriever.getScaledFrameAtTime(
        timeUs: Long,
        option: Int,
        dstWidth: Int,
        dstHeight: Int,
        config: Bitmap.Config,
    ): Bitmap? = if (SDK_INT >= 30) {
        val params = MediaMetadataRetriever.BitmapParams().apply { preferredConfig = config }
        getScaledFrameAtTime(timeUs, option, dstWidth, dstHeight, params)
    } else {
        getScaledFrameAtTime(timeUs, option, dstWidth, dstHeight)
    }

    private inline fun Size.widthPx(scale: Scale, original: () -> Int): Int {
        return if (isOriginal) original() else width.toPx(scale)
    }

    private inline fun Size.heightPx(scale: Scale, original: () -> Int): Int {
        return if (isOriginal) original() else height.toPx(scale)
    }

    private fun Dimension.toPx(scale: Scale) = pxOrElse {
        when (scale) {
            Scale.FILL -> Int.MIN_VALUE
            Scale.FIT -> Int.MAX_VALUE
        }
    }
}
