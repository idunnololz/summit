package com.idunnololz.summit.api.dto

import android.os.Parcelable
import com.idunnololz.summit.util.PreviewInfo
import com.idunnololz.summit.video.VideoSizeHint
import kotlinx.parcelize.Parcelize

data class PersonViewSafe(
    val person: Person,
    val counts: PersonAggregates,
)

data class PersonMentionView(
    val person_mention: PersonMention,
    val comment: Comment,
    val creator: Person,
    val post: Post,
    val community: CommunitySafe,
    val recipient: Person,
    val counts: CommentAggregates,
    val creator_banned_from_community: Boolean,
    val subscribed: SubscribedType,
    val saved: Boolean,
    val creator_blocked: Boolean,
    val my_vote: Int?,
)

data class LocalUserView(
    val local_user: LocalUser,
    val person: Person,
    val counts: PersonAggregates,
)

data class SiteView(
    val site: Site,
    val local_site: LocalSite,
    // val local_site_rate_limit: LocalSiteRateLimit;
    val taglines: List<Tagline>?,
    val counts: SiteAggregates,
)

data class PrivateMessageView(
    val private_message: PrivateMessage,
    val creator: Person,
    val recipient: Person,
)

enum class PostType {
    Image,
    Video,
    Text,
}

sealed interface LemmyListView

@Parcelize
data class PostView(
    val post: Post,
    val creator: Person,
    val community: CommunitySafe,
    val creator_banned_from_community: Boolean,
    val counts: PostAggregates,
    val subscribed: SubscribedType,
    val saved: Boolean,
    val read: Boolean,
    val creator_blocked: Boolean,
    val my_vote: Int?,
    val unread_comments: Int,
) : Parcelable, LemmyListView {
    fun getUniqueKey(): String =
        "${post.community_id.toULong()}_${post.id.toULong()}"

    fun shouldHideItem(): Boolean = post.nsfw

    fun getLowestResHiddenPreviewInfo(): PreviewInfo? {
        return PreviewInfo(
            url = post.thumbnail_url ?: return null,
            width = 16,
            height = 16
        )
    }

    fun getThumbnailUrl(reveal: Boolean): String? =
        if (shouldHideItem()) {
            if (reveal) {
                post.thumbnail_url
            } else {
                getLowestResHiddenPreviewInfo()?.getUrl()
            }
        } else {
            post.thumbnail_url
        }

    fun getDomain(): String? = community.domain

    fun getPreviewInfo(): PreviewInfo? {
        return null
    }

    fun getUrl(instance: String): String {
        return "https://${instance}/post/${post.id}"
    }

    fun getThumbnailPreviewInfo(): PreviewInfo? {
        return null
    }

    fun getVideoInfo(): VideoSizeHint? {
        if (post.embed_video_url == null) {
            return null
        }
        return VideoSizeHint(
            0,
            0,
            post.embed_video_url,
        )
    }

    fun getType(): PostType {
        if (post.thumbnail_url != null) {
            return PostType.Image
        }
//        if (post.embed_video_url != null) {
//            return PostType.Video
//        }
        return PostType.Text
    }
}

data class PostReportView(
    val post_report: PostReport,
    val post: Post,
    val community: CommunitySafe,
    val creator: Person,
    val post_creator: Person,
    val creator_banned_from_community: Boolean,
    val my_vote: Int?,
    val counts: PostAggregates,
    val resolver: Person?,
)

data class CommentView(
    val comment: Comment,
    val creator: Person,
    val post: Post,
    val community: CommunitySafe,
    val counts: CommentAggregates,
    val creator_banned_from_community: Boolean,
    val subscribed: SubscribedType,
    val saved: Boolean,
    val creator_blocked: Boolean,
    val my_vote: Int?,
) : LemmyListView {

    fun getDepth(): Int =
        comment.getDepth()

    fun getUniqueKey(): String =
        "comment_${comment.id}"
}

data class CommentReplyView(
    val comment_reply: CommentReply,
    val comment: Comment,
    val creator: Person,
    val post: Post,
    val community: CommunitySafe,
    val recipient: Person,
    val counts: CommentAggregates,
    val creator_banned_from_community: Boolean,
    val subscribed: SubscribedType,
    val saved: Boolean,
    val creator_blocked: Boolean,
    val my_vote: Int?,
)

data class CommentReportView(
    val comment_report: CommentReport,
    val comment: Comment,
    val post: Post,
    val community: CommunitySafe,
    val creator: Person,
    val comment_creator: Person,
    val counts: CommentAggregates,
    val creator_banned_from_community: Boolean,
    val my_vote: Int?,
    val resolver: Person?,
)

data class ModAddCommunityView(
    val mod_add_community: ModAddCommunity,
    val moderator: Person?,
    val community: CommunitySafe,
    val modded_person: Person,
)

data class ModTransferCommunityView(
    val mod_transfer_community: ModTransferCommunity,
    val moderator: Person?,
    val community: CommunitySafe,
    val modded_person: Person,
)

data class ModAddView(
    val mod_add: ModAdd,
    val moderator: Person?,
    val modded_person: Person,
)

data class ModBanFromCommunityView(
    val mod_ban_from_community: ModBanFromCommunity,
    val moderator: Person?,
    val community: CommunitySafe,
    val banned_person: Person,
)

data class ModBanView(
    val mod_ban: ModBan,
    val moderator: Person?,
    val banned_person: Person,
)

data class ModLockPostView(
    val mod_lock_post: ModLockPost,
    val moderator: Person?,
    val post: Post,
    val community: CommunitySafe,
)

data class ModRemoveCommentView(
    val mod_remove_comment: ModRemoveComment,
    val moderator: Person?,
    val comment: Comment,
    val commenter: Person,
    val post: Post,
    val community: CommunitySafe,
)

data class ModRemoveCommunityView(
    val mod_remove_community: ModRemoveCommunity,
    val moderator: Person?,
    val community: CommunitySafe,
)

data class ModRemovePostView(
    val mod_remove_post: ModRemovePost,
    val moderator: Person?,
    val post: Post,
    val community: CommunitySafe,
)

data class ModFeaturePostView(
    val mod_feature_post: ModFeaturePost,
    val moderator: Person?,
    val post: Post,
    val community: CommunitySafe,
)

data class CommunityFollowerView(
    val community: CommunitySafe,
    val follower: Person,
)

data class CommunityBlockView(
    val person: Person,
    val community: CommunitySafe,
)

data class CommunityModeratorView(
    val community: CommunitySafe,
    val moderator: Person,
)

data class CommunityPersonBanView(
    val community: CommunitySafe,
    val person: Person,
)

data class PersonBlockView(
    val person: Person,
    val target: Person,
)

data class CommunityView(
    val community: CommunitySafe,
    val subscribed: SubscribedType,
    val blocked: Boolean,
    val counts: CommunityAggregates,
)
