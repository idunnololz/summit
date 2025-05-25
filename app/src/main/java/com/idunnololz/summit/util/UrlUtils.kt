package com.idunnololz.summit.util

import android.webkit.URLUtil

object UrlUtils {
  fun getFileName(url: String, mimeType: String? = null): String = URLUtil.guessFileName(url, null, mimeType)
}
