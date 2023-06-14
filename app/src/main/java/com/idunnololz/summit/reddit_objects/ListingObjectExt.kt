package com.idunnololz.summit.reddit_objects

import com.idunnololz.summit.reddit.LikesManager

fun ListingItem.getLikesWithLikesManager(): Boolean? =
    LikesManager.instance.getLike(name)?.let {
        if (it > 0) true else if (it < 0) false else null
    } ?: likes