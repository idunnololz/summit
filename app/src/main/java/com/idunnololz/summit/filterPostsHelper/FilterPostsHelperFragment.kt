package com.idunnololz.summit.filterPostsHelper

import android.graphics.RectF
import android.os.Bundle
import android.text.Layout
import android.text.Selection
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
import android.text.TextPaint
import android.text.method.LinkMovementMethod
import android.text.style.BackgroundColorSpan
import android.text.style.ClickableSpan
import android.text.style.URLSpan
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.navArgs
import com.idunnololz.summit.R
import com.idunnololz.summit.api.dto.lemmy.PostView
import com.idunnololz.summit.databinding.FragmentFilterPostsHelperBinding
import com.idunnololz.summit.util.BaseDialogFragment
import com.idunnololz.summit.util.Utils
import com.idunnololz.summit.util.ext.getColorFromAttribute
import com.idunnololz.summit.util.markwon.DetailsClickableSpan
import dagger.hilt.android.AndroidEntryPoint
import io.noties.markwon.ext.tables.TableRowSpan
import io.noties.markwon.image.AsyncDrawableSpan
import kotlinx.coroutines.launch

@AndroidEntryPoint
class FilterPostsHelperFragment : BaseDialogFragment<FragmentFilterPostsHelperBinding>() {

  companion object {
    fun show(fm: FragmentManager, postView: PostView) {
      FilterPostsHelperFragment()
        .apply {
          arguments = FilterPostsHelperFragmentArgs(postView).toBundle()
        }
        .show(fm, "FilterPostsHelperFragment")
    }
  }

  private val args by navArgs<FilterPostsHelperFragmentArgs>()
  private val viewModel: FilterPostsHelperViewModel by viewModels()

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?,
  ): View {
    super.onCreateView(inflater, container, savedInstanceState)

    setBinding(FragmentFilterPostsHelperBinding.inflate(inflater, container, false))

    return binding.root
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)

    val context = requireContext()

    with(binding) {
      val movementMethod = CustomMovementMethod()
      val linkResolver: (View, SelectableSpan) -> Unit = { view, span ->
        span.selected = !span.selected
        postContent.invalidate()
      }
      val spans = mutableListOf<SelectableSpan>()

      postContent.text = SpannableStringBuilder().apply {
        args.postView.post.name.split(" ").map { token ->
          val s = length
          append(token)
          val e = length
          val span = SelectableSpan(token, linkResolver, context.getColorFromAttribute(androidx.appcompat.R.attr.colorPrimary))
          spans += span
          setSpan(span, s, e, SPAN_EXCLUSIVE_EXCLUSIVE)
          append(" ")
        }
      }.trim()

      postContent.movementMethod = movementMethod

      filterButton.setOnClickListener {
        getMainActivity()?.showPostsKeywordFilters()
      }
      positiveButton.setOnClickListener {
        viewLifecycleOwner.lifecycleScope.launch {
          val filters =
            spans
              .filter { it.selected && it.text.isNotBlank() }
              .map { it.text }

          viewModel.addContentFilter(filters)

          dismiss()

          if (filters.isNotEmpty()) {
            Toast.makeText(context, R.string.filters_added, Toast.LENGTH_SHORT).show()
          }
        }
      }
      negativeButton.setOnClickListener {
        dismiss()
      }
    }
  }

  private class CustomMovementMethod() : LinkMovementMethod() {

    private val touchedLineBounds = RectF()
    private var isUrlHighlighted = false
    private var clickableSpanUnderTouchOnActionDown: Any? = null
    private var activeTextViewHashcode = 0
    private var ongoingLongPressTimer: LongPressTimer? = null
    private var wasLongPressRegistered = false

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

      val clickableSpanUnderTouch: Any? =
        findClickableSpanUnderTouch(textView, text, event)
      if (event.action == MotionEvent.ACTION_DOWN) {
        clickableSpanUnderTouchOnActionDown = clickableSpanUnderTouch
      }
      val touchStartedOverAClickableSpan =
        clickableSpanUnderTouchOnActionDown != null

      return when (event.action) {
        MotionEvent.ACTION_DOWN -> {
          clickableSpanUnderTouch?.let { highlightUrl(textView, it, text) }
          touchStartedOverAClickableSpan
        }
        MotionEvent.ACTION_UP -> {
          // Register a click only if the touch started and ended on the same URL.
          if (!wasLongPressRegistered &&
            touchStartedOverAClickableSpan &&
            clickableSpanUnderTouch === clickableSpanUnderTouchOnActionDown
          ) {
            if (clickableSpanUnderTouch != null) {
              dispatchClick(
                textView,
                clickableSpanUnderTouch,
                RectF(
                  event.x - Utils.convertDpToPixel(16f),
                  event.y - Utils.convertDpToPixel(16f),
                  event.x + Utils.convertDpToPixel(16f),
                  event.y + Utils.convertDpToPixel(16f),
                ),
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

    private fun dispatchClick(textView: TextView, clickableSpan: Any, bounds: RectF) {
      if (clickableSpan is SelectableSpan) {
        clickableSpan.onClick(textView)
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
        textView.getTag(R.id.bettermovementmethod_highlight_background_span)
          as? BackgroundColorSpan
          ?: return
      text.removeSpan(highlightSpan)
      Selection.removeSelection(text)
    }

    /**
     * Adds a background color span at <var>clickableSpan</var>'s location.
     */
    private fun highlightUrl(textView: TextView, clickableSpan: Any?, text: Spannable) {
      if (isUrlHighlighted) {
        return
      }
      isUrlHighlighted = true
      val spanStart = text.getSpanStart(clickableSpan)
      val spanEnd = text.getSpanEnd(clickableSpan)
      val highlightSpan = BackgroundColorSpan(textView.highlightColor)

      if (spanStart < 0 || spanEnd < 0) {
        return // tables dont really support highlighting
      }

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
      event: MotionEvent,
    ): Any? {
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
        val s = findClickableSpanInTable(textView, text, event)
        if (s != null) {
          return s
        }

        // Find a ClickableSpan that lies under the touched area.
        val clickableSpans: Array<ClickableSpan> = text.getSpans(
          touchOffset,
          touchOffset,
          ClickableSpan::class.java,
        )
        for (span in clickableSpans) {
          return span
        }

        val drawableSpans = text.getSpans(
          touchOffset,
          touchOffset,
          AsyncDrawableSpan::class.java,
        )
        for (span in drawableSpans) {
          return span
        }
        // No ClickableSpan found under the touched location.
        null
      } else {
        // Touch lies outside the line's horizontal bounds where no spans should exist.
        null
      }
    }

    private fun findClickableSpanInTable(
      widget: TextView,
      buffer: Spannable,
      event: MotionEvent,
    ): Any? {
//        // handle only action up (originally action down is used in order to handle selection,
//        //  which tables do no have)
//        if (event.action != MotionEvent.ACTION_UP) {
//            return false
//        }
      var x = event.x.toInt()
      var y = event.y.toInt()
      x -= widget.totalPaddingLeft
      y -= widget.totalPaddingTop
      x += widget.scrollX
      y += widget.scrollY
      val layout: Layout = widget.layout
      val line: Int = layout.getLineForVertical(y)
      val off: Int = layout.getOffsetForHorizontal(line, x.toFloat())
      val spans = buffer.getSpans(off, off, TableRowSpan::class.java)
      if (spans.isEmpty()) {
        return null
      }
      val span = spans[0]

      // okay, we can calculate the x to obtain span, but what about y?
      val rowLayout: Layout? = span.findLayoutForHorizontalOffset(x)
      if (rowLayout != null) {
        // line top as basis
        val rowY: Int = layout.getLineTop(line)
        val rowLine: Int = rowLayout.getLineForVertical(y - rowY)
        val rowOffset: Int = rowLayout.getOffsetForHorizontal(
          rowLine,
          (x % span.cellWidth()).toFloat(),
        )

        val rowDetailsClickableSpans = (rowLayout.text as Spanned)
          .getSpans(rowOffset, rowOffset, DetailsClickableSpan::class.java)
        if (rowDetailsClickableSpans.isNotEmpty()) {
          return rowDetailsClickableSpans[0]
        }

        val rowClickableSpans = (rowLayout.text as Spanned)
          .getSpans(rowOffset, rowOffset, ClickableSpan::class.java)
        if (rowClickableSpans.isNotEmpty()) {
          return rowClickableSpans[0]
        }

        val rowDrawableSpans = (rowLayout.text as Spanned)
          .getSpans(rowOffset, rowOffset, AsyncDrawableSpan::class.java)
        if (rowDrawableSpans.isNotEmpty()) {
          return rowDrawableSpans[0]
        }
      }
      return null
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
      val url: String,
    ) {

      companion object {
        fun ofSpan(textView: TextView, span: ClickableSpan): ClickableSpanWithText? {
          val s = textView.text as Spanned
          val start = s.getSpanStart(span)
          val end = s.getSpanEnd(span)

          if (start < 0 || end < 0) {
            return null
          }

          val text = s.subSequence(start, end).toString()
          return ClickableSpanWithText(
            span = span,
            text = text,
            url = if (span is URLSpan) {
              span.url
            } else {
              text
            },
          )
        }
      }
    }
  }

  class SelectableSpan(
    val text: String,
    private val resolver: (View, SelectableSpan) -> Unit,
    val selectedColor: Int,
    var selected: Boolean = false,
  ) : URLSpan(text) {

    override fun onClick(widget: View) {
      resolver(widget, this)
    }

    override fun updateDrawState(ds: TextPaint) {
      if (selected) {
        ds.setColor(selectedColor)
        ds.isUnderlineText = true
      }
    }
  }

}