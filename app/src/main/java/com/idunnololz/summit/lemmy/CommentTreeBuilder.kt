package com.idunnololz.summit.lemmy

import android.util.Log
import com.idunnololz.summit.account.AccountManager
import com.idunnololz.summit.actions.PendingCommentView
import com.idunnololz.summit.api.dto.Comment
import com.idunnololz.summit.api.dto.CommentView
import com.idunnololz.summit.api.dto.PostView
import com.idunnololz.summit.api.utils.getDepth
import com.idunnololz.summit.lemmy.post.PostViewModel
import java.util.LinkedHashMap
import java.util.LinkedList

private const val TAG = "CommentNodeData"

data class CommentNodeData(
    var commentView: PostViewModel.ListView,
    var depth: Int,
    val children: MutableList<CommentNodeData> = mutableListOf(),
)

class CommentTreeBuilder(
    private val accountManager: AccountManager,
) {

    suspend fun buildCommentsTreeListView(
        post: PostView?,
        comments: List<CommentView>?,
        parentComment: Boolean,
        pendingComments: List<PendingCommentView>?,
        supplementaryComments: Map<Int, CommentView>,
        removedCommentIds: Set<Int>,
    ): List<CommentNodeData> {
        val map = LinkedHashMap<Number, CommentNodeData>()
        val firstComment = comments?.firstOrNull()?.comment
        val idToPendingComments = pendingComments?.associateBy { it.commentId } ?: mapOf()

        val depthOffset = if (!parentComment) { 0 } else {
            comments?.minOfOrNull { it.getDepth() } ?: 0
        }

        Log.d(
            TAG,
            "Score: ${comments?.firstOrNull()?.counts?.score} " +
                "Depth: ${firstComment?.getDepth()} First comment: ${firstComment?.content}. ",
        )

        val topNodes = mutableListOf<CommentNodeData>()
        comments?.forEach { comment ->
            val depth = comment.comment.getDepth().minus(depthOffset)
            val node = CommentNodeData(
                commentView = PostViewModel.ListView.CommentListView(
                    comment,
                    pendingCommentView = idToPendingComments[comment.comment.id],
                    isRemoved = removedCommentIds.contains(comment.comment.id),
                ),
                depth = depth,
            )
            map[comment.comment.id] = node
        }
        supplementaryComments.values.forEach { comment ->
            val depth = comment.comment.getDepth().minus(depthOffset)
            val node = CommentNodeData(
                commentView = PostViewModel.ListView.CommentListView(
                    comment,
                    pendingCommentView = idToPendingComments[comment.comment.id],
                    isRemoved = removedCommentIds.contains(comment.comment.id),
                ),
                depth = depth,
            )
            map[comment.comment.id] = node
        }

        map.values.forEach { node ->
            val commentView = node.commentView
            if (commentView !is PostViewModel.ListView.CommentListView) {
                return@forEach
            }

            val parentId = getCommentParentId(commentView.comment.comment)
            parentId?.let { cParentId ->
                val parent = map[cParentId]

                // Necessary because blocked comment might not exist
                parent?.children?.add(node)
            } ?: run {
                topNodes.add(node)
            }
        }

        if (post != null) {
            addMoreItems(post, topNodes)
        }

        pendingComments?.forEach { pendingComment ->
            if (pendingComment.commentId != null) {
                // this is an updated comment

                val originalComment = map[pendingComment.commentId]
                val commentView = originalComment?.commentView
                if (originalComment != null && commentView is PostViewModel.ListView.CommentListView) {
                    originalComment.commentView = commentView.copy(
                        pendingCommentView = pendingComment,
                    )
                }
            } else if (pendingComment.parentId == null) {
                topNodes.add(
                    0,
                    CommentNodeData(
                        commentView = PostViewModel.ListView.PendingCommentListView(
                            pendingComment,
                            author = accountManager.getAccountById(pendingComment.accountId)?.name,
                        ),
                        children = mutableListOf(),
                        depth = 0,
                    ),
                )
            } else {
                val parent = map[pendingComment.parentId]

                parent?.let {
                    it.children.add(
                        0,
                        CommentNodeData(
                            commentView = PostViewModel.ListView.PendingCommentListView(
                                pendingComment,
                                author = accountManager.getAccountById(pendingComment.accountId)?.name,
                            ),
                            children = mutableListOf(),
                            depth = it.depth + 1,
                        ),
                    )
                }
            }
        }

        return topNodes
    }
    private fun addMoreItems(post: PostView, topNodes: MutableList<CommentNodeData>) {
        val toVisit = LinkedList<CommentNodeData>()
        toVisit.addAll(topNodes)

        while (toVisit.isNotEmpty()) {
            val node = toVisit.pollFirst() ?: continue

            val cv = node.commentView
            if (cv !is PostViewModel.ListView.CommentListView) {
                continue
            }

            var childrenCount = 0
            val expectedCount = cv.comment.counts.child_count

            node.children.forEach {
                when (val commentView = it.commentView) {
                    is PostViewModel.ListView.CommentListView -> {
                        childrenCount += commentView.comment.counts.child_count + 1 // + 1 for this comment
                        toVisit.push(it)
                    }
                    is PostViewModel.ListView.MoreCommentsItem -> {}
                    is PostViewModel.ListView.PendingCommentListView -> {
                        childrenCount += 1 // + 1 for this comment
                    }
                    is PostViewModel.ListView.PostListView -> {}
                }
            }

            // At time of writing there is no way to detect number of removed comments
            // A comment can say 3 children but only 2 children exist
            // In this state, we have no idea if there is 1 more child that can be fetched or
            // if the missing comment has been deleted
            //
            // Jerboa just checks if the children is empty to differentiate so we'll do that too.
            if (childrenCount < expectedCount && node.children.isEmpty()) {
                node.children.add(
                    CommentNodeData(
                        PostViewModel.ListView.MoreCommentsItem(
                            cv.comment.comment.id,
                            node.depth + 1,
                            expectedCount - childrenCount,
                        ),
                        node.depth + 1,
                    ),
                )
            }
        }

        var childrenCount = 0
        val expectedCount = post.counts.comments

        topNodes.forEach {
            when (val commentView = it.commentView) {
                is PostViewModel.ListView.CommentListView -> {
                    childrenCount += commentView.comment.counts.child_count + 1 // + 1 for this comment
                }
                is PostViewModel.ListView.MoreCommentsItem -> {}
                is PostViewModel.ListView.PendingCommentListView -> {
                    childrenCount += 1 // + 1 for this comment
                }
                is PostViewModel.ListView.PostListView -> {}
            }
        }

        if (childrenCount < expectedCount) {
            topNodes.add(
                CommentNodeData(
                    PostViewModel.ListView.MoreCommentsItem(
                        null,
                        0,
                        expectedCount - childrenCount,
                    ),
                    0,
                ),
            )
        }
    }

    private fun getCommentParentId(comment: Comment?): Int? {
        val split = comment?.path?.split(".")?.toMutableList()
        // remove the 0
        split?.removeFirst()
        return if (split !== null && split.size > 1) {
            split[split.size - 2].toInt()
        } else {
            null
        }
    }
}

fun CommentNodeData.isChildComment(commentId: Long): Boolean {
    return when (val commentView = commentView) {
        is PostViewModel.ListView.CommentListView -> {
            commentView.comment.comment.id.toLong() == commentId
        }
        is PostViewModel.ListView.MoreCommentsItem -> {
            false
        }
        is PostViewModel.ListView.PendingCommentListView -> {
            commentView.pendingCommentView.id == commentId
        }
        is PostViewModel.ListView.PostListView -> {
            false
        }
    } || children.any { it.isChildComment(commentId) }
}

fun List<CommentNodeData>.flatten(): MutableList<CommentNodeData> {
    val result = mutableListOf<CommentNodeData>()

    fun CommentNodeData.flattenRecursive() {
        result.add(this)

        when (val commentView = this.commentView) {
            is PostViewModel.ListView.CommentListView -> {
                if (commentView.isCollapsed) {
                    return
                }
            }
            is PostViewModel.ListView.PendingCommentListView -> {
                if (commentView.isCollapsed) {
                    return
                }
            }
            is PostViewModel.ListView.PostListView -> {
                // this should never happen
            }

            is PostViewModel.ListView.MoreCommentsItem -> {
                // shouldnt happen
            }
        }

        this.children.forEach {
            it.flattenRecursive()
        }
    }
    this.forEach {
        it.flattenRecursive()
    }

    return result
}
