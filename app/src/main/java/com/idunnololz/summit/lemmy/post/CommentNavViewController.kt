package com.idunnololz.summit.lemmy.post

import android.graphics.Rect
import android.view.GestureDetector
import android.view.GestureDetector.SimpleOnGestureListener
import android.view.Gravity
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.view.updateLayoutParams
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.idunnololz.summit.R
import com.idunnololz.summit.lemmy.CommentNavControlsState
import com.idunnololz.summit.preferences.Preferences
import com.idunnololz.summit.util.Utils
import com.idunnololz.summit.util.ext.getDrawableCompat

class CommentNavViewController(
    private val parentView: ViewGroup,
    private val preferences: Preferences,
) {

    private var areViewsSetup = false
    private lateinit var nextButton: View
    private lateinit var prevButton: View
    private lateinit var statButton: View

    private val smallFabSize = Utils.convertDpToPixel(40f).toInt()
    private val fabMargin = Utils.convertDpToPixel(8f).toInt()

    private var offsetX: Int = 0
    private var offsetY: Int = 0

    private var isDragging = false
    private var startX: Int = 0
    private var startY: Int = 0

    private val gestureDetector = GestureDetector(
        parentView.context,
        object :
            SimpleOnGestureListener() {
            override fun onLongPress(e: MotionEvent) {
                super.onLongPress(e)
                isDragging = true
                startX = e.rawX.toInt()
                startY = e.rawY.toInt()

                statButton.performHapticFeedback(
                    HapticFeedbackConstants.LONG_PRESS,
                )
            }
        },
    )

    fun ensureControls() {
        if (areViewsSetup) {
            return
        }

        areViewsSetup = true

        val context = parentView.context

        val statButton = FloatingActionButton(context).apply {
            id = View.generateViewId()
        }
        val nextButton = makeFab()
        val prevButton = makeFab()

        this.statButton = statButton
        this.nextButton = nextButton
        this.prevButton = prevButton

        statButton.visibility = View.INVISIBLE
        prevButton.visibility = View.INVISIBLE
        nextButton.visibility = View.INVISIBLE

        parentView.addView(nextButton)
        parentView.addView(prevButton)
        parentView.addView(statButton)

//        statButton.text = "text"
        statButton.setImageDrawable(context.getDrawableCompat(R.drawable.baseline_more_horiz_24))

        prevButton.setImageDrawable(context.getDrawableCompat(R.drawable.baseline_expand_less_24))
        nextButton.setImageDrawable(context.getDrawableCompat(R.drawable.baseline_expand_more_24))

        statButton.updateLayoutParams<CoordinatorLayout.LayoutParams> {
            gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
            bottomMargin = Utils.convertDpToPixel(16f).toInt()
        }
        statButton.translationX = offsetX.toFloat()
        statButton.translationY = offsetY.toFloat()
        @Suppress("ClickableViewAccessibility")
        statButton.setOnTouchListener a@{ view, event ->

            if (!isDragging) {
                return@a gestureDetector.onTouchEvent(event)
            }

            parentView.requestDisallowInterceptTouchEvent(true)

            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    isDragging = false
                }
                MotionEvent.ACTION_MOVE -> {
                    val deltaX = event.rawX - startX
                    val deltaY = event.rawY - startY

                    statButton.translationX = deltaX + offsetX
                    statButton.translationY = deltaY + offsetY
                }
                MotionEvent.ACTION_UP -> {
                    isDragging = false
                    val deltaX = event.rawX - startX
                    val deltaY = event.rawY - startY

                    offsetX = (deltaX + offsetX).toInt()
                    offsetY = (deltaY + offsetY).toInt()

                    preferences.commentsNavigationFabOffX = offsetX
                    preferences.commentsNavigationFabOffY = offsetY
                }
                MotionEvent.ACTION_CANCEL -> {
                    isDragging = false
                }
                else -> {}
            }
            true
        }
        prevButton.translationX = (-fabMargin).toFloat()
        prevButton.updateLayoutParams<CoordinatorLayout.LayoutParams> {
            anchorId = statButton.id
            anchorGravity = Gravity.START or Gravity.CENTER_VERTICAL
            gravity = Gravity.START or Gravity.CENTER_VERTICAL
        }
        nextButton.translationX = fabMargin.toFloat()
        nextButton.updateLayoutParams<CoordinatorLayout.LayoutParams> {
            anchorId = statButton.id
            anchorGravity = Gravity.END or Gravity.CENTER_VERTICAL
            gravity = Gravity.END or Gravity.CENTER_VERTICAL
        }
    }

    private fun makeFab() = FloatingActionButton(parentView.context).apply {
        id = View.generateViewId()
        customSize = smallFabSize
    }

    fun show(
        state: CommentNavControlsState,
        onNextClick: () -> Unit,
        onPrevClick: () -> Unit,
        onMoreClick: () -> Unit,
    ) {
        offsetX = state.offsetX
        offsetY = state.offsetY

        ensureControls()

        prevButton.setOnClickListener {
            onPrevClick()
        }
        nextButton.setOnClickListener {
            onNextClick()
        }
        statButton.setOnClickListener {
            onMoreClick()
        }

        statButton.post {
            (statButton as? FloatingActionButton)?.show()
            (prevButton as? FloatingActionButton)?.show()
            (nextButton as? FloatingActionButton)?.show()

            statButton.post {
                ensureControlsInBounds()
            }
        }
    }

    private fun ensureControlsInBounds() {
        val offsetViewBounds = Rect()
        statButton.getDrawingRect(offsetViewBounds)
        parentView.offsetDescendantRectToMyCoords(statButton, offsetViewBounds)

        val finalLeft = offsetViewBounds.left + offsetX
        val finalRight = offsetViewBounds.right + offsetX
        val finalTop = offsetViewBounds.top + offsetY
        val finalBottom = offsetViewBounds.bottom + offsetY

        val boundsLeft = parentView.left + parentView.paddingLeft
        val boundsRight = parentView.right - parentView.paddingRight
        val boundsTop = parentView.top + parentView.paddingTop
        val boundsBottom = parentView.bottom - parentView.paddingBottom

        if (finalLeft < boundsLeft) {
            offsetX += boundsLeft - finalLeft
        } else if (finalRight > boundsRight) {
            offsetX += boundsRight - finalRight
        }

        if (finalTop < boundsTop) {
            offsetY += boundsTop - finalTop
        } else if (finalBottom > boundsBottom) {
            offsetY += boundsBottom - finalBottom
        }

        statButton.translationX = offsetX.toFloat()
        statButton.translationY = offsetY.toFloat()
    }

    fun hide() {
        ensureControls()
        (statButton as? FloatingActionButton)?.hide()
        (prevButton as? FloatingActionButton)?.hide()
        (nextButton as? FloatingActionButton)?.hide()
    }

    fun updateStats(commentPosition: Int, totalComments: Int) {
        ensureControls()
//        statButton.text = "${commentPosition} / ${totalComments}"
    }
}
