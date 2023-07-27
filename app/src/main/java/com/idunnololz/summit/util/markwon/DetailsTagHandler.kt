package com.idunnololz.summit.util.markwon

import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.TextPaint
import android.text.style.ClickableSpan
import android.view.View
import android.widget.TextView
import com.idunnololz.summit.R
import com.idunnololz.summit.util.ext.getColorCompat
import io.noties.markwon.MarkwonVisitor
import io.noties.markwon.core.MarkwonTheme
import io.noties.markwon.core.spans.BlockQuoteSpan
import io.noties.markwon.html.HtmlTag
import io.noties.markwon.html.MarkwonHtmlRenderer
import io.noties.markwon.html.TagHandler
import io.noties.markwon.image.AsyncDrawableScheduler
import java.util.Collections

class DetailsTagHandler : TagHandler() {

    override fun handle(visitor: MarkwonVisitor, renderer: MarkwonHtmlRenderer, tag: HtmlTag) {
        var summaryEnd = -1
        var summaryStart = -1
        for (child in tag.asBlock.children()) {
            if (!child.isClosed) {
                continue
            }

            if ("summary" == child.name()) {
                summaryStart = child.start()
                summaryEnd = child.end()
            }

            val tagHandler = renderer.tagHandler(child.name())
            if (tagHandler != null) {
                tagHandler.handle(visitor, renderer, child)
            } else if (child.isBlock) {
                visitChildren(visitor, renderer, child.asBlock)
            }
        }

        if (summaryEnd > -1 && summaryStart > -1) {
            val summary = visitor.builder().subSequence(summaryStart, summaryEnd)
            val summarySpan = DetailsSummarySpan(summary)
            visitor.builder().setSpan(summarySpan, summaryStart, summaryEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            visitor.builder().setSpan(DetailsParsingSpan(summarySpan), tag.start(), tag.end(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
    }

    override fun supportedTags(): Collection<String> {
        return Collections.singleton("details")
    }
}

data class DetailsSummarySpan(val text: CharSequence)

enum class DetailsSpanState { DORMANT, CLOSED, OPENED }

data class DetailsParsingSpan(
    val summary: DetailsSummarySpan,
    var state: DetailsSpanState = DetailsSpanState.CLOSED,
)

private var markwonTheme: MarkwonTheme? = null

/**
 * Post-process details statements in the text. They act like `<spoiler>` or `<cut>` tag in some websites
 * @param spanned text to be modified to cut out details tags and insert replacements instead of them
 * @param view resulting text view to accept the modified spanned string
 */
fun postProcessDetails(spanned: SpannableStringBuilder, view: TextView) {
    val markwonTheme = markwonTheme ?: MarkwonTheme.create(view.context).also {
        markwonTheme = it
    }
    val emphasisColor = view.context.getColorCompat(R.color.colorTextTitle)

    val spans = spanned.getSpans(0, spanned.length, DetailsParsingSpan::class.java)
    spans.sortBy { spanned.getSpanStart(it) }

    // if we have no details, proceed as usual (single text-view)
    if (spans.isNullOrEmpty()) {
        // no details
        return
    }

    for (span in spans) {
        val startIdx = spanned.getSpanStart(span)
        val endIdx = spanned.getSpanEnd(span)

        val summaryStartIdx = spanned.getSpanStart(span.summary)
        val summaryEndIdx = spanned.getSpanEnd(span.summary)

        // details tags can be nested, skip them if they were hidden
        if (startIdx == -1 || endIdx == -1) {
            continue
        }

        // replace text inside spoiler tag with just spoiler summary that is clickable
        val summaryText = when (span.state) {
            DetailsSpanState.CLOSED -> "${span.summary.text} ▼\n\n"
            DetailsSpanState.OPENED -> "${span.summary.text} ▲\n\n"
            else -> ""
        }

        when (span.state) {
            DetailsSpanState.CLOSED -> {
                span.state = DetailsSpanState.DORMANT
                spanned.removeSpan(span.summary) // will be added later

                // spoiler tag must be closed, all the content under it must be hidden

                // retrieve content under spoiler tag and hide it
                // if it is shown, it should be put in blockquote to distinguish it from text before and after
                val innerSpanned = spanned.subSequence(summaryEndIdx, endIdx) as SpannableStringBuilder
                spanned.replace(summaryStartIdx, endIdx, summaryText)
                spanned.setSpan(span.summary, startIdx, startIdx + summaryText.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)

                // expand text on click
                val wrapper = object : DetailsClickableSpan() {

                    // replace wrappers with real previous spans on click
                    override fun onClick(widget: View) {
                        span.state = DetailsSpanState.OPENED

                        val start = spanned.getSpanStart(this)
                        val end = spanned.getSpanEnd(this)

                        spanned.removeSpan(this)
                        spanned.insert(end, innerSpanned)

                        // make details span cover all expanded text
                        spanned.removeSpan(span)
                        spanned.setSpan(span, start, end + innerSpanned.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)

                        // edge-case: if the span around this text is now too short, expand it as well
                        spanned.getSpans(end, end, Any::class.java)
                            .filter { spanned.getSpanEnd(it) == end }
                            .forEach {
                                if (it is DetailsSummarySpan) {
                                    // don't expand summaries, they are meant to end there
                                    return@forEach
                                }

                                val bqStart = spanned.getSpanStart(it)
                                spanned.removeSpan(it)
                                spanned.setSpan(it, bqStart, end + innerSpanned.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                            }

                        postProcessDetails(spanned, view)

                        view.text = spanned
                        AsyncDrawableScheduler.schedule(view)
                    }

                    override fun updateDrawState(ds: TextPaint) {
                        ds.color = emphasisColor
                    }
                }
                spanned.setSpan(wrapper, startIdx, startIdx + summaryText.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            }

            DetailsSpanState.OPENED -> {
                span.state = DetailsSpanState.DORMANT

                // put the hidden text into blockquote if needed
                var bq = spanned.getSpans(summaryEndIdx, endIdx, BlockQuoteSpan::class.java)
                    .firstOrNull { spanned.getSpanStart(it) == summaryEndIdx && spanned.getSpanEnd(it) == endIdx }
                if (bq == null) {
                    bq = BlockQuoteSpan(markwonTheme)
                    spanned.setSpan(bq, summaryEndIdx, endIdx, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                }

                // content under spoiler tag is shown, but should be hidden again on click
                // change summary text to opened variant
                spanned.replace(summaryStartIdx, summaryEndIdx, summaryText)
                spanned.setSpan(span.summary, startIdx, startIdx + summaryText.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                val wrapper = object : DetailsClickableSpan() {

                    // hide text again on click
                    override fun onClick(widget: View) {
                        span.state = DetailsSpanState.CLOSED

                        spanned.removeSpan(this)

                        postProcessDetails(spanned, view)

                        view.text = spanned
                    }

                    override fun updateDrawState(ds: TextPaint) {
                        ds.color = emphasisColor
                    }
                }
                spanned.setSpan(wrapper, startIdx, startIdx + summaryText.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            }

            DetailsSpanState.DORMANT -> {
                // this state is present so that details spans that were already processed won't be processed again
                // nothing should be done
            }
        }
    }
}

abstract class DetailsClickableSpan : ClickableSpan()
