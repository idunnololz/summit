package com.idunnololz.summit.lemmy

import android.content.Context
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.text.LineBreaker
import android.os.Build
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.ForegroundColorSpan
import android.text.style.StrikethroughSpan
import android.text.style.StyleSpan
import android.view.View
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.idunnololz.summit.R
import com.idunnololz.summit.api.dto.CommentView
import com.idunnololz.summit.api.dto.PostView
import com.idunnololz.summit.api.utils.instance
import com.idunnololz.summit.lemmy.utils.upvotePercentage
import com.idunnololz.summit.links.LinkContext
import com.idunnololz.summit.links.LinkResolver
import com.idunnololz.summit.settings.misc.DisplayInstanceOptions
import com.idunnololz.summit.spans.CenteredImageSpan
import com.idunnololz.summit.spans.HorizontalDividerSpan
import com.idunnololz.summit.spans.RoundedBackgroundSpan
import com.idunnololz.summit.util.CustomLinkMovementMethod
import com.idunnololz.summit.util.DefaultLinkLongClickListener
import com.idunnololz.summit.util.LinkUtils
import com.idunnololz.summit.util.PrettyPrintUtils
import com.idunnololz.summit.util.Utils
import com.idunnololz.summit.util.dateStringToPretty
import com.idunnololz.summit.util.ext.appendLink
import com.idunnololz.summit.util.ext.getColorCompat
import com.idunnololz.summit.view.LemmyHeaderView

class LemmyHeaderHelper(
    private val context: Context,
) {

    companion object {
        private val TAG = "LemmyHeaderHelper"

        const val SEPARATOR = " ● "

        private val condensedTypeface: Typeface by lazy {
            Typeface.create("sans-serif-condensed", Typeface.NORMAL)
        }
    }

    private val unimportantColor: Int = ContextCompat.getColor(context, R.color.colorTextFaint)
    private val regularColor: Int = ContextCompat.getColor(context, R.color.colorText)
    private val accentColor: Int = ContextCompat.getColor(context, R.color.colorAccent)
    private val infoColor: Int = ContextCompat.getColor(context, R.color.style_blue_gray)
    private val criticalWarningColor: Int = ContextCompat.getColor(context, R.color.style_red)
    private val modColor: Int = context.getColorCompat(R.color.style_green)
    private val adminColor: Int = context.getColorCompat(R.color.style_red)
    private val savedColor: Int = context.getColorCompat(R.color.style_blue)
    private val emphasisColor: Int = context.getColorCompat(R.color.colorTextTitle)

    fun populateHeaderSpan(
        headerContainer: LemmyHeaderView,
        postView: PostView,
        instance: String,
        onPageClick: (PageRef) -> Unit,
        onLinkClick: (url: String, text: String, linkContext: LinkContext) -> Unit,
        onLinkLongClick: (url: String, text: String) -> Unit,
        displayInstanceStyle: Int,
        listAuthor: Boolean = true,
        showUpvotePercentage: Boolean,
        useMultilineHeader: Boolean,
        wrapHeader: Boolean,
        isCurrentUser: Boolean,
        showEditedDate: Boolean,
    ) {
        var currentTextView = headerContainer.textView1

        val context = headerContainer.context
        var sb = SpannableStringBuilder()

        if (isCurrentUser) {
            val s = sb.length
            sb.append(context.getString(R.string.you))
            val e = sb.length
            sb.setSpan(
                RoundedBackgroundSpan(infoColor, emphasisColor),
                s,
                e,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE,
            )
            sb.append(" ")
        }

        if (postView.post.nsfw) {
            val s = sb.length
            sb.append(context.getString(R.string.nsfw))
            val e = sb.length
            sb.setSpan(
                RoundedBackgroundSpan(criticalWarningColor, emphasisColor),
                s,
                e,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE,
            )
            sb.appendSeparator()
        }

        if (postView.saved) {
            val d = Utils.tint(context, R.drawable.baseline_bookmark_24, R.color.style_blue)
            val size: Int = Utils.convertDpToPixel(16f).toInt()
            d.setBounds(0, 0, size, size)
            val s = sb.length
            sb.append("  ")
            val e = sb.length
            sb.setSpan(
                CenteredImageSpan(d),
                s,
                e,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE,
            )
            sb.appendSeparator()
        }

        if (postView.post.removed || postView.post.deleted) {
            val d = Utils.tint(context, R.drawable.baseline_delete_24, R.color.style_red)
            val size: Int = Utils.convertDpToPixel(16f).toInt()
            d.setBounds(0, 0, size, size)
            val s = sb.length
            sb.append("  ")
            val e = sb.length
            sb.setSpan(
                CenteredImageSpan(d),
                s,
                e,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE,
            )
            sb.appendSeparator()
        }

        if (postView.post.featured_local || postView.post.featured_community) {
            val d = Utils.tint(context, R.drawable.baseline_push_pin_24, R.color.style_green)
            val size: Int = Utils.convertDpToPixel(16f).toInt()
            d.setBounds(0, 0, size, size)
            val s = sb.length
            sb.append("  ")
            val e = sb.length
            sb.setSpan(
                CenteredImageSpan(d),
                s,
                e,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE,
            )
            sb.appendSeparator()
        }
        if (postView.post.locked) {
            val d = Utils.tint(context, R.drawable.outline_lock_24, R.color.style_amber)
            val size: Int = Utils.convertDpToPixel(16f).toInt()
            d.setBounds(0, 0, size, size)
            val s = sb.length
            sb.append("  ")
            val e = sb.length
            sb.setSpan(
                CenteredImageSpan(d),
                s,
                e,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE,
            )
            sb.appendSeparator()
        }

        val postInstance = postView.community.instance
        val displayFullName = when (displayInstanceStyle) {
            DisplayInstanceOptions.NeverDisplayInstance -> {
                false
            }
            DisplayInstanceOptions.OnlyDisplayNonLocalInstances -> {
                instance != postInstance
            }
            DisplayInstanceOptions.AlwaysDisplayInstance -> {
                true
            }
            else -> false
        }

        if (displayFullName) {
            sb.appendNameWithInstance(
                context = context,
                name = postView.community.name,
                instance = postInstance,
                url = LinkUtils.getLinkForCommunity(postView.community.toCommunityRef()),
            )
        } else {
            sb.appendLink(
                postView.community.name,
                LinkUtils.getLinkForCommunity(postView.community.toCommunityRef()),
            )
        }

        sb.appendSeparator()
        dateStringToPretty(context, postView.post.published).let {
            sb.append(it)
        }

        if (showEditedDate && postView.post.updated != null) {
            dateStringToPretty(context, postView.post.updated).let {
                sb.append(" ($it)")
            }
        }

        if (wrapHeader) {
//            currentTextView.setSingleLineAvoidingRelayout(false)
            if (currentTextView.maxLines != 2) {
                currentTextView.maxLines = 2
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                currentTextView.breakStrategy = LineBreaker.BREAK_STRATEGY_SIMPLE
            }
            currentTextView.isSingleLine = false
            headerContainer.multiline = false
        } else if (useMultilineHeader) {
            currentTextView.movementMethod = makeMovementMethod(
                instance = instance,
                onPageClick = onPageClick,
                onLinkClick = onLinkClick,
                onLinkLongClick = onLinkLongClick,
            )
            currentTextView.text = sb
            currentTextView.isSingleLine = true
//            currentTextView.setSingleLineAvoidingRelayout(true)
            headerContainer.multiline = true

            currentTextView = headerContainer.textView2
            sb = SpannableStringBuilder()
        } else {
            currentTextView.isSingleLine = true
            headerContainer.multiline = false
        }

        if (listAuthor) {
            if (sb.isNotEmpty()) {
                sb.appendSeparator()
            }

            val s = sb.length
            sb.appendLink(
                LemmyUtils.formatAuthor(postView.creator.name),
                LinkUtils.getLinkForPerson(postView.creator.instance, postView.creator.name),
            )
            val e = sb.length

            if (postView.creator_is_admin == true) {
                sb.setSpan(
                    ForegroundColorSpan(adminColor),
                    s,
                    e,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE,
                )
                sb.setSpan(
                    StyleSpan(Typeface.BOLD),
                    s,
                    e,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE,
                )
            } else if (postView.creator_is_moderator == true) {
                sb.setSpan(
                    ForegroundColorSpan(modColor),
                    s,
                    e,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE,
                )
                sb.setSpan(
                    StyleSpan(Typeface.BOLD),
                    s,
                    e,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE,
                )
            } else {
                sb.setSpan(
                    ForegroundColorSpan(regularColor),
                    s,
                    e,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE,
                )
            }

            if (postView.creator_banned_from_community) {
                sb.setSpan(
                    StrikethroughSpan(),
                    s,
                    e,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE,
                )
            }
        }
        if (showUpvotePercentage) {
            sb.appendSeparator()
            sb.append(
                PrettyPrintUtils.defaultShortPercentFormat.format(
                    postView.upvotePercentage,
                ),
            )
        }

        currentTextView.text = sb
        currentTextView.movementMethod = makeMovementMethod(
            instance = instance,
            onPageClick = onPageClick,
            onLinkClick = onLinkClick,
            onLinkLongClick = onLinkLongClick,
        )
    }

    fun populateHeaderSpan(
        headerContainer: LemmyHeaderView,
        commentView: CommentView,
        instance: String,
        score: Int?,
        onPageClick: (PageRef) -> Unit,
        onLinkClick: (url: String, text: String, linkContext: LinkContext) -> Unit,
        onLinkLongClick: (url: String, text: String) -> Unit,
        displayInstanceStyle: Int,
        showUpvotePercentage: Boolean,
        useMultilineHeader: Boolean,
        isCurrentUser: Boolean,
        showEditedDate: Boolean,
        useCondensedTypeface: Boolean,
        detailed: Boolean = false,
        childrenCount: Int? = null,
        wrapHeader: Boolean = false,
        scoreColor: Int? = null,
    ) {
        val creatorInstance = commentView.creator.instance
        val currentTextView = headerContainer.textView1

        if (useCondensedTypeface) {
            headerContainer.setTypeface(condensedTypeface)
        } else {
            headerContainer.setTypeface(null)
        }

        val sb = SpannableStringBuilder()

        if (isCurrentUser) {
            val s = sb.length
            sb.append(context.getString(R.string.you))
            val e = sb.length
            sb.setSpan(
                RoundedBackgroundSpan(infoColor, emphasisColor),
                s,
                e,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE,
            )
            sb.append(" ")
        }

        if (commentView.saved) {
            val d = Utils.tint(context, R.drawable.baseline_bookmark_24, R.color.style_blue)
            val size: Int = Utils.convertDpToPixel(16f).toInt()
            d.setBounds(0, 0, size, size)
            val s = sb.length
            sb.append("  ")
            val e = sb.length
            sb.setSpan(
                CenteredImageSpan(d),
                s,
                e,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE,
            )
        }

        val displayFullName = when (displayInstanceStyle) {
            DisplayInstanceOptions.NeverDisplayInstance -> {
                false
            }
            DisplayInstanceOptions.OnlyDisplayNonLocalInstances -> {
                instance != creatorInstance
            }
            DisplayInstanceOptions.AlwaysDisplayInstance -> {
                true
            }
            else -> false
        }
        val creatorName = commentView.creator.name.trim()

        val nameStart = sb.length
        val nameEnd = nameStart + creatorName.length

        if (displayFullName) {
            sb.appendNameWithInstance(
                context = context,
                name = creatorName,
                instance = creatorInstance,
                url = LinkUtils.getLinkForPerson(creatorInstance, commentView.creator.name),
            )
        } else {
            sb.appendLink(
                text = creatorName,
                url = LinkUtils.getLinkForPerson(creatorInstance, commentView.creator.name),
                underline = false,
            )
        }

        if (commentView.creator_is_admin == true) {
            sb.setSpan(
                ForegroundColorSpan(adminColor),
                nameStart,
                nameEnd,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE,
            )
            sb.setSpan(
                StyleSpan(Typeface.BOLD),
                nameStart,
                nameEnd,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE,
            )
        } else if (commentView.creator_is_moderator == true) {
            sb.setSpan(
                ForegroundColorSpan(modColor),
                nameStart,
                nameEnd,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE,
            )
            sb.setSpan(
                StyleSpan(Typeface.BOLD),
                nameStart,
                nameEnd,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE,
            )
        } else {
            sb.setSpan(
                ForegroundColorSpan(regularColor),
                nameStart,
                nameEnd,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE,
            )
        }

        if (commentView.creator_banned_from_community) {
            sb.setSpan(
                StrikethroughSpan(),
                nameStart,
                nameEnd,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE,
            )
        }

        if (commentView.creator.id == commentView.post.creator_id) {
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
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE,
                )
            }
        }

        if (sb.isNotEmpty()) {
            sb.append("  ")
        }
        sb.append(
            dateStringToPretty(context, commentView.comment.published),
        )

        if (showEditedDate && commentView.comment.updated != null) {
            dateStringToPretty(context, commentView.comment.updated).let {
                sb.append(" ($it)")
            }
        }
        headerContainer.getFlairView().visibility = View.GONE

        if (detailed) {
            sb.appendSeparator()

            if (score != null) {
                val scoreStart = sb.length
                sb.append(
                    headerContainer.context.resources.getQuantityString(
                        R.plurals.point_count_format,
                        score,
                        LemmyUtils.abbrevNumber(score.toLong()),
                    ),
                )

                if (scoreColor != null) {
                    sb.setSpan(
                        ForegroundColorSpan(scoreColor),
                        scoreStart,
                        sb.length,
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE,
                    )
                }
            }

            if (childrenCount != null) {
                sb.appendSeparator()
                sb.append(
                    headerContainer.context.resources.getQuantityString(
                        R.plurals.children_count_format,
                        childrenCount,
                        childrenCount,
                    ),
                )
            }
            headerContainer.textView1.apply {
                maxLines = 2
                isSingleLine = false
            }
        }
        if (showUpvotePercentage) {
            sb.appendSeparator()
            sb.append(
                PrettyPrintUtils.defaultShortPercentFormat.format(
                    commentView.upvotePercentage,
                ),
            )
        }

        if (wrapHeader) {
            currentTextView.isSingleLine = false
            currentTextView.maxLines = 2
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                currentTextView.breakStrategy = LineBreaker.BREAK_STRATEGY_SIMPLE
            }
            headerContainer.multiline = false
        } else if (useMultilineHeader) {
            currentTextView.isSingleLine = true
            headerContainer.multiline = true
        } else {
            currentTextView.isSingleLine = true
            headerContainer.multiline = false
        }

        currentTextView.text = sb

        currentTextView.movementMethod = makeMovementMethod(
            instance = instance,
            onPageClick = onPageClick,
            onLinkClick = onLinkClick,
            onLinkLongClick = onLinkLongClick,
        )
    }

    private fun makeMovementMethod(
        instance: String,
        onPageClick: (PageRef) -> Unit,
        onLinkClick: (url: String, text: String, linkContext: LinkContext) -> Unit,
        onLinkLongClick: (url: String, text: String) -> Unit,
    ) = CustomLinkMovementMethod().apply {
        onLinkLongClickListener = DefaultLinkLongClickListener(context, onLinkLongClick)
        onLinkClickListener = object : CustomLinkMovementMethod.OnLinkClickListener {
            override fun onClick(
                textView: TextView,
                url: String,
                text: String,
                rect: RectF,
            ): Boolean {
                val pageRef = LinkResolver.parseUrl(url, instance)

                if (pageRef != null) {
                    onPageClick(pageRef)
                } else {
                    onLinkClick(url, text, LinkContext.Text)
                }
                return true
            }
        }
    }
}

@Suppress("NOTHING_TO_INLINE")
inline fun SpannableStringBuilder.appendSeparator() {
    val s = length
    append(LemmyHeaderHelper.SEPARATOR)
    val e = length
    setSpan(
        HorizontalDividerSpan(),
        s + 1,
        e - 1,
        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE,
    )
}

fun TextView.setSingleLineAvoidingRelayout(isSingleLine: Boolean) {
    if (this.getTag(R.id.is_single_line) as? Boolean == isSingleLine) {
        return
    }
    this.isSingleLine = false
    this.setTag(R.id.is_single_line, isSingleLine)
}

fun TextView.setMarkdown(markdown: String) {
    text = LemmyTextHelper.getSpannable(context, markdown)
}
