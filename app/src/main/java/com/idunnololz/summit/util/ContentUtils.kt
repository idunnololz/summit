package com.idunnololz.summit.util

import com.idunnololz.summit.preview.VideoType

object ContentUtils {
  fun isUrlImage(url: String): Boolean {
    val urlWithoutParams = url.split("?").getOrElse(0) { "" }
    return urlWithoutParams.endsWith(".jpg", ignoreCase = true) ||
      urlWithoutParams.endsWith(".jpeg", ignoreCase = true) ||
      urlWithoutParams.endsWith(".png", ignoreCase = true) ||
      urlWithoutParams.endsWith(".webp", ignoreCase = true) ||
      urlWithoutParams.endsWith(".gif", ignoreCase = true) ||
      urlWithoutParams.endsWith(".svg", ignoreCase = true) ||
      urlWithoutParams.endsWith(".avif", ignoreCase = true) ||
      urlWithoutParams.endsWith("/image_proxy", ignoreCase = true)
  }

  fun isUrlVideo(url: String): Boolean {
    val urlWithoutParams = url.split("?").getOrElse(0) { "" }
    return isUrlMp4(urlWithoutParams) ||
      isUrlWebm(urlWithoutParams) ||
      isUrlHls(urlWithoutParams) ||
      isUrlDash(urlWithoutParams)
  }

  fun isUrlMp4(url: String) = url.endsWith(".mp4", ignoreCase = true)

  fun isUrlWebm(url: String) = url.endsWith(".webm", ignoreCase = true)

  fun isUrlHls(url: String) = url.endsWith(".m3u8", ignoreCase = true)

  fun isUrlDash(url: String) = url.endsWith(".mpd", ignoreCase = true)

  fun getVideoType(urlWithoutParams: String) = if (isUrlMp4(urlWithoutParams)) {
    VideoType.Mp4
  } else if (isUrlHls(urlWithoutParams)) {
    VideoType.Hls
  } else if (isUrlWebm(urlWithoutParams)) {
    VideoType.Webm
  } else if (isUrlDash(urlWithoutParams)) {
    VideoType.Dash
  } else {
    VideoType.Unknown
  }
}
