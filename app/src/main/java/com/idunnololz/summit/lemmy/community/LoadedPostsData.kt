package com.idunnololz.summit.lemmy.community

import android.os.Parcelable
import com.idunnololz.summit.lemmy.LocalPostView
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

@Parcelize
@Serializable
data class LoadedPostsData(
    // Used for filtering
    val allPosts: List<LocalPostView>,
    val posts: List<LocalPostView>,
    val instance: String,
    val pageIndex: Int,
    val dedupingKey: String,
    val hasMore: Boolean,
    val isReadPostUpdate: Boolean = true,
    val error: PostLoadError? = null,
) : Parcelable

@Parcelize
@Serializable
data class PostLoadError(
    val errorCode: Int,
    val errorMessage: String,
    val isRetryable: Boolean,
    var isLoading: Boolean,
) : Parcelable
