package it.vercruysse.lemmyapi.v0.x19.x3.datatypes

import kotlinx.serialization.Serializable

@Serializable
internal data class LocalUserView(
    val local_user: LocalUser,
    val local_user_vote_display_mode: LocalUserVoteDisplayMode,
    val person: Person,
    val counts: PersonAggregates,
)
