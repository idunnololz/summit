package com.idunnololz.summit.api.utils

import android.content.Context
import androidx.core.net.toUri
import com.idunnololz.summit.R
import com.idunnololz.summit.models.PostView
import com.idunnololz.summit.models.processed.PostTag
import com.idunnololz.summit.util.ContentUtils.isUrlImage
import com.idunnololz.summit.util.ContentUtils.isUrlVideo
import com.idunnololz.summit.util.PreviewInfo
import com.idunnololz.summit.util.crashLogger.crashLogger
import com.idunnololz.summit.video.VideoSizeHint

enum class PostType {
  Image,
  Video,
  Text,
  Link,
}

val PostView.isSpoiler
  get() = tags?.contains(PostTag.Spoiler) == true

fun PostView.getUniqueKey(): String = "${post.community_id.toULong()}_${post.id.toULong()}"

fun PostView.shouldBlurItem(): Boolean = post.nsfw || isSpoiler

fun PostView.hideReasonMessage(context: Context): String =
  when {
    post.nsfw -> {
      context.getString(R.string.reveal_warning_nsfw)
    }
    isSpoiler -> {
      context.getString(R.string.reveal_warning_spoiler)
    }
    else -> {
      context.getString(R.string.reveal_warning_default)
    }
  }

fun PostView.getLowestResHiddenPreviewInfo(): PreviewInfo? {
  return PreviewInfo(
    url = post.thumbnail_url ?: return null,
    width = 16,
    height = 16,
  )
}

fun PostView.getImageUrl(reveal: Boolean): String? = if (shouldBlurItem()) {
  if (reveal) {
    getBestImageUrl()
  } else {
    getLowestResHiddenPreviewInfo()?.getUrl()
  }
} else {
  getBestImageUrl()
}

fun PostView.getBestImageUrl(): String? {
  val postUrl = post.url
  if (postUrl == null || !isUrlImage(postUrl)) {
    return getThumbnailUrl(reveal = true)
  }
  return postUrl
}

fun PostView.getThumbnailUrl(reveal: Boolean): String? = if (shouldBlurItem()) {
  if (reveal) {
    post.thumbnail_url
  } else {
    getLowestResHiddenPreviewInfo()?.getUrl()
  }
} else {
  post.thumbnail_url
}

fun PostView.getPreviewInfo(): PreviewInfo? = null

fun PostView.getUrl(instance: String): String = "https://$instance/post/${post.id}"

fun PostView.getThumbnailPreviewInfo(): PreviewInfo? = null

fun PostView.getVideoInfo(embedded: Boolean = false): VideoSizeHint? {
  val originalUrl = if (embedded) {
    if (post.embed_video_url != null) {
      post.embed_video_url
    } else {
      null
    }
  } else if (isUrlVideo(post.thumbnail_url ?: "")) {
    post.thumbnail_url
  } else if (isUrlVideo(post.url ?: "")) {
    post.url
  } else {
    null
  }

  originalUrl ?: return null

  var url: String = originalUrl

  try {
    val uri = url.toUri()
    if (uri.host == "redgifs.com" || uri.host == "www.redgifs.com") {
      val pathSegments = uri.pathSegments
      if (pathSegments.getOrNull(0) == "watch") {
        val videoKey = pathSegments.getOrNull(1)

        if (videoKey != null) {
          url = "https://api.redgifs.com/v2/gifs/$videoKey/sd.m3u8"
        }
      }
    } else if (uri.host == "api.redgifs.com") {
      // example:
      // https://api.redgifs.com/v2/gifs/scarymilkyclam/files/ScaryMilkyClam-silent.mp4
      val pathSegments = uri.pathSegments
      if (pathSegments.getOrNull(0) == "v2" && pathSegments.getOrNull(1) == "gifs") {
        val videoKey = pathSegments.getOrNull(2)

        if (videoKey != null) {
          url = "https://api.redgifs.com/v2/gifs/$videoKey/sd.m3u8"
        }
      }
    }
  } catch (e: Exception) {
    // best effort!

    crashLogger?.recordException(e)
  }

  return VideoSizeHint(
    width = 0,
    height = 0,
    videoUrl = url,
  )
}

fun PostView.getType(): PostType {
  val url = post.url
  if (url != null && isUrlImage(url)) {
    return PostType.Image
  }
  if (url != null && isUrlVideo(url)) {
    return PostType.Video
  }
  if (url != null) {
    return PostType.Link
  }
  return PostType.Text
}

fun PostView.getDominantType(): PostType {
  val embedVideoUrl = post.embed_video_url
  if (embedVideoUrl != null && isUrlVideo(embedVideoUrl)) {
    return PostType.Video
  }
  val url = post.url
  if (url != null) {
    if (isUrlImage(url)) {
      return PostType.Image
    }
    if (isUrlVideo(url)) {
      return PostType.Video
    }
    return PostType.Link
  }
  return PostType.Text
}

val PostView.instance: String
  get() = this.post.ap_id.toUri().host ?: this.community.instance
