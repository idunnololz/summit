package com.idunnololz.summit.lemmy.utils

import android.text.Spanned
import androidx.core.text.buildSpannedString
import com.idunnololz.summit.api.dto.CommentView
import com.idunnololz.summit.api.dto.PostView
import com.idunnololz.summit.lemmy.LemmyUtils
import com.idunnololz.summit.util.Utils

fun PostView.getUpvoteText(): CharSequence? =
    LemmyUtils.abbrevNumber(counts.score.toLong())

fun PostView.getFormattedTitle(): Spanned = Utils.fromHtml(this.post.name)

fun PostView.getFormattedAuthor(): Spanned = buildSpannedString {
    append(creator.name)
}

fun CommentView.getUpvoteText(): CharSequence? =
    LemmyUtils.abbrevNumber(counts.score.toLong())
