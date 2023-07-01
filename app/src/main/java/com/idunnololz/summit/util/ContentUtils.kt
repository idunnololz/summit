package com.idunnololz.summit.util

object ContentUtils {
    fun isUrlImage(url: String) =
        url.endsWith(".jpg") ||
                url.endsWith(".jpeg") ||
                url.endsWith(".png") ||
                url.endsWith(".webp")
}