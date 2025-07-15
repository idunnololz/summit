package com.idunnololz.summit.util.markwon

import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.TextPaint
import android.text.style.ClickableSpan
import android.util.Log
import android.view.View
import android.widget.TextView
import com.idunnololz.summit.util.crashLogger.crashLogger
import io.noties.markwon.AbstractMarkwonPlugin
import io.noties.markwon.MarkwonPlugin
import io.noties.markwon.MarkwonVisitor
import io.noties.markwon.core.CorePlugin
import io.noties.markwon.core.MarkwonTheme
import io.noties.markwon.core.spans.BlockQuoteSpan
import io.noties.markwon.image.AsyncDrawableScheduler
import java.util.regex.Pattern

abstract class DetailsClickableSpan : ClickableSpan()

data class DetailsStartSpan(
  val theme: MarkwonTheme,
  val title: CharSequence,
  var isExpanded: Boolean = false,
  var isProcessed: Boolean = false,
) {
  // Don't generate hashCode due to stackoverflow from self reference
  var spoilerText: SpannableStringBuilder? = null
}

class DetailsEndSpan

private const val TAG = "SpoilerPlugin"

class SpoilerPlugin : AbstractMarkwonPlugin() {

  companion object {
    private const val SPOILER_START_REGEX = "(:::\\s*spoiler\\s+)(.*)\n?"
    private const val SPOILER_END_REGEX = ":::(?!\\s*spoiler)"
  }

  private val spoilerStartMatcher = Pattern.compile(SPOILER_START_REGEX)
  private val spoilerEndMatcher = Pattern.compile(SPOILER_END_REGEX)

  override fun configure(registry: MarkwonPlugin.Registry) {
    registry.require(CorePlugin::class.java) {
      it.addOnTextAddedListener(object : CorePlugin.OnTextAddedListener {
        override fun onTextAdded(visitor: MarkwonVisitor, text: String, start: Int) {
          val startMatcher = spoilerStartMatcher.matcher(text)
          val endMatcher = spoilerEndMatcher.matcher(text)

          while (startMatcher.find()) {
            val spoilerTitle = startMatcher.group(2)!!
            visitor.builder().setSpan(
              DetailsStartSpan(visitor.configuration().theme(), spoilerTitle),
              start + startMatcher.start(),
              start + startMatcher.end(2),
              Spanned.SPAN_EXCLUSIVE_EXCLUSIVE or Spanned.SPAN_PRIORITY,
            )
          }
          while (endMatcher.find()) {
            visitor.builder().apply {
              if (start + 4 >= this.length) {
                append(" ")
              }
              setSpan(
                DetailsEndSpan(),
                start + endMatcher.start(),
                start + endMatcher.end() + 1,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE or Spanned.SPAN_PRIORITY,
              )
            }
          }
        }
      })
    }
  }

  override fun afterSetText(textView: TextView) {
    processSpoilers(textView, resetProcessed = true)
  }

  private fun processSpoilers(textView: TextView, resetProcessed: Boolean) {
    try {
      val spanned = SpannableStringBuilder(textView.text)
      val detailsStartSpans =
        spanned.getSpans(0, spanned.length, DetailsStartSpan::class.java)
      val detailsEndSpans =
        spanned.getSpans(0, spanned.length, DetailsEndSpan::class.java)

      detailsStartSpans.sortBy { spanned.getSpanStart(it) }
      detailsEndSpans.sortBy { spanned.getSpanStart(it) }

      if (resetProcessed) {
        detailsStartSpans.forEach {
          it.isProcessed = false
        }
      }

      for ((index, detailsStartSpan) in detailsStartSpans.withIndex()) {
        if (detailsStartSpan.isProcessed) {
          continue
        }

        val spoilerStart = spanned.getSpanStart(detailsStartSpan)

        var spoilerEnd = spanned.length
        if (index < detailsEndSpans.size) {
          val spoilerCloseSpan = detailsEndSpans.getOrNull(index)

          if (spoilerCloseSpan == null) {
            continue // something is malformed...
          }

          spoilerEnd = spanned.getSpanEnd(spoilerCloseSpan)
        }

        // The space at the end is necessary for the lengths to be the same
        // This reduces complexity as else it would need complex logic to determine the replacement length
        val spoilerTitle = if (detailsStartSpan.isExpanded) {
          "${detailsStartSpan.title} ▲\n"
        } else {
          "${detailsStartSpan.title} ▼\n"
        }

        if (detailsStartSpan.spoilerText == null) {
          val spoilerContent =
            spanned.subSequence(
              spanned.getSpanEnd(detailsStartSpan) + 1,
              // -5 because -4 for the spoiler end tag and then we remove the newline character
              // added by LemmyTextHelper. See LemmyPlugin.processAll()
              spoilerEnd - 5,
            ) as SpannableStringBuilder

          spoilerContent.setSpan(
            BlockQuoteSpan(detailsStartSpan.theme),
            0,
            spoilerContent.length,
            // SPAN_PRIORITY makes sure this span has highest priority, so it is always applied first
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE or Spanned.SPAN_PRIORITY,
          )

          detailsStartSpan.spoilerText = spoilerContent
        }

        // Remove spoiler content from span
        spanned.replace(spoilerStart, spoilerEnd - 1, spoilerTitle)
        // Set span block title
        spanned.setSpan(
          detailsStartSpan,
          spoilerStart,
          spoilerStart + spoilerTitle.length,
          Spanned.SPAN_EXCLUSIVE_EXCLUSIVE,
        )

        if (detailsStartSpan.isExpanded) {
          spanned.replace(
            spoilerStart,
            spoilerStart + spoilerTitle.length,
            spoilerTitle,
          )
          spanned.insert(
            spoilerStart + spoilerTitle.length,
            detailsStartSpan.spoilerText,
          )
          spanned.setSpan(
            detailsStartSpan,
            spoilerStart,
            spoilerStart + spoilerTitle.length,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE,
          )
        }

        val wrapper =
          object : DetailsClickableSpan() {
            override fun onClick(p0: View) {
              detailsStartSpan.isExpanded = !detailsStartSpan.isExpanded
              detailsStartSpan.isProcessed = false
              processSpoilers(textView, resetProcessed = false)
              AsyncDrawableScheduler.schedule(textView)
            }

            override fun updateDrawState(ds: TextPaint) {
            }
          }

        // Set spoiler block type as ClickableSpan
        spanned.setSpan(
          wrapper,
          spoilerStart,
          spoilerStart + spoilerTitle.length,
          Spanned.SPAN_EXCLUSIVE_EXCLUSIVE,
        )

        detailsStartSpan.isProcessed = true
        textView.text = spanned
      }
    } catch (e: Exception) {
      Log.d(TAG, "Spoiler error", e)
      crashLogger?.recordException(
        RuntimeException("Spoiler error", e),
      )
    }
  }
}
