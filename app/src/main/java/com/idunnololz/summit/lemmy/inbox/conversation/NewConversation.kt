package com.idunnololz.summit.lemmy.inbox.conversation

import android.os.Parcelable
import com.idunnololz.summit.api.dto.PersonId
import kotlinx.parcelize.Parcelize

@Parcelize
data class NewConversation(
    val personId: PersonId,
    val personInstance: String,
    val personName: String,
    val personAvatar: String?,
) : Parcelable
