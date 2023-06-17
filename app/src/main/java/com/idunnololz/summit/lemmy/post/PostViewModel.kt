package com.idunnololz.summit.lemmy.post

import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import arrow.core.Either
import com.idunnololz.summit.api.LemmyApiClient
import com.idunnololz.summit.api.dto.Comment
import com.idunnololz.summit.api.dto.CommentView
import com.idunnololz.summit.api.dto.PostView
import com.idunnololz.summit.scrape.WebsiteAdapterLoader
import com.idunnololz.summit.reddit.CommentsSortOrder
import com.idunnololz.summit.reddit_objects.*
import com.idunnololz.summit.reddit_website_adapter.MoreChildrenWebsiteAdapter
import com.idunnololz.summit.util.StatefulLiveData
import com.idunnololz.summit.util.Utils.hashSha256
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import java.util.LinkedHashMap
import javax.inject.Inject

@HiltViewModel
class PostViewModel @Inject constructor(
    private val lemmyApiClient: LemmyApiClient
) : ViewModel() {
    companion object {
        private const val TAG = "PostViewModel"
    }

    private var loader: WebsiteAdapterLoader? = null

    private var commentLoaders = ArrayList<WebsiteAdapterLoader>()

    var commentsSortOrder: CommentsSortOrder? = null
        private set

    val postData = StatefulLiveData<PostData>()
    val redditMoreComments = MutableLiveData<HashMap<String, List<CommentItemObject>>>()

    init {
        redditMoreComments.postValue(HashMap())
    }

    fun fetchPostData(postId: Int, force: Boolean = false) {
        postData.setIsLoading()

        viewModelScope.launch {
            val post = lemmyApiClient.fetchPost(null, Either.Left(postId))
            val comments = lemmyApiClient.fetchComments(null, Either.Left(postId))

            postData.postValue(
                PostData(
                    ListView.PostListView(post),
                    buildCommentsTreeListView(comments, parentComment = true)
                )
            )
        }
    }

    fun fetchMoreComments(url: String, parentId: String, force: Boolean = false) {
        commentLoaders.add(
            WebsiteAdapterLoader().apply {
                add(
                    MoreChildrenWebsiteAdapter(),
                    url,
                    "morechildren:${hashSha256(url)}"
                )
                setOnEachAdapterLoadedListener {
                    if (it is MoreChildrenWebsiteAdapter) {
                        if (it.isSuccess()) {

                            val map = redditMoreComments.value ?: HashMap()

                            it.get().json?.data?.things?.filterIsInstance<CommentItemObject>()
                                ?.let { commentItemObjects ->

                                    // result is flattened... try to unflatten

                                    val dict = commentItemObjects.associateBy { it.data?.name }
                                    val topLevel: MutableList<CommentItemObject> =
                                        commentItemObjects.toMutableList()

                                    commentItemObjects.forEach {
                                        val p = dict[it.data?.parentId]
                                        if (p != null) {
                                            topLevel.remove(it)

                                            p.data?.replies =
                                                ListingObject(
                                                    kind = "Listing",
                                                    data = ListingData(
                                                        modHash = "clientSided",
                                                        dist = 0,
                                                        children = ((p.data?.replies as? ListingObject)
                                                            ?.data?.children ?: listOf()) + listOf(
                                                            it
                                                        )
                                                    )
                                                )
                                        }
                                    }


                                    map[parentId] = topLevel
                                }

                            redditMoreComments.postValue(map)
                        } else {
                            Log.e(TAG, "Error loading more comments: ${it.error}")
                        }
                    }
                }
            }.load(forceRefetch = force)
        )
    }

    override fun onCleared() {
        super.onCleared()

        loader?.destroy()

        for (l in commentLoaders) {
            l.destroy()
        }
    }

    fun setCommentsSortOrder(sortOrder: CommentsSortOrder) {
        commentsSortOrder = sortOrder
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

    fun buildCommentsTreeListView(
        comments: List<CommentView>?,
        parentComment: Boolean,
    ): List<CommentNodeData> {
        val map = LinkedHashMap<Number, CommentNodeData>()
        val firstComment = comments?.firstOrNull()?.comment

        val depthOffset = if (!parentComment) { 0 } else {
            firstComment?.getDepth() ?: 0
        }

        comments?.forEach { cv ->
            val depth = cv.comment.getDepth().minus(depthOffset)
            val node = CommentNodeData(
                commentView = ListView.CommentListView(cv),
                children = mutableListOf(),
                depth,
            )
            map[cv.comment.id] = node
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
}

fun List<PostViewModel.CommentNodeData>.flatten(): MutableList<PostViewModel.ListView.CommentListView> {
    val result = mutableListOf<PostViewModel.ListView.CommentListView>()

    fun PostViewModel.CommentNodeData.flattenRecursive() {

        result.add(this.commentView)

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