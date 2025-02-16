package com.idunnololz.summit.util

import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup

object LoopsVideoUtils {
    fun extractVideoUrl(okHttpClient: OkHttpClient, loopsVideoUrl: String): String? {
        val response = okHttpClient.newCall(
            Request.Builder()
                .url(loopsVideoUrl)
                .build(),
        )
            .execute()
        val pageHtml = response.body?.string()

        return if (pageHtml != null) {
            val doc = Jsoup.parse(pageHtml)
            val videoPlayerElements = doc.getElementsByTag("video-player")
            val videoPlayerElement = videoPlayerElements.firstOrNull()
            val videoUrl = videoPlayerElement?.attr("video-src")

            videoUrl
        } else {
            null
        }
    }
}
