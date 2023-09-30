package com.idunnololz.summit.links

import android.webkit.URLUtil
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.idunnololz.summit.util.LinkUtils
import com.idunnololz.summit.util.StatefulLiveData
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runInterruptible
import org.jsoup.Connection
import org.jsoup.Jsoup
import java.net.URI
import java.net.URISyntaxException
import javax.inject.Inject

@HiltViewModel
class LinkPreviewViewModel @Inject constructor() : ViewModel() {

    val linkMetadata = StatefulLiveData<LinkMetadata>()

    fun loadLinkMetadata(url: String) {
        linkMetadata.setIsLoading()

        viewModelScope.launch {
            try {
                linkMetadata.postValue(_loadLinkMetadata(url))
            } catch (e: Exception) {
                var host: String? = url
                val uri: URI?
                try {
                    uri = URI(url)

                    host = uri.host
                } catch (e: URISyntaxException) {
                    e.printStackTrace()
                }

                val metadata = LinkMetadata(
                    title = url,
                    description = "",
                    mediaType = "",
                    favIcon = null,
                    ogUrl = null,
                    host = host,
                    siteName = null,
                    imageUrl = null,
                )
                linkMetadata.postValue(metadata)
            }
        }
    }

    private suspend fun _loadLinkMetadata(url: String): LinkMetadata {
        val doc = runInterruptible(Dispatchers.IO) {
            val connection: Connection = Jsoup.connect(url)
                .timeout(30 * 1000)
                .userAgent(LinkUtils.USER_AGENT)

            connection.get()
        }

        val elements = doc.getElementsByTag("meta")

        // getTitle doc.select("meta[property=og:title]")

        // getTitle doc.select("meta[property=og:title]")
        var title = doc.select("meta[property=og:title]").attr("content")

        if (title.isEmpty()) {
            title = doc.title()
        }
        if (title.isEmpty()) {
            title = url
        }

        // getDescription

        // getDescription
        var description = doc.select("meta[name=description]").attr("content")
        if (description.isEmpty()) {
            description = doc.select("meta[name=Description]").attr("content")
        }
        if (description.isEmpty()) {
            description = doc.select("meta[property=og:description]").attr("content")
        }
        if (description.isEmpty()) {
            description = ""
        }

        // getMediaType

        // getMediaType
        val mediaTypes = doc.select("meta[name=medium]")
        var type: String? = ""
        type = if (mediaTypes.size > 0) {
            val media = mediaTypes.attr("content")
            if (media == "image") "photo" else media
        } else {
            doc.select("meta[property=og:type]").attr("content")
        }

        // getImages

        // getImages
        val imageUrls = mutableListOf<String>()
        var favIcon: String? = null
        var ogUrl: String? = null
        var siteName: String? = null

        val imageElements = doc.select("meta[property=og:image]")
        if (imageElements.size > 0) {
            val image = imageElements.attr("content")
            if (image.isNotEmpty()) {
                resolveUrl(url, image)?.let {
                    imageUrls.add(it)
                }
            }
        }

        // get image from meta[name=og:image]

        // get image from meta[name=og:image]
        if (imageUrls.isEmpty()) {
            val imageElements = doc.select("meta[name=og:image]")
            if (imageElements.size > 0) {
                val image = imageElements.attr("content")
                if (image.isNotEmpty()) {
                    resolveUrl(url, image)?.let {
                        imageUrls.add(it)
                    }
                }
            }
        }

        if (imageUrls.isEmpty()) {
            var src = doc.select("link[rel=image_src]").attr("href")
            if (src.isNotEmpty()) {
                resolveUrl(url, src)?.let {
                    imageUrls.add(it)
                }
            } else {
                src = doc.select("link[rel=apple-touch-icon]").attr("href")
                if (!src.isEmpty()) {
                    resolveUrl(url, src)?.let {
                        imageUrls.add(it)
                    }
                    favIcon = resolveUrl(url, src)
                } else {
                    src = doc.select("link[rel=icon]").attr("href")
                    if (!src.isEmpty()) {
                        resolveUrl(url, src)?.let {
                            imageUrls.add(it)
                        }
                        favIcon = resolveUrl(url, src)
                    }
                }
            }
        }

        // Favicon

        // Favicon
        var src = doc.select("link[rel=apple-touch-icon]").attr("href")
        if (!src.isEmpty()) {
            favIcon = resolveUrl(url, src)
        } else {
            src = doc.select("link[rel=icon]").attr("href")
            if (!src.isEmpty()) {
                favIcon = resolveUrl(url, src)
            }
        }

        for (element in elements) {
            if (element.hasAttr("property")) {
                val str_property = element.attr("property").trim()
                if (str_property == "og:url") {
                    ogUrl = element.attr("content")
                }
                if (str_property == "og:site_name") {
                    siteName = element.attr("content")
                }
            }
        }

        var uri: URI? = null
        try {
            uri = URI(url)
        } catch (e: URISyntaxException) {
            e.printStackTrace()
        }
        if (ogUrl.isNullOrBlank()) {
            if (uri == null) {
                ogUrl = url
            } else {
                ogUrl = uri.host
            }
        }
        val host = uri?.host ?: url

        return LinkMetadata(
            title,
            description,
            type,
            favIcon,
            ogUrl,
            host,
            siteName,
            imageUrls.firstOrNull(),
        )
    }

    private fun resolveUrl(url: String, part: String): String? {
        return if (URLUtil.isValidUrl(part)) {
            part
        } else {
            var base_uri: URI? = null
            try {
                base_uri = URI(url)
                base_uri = base_uri.resolve(part)
                return base_uri.toString()
            } catch (e: Exception) {
                e.printStackTrace()
            }
            ""
        }
    }

    data class LinkMetadata(
        val title: String,
        val description: String,
        val mediaType: String,
        val favIcon: String?,
        val ogUrl: String?,
        val host: String?,
        val siteName: String?,
        val imageUrl: String?,
    )
}
