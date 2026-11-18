package com.idunnololz.animatedavif

import android.graphics.Bitmap
import coil3.ImageLoader
import coil3.annotation.ExperimentalCoilApi
import coil3.asImage
import coil3.decode.DecodeResult
import coil3.decode.DecodeUtils
import coil3.decode.Decoder
import coil3.fetch.SourceFetchResult
import coil3.gif.repeatCount
import coil3.request.Options
import coil3.request.maxBitmapSize
import coil3.size.Precision
import coil3.size.Size
import coil3.util.component1
import coil3.util.component2
import java.io.IOException
import java.nio.ByteBuffer
import kotlin.math.roundToInt
import kotlinx.coroutines.runInterruptible
import okio.BufferedSource
import org.aomedia.avif.android.AvifDecoder as LibAvifDecoder

class AnimatedAvifDecoder private constructor(
  private val result: SourceFetchResult,
  private val options: Options,
  private val maxFrameSize: Size,
) : Decoder {

  @OptIn(ExperimentalCoilApi::class)
  override suspend fun decode(): DecodeResult? = runInterruptible {
    val bytes = result.source.source().peek().use { it.readByteArray() }
    val encoded = ByteBuffer.allocateDirect(bytes.size).apply {
      put(bytes)
      rewind()
    }
    val decoder = LibAvifDecoder.create(encoded) ?: throw IOException("Invalid AVIF sequence")

    try {
      if (decoder.frameCount <= 1) {
        return@runInterruptible null
      }

      val width = decoder.width
      val height = decoder.height
      if (width <= 0 || height <= 0) throw IOException("Invalid AVIF sequence dimensions")

      val (targetWidth, targetHeight) = DecodeUtils.computeDstSize(
        srcWidth = width,
        srcHeight = height,
        targetSize = options.size,
        scale = options.scale,
        maxSize = options.maxBitmapSize,
      )
      val multiplier = DecodeUtils.computeSizeMultiplier(
        srcWidth = width,
        srcHeight = height,
        dstWidth = targetWidth,
        dstHeight = targetHeight,
        scale = options.scale,
        maxSize = maxFrameSize,
      ).let { if (options.precision == Precision.INEXACT) it.coerceAtMost(1.0) else it }
      val frame = Bitmap.createBitmap(
        (width * multiplier).roundToInt().coerceAtLeast(1),
        (height * multiplier).roundToInt().coerceAtLeast(1),
        Bitmap.Config.ARGB_8888,
      )
      decoder.decodeFrame(0, frame)

      DecodeResult(
        image = AnimatedAvifDrawable(
          encoded,
          frame,
          decoder.frameDurations,
          if (options.repeatCount >= 0) options.repeatCount else decoder.repetitionCount,
        ).asImage(shareable = false),
        isSampled = multiplier < 1,
      )
    } finally {
      decoder.release()
    }
  }

  class Factory(
    private val maxFrameSize: Size,
    private val isEnabled: () -> Boolean = { true },
  ) : Decoder.Factory {
    override fun create(
      result: SourceFetchResult,
      options: Options,
      imageLoader: ImageLoader,
    ): Decoder? = if (isEnabled() && result.source.source().isAvifSequence()) {
      AnimatedAvifDecoder(result, options, maxFrameSize)
    } else {
      null
    }
  }
}

internal fun LibAvifDecoder.decodeFrame(index: Int, bitmap: Bitmap) {
  val status = if (nextFrameIndex() == index) nextFrame(bitmap) else nthFrame(index, bitmap)
  if (status != 0) throw IOException("AVIF frame $index: ${LibAvifDecoder.resultToString(status)}")
}

private const val FILE_TYPE_BOX = 0x66747970 // ftyp
private const val SEQUENCE_BRAND = 0x61766973 // avis
private const val MAX_BRAND_SCAN_BYTES = 4096L

internal fun BufferedSource.isAvifSequence(): Boolean = peek().use { header ->
  if (!header.request(8)) return@use false
  val size = header.readInt().toLong() and 0xffffffffL
  if (header.readInt() != FILE_TYPE_BOX) return@use false

  val headerSize = if (size == 1L) 16 else 8
  val boxSize = if (size == 1L) {
    if (!header.request(8)) return@use false
    header.readLong()
  } else {
    size
  }
  if (boxSize < headerSize + 8 || !header.request(8)) return@use false
  if (header.readInt() == SEQUENCE_BRAND) return@use true
  header.skip(4) // minor version

  val brandCount = (minOf(boxSize, MAX_BRAND_SCAN_BYTES) - headerSize - 8) / 4
  repeat(brandCount.toInt()) {
    if (!header.request(4)) return@use false
    if (header.readInt() == SEQUENCE_BRAND) return@use true
  }
  false
}
