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

    private val linkMetadataHelper = LinkMetadataHelper()

    val linkMetadata = StatefulLiveData<LinkMetadataHelper.LinkMetadata>()

    fun loadLinkMetadata(url: String) {
        linkMetadata.setIsLoading()

        viewModelScope.launch {
            try {
                linkMetadata.postValue(linkMetadataHelper.loadLinkMetadata(url))
            } catch (e: Exception) {
                var host: String? = url
                val uri: URI?
                try {
                    uri = URI(url)

                    host = uri.host
                } catch (e: URISyntaxException) {
                    e.printStackTrace()
                }

                val metadata = LinkMetadataHelper.LinkMetadata(
                    url = url,
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
}
