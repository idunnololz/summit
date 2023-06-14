package com.idunnololz.summit.scrape

import android.os.Process
import androidx.annotation.StringRes
import android.util.Log
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.idunnololz.summit.scrape.WebsiteAdapter.Companion.WEBSITE_ERROR
import com.idunnololz.summit.BuildConfig
import com.idunnololz.summit.R
import com.idunnololz.summit.scrape.WebsiteAdapter.Companion.NETWORK_ERROR
import com.idunnololz.summit.scrape.WebsiteAdapter.Companion.NETWORK_TIMEOUT_ERROR
import com.idunnololz.summit.scrape.WebsiteAdapter.Companion.PAGE_NOT_FOUND
import com.idunnololz.summit.scrape.WebsiteAdapter.Companion.SERVICE_UNAVAILABLE_ERROR
import com.idunnololz.summit.util.Client
import com.idunnololz.summit.util.DataCache
import com.idunnololz.summit.util.IDataCache
import okhttp3.*
import org.jsoup.HttpStatusException
import java.io.IOException
import java.net.HttpURLConnection
import java.util.*
import java.util.concurrent.atomic.AtomicInteger

class WebsiteAdapterLoader {

    companion object {
        private val TAG = WebsiteAdapterLoader::class.java.simpleName

        private val VERBOSE = BuildConfig.DEBUG

        const val NO_CACHE_NO_STORE = 1
        const val NO_CACHE = 2

        private const val KEY_URL = "url"
        private val sWriteCacheLock = Any()

        @StringRes
        fun getDefaultErrorMessageFor(errorCode: Int): Int {
            return when (errorCode) {
                WEBSITE_ERROR -> R.string.error_website
                SERVICE_UNAVAILABLE_ERROR -> R.string.error_server_maintenance
                PAGE_NOT_FOUND, NETWORK_ERROR -> R.string.error_network
                NETWORK_TIMEOUT_ERROR -> R.string.error_network_timeout
                else -> R.string.error_unknown
            }
        }
    }

    private var defaultCacheLifetimeMs: Long = 0

    private var currentThread: Thread? = null
    var isCancelled: Boolean = false
        private set

    private val toLoad = ArrayList<ToLoad>()
    private var dataCache: IDataCache

    private var onLoadedListener: ((websiteAdapterLoader: WebsiteAdapterLoader) -> Unit)? = null
    private var onEachAdapterLoadedListener: OnLoadedListener? = null

    private val adaptersLoaded = AtomicInteger()

    private var useHttpCache = 0
    private var shouldUpdateDataCache = true

    private var customHeaders = HashMap<String, String>()

    /**
     * Delay to put on the network call for debugging use only.
     */
    private var debugLoadDelay: Long = 0

    val isLoading: Boolean
        get() {
            return currentThread?.isAlive == true && !isCancelled
        }

    val adapters: List<WebsiteAdapter<*>>
        get() {
            val adapters = ArrayList<WebsiteAdapter<*>>()
            for (tl in toLoad) {
                adapters.add(tl.adapter)
            }
            return adapters
        }

    val loadersLoaded: Int
        get() = adaptersLoaded.get()

    val loadersCount: Int
        get() = toLoad.size

    init {
        defaultCacheLifetimeMs = DataCache.DEFAULT_FRESH_TIME_MS
        dataCache = DataCache.instance
    }

    fun setDiskCache(diskCache: IDataCache?): WebsiteAdapterLoader {
        dataCache = diskCache ?: DataCache.instance
        return this
    }

    fun setDefaultCacheLifetimeMs(defaultCacheLifetimeMs: Long): WebsiteAdapterLoader {
        this.defaultCacheLifetimeMs = defaultCacheLifetimeMs
        return this
    }

    /**
     * Adds a [WebsiteAdapter] to the loader.
     * @param adapter the adapter to use for the website
     * @param url link to the website. This website will be downloaded and the loaded with the
     * provided adapter.
     * @param cacheKey (optional) cache key for the data cache. If null, the data cache will not be
     * used.
     * @param cacheLifetimeMs if the cache key exists in the data cache, the life time determines if
     * the cached value will be used. If the cached value is older then the
     * life time (in milliseconds) the cached value will be ignored (as
     * though the key was not even in the cache).
     * @param handleStatusCodes true if the adapter wants to handle the raw response codes from
     * querying the url. False if the adapter wants the loader to handle
     * them. In most cases it is ideal for the loader to handle status
     * codes.
     * @return this [WebsiteAdapterLoader]. Useful for chaining.
     */
    fun add(
        adapter: WebsiteAdapter<*>,
        url: String,
        cacheKey: String?,
        cacheLifetimeMs: Long = defaultCacheLifetimeMs,
        handleStatusCodes: Boolean = false
    ): WebsiteAdapterLoader {
        toLoad.add(ToLoad(adapter, url, cacheKey, cacheLifetimeMs, handleStatusCodes))
        return this
    }

    fun setUseHttpCache(b: Boolean): WebsiteAdapterLoader {
        useHttpCache = if (b) {
            0
        } else {
            NO_CACHE_NO_STORE
        }
        return this
    }

    @Suppress("unused")
    fun setUseHttpCacheButNoStore(b: Boolean): WebsiteAdapterLoader {
        useHttpCache = if (b) NO_CACHE else 0
        return this
    }

    fun setHeader(key: String, value: String) {
        customHeaders[key] = value
    }

    @JvmOverloads
    fun load(forceRefetch: Boolean = false, fetchStale: Boolean = false): WebsiteAdapterLoader {
        if (toLoad.size == 0) return this
        if (currentThread?.isAlive == true) {
            isCancelled = true
            currentThread?.interrupt()
            try {
                Log.d(TAG, "Attempting to stop active thread and joining!")
                currentThread?.join()
            } catch (e: InterruptedException) {
                return this
            }
        }
        isCancelled = false
        currentThread = Thread {
            Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND)
            loadAll(forceRefetch, fetchStale)
        }.also {
            it.start()
        }

        return this
    }

    private fun loadAll(forceRefetch: Boolean, fetchStale: Boolean) {
        val client = Client.get()

        val calls = ArrayList<Call>()

        val lock = Object()

        val toLoad = ArrayList(this.toLoad)

        // we use this Thread to do all of our processing, this is to prevent UI stutters
        // We let OkHttp handle concurrent http requests but we process the results in this
        // thread

        val iterator = toLoad.iterator()
        while (iterator.hasNext()) {
            val tl = iterator.next()
            if (isCancelled) break // we need to stop all network calls that might be scheduled..

            onEachAdapterLoadedListener?.let {
                tl.adapter.addOnLoadListener(it)
            }

            try {
                if (tl.key != null) {
                    if (!forceRefetch && dataCache.hasFreshCache(tl.key, tl.cacheLifetime)) {
                        Log.d(TAG, "Loading data from cache for: " + tl.url)
                        processDocument(
                            tl,
                            dataCache.getCachedData(tl.key),
                            true /*fromCache*/,
                            false/*isStale*/,
                            dataCache.getCachedDate(tl.key)
                        )
                        if (tl.adapter.error == WebsiteAdapter.NO_ERROR) {
                            iterator.remove()
                            continue
                        }
                    } else if (!forceRefetch && fetchStale && dataCache.hasCache(tl.key)) {
                        Log.d(TAG, "Loading stale data from cache for: " + tl.url)
                        processDocument(
                            tl,
                            dataCache.getCachedData(tl.key),
                            true /*fromCache*/,
                            true /*isStale*/,
                            dataCache.getCachedDate(tl.key)
                        )
                        if (tl.adapter.error == WebsiteAdapter.NO_ERROR) {
                            iterator.remove()
                            continue
                        }
                    }
                }

                // if we are here, then either the data was not cached or the cache load failed
                Log.d(TAG, "Loading data from: " + tl.url)

                val builder = Request.Builder()
                    .url(tl.url)
                    .header("User-Agent", "Chrome")

                if (useHttpCache == NO_CACHE_NO_STORE) {
                    builder.header("Cache-Control", "no-cache, no-store")
                } else if (useHttpCache == NO_CACHE) {
                    // We can store the result but we can't use the cache when we make the req
                    builder.header("Cache-Control", "no-cache")
                }

                customHeaders.entries.forEach { (k, v) ->
                    builder.header(k, v)
                }

                if (forceRefetch) {
                    builder.cacheControl(CacheControl.FORCE_NETWORK)
                }

                val request = builder.build()

                val c = client.newCall(request)

                val cb = object : Callback {
                    override fun onFailure(call: Call, e: IOException) {
                        synchronized(lock) {
                            if (call.isCanceled()) {
                                // canceled...
                                Log.d(TAG, "Canceled: " + tl.url)
                            } else {
                                tl.adapter.setError(WebsiteAdapter.NETWORK_ERROR)
                            }
                            tl.result = ""
                            lock.notify()
                        }
                    }

                    override fun onResponse(call: Call, response: Response) {
                        // cannot be null according to [body()] doc
                        checkNotNull(response.body).use { body ->
                            try {
                                var success = false
                                var possibleResult: String? = null

                                val successfulResponse = response.isSuccessful

                                synchronized(lock) {
                                    if (successfulResponse || tl.handleStatusCodes)
                                        success = true
                                    else {
                                        val ex = HttpStatusException(
                                            "Status not 200 on first try. Status="
                                                    + response.code + ", URL="
                                                    + request.url.toString(),
                                            response.code, request.url.toString()
                                        )
                                        FirebaseCrashlytics.getInstance()
                                            .setCustomKey(KEY_URL, tl.url)
                                        FirebaseCrashlytics.getInstance().recordException(ex)

                                        Log.d(
                                            TAG,
                                            "Error accessing url: " + tl.url + ". Response not 200 but: " + response.code
                                        )

                                        tl.adapter.setError(
                                            when (response.code) {
                                                HttpURLConnection.HTTP_UNAVAILABLE -> {
                                                    WebsiteAdapter.SERVICE_UNAVAILABLE_ERROR
                                                }
                                                HttpURLConnection.HTTP_NOT_FOUND,
                                                HttpURLConnection.HTTP_FORBIDDEN -> {
                                                    WebsiteAdapter.PAGE_NOT_FOUND
                                                }
                                                else -> {
                                                    WebsiteAdapter.WEBSITE_ERROR
                                                }
                                            }
                                        )
                                        possibleResult = ""
                                    }
                                }

                                if (success) {
                                    val networkResponse = response.networkResponse
                                    // ensure we're not hitting the cache before doing redirect check
                                    if (networkResponse != null) {
                                        val url = networkResponse.request.url
                                        tl.adapter.isRedirected = response.isRedirect
                                        tl.adapter.redirectUrl = url.toString()
                                        if (tl.adapter.isRedirected) {
                                            FirebaseCrashlytics.getInstance()
                                                .log("Redirected to: $url")
                                        }
                                    }
                                }

                                if (success) {
                                    if (VERBOSE) {
                                        Log.d(TAG, "Loaded ${response.request.url}")
                                        Log.d(TAG, "Response 1 response:          $response")
                                        Log.d(
                                            TAG,
                                            "Response 1 cache response:    ${response.cacheResponse}"
                                        )
                                        Log.d(
                                            TAG,
                                            "Response 1 network response:  ${response.networkResponse}"
                                        )
                                    }

                                    FirebaseCrashlytics.getInstance().setCustomKey(
                                        "TL",
                                        tl.adapter.javaClass.canonicalName ?: ""
                                    )

                                    // this line can take a while (this means dling...)
                                    possibleResult = body.string()
                                }

                                // test russia ip block
                                //possibleResult = " <!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 ";

                                if (VERBOSE) {
                                    Log.d(
                                        TAG,
                                        "Url: " + tl.url + " Took " + (System.nanoTime() - tl.ts) / 1000000 + "ms"
                                    )
                                }

                                // uncomment to test url redirection
                                //                            tl.adapter.setRedirectUrl("http://google.com");
                                //                            tl.adapter.setRedirect(true);
                                //                            tl.result = "trolololol";

                                tl.result = possibleResult

                                synchronized(lock) {
                                    lock.notify()
                                }
                            } catch (e: IOException) {
                                // canceled...
                                Log.d(TAG, "Canceled: " + tl.url)
                            }
                        }
                    }
                }
                calls.add(c)
                c.enqueue(cb)
            } catch (e: Exception) {
                tl.adapter.setError(WebsiteAdapter.UNKNOWN_ERROR)
                Log.e(TAG, "", e)
                FirebaseCrashlytics.getInstance().setCustomKey(KEY_URL, tl.url)
                FirebaseCrashlytics.getInstance().recordException(e)
                iterator.remove()
            }

        } // while (iterator)

        if (BuildConfig.DEBUG) {
            if (debugLoadDelay > 0) {
                try {
                    Thread.sleep(debugLoadDelay)
                } catch (e: InterruptedException) { /* do nothing */
                }
            }
        }

        try {
            synchronized(lock) {
                while (!isCancelled) {
                    var i = 0
                    while (i < toLoad.size) {
                        if (isCancelled) break
                        val tl = toLoad[i]
                        val result = tl.result

                        if (result != null) {
                            adaptersLoaded.getAndIncrement()
                            Log.d(TAG, "Processing " + tl.url + " Size: " + result.length)
                            FirebaseCrashlytics.getInstance().setCustomKey("website_url", tl.url)
                            if (!result.isEmpty()) {
                                processDocument(
                                    tl,
                                    result,
                                    false,
                                    false,
                                    System.currentTimeMillis()
                                )
                            }
                            toLoad.removeAt(i)
                            i--
                        }

                        tl.result = null

                        i++
                    }
                    if (toLoad.size != 0) {
                        lock.wait()
                    } else
                        break
                }
            }
        } catch (e: InterruptedException) {
            Log.e(TAG, "", e)
        }

        if (isCancelled) {
            for (c in calls) {
                c.cancel()
            }
            Log.d(TAG, "Politely stopped the threads.")
            return
        }

        onLoadedListener?.invoke(this)

        Log.d(TAG, "All info loaded.")
    }

    private fun processDocument(
        tl: ToLoad,
        s: String,
        fromCache: Boolean,
        isStale: Boolean,
        dateFetched: Long
    ) {
        tl.adapter.isStale = isStale
        tl.adapter.dateFetched = dateFetched

        Log.d(TAG, s)

        if (fromCache) {
            tl.adapter.restoreFromString(s)
        } else {
            tl.adapter.load(s)
        }

        val noAdapterError = tl.adapter.error == WebsiteAdapter.NO_ERROR
        if (shouldUpdateDataCache && !fromCache && tl.key != null && noAdapterError) {
            synchronized(sWriteCacheLock) {
                val serialized = tl.adapter.serialize()
                try {
                    dataCache.cacheData(tl.key, serialized)
                } catch (e: IOException) {
                    Log.e(TAG, "", e)
                }
                null
            }
        }
        tl.result = null // free up some ram...
    }

    fun destroy() {
        onEachAdapterLoadedListener = null
        if (isLoading) {
            isCancelled = true
            currentThread?.interrupt()
        }
    }

    fun setOnEachAdapterLoadedListener(
        onEachAdapterLoadedListener: OnLoadedListener
    ): WebsiteAdapterLoader {
        this.onEachAdapterLoadedListener = onEachAdapterLoadedListener
        return this
    }

    fun setOnLoadedListener(l: ((websiteAdapterLoader: WebsiteAdapterLoader) -> Unit)?) {
        onLoadedListener = l
    }

    fun setShouldUpdateDataCache(shouldUpdateDataCache: Boolean): WebsiteAdapterLoader {
        this.shouldUpdateDataCache = shouldUpdateDataCache
        return this
    }

    fun setDebugLoadDelay(debugLoadDelay: Long) {
        this.debugLoadDelay = debugLoadDelay
    }

    private class ToLoad(
        val adapter: WebsiteAdapter<*>,
        val url: String,
        val key: String?,
        val cacheLifetime: Long,
        val handleStatusCodes: Boolean
    ) {
        val ts: Long = System.nanoTime()
        internal var result: String? = null
    }
}
