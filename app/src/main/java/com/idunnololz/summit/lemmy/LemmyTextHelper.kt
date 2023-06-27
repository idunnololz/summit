package com.idunnololz.summit.lemmy

import android.content.Context
import android.graphics.RectF
import android.util.Log
import android.widget.TextView
import com.idunnololz.summit.R
import com.idunnololz.summit.spans.SpoilerSpan
import com.idunnololz.summit.util.CustomLinkMovementMethod
import com.idunnololz.summit.util.DefaultLinkLongClickListener
import com.idunnololz.summit.util.LinkUtils
import com.idunnololz.summit.util.ext.getColorCompat
import io.noties.markwon.AbstractMarkwonPlugin
import io.noties.markwon.Markwon
import io.noties.markwon.core.MarkwonTheme
import io.noties.markwon.ext.strikethrough.StrikethroughPlugin
import io.noties.markwon.ext.tables.TablePlugin
import io.noties.markwon.html.HtmlPlugin
import io.noties.markwon.html.span.SuperScriptSpan
import io.noties.markwon.image.coil.CoilImagesPlugin
import io.noties.markwon.linkify.LinkifyPlugin
import io.noties.markwon.simple.ext.SimpleExtPlugin
import java.util.regex.Matcher
import java.util.regex.Pattern

object LemmyTextHelper {

    private const val TAG = "LemmyTextHelper"

    fun bindText(
        textView: TextView,
        text: String,
        instance: String,

        onImageClickListener: (url: String) -> Unit,
        onPageClick: (PageRef) -> Unit,
    ) {
        bindLemmyText(textView, text, instance)
        textView.movementMethod = CustomLinkMovementMethod().apply {
            onLinkClickListener = object : CustomLinkMovementMethod.OnLinkClickListener {
                override fun onClick(
                    textView: TextView,
                    url: String,
                    text: String,
                    rect: RectF
                ): Boolean {
                    val pageRef = LinkResolver.parseUrl(url, instance)

                    if (pageRef != null) {
                        onPageClick(pageRef)
                    }
                    return pageRef != null
                }
            }
            onLinkLongClickListener = DefaultLinkLongClickListener(textView.context)
            this.onImageClickListener = onImageClickListener
        }
    }

    private fun bindLemmyText(textView: TextView, text: String, instance: String) {
        createMarkwon(textView.context, instance).setMarkdown(textView, text)
    }

    private class LemmyPlugin(
        private val context: Context,
        private val instance: String,
    ) : AbstractMarkwonPlugin() {

        /**
         * Matches against words where the first character is a caret. Eg. '^hello'
         */
        private val superscriptRegex = Pattern.compile("""\^(\S+)""")

        /**
         * Matches against strings wrapped in '!>', '<!'. Eg. '!>this is a spoiler<!'
         */
        private val spoilerBlockRegex = Pattern.compile(""":::\s*spoiler\s+(.*)\n((?:.*\n*)*?)(:::\n|${'$'})\s*""")

        /**
         * Matches against poorly formated header tags. Eg. `##Asdf` (proper would be `## Asdf`)
         */
        private val improperHeaderTagRegex = Pattern.compile("""(?m)^(#+)(\S*.*)${'$'}""")

        /**
         * Matches against subreddit names. DOES NOT MATCH SUBREDDIT URLS. (technically incomplete because it doesn't have the length limit)
         */
        private val communityNameRegex =
            Pattern.compile("""(^|\s)(/?\bc/([a-zA-Z0-9-][a-zA-Z0-9-_]*)\b)""")

        private fun processSubTags(s: String): String {
            val matcher = superscriptRegex.matcher(s)
            val sb = StringBuffer()
            while (matcher.find()) {
                val g1 = matcher.group(1)
                if (!g1.endsWith("^")) {
                    matcher.appendReplacement(sb, "^${matcher.group(1)}^")
                }
            }
            matcher.appendTail(sb)
            return sb.toString()
        }

        private fun processSpoilerTags(s: String): String {
            val matcher = spoilerBlockRegex.matcher(s)
            val sb = StringBuffer()
            while (matcher.find()) {
                val spoilerTitle: String? = matcher.group(1)?.trim()
                val spoilerText: String? = matcher.group(2)?.trim()
                if (!spoilerTitle.isNullOrBlank() && !spoilerText.isNullOrBlank()) {
                    matcher.appendReplacement(sb, "<br>${spoilerTitle}<br>++${spoilerText}++<br>")
                }
            }
            matcher.appendTail(sb)
            return sb.toString()
        }

        private fun processHeaderTags(s: String): String {
            val matcher = improperHeaderTagRegex.matcher(s)
            val sb = StringBuffer()
            while (matcher.find()) {
                try {
                    val formattingChar = requireNotNull(matcher.group(1)).trim()
                    val rest = requireNotNull(matcher.group(2)).trim()
                    matcher.appendReplacement(
                        sb,
                        Matcher.quoteReplacement("$formattingChar $rest")
                    )
                    Log.d(TAG, "Fixed ${"$formattingChar $rest"}")
                } catch (e: Exception) {
                    throw RuntimeException("Error parsing header tag: $s.", e)
                }
            }
            matcher.appendTail(sb)
            return sb.toString()
        }

        private fun processSubredditMentions(s: String, instance: String): String {
            val matcher = communityNameRegex.matcher(s)
            val sb = StringBuffer()
            while (matcher.find()) {
                val communityUrl = matcher.group(3) ?: continue
                val spacer = matcher.group(1) ?: ""

                val pageRef = LinkResolver.parseUrl(communityUrl, instance)
                    ?: continue

                val communityRef = pageRef as? CommunityRef
                    ?: continue

                matcher.appendReplacement(
                    sb,
                    "$spacer[${matcher.group(2)}](${LinkUtils.getLinkForCommunity(communityRef)})"
                )
            }
            matcher.appendTail(sb)
            return sb.toString()
        }

        override fun processMarkdown(markdown: String): String =
            processSubredditMentions(
                s = processSpoilerTags(processSubTags(processHeaderTags(markdown))),
                instance = instance
            )

        override fun configureTheme(builder: MarkwonTheme.Builder) {
            builder.blockQuoteColor(context.getColorCompat(R.color.colorQuoteLine))
                .thematicBreakColor(context.getColorCompat(R.color.colorQuoteLine))
                .headingBreakHeight(0)
        }
    }

    private fun createMarkwon(context: Context, instance: String): Markwon =
        Markwon.builder(context)
            .usePlugin(CoilImagesPlugin.create(context))
            .usePlugin(LinkifyPlugin.create())
            .usePlugin(TablePlugin.create(context))
            .usePlugin(LemmyPlugin(context, instance))
            .usePlugin(StrikethroughPlugin.create())
            .usePlugin(HtmlPlugin.create())
            .usePlugin(SimpleExtPlugin.create().apply {
                addExtension(1, '^') { _, _ ->
                    SuperScriptSpan()
                }
                addExtension(2, '+') { configuration, _ ->
                    configuration.theme()

                    val spoilerSpan = SpoilerSpan(
                        context.getColorCompat(R.color.colorTextTitle),
                        context.getColorCompat(R.color.colorSpoilerRevealed),
                        context.getColorCompat(R.color.colorTextTitle)
                    )

                    spoilerSpan
                }
            })
            .build()
}