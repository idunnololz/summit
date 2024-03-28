package com.idunnololz.summit.lemmy.utils

import com.idunnololz.summit.api.dto.PostView
import com.idunnololz.summit.util.dateStringToTs
import java.lang.Math.pow
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

fun PostView.getHotRank(): Double {
    return getHotRank(counts.score, dateStringToTs(this.counts.published))
}

fun PostView.getActiveRank(): Double {
    val publishTime = dateStringToTs(this.counts.published)
    val commentTime = if (this.counts.newest_comment_time != null) {
        dateStringToTs(this.counts.newest_comment_time)
    } else {
        0
    }
    val timestampToUse = min(max(publishTime, commentTime), publishTime + 172800000L)
    return getHotRank(counts.score, timestampToUse)
}

fun PostView.getScaledRank(communityMau: Int?): Double {
    return getHotRank() / ln(2.0 + (communityMau ?: 0))
}

fun PostView.getControversialRank(): Double {
    val downvotes = this.counts.downvotes.toDouble()
    val upvotes = this.counts.upvotes.toDouble()

    return if (downvotes == 0.0 && upvotes == 0.0) {
        0.0
    } else {
        (upvotes + downvotes) * min(upvotes, downvotes) / max(upvotes, downvotes)
    }
}

private fun getHotRank(score: Int, timestamp: Long): Double {
    val hoursDiff = (System.currentTimeMillis() - timestamp) / 3600000 /* 1 hour */

    return if (hoursDiff < 168) {
        ln(max(2.0, score + 2.0)) / (hoursDiff + 2.0).pow(1.8)
    } else {
        0.0
    }
}
