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
import androidx.core.view.HapticFeedbackConstantsCompat
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.idunnololz.summit.R
import com.idunnololz.summit.api.dto.PostView
import com.idunnololz.summit.preferences.CommentGestureAction
import com.idunnololz.summit.preferences.GestureSwipeDirectionIds
import com.idunnololz.summit.util.Utils
import com.idunnololz.summit.util.ext.getColorCompat
import com.idunnololz.summit.util.ext.getDimen
import com.idunnololz.summit.util.ext.getDrawableCompat
import com.idunnololz.summit.util.ext.performHapticFeedbackCompat
import kotlin.math.abs

class LemmySwipeActionCallback(
    private val context: Context,
    val recyclerView: RecyclerView,
    val onActionSelected: (SwipeAction, ViewHolder) -> Unit,
    var gestureSize: Float,
    var hapticsEnabled: Boolean,
    var swipeDirection: Int,
) : ItemTouchHelper.Callback() {

    companion object {
        private const val TAG = "LemmySwipeActionCallback"
    }

    data class SwipeAction(
        val id: Int,
        val icon: Drawable,
        @ColorInt val color: Int,
    )

    private val noActionColor = context.getColorCompat(R.color.gray)
    private val clearPaint: Paint = Paint().apply {
        xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
    }
    private val background: ColorDrawable = ColorDrawable()
    private val marginEnd = context.getDimen(R.dimen.padding)
    private val disabledBackground: ColorDrawable = ColorDrawable().apply {
        color = context.getColorCompat(R.color.gray)
    }
    private var lastVhSwiped: ViewHolder? = null

    private var currentSwipeAction: SwipeAction? = null

    var postOnlyActions: List<SwipeAction>? = null
    var postOnlyGestureSize: Float = 0.5f
    var actions: List<SwipeAction> = listOf()

    private val expandDrawable = context.getDrawableCompat(R.drawable.baseline_unfold_more_24)!!

    private fun ViewHolder.isSwipeEnabled() =
        this.itemView.getTag(R.id.swipe_enabled) as? Boolean != false

    private fun ViewHolder.isSwipeable() = this.itemView.getTag(R.id.swipeable) as? Boolean == true

    override fun getMovementFlags(recyclerView: RecyclerView, viewHolder: ViewHolder): Int {
        return if (viewHolder.isSwipeable()) {
            when (swipeDirection) {
                GestureSwipeDirectionIds.RIGHT ->
                    makeMovementFlags(0, ItemTouchHelper.RIGHT)
                GestureSwipeDirectionIds.ANY ->
                    makeMovementFlags(
                        0,
                        ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT,
                    )
//                GestureSwipeDirectionIds.LEFT,
                else ->
                    makeMovementFlags(0, ItemTouchHelper.LEFT)
            }
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

    override fun getAnimationDuration(
        recyclerView: RecyclerView,
        animationType: Int,
        animateDx: Float,
        animateDy: Float,
    ): Long {
        // from the documentation:
        // Called by the ItemTouchHelper when user action finished on a ViewHolder and now the View
        // will be animated to its final position.
        //
        // Therefore we can use this call to know when the user has let go!

        Log.d(
            TAG,
            "Selected action: $currentSwipeAction animateDx: $animateDx animateDy: $animateDy",
        )

        val currentSwipeAction = currentSwipeAction
        val lastVhSwiped = lastVhSwiped

        if (currentSwipeAction != null && lastVhSwiped != null) {
            onActionSelected(currentSwipeAction, lastVhSwiped)

            this.currentSwipeAction = null
            this.lastVhSwiped = null
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

        var maxActionW = itemView.width * gestureSize
        var drawable: Drawable? = null

        val actions = if (viewHolder.itemView.tag is PostView && postOnlyActions != null) {
            maxActionW = itemView.width * postOnlyGestureSize
            postOnlyActions ?: actions
        } else {
            actions
        }

        val deadSpace = Utils.convertDpToPixel(24f)
        val usableSpace = maxActionW - deadSpace
        val actionSpace = usableSpace / actions.size

        val absDx = abs(dX)
        val currentSwipeAction = currentSwipeAction
        if (isCurrentlyActive || currentSwipeAction == null) {
            if (absDx < deadSpace) {
                background.color = noActionColor
//                drawable = actions.first().icon.apply {
//                    val iconTop = itemView.top + (itemHeight - intrinsicHeight) / 2
//                    val iconLeft = itemView.right - marginEnd - intrinsicWidth
//                    val iconRight = itemView.right - marginEnd
//                    val iconBottom = iconTop + intrinsicHeight
//                    this.alpha = alpha
//                    setBounds(iconLeft, iconTop, iconRight, iconBottom)
//                }
                this.currentSwipeAction = null
            } else {
                var thresholdX = deadSpace + actionSpace
                var index = 0
                while (index < actions.size) {
                    if (absDx < thresholdX || index == actions.lastIndex) {
                        val swipeAction = actions[index]
                        background.color = swipeAction.color
                        if (currentSwipeAction != swipeAction) {
                            if (hapticsEnabled) {
                                viewHolder.itemView.performHapticFeedbackCompat(
                                    HapticFeedbackConstantsCompat.CONFIRM,
                                )
                            }
                            this.currentSwipeAction = swipeAction
                        }

                        val icon = if (swipeAction.id == CommentGestureAction.CollapseOrExpand) {
                            if (viewHolder.itemView.getTag(R.id.expanded) == false) {
                                expandDrawable
                            } else {
                                swipeAction.icon
                            }
                        } else {
                            swipeAction.icon
                        }

                        drawable = icon.apply {
                            if (dX < 0) {
                                val iconTop = itemView.top + (itemHeight - intrinsicHeight) / 2
                                val iconLeft = itemView.right - marginEnd - intrinsicWidth
                                val iconRight = itemView.right - marginEnd
                                val iconBottom = iconTop + intrinsicHeight
                                setBounds(iconLeft, iconTop, iconRight, iconBottom)
                            } else {
                                val iconTop = itemView.top + (itemHeight - intrinsicHeight) / 2
                                val iconLeft = itemView.left + marginEnd
                                val iconRight = itemView.left + marginEnd + intrinsicWidth
                                val iconBottom = iconTop + intrinsicHeight
                                setBounds(iconLeft, iconTop, iconRight, iconBottom)
                            }
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
                if (dX < 0) {
                    val iconTop = itemView.top + (itemHeight - intrinsicHeight) / 2
                    val iconLeft = itemView.right - marginEnd - intrinsicWidth
                    val iconRight = itemView.right - marginEnd
                    val iconBottom = iconTop + intrinsicHeight
                    setBounds(iconLeft, iconTop, iconRight, iconBottom)
                } else {
                    val iconTop = itemView.top + (itemHeight - intrinsicHeight) / 2
                    val iconLeft = itemView.left + marginEnd
                    val iconRight = itemView.left + marginEnd + intrinsicWidth
                    val iconBottom = iconTop + intrinsicHeight
                    setBounds(iconLeft, iconTop, iconRight, iconBottom)
                }
                alpha = 255
            }
        }

        if (dX < 0) {
            background.setBounds(
                itemView.right + dX.toInt(),
                itemView.top,
                itemView.right,
                itemView.bottom,
            )
        } else {
            background.setBounds(
                itemView.left,
                itemView.top,
                itemView.left + dX.toInt(),
                itemView.bottom,
            )
        }
        background.draw(c)
        drawable?.draw(c)

        super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
    }

    override fun clearView(recyclerView: RecyclerView, viewHolder: ViewHolder) {
        super.clearView(recyclerView, viewHolder)
    }

    private fun clearCanvas(c: Canvas, left: Float, top: Float, right: Float, bottom: Float) {
        c.drawRect(left, top, right, bottom, clearPaint)
    }

    override fun getSwipeThreshold(viewHolder: ViewHolder): Float {
        return 1f
    }

    override fun getSwipeVelocityThreshold(defaultValue: Float): Float {
        return if (lastVhSwiped?.isSwipeEnabled() == false) {
            0f
        } else {
            5000f
        }
    }

    override fun onSwiped(viewHolder: ViewHolder, direction: Int) {}

    override fun getSwipeEscapeVelocity(defaultValue: Float): Float {
        return 10000f
    }
}
