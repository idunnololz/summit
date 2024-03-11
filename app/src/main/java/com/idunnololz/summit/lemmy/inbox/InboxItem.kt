package com.idunnololz.summit.lemmy.inbox

import android.os.Parcelable
import com.idunnololz.summit.api.dto.CommentReplyView
import com.idunnololz.summit.api.dto.CommentReportView
import com.idunnololz.summit.api.dto.PersonId
import com.idunnololz.summit.api.dto.PersonMentionView
import com.idunnololz.summit.api.dto.PostReportView
import com.idunnololz.summit.api.dto.PrivateMessageReportView
import com.idunnololz.summit.api.dto.PrivateMessageView
import com.idunnololz.summit.api.utils.instance
import com.idunnololz.summit.util.dateStringToTs
import com.squareup.moshi.JsonClass
import dev.zacsweers.moshix.sealed.annotations.TypeLabel
import kotlinx.parcelize.Parcelize

interface CommentBackedItem {
    val score: Int
    val upvotes: Int
    val downvotes: Int
    val myVote: Int?
    val commentId: Int
    val commentPath: String
    val postId: Int
}

sealed interface ReportItem

@JsonClass(generateAdapter = true, generator = "sealed:t")
sealed interface InboxItem : Parcelable {

    val id: Int
    val authorId: PersonId
    val authorName: String
    val authorInstance: String
    val authorAvatar: String?
    val title: String
    val content: String
    val lastUpdate: String
    val lastUpdateTs: Long
    val score: Int?
    val isDeleted: Boolean
    val isRemoved: Boolean
    val isRead: Boolean

    @JsonClass(generateAdapter = true)
    @TypeLabel("1")
    @Parcelize
    data class ReplyInboxItem(
        override val id: Int,
        override val authorId: PersonId,
        override val authorName: String,
        override val authorInstance: String,
        override val authorAvatar: String?,
        override val title: String,
        override val content: String,
        override val lastUpdate: String,
        override val lastUpdateTs: Long,
        override val score: Int,
        override val upvotes: Int,
        override val downvotes: Int,
        override val myVote: Int?,
        override val commentId: Int,
        override val commentPath: String,
        override val postId: Int,
        override val isDeleted: Boolean,
        override val isRemoved: Boolean,
        override val isRead: Boolean,
    ) : InboxItem, CommentBackedItem {

        constructor(reply: CommentReplyView) : this(
            reply.comment_reply.id,
            reply.creator.id,
            reply.creator.name,
            reply.creator.instance,
            reply.creator.avatar,
            reply.post.name,
            reply.comment.content,
            reply.comment.updated ?: reply.comment.published,
            dateStringToTs(reply.comment.updated ?: reply.comment.published),
            reply.counts.score,
            reply.counts.upvotes,
            reply.counts.downvotes,
            reply.my_vote,
            reply.comment.id,
            reply.comment.path,
            reply.post.id,
            reply.comment.deleted,
            reply.comment.removed,
            reply.comment_reply.read,
        )

        override fun toString(): String =
            "ReplyInboxItem { content = $content }"
    }

    @JsonClass(generateAdapter = true)
    @TypeLabel("2")
    @Parcelize
    data class MentionInboxItem(
        override val id: Int,
        override val authorId: PersonId,
        override val authorName: String,
        override val authorInstance: String,
        override val authorAvatar: String?,
        override val title: String,
        override val content: String,
        override val lastUpdate: String,
        override val lastUpdateTs: Long,
        override val score: Int,
        override val upvotes: Int,
        override val downvotes: Int,
        override val myVote: Int?,
        override val commentId: Int,
        override val commentPath: String,
        override val postId: Int,
        override val isDeleted: Boolean,
        override val isRemoved: Boolean,
        override val isRead: Boolean,
    ) : InboxItem, CommentBackedItem {

        constructor(mention: PersonMentionView) : this(
            mention.person_mention.id,
            mention.creator.id,
            mention.creator.name,
            mention.creator.instance,
            mention.creator.avatar,
            mention.post.name,
            mention.comment.content,
            mention.comment.updated ?: mention.comment.published,
            dateStringToTs(mention.comment.updated ?: mention.comment.published),
            mention.counts.score,
            mention.counts.upvotes,
            mention.counts.downvotes,
            mention.my_vote,
            mention.comment.id,
            mention.comment.path,
            mention.post.id,
            mention.comment.deleted,
            mention.comment.removed,
            mention.person_mention.read,
        )

        override fun toString(): String =
            "MentionInboxItem { content = $content }"
    }

    @JsonClass(generateAdapter = true)
    @TypeLabel("3")
    @Parcelize
    data class MessageInboxItem(
        override val id: Int,
        override val authorId: PersonId,
        override val authorName: String,
        override val authorInstance: String,
        override val authorAvatar: String?,
        override val title: String,
        override val content: String,
        override val lastUpdate: String,
        override val lastUpdateTs: Long,
        override val score: Int?,
        override val isDeleted: Boolean,
        override val isRemoved: Boolean,
        override val isRead: Boolean,
    ) : InboxItem {

        constructor(message: PrivateMessageView) : this(
            id = message.private_message.id,
            authorId = message.creator.id,
            authorName = message.creator.name,
            authorInstance = message.creator.instance,
            authorAvatar = message.creator.avatar,
            title = message.creator.name,
            content = message.private_message.content,
            lastUpdate = message.private_message.updated ?: message.private_message.published,
            lastUpdateTs = dateStringToTs(
                message.private_message.updated
                    ?: message.private_message.published,
            ),
            score = null,
            isDeleted = message.private_message.deleted,
            isRemoved = false,
            isRead = message.private_message.read,
        )

        override fun toString(): String =
            "MessageInboxItem { content = $content }"
    }

    @JsonClass(generateAdapter = true)
    @TypeLabel("4")
    @Parcelize
    data class ReportMessageInboxItem(
        override val id: Int,
        override val authorId: PersonId,
        override val authorName: String,
        override val authorInstance: String,
        override val authorAvatar: String?,
        override val title: String,
        override val content: String,
        override val lastUpdate: String,
        override val lastUpdateTs: Long,
        override val score: Int?,
        override val isDeleted: Boolean,
        override val isRemoved: Boolean,
        override val isRead: Boolean,
    ) : InboxItem {

        constructor(message: PrivateMessageReportView) : this(
            id = message.private_message.id,
            authorId = message.creator.id,
            authorName = message.creator.name,
            authorInstance = message.creator.instance,
            authorAvatar = message.creator.avatar,
            title = message.creator.name,
            content = message.private_message.content,
            lastUpdate = message.private_message.updated ?: message.private_message.published,
            lastUpdateTs = dateStringToTs(
                message.private_message.updated
                    ?: message.private_message.published,
            ),
            score = null,
            isDeleted = message.private_message.deleted,
            isRemoved = false,
            isRead = message.private_message.read,
        )

        override fun toString(): String =
            "ReportInboxItem { content = $content }"
    }

    @JsonClass(generateAdapter = true)
    @TypeLabel("5")
    @Parcelize
    data class ReportPostInboxItem(
        override val id: Int,
        override val authorId: PersonId,
        override val authorName: String,
        override val authorInstance: String,
        override val authorAvatar: String?,
        override val title: String,
        override val content: String,
        override val lastUpdate: String,
        override val lastUpdateTs: Long,
        override val score: Int?,
        override val isDeleted: Boolean,
        override val isRemoved: Boolean,
        override val isRead: Boolean,
        val reportedPostId: Int,
    ) : InboxItem, ReportItem {

        constructor(reportView: PostReportView) : this(
            id = reportView.post_report.id,
            authorId = reportView.creator.id,
            authorName = reportView.creator.name,
            authorInstance = reportView.creator.instance,
            authorAvatar = reportView.creator.avatar,
            title = reportView.post.name,
            content = reportView.post_report.reason,
            lastUpdate = reportView.post_report.updated ?: reportView.post_report.published,
            lastUpdateTs = dateStringToTs(reportView.post_report.updated ?: reportView.post_report.published),
            score = reportView.counts.score,
            isDeleted = false,
            isRemoved = false,
            isRead = reportView.post_report.resolved,
            reportedPostId = reportView.post_report.post_id,
        )

        override fun toString(): String =
            "ReplyInboxItem { content = $content }"
    }

    @JsonClass(generateAdapter = true)
    @TypeLabel("6")
    @Parcelize
    data class ReportCommentInboxItem(
        override val id: Int,
        override val authorId: PersonId,
        override val authorName: String,
        override val authorInstance: String,
        override val authorAvatar: String?,
        override val title: String,
        override val content: String,
        override val lastUpdate: String,
        override val lastUpdateTs: Long,
        override val score: Int?,
        override val isDeleted: Boolean,
        override val isRemoved: Boolean,
        override val isRead: Boolean,
        val postId: Int,
        val reportedCommentId: Int,
        val reportedCommentPath: String,
    ) : InboxItem, ReportItem {

        constructor(reportView: CommentReportView) : this(
            id = reportView.comment_report.id,

            authorId = reportView.creator.id,
            authorName = reportView.creator.name,
            authorInstance = reportView.creator.instance,
            authorAvatar = reportView.creator.avatar,
            title = reportView.post.name,
            content = reportView.comment_report.reason,
            lastUpdate = reportView.comment_report.updated ?: reportView.comment_report.published,
            lastUpdateTs = dateStringToTs(reportView.comment_report.updated ?: reportView.comment_report.published),
            score = reportView.counts.score,
            isDeleted = false,
            isRemoved = false,
            isRead = reportView.comment_report.resolved,
            postId = reportView.post.id,
            reportedCommentId = reportView.comment_report.comment_id,
            reportedCommentPath = reportView.comment.path,
        )

        override fun toString(): String =
            "ReplyInboxItem { content = $content }"
    }

    val commentId: Int?
        get() = when (this) {
            is MentionInboxItem -> commentId
            is MessageInboxItem -> null
            is ReplyInboxItem -> commentId
            is ReportMessageInboxItem -> null
            is ReportCommentInboxItem -> null
            is ReportPostInboxItem -> null
        }
}

fun CommentReplyView.toInboxItem() =
    InboxItem.ReplyInboxItem(this)

fun PersonMentionView.toInboxItem() =
    InboxItem.MentionInboxItem(this)

fun PrivateMessageView.toInboxItem() =
    InboxItem.MessageInboxItem(this)

fun CommentReportView.toInboxItem() =
    InboxItem.ReportCommentInboxItem(this)

fun PostReportView.toInboxItem() =
    InboxItem.ReportPostInboxItem(this)