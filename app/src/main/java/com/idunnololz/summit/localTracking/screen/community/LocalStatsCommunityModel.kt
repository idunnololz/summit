package com.idunnololz.summit.localTracking.screen.community

import com.idunnololz.summit.lemmy.multicommunity.FetchedPost

data class LocalStatsCommunityModel(
  val items: List<Item>
) {

  sealed interface Item

  data class PostModelItem(
    val postId: Long?,
    val fetchedPost: Result<FetchedPost>?,
  ): Item

  data class CommunityStatsSummaryItem(
    val totalScore: Int,
    val upvotes: Int,
    val downvotes: Int,
    val recentTotalScore: Int,
    val recentUpvotes: Int,
    val recentDownvotes: Int,
    val recentInteractionsCount: Int,
    val totalInteractionsCount: Int,
    val recentViewsCount: Int,
    val totalViewsCount: Int,
  ): Item

  data object NoPostsModelItem : Item
}