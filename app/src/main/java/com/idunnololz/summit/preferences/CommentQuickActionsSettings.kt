package com.idunnololz.summit.preferences

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
class CommentQuickActionsSettings(
    val actions: List<CommentQuickActionId> =
        listOf(
            CommentQuickActionIds.Voting,
            CommentQuickActionIds.Reply,
            CommentQuickActionIds.Save,
        ),
)
