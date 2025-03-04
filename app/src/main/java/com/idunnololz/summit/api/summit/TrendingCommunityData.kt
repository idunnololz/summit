package com.idunnololz.summit.api.summit

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
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

@Serializable
class TrendingStats(
    val trendScore7Day: Double,
    val trendScore30Day: Double,
    val hotScore: Double,
)

@Serializable
data class CommunityCounts(
    @SerialName("community_id") val communityId: Int,
    val comments: Int,
    @SerialName("users_active_day") val usersActiveDay: Int,
    val subscribers: Int,
    @SerialName("users_active_month") val usersActiveMonth: Int,
    val published: String,
    @SerialName("users_active_week") val usersActiveWeek: Int,
    @SerialName("users_active_half_year") val usersActiveHalfYear: Int,
    val posts: Int,
    val id: Int? = null,
    @SerialName("hot_rank") val hotRank: Int? = null,
    @SerialName("subscribers_local") val subscribersLocal: Int? = null,
)
