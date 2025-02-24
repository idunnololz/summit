package com.idunnololz.summit.preferences

import kotlinx.serialization.Serializable

@Serializable
class CommentQuickActionsSettings(
    val actions: List<CommentQuickActionId> =
        listOf(
            CommentQuickActionIds.Voting,
            CommentQuickActionIds.Reply,
            CommentQuickActionIds.Save,
        ),
)
