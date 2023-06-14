package com.idunnololz.summit.reddit

import android.content.Context
import android.graphics.RectF
import android.graphics.Typeface
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.format.DateUtils
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.idunnololz.summit.R
import com.idunnololz.summit.reddit_objects.AwardInfo
import com.idunnololz.summit.reddit_objects.ListingItem
import com.idunnololz.summit.reddit_objects.RedditCommentItem
import com.idunnololz.summit.spans.CenteredImageSpan
import com.idunnololz.summit.spans.HorizontalDividerSpan
import com.idunnololz.summit.spans.RoundedBackgroundSpan
import com.idunnololz.summit.util.*
import com.idunnololz.summit.util.ext.appendLink
import com.idunnololz.summit.util.ext.getColorCompat
import com.idunnololz.summit.view.RedditHeaderView
import com.idunnololz.summit.view.RewardView
import java.util.*

class RedditHeaderHelper(
    private val context: Context
) {

    companion object {
        private val TAG = "RedditHeaderHelper"

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
        listingItem: ListingItem,
        listAuthor: Boolean = true
    ) {
        val context = headerContainer.context
        val sb = SpannableStringBuilder()

        if (listingItem.over_18) {
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

        if (listingItem.spoiler) {
            val s = sb.length
            sb.append(context.getString(R.string.spoiler).toUpperCase(Locale.US))
            val e = sb.length
            sb.setSpan(
                RoundedBackgroundSpan(infoColor, regularColor),
                s,
                e,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            appendSeparator(sb)
        }

        if (listingItem.stickied) {
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
            listingItem.subredditNamePrefixed,
            LinkUtils.getLinkForSubreddit(listingItem.subreddit)
        )
        appendSeparator(sb)
        sb.append(
            DateUtils.getRelativeTimeSpanString(
                ((listingItem.edited as? Double)?.toLong()
                    ?: listingItem.createdUtc.toLong()) * 1000L,
                System.currentTimeMillis(),
                DateUtils.SECOND_IN_MILLIS,
                DateUtils.FORMAT_ABBREV_RELATIVE
            )
        )
        if (listAuthor) {
            appendSeparator(sb)
            if (listingItem.distinguished == "moderator") {
                val s = sb.length
                sb.append(listingItem.author)
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
            } else if (listingItem.distinguished == "admin") {
                run {
                    val s = sb.length
                    sb.append(listingItem.author)
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
                sb.append(RedditUtils.formatAuthor(listingItem.author))
            }
        }

        appendSeparator(sb)
        sb.append(listingItem.domain)
        if (listingItem.linkFlairText != null) {
            appendSeparator(sb)
            run {
                val s = sb.length
                sb.append(listingItem.linkFlairText ?: "")
                val e = sb.length
                Log.d(
                    TAG,
                    "color: ${listingItem.linkFlairTextColor} c: ${listingItem.linkFlairCssClass}"
                )
                sb.setSpan(
                    ForegroundColorSpan(accentColor),
                    s,
                    e,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }
        }

        appendAwards(headerContainer, listingItem.allAwardings, sb)

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
        item: RedditCommentItem,
        detailed: Boolean = false
    ) {
        var sb = SpannableStringBuilder()
        if (item.distinguished == "moderator") {
            run {
                val s = sb.length
                sb.append(item.author)
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
            }
        } else if (item.distinguished == "admin") {
            run {
                val s = sb.length
                sb.append(item.author)
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
            sb.append(item.author)
        }

        if (item.isSubmitter) {
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
            DateUtils.getRelativeTimeSpanString(
                ((item.edited as? Double)?.toLong() ?: item.createdUtc.toLong()) * 1000L,
                System.currentTimeMillis(),
                DateUtils.SECOND_IN_MILLIS,
                DateUtils.FORMAT_ABBREV_RELATIVE
            )
        )

        if (item.stickied) {
            appendSeparator(sb)
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
        }

        if (item.locked) {
            appendSeparator(sb)
            val d = Utils.tint(context, R.drawable.baseline_lock_black_18, R.color.style_amber)
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
        }

        if (item.authorFlairText != null) {
            if (item.authorFlairRichtext is List<*> && item.authorFlairRichtext.isNotEmpty()) {
                appendSeparator(sb)
                headerContainer.getFlairView().apply {
                    setFlairRichText(item.authorFlairRichtext)

                    visibility = View.VISIBLE
                }
            } else if (item.authorFlairText.isNotBlank()) {
                appendSeparator(sb)
                headerContainer.getFlairView().visibility = View.GONE
                val s = sb.length
                sb.append(item.authorFlairText ?: "")
                val e = sb.length
                sb.setSpan(
                    ForegroundColorSpan(accentColor),
                    s,
                    e,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            } else {
                headerContainer.getFlairView().visibility = View.GONE
            }
        } else {
            headerContainer.getFlairView().visibility = View.GONE
        }

        headerContainer.setTextFirstPart(sb)
        sb = SpannableStringBuilder()

        if (detailed) {
            appendSeparator(sb)
            if (item.scoreHidden) {
                sb.append(headerContainer.context.getString(R.string.score_hidden))
            } else {
                sb.append(
                    headerContainer.context.resources.getQuantityString(
                        R.plurals.point_count_format,
                        item.score,
                        RedditUtils.abbrevNumber(item.score.toLong())
                    )
                )
            }

            appendSeparator(sb)
            val count = RedditUtils.countCommentChildren(item)
            sb.append(
                headerContainer.context.resources.getQuantityString(
                    R.plurals.children_count_format, count, count
                )
            )
        } else {
            appendAwards(headerContainer, item.allAwardings, sb)
        }
        headerContainer.setTextSecondPart(sb)
    }

    private fun appendAwards(
        headerContainer: RedditHeaderView,
        allAwards: List<AwardInfo>,
        sb: SpannableStringBuilder
    ) {
        for (i in headerContainer.childCount - 1 downTo RedditHeaderView.STATIC_VIEW_COUNT) {
            val v = headerContainer.getChildAt(i)
            headerContainer.removeViewAt(i)
            rewardViewRecycler.addRecycledView(v as RewardView)
        }

        if (!allAwards.isNullOrEmpty()) {
            appendSeparator(sb)
            allAwards.withIndex().forEach { (index, awardInfo) ->
                val rewardView = rewardViewRecycler.getRecycledView() ?: RewardView(context).apply {
                    layoutParams = LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                    )
                }

                if (index == 0) {
                    rewardView.setPadding(
                        0, 0, Utils.convertDpToPixel(4f).toInt(), 0
                    )
                } else {
                    rewardView.setPadding(
                        Utils.convertDpToPixel(4f).toInt(), 0,
                        Utils.convertDpToPixel(4f).toInt(), 0
                    )
                }

                rewardView.setAward(awardInfo)
                headerContainer.addView(rewardView)
            }
        }
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