package it.vercruysse.lemmyapi.v0.x19.x3.datatypes

import it.vercruysse.lemmyapi.dto.SearchType
import kotlinx.serialization.Serializable

@Serializable
internal data class SearchResponse(
    val type_: SearchType /* "All" | "Comments" | "Posts" | "Communities" | "Users" | "Url" */,
    val comments: List<CommentView>,
    val posts: List<PostView>,
    val communities: List<CommunityView>,
    val users: List<PersonView>,
)
