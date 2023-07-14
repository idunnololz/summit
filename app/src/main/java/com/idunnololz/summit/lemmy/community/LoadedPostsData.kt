package com.idunnololz.summit.lemmy.community

import android.os.Parcelable
import com.idunnololz.summit.api.dto.PostView
import kotlinx.parcelize.Parcelize

@Parcelize
data class LoadedPostsData(
    val posts: List<PostView>,
    val instance: String,
    val pageIndex: Int,
    val hasMore: Boolean,
    val isReadPostUpdate: Boolean = true,
    val error: Throwable? = null,
): Parcelable