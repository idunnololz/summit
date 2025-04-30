package com.idunnololz.summit.preferences

import kotlinx.serialization.Serializable

@Serializable
data class PostsInFeedQuickActionsSettings(
    val enabled: Boolean = false,
    val actions: List<PostQuickActionId> = listOf(),
)