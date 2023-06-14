package com.idunnololz.summit.reddit

import java.util.*

sealed class RedditSortOrder() {
    object HotOrder : RedditSortOrder()
    object NewOrder : RedditSortOrder()
    object RisingOrder : RedditSortOrder()

    data class TopOrder(
        val timeFrame: TimeFrame = TimeFrame.TODAY
    ) : RedditSortOrder()

    enum class TimeFrame {
        NOW,
        TODAY,
        THIS_WEEK,
        THIS_MONTH,
        THIS_YEAR,
        ALL_TIME
    }
}

fun RedditSortOrder.getKey(): String = when (this) {
    RedditSortOrder.HotOrder -> "hot"
    RedditSortOrder.NewOrder -> "new"
    RedditSortOrder.RisingOrder -> "rising"
    is RedditSortOrder.TopOrder -> "top_${timeFrame.name.toLowerCase(Locale.US)}"
}