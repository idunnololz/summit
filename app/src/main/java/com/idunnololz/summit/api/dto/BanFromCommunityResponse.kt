package com.idunnololz.summit.api.dto

data class BanFromCommunityResponse(
    val person_view: PersonView,
    val banned: Boolean,
)
