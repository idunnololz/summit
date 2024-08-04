package com.idunnololz.summit.lemmy.inbox

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
enum class PageType : Parcelable {
    Unread,
    All,
    Replies,
    Mentions,
    Messages,
    Reports,
    Conversation,
}
