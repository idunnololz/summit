package com.idunnololz.summit.reddit

import android.content.Context
import android.content.Intent
import android.graphics.Point
import android.icu.text.CompactDecimalFormat
import android.net.Uri
import android.os.Build
import android.util.Log
import android.view.View
import android.widget.*
import com.idunnololz.summit.R
import com.idunnololz.summit.lemmy.CommunityRef
import com.idunnololz.summit.lemmy.LinkResolver
import com.idunnololz.summit.main.MainActivity
import com.idunnololz.summit.spans.SpoilerSpan
import com.idunnololz.summit.util.*
import com.idunnololz.summit.util.ext.getColorCompat
import com.idunnololz.summit.video.VideoSizeHint
import io.noties.markwon.*
import io.noties.markwon.core.MarkwonTheme
import io.noties.markwon.ext.strikethrough.StrikethroughPlugin
import io.noties.markwon.ext.tables.TablePlugin
import io.noties.markwon.html.HtmlPlugin
import io.noties.markwon.html.span.SuperScriptSpan
import io.noties.markwon.image.coil.CoilImagesPlugin
import io.noties.markwon.linkify.LinkifyPlugin
import io.noties.markwon.simple.ext.SimpleExtPlugin
import java.util.*
import java.util.regex.Matcher
import java.util.regex.Pattern

object LemmyUtils {

    private val TAG = LemmyUtils::class.java.canonicalName

    private val SUBREDDIT_NAME_MAX_LENGTH = 20

    /**
     * Matches against subreddit names. (technically incomplete because it doesn't have the length limit)
     */
    val SUBREDDIT_REGEX = Pattern.compile("""(^|\s)(\/?c\/([a-zA-Z0-9-][a-zA-Z0-9-_@]*))""")

    private val GIPHY_REGEX = Pattern.compile("\\(giphy\\|([^\\s]*)\\)")

    private class LemmyPlugin(
        private val context: Context,
        private val instance: String,
    ) : AbstractMarkwonPlugin() {

        /**
         * Matches against words where the first character is a caret. Eg. '^hello'
         */
        private val SUB_REGEX = Pattern.compile("\\^([^\\s]+)")

        /**
         * Matches against strings wrapped in '!>', '<!'. Eg. '!>this is a spoiler<!'
         */
        private val SPOILER_REGEX = Pattern.compile(""":::\s*spoiler\s+(.*)\n((?:.*\n*)*?)(:::\n|${'$'})\s*""")

        /**
         * Matches against poorly formated header tags. Eg. `##Asdf` (proper would be `## Asdf`)
         */
        val HEADER_TAG_REGEX = Pattern.compile("(?m)^([#]+)([^\\s]*.*)$")

        /**
         * Matches against subreddit names. DOES NOT MATCH SUBREDDIT URLS. (technically incomplete because it doesn't have the length limit)
         */
        private val SUBREDDIT_REGEX =
            Pattern.compile("(^|\\s)(/?\\bc/([a-zA-Z0-9-][a-zA-Z0-9-_]*)\\b)")

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
                    Log.d(TAG, "Fixed ${"$formattingChar $rest"}")
                } catch (e: Exception) {
                    throw RuntimeException("Error parsing header tag: $s.", e)
                }
            }
            matcher.appendTail(sb)
            return sb.toString()
        }

        private fun processSubredditMentions(s: String, instance: String): String {
            val matcher = SUBREDDIT_REGEX.matcher(s)
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

    fun createMarkwon(context: Context, instance: String): Markwon =
        Markwon.builder(context)
            .usePlugin(CoilImagesPlugin.create(context))
            .usePlugin(LinkifyPlugin.create())
            .usePlugin(TablePlugin.create(context))
            .usePlugin(LemmyPlugin(context, instance))
            .usePlugin(StrikethroughPlugin.create())
            .usePlugin(HtmlPlugin.create())
            .usePlugin(SimpleExtPlugin.create().apply {
                addExtension(1, '^') { configuration, props ->
                    SuperScriptSpan()
                }
                addExtension(2, '+') { configuration, props ->
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

    fun formatAuthor(author: String): String = "u/%s".format(author)

    fun abbrevNumber(number: Long): String {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            val formatter = CompactDecimalFormat.getInstance(
                Locale.getDefault(), CompactDecimalFormat.CompactStyle.SHORT
            )

            formatter.format(number)
        } else {
            NumberFormatUtil.format(number)
        }
    }

    fun calculateBestVideoSize(
        context: Context,
        redditVideo: VideoSizeHint,
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


    fun bindLemmyText(textView: TextView, text: String, instance: String) {
        createMarkwon(textView.context, instance).setMarkdown(textView, text)
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
