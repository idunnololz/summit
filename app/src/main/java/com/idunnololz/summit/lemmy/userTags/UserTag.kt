package com.idunnololz.summit.lemmy.userTags

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class UserTag(
  val personName: String,
  val config: UserTagConfig,
  val id: Long,
) : Parcelable

fun UserTagEntry.toUserTag() = UserTag(this.actorId, this.tag, this.id)
