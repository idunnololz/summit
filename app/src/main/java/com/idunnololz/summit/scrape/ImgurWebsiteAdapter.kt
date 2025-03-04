package com.idunnololz.summit.scrape

import android.util.Log
import com.fleeksoft.ksoup.Ksoup
import com.idunnololz.summit.util.isGif
import java.net.URL

class ImgurWebsiteAdapter(
    val url: String,
) : WebsiteAdapter<ImgurWebsiteAdapter.PreviewInfo>() {

    companion object {
        private const val TAG = "ImgurWebsiteAdapter"
    }

    data class PreviewInfo(
        val url: String,
        val wasUrlRawGif: Boolean = false,
    )

    private var content: String? = null
    private var previewInfo: PreviewInfo? = null

    override fun consume(s: String) {
        super.consume(s)
        restore(s)
    }

    override fun get(): PreviewInfo = previewInfo ?: PreviewInfo("")

    override fun serialize(): String = content ?: ""

    override fun restore(s: String) {
        content = s

        // check if the string is actually a gif!

        if (isGif(s)) {
            previewInfo = PreviewInfo(url = "", wasUrlRawGif = true)
            return
        }

        val doc = Ksoup.parse(s)

        doc.head().select("link[rel='image_src']").let { elems ->
            if (elems.size > 0) {
                val base = URL(url)
                val relativeUrl = elems.attr("href")
                previewInfo = PreviewInfo(URL(base, relativeUrl).toString())
                return
            }
        }

        doc.select("meta[name=twitter:image]").let { elems ->
            if (elems.size > 0) {
                previewInfo = PreviewInfo(elems.attr("content"))
                return
            }
        }

        Log.e(TAG, "Unable to extract image url.")
        setError(UNKNOWN_ERROR)
    }
}
