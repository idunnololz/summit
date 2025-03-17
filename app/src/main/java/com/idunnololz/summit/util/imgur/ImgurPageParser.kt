package com.idunnololz.summit.util.imgur

import android.util.Log
import com.fleeksoft.ksoup.Ksoup
import com.idunnololz.summit.util.isGif
import java.net.URL

class ImgurPageParser {
    data class PreviewInfo(
        val url: String,
        val wasUrlRawGif: Boolean = false,
    )

    fun parsePage(url: String, html: String): PreviewInfo? {
        // check if the string is actually a gif!
        if (isGif(html)) {
            return PreviewInfo(url = "", wasUrlRawGif = true)
        }

        val doc = Ksoup.parse(html)

        doc.head().select("link[rel='image_src']").let { elems ->
            if (elems.size > 0) {
                val base = URL(url)
                val relativeUrl = elems.attr("href")
                return PreviewInfo(URL(base, relativeUrl).toString())
            }
        }

        doc.select("meta[name=twitter:image]").let { elems ->
            if (elems.size > 0) {
                return PreviewInfo(elems.attr("content"))
            }
        }

        Log.e("ImgurPageParser", "Unable to extract image url.")
        return null
    }
}
