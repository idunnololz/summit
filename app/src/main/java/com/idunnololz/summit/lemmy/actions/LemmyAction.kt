package com.idunnololz.summit.lemmy.actions

sealed interface LemmyAction {
    val id: Long
    val ts: Long
    val creationTs: Long
    val info: ActionInfo?
}
