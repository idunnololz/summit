package com.idunnololz.summit.api.summit

import com.google.gson.annotations.SerializedName

class TrendingCommunityData(
    val baseurl: String,
    val nsfw: Boolean,
    val isSuspicious: Boolean,
    val name: String,
    val published: Long,
    val title: String,
    val url: String,
    val desc: String,
    val trendStats: TrendingStats,
    val counts: CommunityCounts,
    val lastUpdateTime: String? = null,
    val icon: String? = null,
    val banner: String? = null,
)

class TrendingStats(
    val trendScore7Day: Double,
    val trendScore30Day: Double,
    val hotScore: Double,
)

data class CommunityCounts(
    @SerializedName("community_id") val communityId: Int,
    val comments: Int,
    @SerializedName("users_active_day") val usersActiveDay: Int,
    val subscribers: Int,
    @SerializedName("users_active_month") val usersActiveMonth: Int,
    val published: String,
    @SerializedName("users_active_week") val usersActiveWeek: Int,
    @SerializedName("users_active_half_year") val usersActiveHalfYear: Int,
    val posts: Int,
    val id: Int? = null,
    @SerializedName("hot_rank") val hotRank: Int? = null,
    @SerializedName("subscribers_local") val subscribersLocal: Int? = null,
)
