package com.idunnololz.summit.util

import android.util.DisplayMetrics
import android.view.Gravity
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.widget.Scroller
import androidx.annotation.Px
import androidx.core.text.TextUtilsCompat
import androidx.core.view.ViewCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearSmoothScroller
import androidx.recyclerview.widget.LinearSnapHelper
import androidx.recyclerview.widget.OrientationHelper
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.SmoothScroller
import androidx.recyclerview.widget.RecyclerView.SmoothScroller.ScrollVectorProvider
import java.util.Locale
import kotlin.math.abs
import kotlin.math.max

/**
 * A [LinearSnapHelper] that allows snapping to an edge or to the center.
 *
 *
 * Possible snap positions:
 * [Gravity.START], [Gravity.TOP], [Gravity.END], [Gravity.BOTTOM],
 * [Gravity.CENTER].
 *
 *
 * To customize the scroll duration, use [GravitySnapHelper.scrollMsPerInch].
 *
 *
 * To customize the maximum scroll distance during flings,
 * use [GravitySnapHelper.setMaxFlingSizeFraction]
 * or [GravitySnapHelper.setMaxFlingDistance]
 */
class GravitySnapHelper @JvmOverloads constructor(
    gravity: Int,
    enableSnapLastItem: Boolean = false,
    snapListener: SnapListener? = null,
) :
    LinearSnapHelper() {
    var snapPadding: Int = 0

    private var gravity: Int
    private var isRtl = false
    /**
     * @return true if this SnapHelper should snap to the last item
     */
    /**
     * Enable snapping of the last item that's snappable.
     * The default value is false, because you can't see the last item completely
     * if this is enabled.
     */
    var snapLastItem: Boolean
    private var nextSnapPosition = 0
    private var isScrolling = false
    /**
     * @return true if this SnapHelper should snap to the padding. Defaults to false.
     */
    /**
     * If true, GravitySnapHelper will snap to the gravity edge
     * plus any amount of padding that was set in the RecyclerView.
     *
     *
     * The default value is false.
     *
     * @param snapToPadding true if you want to snap to the padding
     */
    var snapToPadding: Boolean = false
    /**
     * @return last scroll speed set through [GravitySnapHelper.scrollMsPerInch]
     * or 100f
     */
    /**
     * Sets the scroll duration in ms per inch.
     *
     *
     * Default value is 100.0f
     *
     *
     * This value will be used in
     * [GravitySnapHelper.createScroller]
     *
     * @param ms scroll duration in ms per inch
     */
    var scrollMsPerInch: Float = 100f
    private var maxFlingDistance = FLING_DISTANCE_DISABLE
    private var maxFlingSizeFraction = FLING_SIZE_FRACTION_DISABLE
    private var verticalHelper: OrientationHelper? = null
    private var horizontalHelper: OrientationHelper? = null
    private var listener: SnapListener?
    private var recyclerView: RecyclerView? = null
    private val scrollListener: RecyclerView.OnScrollListener =
        object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)
                this@GravitySnapHelper.onScrollStateChanged(newState)
            }
        }

    constructor(gravity: Int, snapListener: SnapListener) : this(gravity, false, snapListener)

    init {
        require(
            !(gravity != Gravity.START && gravity != Gravity.END && gravity != Gravity.BOTTOM && gravity != Gravity.TOP && gravity != Gravity.CENTER),
        ) {
            "Invalid gravity value. Use START " +
                "| END | BOTTOM | TOP | CENTER constants"
        }
        this.snapLastItem = enableSnapLastItem
        this.gravity = gravity
        this.listener = snapListener
    }

    @Throws(IllegalStateException::class)
    override fun attachToRecyclerView(recyclerView: RecyclerView?) {
        if (this.recyclerView != null) {
            this.recyclerView!!.removeOnScrollListener(scrollListener)
        }
        if (recyclerView != null) {
            recyclerView.onFlingListener = null
            if (gravity == Gravity.START || gravity == Gravity.END) {
                isRtl = (
                    TextUtilsCompat.getLayoutDirectionFromLocale(Locale.getDefault())
                        == ViewCompat.LAYOUT_DIRECTION_RTL
                    )
            }
            recyclerView.addOnScrollListener(scrollListener)
            this.recyclerView = recyclerView
        } else {
            this.recyclerView = null
        }
        super.attachToRecyclerView(recyclerView)
    }

    override fun findSnapView(lm: RecyclerView.LayoutManager): View? {
        return findSnapView(lm, true)
    }

    fun findSnapView(lm: RecyclerView.LayoutManager, checkEdgeOfList: Boolean): View? {
        var snapView: View? = null

        when (gravity) {
            Gravity.START -> snapView = findView(
                lm,
                getHorizontalHelper(lm)!!, Gravity.START, checkEdgeOfList,
            )

            Gravity.END -> snapView = findView(
                lm,
                getHorizontalHelper(lm)!!, Gravity.END, checkEdgeOfList,
            )

            Gravity.TOP -> snapView = findView(
                lm,
                getVerticalHelper(lm)!!, Gravity.START, checkEdgeOfList,
            )

            Gravity.BOTTOM -> snapView = findView(
                lm,
                getVerticalHelper(lm)!!, Gravity.END, checkEdgeOfList,
            )

            Gravity.CENTER -> snapView = if (lm.canScrollHorizontally()) {
                findView(
                    lm, getHorizontalHelper(lm)!!, Gravity.CENTER,
                    checkEdgeOfList,
                )
            } else {
                findView(
                    lm, getVerticalHelper(lm)!!, Gravity.CENTER,
                    checkEdgeOfList,
                )
            }
        }
        nextSnapPosition = if (snapView != null) {
            recyclerView!!.getChildAdapterPosition(snapView)
        } else {
            RecyclerView.NO_POSITION
        }
        return snapView
    }

    override fun calculateDistanceToFinalSnap(
        layoutManager: RecyclerView.LayoutManager,
        targetView: View,
    ): IntArray {
        if (gravity == Gravity.CENTER) {
            return super.calculateDistanceToFinalSnap(layoutManager, targetView)!!
        }

        val out = IntArray(2)

        if (layoutManager !is LinearLayoutManager) {
            return out
        }

        val lm = layoutManager

        if (lm.canScrollHorizontally()) {
            if ((isRtl && gravity == Gravity.END) || (!isRtl && gravity == Gravity.START)) {
                out[0] = getDistanceToStart(targetView, getHorizontalHelper(lm)!!) + snapPadding
            } else {
                out[0] = getDistanceToEnd(targetView, getHorizontalHelper(lm)!!) - snapPadding
            }
        } else if (lm.canScrollVertically()) {
            if (gravity == Gravity.TOP) {
                out[1] = getDistanceToStart(targetView, getVerticalHelper(lm)!!) + snapPadding
            } else {
                out[1] = getDistanceToEnd(targetView, getVerticalHelper(lm)!!) - snapPadding
            }
        }
        return out
    }

    override fun calculateScrollDistance(velocityX: Int, velocityY: Int): IntArray {
        if (recyclerView == null || (verticalHelper == null && horizontalHelper == null) ||
            (
                maxFlingDistance == FLING_DISTANCE_DISABLE &&
                    maxFlingSizeFraction == FLING_SIZE_FRACTION_DISABLE
                )
        ) {
            return super.calculateScrollDistance(velocityX, velocityY)
        }
        val out = IntArray(2)
        val scroller = Scroller(
            recyclerView!!.context,
            DecelerateInterpolator(),
        )
        val maxDistance = flingDistance
        scroller.fling(
            0,
            0,
            velocityX,
            velocityY,
            -maxDistance,
            maxDistance,
            -maxDistance,
            maxDistance,
        )
        out[0] = scroller.finalX
        out[1] = scroller.finalY
        return out
    }

    public override fun createScroller(layoutManager: RecyclerView.LayoutManager): SmoothScroller? {
        if (layoutManager !is ScrollVectorProvider || recyclerView == null) {
            return null
        }
        return object : LinearSmoothScroller(recyclerView!!.context) {
            override fun onTargetFound(
                targetView: View,
                state: RecyclerView.State,
                action: Action,
            ) {
                if (recyclerView == null || recyclerView!!.layoutManager == null) {
                    // The associated RecyclerView has been removed so there is no action to take.
                    return
                }
                val snapDistances = calculateDistanceToFinalSnap(
                    recyclerView!!.layoutManager!!,
                    targetView,
                )
                val dx = snapDistances[0]
                val dy = snapDistances[1]
                val time = calculateTimeForDeceleration(
                    max(abs(dx.toDouble()), abs(dy.toDouble()))
                        .toInt(),
                )
                if (time > 0) {
                    action.update(dx, dy, time, mDecelerateInterpolator)
                }
            }

            override fun calculateSpeedPerPixel(displayMetrics: DisplayMetrics): Float {
                return scrollMsPerInch / displayMetrics.densityDpi
            }
        }
    }

    /**
     * Sets a [SnapListener] to listen for snap events
     *
     * @param listener a [SnapListener] that'll receive snap events or null to clear it
     */
    fun setSnapListener(listener: SnapListener?) {
        this.listener = listener
    }

    /**
     * Changes the gravity of this [GravitySnapHelper]
     * and dispatches a smooth scroll for the new snap position.
     *
     * @param newGravity one of the following: [Gravity.START], [Gravity.TOP],
     * [Gravity.END], [Gravity.BOTTOM], [Gravity.CENTER]
     * @param smooth     true if we should smooth scroll to new edge, false otherwise
     */
    fun setGravity(newGravity: Int, smooth: Boolean) {
        if (this.gravity != newGravity) {
            this.gravity = newGravity
            updateSnap(smooth, false)
        }
    }

    /**
     * Updates the current view to be snapped
     *
     * @param smooth          true if we should smooth scroll, false otherwise
     * @param checkEdgeOfList true if we should check if we're at an edge of the list
     * and snap according to [GravitySnapHelper.snapLastItem],
     * or false to force snapping to the nearest view
     */
    fun updateSnap(smooth: Boolean, checkEdgeOfList: Boolean) {
        if (recyclerView == null || recyclerView!!.layoutManager == null) {
            return
        }
        val lm = recyclerView!!.layoutManager
        val snapView = findSnapView(lm!!, checkEdgeOfList)
        if (snapView != null) {
            val out = calculateDistanceToFinalSnap(lm, snapView)
            if (smooth) {
                recyclerView!!.smoothScrollBy(out[0], out[1])
            } else {
                recyclerView!!.scrollBy(out[0], out[1])
            }
        }
    }

    /**
     * This method will only work if there's a ViewHolder for the given position.
     *
     * @return true if scroll was successful, false otherwise
     */
    fun scrollToPosition(position: Int): Boolean {
        if (position == RecyclerView.NO_POSITION) {
            return false
        }
        return scrollTo(position, false)
    }

    /**
     * Unlike [GravitySnapHelper.scrollToPosition],
     * this method will generally always find a snap view if the position is valid.
     *
     *
     * The smooth scroller from [GravitySnapHelper.createScroller]
     * will be used, and so will [GravitySnapHelper.scrollMsPerInch] for the scroll velocity
     *
     * @return true if scroll was successful, false otherwise
     */
    fun smoothScrollToPosition(position: Int): Boolean {
        if (position == RecyclerView.NO_POSITION) {
            return false
        }
        return scrollTo(position, true)
    }

    /**
     * Get the current gravity being applied
     *
     * @return one of the following: [Gravity.START], [Gravity.TOP], [Gravity.END],
     * [Gravity.BOTTOM], [Gravity.CENTER]
     */
    fun getGravity(): Int {
        return this.gravity
    }

    /**
     * Changes the gravity of this [GravitySnapHelper]
     * and dispatches a smooth scroll for the new snap position.
     *
     * @param newGravity one of the following: [Gravity.START], [Gravity.TOP],
     * [Gravity.END], [Gravity.BOTTOM], [Gravity.CENTER]
     */
    fun setGravity(newGravity: Int) {
        setGravity(newGravity, true)
    }

    /**
     * @return last distance set through [GravitySnapHelper.setMaxFlingDistance]
     * or [GravitySnapHelper.FLING_DISTANCE_DISABLE] if we're not limiting the fling distance
     */
    fun getMaxFlingDistance(): Int {
        return maxFlingDistance
    }

    /**
     * Changes the max fling distance in absolute values.
     *
     * @param distance max fling distance in pixels
     * or [GravitySnapHelper.FLING_DISTANCE_DISABLE]
     * to disable fling limits
     */
    fun setMaxFlingDistance(@Px distance: Int) {
        maxFlingDistance = distance
        maxFlingSizeFraction = FLING_SIZE_FRACTION_DISABLE
    }

    /**
     * @return last distance set through [GravitySnapHelper.setMaxFlingSizeFraction]
     * or [GravitySnapHelper.FLING_SIZE_FRACTION_DISABLE]
     * if we're not limiting the fling distance
     */
    fun getMaxFlingSizeFraction(): Float {
        return maxFlingSizeFraction
    }

    /**
     * Changes the max fling distance depending on the available size of the RecyclerView.
     *
     *
     * Example: if you pass 0.5f and the RecyclerView measures 600dp,
     * the max fling distance will be 300dp.
     *
     * @param fraction size fraction to be used for the max fling distance
     * or [GravitySnapHelper.FLING_SIZE_FRACTION_DISABLE]
     * to disable fling limits
     */
    fun setMaxFlingSizeFraction(fraction: Float) {
        maxFlingDistance = FLING_DISTANCE_DISABLE
        maxFlingSizeFraction = fraction
    }

    val currentSnappedPosition: Int
        /**
         * @return the position of the current view that's snapped
         * or [RecyclerView.NO_POSITION] in case there's none.
         */
        get() {
            if (recyclerView != null && recyclerView!!.layoutManager != null) {
                val snappedView = findSnapView(recyclerView!!.layoutManager!!)
                if (snappedView != null) {
                    return recyclerView!!.getChildAdapterPosition(snappedView)
                }
            }
            return RecyclerView.NO_POSITION
        }

    private val flingDistance: Int
        get() = if (maxFlingSizeFraction != FLING_SIZE_FRACTION_DISABLE) {
            if (verticalHelper != null) {
                (recyclerView!!.height * maxFlingSizeFraction).toInt()
            } else if (horizontalHelper != null) {
                (recyclerView!!.width * maxFlingSizeFraction).toInt()
            } else {
                Int.MAX_VALUE
            }
        } else if (maxFlingDistance != FLING_DISTANCE_DISABLE) {
            maxFlingDistance
        } else {
            Int.MAX_VALUE
        }

    /**
     * @return true if the scroll will snap to a view, false otherwise
     */
    private fun scrollTo(position: Int, smooth: Boolean): Boolean {
        if (recyclerView!!.layoutManager != null) {
            if (smooth) {
                val smoothScroller = createScroller(recyclerView!!.layoutManager!!)
                if (smoothScroller != null) {
                    smoothScroller.targetPosition = position
                    recyclerView!!.layoutManager!!.startSmoothScroll(smoothScroller)
                    return true
                }
            } else {
                val viewHolder = recyclerView!!.findViewHolderForAdapterPosition(position)
                if (viewHolder != null) {
                    val distances = calculateDistanceToFinalSnap(
                        recyclerView!!.layoutManager!!,
                        viewHolder.itemView,
                    )
                    recyclerView!!.scrollBy(distances[0], distances[1])
                    return true
                }
            }
        }
        return false
    }

    private fun getDistanceToStart(targetView: View, helper: OrientationHelper): Int {
        val distance: Int
        // If we don't care about padding, just snap to the start of the view
        if (!snapToPadding) {
            val childStart = helper.getDecoratedStart(targetView)
            distance = if (childStart >= helper.startAfterPadding / 2) {
                childStart - helper.startAfterPadding
            } else {
                childStart
            }
        } else {
            distance = helper.getDecoratedStart(targetView) - helper.startAfterPadding
        }
        return distance
    }

    private fun getDistanceToEnd(targetView: View, helper: OrientationHelper): Int {
        val distance: Int

        if (!snapToPadding) {
            val childEnd = helper.getDecoratedEnd(targetView)
            distance = if (childEnd >= helper.end - (helper.end - helper.endAfterPadding) / 2) {
                helper.getDecoratedEnd(targetView) - helper.end
            } else {
                childEnd - helper.endAfterPadding
            }
        } else {
            distance = helper.getDecoratedEnd(targetView) - helper.endAfterPadding
        }

        return distance
    }

    /**
     * Returns the first view that we should snap to.
     *
     * @param layoutManager the RecyclerView's LayoutManager
     * @param helper        orientation helper to calculate view sizes
     * @param gravity       gravity to find the closest view
     * @return the first view in the LayoutManager to snap to, or null if we shouldn't snap to any
     */
    private fun findView(
        layoutManager: RecyclerView.LayoutManager,
        helper: OrientationHelper,
        gravity: Int,
        checkEdgeOfList: Boolean,
    ): View? {
        if (layoutManager.childCount == 0 || layoutManager !is LinearLayoutManager) {
            return null
        }

        val lm = layoutManager

        // If we're at an edge of the list, we shouldn't snap
        // to avoid having the last item not completely visible.
        if (checkEdgeOfList && (isAtEdgeOfList(lm) && !snapLastItem)) {
            return null
        }

        var edgeView: View? = null
        var distanceToTarget = Int.MAX_VALUE
        val center = if (layoutManager.getClipToPadding()) {
            helper.startAfterPadding + helper.totalSpace / 2
        } else {
            helper.end / 2
        }

        val snapToStart = (gravity == Gravity.START && !isRtl) ||
            (gravity == Gravity.END && isRtl)

        val snapToEnd = (gravity == Gravity.START && isRtl) ||
            (gravity == Gravity.END && !isRtl)

        for (i in 0 until lm.childCount) {
            val currentView = lm.getChildAt(i)
            val currentViewDistance = if (snapToStart) {
                if (!snapToPadding) {
                    abs(helper.getDecoratedStart(currentView).toDouble()).toInt()
                } else {
                    abs(
                        (
                            helper.startAfterPadding -
                                helper.getDecoratedStart(currentView)
                            ).toDouble(),
                    ).toInt()
                }
            } else if (snapToEnd) {
                if (!snapToPadding) {
                    abs(
                        (
                            helper.getDecoratedEnd(currentView) -
                                helper.end
                            ).toDouble(),
                    ).toInt()
                } else {
                    abs(
                        (
                            helper.endAfterPadding -
                                helper.getDecoratedEnd(currentView)
                            ).toDouble(),
                    ).toInt()
                }
            } else {
                abs(
                    (
                        helper.getDecoratedStart(currentView) +
                            (helper.getDecoratedMeasurement(currentView) / 2) - center
                        ).toDouble(),
                ).toInt()
            }
            if (currentViewDistance < distanceToTarget) {
                distanceToTarget = currentViewDistance
                edgeView = currentView
            }
        }
        return edgeView
    }

    private fun isAtEdgeOfList(lm: LinearLayoutManager): Boolean {
        return if ((!lm.reverseLayout && gravity == Gravity.START) ||
            (lm.reverseLayout && gravity == Gravity.END) ||
            (!lm.reverseLayout && gravity == Gravity.TOP) ||
            (lm.reverseLayout && gravity == Gravity.BOTTOM)
        ) {
            lm.findLastCompletelyVisibleItemPosition() == lm.itemCount - 1
        } else if (gravity == Gravity.CENTER) {
            (
                lm.findFirstCompletelyVisibleItemPosition() == 0 ||
                    lm.findLastCompletelyVisibleItemPosition() == lm.itemCount - 1
                )
        } else {
            lm.findFirstCompletelyVisibleItemPosition() == 0
        }
    }

    /**
     * Dispatches a [SnapListener.onSnap] event if the snapped position
     * is different than [RecyclerView.NO_POSITION].
     *
     *
     * When [GravitySnapHelper.findSnapView] returns null,
     * [GravitySnapHelper.dispatchSnapChangeWhenPositionIsUnknown] is called
     *
     * @param newState the new RecyclerView scroll state
     */
    private fun onScrollStateChanged(newState: Int) {
        if (newState == RecyclerView.SCROLL_STATE_IDLE && listener != null) {
            if (isScrolling) {
                if (nextSnapPosition != RecyclerView.NO_POSITION) {
                    listener!!.onSnap(nextSnapPosition)
                } else {
                    dispatchSnapChangeWhenPositionIsUnknown()
                }
            }
        }
        isScrolling = newState != RecyclerView.SCROLL_STATE_IDLE
    }

    /**
     * Calls [GravitySnapHelper.findSnapView]
     * without the check for the edge of the list.
     *
     *
     * This makes sure that a position is reported in [SnapListener.onSnap]
     */
    private fun dispatchSnapChangeWhenPositionIsUnknown() {
        val layoutManager = recyclerView!!.layoutManager ?: return
        val snapView = findSnapView(layoutManager, false) ?: return
        val snapPosition = recyclerView!!.getChildAdapterPosition(snapView)
        if (snapPosition != RecyclerView.NO_POSITION) {
            listener!!.onSnap(snapPosition)
        }
    }

    private fun getVerticalHelper(layoutManager: RecyclerView.LayoutManager): OrientationHelper? {
        if (verticalHelper == null || verticalHelper!!.layoutManager !== layoutManager) {
            verticalHelper = OrientationHelper.createVerticalHelper(layoutManager)
        }
        return verticalHelper
    }

    private fun getHorizontalHelper(layoutManager: RecyclerView.LayoutManager): OrientationHelper? {
        if (horizontalHelper == null || horizontalHelper!!.layoutManager !== layoutManager) {
            horizontalHelper = OrientationHelper.createHorizontalHelper(layoutManager)
        }
        return horizontalHelper
    }

    /**
     * A listener that's called when the [RecyclerView] used by [GravitySnapHelper]
     * changes its scroll state to [RecyclerView.SCROLL_STATE_IDLE]
     * and there's a valid snap position.
     */
    interface SnapListener {
        /**
         * @param position last position snapped to
         */
        fun onSnap(position: Int)
    }

    companion object {
        const val FLING_DISTANCE_DISABLE: Int = -1
        const val FLING_SIZE_FRACTION_DISABLE: Float = -1f
    }
}
