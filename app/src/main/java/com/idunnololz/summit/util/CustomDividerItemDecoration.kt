package com.idunnololz.summit.util

import android.content.Context
import android.graphics.Canvas
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.util.Log
import android.view.View
import android.widget.LinearLayout
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ItemDecoration

/**
 * DividerItemDecoration is a [RecyclerView.ItemDecoration] that can be used as a divider
 * between items of a [LinearLayoutManager]. It supports both [.HORIZONTAL] and
 * [.VERTICAL] orientations.
 *
 * mDividerItemDecoration = new DividerItemDecoration(recyclerView.getContext(),
 * mLayoutManager.getOrientation());
 * recyclerView.addItemDecoration(mDividerItemDecoration);
 *
 * Creates a divider [RecyclerView.ItemDecoration] that can be used with a
 * [LinearLayoutManager].
 *
 * @param context Current context, it will be used to access resources.
 * @param orientation Divider orientation. Should be [.HORIZONTAL] or [.VERTICAL].
 */
class CustomDividerItemDecoration(
    val context: Context,
    orientation: Int,
    val dividerAfterLastItem: Boolean = false,
    val drawDividerAfter: ((Int) -> Boolean)? = null,
) : ItemDecoration() {
    /**
     * @return the [Drawable] for this divider.
     */
    var drawable: Drawable? = null
        private set

    /**
     * Current orientation. Either [.HORIZONTAL] or [.VERTICAL].
     */
    private val mBounds = Rect()

    private var orientation: Int = orientation
        set(value) {
            require(!(orientation != HORIZONTAL && orientation != VERTICAL)) {
                "Invalid orientation. It should be either HORIZONTAL or VERTICAL"
            }
            field = value
        }

    init {
        val a = context.obtainStyledAttributes(ATTRS)
        drawable = a.getDrawable(0)
        if (drawable == null) {
            Log.w(
                TAG,
                "@android:attr/listDivider was not set in the theme used for this " +
                    "DividerItemDecoration. Please set that attribute all call setDrawable()",
            )
        }
        a.recycle()
    }

    /**
     * Sets the [Drawable] for this divider.
     *
     * @param drawable Drawable that should be used as a divider.
     */
    fun setDrawable(drawable: Drawable) {
        requireNotNull(drawable) { "Drawable cannot be null." }
        this.drawable = drawable
    }

    override fun onDraw(c: Canvas, parent: RecyclerView, state: RecyclerView.State) {
        if (parent.layoutManager == null || drawable == null) {
            return
        }
        if (orientation == VERTICAL) {
            drawVertical(c, parent)
        } else {
            drawHorizontal(c, parent)
        }
    }

    private fun drawVertical(canvas: Canvas, parent: RecyclerView) {
        canvas.save()
        val left: Int
        val right: Int
        if (parent.clipToPadding) {
            left = parent.paddingLeft
            right = parent.width - parent.paddingRight
            canvas.clipRect(
                left,
                parent.paddingTop,
                right,
                parent.height - parent.paddingBottom,
            )
        } else {
            left = 0
            right = parent.width
        }
        val childCount = parent.childCount
        for (i in 0 until childCount) {
            if (i == childCount - 1 && !dividerAfterLastItem) {
                continue
            }
            if (drawDividerAfter != null && !drawDividerAfter.invoke(i)) {
                continue
            }

            val child = parent.getChildAt(i)
            parent.getDecoratedBoundsWithMargins(child, mBounds)
            val bottom = mBounds.bottom + Math.round(child.translationY)
            val top = bottom - drawable!!.intrinsicHeight
            drawable!!.setBounds(left, top, right, bottom)
            drawable!!.draw(canvas)
        }
        canvas.restore()
    }

    private fun drawHorizontal(canvas: Canvas, parent: RecyclerView) {
        canvas.save()
        val top: Int
        val bottom: Int
        if (parent.clipToPadding) {
            top = parent.paddingTop
            bottom = parent.height - parent.paddingBottom
            canvas.clipRect(
                parent.paddingLeft,
                top,
                parent.width - parent.paddingRight,
                bottom,
            )
        } else {
            top = 0
            bottom = parent.height
        }
        val childCount = parent.childCount
        for (i in 0 until childCount) {
            if (i == childCount - 1 && !dividerAfterLastItem) {
                continue
            }
            if (drawDividerAfter != null && !drawDividerAfter.invoke(i)) {
                continue
            }

            val child = parent.getChildAt(i)
            parent.layoutManager!!.getDecoratedBoundsWithMargins(child, mBounds)
            val right = mBounds.right + Math.round(child.translationX)
            val left = right - drawable!!.intrinsicWidth
            drawable!!.setBounds(left, top, right, bottom)
            drawable!!.draw(canvas)
        }
        canvas.restore()
    }

    override fun getItemOffsets(
        outRect: Rect,
        view: View,
        parent: RecyclerView,
        state: RecyclerView.State,
    ) {
        if (drawable == null) {
            outRect[0, 0, 0] = 0
            return
        }
        if (orientation == VERTICAL) {
            outRect[0, 0, 0] = drawable!!.intrinsicHeight
        } else {
            outRect[0, 0, drawable!!.intrinsicWidth] = 0
        }
    }

    companion object {
        const val HORIZONTAL = LinearLayout.HORIZONTAL
        const val VERTICAL = LinearLayout.VERTICAL
        private const val TAG = "DividerItem"
        private val ATTRS = intArrayOf(android.R.attr.listDivider)
    }
}
