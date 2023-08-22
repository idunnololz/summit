package com.idunnololz.summit.util

import android.net.Uri
import com.idunnololz.summit.R
import com.idunnololz.summit.api.dto.CommentId
import com.idunnololz.summit.api.dto.PostId
import com.idunnololz.summit.lemmy.CommunityRef
import com.idunnololz.summit.lemmy.toUrl
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

    fun getLinkForMoreChildren(
        linkId: String,
        children: List<String>,
        commentsSortOrder: String?,
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
        Uri.parse("https://www.reddit.com/subreddits/$sortOrder.json")
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
        "https://$instance/post/$postId"

    fun getLinkForPerson(instance: String, name: String): String =
        "https://$instance/u/$name"

    fun getLinkForPost(instance: String, id: PostId): String =
        "https://$instance/post/$id"

    fun getLinkForComment(instance: String, commentId: CommentId): String =
        "https://$instance/comment/$commentId"
}

fun MainActivity.showBottomMenuForLink(url: String, text: String?) {
    val context = this

    BottomMenu(context).apply {
        setTitle(R.string.link_actions)
        addItemWithIcon(R.id.copy_link, R.string.copy_link_address, R.drawable.baseline_content_copy_24)
        if (text != null) {
            addItemWithIcon(
                R.id.copy_link_text,
                R.string.copy_link_text,
                R.drawable.baseline_content_copy_24,
            )
        }
        addItemWithIcon(R.id.share_link, R.string.share_link, R.drawable.baseline_share_24)
        addItemWithIcon(R.id.open_in_browser, R.string.open_in_browser, R.drawable.baseline_public_24)
        addItemWithIcon(R.id.open_link_incognito, R.string.open_in_incognito, R.drawable.ic_incognito_24)

        setOnMenuItemClickListener {
            when (it.id) {
                R.id.copy_link -> {
                    Utils.copyToClipboard(context, url)
                }
                R.id.copy_link_text -> {
                    Utils.copyToClipboard(context, requireNotNull(text))
                }
                R.id.share_link -> {
                    Utils.shareLink(context, url)
                }
                R.id.open_in_browser -> {
                    Utils.openExternalLink(context, url)
                }
                R.id.open_link_incognito -> {
                    Utils.openExternalLink(context, url, openNewIncognitoTab = true)
                }
            }
        }
    }.let {
        showBottomMenu(it)
    }
}
