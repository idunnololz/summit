package com.idunnololz.summit.lemmy.utils

import android.text.Spanned
import androidx.core.text.buildSpannedString
import com.idunnololz.summit.api.dto.CommentView
import com.idunnololz.summit.api.dto.PostView
import com.idunnololz.summit.lemmy.VotesManager
import com.idunnololz.summit.reddit.RedditUtils
import com.idunnololz.summit.util.Utils


fun PostView.getUpvoteText(): CharSequence? =
    RedditUtils.abbrevNumber(counts.score.toLong())

fun PostView.getFormattedTitle(): Spanned = Utils.fromHtml(this.post.name)

fun PostView.getFormattedAuthor(): Spanned = buildSpannedString {
    append(creator.name)
}

fun CommentView.getUpvoteText(): CharSequence? =
    RedditUtils.abbrevNumber(counts.score.toLong())