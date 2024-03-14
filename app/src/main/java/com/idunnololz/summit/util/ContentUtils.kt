package com.idunnololz.summit.util

object ContentUtils {
    fun isUrlImage(url: String) =
        url.endsWith(".jpg", ignoreCase = true) ||
            url.endsWith(".jpeg", ignoreCase = true) ||
            url.endsWith(".png", ignoreCase = true) ||
            url.endsWith(".webp", ignoreCase = true) ||
            url.endsWith(".gif", ignoreCase = true) ||
            url.endsWith(".svg", ignoreCase = true)

    fun isUrlVideo(url: String) =
        url.endsWith(".mp4", ignoreCase = true) ||
            url.endsWith(".webm", ignoreCase = true)

    fun isUrlMp4(url: String) =
        url.endsWith(".mp4", ignoreCase = true)

    fun isUrlWebm(url: String) =
        url.endsWith(".webm", ignoreCase = true)

    fun isUrlHls(url: String) =
        url.endsWith(".m3u8", ignoreCase = true)
}
