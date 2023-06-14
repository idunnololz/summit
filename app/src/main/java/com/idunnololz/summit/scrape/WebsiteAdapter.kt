package com.idunnololz.summit.scrape

import android.util.Log

import com.google.firebase.crashlytics.FirebaseCrashlytics

import org.json.JSONException

import java.io.IOException
import java.util.ArrayList

abstract class WebsiteAdapter<T : Any> {

    companion object {

        private val TAG = WebsiteAdapter::class.java.simpleName

        const val UNKNOWN_ERROR = 0x800000
        const val NETWORK_ERROR = 0x100001
        const val PAGE_NOT_FOUND = 0x100002
        const val NO_ERROR = -1
        const val WEBSITE_ERROR = 0x100003
        const val UNSUPPORTED_VERSION_ERROR = 0x100004
        const val NETWORK_TIMEOUT_ERROR = 0x100005

        /**
         * Used by custom API server to signal that a maintenance is happening.
         */
        const val SERVICE_UNAVAILABLE_ERROR = 0x100006

        // CSK = CustomServer key (for json objects)
        internal const val CSK_VERSION = "version"
        internal const val CSK_DATA = "data"
    }

    private val onLoadListeners = ArrayList<OnLoadedListener>()

    var isLoaded = false
        protected set

    private val lock = Any()

    var error = -1
        private set
    var isStale: Boolean = false
    var dateFetched: Long = 0

    var isRedirected: Boolean = false
    var redirectUrl: String? = null

    @Throws(IOException::class, JSONException::class, UnsupportedServerVersionException::class)
    protected open fun consume(s: String) {
    }

    fun load(s: String) {
        clearError()

        var consumeSuccess = true
        try {
            consume(s)
        } catch (e: Exception) {
            Log.e(TAG, "", e)

            if (!isRedirected) {
                // Only log an error if there was not a redirect
                FirebaseCrashlytics.getInstance().log(s)
                FirebaseCrashlytics.getInstance().recordException(e)
            }
            consumeSuccess = false
        }

        if (consumeSuccess && error == NO_ERROR) {
            clearError()
            isLoaded = true
            onLoaded()
        } else {
            if (error == NO_ERROR) {
                setError(UNKNOWN_ERROR)
            }
        }
    }

    fun restoreFromString(s: String) {
        clearError()

        var consumeSuccess = true
        try {
            restore(s)
        } catch (e: Exception) {
            Log.e(TAG, "", e)
            FirebaseCrashlytics.getInstance().recordException(e)
            consumeSuccess = false
        }

        if (consumeSuccess && error == NO_ERROR) {
            clearError()
            isLoaded = true
            onLoaded()
        } else {
            if (error == NO_ERROR) {
                setError(UNKNOWN_ERROR)
            }
        }
    }

    protected fun onLoaded() {
        synchronized(lock) {
            isLoaded = true
            for (l in onLoadListeners) {
                l(this)
            }
            onLoadListeners.clear()
        }
    }

    fun addOnLoadListener(listener: OnLoadedListener) {
        synchronized(lock) {
            if (isLoaded) {
                listener(this)
                return
            }
            onLoadListeners.add(listener)
        }
    }

    fun setError(errorCode: Int) {
        error = errorCode
        onLoaded()
    }

    private fun clearError() {
        error = -1
    }

    abstract fun get(): T

    abstract fun serialize(): String

    protected abstract fun restore(s: String)

    fun isSuccess(): Boolean = error == NO_ERROR
}
