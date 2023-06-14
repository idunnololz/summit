package com.idunnololz.summit.reddit

import androidx.collection.LruCache

val redditPageMemoryCache = LruCache<String, CachedObject>(3)

class CachedObject(
    val ts: Long,
    val o: Any
)