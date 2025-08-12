package com.idunnololz.summit.util

import com.idunnololz.summit.api.GetNetworkException
import com.idunnololz.summit.network.BrowserLikeAuthed
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
  @param:BrowserLikeAuthed private val okHttpClient: OkHttpClient,
) {

  suspend fun downloadSite(
    url: String,
    cache: Boolean = false,
    client: OkHttpClient = okHttpClient,
  ): Result<String> = runInterruptible(Dispatchers.IO) {
    val response = try {
      doRequest(url, cache, client)
    } catch (e: Exception) {
      return@runInterruptible Result.failure(e)
    }

    val responseCode = response.code
    if (response.isSuccessful) {
      return@runInterruptible Result.success(response.body.string())
    } else {
      val err = response.body.string()
      response.body.close()
      return@runInterruptible Result.failure(
        GetNetworkException(
          "Response was not 200. Response code: $responseCode. Response: $err Url: $url. req-headers: ${response.request.headers}",
        ),
      )
    }
  }

  private fun doRequest(url: String, cache: Boolean, client: OkHttpClient): Response {
    val builder = Request.Builder()
      .url(url)
    if (!cache) {
      builder.cacheControl(CacheControl.FORCE_NETWORK)
        .header("Cache-Control", "no-cache, no-store")
    }
    val request = builder.build()
    return client.newCall(request).execute()
  }
}
