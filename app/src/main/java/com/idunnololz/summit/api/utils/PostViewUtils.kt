package com.idunnololz.summit.api.utils

import android.net.Uri
import com.idunnololz.summit.api.dto.PostView
import com.idunnololz.summit.util.PreviewInfo
import com.idunnololz.summit.video.VideoSizeHint

enum class PostType {
    Image,
    Video,
    Text,
}

fun PostView.getUniqueKey(): String =
    "${post.community_id.toULong()}_${post.id.toULong()}"

fun PostView.shouldHideItem(): Boolean = post.nsfw

fun PostView.getLowestResHiddenPreviewInfo(): PreviewInfo? {
    return PreviewInfo(
        url = post.thumbnail_url ?: return null,
        width = 16,
        height = 16
    )
}

fun PostView.getThumbnailUrl(reveal: Boolean): String? =
    if (shouldHideItem()) {
        if (reveal) {
            post.thumbnail_url
        } else {
            getLowestResHiddenPreviewInfo()?.getUrl()
        }
    } else {
        post.thumbnail_url
    }

fun PostView.getPreviewInfo(): PreviewInfo? {
    return null
}

fun PostView.getUrl(instance: String): String {
    return "https://${instance}/post/${post.id}"
}

fun PostView.getThumbnailPreviewInfo(): PreviewInfo? {
    return null
}

fun PostView.getVideoInfo(): VideoSizeHint? {
    if (post.embed_video_url == null) {
        return null
    }
    return VideoSizeHint(
        0,
        0,
        post.embed_video_url,
    )
}

fun PostView.getType(): PostType {
    if (post.thumbnail_url != null) {
        return PostType.Image
    }
//        if (post.embed_video_url != null) {
//            return PostType.Video
//        }
    return PostType.Text
}

val PostView.domain: String
    get() = Uri.parse(this.post.ap_id).host ?: this.community.domain