package com.idunnololz.summit.reddit

enum class CommentsSortOrder(
    val key: String
) {
    CONFIDENCE("confidence"),
    TOP("top"),
    NEW("new"),
    CONTROVERSIAL("controversial"),
    OLD("old"),
    RANDOM("random"),
    QA("qa"),
    //LIVE("live") // apparently this is dead https://www.reddit.com/r/redditdev/comments/5kv2zn/what_is_confidence_and_live_sorts_in_apiset/
}