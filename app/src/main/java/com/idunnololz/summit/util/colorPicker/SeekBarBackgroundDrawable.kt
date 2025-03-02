package com.idunnololz.summit.util.colorPicker

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PixelFormat
import android.graphics.Rect
import android.graphics.drawable.Drawable
import com.idunnololz.summit.util.Utils

class SeekBarBackgroundDrawable(private val drawable: Drawable) : Drawable() {
    private val height: Float = Utils.convertDpToPixel(16f)
    private val horizontalMargin = Utils.convertDpToPixel(8f)
    private val paint: Paint = Paint()
    private var path: Path = Path()
    private var bitmap: Bitmap? = null

    override fun draw(canvas: Canvas) {
        canvas.save()
        canvas.clipPath(path)

        val bitmap = bitmap
        if (bitmap != null) {
            canvas.drawBitmap(bitmap, horizontalMargin, 0f, paint)
        }

        canvas.restore()
    }

    override fun setAlpha(alpha: Int) {
        paint.alpha = alpha
    }

    override fun setColorFilter(colorFilter: ColorFilter?) {
    }

    override fun getOpacity(): Int {
        return PixelFormat.TRANSPARENT
    }

    override fun onBoundsChange(bounds: Rect) {
        super.onBoundsChange(bounds)

        val cornerRadius = Utils.convertDpToPixel(bounds.height().toFloat())

        path.reset()
        path.addRoundRect(
            bounds.left.toFloat() + horizontalMargin,
            (bounds.centerY() - height / 2),
            bounds.right.toFloat() - horizontalMargin,
            (bounds.centerY() + height / 2),
            cornerRadius,
            cornerRadius,
            Path.Direction.CW,
        )
        path.close()

        val bitmap = Bitmap.createBitmap(
            (bounds.width() - horizontalMargin * 2).toInt(),
            bounds.height(),
            Bitmap.Config.RGB_565
        )
        drawable.setBounds(0, 0, bounds.width(), bounds.height())
        drawable.draw(Canvas(bitmap))

        this.bitmap = bitmap
    }
}