package com.idunnololz.summit.api.dto

import android.os.Parcelable
import com.squareup.moshi.JsonClass
import kotlinx.parcelize.Parcelize

@Parcelize
@JsonClass(generateAdapter = true)
data class PostAggregates(
    val id: Int,
    val post_id: PostId,
    val comments: Int,
    val score: Int,
    val upvotes: Int,
    val downvotes: Int,
    val published: String,
    val newest_comment_time_necro: String? = null,
    val newest_comment_time: String? = null,
    val featured_community: Boolean,
    val featured_local: Boolean,
    var hot_rank: Double? = null,
    var hot_rank_active: Double? = null,
    var scaled_rank: Double? = null,
    var controversy_rank: Double? = null,
) : Parcelable
