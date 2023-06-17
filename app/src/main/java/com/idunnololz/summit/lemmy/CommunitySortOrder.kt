package com.idunnololz.summit.lemmy

import com.idunnololz.summit.reddit.RedditSortOrder

sealed interface CommunitySortOrder {
    object Hot : CommunitySortOrder
    object Active : CommunitySortOrder
    object New : CommunitySortOrder

    data class TopOrder(
        val timeFrame: TimeFrame = TimeFrame.TODAY
    ) : CommunitySortOrder

    enum class TimeFrame {
        NOW,
        TODAY,
        THIS_WEEK,
        THIS_MONTH,
        THIS_YEAR,
        ALL_TIME
    }
}