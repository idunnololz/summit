package com.idunnololz.summit.util

import android.util.Log
import okhttp3.Cache
import okhttp3.OkHttpClient
import java.io.File
import java.util.concurrent.TimeUnit


object Client {
    private val TAG = Client::class.java.simpleName

    private val client: OkHttpClient by lazy { makeClient() }

    private fun makeClient(): OkHttpClient {
        val cacheSize = 10 * 1024 * 1024 // 10 MiB
        val cacheDir = File(DataCache.instance.cacheDir, "okhttp")
        Log.d(TAG, cacheDir.path)
        cacheDir.mkdir()

        val cache = Cache(cacheDir, cacheSize.toLong())

        return OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .writeTimeout(10, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .cache(cache)
            .enableTls12()
            .build()
    }

    fun get(): OkHttpClient {
        return client
    }
}

