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

private const val TAG = "CommentTreeBuilder"

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
        fullyLoadedCommentIds: Set<Int>,
        targetCommentRef: CommentRef?,
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

        val targetCommentId = targetCommentRef?.id

        // add missing comments first
        ArrayList(map.values).forEach { node ->
            val commentView = node.commentView
            if (commentView !is PostViewModel.ListView.CommentListView) {
                return@forEach
            }

            val parentId = getCommentParentId(commentView.comment.comment)

            parentId?.let { cParentId ->
                if (map[cParentId] == null && commentView.comment.comment.id != targetCommentId) {
                    val parentParentNodeId = commentView.comment.comment.parentParentNodeId

                    Log.d(
                        TAG,
                        "Can't find parent id: ${commentView.comment.comment.path} " +
                            "parentNodeId: $cParentId parentParentNodeId: $parentParentNodeId " +
                            "depth: ${node.depth}",
                    )

                    // Let's make a dummy comment in this case!
                    val parentNodeData = CommentNodeData(
                        PostViewModel.ListView.MissingCommentItem(
                            cParentId,
                            parentParentNodeId,
                            false,
                        ),
                        node.depth - 1,
                    )
                    map[cParentId] = parentNodeData
                }
            }
        }

        map.values.forEach { node ->
            val commentView = node.commentView
            if (commentView !is PostViewModel.ListView.CommentListView &&
                commentView !is PostViewModel.ListView.MissingCommentItem
            ) {
                return@forEach
            }

            when (commentView) {
                is PostViewModel.ListView.CommentListView -> {
                    val parentId = getCommentParentId(commentView.comment.comment)

                    parentId?.let { cParentId ->
                        val parent = map[cParentId]

                        if (parent == null) {
                            Log.e(TAG, "Can't find parent id: ${commentView.comment.comment.path}")
                        }

                        // Necessary because blocked comment might not exist
                        parent?.children?.add(node)
                    } ?: run {
                        topNodes.add(node)
                    }
                }
                is PostViewModel.ListView.MissingCommentItem -> {
                    val parentId = commentView.parentCommentId

                    parentId?.let { cParentId ->
                        val parent = map[cParentId]

                        // Necessary because blocked comment might not exist
                        parent?.children?.add(node)
                    } ?: run {
                        topNodes.add(node)
                    }
                }
                is PostViewModel.ListView.MoreCommentsItem,
                is PostViewModel.ListView.PendingCommentListView,
                is PostViewModel.ListView.PostListView,
                -> {}
            }
        }

        if (post != null) {
            addMoreItems(post, topNodes, fullyLoadedCommentIds)
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

    private fun addMoreItems(
        post: PostView,
        topNodes: MutableList<CommentNodeData>,
        fullyLoadedCommentIds: Set<Int>,
    ) {
        val toVisit = LinkedList<CommentNodeData>()
        toVisit.addAll(topNodes)

        while (toVisit.isNotEmpty()) {
            val node = toVisit.pollFirst() ?: continue

            val commentView = node.commentView
            if (commentView !is PostViewModel.ListView.CommentListView) {
                continue
            }

            var childrenCount = 0
            val expectedCount = commentView.comment.counts.child_count

            node.children.forEach {
                when (val commentView = it.commentView) {
                    is PostViewModel.ListView.CommentListView -> {
                        childrenCount += commentView.comment.counts.child_count + 1 // + 1 for this comment
                        toVisit.push(it)
                    }
                    is PostViewModel.ListView.PendingCommentListView -> {
                        childrenCount += 1 // + 1 for this comment
                    }
                    is PostViewModel.ListView.MoreCommentsItem,
                    is PostViewModel.ListView.PostListView,
                    is PostViewModel.ListView.MissingCommentItem,
                    -> {}
                }
            }

            // At time of writing there is no way to detect number of removed comments
            // A comment can say 3 children but only 2 children exist
            // In this state, we have no idea if there is 1 more child that can be fetched or
            // if the missing comment has been deleted
            //
            // Jerboa just checks if the children is empty to differentiate so we'll do that too.
            if (childrenCount < expectedCount && node.children.isEmpty()) {
                if (fullyLoadedCommentIds.contains(commentView.comment.comment.id)) {
                    val missingCount = expectedCount - childrenCount

                    val parentParentNodeId = commentView.comment.comment.parentParentNodeId

                    repeat(missingCount) {
                        node.children.add(
                            CommentNodeData(
                                PostViewModel.ListView.MissingCommentItem(
                                    commentView.comment.comment.id,
                                    parentParentNodeId,
                                    false,
                                ),
                                node.depth + 1,
                            )
                        )
                    }
                } else {
                    node.children.add(
                        CommentNodeData(
                            PostViewModel.ListView.MoreCommentsItem(
                                commentView.comment.comment.id,
                                node.depth + 1,
                                expectedCount - childrenCount,
                            ),
                            node.depth + 1,
                        ),
                    )
                }
            }
        }

        var childrenCount = 0
        val expectedCount = post.counts.comments

        topNodes.forEach {
            when (val commentView = it.commentView) {
                is PostViewModel.ListView.CommentListView -> {
                    childrenCount += commentView.comment.counts.child_count + 1 // + 1 for this comment
                }
                is PostViewModel.ListView.PendingCommentListView -> {
                    childrenCount += 1 // + 1 for this comment
                }
                is PostViewModel.ListView.MoreCommentsItem,
                is PostViewModel.ListView.PostListView,
                is PostViewModel.ListView.MissingCommentItem,
                -> {}
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

private val Comment.parentParentNodeId
    get() = path
        .split(".")
        .dropLast(2)
        .lastOrNull()
        ?.toIntOrNull()

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

            is PostViewModel.ListView.MissingCommentItem -> {
                if (commentView.isCollapsed) {
                    return
                }
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
