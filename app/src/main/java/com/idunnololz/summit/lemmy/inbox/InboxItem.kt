package com.idunnololz.summit.lemmy.inbox

import android.os.Parcelable
import com.idunnololz.summit.api.dto.CommentReplyView
import com.idunnololz.summit.api.dto.CommentReportView
import com.idunnololz.summit.api.dto.PersonId
import com.idunnololz.summit.api.dto.PersonMentionView
import com.idunnololz.summit.api.dto.PostReportView
import com.idunnololz.summit.api.dto.PrivateMessageReportView
import com.idunnololz.summit.api.dto.PrivateMessageView
import com.idunnololz.summit.api.dto.RegistrationApplicationView
import com.idunnololz.summit.api.utils.instance
import com.idunnololz.summit.util.dateStringToTs
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonClassDiscriminator

interface CommentBackedItem {
  val score: Int
  val upvotes: Int
  val downvotes: Int
  val myVote: Int?
  val commentId: Int
  val commentPath: String
  val postId: Int
}

enum class RegistrationDecision {
  Approved,
  Declined,
  NoDecision,
  Pending,
}

sealed interface ReportItem : InboxItem

@Serializable
@JsonClassDiscriminator("t")
sealed interface InboxItem : Parcelable, LiteInboxItem {

  override val id: Int
  val authorId: PersonId
  val authorName: String
  val authorInstance: String
  val authorAvatar: String?
  val title: String
  val content: String
  val lastUpdate: String
  override val lastUpdateTs: Long
  val score: Int?
  val isDeleted: Boolean
  val isRemoved: Boolean
  val isRead: Boolean
  val canMarkAsRead: Boolean

  @Serializable
  @SerialName("1")
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
      id = reply.comment_reply.id,
      authorId = reply.creator.id,
      authorName = reply.creator.name,
      authorInstance = reply.creator.instance,
      authorAvatar = reply.creator.avatar,
      title = reply.post.name,
      content = reply.comment.content,
      lastUpdate = reply.comment.updated ?: reply.comment.published,
      lastUpdateTs = dateStringToTs(reply.comment.updated ?: reply.comment.published),
      score = reply.counts.score,
      upvotes = reply.counts.upvotes,
      downvotes = reply.counts.downvotes,
      myVote = reply.my_vote,
      commentId = reply.comment.id,
      commentPath = reply.comment.path,
      postId = reply.post.id,
      isDeleted = reply.comment.deleted,
      isRemoved = reply.comment.removed,
      isRead = reply.comment_reply.read,
    )

    override fun toString(): String = "ReplyInboxItem { content = $content }"

    override fun updateIsRead(isRead: Boolean): LiteInboxItem = copy(isRead = isRead)

    @IgnoredOnParcel
    override val canMarkAsRead: Boolean = true
  }

  @Serializable
  @SerialName("2")
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
      id = mention.person_mention.id,
      authorId = mention.creator.id,
      authorName = mention.creator.name,
      authorInstance = mention.creator.instance,
      authorAvatar = mention.creator.avatar,
      title = mention.post.name,
      content = mention.comment.content,
      lastUpdate = mention.comment.updated ?: mention.comment.published,
      lastUpdateTs = dateStringToTs(mention.comment.updated ?: mention.comment.published),
      score = mention.counts.score,
      upvotes = mention.counts.upvotes,
      downvotes = mention.counts.downvotes,
      myVote = mention.my_vote,
      commentId = mention.comment.id,
      commentPath = mention.comment.path,
      postId = mention.post.id,
      isDeleted = mention.comment.deleted,
      isRemoved = mention.comment.removed,
      isRead = mention.person_mention.read,
    )

    override fun toString(): String = "MentionInboxItem { content = $content }"

    override fun updateIsRead(isRead: Boolean): LiteInboxItem = copy(isRead = isRead)

    @IgnoredOnParcel
    override val canMarkAsRead: Boolean = true
  }

  @Serializable
  @SerialName("3")
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
    val targetUserName: String?,
    val targetAccountId: Long?,
    val targetAccountAvatar: String?,
    val targetInstance: String?,
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
      targetUserName = message.recipient.name,
      targetAccountId = message.recipient.id,
      targetAccountAvatar = message.recipient.avatar,
      targetInstance = message.recipient.instance,
    )

    override fun toString(): String = "MessageInboxItem { content = $content }"

    override fun updateIsRead(isRead: Boolean): LiteInboxItem = copy(isRead = isRead)

    @IgnoredOnParcel
    override val canMarkAsRead: Boolean = true
  }

  @Serializable
  @SerialName("4")
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
  ) : InboxItem, ReportItem {

    constructor(message: PrivateMessageReportView) : this(
      id = message.private_message_report.id,
      authorId = message.creator.id,
      authorName = message.creator.name,
      authorInstance = message.creator.instance,
      authorAvatar = message.creator.avatar,
      title = message.creator.name,
      content = message.private_message_report.reason,
      lastUpdate = message.private_message_report.updated ?: message.private_message_report.published,
      lastUpdateTs = dateStringToTs(
        message.private_message_report.updated
          ?: message.private_message_report.published,
      ),
      score = null,
      isDeleted = false,
      isRemoved = false,
      isRead = message.private_message_report.resolved,
    )

    override fun toString(): String = "ReportInboxItem { content = $content }"

    override fun updateIsRead(isRead: Boolean): LiteInboxItem = copy(isRead = isRead)

    @IgnoredOnParcel
    override val canMarkAsRead: Boolean = true
  }

  @Serializable
  @SerialName("5")
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
      lastUpdateTs = dateStringToTs(
        reportView.post_report.updated ?: reportView.post_report.published,
      ),
      score = reportView.counts.score,
      isDeleted = false,
      isRemoved = false,
      isRead = reportView.post_report.resolved,
      reportedPostId = reportView.post_report.post_id,
    )

    override fun toString(): String = "ReplyInboxItem { content = $content }"

    override fun updateIsRead(isRead: Boolean): LiteInboxItem = copy(isRead = isRead)

    @IgnoredOnParcel
    override val canMarkAsRead: Boolean = true
  }

  @Serializable
  @SerialName("6")
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
    override val commentId: Int,
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
      lastUpdateTs = dateStringToTs(
        reportView.comment_report.updated ?: reportView.comment_report.published,
      ),
      score = reportView.counts.score,
      isDeleted = false,
      isRemoved = false,
      isRead = reportView.comment_report.resolved,
      postId = reportView.post.id,
      commentId = reportView.comment.id,
      reportedCommentId = reportView.comment_report.comment_id,
      reportedCommentPath = reportView.comment.path,
    )

    override fun toString(): String = "ReplyInboxItem { content = $content }"

    override fun updateIsRead(isRead: Boolean): LiteInboxItem = copy(isRead = isRead)

    @IgnoredOnParcel
    override val canMarkAsRead: Boolean = true
  }

  @Serializable
  @SerialName("7")
  @Parcelize
  data class RegistrationApplicationInboxItem(
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
    val decision: RegistrationDecision,
    val denyReason: String?,
  ) : InboxItem {

    constructor(application: RegistrationApplicationView) : this(
      id = application.registration_application.id,
      authorId = application.creator.id,
      authorName = application.creator.name,
      authorInstance = application.creator.instance,
      authorAvatar = application.creator.avatar,
      title = "",
      content = application.registration_application.answer,
      lastUpdate = application.registration_application.published,
      lastUpdateTs = dateStringToTs(application.registration_application.published),
      score = 0,
      isDeleted = false,
      isRemoved = false,
      isRead = application.registration_application.deny_reason != null || application.admin != null,
      decision = if (application.admin != null) {
        if (application.creator_local_user.accepted_application) {
          RegistrationDecision.Approved
        } else {
          RegistrationDecision.Declined
        }
      } else {
        RegistrationDecision.NoDecision
      },
      denyReason = application.registration_application.deny_reason,
    )

    override fun toString(): String = "RegistrationApplicationInboxItem { content = $content }"

    override fun updateIsRead(isRead: Boolean): LiteInboxItem = copy(isRead = false)

    @IgnoredOnParcel
    override val canMarkAsRead: Boolean = false
  }

  val commentId: Int?
    get() = null
}

fun CommentReplyView.toInboxItem() = InboxItem.ReplyInboxItem(this)

fun PersonMentionView.toInboxItem() = InboxItem.MentionInboxItem(this)

fun PrivateMessageView.toInboxItem() = InboxItem.MessageInboxItem(this)

fun CommentReportView.toInboxItem() = InboxItem.ReportCommentInboxItem(this)

fun PostReportView.toInboxItem() = InboxItem.ReportPostInboxItem(this)

fun PrivateMessageReportView.toInboxItem() = InboxItem.ReportMessageInboxItem(this)
