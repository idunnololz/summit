package com.idunnololz.summit.cache

import android.util.Log
import java.io.File
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import java.lang.reflect.Type

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

  inline fun <reified T> getCachedObject(key: String): T? = try {
    val value = cache.getCachedData(key)
    if (value.isNullOrBlank()) {
      null
    } else {
      json.decodeFromString(value)
    }
  } catch (e: Exception) {
    Log.e(TAG, "getCachedObject()", e)
    null
  }

  inline fun <reified T> cacheObject(key: String, obj: T?) {
    try {
      cache.cacheData(key, json.encodeToString(obj))
    } catch (e: Exception) {
      Log.e(TAG, "cacheObject()", e)
    }
  }

  fun <T> cacheObject(key: String, obj: T?, clazz: Type) {
    try {
      if (obj == null) {
        cache.cacheData(key, null)
      } else {
        cache.cacheData(
          key,
          json.encodeToString(json.serializersModule.serializer(clazz), obj as Any)
        )
      }
    } catch (e: Exception) {
      Log.e(TAG, "cacheObject()", e)
    }
  }

  fun <T> getCachedObject(key: String, clazz: Type): T? {
    return try {
      val value = cache.getCachedData(key)
      if (value.isNullOrBlank()) {
        null
      } else {
        json.decodeFromString(json.serializersModule.serializer(clazz), value) as? T
      }
    } catch (e: Exception) {
      Log.e(TAG, "getCachedObject()", e)
      null
    }
  }

  fun printDebugInfo() {
    Log.d(TAG, "Cache size: ${cache.cache.size().toFloat() / (1024 * 1024)} MB")
  }

  fun evict(key: String) {
    cache.evict(key)
  }
}
