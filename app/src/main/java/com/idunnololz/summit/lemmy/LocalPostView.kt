package com.idunnololz.summit.lemmy

import android.os.Parcelable
import com.idunnololz.summit.api.dto.PostView
import com.squareup.moshi.JsonClass
import kotlinx.parcelize.Parcelize

@Parcelize
@JsonClass(generateAdapter = true)
data class LocalPostView(
    val postView: PostView,
    val filterReason: FilterReason?,
) : Parcelable
