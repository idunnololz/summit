package com.idunnololz.summit.offline

/**
 * Contains information reguarding how offlining should work
 */
data class OfflineTaskConfig(
    val minPosts: Int,
    val roundPostsToNearestPage: Boolean
)