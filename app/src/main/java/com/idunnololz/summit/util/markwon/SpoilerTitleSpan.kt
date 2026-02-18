package com.idunnololz.summit.util.markwon

import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.TextPaint
import android.text.style.ClickableSpan
import android.util.Log
import android.view.View
import android.widget.TextView
import com.idunnololz.summit.R
import com.idunnololz.summit.lemmy.post.QueryMatchHelper
import com.idunnololz.summit.util.crashLogger.crashLogger
import io.noties.markwon.AbstractMarkwonPlugin
import io.noties.markwon.MarkwonPlugin
import io.noties.markwon.MarkwonVisitor
import io.noties.markwon.core.CorePlugin
import io.noties.markwon.core.MarkwonTheme
import io.noties.markwon.core.spans.BlockQuoteSpan
import io.noties.markwon.image.AsyncDrawableScheduler
import org.commonmark.node.BlockQuote
import org.commonmark.node.CustomBlock
import org.commonmark.node.FencedCodeBlock
import org.commonmark.parser.Parser
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

class SpoilerNode(
  private val delimiter: String,
) : CustomBlock() {
}

class SpoilerPlugin : AbstractMarkwonPlugin() {

  companion object {
    private const val SPOILER_START_REGEX = "(:::\\s*spoiler\\s+)(.*)\n?"
    private const val SPOILER_END_REGEX = ":::(?!\\s*spoiler)"


    private const val COLLAPSED_ARROW = "▶ "
    private const val EXPANDED_ARROW  = "▼ "
  }

  private val spoilerStartMatcher = Pattern.compile(SPOILER_START_REGEX)
  private val spoilerEndMatcher = Pattern.compile(SPOILER_END_REGEX)

  override fun configureParser(builder: Parser.Builder) {
    builder.customBlockParserFactory(SpoilerBlockParser.SpoilerBlockParserFactory())
  }

  override fun configureVisitor(builder: MarkwonVisitor.Builder) {
    super.configureVisitor(builder)

    builder.on<SpoilerBlock?>(
      SpoilerBlock::class.java,
      MarkwonVisitor.NodeVisitor<SpoilerBlock?> { visitor, spoilerBlock ->

        val builder = visitor.builder()

        val headerStart = builder.length
        val titleNode = spoilerBlock.titleNode
        if (titleNode != null) {
          visitor.visitChildren(titleNode)
        }
        val headerEnd = builder.length

        visitor.visitChildren(spoilerBlock)
        visitor.ensureNewLine()

        builder.setSpan(
          DetailsStartSpan(
            visitor.configuration().theme(),
            builder.subSequence(headerStart, headerEnd)
          ),
          headerStart,
          headerEnd,
          Spanned.SPAN_EXCLUSIVE_EXCLUSIVE or Spanned.SPAN_PRIORITY,
        )

        builder.apply {
          val start = builder.length
          append(" ")
          setSpan(
            DetailsEndSpan(),
            start,
            start + 1,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE or Spanned.SPAN_PRIORITY,
          )
        }

        visitor.ensureNewLine()
      },
    )
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
          SpannableStringBuilder()
            .append(detailsStartSpan.title)
            .append(" ▲\n")
        } else {
          SpannableStringBuilder()
            .append(detailsStartSpan.title)
            .append(" ▼\n")
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

        updateHighlightTextData(textView)
      }
    } catch (e: Exception) {
      Log.d(TAG, "Spoiler error", e)
      crashLogger?.recordException(
        RuntimeException("Spoiler error", e),
      )
    }
  }
}

fun updateHighlightTextData(textView: TextView) {
  val highlightTextData =
    textView.getTag(R.id.highlight_text_data) as? QueryMatchHelper.HighlightTextData
      ?: return

  val spanned = SpannableStringBuilder(textView.text)
  val queryHighlight = highlightTextData.query
  val queryLength = queryHighlight.length
  val matchIndex = highlightTextData.matchIndex
  val matchedTextSpans = spanned.getSpans(0, spanned.length, MatchedTextSpan::class.java)

  matchedTextSpans.forEach { it.highlight = false }

  if (matchIndex == null) {
    textView.setTag(R.id.highlight_line_y, null)
    return
  }

  val matchedTextSpan = matchedTextSpans.firstOrNull { it.matchIndex == matchIndex }

  if (matchedTextSpan == null) {
    textView.setTag(R.id.highlight_line_y, null)
    return
  }

  val currentMatchIndex = spanned.getSpanStart(matchedTextSpan)
  matchedTextSpan.highlight = true

  if (currentMatchIndex != -1) {
    val layout = textView.layout

    if (layout == null) {
      textView.setTag(R.id.highlight_line_y, null)
      textView.post {
        updateHighlightTextData(textView)
      }
      return
    }

    val lineY = layout.getLineTop(layout.getLineForOffset(currentMatchIndex))

    textView.setTag(R.id.highlight_line_y, lineY)
  } else {
    textView.setTag(R.id.highlight_line_y, null)
  }
}
