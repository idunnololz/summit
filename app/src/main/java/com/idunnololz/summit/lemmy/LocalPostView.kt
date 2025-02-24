package com.idunnololz.summit.lemmy

import android.os.Parcelable
import com.idunnololz.summit.lemmy.multicommunity.FetchedPost
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

@Parcelize
@Serializable
data class LocalPostView(
    val fetchedPost: FetchedPost,
    val filterReason: FilterReason?,
    val isDuplicatePost: Boolean,
) : Parcelable
