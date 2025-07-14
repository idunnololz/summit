package com.idunnololz.summit.api.dto.lemmy

data class GetModlog(
  val mod_person_id: PersonId? = null,
  val community_id: CommunityId? = null,
  val page: Int? = null,
  val limit: Int? = null,
  /* "All" | "ModRemovePost" | "ModLockPost" | "ModFeaturePost" | "ModRemoveComment" | "ModRemoveCommunity" | "ModBanFromCommunity" | "ModAddCommunity" | "ModTransferCommunity" | "ModAdd" | "ModBan" | "ModHideCommunity" | "AdminPurgePerson" | "AdminPurgeCommunity" | "AdminPurgePost" | "AdminPurgeComment" */
  val type_: ModlogActionType? = null,
  val other_person_id: PersonId? = null,
  val auth: String? = null,
  val post_id: PostId? = null,
  val comment_id: CommentId? = null,
)
