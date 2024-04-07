package com.idunnololz.summit.util

import com.idunnololz.summit.preview.VideoType

object ContentUtils {
    fun isUrlImage(url: String) =
        url.endsWith(".jpg", ignoreCase = true) ||
            url.endsWith(".jpeg", ignoreCase = true) ||
            url.endsWith(".png", ignoreCase = true) ||
            url.endsWith(".webp", ignoreCase = true) ||
            url.endsWith(".gif", ignoreCase = true) ||
            url.endsWith(".svg", ignoreCase = true)

    fun isUrlVideo(url: String) =
        isUrlMp4(url) ||
            isUrlWebm(url) ||
            isUrlHls(url) ||
            isUrlDash(url)

    fun isUrlMp4(url: String) =
        url.endsWith(".mp4", ignoreCase = true)

    fun isUrlWebm(url: String) =
        url.endsWith(".webm", ignoreCase = true)

    fun isUrlHls(url: String) =
        url.endsWith(".m3u8", ignoreCase = true)

    fun isUrlDash(url: String) =
        url.endsWith(".mpd", ignoreCase = true)

    fun getVideoType(url: String) =
        if (isUrlMp4(url)) {
            VideoType.Mp4
        } else if (isUrlHls(url)) {
            VideoType.Hls
        } else if (isUrlWebm(url)) {
            VideoType.Webm
        } else if (isUrlDash(url)) {
            VideoType.Dash
        } else {
            VideoType.Unknown
        }
}
