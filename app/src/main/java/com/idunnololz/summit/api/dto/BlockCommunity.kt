package com.idunnololz.summit.api.dto

data class BlockCommunity(
    val community_id: CommunityId,
    val block: Boolean,
    val auth: String,
)
