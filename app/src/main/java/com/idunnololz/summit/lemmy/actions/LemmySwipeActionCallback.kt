package com.idunnololz.summit.lemmy.actions

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.util.Log
import androidx.annotation.ColorInt
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.idunnololz.summit.R
import com.idunnololz.summit.util.Utils
import com.idunnololz.summit.util.ext.getColorCompat
import com.idunnololz.summit.util.ext.getDimen
import com.idunnololz.summit.util.ext.getDrawableCompat

class LemmySwipeActionCallback(
    private val context: Context,
    val recyclerView: RecyclerView,
    val onActionSelected: (SwipeAction, ViewHolder) -> Unit,
) : ItemTouchHelper.Callback() {

    companion object {
        private const val TAG = "LemmySwipeActionCallback"
    }

    data class SwipeAction(
        val id: Int,
        val icon: Drawable,
        @ColorInt val color: Int
    )

    private val clearPaint: Paint = Paint()
    private val background: ColorDrawable = ColorDrawable()
    private val marginEnd = context.getDimen(R.dimen.padding)
    private val disabledBackground: ColorDrawable = ColorDrawable().apply {
        color = context.getColorCompat(R.color.gray)
    }
    private var lastVhSwiped: ViewHolder? = null

    private var currentSwipeAction: SwipeAction? = null

    private val actions = listOf(
        SwipeAction(
            R.id.swipe_action_upvote,
            context.getDrawableCompat(R.drawable.baseline_arrow_upward_24)!!.mutate(),
            context.getColorCompat(R.color.style_red)
        ),
        SwipeAction(
            R.id.swipe_action_bookmark,
            context.getDrawableCompat(R.drawable.baseline_bookmark_add_24)!!.mutate(),
            context.getColorCompat(R.color.style_amber)
        ),
        SwipeAction(
            R.id.swipe_action_reply,
            context.getDrawableCompat(R.drawable.baseline_reply_24)!!.mutate(),
            context.getColorCompat(R.color.style_blue)
        )
    )

    init {
        clearPaint.xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
    }

    fun ViewHolder.isSwipeEnabled() =
        this.itemView.getTag(R.id.swipe_enabled) as? Boolean != false

    fun ViewHolder.isSwipeable() =
        this.itemView.getTag(R.id.swipeable) as? Boolean == true


    override fun getMovementFlags(
        recyclerView: RecyclerView,
        viewHolder: ViewHolder
    ): Int {
        if (viewHolder.isSwipeable()) {
            return makeMovementFlags(0, ItemTouchHelper.LEFT)
        } else {
            return 0
        }
    }

    override fun onMove(
        recyclerView: RecyclerView,
        viewHolder: ViewHolder,
        viewHolder1: ViewHolder
    ): Boolean {
        return false
    }

    override fun getAnimationDuration(
        recyclerView: RecyclerView,
        animationType: Int,
        animateDx: Float,
        animateDy: Float
    ): Long {
        // from the documentation:
        // Called by the ItemTouchHelper when user action finished on a ViewHolder and now the View
        // will be animated to its final position.
        //
        // Therefore we can use this call to know when the user has let go!

        Log.d(TAG, "Selected action: $currentSwipeAction")

        val currentSwipeAction = currentSwipeAction
        val lastVhSwiped = lastVhSwiped

        if ( currentSwipeAction != null && lastVhSwiped != null) {
            onActionSelected(currentSwipeAction, lastVhSwiped)
        }


        return super.getAnimationDuration(recyclerView, animationType, animateDx, animateDy)
    }

    override fun onChildDraw(
        c: Canvas,
        recyclerView: RecyclerView,
        viewHolder: ViewHolder,
        dX: Float,
        dY: Float,
        actionState: Int,
        isCurrentlyActive: Boolean
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
                itemView.bottom
            )
            disabledBackground.draw(c)
            super.onChildDraw(c, recyclerView, viewHolder, finalDx, dY, actionState, isCurrentlyActive)
            return
        }

        val isCancelled = dX == 0f && !isCurrentlyActive
        if (isCancelled) {
            clearCanvas(
                c,
                itemView.right + dX,
                itemView.top.toFloat(),
                itemView.right.toFloat(),
                itemView.bottom.toFloat()
            )
            super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
            return
        }

        val maxActionW = itemView.width / 2
        val deadSpace = Utils.convertDpToPixel(48f)
        val usableSpace = maxActionW - deadSpace
        val actionSpace = usableSpace / actions.size

        var drawable: Drawable? = null

        val negDx = -dX
        val currentSwipeAction = currentSwipeAction
        if (isCurrentlyActive || currentSwipeAction == null) {
            if (negDx < deadSpace) {
                val alpha = ((1f - (deadSpace - negDx) / (deadSpace)) * 255).toInt()
                background.alpha = alpha
                background.color = actions.first().color
                drawable = actions.first().icon.apply {
                    val iconTop = itemView.top + (itemHeight - intrinsicHeight) / 2
                    val iconLeft = itemView.right - marginEnd - intrinsicWidth
                    val iconRight = itemView.right - marginEnd
                    val iconBottom = iconTop + intrinsicHeight
                    this.alpha = alpha
                    setBounds(iconLeft, iconTop, iconRight, iconBottom)
                }
                this.currentSwipeAction = null
            } else {
                var thresholdX = deadSpace + actionSpace
                var index = 0
                while (index < actions.size) {
                    if (negDx < thresholdX || index == actions.lastIndex) {
                        val swipeAction = actions[index]
                        background.color = swipeAction.color
                        this.currentSwipeAction = swipeAction

                        drawable = swipeAction.icon.apply {
                            val iconTop = itemView.top + (itemHeight - intrinsicHeight) / 2
                            val iconLeft = itemView.right - marginEnd - intrinsicWidth
                            val iconRight = itemView.right - marginEnd
                            val iconBottom = iconTop + intrinsicHeight
                            setBounds(iconLeft, iconTop, iconRight, iconBottom)
                            alpha = 255
                        }
                        break
                    }
                    thresholdX += actionSpace
                    index++
                }
            }
        } else {
            background.color = currentSwipeAction.color
            drawable = currentSwipeAction.icon.apply {
                val iconTop = itemView.top + (itemHeight - intrinsicHeight) / 2
                val iconLeft = itemView.right - marginEnd - intrinsicWidth
                val iconRight = itemView.right - marginEnd
                val iconBottom = iconTop + intrinsicHeight
                setBounds(iconLeft, iconTop, iconRight, iconBottom)
                alpha = 255
            }
        }

        background.setBounds(
            itemView.right + dX.toInt(),
            itemView.top,
            itemView.right,
            itemView.bottom
        )
        background.draw(c)
        drawable?.draw(c)

        super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
    }

    override fun clearView(recyclerView: RecyclerView, viewHolder: ViewHolder) {
        super.clearView(recyclerView, viewHolder)
        Log.d("HAHA", "clearView")
    }

    private fun clearCanvas(c: Canvas, left: Float, top: Float, right: Float, bottom: Float) {
        c.drawRect(left, top, right, bottom, clearPaint)
    }

    override fun getSwipeThreshold(viewHolder: ViewHolder): Float {
        return 1f
    }

    override fun getSwipeVelocityThreshold(defaultValue: Float): Float {
        return if (lastVhSwiped?.isSwipeEnabled() == false) {
            Log.d("HAHA", "getSwipeVelocityThreshold() 0f")
            0f
        } else {
            Log.d("HAHA", "getSwipeVelocityThreshold() $defaultValue")
            5000f
        }
    }

    override fun onSwiped(viewHolder: ViewHolder, direction: Int) {}

    override fun getSwipeEscapeVelocity(defaultValue: Float): Float {
        return 10000f
    }
}