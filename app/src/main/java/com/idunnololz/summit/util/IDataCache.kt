package com.idunnololz.summit.util

import java.io.IOException
import java.io.InputStream

/**
 * See [com.ggstudios.lolcatalyst.DataCache] and [SimpleDiskCache].
 */
interface IDataCache {

    @Throws(IOException::class)
    fun cacheData(key: String, s: String?)

    @Throws(IOException::class)
    fun cacheData(key: String, s: InputStream?)

    /**
     * Checks to see if there exists any cached data with the given key no matter how old.
     */
    fun hasCache(key: String): Boolean

    fun hasFreshCache(key: String): Boolean

    fun hasFreshCache(key: String, freshTime: Long): Boolean

    /**
     * Returns the time in ms since epoch time that the given key was cached. Returns 0 if the key
     * does not exist.
     */
    fun getCachedDate(key: String): Long

    @Throws(IOException::class)
    fun getCachedData(key: String): String

    fun getCachedDataStream(key: String): InputStream

    @Throws(IOException::class)
    fun evict(key: String)
}
