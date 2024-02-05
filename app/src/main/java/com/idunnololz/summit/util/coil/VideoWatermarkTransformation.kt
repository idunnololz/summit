package com.idunnololz.summit.util.coil

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import androidx.core.graphics.applyCanvas
import androidx.core.graphics.createBitmap
import coil.request.Options
import coil.size.Size
import coil.transform.Transformation
import com.commit451.coiltransformations.BlurTransformation
import com.idunnololz.summit.R
import com.idunnololz.summit.util.ext.getDrawableCompat
import com.idunnololz.summit.util.safeConfig
import kotlin.math.min

class VideoWatermarkTransformation(
    private val context: Context,
    private val sampling: Float = 1f
) : Transformation {

    private val overlay by lazy {
        context.getDrawableCompat(R.drawable.video_overlay)
    }

    @Suppress("NullableToStringCall")
    override val cacheKey = "VideoWatermarkTransformation"

    override suspend fun transform(input: Bitmap, size: Size): Bitmap {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)

        val scaledWidth = (input.width / sampling).toInt()
        val scaledHeight = (input.height / sampling).toInt()
        val output = createBitmap(scaledWidth, scaledHeight, input.safeConfig)

        output.applyCanvas {
            val rectSrc = Rect(0, 0, input.width, input.height)
            val rectDest = Rect(0, 0, scaledWidth, scaledHeight)

            drawBitmap(input, rectSrc, rectDest, paint)

            val overlay = overlay
            if (overlay != null) {
                val size = min(scaledWidth, scaledHeight) * 0.33
                val w = size.toInt()
                val h = size.toInt()
                val start = (scaledWidth - w) / 2
                val top = (scaledHeight - h) / 2
                overlay.bounds = Rect(
                    start,
                    top,
                    start + w,
                    top + h,
                )
                overlay.draw(this)
            }
        }

        return output
    }
}