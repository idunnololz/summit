package com.idunnololz.summit.lemmy

import android.content.Context
import android.graphics.RectF
import android.graphics.Typeface
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.view.View
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.idunnololz.summit.R
import com.idunnololz.summit.api.dto.CommentView
import com.idunnololz.summit.api.dto.PostView
import com.idunnololz.summit.reddit.RedditUtils
import com.idunnololz.summit.spans.CenteredImageSpan
import com.idunnololz.summit.spans.HorizontalDividerSpan
import com.idunnololz.summit.spans.RoundedBackgroundSpan
import com.idunnololz.summit.util.CustomLinkMovementMethod
import com.idunnololz.summit.util.DefaultLinkLongClickListener
import com.idunnololz.summit.util.LinkUtils
import com.idunnololz.summit.util.Utils
import com.idunnololz.summit.util.ViewRecycler
import com.idunnololz.summit.util.dateStringToPretty
import com.idunnololz.summit.util.ext.appendLink
import com.idunnololz.summit.util.ext.getColorCompat
import com.idunnololz.summit.view.RedditHeaderView
import com.idunnololz.summit.view.RewardView

class LemmyHeaderHelper(
    private val context: Context
) {

    companion object {
        private val TAG = "LemmyHeaderHelper"

        private const val SEPARATOR = " ● "
    }

    private val unimportantColor: Int = ContextCompat.getColor(context, R.color.colorText)
    private val regularColor: Int = ContextCompat.getColor(context, R.color.colorTextTitle)
    private val accentColor: Int = ContextCompat.getColor(context, R.color.colorAccent)
    private val infoColor: Int = ContextCompat.getColor(context, R.color.style_blue_gray)
    private val criticalWarningColor: Int = ContextCompat.getColor(context, R.color.style_red)
    private val modColor: Int = context.getColorCompat(R.color.style_green)
    private val adminColor: Int = context.getColorCompat(R.color.style_red)

    private val rewardViewRecycler = ViewRecycler<RewardView>()

    fun populateHeaderSpan(
        headerContainer: RedditHeaderView,
        postView: PostView,
        listAuthor: Boolean = true
    ) {
        val context = headerContainer.context
        val sb = SpannableStringBuilder()

        if (postView.post.nsfw) {
            val s = sb.length
            sb.append(context.getString(R.string.nsfw))
            val e = sb.length
            sb.setSpan(
                RoundedBackgroundSpan(criticalWarningColor, regularColor),
                s,
                e,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            appendSeparator(sb)
        }

//        if (postView.post.spoiler) {
//            val s = sb.length
//            sb.append(context.getString(R.string.spoiler).toUpperCase(Locale.US))
//            val e = sb.length
//            sb.setSpan(
//                RoundedBackgroundSpan(infoColor, regularColor),
//                s,
//                e,
//                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
//            )
//            appendSeparator(sb)
//        }

        if (postView.post.featured_local || postView.post.featured_community) {
            val d = Utils.tint(context, R.drawable.ic_pinned, R.color.style_green)
            val size: Int = Utils.convertDpToPixel(16f).toInt()
            d.setBounds(0, 0, size, size)
            val s = sb.length
            sb.append("  ")
            val e = sb.length
            sb.setSpan(
                CenteredImageSpan(d),
                s,
                e,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            appendSeparator(sb)
        }

        sb.appendLink(
            postView.community.name,
            LinkUtils.getLinkForSubreddit(postView.community.name)
        )
        appendSeparator(sb)
        sb.append(
            dateStringToPretty(postView.post.updated ?: postView.post.published)
        )
        if (listAuthor) {
            appendSeparator(sb)
            if (postView.creator.admin) {
                val s = sb.length
                sb.append(postView.creator.name)
                val e = sb.length
                sb.setSpan(
                    ForegroundColorSpan(modColor),
                    s,
                    e,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                )
                sb.setSpan(
                    StyleSpan(Typeface.BOLD),
                    s,
                    e,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                )
//            } else if (listingItem.distinguished == "admin") {
//                run {
//                    val s = sb.length
//                    sb.append(listingItem.author)
//                    val e = sb.length
//                    sb.setSpan(
//                        ForegroundColorSpan(adminColor),
//                        s,
//                        e,
//                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
//                    )
//                    sb.setSpan(
//                        StyleSpan(Typeface.BOLD),
//                        s,
//                        e,
//                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
//                    )
//                }
            } else {
                sb.append(RedditUtils.formatAuthor(postView.creator.name))
            }
        }

        postView.community.domain?.let { domain ->
            appendSeparator(sb)
            sb.append(domain)
        }
//        if (listingItem.linkFlairText != null) {
//            appendSeparator(sb)
//            run {
//                val s = sb.length
//                sb.append(listingItem.linkFlairText ?: "")
//                val e = sb.length
//                Log.d(
//                    TAG,
//                    "color: ${listingItem.linkFlairTextColor} c: ${listingItem.linkFlairCssClass}"
//                )
//                sb.setSpan(
//                    ForegroundColorSpan(accentColor),
//                    s,
//                    e,
//                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
//                )
//            }
//        }
//
//        appendAwards(headerContainer, listingItem.allAwardings, sb)

        val textView = headerContainer.getChildAt(0) as TextView
        textView.text = sb
        textView.movementMethod = CustomLinkMovementMethod().apply {
            onLinkLongClickListener = DefaultLinkLongClickListener(context)
            onLinkClickListener = object : CustomLinkMovementMethod.OnLinkClickListener {
                override fun onClick(
                    textView: TextView,
                    url: String,
                    text: String,
                    rect: RectF
                ): Boolean =
                    if (RedditUtils.isUrlReddit(url)) {
                        RedditUtils.openRedditUrl(context, url)
                        true
                    } else {
                        false
                    }
            }
        }
    }

    fun populateHeaderSpan(
        headerContainer: RedditHeaderView,
        item: CommentView,
        detailed: Boolean = false,
        childrenCount: Int? = null,
    ) {
        var sb = SpannableStringBuilder()
//        if (item.creator.mode) {
//            run {
//                val s = sb.length
//                sb.append(item.creator.name)
//                val e = sb.length
//                sb.setSpan(
//                    ForegroundColorSpan(modColor),
//                    s,
//                    e,
//                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
//                )
//                sb.setSpan(
//                    StyleSpan(Typeface.BOLD),
//                    s,
//                    e,
//                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
//                )
//            }
//        } else
        if (item.creator.admin) {
            run {
                val s = sb.length
                sb.append(item.creator.name)
                val e = sb.length
                sb.setSpan(
                    ForegroundColorSpan(adminColor),
                    s,
                    e,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                )
                sb.setSpan(
                    StyleSpan(Typeface.BOLD),
                    s,
                    e,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }
        } else {
            sb.append(item.creator.name)
        }

        if (item.creator.id == item.post.creator_id) {
            run {
                val d = Utils.tint(context, R.drawable.ic_op, R.color.style_blue)
                val size: Int = Utils.convertDpToPixel(16f).toInt()
                d.setBounds(0, 0, size, size)
                sb.append(" ")
                val s = sb.length
                sb.append("  ")
                val e = sb.length
                sb.setSpan(
                    CenteredImageSpan(d),
                    s,
                    e,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }
        }

        appendSeparator(sb)
        sb.append(
            dateStringToPretty(item.comment.updated ?: item.comment.published)
        )

//        if (item.comment.distinguished) {
//            appendSeparator(sb)
//            val d = Utils.tint(context, R.drawable.ic_pinned, R.color.style_green)
//            val size: Int = Utils.convertDpToPixel(16f).toInt()
//            d.setBounds(0, 0, size, size)
//            val s = sb.length
//            sb.append("  ")
//            val e = sb.length
//            sb.setSpan(
//                CenteredImageSpan(d),
//                s,
//                e,
//                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
//            )
//        }

//        if (item.locked) {
//            appendSeparator(sb)
//            val d = Utils.tint(context, R.drawable.baseline_lock_black_18, R.color.style_amber)
//            val size: Int = Utils.convertDpToPixel(16f).toInt()
//            d.setBounds(0, 0, size, size)
//            val s = sb.length
//            sb.append("  ")
//            val e = sb.length
//            sb.setSpan(
//                CenteredImageSpan(d),
//                s,
//                e,
//                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
//            )
//        }

//        if (item.creator. != null) {
//            if (item.authorFlairRichtext is List<*> && item.authorFlairRichtext.isNotEmpty()) {
//                appendSeparator(sb)
//                headerContainer.getFlairView().apply {
//                    setFlairRichText(item.authorFlairRichtext)
//
//                    visibility = View.VISIBLE
//                }
//            } else if (item.authorFlairText.isNotBlank()) {
//                appendSeparator(sb)
//                headerContainer.getFlairView().visibility = View.GONE
//                val s = sb.length
//                sb.append(item.authorFlairText ?: "")
//                val e = sb.length
//                sb.setSpan(
//                    ForegroundColorSpan(accentColor),
//                    s,
//                    e,
//                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
//                )
//            } else {
//                headerContainer.getFlairView().visibility = View.GONE
//            }
//        } else {
//            headerContainer.getFlairView().visibility = View.GONE
//        }
        headerContainer.getFlairView().visibility = View.GONE

        headerContainer.setTextFirstPart(sb)
        sb = SpannableStringBuilder()

        if (detailed) {
            appendSeparator(sb)

//            if (item.scoreHidden) {
//                sb.append(headerContainer.context.getString(R.string.score_hidden))
//            } else {
            sb.append(
                headerContainer.context.resources.getQuantityString(
                    R.plurals.point_count_format,
                    item.counts.score,
                    RedditUtils.abbrevNumber(item.counts.score.toLong())
                )
            )
//            }

            if (childrenCount != null) {
                appendSeparator(sb)
                sb.append(
                    headerContainer.context.resources.getQuantityString(
                        R.plurals.children_count_format, childrenCount, childrenCount
                    )
                )
            }
        } else {
//            appendAwards(headerContainer, item.allAwardings, sb)
        }
        headerContainer.setTextSecondPart(sb)
    }

    private inline fun appendSeparator(sb: SpannableStringBuilder) {
        val s = sb.length
        sb.append(SEPARATOR)
        val e = sb.length
        sb.setSpan(
            HorizontalDividerSpan(),
            s + 1,
            e - 1,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )
    }
}