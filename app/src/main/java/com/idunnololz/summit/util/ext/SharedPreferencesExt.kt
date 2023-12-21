package com.idunnololz.summit.util.ext

import android.content.SharedPreferences
import android.util.Log
import com.idunnololz.summit.util.moshi

inline fun <reified T> SharedPreferences.getMoshiValue(key: String): T? {
    return try {
        val json = this.getString(key, null)
            ?: return null
        moshi.adapter(T::class.java).fromJson(
            json,
        )
    } catch (e: Exception) {
        Log.e("SharedPreferencesExt", "", e)
        null
    }
}

inline fun <reified T> SharedPreferences.putMoshiValue(key: String, value: T) {
    this.edit()
        .apply {
            if (value == null) {
                remove(key)
            } else {
                putString(key, moshi.adapter(T::class.java).toJson(value))
            }
        }
        .apply()
}

fun SharedPreferences.getIntOrNull(key: String) =
    if (this.contains(key)) {
        this.getInt(key, 0)
    } else {
        null
    }
