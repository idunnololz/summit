package com.idunnololz.summit.util

import com.idunnololz.summit.api.GetNetworkException
import com.idunnololz.summit.network.BrowserLike
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runInterruptible
import okhttp3.CacheControl
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response

@Singleton
class LinkFetcher @Inject constructor(
    @BrowserLike private val okHttpClient: OkHttpClient,
) {

    suspend fun downloadSite(url: String, cache: Boolean = false): String =
        runInterruptible(Dispatchers.IO) {
            val response = doRequest(url, cache)
            val responseCode = response.code
            if (response.isSuccessful) {
                return@runInterruptible response.body?.string() ?: ""
            } else {
                response.body?.close()
                throw GetNetworkException(
                    "Response was not 200. Response code: $responseCode. Url: $url",
                )
            }
        }

    private fun doRequest(url: String, cache: Boolean): Response {
        val builder = Request.Builder()
            .url(url)
            .header("User-Agent", LinkUtils.USER_AGENT)
        if (!cache) {
            builder.cacheControl(CacheControl.FORCE_NETWORK)
                .header("Cache-Control", "no-cache, no-store")
        }
        val request = builder.build()
        return okHttpClient.newCall(request).execute()
    }
}
