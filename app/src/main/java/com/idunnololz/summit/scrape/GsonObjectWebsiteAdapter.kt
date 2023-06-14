package com.idunnololz.summit.scrape

import android.util.Log

import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.gson.JsonSyntaxException
import com.idunnololz.summit.util.Utils

import org.json.JSONException

import java.io.IOException
import java.lang.reflect.Type

open class GsonObjectWebsiteAdapter<T : Any>(
    private var obj: T,
    private val type: Type,
    private val isSourceGzipped: Boolean
) : WebsiteAdapter<T>() {

    companion object {
        private val TAG = GsonObjectWebsiteAdapter::class.java.simpleName
    }

    private val gson = Utils.gson
    private val defaultValue: T = obj

    @Throws(IOException::class, JSONException::class, UnsupportedServerVersionException::class)
    override fun consume(s: String) {
        if (isSourceGzipped) {
            val decompressed = Utils.decompressGzip(s)
            Log.d(TAG, decompressed)
            restore(decompressed)
        } else {
            restore(s)
        }
    }

    override fun serialize(): String =
        try {
            gson.toJson(obj).also {
                Log.d(TAG, "Serialize()")
                Log.d(TAG, it)
            }
        } catch (e: RuntimeException) {
            throw RuntimeException("An error occurred in class " + javaClass.name, e)
        }

    override fun restore(s: String) {
        val restoredObj: T? = try {
            gson.fromJson(s, type)
        } catch (e: JsonSyntaxException) {
            Log.e(TAG, "Error converting json to object. Class: $javaClass", e)
            Log.e(TAG, s)
            FirebaseCrashlytics.getInstance().log(s)
            FirebaseCrashlytics.getInstance().recordException(e)
            null
        } catch (e: Exception) {
            throw RuntimeException("Thrown by class $javaClass", e)
        }

        if (restoredObj == null) {
            obj = defaultValue
            setError(UNKNOWN_ERROR)
        } else {
            obj = restoredObj
        }
    }

    override fun get(): T = obj

    fun set(o: T) {
        obj = o
    }
}
