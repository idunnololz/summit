package com.idunnololz.summit.preferences

import android.content.SharedPreferences
import java.lang.ClassCastException

class ComposedPreferences(
    private val preferences: List<SharedPreferences>,
    val base: SharedPreferences,
) : SharedPreferences {

    override fun getAll(): MutableMap<String, *> = preferences.first().all

    override fun getString(key: String?, defValue: String?): String? =
        preferences.firstNotNullOfOrNull { it.getString(key, null) }

    override fun getStringSet(key: String?, defValues: MutableSet<String>?): MutableSet<String>? =
        preferences.firstNotNullOfOrNull { it.getStringSet(key, null) }

    override fun getInt(key: String?, defValue: Int): Int = preferences.firstNotNullOfOrNull {
        if (it.contains(key)) {
            it.getInt(key, defValue)
        } else {
            null
        }
    } ?: defValue

    override fun getLong(key: String?, defValue: Long): Long = preferences.firstNotNullOfOrNull {
        if (it.contains(key)) {
            it.getLong(key, defValue)
        } else {
            null
        }
    } ?: defValue

    override fun getFloat(key: String?, defValue: Float): Float = preferences.firstNotNullOfOrNull {
        if (it.contains(key)) {
            try {
                it.getFloat(key, defValue)
            } catch (e: ClassCastException) {
                it.getInt(key, 0).toFloat()
            } catch (e: Exception) {
                defValue
            }
        } else {
            null
        }
    } ?: defValue

    override fun getBoolean(key: String?, defValue: Boolean): Boolean =
        preferences.firstNotNullOfOrNull {
            if (it.contains(key)) {
                it.getBoolean(key, defValue)
            } else {
                null
            }
        } ?: defValue

    override fun contains(key: String?): Boolean = preferences.any {
        it.contains(key)
    }

    override fun edit(): SharedPreferences.Editor = base.edit()

    override fun registerOnSharedPreferenceChangeListener(
        listener: SharedPreferences.OnSharedPreferenceChangeListener?,
    ) {
        preferences.forEach {
            it.registerOnSharedPreferenceChangeListener(listener)
        }
    }

    override fun unregisterOnSharedPreferenceChangeListener(
        listener: SharedPreferences.OnSharedPreferenceChangeListener?,
    ) {
        preferences.forEach {
            it.unregisterOnSharedPreferenceChangeListener(listener)
        }
    }
}
