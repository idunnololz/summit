package com.idunnololz.summit.api.dto

data class DeleteCommunity(
    val community_id: CommunityId,
    val deleted: Boolean,
    val auth: String,
)
