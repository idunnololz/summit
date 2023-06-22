package com.idunnololz.summit.lemmy.post

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import arrow.core.Either
import com.idunnololz.summit.account.AccountActionsManager
import com.idunnololz.summit.api.AccountAwareLemmyClient
import com.idunnololz.summit.api.dto.Comment
import com.idunnololz.summit.api.dto.CommentView
import com.idunnololz.summit.api.dto.PostView
import com.idunnololz.summit.lemmy.PostRef
import com.idunnololz.summit.lemmy.utils.VoteUiHandler
import com.idunnololz.summit.scrape.WebsiteAdapterLoader
import com.idunnololz.summit.reddit.CommentsSortOrder
import com.idunnololz.summit.reddit.toApiSortOrder
import com.idunnololz.summit.util.StatefulLiveData
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import java.util.LinkedHashMap
import javax.inject.Inject

@HiltViewModel
class PostViewModel @Inject constructor(
    private val lemmyApiClient: AccountAwareLemmyClient,
    private val accountActionsManager: AccountActionsManager,
) : ViewModel() {
    companion object {
        private const val TAG = "PostViewModel"
    }

    private var postRef: PostRef? = null

    val voteUiHandler: VoteUiHandler = accountActionsManager.voteUiHandler
    private var loader: WebsiteAdapterLoader? = null

    private var commentLoaders = ArrayList<WebsiteAdapterLoader>()

    private var postView: PostView? = null
    private var comments: List<CommentView>? = null

    val commentsSortOrderLiveData = MutableLiveData(CommentsSortOrder.Top)

    val postData = StatefulLiveData<PostData>()

    init {
        commentsSortOrderLiveData.observeForever {
            val postRef = postRef
            if (postRef != null) {
                fetchPostData(postRef.instance, postRef.id, fetchPostData = false)
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
        postRef = PostRef(instance, postId)
        postData.setIsLoading()

        val sortOrder = requireNotNull(commentsSortOrderLiveData.value).toApiSortOrder()

        viewModelScope.launch {
            lemmyApiClient.changeInstance(instance)

            val post = if (fetchPostData) {
                lemmyApiClient.fetchPostWithRetry(Either.Left(postId), force)
                    .fold(
                        onSuccess = { it },
                        onFailure = {
                            postData.postError(it)
                            null
                        },
                    )
            } else {
                this@PostViewModel.postView
            }
            this@PostViewModel.postView = post

            val comments = if (fetchCommentData) {
                lemmyApiClient.fetchCommentsWithRetry(Either.Left(postId), sortOrder, force)
                    .fold(
                        onSuccess = { it },
                        onFailure = {
                            postData.postError(it)
                            null
                        },
                    )
            } else {
                this@PostViewModel.comments
            }
            this@PostViewModel.comments = comments

            if (post == null || comments == null) {
                return@launch
            }

            postData.postValue(
                PostData(
                    ListView.PostListView(post),
                    buildCommentsTreeListView(comments, parentComment = true)
                )
            )
        }
    }

    fun fetchMoreComments(url: String, parentId: String, force: Boolean = false) {
        TODO()
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
    )

    sealed interface ListView {
        data class PostListView(
            val post: PostView
        ) : ListView

        data class CommentListView(
            val comment: CommentView,
            var isCollapsed: Boolean = false,
        ) : ListView
    }

    data class CommentNodeData(
        val commentView: ListView.CommentListView,
        // Must use a SnapshotStateList and not a MutableList here, otherwise changes in the tree children won't trigger a UI update
        val children: MutableList<CommentNodeData>?,
        var depth: Int,
    )

    private fun buildCommentsTreeListView(
        comments: List<CommentView>?,
        parentComment: Boolean,
    ): List<CommentNodeData> {
        val map = LinkedHashMap<Number, CommentNodeData>()
        val firstComment = comments?.firstOrNull()?.comment

        val depthOffset = if (!parentComment) { 0 } else {
            firstComment?.getDepth() ?: 0
        }

        comments?.forEach { comment ->
            val depth = comment.comment.getDepth().minus(depthOffset)
            val node = CommentNodeData(
                commentView = ListView.CommentListView(comment),
                children = mutableListOf(),
                depth,
            )
            map[comment.comment.id] = node
        }

        val tree = mutableListOf<CommentNodeData>()

        comments?.forEach { cv ->
            val child = map[cv.comment.id]
            child?.let { cChild ->
                val parentId = getCommentParentId(cv.comment)
                parentId?.let { cParentId ->
                    val parent = map[cParentId]

                    // Necessary because blocked comment might not exist
                    parent?.let { cParent ->
                        cParent.children?.add(cChild)
                    }
                } ?: run {
                    tree.add(cChild)
                }
            }
        }

        return tree
    }

    fun getCommentParentId(comment: Comment?): Int? {
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

    fun deleteComment(commentId: String) {
        TODO()
    }
}

fun List<PostViewModel.CommentNodeData>.flatten(): MutableList<PostViewModel.CommentNodeData> {
    val result = mutableListOf<PostViewModel.CommentNodeData>()

    fun PostViewModel.CommentNodeData.flattenRecursive() {

        result.add(this)

        if (this.commentView.isCollapsed) {
            return
        }

        this.children?.forEach {
            it.flattenRecursive()
        }
    }
    this.forEach {
        it.flattenRecursive()
    }

    return result
}