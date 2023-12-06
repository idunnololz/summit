package com.idunnololz.summit.lemmy.modlogs

import com.idunnololz.summit.api.dto.AdminPurgeCommentView
import com.idunnololz.summit.api.dto.AdminPurgeCommunityView
import com.idunnololz.summit.api.dto.AdminPurgePersonView
import com.idunnololz.summit.api.dto.AdminPurgePostView
import com.idunnololz.summit.api.dto.GetModlogResponse
import com.idunnololz.summit.api.dto.ModAddCommunityView
import com.idunnololz.summit.api.dto.ModAddView
import com.idunnololz.summit.api.dto.ModBanFromCommunityView
import com.idunnololz.summit.api.dto.ModBanView
import com.idunnololz.summit.api.dto.ModFeaturePostView
import com.idunnololz.summit.api.dto.ModHideCommunityView
import com.idunnololz.summit.api.dto.ModLockPostView
import com.idunnololz.summit.api.dto.ModRemoveCommentView
import com.idunnololz.summit.api.dto.ModRemoveCommunityView
import com.idunnololz.summit.api.dto.ModRemovePostView
import com.idunnololz.summit.api.dto.ModTransferCommunityView
import com.idunnololz.summit.api.dto.Person
import com.idunnololz.summit.util.dateStringToTs

sealed interface ModEvent {

    val id: Int
    val actionType: ActionType
    val ts: Long

    /**
     * The person who performed the action is called the "agent".
     */
    val agent: Person?

    /**
     * When a moderator removes a post.
     */
    data class ModRemovePostViewEvent(
        override val id: Int,
        override val actionType: ActionType,
        override val ts: Long,
        override val agent: Person?,
        val event: ModRemovePostView,
    ) : ModEvent

    /**
     * When a moderator locks a post (prevents new comments being made).
     */
    data class ModLockPostViewEvent(
        override val id: Int,
        override val actionType: ActionType,
        override val ts: Long,
        override val agent: Person?,
        val event: ModLockPostView,
    ) : ModEvent

    /**
     * When a moderator features a post on a community (pins it to the top).
     */
    data class ModFeaturePostViewEvent(
        override val id: Int,
        override val actionType: ActionType,
        override val ts: Long,
        override val agent: Person?,
        val event: ModFeaturePostView,
    ) : ModEvent

    /**
     * When a moderator removes a comment.
     */
    data class ModRemoveCommentViewEvent(
        override val id: Int,
        override val actionType: ActionType,
        override val ts: Long,
        override val agent: Person?,
        val event: ModRemoveCommentView,
    ) : ModEvent

    /**
     * When a moderator removes a community.
     */
    data class ModRemoveCommunityViewEvent(
        override val id: Int,
        override val actionType: ActionType,
        override val ts: Long,
        override val agent: Person?,
        val event: ModRemoveCommunityView,
    ) : ModEvent

    /**
     * When someone is banned from a community.
     */
    data class ModBanFromCommunityViewEvent(
        override val id: Int,
        override val actionType: ActionType,
        override val ts: Long,
        override val agent: Person?,
        val event: ModBanFromCommunityView,
    ) : ModEvent

    /**
     * When someone is banned from the site.
     */
    data class ModBanViewEvent(
        override val id: Int,
        override val actionType: ActionType,
        override val ts: Long,
        override val agent: Person?,
        val event: ModBanView,
    ) : ModEvent

    /**
     * When someone is added as a community moderator.
     */
    data class ModAddCommunityViewEvent(
        override val id: Int,
        override val actionType: ActionType,
        override val ts: Long,
        override val agent: Person?,
        val event: ModAddCommunityView,
    ) : ModEvent

    /**
     * When a moderator transfers a community to a new owner.
     */
    data class ModTransferCommunityViewEvent(
        override val id: Int,
        override val actionType: ActionType,
        override val ts: Long,
        override val agent: Person?,
        val event: ModTransferCommunityView,
    ) : ModEvent

    /**
     * When someone is added as a site moderator.
     */
    data class ModAddViewEvent(
        override val id: Int,
        override val actionType: ActionType,
        override val ts: Long,
        override val agent: Person?,
        val event: ModAddView,
    ) : ModEvent

    data class AdminPurgePersonViewEvent(
        override val id: Int,
        override val actionType: ActionType,
        override val ts: Long,
        override val agent: Person?,
        val event: AdminPurgePersonView,
    ) : ModEvent

    data class AdminPurgeCommunityViewEvent(
        override val id: Int,
        override val actionType: ActionType,
        override val ts: Long,
        override val agent: Person?,
        val event: AdminPurgeCommunityView,
    ) : ModEvent

    data class AdminPurgePostViewEvent(
        override val id: Int,
        override val actionType: ActionType,
        override val ts: Long,
        override val agent: Person?,
        val event: AdminPurgePostView,
    ) : ModEvent

    data class AdminPurgeCommentViewEvent(
        override val id: Int,
        override val actionType: ActionType,
        override val ts: Long,
        override val agent: Person?,
        val event: AdminPurgeCommentView,
    ) : ModEvent

    /**
     * When a community is hidden from public view.
     */
    data class ModHideCommunityViewEvent(
        override val id: Int,
        override val actionType: ActionType,
        override val ts: Long,
        override val agent: Person?,
        val event: ModHideCommunityView,
    ) : ModEvent
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
