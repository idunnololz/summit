package com.idunnololz.summit.util

import android.graphics.RectF
import android.text.Selection
import android.text.Spannable
import android.text.Spanned
import android.text.method.LinkMovementMethod
import android.text.style.BackgroundColorSpan
import android.text.style.ClickableSpan
import android.text.style.URLSpan
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.ViewConfiguration
import android.widget.TextView
import com.idunnololz.summit.R

class CustomLinkMovementMethod : LinkMovementMethod() {

    var onLinkClickListener: OnLinkClickListener? = null
    var onLinkLongClickListener: OnLinkLongClickListener? = null
    private val touchedLineBounds = RectF()
    private var isUrlHighlighted = false
    private var clickableSpanUnderTouchOnActionDown: ClickableSpan? = null
    private var activeTextViewHashcode = 0
    private var ongoingLongPressTimer: LongPressTimer? = null
    private var wasLongPressRegistered = false

    interface OnLinkClickListener {
        /**
         * @param textView The TextView on which a click was registered.
         * @param url      The clicked URL.
         * @return True if this click was handled. False to let Android handle the URL.
         */
        fun onClick(textView: TextView, url: String, text: String, rect: RectF): Boolean
    }

    interface OnLinkLongClickListener {
        /**
         * @param textView The TextView on which a long-click was registered.
         * @param url      The long-clicked URL.
         * @return True if this long-click was handled. False to let Android handle the URL (as a short-click).
         */
        fun onLongClick(textView: TextView, url: String, text: String, rect: RectF): Boolean
    }

    override fun onTouchEvent(textView: TextView?, text: Spannable?, event: MotionEvent?): Boolean {
        textView ?: return false
        text ?: return false
        event ?: return false

        if (activeTextViewHashcode != textView.hashCode()) {
            // Bug workaround: TextView stops calling onTouchEvent() once any URL is highlighted.
            // A hacky solution is to reset any "autoLink" property set in XML. But we also want
            // to do this once per TextView.
            activeTextViewHashcode = textView.hashCode()
            textView.autoLinkMask = 0
        }

        val clickableSpanUnderTouch: ClickableSpan? =
            findClickableSpanUnderTouch(textView, text, event)
        if (event.action == MotionEvent.ACTION_DOWN) {
            clickableSpanUnderTouchOnActionDown = clickableSpanUnderTouch
        }
        val touchStartedOverAClickableSpan =
            clickableSpanUnderTouchOnActionDown != null

        return when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                clickableSpanUnderTouch?.let { highlightUrl(textView, it, text) }
                if (touchStartedOverAClickableSpan && onLinkLongClickListener != null) {
                    val longClickListener: LongPressTimer.OnTimerReachedListener =
                        object : LongPressTimer.OnTimerReachedListener {
                            override fun onTimerReached() {
                                wasLongPressRegistered = true
                                textView.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                                removeUrlHighlightColor(textView)
                                if (clickableSpanUnderTouch != null) {
                                    dispatchUrlLongClick(
                                        textView, clickableSpanUnderTouch, RectF(
                                            event.x - Utils.convertDpToPixel(16f),
                                            event.y - Utils.convertDpToPixel(16f),
                                            event.x + Utils.convertDpToPixel(16f),
                                            event.y + Utils.convertDpToPixel(16f)
                                        )
                                    )
                                }
                            }
                        }
                    startTimerForRegisteringLongClick(textView, longClickListener)
                }
                touchStartedOverAClickableSpan
            }
            MotionEvent.ACTION_UP -> {
                // Register a click only if the touch started and ended on the same URL.
                if (!wasLongPressRegistered && touchStartedOverAClickableSpan && clickableSpanUnderTouch === clickableSpanUnderTouchOnActionDown) {
                    if (clickableSpanUnderTouch != null) {
                        dispatchUrlClick(
                            textView, clickableSpanUnderTouch, RectF(
                                event.x - Utils.convertDpToPixel(16f),
                                event.y - Utils.convertDpToPixel(16f),
                                event.x + Utils.convertDpToPixel(16f),
                                event.y + Utils.convertDpToPixel(16f)
                            )
                        )
                    }
                }
                cleanupOnTouchUp(textView)

                // Consume this event even if we could not find any spans to avoid letting Android handle this event.
                // Android's TextView implementation has a bug where links get clicked even when there is no more text
                // next to the link and the touch lies outside its bounds in the same direction.
                touchStartedOverAClickableSpan
            }
            MotionEvent.ACTION_CANCEL -> {
                cleanupOnTouchUp(textView)
                false
            }
            MotionEvent.ACTION_MOVE -> {
                // Stop listening for a long-press as soon as the user wanders off to unknown lands.
                if (clickableSpanUnderTouch !== clickableSpanUnderTouchOnActionDown) {
                    removeLongPressCallback(textView)
                }
                if (!wasLongPressRegistered) {
                    // Toggle highlight.
                    clickableSpanUnderTouch?.let { highlightUrl(textView, it, text) }
                        ?: removeUrlHighlightColor(textView)
                }
                touchStartedOverAClickableSpan
            }
            else -> false
        }
    }

    private fun cleanupOnTouchUp(textView: TextView) {
        wasLongPressRegistered = false
        clickableSpanUnderTouchOnActionDown = null
        removeUrlHighlightColor(textView)
        removeLongPressCallback(textView)
    }

    /**
     * Remove the long-press detection timer.
     */
    private fun removeLongPressCallback(textView: TextView) {
        if (ongoingLongPressTimer != null) {
            textView.removeCallbacks(ongoingLongPressTimer)
            ongoingLongPressTimer = null
        }
    }

    private fun startTimerForRegisteringLongClick(
        textView: TextView,
        longClickListener: LongPressTimer.OnTimerReachedListener?
    ) {
        ongoingLongPressTimer = LongPressTimer().apply {
            setOnTimerReachedListener(longClickListener)
        }
        textView.postDelayed(
            ongoingLongPressTimer,
            ViewConfiguration.getLongPressTimeout().toLong()
        )
    }

    private fun dispatchUrlClick(
        textView: TextView,
        clickableSpan: ClickableSpan,
        bounds: RectF
    ) {
        val clickableSpanWithText =
            ClickableSpanWithText.ofSpan(textView, clickableSpan)
        val handled = onLinkClickListener?.onClick(
            textView,
            clickableSpanWithText.url,
            clickableSpanWithText.text,
            bounds
        ) ?: false
        if (!handled) {
            // Let Android handle this click.
            clickableSpanWithText.span.onClick(textView)
        }
    }

    private fun dispatchUrlLongClick(
        textView: TextView,
        clickableSpan: ClickableSpan,
        bounds: RectF
    ) {
        val clickableSpanWithText =
            ClickableSpanWithText.ofSpan(textView, clickableSpan)
        val handled = onLinkLongClickListener?.onLongClick(
            textView,
            clickableSpanWithText.url,
            clickableSpanWithText.text,
            bounds
        ) ?: false
        if (!handled) {
            // Let Android handle this long click as a short-click.
            clickableSpanWithText.span.onClick(textView)
        }
    }

    /**
     * Removes the highlight color under the Url.
     */
    private fun removeUrlHighlightColor(textView: TextView) {
        if (!isUrlHighlighted) {
            return
        }
        isUrlHighlighted = false
        val text = textView.text as Spannable
        val highlightSpan =
            textView.getTag(R.id.bettermovementmethod_highlight_background_span) as BackgroundColorSpan
        text.removeSpan(highlightSpan)
        Selection.removeSelection(text)
    }

    /**
     * Adds a background color span at <var>clickableSpan</var>'s location.
     */
    private fun highlightUrl(
        textView: TextView,
        clickableSpan: ClickableSpan?,
        text: Spannable
    ) {
        if (isUrlHighlighted) {
            return
        }
        isUrlHighlighted = true
        val spanStart = text.getSpanStart(clickableSpan)
        val spanEnd = text.getSpanEnd(clickableSpan)
        val highlightSpan = BackgroundColorSpan(textView.highlightColor)
        text.setSpan(highlightSpan, spanStart, spanEnd, Spannable.SPAN_INCLUSIVE_INCLUSIVE)
        textView.setTag(R.id.bettermovementmethod_highlight_background_span, highlightSpan)
        Selection.setSelection(text, spanStart, spanEnd)
    }

    /**
     * Determines the touched location inside the TextView's text and returns the ClickableSpan found under it (if any).
     *
     * @return The touched ClickableSpan or null.
     */
    private fun findClickableSpanUnderTouch(
        textView: TextView,
        text: Spannable,
        event: MotionEvent
    ): ClickableSpan? {
        // So we need to find the location in text where touch was made, regardless of whether the TextView
        // has scrollable text. That is, not the entire text is currently visible.
        var touchX = event.x.toInt()
        var touchY = event.y.toInt()

        // Ignore padding.
        touchX -= textView.totalPaddingLeft
        touchY -= textView.totalPaddingTop

        // Account for scrollable text.
        touchX += textView.scrollX
        touchY += textView.scrollY
        val layout = textView.layout
        val touchedLine = layout.getLineForVertical(touchY)
        val touchOffset = layout.getOffsetForHorizontal(touchedLine, touchX.toFloat())
        touchedLineBounds.left = layout.getLineLeft(touchedLine)
        touchedLineBounds.top = layout.getLineTop(touchedLine).toFloat()
        touchedLineBounds.right = layout.getLineWidth(touchedLine) + touchedLineBounds.left
        touchedLineBounds.bottom = layout.getLineBottom(touchedLine).toFloat()
        return if (touchedLineBounds.contains(touchX.toFloat(), touchY.toFloat())) {
            // Find a ClickableSpan that lies under the touched area.
            val spans: Array<ClickableSpan> = text.getSpans(
                touchOffset, touchOffset,
                ClickableSpan::class.java
            )
            for (span in spans) {
                return span
            }
            // No ClickableSpan found under the touched location.
            null
        } else {
            // Touch lies outside the line's horizontal bounds where no spans should exist.
            null
        }
    }

    class LongPressTimer : Runnable {
        private var onTimerReachedListener: OnTimerReachedListener? = null

        interface OnTimerReachedListener {
            fun onTimerReached()
        }

        override fun run() {
            onTimerReachedListener!!.onTimerReached()
        }

        fun setOnTimerReachedListener(listener: OnTimerReachedListener?) {
            onTimerReachedListener = listener
        }
    }

    /**
     * A wrapper to support all [ClickableSpan]s that may or may not provide URLs.
     */
    data class ClickableSpanWithText(
        val span: ClickableSpan,
        val text: String,
        val url: String
    ) {

        companion object {
            fun ofSpan(textView: TextView, span: ClickableSpan): ClickableSpanWithText {
                val s = textView.text as Spanned
                val text = run {
                    val start = s.getSpanStart(span)
                    val end = s.getSpanEnd(span)
                    s.subSequence(start, end).toString()
                }
                return ClickableSpanWithText(
                    span = span,
                    text = text,
                    url = if (span is URLSpan) {
                        span.url
                    } else {
                        text
                    }
                )
            }
        }
    }
}