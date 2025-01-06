package com.idunnololz.summit.lemmy.userTags

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class UserTag(
    val personName: String,
    val config: UserTagConfig,
) : Parcelable
