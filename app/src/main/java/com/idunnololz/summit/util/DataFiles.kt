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

class DataFiles private constructor(private val context: Context) : IDataCache {

    companion object {

        private val TAG = DataCache::class.java.simpleName

        /**
         * Amount of time a cached file stays fresh for in ms.
         */
        val DEFAULT_FRESH_TIME_MS = (24 * 60 * 60 * 1000).toLong() // 1 day

        @SuppressLint("StaticFieldLeak") // application context
        lateinit var instance: DataFiles
            private set

        fun initialize(context: Context) {
            instance = DataFiles(context.applicationContext)
        }
    }

    private lateinit var dataFilesDir: File

    init {
        PreferenceUtil.initialize(context)
        setupDataDir()
    }

    private fun setupDataDir() {
        Log.d(TAG, "Setting up cache dir...")

        dataFilesDir = File(context.filesDir, "data")
        if (dataFilesDir.mkdirs()) {
            Log.d(TAG, "Created data_cache directory.")
        }
    }

    @Throws(FileNotFoundException::class)
    override fun cacheData(key: String, s: String?) {
        if (s == null) return
        ensureDirExist()
        PrintWriter(FileOutputStream(File(dataFilesDir, key))).use {
            it.write(s)
        }
    }

    override fun cacheData(key: String, s: InputStream?) {
        s ?: return
        ensureDirExist()

        File(dataFilesDir, key).outputStream().use {
            s.copyTo(it)
        }
    }

    private fun ensureDirExist() {
        if (!dataFilesDir.exists()) {
            if (!dataFilesDir.mkdirs()) {
                // Something is wrong!

                // Check if the storage we were using was removed...
                setupDataDir()
            }
        }
    }

    /**
     * Checks to see if there exists any cached data with the given key no matter how old.
     */
    override fun hasCache(key: String): Boolean = File(dataFilesDir, key).exists()

    override fun hasFreshCache(key: String): Boolean = hasFreshCache(key, DEFAULT_FRESH_TIME_MS)

    override fun hasFreshCache(key: String, freshTime: Long): Boolean {
        val f = File(dataFilesDir, key)
        return f.exists() && Date().time - f.lastModified() < freshTime
    }

    override fun getCachedDate(key: String): Long {
        val f = File(dataFilesDir, key)
        return if (f.exists()) f.lastModified() else 0
    }

    @Throws(IOException::class)
    override fun getCachedData(key: String): String = Utils.readFile(File(dataFilesDir, key))

    override fun getCachedDataStream(key: String): InputStream =
        File(dataFilesDir, key).inputStream()

    override fun evict(key: String) {
        val f = File(dataFilesDir, key)
        if (f.exists()) {
            f.delete()
        }
    }

    /**
     * Updates the cache directory based on the preferences
     */
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    fun updateCacheDir() {
        setupDataDir()
    }
}
