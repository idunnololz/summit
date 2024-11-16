package com.idunnololz.summit.cache

import com.idunnololz.summit.preferences.Preferences
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CachePolicyManager @Inject constructor(private val preferences: Preferences) {

    var cachePolicy: CachePolicy = preferences.cachePolicy
        private set

    fun refreshCachePolicy() {
        cachePolicy = preferences.cachePolicy
    }
}
