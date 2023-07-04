package com.idunnololz.summit.lemmy.post

import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import arrow.core.Either
import com.idunnololz.summit.account.AccountActionsManager
import com.idunnololz.summit.account.AccountManager
import com.idunnololz.summit.actions.PendingCommentView
import com.idunnololz.summit.api.AccountAwareLemmyClient
import com.idunnololz.summit.api.dto.Comment
import com.idunnololz.summit.api.dto.CommentId
import com.idunnololz.summit.api.dto.CommentSortType
import com.idunnololz.summit.api.dto.CommentView
import com.idunnololz.summit.api.dto.Post
import com.idunnololz.summit.api.dto.PostView
import com.idunnololz.summit.api.utils.getDepth
import com.idunnololz.summit.lemmy.PostRef
import com.idunnololz.summit.lemmy.utils.VoteUiHandler
import com.idunnololz.summit.scrape.WebsiteAdapterLoader
import com.idunnololz.summit.reddit.CommentsSortOrder
import com.idunnololz.summit.reddit.toApiSortOrder
import com.idunnololz.summit.util.StatefulLiveData
import com.idunnololz.summit.util.dateStringToTs
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.LinkedHashMap
import java.util.LinkedList
import javax.inject.Inject

@HiltViewModel
class PostViewModel @Inject constructor(
    private val lemmyApiClient: AccountAwareLemmyClient,
    private val accountActionsManager: AccountActionsManager,
    private val accountManager: AccountManager,
) : ViewModel() {

    companion object {
        private const val TAG = "PostViewModel"

        const val HIGHLIGHT_COMMENT_MS = 3_500L
    }

    private var postRef: PostRef? = null

    val voteUiHandler: VoteUiHandler = accountActionsManager.voteUiHandler
    private var loader: WebsiteAdapterLoader? = null

    private var commentLoaders = ArrayList<WebsiteAdapterLoader>()

    private var postView: PostView? = null
    private var comments: List<CommentView>? = null
    private var pendingComments: List<PendingCommentView>? = null
    private var newlyPostedCommentId: CommentId? = null

    private val additionalLoadedCommentIds = mutableSetOf<CommentId>()
    /**
     * Comments that didn't load by default but were loaded by the user requesting additional comments
     */
    private var supplementaryComments = mutableMapOf<Int, CommentView>()

    val commentsSortOrderLiveData = MutableLiveData(CommentsSortOrder.Top)

    val postData = StatefulLiveData<PostData>()

    val deletePostResult = StatefulLiveData<PostView>()

    init {
        commentsSortOrderLiveData.observeForever {
            val postRef = postRef
            if (postRef != null) {
                fetchPostData(postRef.instance, postRef.id, fetchPostData = false)
            }
        }
        viewModelScope.launch {
            accountActionsManager.onCommentActionChanged.collect {
                val postRef = postRef
                if (postRef != null) {
                    updatePendingComments(postRef, resolveCompletedPendingComments = true)
                }
            }
        }

    }

    val instance: String
        get() = lemmyApiClient.instance

    fun fetchPostData(
        instance: String,
        postId: Int,
        fetchPostData: Boolean = true,
        fetchCommentData: Boolean = true,
        force: Boolean = false
    ) {
        val postRef = PostRef(instance, postId).also {
            postRef = it
        }
        postData.setIsLoading()

        val sortOrder = requireNotNull(commentsSortOrderLiveData.value).toApiSortOrder()

        viewModelScope.launch {
            lemmyApiClient.changeInstance(instance)

            val postResult = if (fetchPostData) {
                lemmyApiClient.fetchPostWithRetry(Either.Left(postId), force)
            } else {
                Result.success(this@PostViewModel.postView)
            }

            this@PostViewModel.postView = postResult.getOrNull()

            val commentsResult = if (fetchCommentData) {
                lemmyApiClient.fetchCommentsWithRetry(Either.Left(postId), sortOrder, force)
            } else {
                Result.success(this@PostViewModel.comments)
            }
            this@PostViewModel.comments = commentsResult.getOrNull()

            if (force) {
                additionalLoadedCommentIds.forEach {
                    fetchMoreCommentsInternal(it, sortOrder, force)
                }
            }

            updatePendingCommentsInternal(postRef, sortOrder, true)

            val post = postResult.getOrNull()
            val comments = commentsResult.getOrNull()
            if (post == null || comments == null) {
                postResult
                    .onFailure {
                        postData.postError(it)
                    }
                    .onSuccess {}

                commentsResult
                    .onFailure {
                        postData.postError(it)
                    }
                    .onSuccess {}
            } else {
                updateData()
            }
        }
    }

    private fun updatePendingComments(
        postRef: PostRef,
        resolveCompletedPendingComments: Boolean,
    ) {
        val sortOrder = requireNotNull(commentsSortOrderLiveData.value).toApiSortOrder()

        viewModelScope.launch {
            updatePendingCommentsInternal(postRef, sortOrder, resolveCompletedPendingComments)

            updateData()
        }
    }

    private suspend fun updatePendingCommentsInternal(
        postRef: PostRef,
        sortOrder: CommentSortType,
        resolveCompletedPendingComments: Boolean,
    ) {
        pendingComments = accountActionsManager.getPendingComments(postRef)

        var modified = false
        if (resolveCompletedPendingComments) {
            pendingComments?.forEach { pendingComment ->
                if (pendingComment.complete) {
                    val result = lemmyApiClient.fetchCommentsWithRetry(
                        Either.Left(pendingComment.postRef.id), sortOrder, force = true
                    )

                    this@PostViewModel.comments = result.getOrNull()

                    val commentsResult = if (pendingComment.parentId == null) {
                        result
                    } else {
                        fetchMoreCommentsInternal(pendingComment.parentId, sortOrder, force = true)
                    }

                    commentsResult
                        .onSuccess {
                            modified = true

                            // find the comment that was recently posts by guessing!

                            if (pendingComment.isActionDelete) {
                                newlyPostedCommentId = pendingComment.commentId
                            } else if (pendingComment.commentId != null) {
                                newlyPostedCommentId = pendingComment.commentId
                            } else {
                                newlyPostedCommentId = it
                                    .sortedByDescending {
                                        if (it.comment.updated != null) {
                                            dateStringToTs(it.comment.updated)
                                        } else {
                                            dateStringToTs(it.comment.published)
                                        }
                                    }
                                    .firstOrNull {
                                        it.comment.creator_id == accountManager.currentAccount.value?.id
                                    }
                                    ?.comment?.id
                            }

                            accountActionsManager.removePendingComment(pendingComment)
                        }
                }
            }
        }

        if (modified) {
            pendingComments = accountActionsManager.getPendingComments(postRef)
        }
    }

    private suspend fun updateData() {
        val post = postView ?: return
        val comments = comments
        val pendingComments = pendingComments
        val supplementaryComments = supplementaryComments

        postData.postValue(
            PostData(
                ListView.PostListView(post),
                buildCommentsTreeListView(
                    post,
                    comments,
                    parentComment = true,
                    pendingComments,
                    supplementaryComments,
                ),
                newlyPostedCommentId = newlyPostedCommentId,
            )
        )
    }

    fun fetchMoreComments(parentId: CommentId?, force: Boolean = false) {
        val postRef = postRef ?: return
        val sortOrder = requireNotNull(commentsSortOrderLiveData.value).toApiSortOrder()

        viewModelScope.launch {
            if (parentId != null) {
                fetchMoreCommentsInternal(parentId, sortOrder, force)
            } else {
                // TODO maybe?
            }

            updateData()
        }
    }

    fun resetNewlyPostedComment() {
        newlyPostedCommentId = null

        viewModelScope.launch {
            delay(HIGHLIGHT_COMMENT_MS)

            updateData()
        }
    }

    private suspend fun fetchMoreCommentsInternal(
        parentId: CommentId,
        sortOrder: CommentSortType,
        force: Boolean = false
    ): Result<List<CommentView>> {
        val result = lemmyApiClient.fetchCommentsWithRetry(
            Either.Right(parentId),
            sortOrder,
            force,
        )

        additionalLoadedCommentIds.add(parentId)

        result.onSuccess { comments ->
            comments.forEach {
                supplementaryComments[it.comment.id] = it
            }
        }

        return result
    }

    override fun onCleared() {
        super.onCleared()

        loader?.destroy()

        for (l in commentLoaders) {
            l.destroy()
        }
    }

    data class PostData(
        val postView: ListView.PostListView,
        val commentTree: List<CommentNodeData>,
        val newlyPostedCommentId: CommentId?,
    )

    sealed interface ListView {
        data class PostListView(
            val post: PostView
        ) : ListView

        data class CommentListView(
            val comment: CommentView,
            val pendingCommentView: PendingCommentView? = null,
            var isCollapsed: Boolean = false,
        ) : ListView

        data class PendingCommentListView(
            val pendingCommentView: PendingCommentView,
            var isCollapsed: Boolean = false,
            var author: String?,
        ) : ListView

        data class MoreCommentsItem(
            val parentCommentId: CommentId?,
            val depth: Int,
            val moreCount: Int,
        ) : ListView
    }

    data class CommentNodeData(
        var commentView: ListView,
        var depth: Int,
        val children: MutableList<CommentNodeData> = mutableListOf(),
    )

    private suspend fun buildCommentsTreeListView(
        post: PostView,
        comments: List<CommentView>?,
        parentComment: Boolean,
        pendingComments: List<PendingCommentView>?,
        supplementaryComments: Map<Int, CommentView>,
    ): List<CommentNodeData> {
        val map = LinkedHashMap<Number, CommentNodeData>()
        val firstComment = comments?.firstOrNull()?.comment
        val idToPendingComments = pendingComments?.associateBy { it.commentId } ?: mapOf()

        val depthOffset = if (!parentComment) { 0 } else {
            comments?.minOfOrNull { it.getDepth() } ?: 0
        }

        Log.d(TAG, "Score: ${comments?.firstOrNull()?.counts?.score} " +
                "Depth: ${firstComment?.getDepth()} First comment: ${firstComment?.content}. ")

        val topNodes = mutableListOf<CommentNodeData>()

        supplementaryComments.values.forEach { comment ->
            val depth = comment.comment.getDepth().minus(depthOffset)
            val node = CommentNodeData(
                commentView = ListView.CommentListView(
                    comment,
                    pendingCommentView = idToPendingComments[comment.comment.id]
                ),
                depth = depth,
            )
            map[comment.comment.id] = node
        }
        comments?.forEach { comment ->
            val depth = comment.comment.getDepth().minus(depthOffset)
            val node = CommentNodeData(
                commentView = ListView.CommentListView(
                    comment,
                    pendingCommentView = idToPendingComments[comment.comment.id]
                ),
                depth = depth,
            )
            map[comment.comment.id] = node
        }

        map.values.forEach { node ->
            val commentView = node.commentView
            if (commentView !is ListView.CommentListView) {
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

        addMoreItems(post, topNodes)

        pendingComments?.forEach { pendingComment ->
            if (pendingComment.commentId != null) {
                // this is an updated comment

                val originalComment = map[pendingComment.commentId]
                val commentView = originalComment?.commentView
                if (originalComment != null && commentView is ListView.CommentListView) {
                    originalComment.commentView = commentView.copy(
                        pendingCommentView = pendingComment
                    )
                }
            } else if (pendingComment.parentId == null) {
                topNodes.add(
                    0,
                    CommentNodeData(
                        commentView = ListView.PendingCommentListView(
                            pendingComment,
                            author = accountManager.getAccountById(pendingComment.accountId)?.name
                        ),
                        children = mutableListOf(),
                        depth = 0,
                    )
                )
            } else {
                val parent = map[pendingComment.parentId]

                Log.d("HAHA", "depthOffset: ${depthOffset}")
                Log.d("HAHA", "comments: $comments")

                parent?.let {
                    it.children.add(
                        0,
                        CommentNodeData(
                            commentView = ListView.PendingCommentListView(
                                pendingComment,
                                author = accountManager.getAccountById(pendingComment.accountId)?.name
                            ),
                            children = mutableListOf(),
                            depth = it.depth + 1,
                        )
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
            if (cv !is ListView.CommentListView) {
                continue
            }

            var childrenCount = 0
            val expectedCount = cv.comment.counts.child_count

            node.children.forEach {
                when (val commentView = it.commentView) {
                    is ListView.CommentListView -> {
                        childrenCount += commentView.comment.counts.child_count + 1 // + 1 for this comment
                        toVisit.push(it)
                    }
                    is ListView.MoreCommentsItem -> {}
                    is ListView.PendingCommentListView -> {
                        childrenCount += 1 // + 1 for this comment
                    }
                    is ListView.PostListView -> {}
                }
            }

            if (childrenCount < expectedCount) {
                node.children.add(
                    CommentNodeData(
                        ListView.MoreCommentsItem(
                            cv.comment.comment.id,
                            node.depth + 1,
                            expectedCount - childrenCount
                        ),
                        node.depth + 1,
                    )
                )
            }
        }

        var childrenCount = 0
        val expectedCount = post.counts.comments

        topNodes.forEach {
            when (val commentView = it.commentView) {
                is ListView.CommentListView -> {
                    childrenCount += commentView.comment.counts.child_count + 1 // + 1 for this comment
                }
                is ListView.MoreCommentsItem -> {}
                is ListView.PendingCommentListView -> {
                    childrenCount += 1 // + 1 for this comment
                }
                is ListView.PostListView -> {}
            }
        }

        if (childrenCount < expectedCount) {
            topNodes.add(
                CommentNodeData(
                    ListView.MoreCommentsItem(
                        null,
                        0,
                        expectedCount - childrenCount
                    ),
                    0,
                )
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

    fun setCommentsSortOrder(sortOrder: CommentsSortOrder) {
        commentsSortOrderLiveData.value = sortOrder
    }

    fun deleteComment(postRef: PostRef, commentId: Int) {
        viewModelScope.launch {
            accountActionsManager.deleteComment(postRef, commentId)
        }
    }

    fun deletePost(post: Post) {
        viewModelScope.launch {
            lemmyApiClient.deletePost(post.id)
                .onSuccess {
                    deletePostResult.postValue(it)
                }
                .onFailure {
                    deletePostResult.postError(it)
                }
        }
    }
}

fun List<PostViewModel.CommentNodeData>.flatten(): MutableList<PostViewModel.CommentNodeData> {
    val result = mutableListOf<PostViewModel.CommentNodeData>()

    fun PostViewModel.CommentNodeData.flattenRecursive() {

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