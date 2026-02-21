package com.idunnololz.summit.util

import android.net.Uri
import android.util.Patterns
import com.idunnololz.summit.api.dto.lemmy.CommentId
import com.idunnololz.summit.api.dto.lemmy.PostId
import com.idunnololz.summit.lemmy.CommunityRef
import com.idunnololz.summit.lemmy.PageRef
import com.idunnololz.summit.lemmy.PersonRef
import com.idunnololz.summit.lemmy.toUrl
import com.idunnololz.summit.lemmy.utils.showAdvancedLinkOptions
import com.idunnololz.summit.links.LinkResolver

object LinkUtils {

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

  fun analyzeLink(url: String, instance: String, linkResolver: LinkResolver): AdvancedLink {
    if (ContentUtils.isUrlImage(url)) {
      return AdvancedLink.ImageLink(url)
    } else if (ContentUtils.isUrlVideo(url)) {
      return AdvancedLink.VideoLink(url)
    } else {
      val pageRef = linkResolver.parseUrl(
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

  fun getLinkForPerson(personRef: PersonRef.PersonRefComplete) =
    getLinkForPerson(personRef.instance, personRef.name)

  fun getLinkForPerson(instance: String, name: String): String = "https://$instance/u/$name"

  fun getLinkForPost(instance: String, id: PostId): String = "https://$instance/post/$id"

  fun getLinkForComment(instance: String, commentId: CommentId): String =
    "https://$instance/comment/$commentId"

  fun getLinkForInstance(instance: String): String = "https://$instance/"

  fun getLinkForCommunity(instance: String, communityName: String): String =
    "https://$instance/c/$communityName"

  fun isValidUrl(url: String) = Patterns.WEB_URL.matcher(url).matches()

  fun isValidInstance(instance: String) = isValidUrl("https://$instance") && instance.isNotBlank()
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

fun SummitActivity.showMoreLinkOptions(
  url: String,
  text: String?,
  downloadContext: FileDownloadContext?,
) {
  showAdvancedLinkOptions(
    url = url,
    moreActionsHelper = moreActionsHelper,
    fragmentManager = supportFragmentManager,
    linkResolver = linkResolver,
    textOrFileName = text,
    downloadContext = downloadContext,
  )
}

fun SummitActivity.showMoreLinkOptions(person: PersonRef.PersonRefByName, text: String?) {
  showAdvancedLinkOptions(
    url = LinkUtils.getLinkForPerson(person),
    moreActionsHelper = moreActionsHelper,
    fragmentManager = supportFragmentManager,
    linkResolver = linkResolver,
    textOrFileName = text,
  )
}

fun SummitActivity.showMoreLinkOptions(person: PersonRef.PersonRefComplete, text: String?) {
  showAdvancedLinkOptions(
    url = LinkUtils.getLinkForPerson(person),
    moreActionsHelper = moreActionsHelper,
    fragmentManager = supportFragmentManager,
    linkResolver = linkResolver,
    textOrFileName = text,
  )
}
