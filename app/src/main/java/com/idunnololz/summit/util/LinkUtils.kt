package com.idunnololz.summit.util

import android.net.Uri
import com.idunnololz.summit.api.dto.CommentId
import com.idunnololz.summit.api.dto.PostId
import com.idunnololz.summit.lemmy.CommunityRef
import com.idunnololz.summit.lemmy.PageRef
import com.idunnololz.summit.lemmy.PersonRef
import com.idunnololz.summit.lemmy.toUrl
import com.idunnololz.summit.lemmy.utils.showAdvancedLinkOptions
import com.idunnololz.summit.links.LinkResolver
import com.idunnololz.summit.main.MainActivity

object LinkUtils {
    const val USER_AGENT =
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:134.0) Gecko/20100101 Firefox/134.0"

    fun getLinkForCommunity(communityRef: CommunityRef): String = communityRef.toUrl("lemmy.world")

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

    fun analyzeLink(url: String, instance: String): AdvancedLink {
        if (ContentUtils.isUrlImage(url)) {
            return AdvancedLink.ImageLink(url)
        } else if (ContentUtils.isUrlVideo(url)) {
            return AdvancedLink.VideoLink(url)
        } else {
            val pageRef = LinkResolver.parseUrl(
                url = url,
                currentInstance = instance,
                mustHandle = false,
            )
            if (pageRef != null) {
                return AdvancedLink.PageLink(url, pageRef)
            } else {
                return AdvancedLink.OtherLink(url)
            }
        }
    }

    fun postIdToLink(instance: String, postId: Int) = "https://$instance/post/$postId"

    fun getLinkForPerson(personRef: PersonRef.PersonRefByName) =
        getLinkForPerson(personRef.instance, personRef.name)

    fun getLinkForPerson(instance: String, name: String): String = "https://$instance/u/$name"

    fun getLinkForPost(instance: String, id: PostId): String = "https://$instance/post/$id"

    fun getLinkForComment(instance: String, commentId: CommentId): String =
        "https://$instance/comment/$commentId"

    fun getLinkForInstance(instance: String): String = "https://$instance/"

    fun getLinkForCommunity(instance: String, communityName: String): String =
        "https://$instance/c/$communityName"
}

sealed interface AdvancedLink {
    val url: String

    data class PageLink(
        override val url: String,
        val pageRef: PageRef,
    ) : AdvancedLink

    data class ImageLink(
        override val url: String,
    ) : AdvancedLink

    data class VideoLink(
        override val url: String,
    ) : AdvancedLink

    data class OtherLink(
        override val url: String,
    ) : AdvancedLink
}

fun MainActivity.showMoreLinkOptions(url: String, text: String?) {
    showAdvancedLinkOptions(
        url,
        moreActionsHelper,
        supportFragmentManager,
        text,
    )
}
