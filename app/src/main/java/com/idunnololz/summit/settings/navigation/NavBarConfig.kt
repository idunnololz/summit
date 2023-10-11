package com.idunnololz.summit.settings.navigation

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class NavBarConfig(
    val navBarDestinations: List<NavBarDestId> = listOf(
        NavBarDestinations.Home,
        NavBarDestinations.Saved,
        NavBarDestinations.Search,
        NavBarDestinations.History,
        NavBarDestinations.Inbox,
    )
)