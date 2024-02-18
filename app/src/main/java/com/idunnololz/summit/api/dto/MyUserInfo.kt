package com.idunnololz.summit.api.dto

data class MyUserInfo(
    val local_user_view: LocalUserView,
    val follows: List<CommunityFollowerView>,
    val moderates: List<CommunityModeratorView>,
    val community_blocks: List<CommunityBlockView>,
    val person_blocks: List<PersonBlockView>,
    val instance_blocks: List<InstanceBlockView>,
    val discussion_languages: List<LanguageId>,
)
