package com.idunnololz.summit.settings.navigation

import kotlinx.serialization.Serializable

@Serializable
data class NavBarConfig(
    val navBarDestinations: List<NavBarDestId> = listOf(
        NavBarDestinations.Home,
        NavBarDestinations.Search,
        NavBarDestinations.Inbox,
        NavBarDestinations.You,
        NavBarDestinations.None,
    ),
)
