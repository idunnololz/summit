package com.idunnololz.summit.util

import android.net.Uri
import com.idunnololz.summit.api.dto.CommentId
import com.idunnololz.summit.api.dto.PostId
import com.idunnololz.summit.lemmy.CommunityRef
import com.idunnololz.summit.lemmy.toUrl
import com.idunnololz.summit.lemmy.utils.showMoreImageOrLinkOptions
import com.idunnololz.summit.main.MainActivity
import okhttp3.CacheControl
import okhttp3.Request
import okhttp3.Response
import org.jsoup.HttpStatusException

object LinkUtils {

    const val USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/115.0.0.0 Safari/537.36"

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
                url,
            )
        }
    }

    private fun doRequest(url: String, cache: Boolean): Response {
        val builder = Request.Builder()
            .url(url)
            .header("User-Agent", USER_AGENT)
        if (!cache) {
            builder.cacheControl(CacheControl.FORCE_NETWORK)
                .header("Cache-Control", "no-cache, no-store")
        }
        val request = builder.build()
        return Client.get().newCall(request).execute()
    }

    fun getLinkForCommunity(communityRef: CommunityRef): String =
        communityRef.toUrl("lemmy.world")

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
        "https://$instance/post/$postId"

    fun getLinkForPerson(instance: String, name: String): String =
        "https://$instance/u/$name"

    fun getLinkForPost(instance: String, id: PostId): String =
        "https://$instance/post/$id"

    fun getLinkForComment(instance: String, commentId: CommentId): String =
        "https://$instance/comment/$commentId"

    fun getLinkForInstance(instance: String): String =
        "https://$instance/"

    fun getLinkForCommunity(instance: String, communityName: String): String =
        "https://$instance/c/$communityName"
}

fun MainActivity.showMoreLinkOptions(url: String, text: String?) {
    showMoreImageOrLinkOptions(
        url,
        actionsViewModel,
        supportFragmentManager,
        text,
    )
}
