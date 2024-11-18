package com.idunnololz.summit.lemmy

import android.content.Context
import android.graphics.RectF
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.BackgroundColorSpan
import android.text.style.ForegroundColorSpan
import android.text.util.Linkify
import android.util.Log
import android.widget.TextView
import coil.imageLoader
import com.idunnololz.summit.R
import com.idunnololz.summit.lemmy.post.QueryMatchHelper.HighlightTextData
import com.idunnololz.summit.links.LinkContext
import com.idunnololz.summit.links.LinkResolver
import com.idunnololz.summit.util.ContentUtils.isUrlImage
import com.idunnololz.summit.util.ContentUtils.isUrlVideo
import com.idunnololz.summit.util.CustomLinkMovementMethod
import com.idunnololz.summit.util.DefaultLinkLongClickListener
import com.idunnololz.summit.util.ImagesAsLinksPlugin
import com.idunnololz.summit.util.LinkUtils
import com.idunnololz.summit.util.coil.CoilImagesPlugin
import com.idunnololz.summit.util.ext.getColorCompat
import com.idunnololz.summit.util.ext.getColorFromAttribute
import com.idunnololz.summit.util.markwon.SpoilerPlugin
import com.idunnololz.summit.util.markwon.SummitInlineParser
import io.noties.markwon.AbstractMarkwonPlugin
import io.noties.markwon.Markwon
import io.noties.markwon.core.MarkwonTheme
import io.noties.markwon.ext.strikethrough.StrikethroughPlugin
import io.noties.markwon.ext.tables.TablePlugin
import io.noties.markwon.html.span.SubScriptSpan
import io.noties.markwon.html.span.SuperScriptSpan
import io.noties.markwon.linkify.LinkifyPlugin
import io.noties.markwon.simple.ext.SimpleExtPlugin
import java.util.Locale
import java.util.regex.Matcher
import java.util.regex.Pattern
import org.commonmark.parser.Parser

object LemmyTextHelper {
    private const val TAG = "LemmyTextHelper"

    private var markwon: Markwon? = null
    private var noMediaMarkwon: Markwon? = null

    var autoLinkPhoneNumbers: Boolean = true

    fun bindText(
        textView: TextView,
        text: String,
        instance: String,
        spannedText: Spanned? = null,
        highlight: HighlightTextData? = null,
        showMediaAsLinks: Boolean = false,
        onImageClick: (url: String) -> Unit,
        onVideoClick: (url: String) -> Unit,
        onPageClick: (PageRef) -> Unit,
        onLinkClick: (url: String, text: String, linkContext: LinkContext) -> Unit,
        onLinkLongClick: (url: String, text: String) -> Unit,
    ): Spanned? {
        val spannable = bindLemmyText(textView, text, spannedText, highlight, showMediaAsLinks)

        textView.movementMethod = CustomLinkMovementMethod().apply {
            onLinkClickListener = object : CustomLinkMovementMethod.OnLinkClickListener {
                override fun onClick(
                    textView: TextView,
                    url: String,
                    text: String,
                    rect: RectF,
                ): Boolean {
                    val pageRef = LinkResolver.parseUrl(url, instance)

                    if (isUrlVideo(url)) {
                        onVideoClick(url)
                    } else if (isUrlImage(url)) {
                        onImageClick(url)
                    } else if (pageRef != null) {
                        onPageClick(pageRef)
                    } else {
                        onLinkClick(url, text, LinkContext.Text)
                    }
                    return true
                }
            }
            onLinkLongClickListener = DefaultLinkLongClickListener(textView.context, onLinkLongClick)
            this.onImageClickListener = { url ->
                if (isUrlVideo(url)) {
                    onVideoClick(url)
                } else {
                    onImageClick(url)
                }
            }
        }

        return spannable
    }

    fun getSpannable(context: Context, text: String): Spanned = try {
        val markwon = getMarkwon(context)

        markwon.toMarkdown(text)
    } catch (e: Exception) {
        // java.lang.IndexOutOfBoundsException: setSpan (2445 ... 2449) ends beyond length 2448

        Log.e(TAG, "Error parsing text", e)

        SpannableStringBuilder(text)
    }

    private fun bindLemmyText(
        textView: TextView,
        text: String,
        spannableText: Spanned? = null,
        highlight: HighlightTextData?,
        showMediaAsLinks: Boolean,
    ): Spanned? {
        val markwon = if (showMediaAsLinks) {
            getNoMediaMarkwon(textView.context)
        } else {
            getMarkwon(textView.context)
        }

        return try {
            val _spanned = spannableText ?: markwon.toMarkdown(text)
            val spanned = SpannableStringBuilder(_spanned)

            if (highlight != null) {
                val highlightColor = textView.context.getColorFromAttribute(
                    com.google.android.material.R.attr.colorSurfaceInverse,
                )
                val highlightTextColor = textView.context.getColorFromAttribute(
                    com.google.android.material.R.attr.colorOnSurfaceInverse,
                )
                val currentQueryColor = textView.context.getColorFromAttribute(
                    androidx.appcompat.R.attr.colorPrimary,
                )
                val queryHighlight = highlight.query
                val queryLength = queryHighlight.length
                var curStartIndex = 0
                var matchIndex = 0
                while (true) {
                    val index =
                        spanned.indexOf(queryHighlight, curStartIndex, ignoreCase = true)

                    if (index < 0) {
                        break
                    }

                    if (highlight.matchIndex == matchIndex) {
                        spanned.setSpan(
                            BackgroundColorSpan(currentQueryColor),
                            index,
                            index + queryLength,
                            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE,
                        )
                    } else {
                        spanned.setSpan(
                            BackgroundColorSpan(highlightColor),
                            index,
                            index + queryLength,
                            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE,
                        )
                        spanned.setSpan(
                            ForegroundColorSpan(highlightTextColor),
                            index,
                            index + queryLength,
                            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE,
                        )
                    }
                    curStartIndex = index + queryLength

                    matchIndex++
                }
            }

            markwon.setParsedMarkdown(textView, spanned)

            _spanned
        } catch (e: Exception) {
            Log.w(TAG, "Error parsing markdown. Falling back to plain text", e)
            textView.text = text

            null
        }
    }

    private class LemmyPlugin(
        private val context: Context,
    ) : AbstractMarkwonPlugin() {

        /**
         * Combination of 4 regexes "or'd" (|) together.
         * 1) Matches against words where the first character is a caret. Eg. '^hello'
         * 2) Matches against poorly formatted header tags. Eg. `##Asdf` (proper would be `## Asdf`)
         * 3) Matches against full community names (!a@b.com)
         */
        private val largeRegex = Pattern.compile(
            """\^(\S+)|(?m)^(#+)(\S*.*)${'$'}|(]\()?(!|/?[cC]/|@|/?[uU]/)([^@\s]+)@([^@\s]+\.[^@\s)]*\w)""",
        )

        private fun processAll(s: String): String {
            val matcher = largeRegex.matcher(s)
            val sb = StringBuffer()
            while (matcher.find()) {
                val g1 = matcher.group(1)
                if (g1 != null && !g1.endsWith("^")) {
                    matcher.appendReplacement(sb, "^${matcher.group(1)}^")
                    continue
                }

                val formattingChar = matcher.group(2)?.trim()
                val rest = matcher.group(3)?.trim()
                if (formattingChar != null && rest != null) {
                    matcher.appendReplacement(
                        sb,
                        Matcher.quoteReplacement("$formattingChar $rest"),
                    )
                    Log.d(TAG, "Fixed ${"$formattingChar $rest"}")
                }

                val linkStart = matcher.group(4)
                val referenceTypeToken = matcher.group(5)
                val name = matcher.group(6)
                val instance = matcher.group(7)

                if (linkStart == null &&
                    referenceTypeToken != null &&
                    name != null &&
                    instance != null &&
                    !name.contains("]") &&
                    !instance.contains("]") /* make sure we are not within a link def */
                ) {
                    when (referenceTypeToken.lowercase(Locale.US)) {
                        "!", "c/", "/c/" -> {
                            val communityRef = CommunityRef.CommunityRefByName(name, instance)

                            matcher.appendReplacement(
                                sb,
                                "[${matcher.group(
                                    0,
                                )}](${LinkUtils.getLinkForCommunity(communityRef)})",
                            )
                        }
                        "@", "u/", "/u/" -> {
                            val url = LinkUtils.getLinkForPerson(instance = instance, name = name)
                            matcher.appendReplacement(
                                sb,
                                "[${matcher.group(0)}]($url)",
                            )
                        }
                    }
                }
            }
            matcher.appendTail(sb)
            return sb.toString()
        }

        override fun processMarkdown(markdown: String): String = processAll(markdown)

        override fun configureTheme(builder: MarkwonTheme.Builder) {
            builder.blockQuoteColor(context.getColorCompat(R.color.colorQuoteLine))
                .thematicBreakColor(context.getColorCompat(R.color.colorQuoteLine))
                .headingBreakHeight(0)
        }
    }

    fun resetMarkwon(context: Context) {
        markwon = createMarkwon(context, inlineMedia = true)
        noMediaMarkwon = null
    }

    fun getMarkwonTheme(context: Context): MarkwonTheme {
        return getMarkwon(context).configuration().theme()
    }

    private fun getMarkwon(context: Context) =
        markwon ?: createMarkwon(context, inlineMedia = true).also {
            markwon = it
        }

    private fun getNoMediaMarkwon(context: Context) =
        noMediaMarkwon ?: createMarkwon(context, inlineMedia = false).also {
            noMediaMarkwon = it
        }

    private fun createMarkwon(context: Context, inlineMedia: Boolean): Markwon =
        Markwon.builder(context)
            .apply {
                if (inlineMedia) {
                    usePlugin(
                        CoilImagesPlugin.create(context, context.applicationContext.imageLoader),
                    )
                } else {
                    usePlugin(ImagesAsLinksPlugin())
                }
            }
            .usePlugin(
                LinkifyPlugin.create(
                    if (autoLinkPhoneNumbers) {
                        Linkify.EMAIL_ADDRESSES or Linkify.PHONE_NUMBERS or Linkify.WEB_URLS
                    } else {
                        Linkify.EMAIL_ADDRESSES or Linkify.WEB_URLS
                    },
                ),
            )
            .usePlugin(TablePlugin.create(context))
            .usePlugin(LemmyPlugin(context))
            .usePlugin(StrikethroughPlugin.create())
            .usePlugin(SpoilerPlugin())
            .usePlugin(
                object : AbstractMarkwonPlugin() {
                    override fun configureParser(builder: Parser.Builder) {
                        super.configureParser(builder)
                        builder.inlineParserFactory {
                            SummitInlineParser(it)
                        }
                    }
                },
            )
            .usePlugin(
                SimpleExtPlugin.create().apply {
                    addExtension(1, '^') { _, _ ->
                        SuperScriptSpan()
                    }
                },
            )
            .usePlugin(
                SimpleExtPlugin.create().apply {
                    addExtension(1, '~') { _, _ ->
                        SubScriptSpan()
                    }
                },
            )
            .build()
}
