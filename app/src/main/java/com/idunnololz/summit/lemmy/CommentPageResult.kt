package com.idunnololz.summit.lemmy

import com.idunnololz.summit.api.dto.CommentView

data class CommentPageResult(
    val comments: List<CommentView>,
    val instance: String,
    val pageIndex: Int,
    val hasMore: Boolean,
    val error: Throwable?,
)