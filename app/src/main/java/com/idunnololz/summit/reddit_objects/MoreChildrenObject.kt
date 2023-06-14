package com.idunnololz.summit.reddit_objects

class MoreChildrenObject(
    val json: MoreChildrenIntermediateObject? = null
)

class MoreChildrenIntermediateObject(
    val errors: Any?,
    val data: MoreChildren
)

class MoreChildren(
    val things: List<RedditObject>
)