package com.idunnololz.summit.api.dto

data class GetCommunity(
    val id: CommunityId? = null,
    val name: String? = null,
    val auth: String? = null,
)
