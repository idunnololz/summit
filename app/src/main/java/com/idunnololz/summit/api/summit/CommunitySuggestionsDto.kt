package com.idunnololz.summit.api.summit

class CommunitySuggestionsDto(
    val popularLast7Days: List<TrendingCommunityData>,
    val trendingLast7Days: List<TrendingCommunityData>,
    val hot: List<TrendingCommunityData>,
)
