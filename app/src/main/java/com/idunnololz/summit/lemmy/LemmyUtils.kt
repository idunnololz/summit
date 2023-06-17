package com.idunnololz.summit.lemmy

import android.text.Spanned
import androidx.core.text.buildSpannedString
import com.idunnololz.summit.api.dto.CommentView
import com.idunnololz.summit.api.dto.PostView
import com.idunnololz.summit.reddit.LikesManager
import com.idunnololz.summit.reddit.RedditUtils
import com.idunnololz.summit.reddit_objects.ListingItem
import com.idunnololz.summit.util.Utils

fun PostView.getLikesWithLikesManager(): Boolean? =
    LikesManager.instance.getLike(getUniqueKey())?.let {
        if (it > 0) true else if (it < 0) false else null
    }

fun PostView.getUpvoteText(): CharSequence? =
    RedditUtils.abbrevNumber(counts.score.toLong())

fun PostView.getFormattedTitle(): Spanned = Utils.fromHtml(this.post.name)

fun PostView.getFormattedAuthor(): Spanned = buildSpannedString {
    append(creator.name)
}


fun CommentView.getLikesWithLikesManager(): Boolean? =
    LikesManager.instance.getLike(getUniqueKey())?.let {
        if (it > 0) true else if (it < 0) false else null
    }

fun CommentView.getUpvoteText(): CharSequence? =
    RedditUtils.abbrevNumber(counts.score.toLong())