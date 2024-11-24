package com.idunnololz.summit.settings.navigation

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class NavBarConfig(
    val navBarDestinations: List<NavBarDestId> = listOf(
        NavBarDestinations.Home,
        NavBarDestinations.Search,
        NavBarDestinations.Inbox,
        NavBarDestinations.You,
        NavBarDestinations.None,
    ),
)
