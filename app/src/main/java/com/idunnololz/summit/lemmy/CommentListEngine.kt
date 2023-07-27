package com.idunnololz.summit.lemmy

import com.idunnololz.summit.api.dto.CommentId

class CommentListEngine {
    var commentPages: List<CommentPageResult> = listOf()

    val nextPage: Int
        get() = (commentPages.lastOrNull()?.pageIndex ?: 0) + 1

    val hasMore: Boolean
        get() = commentPages.lastOrNull()?.hasMore != false

    fun addComments(commentPageResult: CommentPageResult) {
        val newPages = commentPages.toMutableList()
        val existingPage = newPages.getOrNull(commentPageResult.pageIndex)
        if (existingPage != null) {
            newPages[commentPageResult.pageIndex] = existingPage
        } else {
            newPages.add(commentPageResult)
        }

        commentPages = newPages
    }

    fun clear() {
        commentPages = listOf()
    }

    fun removeComment(id: CommentId) {
        val pages = commentPages.toMutableList()
        for ((index, page) in pages.withIndex()) {
            if (page.comments.any { it.comment.id == id }) {
                pages[index] = page.copy(comments = page.comments.filter { it.comment.id != id })
            }
        }
        commentPages = pages
    }
}
