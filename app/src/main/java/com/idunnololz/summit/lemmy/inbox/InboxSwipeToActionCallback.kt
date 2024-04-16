package com.idunnololz.summit.lemmy.inbox

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.idunnololz.summit.R
import com.idunnololz.summit.util.ext.getColorCompat
import com.idunnololz.summit.util.ext.getDimen
import com.idunnololz.summit.util.ext.getDrawableCompat

class InboxSwipeToActionCallback(
    private val context: Context,
    @ColorInt val color: Int,
    @DrawableRes val icon: Int,
    val recyclerView: RecyclerView,
    val onSwipe: (viewHolder: ViewHolder, direction: Int) -> Unit,
) : ItemTouchHelper.Callback() {

    private val clearPaint: Paint = Paint()
    private val background: ColorDrawable = ColorDrawable()
    private val backgroundColor: Int = color
    private val deleteDrawable: Drawable?
    private val intrinsicWidth: Int
    private val intrinsicHeight: Int
    private val marginEnd = context.getDimen(R.dimen.dialog_padding)
    private val disabledBackground: ColorDrawable = ColorDrawable().apply {
        color = context.getColorCompat(R.color.gray)
    }
    private var lastVhSwiped: ViewHolder? = null

    init {
        clearPaint.xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
        deleteDrawable = context.getDrawableCompat(icon)
        intrinsicWidth = deleteDrawable!!.intrinsicWidth
        intrinsicHeight = deleteDrawable.intrinsicHeight
    }

    fun ViewHolder.isSwipeEnabled() = this.itemView.getTag(R.id.swipe_enabled) as? Boolean != false

    override fun getMovementFlags(recyclerView: RecyclerView, viewHolder: ViewHolder): Int {
        return if (viewHolder.isSwipeEnabled()) {
            makeMovementFlags(0, ItemTouchHelper.LEFT)
        } else {
            0
        }
    }

    override fun onMove(
        recyclerView: RecyclerView,
        viewHolder: ViewHolder,
        viewHolder1: ViewHolder,
    ): Boolean {
        return false
    }

    override fun onChildDraw(
        c: Canvas,
        recyclerView: RecyclerView,
        viewHolder: ViewHolder,
        dX: Float,
        dY: Float,
        actionState: Int,
        isCurrentlyActive: Boolean,
    ) {
        val itemView = viewHolder.itemView
        val itemHeight = itemView.height

        if (isCurrentlyActive) {
            lastVhSwiped = viewHolder
        }

        if (!viewHolder.isSwipeEnabled()) {
            val finalDx = dX / 4
            disabledBackground.setBounds(
                itemView.right + finalDx.toInt(),
                itemView.top,
                itemView.right,
                itemView.bottom,
            )
            disabledBackground.draw(c)
            super.onChildDraw(
                c,
                recyclerView,
                viewHolder,
                finalDx,
                dY,
                actionState,
                isCurrentlyActive,
            )
            return
        }

        val isCancelled = dX == 0f && !isCurrentlyActive
        if (isCancelled) {
            clearCanvas(
                c,
                itemView.right + dX,
                itemView.top.toFloat(),
                itemView.right.toFloat(),
                itemView.bottom.toFloat(),
            )
            super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
            return
        }

        background.color = backgroundColor
        background.setBounds(
            itemView.right + dX.toInt(),
            itemView.top,
            itemView.right,
            itemView.bottom,
        )
        background.draw(c)
        val deleteIconTop = itemView.top + (itemHeight - intrinsicHeight) / 2
        val deleteIconLeft = itemView.right - marginEnd - intrinsicWidth
        val deleteIconRight = itemView.right - marginEnd
        val deleteIconBottom = deleteIconTop + intrinsicHeight
        deleteDrawable!!.setBounds(deleteIconLeft, deleteIconTop, deleteIconRight, deleteIconBottom)
        deleteDrawable.draw(c)
        super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
    }

    private fun clearCanvas(c: Canvas, left: Float, top: Float, right: Float, bottom: Float) {
        c.drawRect(left, top, right, bottom, clearPaint)
    }

    override fun getSwipeThreshold(viewHolder: ViewHolder): Float {
        return if (viewHolder.isSwipeEnabled()) {
            0.5f
        } else {
            1f
        }
    }

    override fun getSwipeVelocityThreshold(defaultValue: Float): Float {
        return if (lastVhSwiped?.isSwipeEnabled() == false) {
            0f
        } else {
            5000f
        }
    }

    override fun getSwipeEscapeVelocity(defaultValue: Float): Float {
        return 1f
    }

    override fun onSwiped(viewHolder: ViewHolder, direction: Int) {
        onSwipe(viewHolder, direction)
    }
}
