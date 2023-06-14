package com.idunnololz.summit.reddit

import android.content.Context
import android.content.Intent
import android.graphics.Point
import android.icu.text.CompactDecimalFormat
import android.net.Uri
import android.os.Build
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.*
import android.view.View
import android.widget.*
import androidx.core.content.ContextCompat
import com.idunnololz.summit.R
import com.idunnololz.summit.main.MainActivity
import com.idunnololz.summit.reddit_objects.*
import com.idunnololz.summit.spans.CustomQuoteSpan
import com.idunnololz.summit.spans.SpoilerSpan
import com.idunnololz.summit.util.*
import com.idunnololz.summit.util.ext.getColorCompat
import io.noties.markwon.*
import io.noties.markwon.core.MarkwonTheme
import io.noties.markwon.ext.strikethrough.StrikethroughPlugin
import io.noties.markwon.ext.tables.TablePlugin
import io.noties.markwon.html.span.SuperScriptSpan
import io.noties.markwon.linkify.LinkifyPlugin
import io.noties.markwon.simple.ext.SimpleExtPlugin
import org.apache.commons.lang3.StringEscapeUtils
import java.lang.RuntimeException
import java.util.*
import java.util.regex.Matcher
import java.util.regex.Pattern

object RedditUtils {

    private val TAG = RedditUtils::class.java.canonicalName

    private val SUBREDDIT_NAME_MAX_LENGTH = 20

    /**
     * Matches against subreddit names. (technically incomplete because it doesn't have the length limit)
     */
    val SUBREDDIT_REGEX = Pattern.compile("/?\\br/([a-zA-Z0-9-][a-zA-Z0-9-_]*)\\b")

    private val GIPHY_REGEX = Pattern.compile("\\(giphy\\|([^\\s]*)\\)")

    private class RedditPlugin(
        private val context: Context
    ) : AbstractMarkwonPlugin() {

        /**
         * Matches against words where the first character is a caret. Eg. '^hello'
         */
        private val SUB_REGEX = Pattern.compile("\\^([^\\s]+)")

        /**
         * Matches against strings wrapped in '!>', '<!'. Eg. '!>this is a spoiler<!'
         */
        private val SPOILER_REGEX = Pattern.compile(">!(.*?)!<")

        /**
         * Matches against poorly formated header tags. Eg. `##Asdf` (proper would be `## Asdf`)
         */
        val HEADER_TAG_REGEX = Pattern.compile("(?m)^([#]+)([^\\s].*)$")

        /**
         * Matches against subreddit names. DOES NOT MATCH SUBREDDIT URLS. (technically incomplete because it doesn't have the length limit)
         */
        private val SUBREDDIT_REGEX =
            Pattern.compile("(^|\\s)(/?\\br/([a-zA-Z0-9-][a-zA-Z0-9-_]*)\\b)")

        private fun processSubTags(s: String): String {
            val matcher = SUB_REGEX.matcher(s)
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
            val matcher = SPOILER_REGEX.matcher(s)
            val sb = StringBuffer()
            while (matcher.find()) {
                matcher.appendReplacement(sb, "++${matcher.group(1).trim()}++")
            }
            matcher.appendTail(sb)
            return sb.toString()
        }

        private fun processHeaderTags(s: String): String {
            val matcher = HEADER_TAG_REGEX.matcher(s)
            val sb = StringBuffer()
            while (matcher.find()) {
                try {
                    val formattingChar = requireNotNull(matcher.group(1)).trim()
                    val rest = requireNotNull(matcher.group(2)).trim()
                    matcher.appendReplacement(
                        sb,
                        Matcher.quoteReplacement("$formattingChar $rest")
                    )
                } catch (e: Exception) {
                    throw RuntimeException("Error parsing header tag: $s.", e)
                }
            }
            matcher.appendTail(sb)
            return sb.toString()
        }

        private fun processSubredditMentions(s: String): String {
            val matcher = SUBREDDIT_REGEX.matcher(s)
            val sb = StringBuffer()
            while (matcher.find()) {
                val subredditName = matcher.group(3) ?: continue
                val spacer = matcher.group(1) ?: ""
                if (subredditName.length > SUBREDDIT_NAME_MAX_LENGTH) continue
                matcher.appendReplacement(
                    sb,
                    "$spacer[${matcher.group(2)}](${LinkUtils.getLinkForSubreddit(subredditName)})"
                )
            }
            matcher.appendTail(sb)
            return sb.toString()
        }

        override fun processMarkdown(markdown: String): String =
            processSubredditMentions(
                processSpoilerTags(processSubTags(processHeaderTags(markdown)))
            )

        override fun configureTheme(builder: MarkwonTheme.Builder) {
            builder.blockQuoteColor(context.getColorCompat(R.color.colorQuoteLine))
                .thematicBreakColor(context.getColorCompat(R.color.colorQuoteLine))
                .headingBreakHeight(0)
        }
    }

    fun createMarkwon(context: Context): Markwon =
        Markwon.builder(context)
            .usePlugin(LinkifyPlugin.create())
            .usePlugin(TablePlugin.create(context))
            .usePlugin(RedditPlugin(context))
            .usePlugin(StrikethroughPlugin.create())
            .usePlugin(SimpleExtPlugin.create().apply {
                addExtension(1, '^', SpanFactory { configuration, props ->
                    SuperScriptSpan()
                })
                addExtension(2, '+', SpanFactory { configuration, props ->
                    configuration.theme()

                    val spoilerSpan = SpoilerSpan(
                        context.getColorCompat(R.color.colorTextTitle),
                        context.getColorCompat(R.color.colorSpoilerRevealed),
                        context.getColorCompat(R.color.colorTextTitle)
                    )

                    spoilerSpan
                })
            })
            .build()

    fun formatAuthor(author: String): String = "u/%s".format(author)

    fun abbrevNumber(number: Long): String {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            val formatter = CompactDecimalFormat.getInstance(
                Locale.getDefault(), CompactDecimalFormat.CompactStyle.SHORT
            );

            formatter.format(number)
        } else {
            NumberFormatUtil.format(number)
        }
    }

    fun calculateBestVideoSize(
        context: Context,
        redditVideo: VideoInfo,
        availableW: Int = Utils.getScreenWidth(context) - context.resources.getDimensionPixelOffset(
            R.dimen.padding_half
        ) * 2,
        availableH: Int = Utils.getScreenHeight(context)
    ): Point {
        val w = redditVideo.width
        val h = redditVideo.height

        // find limiting factor
        val scale = availableW.toDouble() / w
        val scaledH = h * scale
        if (scaledH > availableH) {
            return Point((availableH.toDouble() / h * w).toInt(), availableH)
        } else {
            return Point(availableW, scaledH.toInt())
        }
    }

    fun calculateMaxImagePreviewSize(
        context: Context,
        imageWidth: Int,
        imageHeight: Int,
        availableW: Int = Utils.getScreenWidth(context),
        availableH: Int = Utils.getScreenHeight(context)
    ): Size {
        if (availableW > imageWidth && availableH > imageHeight) {
            return Size(imageWidth, imageHeight)
        }

        val w = imageWidth
        val h = imageHeight

        // find limiting factor
        val scale = availableW.toDouble() / w
        val scaledH = h * scale
        if (scaledH > availableH) {
            return Size((availableH.toDouble() / h * w).toInt(), availableH)
        } else {
            return Size(availableW, scaledH.toInt())
        }
    }

    fun trimSpanned(spanned: Spanned): SpannableStringBuilder {
        val spannable: SpannableStringBuilder = if (spanned is SpannableStringBuilder) {
            spanned
        } else {
            SpannableStringBuilder(spanned)
        }
        var trimStart = 0
        var trimEnd = 0
        var text = spannable.toString()
        while (text.length > 0 && text.startsWith("\n")) {
            text = text.substring(1)
            trimStart += 1
        }
        while (text.length > 0 && text.endsWith("\n")) {
            text = text.substring(0, text.length - 1)
            trimEnd += 1
        }
        return spannable.delete(0, trimStart).delete(spannable.length - trimEnd, spannable.length)
    }

    fun getUpvoteText(listingItem: ListingItem): CharSequence? =
        if (listingItem.hideScore) {
            " ● "
        } else {
            abbrevNumber(listingItem.score.toLong())
        }

    fun getUpvoteText(commentItem: RedditCommentItem): CharSequence? =
        if (commentItem.scoreHidden) {
            " ● "
        } else {
            abbrevNumber(commentItem.score.toLong())
        }

    fun countCommentChildren(item: RedditCommentItem): Int {
        val toTraverse = LinkedList<RedditObject>()
        var c = 0
        item.replies?.let {
            toTraverse.add(it)
        }

        while (toTraverse.isNotEmpty()) {
            when (val o = toTraverse.pop()) {
                is ListingObject -> toTraverse.addAll(o.data?.children ?: listOf())
                is CommentItemObject -> {
                    c++
                    o.data?.replies?.let {
                        toTraverse.add(it)
                    }
                }
                is MoreItemObject -> {
                    c += o.data?.count ?: 0
                }
            }
        }

        return c
    }

    fun formatBodyText(context: Context, bodyHtml: String): CharSequence? {
        // This is not a troll... first fromHtml translates "&lt;" -> "<" for instance
        val htmlString = Utils.fromHtml(bodyHtml).toString()
        val span = trimSpanned(Utils.fromHtml(htmlString))
        val quoteSpans = span.getSpans(0, span.length, QuoteSpan::class.java)
        for (quoteSpan in quoteSpans) {
            val start = span.getSpanStart(quoteSpan)
            val end = span.getSpanEnd(quoteSpan)
            val flags = span.getSpanFlags(quoteSpan)
            span.removeSpan(quoteSpan)
            span.setSpan(
                CustomQuoteSpan(
                    ContextCompat.getColor(context, android.R.color.transparent),
                    ContextCompat.getColor(context, R.color.colorDivider),
                    Utils.convertDpToPixel(4f),
                    Utils.convertDpToPixel(12f)
                ),
                start,
                end,
                flags
            )
        }

        return span
    }

    fun formatBodyText2(context: Context, text: String): CharSequence? =
        with(createMarkwon(context)) {
            render(parse(StringEscapeUtils.unescapeHtml4(text)))
        }

    fun bindRedditText(textView: TextView, text: String) {
        textView.text = formatBodyText2(textView.context, text)
    }

    fun needsWebView(html: String): Boolean = html.contains("&lt;table&gt;")

    fun isUrlAGif(url: String): Boolean = url.endsWith(".gif", ignoreCase = true)

    fun setImageViewSizeBasedOnPreview(
        context: Context,
        previewInfo: PreviewInfo?,
        rootView: View,
        imageView: ImageView
    ) {
        previewInfo ?: return
        val width = previewInfo.width
        val height = previewInfo.height
        val screenSize = Utils.getScreenSize(context)

        rootView.measure(
            View.MeasureSpec.makeMeasureSpec(screenSize.x, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(screenSize.y, View.MeasureSpec.AT_MOST)
        )
        if (width != 0 && height != 0) {
            val thumbnailHeight = (imageView.measuredWidth * (height.toDouble() / width)).toInt()
            imageView.layoutParams = imageView.layoutParams.apply {
                this.width = imageView.measuredWidth
                this.height = thumbnailHeight
            }
        }
    }

    fun normalizeSubredditPath(subredditPath: String): String =
        subredditPath.trim()
            .let {
                if (it.endsWith("/")) {
                    it.substring(0, it.length - 1)
                } else it
            }
            .let {
                if (it.startsWith("/")) {
                    it.substring(1)
                } else it
            }

    fun convertRedditUrl(url: String, desiredFormat: String = "", sharable: Boolean): String {
        val uri = Uri.parse(url)
        val path = uri.path ?: ""
        val cleanPath = if (path.endsWith(".xml")) {
            path.substring(0, path.lastIndexOf(".xml") - 1)
        } else if (path.endsWith(".json")) {
            path.substring(0, path.lastIndexOf(".json") - 1)
        } else {
            path
        }

        return uri.buildUpon().apply {
            path("$cleanPath$desiredFormat")

            if (uri.authority == "oauth.reddit.com" && sharable) {
                authority("www.reddit.com")
            } else if (uri.authority == "amp.reddit.com") {
                authority("www.reddit.com")
            }
        }.build().toString()
    }

    fun toJsonUrl(url: String): String =
        convertRedditUrl(url, desiredFormat = ".json", sharable = false)

    fun toSharedLink(url: String): String =
        convertRedditUrl(url, desiredFormat = "", sharable = true)

    /**
     * Extracts a prefixed subreddit. Eg. 'r/LeagueOfLegends'
     */
    fun extractPrefixedSubreddit(s: String): String? {
        val matcher = SUBREDDIT_REGEX.matcher(s)
        return if (matcher.find()) {
            "r/${matcher.group(1)}"
        } else {
            null
        }
    }

    fun openRedditUrl(context: Context, url: String) {
        context.startActivity(
            Intent(context, MainActivity::class.java)
                .setData(Uri.parse(url))
        )
    }

    fun isUrlReddit(url: String): Boolean =
        isUriReddit(Uri.parse(url))

    fun isUriReddit(uri: Uri): Boolean =
        uri.host == "www.reddit.com"
                || uri.host == "reddit.com"
                || uri.host == "oauth.reddit.com"
                || uri.host == "redd.it"
                || uri.host == "amp.reddit.com"

    fun isUrlRedirect(url: String): Boolean =
        isUriRedirect(Uri.parse(url))

    fun isUriRedirect(uri: Uri): Boolean =
        uri.host == "redd.it"

    fun isUriGallery(uri: Uri): Boolean =
        uri.pathSegments[0] == "gallery"

    fun findGiphyLinks(s: String): List<String> {
        val matcher = GIPHY_REGEX.matcher(s)
        val matches = mutableListOf<String>()
        while (matcher.find()) {
            val g1 = matcher.group(1)
            if (g1 != null) {
                matches.add(g1)
            }
        }
        return matches
    }
}
