package com.idunnololz.summit.lemmy

import android.os.Parcelable
import com.idunnololz.summit.lemmy.multicommunity.FetchedPost
import com.squareup.moshi.JsonClass
import kotlinx.parcelize.Parcelize

@Parcelize
@JsonClass(generateAdapter = true)
data class LocalPostView(
    val fetchedPost: FetchedPost,
    val filterReason: FilterReason?,
) : Parcelable
