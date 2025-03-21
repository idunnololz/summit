package com.idunnololz.summit.util.markwon

import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.TextPaint
import android.text.style.ClickableSpan
import android.util.Log
import android.view.View
import android.widget.TextView
import com.idunnololz.summit.util.crashlytics
import io.noties.markwon.AbstractMarkwonPlugin
import io.noties.markwon.MarkwonPlugin
import io.noties.markwon.MarkwonVisitor
import io.noties.markwon.core.CorePlugin
import io.noties.markwon.core.MarkwonTheme
import io.noties.markwon.core.spans.BlockQuoteSpan
import io.noties.markwon.image.AsyncDrawableScheduler

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

    override fun configure(registry: MarkwonPlugin.Registry) {
        registry.require(CorePlugin::class.java) {
            it.addOnTextAddedListener(
                SpoilerTextAddedListener(),
            )
        }
    }

    private class SpoilerTextAddedListener : CorePlugin.OnTextAddedListener {
        override fun onTextAdded(visitor: MarkwonVisitor, text: String, start: Int) {
            val spoilerTitleRegex = Regex("(:::\\s*spoiler\\s+)(.*)")
            val spoilerTitles = spoilerTitleRegex.findAll(text)

            for (match in spoilerTitles) {
                val spoilerTitle = match.groups[2]!!.value
                visitor.builder().setSpan(
                    DetailsStartSpan(visitor.configuration().theme(), spoilerTitle),
                    start,
                    start + match.groups[2]!!.range.last,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE,
                )
            }

            val spoilerCloseRegex = Regex("^(?!.*spoiler).*:::")
            val spoilerCloses = spoilerCloseRegex.findAll(text)
            for (match in spoilerCloses) {
                visitor.builder().apply {
                    if (start + 4 >= this.length) {
                        append(" ")
                    }
                    setSpan(DetailsEndSpan(), start, start + 4, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                }
            }
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
                    val spoilerCloseSpan = detailsEndSpans[index]
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
                            spoilerEnd - 4,
                        ) as SpannableStringBuilder

                    if (spoilerContent.last() != '\n') {
                        spoilerContent.append("\n")
                    }

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
            crashlytics?.recordException(
                RuntimeException("Spoiler error", e),
            )
        }
    }
}
