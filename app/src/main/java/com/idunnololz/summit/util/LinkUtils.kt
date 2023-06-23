package com.idunnololz.summit.util

import android.net.Uri
import com.idunnololz.summit.lemmy.CommunityRef
import com.idunnololz.summit.lemmy.toUrl
import okhttp3.CacheControl
import okhttp3.Request
import okhttp3.Response
import org.jsoup.HttpStatusException

object LinkUtils {


    const val APP_PERMISSIONS_HELP_ARTICLE = "https://support.google.com/android/answer/9431959"

    fun downloadSite(url: String, cache: Boolean = false): String {
        val response = doRequest(url, cache)
        val responseCode = response.code
        if (response.isSuccessful) {
            return response.body?.string() ?: ""
        } else {
            response.body?.close()
            throw HttpStatusException(
                "Response was not 200. Response code: $responseCode",
                responseCode,
                url
            )
        }
    }

    private fun doRequest(url: String, cache: Boolean): Response {
        val builder = Request.Builder()
            .url(url)
            .header("User-Agent", "Chrome")
        if (!cache) {
            builder.cacheControl(CacheControl.FORCE_NETWORK)
                .header("Cache-Control", "no-cache, no-store")
        }
        val request = builder.build()
        return Client.get().newCall(request).execute()
    }

    fun getLinkForCommunity(communityRef: CommunityRef): String =
        communityRef.toUrl()

    fun getLinkForMoreChildren(
        linkId: String,
        children: List<String>,
        commentsSortOrder: String?
    ): String =
        Uri.parse("https://www.reddit.com/api/morechildren.json")
            .buildUpon()
            .appendQueryParameter("link_id", linkId)
            .appendQueryParameter("children", children.joinToString())
            .appendQueryParameter("api_type", "json")
            .apply {
                if (commentsSortOrder != null) {
                    appendQueryParameter("sort", commentsSortOrder)
                }
            }
            .build()
            .toString()

    fun apiVote(): String =
        "https://oauth.reddit.com/api/vote"

    fun apiComment(): String =
        "https://oauth.reddit.com/api/comment"

    fun apiDeleteComment(): String =
        "https://oauth.reddit.com/api/del"

    fun apiEditUserText(): String =
        "https://oauth.reddit.com/api/editusertext"

    fun subreddits(sortOrder: String = "popular", limit: Int, showAll: Boolean): String =
        Uri.parse("https://www.reddit.com/subreddits/${sortOrder}.json")
            .buildUpon()
            .appendQueryParameter("limit", limit.toString())
            .apply {
                if (showAll) {
                    appendQueryParameter("show", "all")
                }
            }
            .toString()

    fun convertToHttps(url: String): String {
        val uri = Uri.parse(url)
        return if (uri.scheme == "http") {
            uri.buildUpon()
                .scheme("https")
                .build().toString()
        } else {
            return uri.toString()
        }
    }

    fun subredditsSearch(query: CharSequence?): String =
        "https://www.reddit.com/subreddits/search.json?q=$query"

    fun getRedirectLink(thingId: String): String =
        "https://www.reddit.com/$thingId"

    fun postIdToLink(instance: String, postId: Int) =
        "https://${instance}/post/$postId"
}