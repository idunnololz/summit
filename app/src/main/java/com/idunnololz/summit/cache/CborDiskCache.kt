package com.idunnololz.summit.cache

import android.util.Log
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.cbor.Cbor
import kotlinx.serialization.decodeFromByteArray
import kotlinx.serialization.encodeToByteArray
import java.io.File
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import java.lang.reflect.Type

@OptIn(ExperimentalSerializationApi::class)
class CborDiskCache(
  val cache: SimpleDiskCache,
) {

  companion object {
    const val TAG = "JsonDiskCache"

    fun create(dir: File, appVersion: Int, maxSize: Long) = CborDiskCache(
      SimpleDiskCache(dir, appVersion, maxSize),
    )
  }

  inline fun <reified T> getCachedObject(key: String): T? = try {
    val value = cache.getCachedDataAsByteArray(key)
    if (value == null) {
      null
    } else {
      Cbor.decodeFromByteArray(value)
    }
  } catch (e: Exception) {
    Log.e(TAG, "getCachedObject()", e)
    null
  }

  inline fun <reified T> cacheObject(key: String, obj: T?) {
    try {
      cache.put(key, Cbor.encodeToByteArray(obj), mapOf())
    } catch (e: Exception) {
      Log.e(TAG, "cacheObject()", e)
    }
  }

  fun <T> cacheObject(key: String, obj: T?, clazz: Type) {
    try {
      if (obj == null) {
        cache.cacheData(key, null)
      } else {
        cache.put(
          key,
          Cbor.encodeToByteArray(Cbor.serializersModule.serializer(clazz), obj as Any),
          mapOf()
        )
      }
    } catch (e: Exception) {
      Log.e(TAG, "cacheObject()", e)
    }
  }

  fun <T> getCachedObject(key: String, clazz: Type): T? {
    return try {
      val value = cache.getCachedDataAsByteArray(key)
      if (value == null) {
        null
      } else {
        Cbor.decodeFromByteArray(Cbor.serializersModule.serializer(clazz), value) as? T
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
