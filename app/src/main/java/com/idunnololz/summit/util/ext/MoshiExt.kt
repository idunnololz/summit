package com.idunnololz.summit.util.ext

import com.squareup.moshi.Moshi

inline fun <reified T> Moshi.fromJson(s: String) =
    adapter(T::class.java).fromJson(s)

inline fun <reified T> Moshi.fromJsonSafe(s: String?) =
    try {
        if (s != null) {
            adapter(T::class.java).fromJson(s)
        } else {
            null
        }
    } catch (e: Exception) {
        null
    }

inline fun <reified T> Moshi.toJsonSafe(o: T?): String? =
    try {
        adapter(T::class.java).toJson(o)
    } catch (e: Exception) {
        null
    }