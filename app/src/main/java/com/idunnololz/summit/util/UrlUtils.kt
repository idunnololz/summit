package com.idunnololz.summit.util

import android.webkit.URLUtil

object UrlUtils {
    fun getFileName(url: String): String = URLUtil.guessFileName(url, null, null)
}
