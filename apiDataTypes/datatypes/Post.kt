package it.vercruysse.lemmyapi.v0.x19.x3.datatypes

import kotlinx.serialization.Serializable

@Serializable
internal data class Post(
    val id: PostId,
    val name: String,
    val url: String? = null,
    val body: String? = null,
    val creator_id: PersonId,
    val community_id: CommunityId,
    val removed: Boolean,
    val locked: Boolean,
    val published: String,
    val updated: String? = null,
    val deleted: Boolean,
    val nsfw: Boolean,
    val embed_title: String? = null,
    val embed_description: String? = null,
    val thumbnail_url: String? = null,
    val ap_id: String,
    val local: Boolean,
    val embed_video_url: String? = null,
    val language_id: LanguageId,
    val featured_community: Boolean,
    val featured_local: Boolean,
    val url_content_type: String? = null,
    val alt_text: String? = null,
)
