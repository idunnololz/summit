package com.idunnololz.summit.util.coil

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.os.Build.VERSION.SDK_INT
import android.util.Log
import androidx.core.graphics.createBitmap
import coil3.ImageLoader
import coil3.annotation.ExperimentalCoilApi
import coil3.annotation.InternalCoilApi
import coil3.asImage
import coil3.decode.DecodeResult
import coil3.decode.DecodeUtils
import coil3.decode.Decoder
import coil3.decode.ExifOrientationStrategy
import coil3.decode.ExifOrientationStrategy.Companion.RESPECT_PERFORMANCE
import coil3.fetch.SourceFetchResult
import coil3.request.Options
import coil3.request.allowRgb565
import coil3.request.bitmapConfig
import coil3.request.colorSpace
import coil3.request.maxBitmapSize
import coil3.request.premultipliedAlpha
import coil3.size.Precision
import coil3.util.component1
import coil3.util.component2
import coil3.util.toSoftware
import com.idunnololz.summit.preferences.Preferences
import kotlinx.coroutines.runInterruptible
import okio.Buffer
import okio.ByteString.Companion.encodeUtf8
import okio.ForwardingSource
import okio.Source
import okio.buffer
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.nio.ByteBuffer
import java.util.concurrent.atomic.AtomicReference

class AvifDecoder(
  private val source: SourceFetchResult,
  private val options: Options,
  private val exifOrientationStrategy: ExifOrientationStrategy = RESPECT_PERFORMANCE,
) : Decoder {

  companion object {
    private const val TAG = "AvifDecoder"

    private const val BUFFER_SIZE = 16384
    private val BUFFER_REF = AtomicReference<ByteArray>()
    private const val MIME_TYPE_JPEG = "image/jpeg"
  }

  override suspend fun decode(): DecodeResult? = runInterruptible {
    val safeSource = ExceptionCatchingSource(source.source.source())
    val safeBufferedSource = safeSource.buffer()
    val byteBuffer = fromStream(safeBufferedSource.peek().inputStream())

    val boundsOptions = BitmapFactory.Options().apply {
      inJustDecodeBounds = true
    }

    if (SDK_INT > 32) {
      BitmapFactory.decodeStream(
        safeBufferedSource.peek().inputStream(),
        null,
        boundsOptions
      )
      safeSource.exception?.let { throw it }
    } else {
      val info = org.aomedia.avif.android.AvifDecoder.Info()
      val success = org.aomedia.avif.android.AvifDecoder.getInfo(
        byteBuffer, byteBuffer.remaining(), info)

      if (!success) {
        error("Requested to decode byte buffer which cannot be handled by AvifDecoder")
      }

      boundsOptions.outWidth = info.width
      boundsOptions.outHeight = info.height
    }
    boundsOptions.inJustDecodeBounds = false

    val exifData = ExifUtils.getExifData(
      mimeType = boundsOptions.outMimeType,
      source = safeBufferedSource,
      strategy = exifOrientationStrategy
    )
    safeSource.exception?.let { throw it }

    boundsOptions.inMutable = false

    if (SDK_INT >= 26 && options.colorSpace != null) {
      boundsOptions.inPreferredColorSpace = options.colorSpace
    }
    boundsOptions.inPremultiplied = options.premultipliedAlpha

    boundsOptions.configureConfig(exifData)
    val scale = boundsOptions.getScale(exifData)

    val srcWidth = if (exifData.isSwapped) boundsOptions.outHeight else boundsOptions.outWidth
    val srcHeight = if (exifData.isSwapped) boundsOptions.outWidth else boundsOptions.outHeight

    val dstWidth = (srcWidth * scale).toInt()
    val dstHeight = (srcHeight * scale).toInt()

    Log.d(TAG, "image out: $dstWidth x $dstHeight scale: $scale")

    val bitmap = createBitmap(dstWidth, dstHeight)

    safeBufferedSource.use {
      org.aomedia.avif.android.AvifDecoder.decode(
        byteBuffer, byteBuffer.remaining(), bitmap,
      )

      return@runInterruptible DecodeResult(
        image = bitmap.asImage(),
        isSampled = true,
      )
    }
  }

  @Throws(IOException::class)
  private fun fromStream(stream: InputStream): ByteBuffer {
    val outStream = ByteArrayOutputStream(BUFFER_SIZE)

    var buffer: ByteArray? = BUFFER_REF.getAndSet(null)
    if (buffer == null) {
      buffer = ByteArray(BUFFER_SIZE)
    }

    var n: Int
    while ((stream.read(buffer).also { n = it }) >= 0) {
      outStream.write(buffer, 0, n)
    }

    BUFFER_REF.set(buffer)

    val bytes = outStream.toByteArray()

    // Some resource decoders require a direct byte buffer. Prefer allocateDirect() over wrap()
    return rewind(ByteBuffer.allocateDirect(bytes.size).put(bytes))
  }

  private fun rewind(buffer: ByteBuffer): ByteBuffer {
    return buffer.position(0) as ByteBuffer
  }

  /** Compute and set [BitmapFactory.Options.inPreferredConfig]. */
  @OptIn(InternalCoilApi::class)
  private fun BitmapFactory.Options.configureConfig(exifData: ExifData) {
    var config = options.bitmapConfig

    // Disable hardware bitmaps if we need to perform EXIF transformations.
    if (exifData.isFlipped || exifData.isRotated) {
      config = config.toSoftware()
    }

    // Decode the image as RGB_565 as an optimization if allowed.
    if (options.allowRgb565 && config == Bitmap.Config.ARGB_8888 && outMimeType == MIME_TYPE_JPEG) {
      config = Bitmap.Config.RGB_565
    }

    // High color depth images must be decoded as either RGBA_F16 or HARDWARE.
    if (SDK_INT >= 26 && outConfig == Bitmap.Config.RGBA_F16 && config != Bitmap.Config.HARDWARE) {
      config = Bitmap.Config.RGBA_F16
    }

    inPreferredConfig = config
  }

  @OptIn(ExperimentalCoilApi::class)
  private fun BitmapFactory.Options.getScale(exifData: ExifData): Double {
    // This occurs if there was an error decoding the image's size.
    if (outWidth <= 0 || outHeight <= 0) {
      inSampleSize = 1
      inScaled = false
      return 1.0
    }

    // srcWidth and srcHeight are the original dimensions of the image after
    // EXIF transformations (but before sampling).
    val srcWidth = if (exifData.isSwapped) outHeight else outWidth
    val srcHeight = if (exifData.isSwapped) outWidth else outHeight

    val (dstWidth, dstHeight) = DecodeUtils.computeDstSize(
      srcWidth = srcWidth,
      srcHeight = srcHeight,
      targetSize = options.size,
      scale = options.scale,
      maxSize = options.maxBitmapSize,
    )

    // Calculate the image's sample size.
    inSampleSize = DecodeUtils.calculateInSampleSize(
      srcWidth = srcWidth,
      srcHeight = srcHeight,
      dstWidth = dstWidth,
      dstHeight = dstHeight,
      scale = options.scale,
    )

    // Calculate the image's density scaling multiple.
    var scale = DecodeUtils.computeSizeMultiplier(
      srcWidth = srcWidth / inSampleSize.toDouble(),
      srcHeight = srcHeight / inSampleSize.toDouble(),
      dstWidth = dstWidth.toDouble(),
      dstHeight = dstHeight.toDouble(),
      scale = options.scale,
    )
    if (options.precision == Precision.INEXACT) {
      scale = scale.coerceAtMost(1.0)
    }

    return scale
  }

  class Factory(private val preferences: Preferences) : Decoder.Factory {
    override fun create(
      result: SourceFetchResult,
      options: Options,
      imageLoader: ImageLoader,
    ): Decoder? {
      if (!preferences.useBundledAvifDecoder) {
        return null
      }

      return if (
        AVAILABLE_BRANDS.any {
          result.source.source().rangeEquals(4, it)
        }
      ) {
        AvifDecoder(
          source = result,
          options = options,
        )
      } else null
    }

    companion object {
      private val MIF = "ftypmif1".encodeUtf8()
      private val MSF = "ftypmsf1".encodeUtf8()
      private val HEIC = "ftypheic".encodeUtf8()
      private val HEIX = "ftypheix".encodeUtf8()
      private val HEVC = "ftyphevc".encodeUtf8()
      private val HEVX = "ftyphevx".encodeUtf8()
      private val AVIF = "ftypavif".encodeUtf8()
      private val AVIS = "ftypavis".encodeUtf8()

      private val AVAILABLE_BRANDS = listOf(MIF, MSF, HEIC, HEIX, HEVC, HEVX, AVIF, AVIS)
    }
  }

  private class ExceptionCatchingSource(delegate: Source) : ForwardingSource(delegate) {

    var exception: Exception? = null
      private set

    override fun read(sink: Buffer, byteCount: Long): Long {
      try {
        return super.read(sink, byteCount)
      } catch (e: Exception) {
        exception = e
        throw e
      }
    }
  }
}