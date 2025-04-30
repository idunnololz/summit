package com.idunnololz.summit.util.ext

import android.content.SharedPreferences
import android.util.Log
import androidx.core.content.edit
import com.idunnololz.summit.BuildConfig
import com.idunnololz.summit.preferences.ComposedPreferences
import com.idunnololz.summit.util.PreferenceUtils
import kotlinx.serialization.json.Json
import org.json.JSONObject

private const val TAG = "SharedPreferencesExt"

inline fun <reified T> SharedPreferences.getJsonValue(json: Json, key: String): T? {
  return try {
    json.decodeFromString(this.getString(key, null) ?: return null)
  } catch (e: Exception) {
    Log.e("SharedPreferencesExt", "", e)
    null
  }
}

inline fun <reified T> SharedPreferences.putJsonValue(json: Json, key: String, value: T) {
  this.edit {
    if (value == null) {
      remove(key)
    } else {
      putString(key, json.encodeToString(value))
    }
  }
}

fun SharedPreferences.getIntOrNull(key: String) = if (this.contains(key)) {
  this.getInt(key, 0)
} else {
  null
}

fun SharedPreferences.getLongSafe(key: String, defValue: Long): Long {
  try {
    return getLong(key, defValue)
  } catch (e: Exception) {
    if (!contains(key)) {
      return defValue
    }

    try {
      // sometimes this is an import issue and the value was converted to an int...
      val value = getInt(key, 0).toLong()

      edit()
        .putLong(key, value)
        .apply()

      return value
    } catch (e: Exception) {
      edit().remove(key)
        .apply()

      return defValue
    }
  }
}

fun SharedPreferences.getFloatSafe(key: String, defValue: Float): Float {
  try {
    return getFloat(key, defValue)
  } catch (e: Exception) {
    if (!contains(key)) {
      return defValue
    }

    try {
      // sometimes this is an import issue and the value was converted to an int...
      val value = getInt(key, 0).toFloat()

      edit()
        .putFloat(key, value)
        .apply()

      return value
    } catch (e: Exception) {
      edit().remove(key)
        .apply()

      return defValue
    }
  }
}

val SharedPreferences.base
  get() = when (this) {
    is ComposedPreferences -> base
    else -> this
  }

fun SharedPreferences.asJson(): JSONObject {
  val json = JSONObject()

  for ((key, value) in this.all.entries) {
    when (value) {
      is String -> json.put(key, value)
      is Boolean -> json.put(key, value)
      is Number -> json.put(key, value)
      null -> json.put(key, null)
      else -> Log.d(TAG, "Unsupported type ${value::class}. Key was $key.")
    }
  }

  json.put(PreferenceUtils.PREFERENCE_VERSION_CODE, BuildConfig.VERSION_CODE)

  return json
}
