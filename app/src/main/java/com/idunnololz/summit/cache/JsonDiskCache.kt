package com.idunnololz.summit.cache

import android.util.Log
import java.io.File
import kotlinx.serialization.json.Json

class JsonDiskCache(
    val json: Json,
    val cache: SimpleDiskCache,
) {

    companion object {
        const val TAG = "JsonDiskCache"

        fun create(json: Json, dir: File, appVersion: Int, maxSize: Long) = JsonDiskCache(
            json,
            SimpleDiskCache(dir, appVersion, maxSize),
        )
    }

    inline fun <reified T> getCachedObject(key: String): T? {
        return try {
            val value = cache.getCachedData(key)
            if (value.isNullOrBlank()) {
                null
            } else {
                json.decodeFromString(key)
            }
        } catch (e: Exception) {
            Log.e(TAG, "getCachedObject()", e)
            null
        }
    }

    inline fun <reified T> cacheObject(key: String, obj: T?) {
        try {
            cache.cacheData(key, json.encodeToString(obj))
        } catch (e: Exception) {
            Log.e(TAG, "cacheObject()", e)
        }
    }

    fun printDebugInfo() {
        Log.d(TAG, "Cache size: ${cache.cache.size().toFloat() / (1024 * 1024)} MB")
    }

    fun evict(key: String) {
        cache.evict(key)
    }
}
