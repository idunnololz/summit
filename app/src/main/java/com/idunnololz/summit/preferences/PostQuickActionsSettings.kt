package com.idunnololz.summit.preferences

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
class PostQuickActionsSettings(
    val actions: List<PostQuickActionId> =
        listOf(
            PostQuickActionIds.Voting,
            PostQuickActionIds.Reply,
            PostQuickActionIds.Save,
        ),
)
