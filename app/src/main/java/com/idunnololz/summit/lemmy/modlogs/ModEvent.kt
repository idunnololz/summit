package com.idunnololz.summit.lemmy.modlogs

import android.content.Context
import androidx.annotation.DrawableRes
import com.idunnololz.summit.R
import com.idunnololz.summit.api.dto.lemmy.AdminPurgeCommentView
import com.idunnololz.summit.api.dto.lemmy.AdminPurgeCommunityView
import com.idunnololz.summit.api.dto.lemmy.AdminPurgePersonView
import com.idunnololz.summit.api.dto.lemmy.AdminPurgePostView
import com.idunnololz.summit.api.dto.lemmy.GetModlogResponse
import com.idunnololz.summit.api.dto.lemmy.ModAddCommunityView
import com.idunnololz.summit.api.dto.lemmy.ModAddView
import com.idunnololz.summit.api.dto.lemmy.ModBanFromCommunityView
import com.idunnololz.summit.api.dto.lemmy.ModBanView
import com.idunnololz.summit.api.dto.lemmy.ModFeaturePostView
import com.idunnololz.summit.api.dto.lemmy.ModHideCommunityView
import com.idunnololz.summit.api.dto.lemmy.ModLockPostView
import com.idunnololz.summit.api.dto.lemmy.ModRemoveCommentView
import com.idunnololz.summit.api.dto.lemmy.ModRemoveCommunityView
import com.idunnololz.summit.api.dto.lemmy.ModRemovePostView
import com.idunnololz.summit.api.dto.lemmy.ModTransferCommunityView
import com.idunnololz.summit.api.dto.lemmy.ModlogActionType
import com.idunnololz.summit.api.dto.lemmy.Person
import com.idunnololz.summit.util.dateStringToTs
import com.idunnololz.summit.util.ext.getColorCompat
import com.idunnololz.summit.util.ext.getColorFromAttribute
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
    val event: ModRemovePostView,
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
    val event: ModLockPostView,
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
    val event: ModFeaturePostView,
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
    val event: ModRemoveCommentView,
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
    val event: ModRemoveCommunityView,
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
    val event: ModBanFromCommunityView,
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
    val event: ModBanView,
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
    val event: ModAddCommunityView,
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
    val event: ModTransferCommunityView,
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
    val event: ModAddView,
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
    val event: AdminPurgePersonView,
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
    val event: AdminPurgeCommunityView,
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
    val event: AdminPurgePostView,
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
    val event: AdminPurgeCommentView,
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
    val event: ModHideCommunityView,
  ) : ModEvent {
    override val actionType = ModlogActionType.ModHideCommunity
  }
}

fun GetModlogResponse.toModEvents(): MutableList<ModEvent> {
  val events = mutableListOf<ModEvent>()

  this.removed_posts.mapTo(events) {
    ModEvent.ModRemovePostViewEvent(
      it.mod_remove_post.id,
      ActionType.Mod,
      dateStringToTs(it.mod_remove_post.when_),
      it.moderator,
      it,
    )
  }
  this.locked_posts.mapTo(events) {
    ModEvent.ModLockPostViewEvent(
      it.mod_lock_post.id,
      ActionType.Mod,
      dateStringToTs(it.mod_lock_post.when_),
      it.moderator,
      it,
    )
  }
  this.featured_posts.mapTo(events) {
    ModEvent.ModFeaturePostViewEvent(
      it.mod_feature_post.id,
      ActionType.Mod,
      dateStringToTs(it.mod_feature_post.when_),
      it.moderator,
      it,
    )
  }
  this.removed_comments.mapTo(events) {
    ModEvent.ModRemoveCommentViewEvent(
      it.mod_remove_comment.id,
      ActionType.Mod,
      dateStringToTs(it.mod_remove_comment.when_),
      it.moderator,
      it,
    )
  }
  this.removed_communities.mapTo(events) {
    ModEvent.ModRemoveCommunityViewEvent(
      it.mod_remove_community.id,
      ActionType.Mod,
      dateStringToTs(it.mod_remove_community.when_),
      it.moderator,
      it,
    )
  }
  this.banned_from_community.mapTo(events) {
    ModEvent.ModBanFromCommunityViewEvent(
      it.mod_ban_from_community.id,
      ActionType.Mod,
      dateStringToTs(it.mod_ban_from_community.when_),
      it.moderator,
      it,
    )
  }
  this.banned.mapTo(events) {
    ModEvent.ModBanViewEvent(
      it.mod_ban.id,
      ActionType.Mod,
      dateStringToTs(it.mod_ban.when_),
      it.moderator,
      it,
    )
  }
  this.added_to_community.mapTo(events) {
    ModEvent.ModAddCommunityViewEvent(
      it.mod_add_community.id,
      ActionType.Mod,
      dateStringToTs(it.mod_add_community.when_),
      it.moderator,
      it,
    )
  }
  this.transferred_to_community.mapTo(events) {
    ModEvent.ModTransferCommunityViewEvent(
      it.mod_transfer_community.id,
      ActionType.Mod,
      dateStringToTs(it.mod_transfer_community.when_),
      it.moderator,
      it,
    )
  }
  this.added.mapTo(events) {
    ModEvent.ModAddViewEvent(
      it.mod_add.id,
      ActionType.Mod,
      dateStringToTs(it.mod_add.when_),
      it.moderator,
      it,
    )
  }
  this.admin_purged_persons.mapTo(events) {
    ModEvent.AdminPurgePersonViewEvent(
      it.admin_purge_person.id,
      ActionType.Admin,
      dateStringToTs(it.admin_purge_person.when_),
      it.admin,
      it,
    )
  }
  this.admin_purged_communities.mapTo(events) {
    ModEvent.AdminPurgeCommunityViewEvent(
      it.admin_purge_community.id,
      ActionType.Admin,
      dateStringToTs(it.admin_purge_community.when_),
      it.admin,
      it,
    )
  }
  this.admin_purged_posts.mapTo(events) {
    ModEvent.AdminPurgePostViewEvent(
      it.admin_purge_post.id,
      ActionType.Admin,
      dateStringToTs(it.admin_purge_post.when_),
      it.admin,
      it,
    )
  }
  this.admin_purged_comments.mapTo(events) {
    ModEvent.AdminPurgeCommentViewEvent(
      it.admin_purge_comment.id,
      ActionType.Admin,
      dateStringToTs(it.admin_purge_comment.when_),
      it.admin,
      it,
    )
  }
  this.hidden_communities.mapTo(events) {
    ModEvent.ModHideCommunityViewEvent(
      it.mod_hide_community.id,
      ActionType.Mod,
      dateStringToTs(it.mod_hide_community.when_),
      it.admin,
      it,
    )
  }

  return events
}

enum class ActionType {
  Mod,
  Admin,
}

fun ModEvent.getColor(context: Context): Int {
  val modEvent = this

  return when (modEvent) {
    is ModEvent.AdminPurgeCommentViewEvent,
    is ModEvent.AdminPurgeCommunityViewEvent,
    is ModEvent.AdminPurgePersonViewEvent,
    is ModEvent.AdminPurgePostViewEvent,
    ->
      context.getColorFromAttribute(com.google.android.material.R.attr.colorError)
    is ModEvent.ModAddCommunityViewEvent -> {
      if (modEvent.event.mod_add_community.removed) {
        context.getColorFromAttribute(com.google.android.material.R.attr.colorError)
      } else {
        context.getColorCompat(R.color.style_green)
      }
    }
    is ModEvent.ModAddViewEvent -> {
      if (modEvent.event.mod_add.removed) {
        context.getColorFromAttribute(com.google.android.material.R.attr.colorError)
      } else {
        context.getColorCompat(R.color.style_green)
      }
    }
    is ModEvent.ModBanFromCommunityViewEvent -> {
      if (modEvent.event.mod_ban_from_community.banned) {
        context.getColorFromAttribute(com.google.android.material.R.attr.colorError)
      } else {
        context.getColorFromAttribute(com.google.android.material.R.attr.colorPrimary)
      }
    }
    is ModEvent.ModBanViewEvent -> {
      if (modEvent.event.mod_ban.banned) {
        context.getColorFromAttribute(com.google.android.material.R.attr.colorError)
      } else {
        context.getColorFromAttribute(com.google.android.material.R.attr.colorPrimary)
      }
    }
    is ModEvent.ModFeaturePostViewEvent -> {
      if (modEvent.event.mod_feature_post.featured) {
        context.getColorCompat(R.color.style_green)
      } else {
        context.getColorCompat(R.color.style_green)
      }
    }
    is ModEvent.ModHideCommunityViewEvent -> {
      if (modEvent.event.mod_hide_community.hidden) {
        context.getColorFromAttribute(com.google.android.material.R.attr.colorPrimary)
      } else {
        context.getColorFromAttribute(com.google.android.material.R.attr.colorPrimary)
      }
    }
    is ModEvent.ModLockPostViewEvent -> {
      if (modEvent.event.mod_lock_post.locked) {
        context.getColorCompat(R.color.style_amber)
      } else {
        context.getColorCompat(R.color.style_amber)
      }
    }
    is ModEvent.ModRemoveCommentViewEvent -> {
      if (modEvent.event.mod_remove_comment.removed) {
        context.getColorFromAttribute(com.google.android.material.R.attr.colorError)
      } else {
        context.getColorFromAttribute(com.google.android.material.R.attr.colorPrimary)
      }
    }
    is ModEvent.ModRemoveCommunityViewEvent -> {
      if (modEvent.event.mod_remove_community.removed) {
        context.getColorFromAttribute(com.google.android.material.R.attr.colorError)
      } else {
        context.getColorFromAttribute(com.google.android.material.R.attr.colorPrimary)
      }
    }
    is ModEvent.ModRemovePostViewEvent -> {
      if (modEvent.event.mod_remove_post.removed) {
        context.getColorFromAttribute(com.google.android.material.R.attr.colorError)
      } else {
        context.getColorFromAttribute(com.google.android.material.R.attr.colorPrimary)
      }
    }
    is ModEvent.ModTransferCommunityViewEvent -> {
      context.getColorFromAttribute(com.google.android.material.R.attr.colorPrimary)
    }
  }
}

@DrawableRes
fun ModEvent.getIconRes(): Int {
  return when (this) {
    is ModEvent.AdminPurgeCommentViewEvent ->
      R.drawable.baseline_delete_24
    is ModEvent.AdminPurgeCommunityViewEvent ->
      R.drawable.baseline_delete_24
    is ModEvent.AdminPurgePersonViewEvent ->
      R.drawable.baseline_delete_24
    is ModEvent.AdminPurgePostViewEvent ->
      R.drawable.baseline_delete_24
    is ModEvent.ModAddCommunityViewEvent -> {
      if (this.event.mod_add_community.removed) {
        R.drawable.outline_remove_moderator_24
      } else {
        R.drawable.outline_add_moderator_24
      }
    }
    is ModEvent.ModAddViewEvent -> {
      if (this.event.mod_add.removed) {
        R.drawable.outline_remove_moderator_24
      } else {
        R.drawable.outline_add_moderator_24
      }
    }
    is ModEvent.ModBanFromCommunityViewEvent -> {
      if (this.event.mod_ban_from_community.banned) {
        R.drawable.outline_person_remove_24
      } else {
        R.drawable.baseline_person_add_alt_24
      }
    }
    is ModEvent.ModBanViewEvent -> {
      if (this.event.mod_ban.banned) {
        R.drawable.outline_person_remove_24
      } else {
        R.drawable.baseline_person_add_alt_24
      }
    }
    is ModEvent.ModFeaturePostViewEvent -> {
      if (this.event.mod_feature_post.featured) {
        R.drawable.baseline_push_pin_24
      } else {
        R.drawable.ic_unpin_24
      }
    }
    is ModEvent.ModHideCommunityViewEvent -> {
      if (this.event.mod_hide_community.hidden) {
        R.drawable.baseline_hide_24
      } else {
        R.drawable.baseline_expand_content_24
      }
    }
    is ModEvent.ModLockPostViewEvent -> {
      if (this.event.mod_lock_post.locked) {
        R.drawable.outline_lock_24
      } else {
        R.drawable.baseline_lock_open_24
      }
    }
    is ModEvent.ModRemoveCommentViewEvent -> {
      if (this.event.mod_remove_comment.removed) {
        R.drawable.baseline_remove_circle_outline_24
      } else {
        R.drawable.baseline_undo_24
      }
    }
    is ModEvent.ModRemoveCommunityViewEvent -> {
      if (this.event.mod_remove_community.removed) {
        R.drawable.baseline_remove_circle_outline_24
      } else {
        R.drawable.baseline_undo_24
      }
    }
    is ModEvent.ModRemovePostViewEvent -> {
      if (this.event.mod_remove_post.removed) {
        R.drawable.baseline_remove_circle_outline_24
      } else {
        R.drawable.baseline_undo_24
      }
    }
    is ModEvent.ModTransferCommunityViewEvent ->
      R.drawable.baseline_swap_horiz_24
  }
}
//  when (this) {
//    is ModEvent.AdminPurgeCommentViewEvent,
//    is ModEvent.AdminPurgeCommunityViewEvent,
//    is ModEvent.AdminPurgePersonViewEvent,
//    is ModEvent.AdminPurgePostViewEvent ->
//      context.getColorFromAttribute(com.google.android.material.R.attr.colorError)
//    is ModEvent.ModAddCommunityViewEvent -> {
//      if (this.event.mod_add_community.removed) {
//        context.getColorFromAttribute(com.google.android.material.R.attr.colorError)
//      } else {
//        context.getColorCompat(R.color.style_green)
//      }
//    }
//    is ModEvent.ModAddViewEvent -> {
//      if (this.event.mod_add.removed) {
//        context.getColorFromAttribute(com.google.android.material.R.attr.colorError)
//      } else {
//        context.getColorCompat(R.color.style_green)
//      }
//    }
//    is ModEvent.ModBanFromCommunityViewEvent,
//    is ModEvent.ModBanViewEvent ->
//      context.getColorFromAttribute(com.google.android.material.R.attr.colorError)
//    is ModEvent.ModFeaturePostViewEvent ->
//      context.getColorCompat(R.color.style_green)
//    is ModEvent.ModHideCommunityViewEvent ->
//      context.getColorFromAttribute(com.google.android.material.R.attr.colorPrimary)
//    is ModEvent.ModLockPostViewEvent ->
//      context.getColorCompat(R.color.style_amber)
//    is ModEvent.ModRemoveCommentViewEvent,
//    is ModEvent.ModRemoveCommunityViewEvent,
//    is ModEvent.ModRemovePostViewEvent ->
//      context.getColorFromAttribute(com.google.android.material.R.attr.colorError)
//    is ModEvent.ModTransferCommunityViewEvent ->
//      context.getColorFromAttribute(com.google.android.material.R.attr.colorPrimary)
//  }
