package com.idunnololz.summit.view.swipeRefreshLayout

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.util.TypedValue
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.widget.ListView
import androidx.core.view.NestedScrollingChild
import androidx.core.view.NestedScrollingChildHelper
import androidx.core.view.NestedScrollingParent
import androidx.core.view.NestedScrollingParentHelper
import androidx.core.view.ViewCompat
import androidx.core.view.isVisible
import androidx.dynamicanimation.animation.DynamicAnimation
import androidx.dynamicanimation.animation.SpringAnimation
import androidx.dynamicanimation.animation.SpringForce
import com.idunnololz.summit.R
import kotlin.io.path.Path
import kotlin.math.abs
import kotlin.math.max

@SuppressLint("DrawAllocation")
abstract class SimpleSwipeRefreshLayout
  : ViewGroup, NestedScrollingParent, NestedScrollingChild {

  companion object {
    private const val STICKY_FACTOR = 0.66F
    private const val STICKY_MULTIPLIER = 0.75F
    private const val ROLL_BACK_DURATION = 300L
    private const val DEFAULT_INDICATOR_TARGET = 64f

    const val ELEVATION = 4
  }

  private var notify: Boolean = true
  var isRefreshing: Boolean = false
    set(value) {
      Log.d("HAHA", "isRefreshing: $value")
      if (field != value) {
        Log.d("HAHA", "isRefreshing2: $value")
        field = value

        if (value) {
          if (currentState != State.TRIGGERING) {
            startRefreshing()
          }
        } else {
          notify = false
          currentState = State.ROLLING
          stopRefreshing()
        }
      }
    }

  /**
   * @param triggerOffSetTop : The offset in pixels from the top of this view at which the progress indicator should
   *         come to rest after a successful swipe gesture.
   */
  var triggerOffSetTop = 0
    private set

  /**
   * @param maxOffSetTop : The maximum distance in pixels that the refresh indicator can be pulled
   *        beyond its resting position.
   */
  var maxOffSetTop = 0
    private set

  var topInset: Int = 0
    set(value) {
      field = value

      requestLayout()
    }

  private var startRefreshingAnimator: ValueAnimator? = null
  private var stopRefreshingAnimator: ValueAnimator? = null

  private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop
  private var contentOffsetY = 0f

  private var downX = 0F
  private var downY = 0F

  private var indicatorOffsetY = 0F
  private var lastPullFraction = 0F

  private var currentState: State = State.IDLE

  private val onProgressListeners: MutableCollection<(Float) -> Unit> = mutableListOf()
  private val onTriggerListeners: MutableCollection<() -> Unit> = mutableListOf()

  var mNestedScrollingParentHelper: NestedScrollingParentHelper
  var mNestedScrollingChildHelper: NestedScrollingChildHelper
  private val mParentScrollConsumed = IntArray(2)
  private val mParentOffsetInWindow = IntArray(2)
  private var mNestedScrollInProgress = false

  private lateinit var topChildView: ChildView
  private lateinit var contentChildView: ChildView

  private val actualTriggerOffSetTop
    get() = triggerOffSetTop + topInset

  private val swipeFriction
    get() = (1 - STICKY_FACTOR * STICKY_MULTIPLIER) *
      (1 + topInset / actualTriggerOffSetTop.toFloat())

  private val contentSwipeFriction
    get() = (1 - STICKY_FACTOR * STICKY_MULTIPLIER) *
      (1 + topInset / actualTriggerOffSetTop.toFloat()) *
      0.5f

  constructor(context: Context?) : super(context)

  constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs) {
    applyAttrs(attrs, 0)
  }

  constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(
    context,
    attrs,
    defStyleAttr,
  ) {
    applyAttrs(attrs, defStyleAttr)
  }

  init {
    mNestedScrollingParentHelper = NestedScrollingParentHelper(this)
    mNestedScrollingChildHelper = NestedScrollingChildHelper(this)
    isNestedScrollingEnabled = true
  }

  private fun applyAttrs(attrs: AttributeSet?, defStyleAttr: Int) {
    context.theme.obtainStyledAttributes(attrs, R.styleable.SimpleSwipeRefreshLayout, defStyleAttr, 0).let {
      val defaultValue = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, DEFAULT_INDICATOR_TARGET, context.resources.displayMetrics).toInt()

      triggerOffSetTop = it.getDimensionPixelOffset(R.styleable.SimpleSwipeRefreshLayout_trigger_offset_top, defaultValue)
      maxOffSetTop = it.getDimensionPixelOffset(R.styleable.SimpleSwipeRefreshLayout_max_offset_top, defaultValue * 3)

      if (maxOffSetTop <= triggerOffSetTop) {
        maxOffSetTop = triggerOffSetTop * 3
      }

      it.recycle()
    }
  }

  override fun onFinishInflate() {
    super.onFinishInflate()

    if (childCount != 2) {
      throw IllegalStateException("Only a topView and a contentView are allowed. Exactly 2 children are expected, but was $childCount")
    }

    topChildView = ChildView(getChildAt(0))
    contentChildView = ChildView(getChildAt(1))
  }

  override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
    super.onMeasure(widthMeasureSpec, heightMeasureSpec)
    fun measureChild(childView: ChildView, widthMeasureSpec: Int, heightMeasureSpec: Int) {
      measureChildWithMargins(childView.view, widthMeasureSpec, 0, heightMeasureSpec, 0)
    }

    fun setInitialValues() {
      val topView = topChildView.view
      val layoutParams = topView.layoutParams as LayoutParams
      val topViewHeight = topView.measuredHeight + layoutParams.topMargin + layoutParams.bottomMargin
      topChildView = topChildView.copy(positionAttr = PositionAttr(height = topViewHeight))
    }

    measureChild(topChildView, widthMeasureSpec, heightMeasureSpec)
    measureChild(contentChildView, widthMeasureSpec, heightMeasureSpec)

    setInitialValues()
  }

  override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
    layoutTopView()
    layoutContentView()
  }

  fun layoutTopView() {
    val topView = topChildView.view
    val topViewAttr = topChildView.positionAttr
    val topSpace = (triggerOffSetTop - topView.height) / 2

    val lp = topView.layoutParams as LayoutParams
    if (lp.width == ViewGroup.LayoutParams.MATCH_PARENT) {
      val left: Int = paddingLeft + lp.leftMargin
      val top: Int = (paddingTop + lp.topMargin) - topViewAttr.height - ELEVATION - topSpace
      val right: Int = left + topView.measuredWidth
      val bottom = - ELEVATION
      topChildView = topChildView.copy(positionAttr = PositionAttr(left = left, top = top, right = right, bottom = bottom))
      topView.layout(left, top, right, bottom)
    } else {
      val indicatorWidth: Int = topView.measuredWidth
      val left: Int = width / 2 - indicatorWidth / 2
      val top: Int = (paddingTop + lp.topMargin) - topViewAttr.height - ELEVATION - topSpace
      val right: Int = width / 2 + indicatorWidth / 2
      val bottom = -ELEVATION

      topChildView = topChildView.copy(positionAttr = PositionAttr(left = left, top = top, right = right, bottom = bottom))
      topView.layout(left, top, right, bottom)
    }
  }

  fun layoutContentView() {
    val contentView = contentChildView.view

    val lp = contentView.layoutParams as LayoutParams
    val left: Int = paddingLeft + lp.leftMargin
    val top: Int = paddingTop + lp.topMargin
    val right: Int = left + contentView.measuredWidth
    val bottom: Int = top + contentView.measuredHeight

    contentChildView = contentChildView.copy(positionAttr = PositionAttr(left = left, top = top, right = right, bottom = bottom))
    contentView.layout(left, top, right, bottom)
  }

  override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
    if (!isEnabled || isRefreshing || currentState == State.ROLLING || mNestedScrollInProgress || canChildScrollUp()) {
      return false
    }

    fun checkIfScrolledFurther(ev: MotionEvent, dy: Float, dx: Float): Boolean {
      if (!contentChildView.view.canScrollVertically(-1)) {
        if (ev.y - downY < touchSlop) {
          return false
        }

        return ev.y > downY && Math.abs(dy) > Math.abs(dx)
      } else {
        return false
      }
    }

    var shouldStealTouchEvents = false

    if (currentState != State.IDLE) {
      shouldStealTouchEvents = false
    }

    when (ev.action) {
      MotionEvent.ACTION_DOWN -> {
        downX = ev.x
        downY = ev.y
      }
      MotionEvent.ACTION_MOVE -> {
        val dx = ev.x - downX
        val dy = ev.y - downY
        shouldStealTouchEvents = checkIfScrolledFurther(ev, dy, dx)
      }
    }

    return shouldStealTouchEvents
  }

  override fun onTouchEvent(event: MotionEvent): Boolean {
    if (!isEnabled || isRefreshing || currentState == State.ROLLING || mNestedScrollInProgress || canChildScrollUp()) {
      return false
    }

    var handledTouchEvent = true

    if (currentState != State.IDLE) {
      handledTouchEvent = false
    }

    parent.requestDisallowInterceptTouchEvent(true)
    when (event.action) {
      MotionEvent.ACTION_MOVE -> {
        indicatorOffsetY = (event.y - downY) * swipeFriction
        contentOffsetY = (event.y - downY) * contentSwipeFriction
        notify = true
        move()
      }
      MotionEvent.ACTION_CANCEL,
      MotionEvent.ACTION_UP,
        -> {
        currentState = State.ROLLING
        onAnimateToFinalPosition()
      }
    }

    return handledTouchEvent
  }

  open fun startRefreshing() {
    val realTriggerOffSetTop = actualTriggerOffSetTop
    val triggerOffset: Float = if (indicatorOffsetY > realTriggerOffSetTop)
      indicatorOffsetY
    else
      realTriggerOffSetTop.toFloat()

    stopRefreshingAnimator?.cancel()
    startRefreshingAnimator?.cancel()
    startRefreshingAnimator = ValueAnimator.ofFloat(0F, 1F).apply {
      duration = ROLL_BACK_DURATION
      interpolator = DecelerateInterpolator(2f)
      addUpdateListener {
        positionChildren(triggerOffset * animatedValue as Float, contentOffsetY)
      }
      addListener(
        object : AnimatorListenerAdapter() {
          override fun onAnimationEnd(animation: Animator) {
            indicatorOffsetY = triggerOffset
            contentOffsetY = 0f
          }
        },
      )
      start()
    }
  }

  private fun move() {
    val realTriggerOffSetTop = actualTriggerOffSetTop
    val pullFraction: Float =
      if (indicatorOffsetY == 0F) 0F
      else if (realTriggerOffSetTop > indicatorOffsetY) indicatorOffsetY / realTriggerOffSetTop
      else 1F

    indicatorOffsetY = indicatorOffsetY.coerceIn(0f, maxOffSetTop.toFloat() + topInset)
    contentOffsetY = max(contentOffsetY, 0f)

    if (indicatorOffsetY > 0f && !topChildView.view.isVisible) {
      topChildView.view.visibility = VISIBLE
    }

    onProgressListeners.forEach { it(pullFraction) }
    lastPullFraction = pullFraction

    positionChildren(indicatorOffsetY, contentOffsetY)
  }

  private fun onAnimateToFinalPosition() {
    val realTriggerOffSetTop = actualTriggerOffSetTop
    val refreshTriggered = indicatorOffsetY > realTriggerOffSetTop

    if (refreshTriggered) {
      SpringAnimation(topChildView.view, DynamicAnimation.TRANSLATION_Y)
        .addEndListener { animation, canceled, value, velocity ->
          currentState = State.TRIGGERING
          isRefreshing = true
          indicatorOffsetY = realTriggerOffSetTop.toFloat()
          onTriggerListeners.forEach { it() }
        }
        .animateToFinalPosition(realTriggerOffSetTop.toFloat())
    } else {
      SpringAnimation(topChildView.view, DynamicAnimation.TRANSLATION_Y)
        .addEndListener { animation, canceled, value, velocity ->
          stopRefreshingComplete()
          currentState = State.IDLE
          indicatorOffsetY = 0f
        }
        .animateToFinalPosition(0f)
    }

    val startContentOffsetY = contentOffsetY
    stopRefreshingAnimator = ValueAnimator.ofFloat(1F, 0F).apply {
      duration = ROLL_BACK_DURATION
      interpolator = AccelerateDecelerateInterpolator()
      addUpdateListener {
        positionContentView(startContentOffsetY * animatedValue as Float)
      }
      addListener(
        object : AnimatorListenerAdapter() {
          override fun onAnimationEnd(animation: Animator) {
            contentOffsetY = 0f
          }
        },
      )
      start()
    }
  }

  private fun stopRefreshing() {
    topChildView.view
      .animate()
      .scaleY(0f)
      .scaleX(0f)
      .alpha(0f)
      .withEndAction {
        stopRefreshingComplete()
        currentState = State.IDLE
        topChildView.view.apply {
          scaleX = 1f
          scaleY = 1f
          alpha = 1f
        }
        topChildView.view.y = topChildView.positionAttr.top.toFloat()
        topChildView.view.visibility = View.INVISIBLE
      }
      .start()
  }

  open fun stopRefreshingComplete() {}

  private fun positionChildren(indicatorOffset: Float, contentOffset: Float) {
    topChildView.view.bringToFront()
    topChildView.view.y = topChildView.positionAttr.top + indicatorOffset

    contentChildView.view.y = contentChildView.positionAttr.top + contentOffset
  }

  private fun positionContentView(contentOffset: Float) {
    contentChildView.view.y = contentChildView.positionAttr.top + contentOffset
  }

  //<editor-fold desc="Helpers">
  fun addProgressListener(onProgressListener: (Float) -> Unit) {
    onProgressListeners.add(onProgressListener)
  }

  /**
   * Set the listener to be notified when a refresh is triggered via the swipe
   * gesture.
   */
  open fun setOnRefreshListener(listener: () -> Unit) {
    onTriggerListeners.add(listener)
  }

  fun addTriggerListener(onTriggerListener: () -> Unit) {
    onTriggerListeners.add(onTriggerListener)
  }

  fun removeOnTriggerListener(onTriggerListener: () -> Unit) {
    onTriggerListeners.remove(onTriggerListener)
  }

  override fun checkLayoutParams(p: ViewGroup.LayoutParams?) = null != p && p is LayoutParams

  override fun generateDefaultLayoutParams() = LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)

  override fun generateLayoutParams(attrs: AttributeSet?) = LayoutParams(context, attrs)

  override fun generateLayoutParams(p: ViewGroup.LayoutParams?) = LayoutParams(p)

  class LayoutParams : MarginLayoutParams {

    constructor(c: Context, attrs: AttributeSet?) : super(c, attrs)

    constructor(width: Int, height: Int) : super(width, height)
    constructor(source: MarginLayoutParams) : super(source)
    constructor(source: ViewGroup.LayoutParams) : super(source)
  }

  enum class State {
    IDLE,
    ROLLING,
    TRIGGERING
  }

  data class ChildView(val view: View, val positionAttr: PositionAttr = PositionAttr())
  data class PositionAttr(val left: Int = 0, val top: Int = 0, val right: Int = 0, val bottom: Int = 0, val height: Int = 0)
  //</editor-fold>

  // NestedScrollingParent

  // NestedScrollingParent
  override fun onStartNestedScroll(child: View, target: View, nestedScrollAxes: Int): Boolean {
    return (isEnabled && currentState != State.ROLLING && !isRefreshing
      && nestedScrollAxes and ViewCompat.SCROLL_AXIS_VERTICAL != 0)
  }

  override fun onNestedScrollAccepted(child: View, target: View, axes: Int) {
    // Reset the counter of how much leftover scroll needs to be consumed.
    mNestedScrollingParentHelper.onNestedScrollAccepted(child, target, axes)
    // Dispatch up to the nested parent
    startNestedScroll(axes and ViewCompat.SCROLL_AXIS_VERTICAL)
    indicatorOffsetY = 0f
    contentOffsetY = 0f
    mNestedScrollInProgress = true
  }

  override fun onNestedPreScroll(target: View, dx: Int, dy: Int, consumed: IntArray) {
    // If we are in the middle of consuming, a scroll, then we want to move the spinner back up
    // before allowing the list to scroll
    if (dy > 0 && indicatorOffsetY > 0) {
      if (dy > indicatorOffsetY) {
        consumed[1] = dy - indicatorOffsetY.toInt()
        indicatorOffsetY = 0f
        contentOffsetY = 0f
      } else {
        indicatorOffsetY -= dy.toFloat()
        contentOffsetY -= dy.toFloat()
        consumed[1] = dy
      }
      move()
    }

    // Now let our nested parent consume the leftovers
    val parentConsumed: IntArray = mParentScrollConsumed
    if (dispatchNestedPreScroll(dx - consumed[0], dy - consumed[1], parentConsumed, null)) {
      consumed[0] += parentConsumed[0]
      consumed[1] += parentConsumed[1]
    }
  }

  override fun getNestedScrollAxes(): Int {
    return mNestedScrollingParentHelper.nestedScrollAxes
  }

  override fun onStopNestedScroll(target: View) {
    mNestedScrollingParentHelper.onStopNestedScroll(target)
    mNestedScrollInProgress = false
    // Finish the Indicator for nested scrolling if we ever consumed any unconsumed nested scroll
    if (indicatorOffsetY > 0) {
      notify = true
      currentState = State.ROLLING
      onAnimateToFinalPosition()
      indicatorOffsetY = 0f
      contentOffsetY = 0f
    }
    // Dispatch up our nested parent
    stopNestedScroll()
  }

  override fun onNestedScroll(
    target: View, dxConsumed: Int, dyConsumed: Int,
    dxUnconsumed: Int, dyUnconsumed: Int,
  ) {
    // Dispatch up to the nested parent first
    dispatchNestedScroll(
      dxConsumed, dyConsumed, dxUnconsumed, dyUnconsumed,
      mParentOffsetInWindow,
    )

    // This is a bit of a hack. Nested scrolling works from the bottom up, and as we are
    // sometimes between two nested scrolling views, we need a way to be able to know when any
    // nested scrolling parent has stopped handling events. We do that by using the
    // 'offset in window 'functionality to see if we have been moved from the event.
    // This is a decent indication of whether we should take over the event stream or not.
    val dy: Int = dyUnconsumed + mParentOffsetInWindow[1]
    if (dy < 0 && !canChildScrollUp()) {
      indicatorOffsetY += abs(dy).toFloat() * swipeFriction
      contentOffsetY += abs(dy).toFloat() * contentSwipeFriction
      move()
    }
  }

  // NestedScrollingChild
  override fun setNestedScrollingEnabled(enabled: Boolean) {
    mNestedScrollingChildHelper.isNestedScrollingEnabled = enabled
  }

  override fun isNestedScrollingEnabled(): Boolean {
    return mNestedScrollingChildHelper.isNestedScrollingEnabled
  }

  override fun startNestedScroll(axes: Int): Boolean {
    return mNestedScrollingChildHelper.startNestedScroll(axes)
  }

  override fun stopNestedScroll() {
    mNestedScrollingChildHelper.stopNestedScroll()
  }

  override fun hasNestedScrollingParent(): Boolean {
    return mNestedScrollingChildHelper.hasNestedScrollingParent()
  }

  override fun dispatchNestedScroll(
    dxConsumed: Int, dyConsumed: Int, dxUnconsumed: Int,
    dyUnconsumed: Int, offsetInWindow: IntArray?,
  ): Boolean {
    return mNestedScrollingChildHelper.dispatchNestedScroll(
      dxConsumed, dyConsumed,
      dxUnconsumed, dyUnconsumed, offsetInWindow,
    )
  }

  override fun dispatchNestedPreScroll(dx: Int, dy: Int, consumed: IntArray?, offsetInWindow: IntArray?): Boolean {
    return mNestedScrollingChildHelper.dispatchNestedPreScroll(
      dx, dy, consumed, offsetInWindow,
    )
  }

  override fun dispatchNestedFling(velocityX: Float, velocityY: Float, consumed: Boolean): Boolean {
    return mNestedScrollingChildHelper.dispatchNestedFling(velocityX, velocityY, consumed)
  }

  override fun dispatchNestedPreFling(velocityX: Float, velocityY: Float): Boolean {
    return mNestedScrollingChildHelper.dispatchNestedPreFling(velocityX, velocityY)
  }

  /**
   * @return Whether it is possible for the child view of this layout to
   * scroll up. Override this if the child view is a custom view.
   */
  open fun canChildScrollUp(): Boolean {
    val view = contentChildView.view
    return if (view is ListView) {
      view.canScrollList(-1)
    } else contentChildView.view.canScrollVertically(-1)
  }

}