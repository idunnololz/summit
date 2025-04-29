package com.idunnololz.summit.preferences

import android.content.SharedPreferences
import android.util.Log
import androidx.core.content.edit
import com.idunnololz.summit.preferences.SharedPreferencesPreferences.BooleanPreferenceDelegate
import com.idunnololz.summit.preferences.SharedPreferencesPreferences.FloatPreferenceDelegate
import com.idunnololz.summit.preferences.SharedPreferencesPreferences.IntPreferenceDelegate
import com.idunnololz.summit.preferences.SharedPreferencesPreferences.JsonPreferenceDelegate
import com.idunnololz.summit.preferences.SharedPreferencesPreferences.LongPreferenceDelegate
import com.idunnololz.summit.preferences.SharedPreferencesPreferences.NullableIntPreferenceDelegate
import com.idunnololz.summit.preferences.SharedPreferencesPreferences.StringPreferenceDelegate
import com.idunnololz.summit.util.ext.getFloatSafe
import com.idunnololz.summit.util.ext.getIntOrNull
import com.idunnololz.summit.util.ext.getLongSafe
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer

private const val TAG = "SPP"

interface SharedPreferencesPreferences {
    val sharedPreferences: SharedPreferences
    val json: Json

    class StringPreferenceDelegate(
        val prefs: SharedPreferencesPreferences,
        val key: String,
        val defaultValue: String? = "",
    ) : ReadWriteProperty<Any, String?> {

        override fun getValue(thisRef: Any, property: KProperty<*>) =
            prefs.sharedPreferences.getString(key, defaultValue)

        override fun setValue(thisRef: Any, property: KProperty<*>, value: String?) =
            prefs.sharedPreferences.edit { putString(key, value) }
    }

    class FloatPreferenceDelegate(
        val prefs: SharedPreferencesPreferences,
        val key: String,
        val defaultValue: Float = 0f,
    ) : ReadWriteProperty<Any, Float> {

        override fun getValue(thisRef: Any, property: KProperty<*>) =
            prefs.sharedPreferences.getFloatSafe(key, defaultValue)

        override fun setValue(thisRef: Any, property: KProperty<*>, value: Float) =
            prefs.sharedPreferences.edit().putFloat(key, value).apply()
    }

    class BooleanPreferenceDelegate(
        val prefs: SharedPreferencesPreferences,
        val key: String,
        val defaultValue: Boolean = false,
    ) : ReadWriteProperty<Any, Boolean> {

        override fun getValue(thisRef: Any, property: KProperty<*>) = try {
            // Accidentally set this to an int once...
            prefs.sharedPreferences.getBoolean(key, defaultValue)
        } catch (e: Exception) {
            defaultValue
        }

        override fun setValue(thisRef: Any, property: KProperty<*>, value: Boolean) =
            prefs.sharedPreferences.edit().putBoolean(key, value).apply()
    }

    class IntPreferenceDelegate(
        val prefs: SharedPreferencesPreferences,
        val key: String,
        val defaultValue: Int = 0,
    ) : ReadWriteProperty<Any, Int> {

        override fun getValue(thisRef: Any, property: KProperty<*>) =
            prefs.sharedPreferences.getInt(key, defaultValue)

        override fun setValue(thisRef: Any, property: KProperty<*>, value: Int) =
            prefs.sharedPreferences.edit().putInt(key, value).apply()
    }

    class NullableIntPreferenceDelegate(
        val prefs: SharedPreferencesPreferences,
        val key: String,
    ) : ReadWriteProperty<Any, Int?> {

        override fun getValue(thisRef: Any, property: KProperty<*>) =
            prefs.sharedPreferences.getIntOrNull(key)

        override fun setValue(thisRef: Any, property: KProperty<*>, value: Int?) {
            if (value != null) {
                prefs.sharedPreferences.edit().putInt(key, value).apply()
            }
        }
    }

    class LongPreferenceDelegate(
        val prefs: SharedPreferencesPreferences,
        val key: String,
        val defaultValue: Long = 0,
    ) : ReadWriteProperty<Any, Long> {

        override fun getValue(thisRef: Any, property: KProperty<*>) =
            prefs.sharedPreferences.getLongSafe(key, defaultValue)

        override fun setValue(thisRef: Any, property: KProperty<*>, value: Long) =
            prefs.sharedPreferences.edit().putLong(key, value).apply()
    }

    class JsonPreferenceDelegate<T>(
        val prefs: SharedPreferencesPreferences,
        val json: Json,
        val key: String,
        val serializer: KSerializer<T>,
        val default: () -> T,
    ) : ReadWriteProperty<Any, T> {

//    var isCached: Boolean = false
//    var cache: T? = null

        override fun getValue(thisRef: Any, property: KProperty<*>): T {
//        if (isCached) {
//            @Suppress("UNCHECKED_CAST")
//            return cache as T
//        }

            val s = prefs.sharedPreferences.getString(key, null)
            return try {
                if (s != null) {
                    json.decodeFromString(serializer, s)
                        ?: default()
                } else {
                    default()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error reading json. Key: $key", e)
                default()
            }
//            .also {
//            cache(it)
//        }
        }

        override fun setValue(thisRef: Any, property: KProperty<*>, value: T) {
            val s = try {
                json.encodeToString(serializer, value)
            } catch (e: Exception) {
                Log.e(TAG, "Error converting object to json. Key: $key", e)
                null
            }
            prefs.sharedPreferences.edit()
                .putString(key, s)
                .apply()

//        cache(value)
        }

//    @Suppress("NOTHING_TO_INLINE")
//    inline fun cache(value: T?) {
//        isCached = true
//        cache = value
//    }
    }
}

@Suppress("NOTHING_TO_INLINE")
inline fun SharedPreferencesPreferences.stringPreference(key: String, defaultValue: String? = "") =
    StringPreferenceDelegate(this, key, defaultValue)

@Suppress("NOTHING_TO_INLINE")
inline fun SharedPreferencesPreferences.floatPreference(key: String, defaultValue: Float = 0.0f) =
    FloatPreferenceDelegate(this, key, defaultValue)

@Suppress("NOTHING_TO_INLINE")
inline fun SharedPreferencesPreferences.booleanPreference(
    key: String,
    defaultValue: Boolean = false,
) = BooleanPreferenceDelegate(this, key, defaultValue)

@Suppress("NOTHING_TO_INLINE")
inline fun SharedPreferencesPreferences.intPreference(key: String, defaultValue: Int = 0) =
    IntPreferenceDelegate(this, key, defaultValue)

@Suppress("NOTHING_TO_INLINE")
inline fun SharedPreferencesPreferences.nullableIntPreference(key: String) =
    NullableIntPreferenceDelegate(this, key)

@Suppress("NOTHING_TO_INLINE")
inline fun SharedPreferencesPreferences.longPreference(key: String, defaultValue: Long = 0) =
    LongPreferenceDelegate(this, key, defaultValue)

inline fun <reified T> SharedPreferencesPreferences.jsonPreference(
    key: String,
    noinline defaultValue: () -> T,
) = JsonPreferenceDelegate(
    this,
    json,
    key,
    serializer(),
    defaultValue,
)
