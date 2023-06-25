package com.idunnololz.summit.api.dto

data class TransferCommunity(
    val community_id: CommunityId,
    val person_id: PersonId,
    val auth: String,
)
