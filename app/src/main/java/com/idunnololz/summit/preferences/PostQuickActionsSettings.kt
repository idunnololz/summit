package com.idunnololz.summit.preferences

import kotlinx.serialization.Serializable

@Serializable
class PostQuickActionsSettings(
    val actions: List<PostQuickActionId> =
        listOf(
            PostQuickActionIds.Voting,
            PostQuickActionIds.Reply,
            PostQuickActionIds.Save,
        ),
)
