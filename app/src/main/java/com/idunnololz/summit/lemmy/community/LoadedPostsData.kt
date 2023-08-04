package com.idunnololz.summit.lemmy.community

import android.os.Parcelable
import com.idunnololz.summit.api.dto.PostView
import com.squareup.moshi.JsonClass
import kotlinx.parcelize.Parcelize

@Parcelize
@JsonClass(generateAdapter = true)
data class LoadedPostsData(
    val posts: List<PostView>,
    val instance: String,
    val pageIndex: Int,
    val hasMore: Boolean,
    val isReadPostUpdate: Boolean = true,
    val error: PostLoadError? = null,
) : Parcelable

@Parcelize
@JsonClass(generateAdapter = true)
data class PostLoadError(
    val errorCode: Int,
    val errorMessage: String,
    val isRetryable: Boolean,
) : Parcelable
