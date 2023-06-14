package com.idunnololz.summit.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.util.AttributeSet
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.idunnololz.summit.R
import com.idunnololz.summit.util.ext.getColorCompat

class GalleryPageIndicator : View {

    private val tempRect = Rect()

    private val bgPaint = Paint().apply {
        color = context.getColorCompat(R.color.gray)
    }

    private val indicatorPaint = Paint().apply {
        color = context.getColorCompat(R.color.colorPrimary)
    }

    private var numSegs = 0
    private var curSeg = -1

    /**
     * The position of the indicator relative to the number of segments.
     * Eg. if there are 2 segments then a position of 0.5 indicates that the indicator is halfway
     * between page 0 and 1
     */
    private var indicatorPosition = 0.0

    private var currentRecyclerView: RecyclerView? = null
    private var currentOnScrollListener: RecyclerView.OnScrollListener? = null

    constructor(context: Context?) : super(context)
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    )

    fun setup(recyclerView: RecyclerView) {
        numSegs = recyclerView.adapter?.itemCount ?: 0

        if (currentRecyclerView != recyclerView) {
            currentOnScrollListener?.let {
                currentRecyclerView?.removeOnScrollListener(it)
            }

            currentRecyclerView = recyclerView
            val currentOnScrollListener = object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    super.onScrolled(recyclerView, dx, dy)

                    val offset = recyclerView.computeHorizontalScrollOffset()
                    val itemWidth = recyclerView.width

                    indicatorPosition = offset / itemWidth.toDouble()

                    invalidate()
                }
            }.also {
                currentOnScrollListener = it
            }

            recyclerView.addOnScrollListener(currentOnScrollListener)
        }
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)

        val w = width
        val h = height

        tempRect.set(0, 0, w, h)

        canvas?.drawRect(tempRect, bgPaint)

        if (numSegs > 0 && indicatorPosition >= 0 && indicatorPosition <= numSegs) {
            val segW = w / numSegs
            val x = segW * indicatorPosition

            tempRect.set(x.toInt(), 0, (segW + x).toInt(), h)

            canvas?.drawRect(tempRect, indicatorPaint)
        }
    }
}