package com.idunnololz.summit.view

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.drawable.InsetDrawable
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.interpolator.view.animation.FastOutLinearInInterpolator
import androidx.interpolator.view.animation.LinearOutSlowInInterpolator
import androidx.recyclerview.widget.RecyclerView
import com.idunnololz.summit.R
import com.idunnololz.summit.util.Utils
import com.idunnololz.summit.util.ext.getColorFromAttribute
import com.idunnololz.summit.util.ext.tint

class FastScroller : LinearLayout {

    companion object {

        private val TAG = FastScroller::class.java.simpleName

        private const val AUTO_HIDE_DELAY_MS: Long = 1600

        private fun getViewRawY(view: View): Float {
            val location = IntArray(2)
            location[0] = 0
            location[1] = view.y.toInt()
            (view.parent as View).getLocationInWindow(location)
            return location[1].toFloat()
        }

        private fun getViewRawX(view: View): Float {
            val location = IntArray(2)
            location[0] = view.x.toInt()
            location[1] = 0
            (view.parent as View).getLocationInWindow(location)
            return location[0].toFloat()
        }

        private fun getValueInRange(min: Float, max: Float, value: Float): Float {
            val minimum = Math.max(min, value)
            return Math.min(minimum, max)
        }
    }

    private val scrollListener = object : RecyclerView.OnScrollListener() {
        override fun onScrolled(rv: RecyclerView, dx: Int, dy: Int) {
            if (shouldUpdateHandlePosition()) {
                updateHandlePosition(rv)
            }
            showHandle()
        }
    }

    private var recyclerView: RecyclerView? = null

    private lateinit var handle: View

    private var autoHideAnimator: ValueAnimator? = null
    private var animatingShow: Boolean = false

    private var scrollerOrientation: Int = 0

    private var isTouchingHandle = false
    private var isHandleInitialized = false

    // TODO the name should be fixed, also check if there is a better way of handling the visibility, because this is somewhat convoluted
    private var maxVisibility: Int = 0

    private var manuallyChangingPosition: Boolean = false
    private var touchOffset: Int = 0
    private lateinit var config: ViewConfiguration
    private var isDragging: Boolean = false

    private val hideRunnable = Runnable {
        Log.d(TAG, "Checking is should hide...")
        if (!isTouchingHandle) {
            Log.d(TAG, "Hiding scroll bar")
            autoHideAnimator?.cancel()
            autoHideAnimator = getAnimator().apply {
                setFloatValues(handle.translationX, handle.width.toFloat())
                interpolator = FastOutLinearInInterpolator()
                duration = 250
            }.also {
                it.start()
            }
        }
    }

    private var downY: Float = 0f
    private var lastY: Float = 0f

    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)

    constructor(context: Context, attrs: AttributeSet, defStyle: Int) : super(
        context,
        attrs,
        defStyle,
    )

    init {
        orientation = VERTICAL
    }

    override fun onFinishInflate() {
        super.onFinishInflate()

        val context = context
        val res = resources

        clipChildren = false
        maxVisibility = visibility

        config = ViewConfiguration.get(context)

        handle = View(context)

        val horizontalInset = if (!isVertical()) 0 else Utils.convertDpToPixel(16f).toInt()
        val verticalInset = if (isVertical()) 0 else Utils.convertDpToPixel(16f).toInt()
        val drawable = ContextCompat.getDrawable(context, R.drawable.fastscroll__default_handle)
        val tinted = drawable?.tint(context.getColorFromAttribute(androidx.appcompat.R.attr.colorPrimary))
        val handleBg = InsetDrawable(
            tinted,
            horizontalInset,
            verticalInset,
            0,
            0,
        )
        ViewCompat.setBackground(handle, handleBg)

        val handleWidth =
            res.getDimensionPixelSize(if (isVertical()) R.dimen.fastscroll__handle_clickable_width else R.dimen.fastscroll__handle_height)
        val handleHeight =
            res.getDimensionPixelSize(if (isVertical()) R.dimen.fastscroll__handle_height else R.dimen.fastscroll__handle_clickable_width)
        handle.layoutParams = ViewGroup.LayoutParams(handleWidth, handleHeight)
        addView(handle)
        ViewCompat.setElevation(handle, Utils.convertDpToPixel(2f))
    }

    /**
     * Attach the [FastScroller] to [RecyclerView]. Should be used after the adapter is set
     * to the [RecyclerView]. If the adapter implements SectionTitleProvider, the FastScroller
     * will showHandle a bubble with title.
     * @param recyclerView A [RecyclerView] to attach the [FastScroller] to.
     */
    fun setRecyclerView(recyclerView: RecyclerView) {
        this.recyclerView = recyclerView
        recyclerView.addOnScrollListener(scrollListener)
        recyclerView.adapter?.registerAdapterDataObserver(
            object :
                RecyclerView.AdapterDataObserver() {
                override fun onChanged() {
                    super.onChanged()
                    updateScrollerPosition()
                }

                override fun onItemRangeChanged(positionStart: Int, itemCount: Int) {
                    super.onItemRangeChanged(positionStart, itemCount)
                    updateScrollerPosition()
                }

                override fun onItemRangeChanged(positionStart: Int, itemCount: Int, payload: Any?) {
                    super.onItemRangeChanged(positionStart, itemCount, payload)
                    updateScrollerPosition()
                }

                override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
                    super.onItemRangeInserted(positionStart, itemCount)
                    updateScrollerPosition()
                }

                override fun onItemRangeRemoved(positionStart: Int, itemCount: Int) {
                    super.onItemRangeRemoved(positionStart, itemCount)
                    updateScrollerPosition()
                }

                override fun onItemRangeMoved(fromPosition: Int, toPosition: Int, itemCount: Int) {
                    super.onItemRangeMoved(fromPosition, toPosition, itemCount)
                    updateScrollerPosition()
                }
            },
        )
        invalidateVisibility()
        recyclerView.setOnHierarchyChangeListener(
            object : ViewGroup.OnHierarchyChangeListener {
                override fun onChildViewAdded(parent: View, child: View) {
                    invalidateVisibility()
                }

                override fun onChildViewRemoved(parent: View, child: View) {
                    invalidateVisibility()
                }
            },
        )
    }

    private fun updateScrollerPosition() {
        recyclerView?.let {
            it.postDelayed({
                scrollListener.onScrolled(it, 0, 0)
            }, 250,)
        }
    }

    internal fun updateHandlePosition(rv: RecyclerView?) {
        rv ?: return

        val relativePos: Float = if (isVertical()) {
            val offset = rv.computeVerticalScrollOffset()
            val extent = rv.computeVerticalScrollExtent()
            val range = rv.computeVerticalScrollRange()
            offset / (range - extent).toFloat()
        } else {
            val offset = rv.computeHorizontalScrollOffset()
            val extent = rv.computeHorizontalScrollExtent()
            val range = rv.computeHorizontalScrollRange()
            offset / (range - extent).toFloat()
        }
        setScrollerPosition(relativePos)
    }

    /**
     * Set the orientation of the [FastScroller]. The orientation of the [FastScroller]
     * should generally match the orientation of connected  [RecyclerView] for good UX but it's not enforced.
     * Note: This method is overridden from [LinearLayout.setOrientation] but for [FastScroller]
     * it has a totally different meaning.
     * @param orientation of the [FastScroller]. [.VERTICAL] or [.HORIZONTAL]
     */
    override fun setOrientation(orientation: Int) {
        scrollerOrientation = orientation
        // switching orientation, because orientation in linear layout
        // is something different than orientation of fast scroller
        super.setOrientation(
            if (orientation == LinearLayout.HORIZONTAL) {
                LinearLayout.VERTICAL
            } else {
                LinearLayout.HORIZONTAL
            },
        )
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        Log.d(TAG, "onLayout")
        super.onLayout(changed, l, t, r, b)

        if (!isHandleInitialized) {
            isHandleInitialized = true
            initHandleMovement()
        }

        if (!isInEditMode) {
            // sometimes recycler starts with a defined scroll (e.g. when coming from saved state)
            updateHandlePosition(recyclerView)
        }
    }

    override fun setVisibility(visibility: Int) {
        maxVisibility = visibility
        invalidateVisibility()
    }

    private fun initHandleMovement() {
        handle.setOnTouchListener { _, event ->
            requestDisallowInterceptTouchEvent(true)

            val action = event.action
            val y = event.y
            return@setOnTouchListener when (action) {
                MotionEvent.ACTION_DOWN -> {
                    isTouchingHandle = true
                    touchOffset = y.toInt()
                    Log.d(TAG, "touchOffset: $touchOffset")
                    downY = y
                    lastY = y
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    if (!isDragging && Math.abs(y - downY) > config.scaledTouchSlop) {
                        isDragging = true
                        touchOffset += (lastY - downY).toInt()
                        Log.d(TAG, "touchOffset: $touchOffset")
                    }
                    if (isDragging) {
                        manuallyChangingPosition = true
                        val relativePos = getRelativeTouchPosition(event)
                        setScrollerPosition(relativePos)
                        setRecyclerViewPosition(relativePos)
                    }
                    lastY = y
                    true
                }
                MotionEvent.ACTION_UP -> {
                    manuallyChangingPosition = false

                    isTouchingHandle = false
                    cancelAutoHide()
                    postDelayed(hideRunnable, AUTO_HIDE_DELAY_MS)
                    true
                }
                MotionEvent.ACTION_CANCEL -> {
                    isTouchingHandle = false
                    cancelAutoHide()
                    postDelayed(hideRunnable, AUTO_HIDE_DELAY_MS)
                    true
                }
                else -> false
            }
        }
    }

    private fun cancelAutoHide() {
        removeCallbacks(hideRunnable)
    }

    private fun getRelativeTouchPosition(event: MotionEvent): Float {
        return if (isVertical()) {
            val yInParent = event.rawY - getViewRawY(handle) - touchOffset.toFloat()
            yInParent / (height - handle.height)
        } else {
            val xInParent = event.rawX - getViewRawX(handle)
            xInParent / (width - handle.width)
        }
    }

    private fun invalidateVisibility() {
        val recyclerView = recyclerView
        if (recyclerView == null ||
            recyclerView.adapter == null ||
            recyclerView.adapter?.itemCount == 0 ||
            recyclerView.getChildAt(0) == null ||
            !isRecyclerViewScrollable() ||
            maxVisibility != View.VISIBLE
        ) {
            super.setVisibility(View.INVISIBLE)
        } else {
            super.setVisibility(View.VISIBLE)
        }
    }

    private fun setRecyclerViewPosition(relativePos: Float) {
        recyclerView?.let {
            val itemCount = it.adapter?.itemCount ?: 0
            val targetPos = getValueInRange(
                0f,
                itemCount - 1f,
                (relativePos * itemCount.toFloat()).toInt().toFloat(),
            ).toInt()
            it.scrollToPosition(targetPos)
        }
    }

    private fun setScrollerPosition(relativePos: Float) {
        invalidateVisibility()
        if (isVertical()) {
            handle.y = getValueInRange(
                0f,
                (height - handle.height).toFloat(),
                relativePos * (height - handle.height),
            )
        } else {
            handle.x = getValueInRange(
                0f,
                (width - handle.width).toFloat(),
                relativePos * (width - handle.width),
            )
        }
    }

    private fun shouldUpdateHandlePosition(): Boolean {
        val recyclerView = recyclerView
        return !manuallyChangingPosition && recyclerView != null && recyclerView.childCount > 0
    }

    private fun getAnimator(): ValueAnimator {
        return autoHideAnimator ?: ValueAnimator.ofFloat(0f, 0f).also { va ->
            va.addUpdateListener { handle.translationX = it.animatedValue as Float }
            autoHideAnimator = va
        }
    }

    private fun isRecyclerViewScrollable(): Boolean {
        return recyclerView?.let {
            it.computeHorizontalScrollRange() > it.width || it.computeVerticalScrollRange() > it.height
        } ?: false
    }

    private fun showHandle() {
        if (!animatingShow) {
            autoHideAnimator?.cancel()
            autoHideAnimator = getAnimator().apply {
                setFloatValues(handle.translationX, 0f)
                interpolator = LinearOutSlowInInterpolator()
                duration = 250
                addListener(
                    object : AnimatorListenerAdapter() {
                        override fun onAnimationCancel(animation: Animator) {
                            super.onAnimationCancel(animation)
                            animation.removeListener(this)
                            animatingShow = false
                        }

                        override fun onAnimationEnd(animation: Animator) {
                            super.onAnimationEnd(animation)
                            animation.removeListener(this)
                            animatingShow = false
                        }
                    },
                )
            }.also {
                it.start()
            }
            animatingShow = true
        }

        cancelAutoHide()
        postDelayed(hideRunnable, AUTO_HIDE_DELAY_MS)
    }

    private fun isVertical() = scrollerOrientation == LinearLayout.VERTICAL
}
