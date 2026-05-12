package com.idunnololz.summit.preferences

import kotlinx.serialization.Serializable

@Serializable
data class SearchHomeConfig(
  val showSearchSuggestions: Boolean = true,
  val showSubscribedCommunities: Boolean = true,
  val showTopCommunity7DaysSuggestions: Boolean = true,
  val showTrendingCommunitySuggestions: Boolean = true,
  val showRisingCommunitySuggestions: Boolean = true,
  val showRandomCommunitySuggestions: Boolean = true,
)

val SearchHomeConfig.anySectionsEnabled
  get() = showSearchSuggestions ||
    showSubscribedCommunities ||
    showTopCommunity7DaysSuggestions ||
    showTrendingCommunitySuggestions ||
    showRisingCommunitySuggestions ||
    showRandomCommunitySuggestions

/**
 * True if any section that requires data from the Summit server is enabled.
 */
val SearchHomeConfig.anyServerBasedSectionsEnabled
  get() = showTopCommunity7DaysSuggestions ||
    showTrendingCommunitySuggestions ||
    showRisingCommunitySuggestions ||
    showRandomCommunitySuggestions