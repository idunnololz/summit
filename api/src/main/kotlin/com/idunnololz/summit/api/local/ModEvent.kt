package com.idunnololz.summit.api.local

import com.idunnololz.summit.api.converters.toComment
import com.idunnololz.summit.api.converters.toCommunity
import com.idunnololz.summit.api.converters.toInstance
import com.idunnololz.summit.api.converters.toPerson
import com.idunnololz.summit.api.converters.toPost
import com.idunnololz.summit.api.dto.lemmy.Comment
import com.idunnololz.summit.api.dto.lemmy.Community
import com.idunnololz.summit.api.dto.lemmy.GetModlogResponse
import com.idunnololz.summit.api.dto.lemmy.Instance
import com.idunnololz.summit.api.dto.lemmy.ModlogActionType
import com.idunnololz.summit.api.dto.lemmy.Person
import com.idunnololz.summit.api.dto.lemmy.Post
import com.idunnololz.summit.api.dto.lemmy.v4.models.ModlogKind
import com.idunnololz.summit.api.dto.lemmy.v4.models.ModlogView
import java.time.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonClassDiscriminator

@Serializable
@JsonClassDiscriminator("t")
sealed interface ModEvent {

  val id: Int
  val actionOrder: ActionType
  val ts: Long
  val actionType: ModlogActionType

  /**
   * The person who performed the action is called the "agent".
   */
  val agent: Person?

  /**
   * When a moderator removes a post.
   */
  @Serializable
  @SerialName("1")
  data class ModRemovePostViewEvent(
    override val id: Int,
    override val actionOrder: ActionType,
    override val ts: Long,
    override val agent: Person?,
    val removed: Boolean,
    val post: Post,
    val community: Community,
    val reason: String?,
  ) : ModEvent {
    override val actionType = ModlogActionType.ModRemovePost
  }

  /**
   * When a moderator locks a post (prevents new comments being made).
   */
  @Serializable
  @SerialName("2")
  data class ModLockPostViewEvent(
    override val id: Int,
    override val actionOrder: ActionType,
    override val ts: Long,
    override val agent: Person?,
    val locked: Boolean,
    val post: Post,
    val community: Community,
  ) : ModEvent {
    override val actionType = ModlogActionType.ModLockPost
  }

  /**
   * When a moderator features a post on a community (pins it to the top).
   */
  @Serializable
  @SerialName("3")
  data class ModFeaturePostViewEvent(
    override val id: Int,
    override val actionOrder: ActionType,
    override val ts: Long,
    override val agent: Person?,
    val featured: Boolean,
    val post: Post,
    val community: Community,
  ) : ModEvent {
    override val actionType = ModlogActionType.ModFeaturePost
  }

  /**
   * When a moderator removes a comment.
   */
  @Serializable
  @SerialName("4")
  data class ModRemoveCommentViewEvent(
    override val id: Int,
    override val actionOrder: ActionType,
    override val ts: Long,
    override val agent: Person?,
    val removed: Boolean,
    val comment: Comment,
    val post: Post,
    val community: Community,
    val person: Person,
    val reason: String?,
  ) : ModEvent {
    override val actionType = ModlogActionType.ModRemoveComment
  }

  /**
   * When a moderator removes a community.
   */
  @Serializable
  @SerialName("5")
  data class ModRemoveCommunityViewEvent(
    override val id: Int,
    override val actionOrder: ActionType,
    override val ts: Long,
    override val agent: Person?,
    val removed: Boolean,
    val expires: Long?,
    val community: Community,
    val reason: String?,
  ) : ModEvent {
    override val actionType = ModlogActionType.ModRemoveCommunity
  }

  /**
   * When someone is banned from a community.
   */
  @Serializable
  @SerialName("6")
  data class ModBanFromCommunityViewEvent(
    override val id: Int,
    override val actionOrder: ActionType,
    override val ts: Long,
    override val agent: Person?,
    val banned: Boolean,
    val expires: Long?,
    val person: Person,
    val community: Community,
    val reason: String?,
  ) : ModEvent {
    override val actionType = ModlogActionType.ModBanFromCommunity
  }

  /**
   * When someone is banned from the site.
   */
  @Serializable
  @SerialName("7")
  data class ModBanViewEvent(
    override val id: Int,
    override val actionOrder: ActionType,
    override val ts: Long,
    override val agent: Person?,
    val banned: Boolean,
    val expires: Long?,
    val person: Person,
    val reason: String?,
  ) : ModEvent {
    override val actionType = ModlogActionType.ModBan
  }

  /**
   * When someone is added as a community moderator.
   */
  @Serializable
  @SerialName("8")
  data class ModAddCommunityViewEvent(
    override val id: Int,
    override val actionOrder: ActionType,
    override val ts: Long,
    override val agent: Person?,
    val removed: Boolean,
    val person: Person,
    val community: Community,
  ) : ModEvent {
    override val actionType = ModlogActionType.ModAddCommunity
  }

  /**
   * When a moderator transfers a community to a new owner.
   */
  @Serializable
  @SerialName("9")
  data class ModTransferCommunityViewEvent(
    override val id: Int,
    override val actionOrder: ActionType,
    override val ts: Long,
    override val agent: Person?,
    val person: Person,
    val community: Community,
  ) : ModEvent {
    override val actionType = ModlogActionType.ModTransferCommunity
  }

  /**
   * When someone is added as a site moderator.
   */
  @Serializable
  @SerialName("10")
  data class ModAddViewEvent(
    override val id: Int,
    override val actionOrder: ActionType,
    override val ts: Long,
    override val agent: Person?,
    val removed: Boolean,
    val person: Person,
  ) : ModEvent {
    override val actionType = ModlogActionType.ModAdd
  }

  @Serializable
  @SerialName("11")
  data class AdminPurgePersonViewEvent(
    override val id: Int,
    override val actionOrder: ActionType,
    override val ts: Long,
    override val agent: Person?,
    val reason: String?,
  ) : ModEvent {
    override val actionType = ModlogActionType.AdminPurgePerson
  }

  @Serializable
  @SerialName("12")
  data class AdminPurgeCommunityViewEvent(
    override val id: Int,
    override val actionOrder: ActionType,
    override val ts: Long,
    override val agent: Person?,
    val reason: String?,
  ) : ModEvent {
    override val actionType = ModlogActionType.AdminPurgeCommunity
  }

  @Serializable
  @SerialName("13")
  data class AdminPurgePostViewEvent(
    override val id: Int,
    override val actionOrder: ActionType,
    override val ts: Long,
    override val agent: Person?,
    val community: Community,
    val reason: String?,
  ) : ModEvent {
    override val actionType = ModlogActionType.AdminPurgePost
  }

  @Serializable
  @SerialName("14")
  data class AdminPurgeCommentViewEvent(
    override val id: Int,
    override val actionOrder: ActionType,
    override val ts: Long,
    override val agent: Person?,
    val post: Post,
    val reason: String?,
  ) : ModEvent {
    override val actionType = ModlogActionType.AdminPurgeComment
  }

  /**
   * When a community is hidden from public view.
   */
  @Serializable
  @SerialName("15")
  data class ModHideCommunityViewEvent(
    override val id: Int,
    override val actionOrder: ActionType,
    override val ts: Long,
    override val agent: Person?,
    val hidden: Boolean,
    val community: Community,
    val reason: String?,
  ) : ModEvent {
    override val actionType = ModlogActionType.ModHideCommunity
  }

  /**
   * When an instance is federated to this instance.
   */
  @Serializable
  @SerialName("16")
  data class AdminBlockInstanceEvent(
    override val id: Int,
    override val actionOrder: ActionType,
    override val ts: Long,
    override val agent: Person?,
    val blocked: Boolean,
    val instance: Instance,
    val reason: String?,
  ) : ModEvent {
    override val actionType = ModlogActionType.ModHideCommunity
  }

  /**
   * When a admin features a post on the instance (pins it to the top).
   */
  @Serializable
  @SerialName("17")
  data class AdminFeaturePostViewEvent(
    override val id: Int,
    override val actionOrder: ActionType,
    override val ts: Long,
    override val agent: Person?,
    val featured: Boolean,
    val post: Post,
  ) : ModEvent {
    override val actionType = ModlogActionType.ModFeaturePost
  }

  /**
   * When a moderator locks a comment (prevents new comments being made).
   */
  @Serializable
  @SerialName("18")
  data class ModLockCommentViewEvent(
    override val id: Int,
    override val actionOrder: ActionType,
    override val ts: Long,
    override val agent: Person?,
    val locked: Boolean,
    val comment: Comment,
    val post: Post,
    val community: Community,
    val person: Person,
  ) : ModEvent {
    override val actionType = ModlogActionType.ModLockPost
  }

  /**
   * When a moderator issues a warning on a post.
   */
  @Serializable
  @SerialName("19")
  data class ModWarnPostViewEvent(
    override val id: Int,
    override val actionOrder: ActionType,
    override val ts: Long,
    override val agent: Person?,
    val warned: Boolean,
    val post: Post,
    val community: Community,
    val person: Person,
    val reason: String?,
  ) : ModEvent {
    override val actionType = ModlogActionType.ModLockPost
  }

  /**
   * When a moderator issues a warning on a comment.
   */
  @Serializable
  @SerialName("20")
  data class ModWarnCommentViewEvent(
    override val id: Int,
    override val actionOrder: ActionType,
    override val ts: Long,
    override val agent: Person?,
    val warned: Boolean,
    val comment: Comment,
    val post: Post,
    val community: Community,
    val person: Person,
    val reason: String?,
  ) : ModEvent {
    override val actionType = ModlogActionType.ModLockPost
  }
}

enum class ActionType {
  Mod,
  Admin,
}

fun dateStringToTs(dateString: String): Long = Instant.parse(
  if (dateString.endsWith("Z")) {
    dateString
  } else {
    dateString + "Z"
  },
).toEpochMilli()

fun GetModlogResponse.toModEvents(): MutableList<ModEvent> {
  val events = mutableListOf<ModEvent>()

  this.removed_posts.mapTo(events) {
    ModEvent.ModRemovePostViewEvent(
      id = it.mod_remove_post.id,
      actionOrder = ActionType.Mod,
      ts = dateStringToTs(it.mod_remove_post.when_),
      agent = it.moderator,
      removed = it.mod_remove_post.removed,
      post = it.post,
      community = it.community,
      reason = it.mod_remove_post.reason,
    )
  }
  this.locked_posts.mapTo(events) {
    ModEvent.ModLockPostViewEvent(
      id = it.mod_lock_post.id,
      actionOrder = ActionType.Mod,
      ts = dateStringToTs(it.mod_lock_post.when_),
      agent = it.moderator,
      locked = it.mod_lock_post.locked,
      post = it.post,
      community = it.community,
    )
  }
  this.featured_posts.mapTo(events) {
    ModEvent.ModFeaturePostViewEvent(
      id = it.mod_feature_post.id,
      actionOrder = ActionType.Mod,
      ts = dateStringToTs(it.mod_feature_post.when_),
      agent = it.moderator,
      featured = it.mod_feature_post.featured,
      post = it.post,
      community = it.community,
    )
  }
  this.removed_comments.mapTo(events) {
    ModEvent.ModRemoveCommentViewEvent(
      id = it.mod_remove_comment.id,
      actionOrder = ActionType.Mod,
      ts = dateStringToTs(it.mod_remove_comment.when_),
      agent = it.moderator,
      removed = it.mod_remove_comment.removed,
      comment = it.comment,
      post = it.post,
      community = it.community,
      person = it.commenter,
      reason = it.mod_remove_comment.reason,
    )
  }
  this.removed_communities.mapTo(events) {
    ModEvent.ModRemoveCommunityViewEvent(
      id = it.mod_remove_community.id,
      actionOrder = ActionType.Mod,
      ts = dateStringToTs(it.mod_remove_community.when_),
      agent = it.moderator,
      removed = it.mod_remove_community.removed,
      expires = it.mod_remove_community.expires?.let { dateStringToTs(it) },
      community = it.community,
      reason = it.mod_remove_community.reason,
    )
  }
  this.banned_from_community.mapTo(events) {
    ModEvent.ModBanFromCommunityViewEvent(
      id = it.mod_ban_from_community.id,
      actionOrder = ActionType.Mod,
      ts = dateStringToTs(it.mod_ban_from_community.when_),
      agent = it.moderator,
      banned = it.mod_ban_from_community.banned,
      expires = it.mod_ban_from_community.expires?.let { dateStringToTs(it) },
      person = it.banned_person,
      community = it.community,
      reason = it.mod_ban_from_community.reason,
    )
  }
  this.banned.mapTo(events) {
    ModEvent.ModBanViewEvent(
      id = it.mod_ban.id,
      actionOrder = ActionType.Mod,
      ts = dateStringToTs(it.mod_ban.when_),
      agent = it.moderator,
      banned = it.mod_ban.banned,
      expires = it.mod_ban.expires?.let { dateStringToTs(it) },
      person = it.banned_person,
      reason = it.mod_ban.reason,
    )
  }
  this.added_to_community.mapTo(events) {
    ModEvent.ModAddCommunityViewEvent(
      id = it.mod_add_community.id,
      actionOrder = ActionType.Mod,
      ts = dateStringToTs(it.mod_add_community.when_),
      agent = it.moderator,
      removed = it.mod_add_community.removed,
      person = it.modded_person,
      community = it.community,
    )
  }
  this.transferred_to_community.mapTo(events) {
    ModEvent.ModTransferCommunityViewEvent(
      id = it.mod_transfer_community.id,
      actionOrder = ActionType.Mod,
      ts = dateStringToTs(it.mod_transfer_community.when_),
      agent = it.moderator,
      person = it.modded_person,
      community = it.community,
    )
  }
  this.added.mapTo(events) {
    ModEvent.ModAddViewEvent(
      id = it.mod_add.id,
      actionOrder = ActionType.Mod,
      ts = dateStringToTs(it.mod_add.when_),
      agent = it.moderator,
      removed = it.mod_add.removed,
      person = it.modded_person,
    )
  }
  this.admin_purged_persons.mapTo(events) {
    ModEvent.AdminPurgePersonViewEvent(
      id = it.admin_purge_person.id,
      actionOrder = ActionType.Admin,
      ts = dateStringToTs(it.admin_purge_person.when_),
      agent = it.admin,
      reason = it.admin_purge_person.reason,
    )
  }
  this.admin_purged_communities.mapTo(events) {
    ModEvent.AdminPurgeCommunityViewEvent(
      id = it.admin_purge_community.id,
      actionOrder = ActionType.Admin,
      ts = dateStringToTs(it.admin_purge_community.when_),
      agent = it.admin,
      reason = it.admin_purge_community.reason,
    )
  }
  this.admin_purged_posts.mapTo(events) {
    ModEvent.AdminPurgePostViewEvent(
      id = it.admin_purge_post.id,
      actionOrder = ActionType.Admin,
      ts = dateStringToTs(it.admin_purge_post.when_),
      agent = it.admin,
      community = it.community,
      reason = it.admin_purge_post.reason,
    )
  }
  this.admin_purged_comments.mapTo(events) {
    ModEvent.AdminPurgeCommentViewEvent(
      id = it.admin_purge_comment.id,
      actionOrder = ActionType.Admin,
      ts = dateStringToTs(it.admin_purge_comment.when_),
      agent = it.admin,
      post = it.post,
      reason = it.admin_purge_comment.reason,
    )
  }
  this.hidden_communities.mapTo(events) {
    ModEvent.ModHideCommunityViewEvent(
      id = it.mod_hide_community.id,
      actionOrder = ActionType.Mod,
      ts = dateStringToTs(it.mod_hide_community.when_),
      agent = it.admin,
      hidden = it.mod_hide_community.hidden,
      community = it.community,
      reason = it.mod_hide_community.reason,
    )
  }

  return events
}

fun ModlogView.toModEvent(): ModEvent = when (this.modlog.kind) {
  ModlogKind.admin_add -> ModEvent.ModAddViewEvent(
    id = modlog.id,
    actionOrder = ActionType.Admin,
    ts = dateStringToTs(modlog.publishedAt),
    agent = moderator?.toPerson(isBanned = false, banExpires = null, isAdmin = false),
    removed = modlog.isRevert,
    person = requireNotNull(
      targetPerson,
    ).toPerson(isBanned = false, banExpires = null, isAdmin = false),
  )
  ModlogKind.admin_ban -> ModEvent.ModBanViewEvent(
    id = modlog.id,
    actionOrder = ActionType.Admin,
    ts = dateStringToTs(modlog.publishedAt),
    agent = moderator?.toPerson(isBanned = false, banExpires = null, isAdmin = false),
    banned = !modlog.isRevert,
    expires = modlog.expiresAt?.let { dateStringToTs(it) },
    person = requireNotNull(
      targetPerson,
    ).toPerson(isBanned = false, banExpires = null, isAdmin = false),
    reason = modlog.reason,
  )
  ModlogKind.admin_allow_instance -> ModEvent.AdminBlockInstanceEvent(
    id = modlog.id,
    actionOrder = ActionType.Admin,
    ts = dateStringToTs(modlog.publishedAt),
    agent = moderator?.toPerson(isBanned = false, banExpires = null, isAdmin = false),
    blocked = modlog.isRevert,
    instance = requireNotNull(targetInstance).toInstance(),
    reason = modlog.reason,
  )
  ModlogKind.admin_block_instance -> ModEvent.AdminBlockInstanceEvent(
    id = modlog.id,
    actionOrder = ActionType.Admin,
    ts = dateStringToTs(modlog.publishedAt),
    agent = moderator?.toPerson(isBanned = false, banExpires = null, isAdmin = false),
    blocked = !modlog.isRevert,
    instance = requireNotNull(targetInstance).toInstance(),
    reason = modlog.reason,
  )
  ModlogKind.admin_purge_comment -> ModEvent.AdminPurgeCommentViewEvent(
    id = modlog.id,
    actionOrder = ActionType.Admin,
    ts = dateStringToTs(modlog.publishedAt),
    agent = moderator?.toPerson(isBanned = false, banExpires = null, isAdmin = false),
    post = requireNotNull(targetPost).toPost(),
    reason = modlog.reason,
  )
  ModlogKind.admin_purge_community -> ModEvent.AdminPurgeCommunityViewEvent(
    id = modlog.id,
    actionOrder = ActionType.Admin,
    ts = dateStringToTs(modlog.publishedAt),
    agent = moderator?.toPerson(isBanned = false, banExpires = null, isAdmin = false),
    reason = modlog.reason,
  )
  ModlogKind.admin_purge_person -> ModEvent.AdminPurgePersonViewEvent(
    id = modlog.id,
    actionOrder = ActionType.Admin,
    ts = dateStringToTs(modlog.publishedAt),
    agent = moderator?.toPerson(isBanned = false, banExpires = null, isAdmin = false),
    reason = modlog.reason,
  )
  ModlogKind.admin_purge_post -> ModEvent.AdminPurgePostViewEvent(
    id = modlog.id,
    actionOrder = ActionType.Admin,
    ts = dateStringToTs(modlog.publishedAt),
    agent = moderator?.toPerson(isBanned = false, banExpires = null, isAdmin = false),
    community = requireNotNull(targetCommunity).toCommunity(),
    reason = modlog.reason,
  )
  ModlogKind.mod_add_to_community -> ModEvent.ModAddCommunityViewEvent(
    id = modlog.id,
    actionOrder = ActionType.Admin,
    ts = dateStringToTs(modlog.publishedAt),
    agent = moderator?.toPerson(isBanned = false, banExpires = null, isAdmin = false),
    removed = modlog.isRevert,
    person = requireNotNull(
      targetPerson,
    ).toPerson(isBanned = false, banExpires = null, isAdmin = false),
    community = requireNotNull(targetCommunity).toCommunity(),
  )
  ModlogKind.mod_ban_from_community -> ModEvent.ModBanFromCommunityViewEvent(
    id = modlog.id,
    actionOrder = ActionType.Admin,
    ts = dateStringToTs(modlog.publishedAt),
    agent = moderator?.toPerson(isBanned = false, banExpires = null, isAdmin = false),
    banned = !modlog.isRevert,
    expires = modlog.expiresAt?.let { dateStringToTs(it) },
    person = requireNotNull(
      targetPerson,
    ).toPerson(isBanned = false, banExpires = null, isAdmin = false),
    community = requireNotNull(targetCommunity).toCommunity(),
    reason = modlog.reason,
  )
  ModlogKind.admin_feature_post_site -> ModEvent.AdminFeaturePostViewEvent(
    id = modlog.id,
    actionOrder = ActionType.Admin,
    ts = dateStringToTs(modlog.publishedAt),
    agent = moderator?.toPerson(isBanned = false, banExpires = null, isAdmin = false),
    featured = !modlog.isRevert,
    post = requireNotNull(targetPost).toPost(),
  )
  ModlogKind.mod_feature_post_community -> ModEvent.ModFeaturePostViewEvent(
    id = modlog.id,
    actionOrder = ActionType.Admin,
    ts = dateStringToTs(modlog.publishedAt),
    agent = moderator?.toPerson(isBanned = false, banExpires = null, isAdmin = false),
    featured = !modlog.isRevert,
    post = requireNotNull(targetPost).toPost(),
    community = requireNotNull(targetCommunity).toCommunity(),
  )
  ModlogKind.mod_change_community_visibility -> ModEvent.ModHideCommunityViewEvent(
    id = modlog.id,
    actionOrder = ActionType.Admin,
    ts = dateStringToTs(modlog.publishedAt),
    agent = moderator?.toPerson(isBanned = false, banExpires = null, isAdmin = false),
    hidden = !modlog.isRevert,
    community = requireNotNull(targetCommunity).toCommunity(),
    reason = modlog.reason,
  )
  ModlogKind.mod_lock_post -> ModEvent.ModLockPostViewEvent(
    id = modlog.id,
    actionOrder = ActionType.Admin,
    ts = dateStringToTs(modlog.publishedAt),
    agent = moderator?.toPerson(isBanned = false, banExpires = null, isAdmin = false),
    locked = !modlog.isRevert,
    post = requireNotNull(targetPost).toPost(),
    community = requireNotNull(targetCommunity).toCommunity(),
  )
  ModlogKind.mod_remove_comment -> ModEvent.ModRemoveCommentViewEvent(
    id = modlog.id,
    actionOrder = ActionType.Admin,
    ts = dateStringToTs(modlog.publishedAt),
    agent = moderator?.toPerson(isBanned = false, banExpires = null, isAdmin = false),
    removed = !modlog.isRevert,
    comment = requireNotNull(targetComment).toComment(),
    post = requireNotNull(targetPost).toPost(),
    community = requireNotNull(targetCommunity).toCommunity(),
    person = requireNotNull(
      targetPerson,
    ).toPerson(isBanned = false, banExpires = null, isAdmin = false),
    reason = modlog.reason,
  )
  ModlogKind.admin_remove_community -> ModEvent.ModRemoveCommunityViewEvent(
    id = modlog.id,
    actionOrder = ActionType.Admin,
    ts = dateStringToTs(modlog.publishedAt),
    agent = moderator?.toPerson(isBanned = false, banExpires = null, isAdmin = false),
    removed = !modlog.isRevert,
    expires = modlog.expiresAt?.let { dateStringToTs(it) },
    community = requireNotNull(targetCommunity).toCommunity(),
    reason = modlog.reason,
  )
  ModlogKind.mod_remove_post -> ModEvent.ModRemoveCommunityViewEvent(
    id = modlog.id,
    actionOrder = ActionType.Admin,
    ts = dateStringToTs(modlog.publishedAt),
    agent = moderator?.toPerson(isBanned = false, banExpires = null, isAdmin = false),
    removed = !modlog.isRevert,
    expires = modlog.expiresAt?.let { dateStringToTs(it) },
    community = requireNotNull(targetCommunity).toCommunity(),
    reason = modlog.reason,
  )
  ModlogKind.mod_transfer_community -> ModEvent.ModTransferCommunityViewEvent(
    id = modlog.id,
    actionOrder = ActionType.Admin,
    ts = dateStringToTs(modlog.publishedAt),
    agent = moderator?.toPerson(isBanned = false, banExpires = null, isAdmin = false),
    person = requireNotNull(
      targetPerson,
    ).toPerson(isBanned = false, banExpires = null, isAdmin = false),
    community = requireNotNull(targetCommunity).toCommunity(),
  )
  ModlogKind.mod_lock_comment -> ModEvent.ModLockCommentViewEvent(
    id = modlog.id,
    actionOrder = ActionType.Admin,
    ts = dateStringToTs(modlog.publishedAt),
    agent = moderator?.toPerson(isBanned = false, banExpires = null, isAdmin = false),
    locked = !modlog.isRevert,
    comment = requireNotNull(targetComment).toComment(),
    post = requireNotNull(targetPost).toPost(),
    community = requireNotNull(targetCommunity).toCommunity(),
    person = requireNotNull(
      targetPerson,
    ).toPerson(isBanned = false, banExpires = null, isAdmin = false),
  )
  ModlogKind.mod_warn_comment -> ModEvent.ModWarnCommentViewEvent(
    id = modlog.id,
    actionOrder = ActionType.Admin,
    ts = dateStringToTs(modlog.publishedAt),
    agent = moderator?.toPerson(isBanned = false, banExpires = null, isAdmin = false),
    warned = !modlog.isRevert,
    comment = requireNotNull(targetComment).toComment(),
    post = requireNotNull(targetPost).toPost(),
    community = requireNotNull(targetCommunity).toCommunity(),
    person = requireNotNull(
      targetPerson,
    ).toPerson(isBanned = false, banExpires = null, isAdmin = false),
    reason = modlog.reason,
  )
  ModlogKind.mod_warn_post -> ModEvent.ModWarnPostViewEvent(
    id = modlog.id,
    actionOrder = ActionType.Admin,
    ts = dateStringToTs(modlog.publishedAt),
    agent = moderator?.toPerson(isBanned = false, banExpires = null, isAdmin = false),
    warned = !modlog.isRevert,
    post = requireNotNull(targetPost).toPost(),
    community = requireNotNull(targetCommunity).toCommunity(),
    person = requireNotNull(
      targetPerson,
    ).toPerson(isBanned = false, banExpires = null, isAdmin = false),
    reason = modlog.reason,
  )
}
