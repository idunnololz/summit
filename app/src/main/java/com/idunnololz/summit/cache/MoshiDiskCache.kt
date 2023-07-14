package com.idunnololz.summit.cache

import android.util.Log
import com.idunnololz.summit.util.IDataCache
import com.squareup.moshi.Moshi
import java.io.File

class MoshiDiskCache(
    val moshi: Moshi,
    private val cache: SimpleDiskCache,
): IDataCache by cache {

    companion object {
        const val TAG = "MoshiDiskCache"

        fun create(
            moshi: Moshi,
            dir: File,
            appVersion: Int,
            maxSize: Long,
        ) =
            MoshiDiskCache(
                moshi,
                SimpleDiskCache(dir, appVersion, maxSize)
            )
    }

    inline fun <reified T> getCachedObject(key: String): T? {
        val adapter = moshi.adapter(T::class.java)

        return try {
            val value = getCachedData(key)
            if (value.isNullOrBlank()) {
                null
            } else {
                adapter.fromJson(value)
            }
        } catch (e: Exception) {
            Log.e(TAG, "getCachedObject()", e)
            null
        }
    }

    inline fun <reified T> cacheObject(key: String, obj: T?) {
        val adapter = moshi.adapter(T::class.java)

        try {
            cacheData(key, adapter.toJson(obj))
        } catch (e: Exception) {
            Log.e(TAG, "cacheObject()", e)
        }
    }

    fun printDebugInfo() {
        Log.d(TAG, "Cache size: ${cache.cache.size().toFloat() / (1024 * 1024)} MB")
    }
}