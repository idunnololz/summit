package com.idunnololz.summit.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.drawable.ShapeDrawable
import android.graphics.drawable.shapes.RectShape
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import com.idunnololz.summit.R
import com.idunnololz.summit.util.Utils
import java.text.NumberFormat

data class StorageUsageItem(
    val label: String,
    val sizeInBytes: Long,
    val color: Int,
)

class StorageUsageView : ConstraintLayout {

    private val storageUsageTextView: TextView
    private val placeholder: View
    private val detailsContainer: ViewGroup

    private var storageUsageItems: List<StorageUsageItem> = listOf()

    private val nf = NumberFormat.getNumberInstance()
    private val pf = NumberFormat.getPercentInstance()

    private val tempRect = Rect()

    private val paint = Paint()
    private val borderPaint = Paint().apply {
        strokeWidth = Utils.convertDpToPixel(2f)
        color = ContextCompat.getColor(context, R.color.colorTextTitle)
        style = Paint.Style.STROKE
    }

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr,
    )

    init {
        View.inflate(context, R.layout.storage_usage_view, this)

        storageUsageTextView = findViewById(R.id.storageUsageTextView)
        placeholder = findViewById(R.id.placeholder)
        detailsContainer = findViewById(R.id.detailsContainer)

        setWillNotDraw(false)
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)

        canvas ?: return

        val offsetViewBounds = tempRect
        placeholder.getDrawingRect(offsetViewBounds)
        offsetDescendantRectToMyCoords(placeholder, offsetViewBounds)

        val y: Float = offsetViewBounds.top.toFloat()
        val totalSize = storageUsageItems.sumOf { it.sizeInBytes.toDouble() }
        var x: Float = offsetViewBounds.left.toFloat()

        canvas.drawRect(offsetViewBounds, borderPaint)
        storageUsageItems.forEach {
            val w: Float = (it.sizeInBytes / totalSize * offsetViewBounds.width()).toFloat()
            paint.color = it.color
            canvas.drawRect(
                x,
                y,
                x + w,
                y + offsetViewBounds.height(),
                paint,
            )

            x += w
        }
    }

    fun setStorageUsage(storageUsage: List<StorageUsageItem>) {
        storageUsageItems = storageUsage

        val totalBytes = storageUsage.sumByDouble { it.sizeInBytes.toDouble() }

        storageUsageTextView.text = Utils.fileSizeToHumanReadableString(totalBytes, nf)

        detailsContainer.removeAllViews()

        storageUsage.withIndex().forEach { (index, item) ->
            val drawable = ShapeDrawable(RectShape())
            drawable.paint.color = item.color
            drawable.intrinsicWidth = Utils.convertDpToPixel(16f).toInt()
            drawable.intrinsicHeight = Utils.convertDpToPixel(16f).toInt()

            val percent = if (totalBytes == 0.0) {
                0.0
            } else {
                item.sizeInBytes / totalBytes
            }

            val tv = TextView(context)
            tv.setTextColor(ContextCompat.getColor(context, R.color.colorTextTitle))
            tv.text = context.getString(
                R.string.storage_usage_legend_format,
                item.label,
                pf.format(percent),
            )
            tv.setCompoundDrawablesWithIntrinsicBounds(drawable, null, null, null)
            tv.compoundDrawablePadding =
                context.resources.getDimensionPixelOffset(R.dimen.padding_half)
            tv.textSize = 13f

            detailsContainer.addView(tv)

            if (index != storageUsage.size - 1) {
                tv.layoutParams = (tv.layoutParams as MarginLayoutParams).apply {
                    rightMargin = context.resources.getDimensionPixelOffset(R.dimen.padding)
                }
            }
        }

        invalidate()
    }
}
