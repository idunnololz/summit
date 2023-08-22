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
import kotlinx.parcelize.Parcelize

interface CommentBackedItem {
    val score: Int
    val myVote: Int?
    val commentId: Int
    val commentPath: String
    val postId: Int
}

sealed interface ReportItem

sealed interface InboxItem : Parcelable {

    val id: Int
    val authorId: PersonId
    val authorName: String
    val authorInstance: String
    val title: String
    val content: String
    val lastUpdate: String
    val lastUpdateTs: Long
    val score: Int?
    val isDeleted: Boolean
    val isRemoved: Boolean
    val isRead: Boolean

    @Parcelize
    data class ReplyInboxItem(
        override val id: Int,
        override val authorId: PersonId,
        override val authorName: String,
        override val authorInstance: String,
        override val title: String,
        override val content: String,
        override val lastUpdate: String,
        override val lastUpdateTs: Long,
        override val score: Int,
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
            reply.post.name,
            reply.comment.content,
            reply.comment.updated ?: reply.comment.published,
            dateStringToTs(reply.comment.updated ?: reply.comment.published),
            reply.counts.score,
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

    @Parcelize
    data class MentionInboxItem(
        override val id: Int,
        override val authorId: PersonId,
        override val authorName: String,
        override val authorInstance: String,
        override val title: String,
        override val content: String,
        override val lastUpdate: String,
        override val lastUpdateTs: Long,
        override val score: Int,
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
            mention.post.name,
            mention.comment.content,
            mention.comment.updated ?: mention.comment.published,
            dateStringToTs(mention.comment.updated ?: mention.comment.published),
            mention.counts.score,
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

    @Parcelize
    data class MessageInboxItem(
        override val id: Int,
        override val authorId: PersonId,
        override val authorName: String,
        override val authorInstance: String,
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
            message.private_message.id,
            message.creator.id,
            message.creator.name,
            message.creator.instance,
            message.creator.name,
            message.private_message.content,
            message.private_message.updated ?: message.private_message.published,
            dateStringToTs(
                message.private_message.updated
                    ?: message.private_message.published,
            ),
            null,
            message.private_message.deleted,
            isRemoved = false,
            message.private_message.read,
        )

        override fun toString(): String =
            "MessageInboxItem { content = $content }"
    }

    @Parcelize
    data class ReportMessageInboxItem(
        override val id: Int,
        override val authorId: PersonId,
        override val authorName: String,
        override val authorInstance: String,
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
            message.private_message.id,
            message.creator.id,
            message.creator.name,
            message.creator.instance,
            message.creator.name,
            message.private_message.content,
            message.private_message.updated ?: message.private_message.published,
            dateStringToTs(
                message.private_message.updated
                    ?: message.private_message.published,
            ),
            null,
            message.private_message.deleted,
            isRemoved = false,
            message.private_message.read,
        )

        override fun toString(): String =
            "ReportInboxItem { content = $content }"
    }

    @Parcelize
    data class ReportPostInboxItem(
        override val id: Int,
        override val authorId: PersonId,
        override val authorName: String,
        override val authorInstance: String,
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

    @Parcelize
    data class ReportCommentInboxItem(
        override val id: Int,
        override val authorId: PersonId,
        override val authorName: String,
        override val authorInstance: String,
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
