package com.idunnololz.summit.util

import com.fleeksoft.ksoup.Ksoup
import com.idunnololz.summit.api.NetworkException
import okhttp3.OkHttpClient
import okhttp3.Request

object LoopsVideoUtils {
  fun extractVideoUrl(okHttpClient: OkHttpClient, loopsVideoUrl: String): Result<String> {
    val responseResult = runCatching {
      okHttpClient.newCall(
        Request.Builder()
          .url(loopsVideoUrl)
          .build(),
      ).execute()
    }

    responseResult.exceptionOrNull()?.let {
      return Result.failure(it)
    }

    val response = responseResult.getOrNull()

    if (response == null || !response.isSuccessful) {
      return Result.failure(
        RuntimeException(
          "Network error: ${response?.message}. Code: ${response?.code}"
        )
      )
    }

      val pageHtml = response.body.string()
      val doc = Ksoup.parse(pageHtml, response.request.url.toString())
      val videoUrl = sequenceOf(
        doc.selectFirst("video-player[video-src]")?.attr("video-src"),
        doc.selectFirst("meta[property=og:video:secure_url]")?.attr("content"),
        doc.selectFirst("meta[property=og:video]")?.attr("content"),
        doc.selectFirst("meta[name=twitter:player:stream]")?.attr("content"),
        doc.selectFirst("video[src]")?.attr("src"),
        doc.selectFirst("video source[src]")?.attr("src"),
        extractJsonUrl(pageHtml, "src_url"),
        extractJsonUrl(pageHtml, "hls_url"),
      ).firstOrNull { !it.isNullOrBlank() }
        ?: return Result.failure(RuntimeException("Unable to extract video from loops URL."))

      val finalUrl = response.request.url.resolve(videoUrl)?.toString()

    if (finalUrl == null) {
      return Result.failure(RuntimeException("Unable to resolve video url from loops page."))
    }

    return Result.success(finalUrl)
  }

  private fun extractJsonUrl(pageHtml: String, property: String): String? {
    val match = Regex(
      """[\"']$property[\"']\s*:\s*[\"']([^\"']+)[\"']""",
      RegexOption.IGNORE_CASE,
    ).find(pageHtml)

    return match?.groupValues?.get(1)?.replace("\\/", "/")
  }
}
