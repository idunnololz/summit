package com.idunnololz.summit.lemmy

import com.idunnololz.summit.api.dto.CommentId
import com.idunnololz.summit.api.dto.CommentView
import com.idunnololz.summit.filterLists.ContentFiltersManager
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

class CommentListEngine @AssistedInject constructor(
    private val contentFiltersManager: ContentFiltersManager,
) {

    @AssistedFactory
    interface Factory {
        fun create(): CommentListEngine
    }

    var commentPages: List<CommentPage> = listOf()

    val nextPage: Int
        get() = (commentPages.lastOrNull()?.pageIndex ?: 0) + 1

    val hasMore: Boolean
        get() = commentPages.lastOrNull()?.hasMore != false

    fun addComments(
        comments: List<CommentView>,
        instance: String,
        pageIndex: Int,
        hasMore: Boolean,
        error: Throwable?,
    ) {
        val newPages = commentPages.toMutableList()
        val existingPage = newPages.getOrNull(pageIndex)

        val commentItems = comments.map {
//            if (contentFiltersManager.testCommentView(it)) {
//                FilteredCommentItem(it)
//            } else {
            VisibleCommentItem(it)
//            }
        }

        val commentPage = CommentPage(
            comments = commentItems,
            instance = instance,
            pageIndex = pageIndex,
            hasMore = hasMore,
            error = error,
        )

        if (existingPage != null) {
            newPages[pageIndex] = commentPage
        } else {
            newPages.add(commentPage)
        }

        commentPages = newPages
    }

    fun clear() {
        commentPages = listOf()
    }

    fun removeComment(id: CommentId) {
        val pages = commentPages.toMutableList()
        for ((index, page) in pages.withIndex()) {
            if (page.comments.any { it.commentView.comment.id == id }) {
                pages[index] = page.copy(comments = page.comments.filter { it.commentView.comment.id != id })
            }
        }
        commentPages = pages
    }
}
