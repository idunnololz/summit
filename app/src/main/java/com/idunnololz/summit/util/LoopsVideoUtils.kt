package com.idunnololz.summit.util

import com.fleeksoft.ksoup.Ksoup
import okhttp3.OkHttpClient
import okhttp3.Request

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
            val doc = Ksoup.parse(pageHtml)
            val videoPlayerElements = doc.getElementsByTag("video-player")
            val videoPlayerElement = videoPlayerElements.firstOrNull()
            val videoUrl = videoPlayerElement?.attr("video-src")

            videoUrl
        } else {
            null
        }
    }
}
