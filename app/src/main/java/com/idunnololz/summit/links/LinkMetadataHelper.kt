package com.idunnololz.summit.links

import android.webkit.URLUtil
import com.fleeksoft.ksoup.Ksoup
import com.idunnololz.summit.api.AccountAwareLemmyClient
import com.idunnololz.summit.lemmy.PageRef
import com.idunnololz.summit.util.LinkFetcher
import java.net.URI
import java.net.URISyntaxException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Singleton
class LinkMetadataHelper @Inject constructor(
  private val apiClient: AccountAwareLemmyClient,
  private val linkFixer: LinkFixer,
  private val linkFetcher: LinkFetcher,
) {

  suspend fun loadLinkMetadata(url: String): LinkMetadata {
    val html = linkFetcher.downloadSite(url)
    val doc = withContext(Dispatchers.Default) {
      Ksoup.parse(html)
    }

    val elements = doc.getElementsByTag("meta")

    // getTitle doc.select("meta[property=og:title]")

    // getTitle doc.select("meta[property=og:title]")
    var title: String? = doc.select("meta[property=og:title]").attr("content")

    if (title.isNullOrBlank()) {
      title = doc.title()
    }
    if (title.isEmpty()) {
      title = null
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

    val pageRef = LinkResolver.parseUrl(url, apiClient.instance, mustHandle = true)
    val fixedPageRef = if (pageRef != null) {
      linkFixer.fixPageRef(pageRef)
    } else {
      null
    }

    return LinkMetadata(
      url = url,
      title = title,
      description = description,
      mediaType = type,
      favIcon = favIcon,
      ogUrl = ogUrl,
      host = host,
      siteName = siteName,
      imageUrl = imageUrls.firstOrNull(),
      pageRef = fixedPageRef,
    )
  }

  private fun resolveUrl(url: String, part: String): String? {
    return if (URLUtil.isValidUrl(part)) {
      part
    } else {
      var baseUri: URI? = null
      try {
        baseUri = URI(url)
        baseUri = baseUri.resolve(part)
        return baseUri.toString()
      } catch (e: Exception) {
        e.printStackTrace()
      }
      ""
    }
  }

  data class LinkMetadata(
    val url: String,
    val title: String?,
    val description: String,
    val mediaType: String,
    val favIcon: String?,
    val ogUrl: String?,
    val host: String?,
    val siteName: String?,
    val imageUrl: String?,
    val pageRef: PageRef?,
  )
}
