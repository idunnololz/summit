package com.idunnololz.summit.api.dto

import com.idunnololz.summit.api.dto.AdminPurgeComment

data class AdminPurgeCommentView(
    val admin_purge_comment: AdminPurgeComment,
    val admin: Person? = null,
    val post: Post,
)
