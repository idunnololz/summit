package com.idunnololz.summit.util

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.PrintWriter
import java.util.Date

class DataCache private constructor(private val context: Context) : IDataCache {

    companion object {

        private val TAG = DataCache::class.java.simpleName

        /**
         * Amount of time a cached file stays fresh for in ms.
         */
        const val DEFAULT_FRESH_TIME_MS = (24 * 60 * 60 * 1000).toLong() // 1 day

        @SuppressLint("StaticFieldLeak") // application context
        lateinit var instance: DataCache
            private set

        fun initialize(context: Context) {
            instance = DataCache(context.applicationContext)
        }
    }

    // initialized in setupCacheDir()
    lateinit var cacheDir: File
        private set
    private lateinit var dataCacheDir: File

    init {
        PreferenceUtil.initialize(context)
        setupCacheDir()
    }

    private fun setupCacheDir() {
        Log.d(TAG, "Setting up cache dir...")

        cacheDir = null ?: context.cacheDir

        dataCacheDir = File(cacheDir, "data_cache")
        if (dataCacheDir.mkdirs()) {
            Log.d(TAG, "Created data_cache directory.")
        }
        Log.d(TAG, "Cache dir setup complete. Cache dir: " + cacheDir.absolutePath)
    }

    @Throws(FileNotFoundException::class)
    override fun cacheData(key: String, s: String?) {
        if (s == null) return
        ensureDirExist()
        PrintWriter(FileOutputStream(File(dataCacheDir, key))).use {
            it.write(s)
        }
    }

    override fun cacheData(key: String, s: InputStream?) {
        s ?: return
        ensureDirExist()

        File(dataCacheDir, key).outputStream().use {
            s.copyTo(it)
        }
    }

    private fun ensureDirExist() {
        if (!dataCacheDir.exists()) {
            if (!dataCacheDir.mkdirs()) {
                // Something is wrong!

                // Check if the storage we were using was removed...
                setupCacheDir()
            }
        }
    }

    /**
     * Checks to see if there exists any cached data with the given key no matter how old.
     */
    override fun hasCache(key: String): Boolean = File(dataCacheDir, key).exists()

    override fun hasFreshCache(key: String): Boolean = hasFreshCache(key, DEFAULT_FRESH_TIME_MS)

    override fun hasFreshCache(key: String, freshTime: Long): Boolean {
        val f = File(dataCacheDir, key)
        return f.exists() && Date().time - f.lastModified() < freshTime
    }

    override fun getCachedDate(key: String): Long {
        val f = File(dataCacheDir, key)
        return if (f.exists()) f.lastModified() else 0
    }

    @Throws(IOException::class)
    override fun getCachedData(key: String): String = Utils.readFile(File(dataCacheDir, key))

    override fun getCachedDataStream(key: String): InputStream =
        File(dataCacheDir, key).inputStream()

    override fun evict(key: String) {
        val f = File(dataCacheDir, key)
        if (f.exists()) {
            f.delete()
        }
    }

    /**
     * Updates the cache directory based on the preferences
     */
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    fun updateCacheDir() {
        setupCacheDir()
    }
}
