package com.idunnololz.summit.lemmy.post

import android.app.Application
import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import arrow.core.Either
import com.idunnololz.summit.R
import com.idunnololz.summit.account.Account
import com.idunnololz.summit.account.AccountActionsManager
import com.idunnololz.summit.account.AccountManager
import com.idunnololz.summit.account.AccountView
import com.idunnololz.summit.account.info.AccountInfoManager
import com.idunnololz.summit.actions.PendingCommentView
import com.idunnololz.summit.actions.PostReadManager
import com.idunnololz.summit.api.AccountAwareLemmyClient
import com.idunnololz.summit.api.CommentsFetcher
import com.idunnololz.summit.api.LemmyApiClient
import com.idunnololz.summit.api.dto.CommentId
import com.idunnololz.summit.api.dto.CommentSortType
import com.idunnololz.summit.api.dto.CommentView
import com.idunnololz.summit.api.dto.PostView
import com.idunnololz.summit.lemmy.CommentNavControlsState
import com.idunnololz.summit.lemmy.CommentNodeData
import com.idunnololz.summit.lemmy.CommentRef
import com.idunnololz.summit.lemmy.CommentTreeBuilder
import com.idunnololz.summit.lemmy.CommentsSortOrder
import com.idunnololz.summit.lemmy.PostRef
import com.idunnololz.summit.lemmy.toApiSortOrder
import com.idunnololz.summit.lemmy.utils.toVotableRef
import com.idunnololz.summit.preferences.PreferenceManager
import com.idunnololz.summit.preferences.Preferences
import com.idunnololz.summit.util.StatefulLiveData
import com.idunnololz.summit.util.dateStringToTs
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class PostViewModel @Inject constructor(
    private val context: Application,
    private val lemmyApiClientFactory: AccountAwareLemmyClient.Factory,
    private val accountActionsManager: AccountActionsManager,
    private val accountManager: AccountManager,
    private val accountInfoManager: AccountInfoManager,
    private val postReadManager: PostReadManager,
    private val preferenceManager: PreferenceManager,
    private val state: SavedStateHandle,
    private val unauthedApiClient: LemmyApiClient,
    val queryMatchHelper: QueryMatchHelper,
) : ViewModel() {

    companion object {
        private const val TAG = "PostViewModel"

        const val HIGHLIGHT_COMMENT_MS = 3_500L
    }

    /**
     * Create a new instance so we can change the instance without screwing up app state
     */
    val lemmyApiClient = lemmyApiClientFactory.create()

    var postOrCommentRef: Either<PostRef, CommentRef>? = null
        set(value) {
            field = value

            state["postRef"] = value?.leftOrNull()
            state["commentRef"] = value?.getOrNull()
        }
    val onPostOrCommentRefChange = MutableLiveData<Either<PostRef, CommentRef>>()

    val currentAccountView = MutableLiveData<AccountView?>()

    val findInPageVisible = MutableLiveData<Boolean>(false)
    val findInPageQuery = MutableLiveData<String>("")
    val screenshotMode = MutableLiveData<Boolean>(false)
    private val findInPageQueryFlow = MutableStateFlow<String>("")

    private var postView: PostView? = null
    private var comments: List<CommentView>? = null
    private var pendingComments: List<PendingCommentView>? = null
    private var newlyPostedCommentId: CommentId? = null

    private val additionalLoadedCommentIds = mutableSetOf<CommentId>()
    private val removedCommentIds = mutableSetOf<CommentId>()

    /**
     * This is used for the edge case where a comment is fully loaded and some of it's direct
     * descendants are missing. This is can be used to check if comments are missing or just not
     * loaded yet.
     */
    private val fullyLoadedCommentIds = mutableSetOf<CommentId>()

    val switchAccountState = StatefulLiveData<Unit>()

    /**
     * Comments that didn't load by default but were loaded by the user requesting additional comments
     */
    private var supplementaryComments = mutableMapOf<Int, CommentView>()

    private val commentsFetcher = CommentsFetcher(lemmyApiClient, accountActionsManager)

    var preferences: Preferences = preferenceManager.currentPreferences

    val commentsSortOrderLiveData = MutableLiveData(
        preferences.defaultCommentsSortOrder ?: CommentsSortOrder.Top,
    )

    val postData = StatefulLiveData<PostData>()
    val commentNavControlsState = MutableLiveData<CommentNavControlsState?>()

    init {
        state.get<PostRef>("postRef")?.let {
            postOrCommentRef = Either.Left(it)
        }
        state.get<CommentRef>("commentRef")?.let {
            postOrCommentRef = Either.Right(it)
        }

        commentsSortOrderLiveData.observeForever {
            fetchPostData(fetchPostData = false)
        }
        viewModelScope.launch {
            accountActionsManager.onCommentActionChanged.collect {
                val postOrCommentRef = postOrCommentRef
                if (postOrCommentRef != null) {
                    updatePendingComments(
                        postOrCommentRef = postOrCommentRef,
                        resolveCompletedPendingComments = true,
                    )
                }
            }
        }
        viewModelScope.launch {
            accountManager.currentAccount.collect {
                withContext(Dispatchers.Main) {
                    preferences = preferenceManager.currentPreferences

                    if (it != null) {
                        currentAccountView.value = accountInfoManager.getAccountViewForAccount(it)
                    } else {
                        currentAccountView.value = null
                    }
                }
            }
        }
        viewModelScope.launch {
            findInPageQueryFlow
                .debounce(300)
                .collect {
                    findInPageQuery.postValue(it)
                }
        }
    }

    val apiInstance: String
        get() = lemmyApiClient.instance

    private val maxDepth: Int?
        get() = if (preferences.collapseChildCommentsByDefault) {
            1
        } else {
            null
        }

    fun updatePostOrCommentRef(
        postOrCommentRef: Either<PostRef, CommentRef>,
    ) {
        this.postOrCommentRef = postOrCommentRef

        onPostOrCommentRefChange.postValue(postOrCommentRef)
    }

    fun switchToNativeInstance() {
        val nativeInstance = currentAccountView.value?.account?.instance
            ?: return

        lemmyApiClient.changeInstance(nativeInstance)
        fetchPostData(force = true, switchToNativeInstance = true)
    }

    fun fetchPostData(
        fetchPostData: Boolean = true,
        fetchCommentData: Boolean = true,
        force: Boolean = false,
        switchToNativeInstance: Boolean = false,
    ): Job? {
        Log.d(
            TAG,
            "fetchPostData(): fetchPostData = $fetchPostData " +
                "fetchCommentData = $fetchCommentData force = $force",
        )

        postOrCommentRef ?: return null

        postData.setIsLoading()

        val sortOrder = requireNotNull(commentsSortOrderLiveData.value).toApiSortOrder()

        return viewModelScope.launch {
            if (switchToNativeInstance) {
                translatePostToCurrentInstance()
            }

            val postOrCommentRef = postOrCommentRef ?: return@launch

            lemmyApiClient.changeInstance(
                postOrCommentRef
                    .fold(
                        {
                            it.instance
                        },
                        {
                            it.instance
                        },
                    ),
            )

            val postResult = if (fetchPostData) {
                postOrCommentRef
                    .fold(
                        {
                            lemmyApiClient.fetchPostWithRetry(Either.Left(it.id), force)
                        },
                        {
                            lemmyApiClient.fetchPostWithRetry(Either.Right(it.id), force)
                        },
                    )
            } else {
                Result.success(this@PostViewModel.postView)
            }

            this@PostViewModel.postView = postResult.getOrNull()

            val commentsResult = if (fetchCommentData) {
                postOrCommentRef
                    .fold(
                        {
                            commentsFetcher.fetchCommentsWithRetry(
                                id = Either.Left(it.id),
                                sort = sortOrder,
                                maxDepth = maxDepth,
                                force = force,
                            )
                        },
                        {
                            commentsFetcher.fetchCommentsWithRetry(
                                id = Either.Right(it.id),
                                sort = sortOrder,
                                maxDepth = maxDepth,
                                force = force,
                            )
                        },
                    )
            } else {
                Result.success(this@PostViewModel.comments)
            }
            this@PostViewModel.comments = commentsResult.getOrNull()

            if (force) {
                additionalLoadedCommentIds.forEach {
                    fetchMoreCommentsInternal(it, sortOrder, null, force)
                }
            }

            updatePendingCommentsInternal(postOrCommentRef, sortOrder, true)

            val post = postResult.getOrNull()
            val comments = commentsResult.getOrNull()

            if (post != null) {
                postReadManager.markPostAsReadLocal(apiInstance, post.post.id, read = true)
                if (force) {
                    accountActionsManager.setScore(post.toVotableRef(), post.counts.score)
                }
            }

            if (force) {
                if (comments != null && fetchCommentData) {
                    comments.forEach {
                        accountActionsManager.setScore(it.toVotableRef(), it.counts.score)
                    }
                }
                if (post != null && fetchPostData) {
                    accountActionsManager.setScore(post.toVotableRef(), post.counts.score)
                }
            }

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

    fun switchAccount(account: Account) {
        val postOrCommentRef = postOrCommentRef ?: return

        val instance = postOrCommentRef.fold(
            { it.instance },
            { it.instance },
        )
        val didInstanceChange = instance != account.instance

        if (account.id == currentAccountView.value?.account?.id) {
            return
        }

        switchAccountState.setIsLoading(context.getString(R.string.switching_instance))

        Log.d(TAG, "Instance changed. Trying to resolve post in new instance.")

        unauthedApiClient.changeInstance(instance)

        viewModelScope.launch {
            val linkToResolve = postOrCommentRef
                .fold(
                    {
                        unauthedApiClient.fetchPost(null, Either.Left(it.id), force = false)
                            .fold(
                                onSuccess = {
                                    Result.success(it.post.ap_id)
                                },
                                onFailure = {
                                    Result.failure(it)
                                },
                            )
                    },
                    { commentRef ->
                        unauthedApiClient
                            .fetchComments(
                                null,
                                id = Either.Right(commentRef.id),
                                sort = CommentSortType.Top,
                                force = false,
                                maxDepth = 0,
                            )
                            .fold(
                                onSuccess = {
                                    val url = it.firstOrNull { it.comment.id == commentRef.id }?.comment?.ap_id
                                    if (url != null) {
                                        Result.success(url)
                                    } else {
                                        Result.failure(ObjectResolverFailedException())
                                    }
                                },
                                onFailure = {
                                    Result.failure(it)
                                },
                            )
                    },
                )

            accountManager.setCurrentAccount(account)

            linkToResolve
                .fold(
                    onSuccess = {
                        Log.d(
                            TAG,
                            "Attempting to resolve $linkToResolve " +
                                "on instance $apiInstance",
                        )
                        lemmyApiClient.resolveObject(it)
                    },
                    onFailure = {
                        Result.failure(it)
                    },
                )
                .fold(
                    onSuccess = {
                        val newPostOrCommentRef = if (it.post != null) {
                            Either.Left(PostRef(account.instance, it.post.post.id))
                        } else if (it.comment != null) {
                            Either.Right(CommentRef(account.instance, it.comment.comment.id))
                        } else {
                            null
                        }

                        if (newPostOrCommentRef != null) {
                            updatePostOrCommentRef(newPostOrCommentRef)

                            if (didInstanceChange) {
                                comments = null
                                pendingComments = null
                                supplementaryComments.clear()
                                newlyPostedCommentId = null
                                additionalLoadedCommentIds.clear()
                                removedCommentIds.clear()
                            }

                            fetchPostData(fetchPostData = true, force = true)
                                ?.join()
                        }

                        switchAccountState.postValue(Unit)
                    },
                    onFailure = {
                        Log.e(TAG, "Error resolving object.", it)

                        switchAccountState.postError(it)
                    },
                )
        }
    }

    private suspend fun translatePostToCurrentInstance() {
        val postOrCommentRef = postOrCommentRef ?: return
        val currentAccount = currentAccountView.value?.account ?: return

        val instance = postOrCommentRef.fold(
            { it.instance },
            { it.instance },
        )
        val isNativePost = instance == apiInstance

        if (isNativePost) return

        unauthedApiClient.changeInstance(instance)

        val linkToResolve = postOrCommentRef
            .fold(
                {
                    unauthedApiClient.fetchPost(null, Either.Left(it.id), force = false)
                        .fold(
                            onSuccess = {
                                Result.success(it.post.ap_id)
                            },
                            onFailure = {
                                Result.failure(it)
                            },
                        )
                },
                { commentRef ->
                    unauthedApiClient
                        .fetchComments(
                            null,
                            id = Either.Right(commentRef.id),
                            sort = CommentSortType.Top,
                            force = false,
                            maxDepth = 0,
                        )
                        .fold(
                            onSuccess = {
                                val url = it.firstOrNull { it.comment.id == commentRef.id }?.comment?.ap_id
                                if (url != null) {
                                    Result.success(url)
                                } else {
                                    Result.failure(ObjectResolverFailedException())
                                }
                            },
                            onFailure = {
                                Result.failure(it)
                            },
                        )
                },
            )

        linkToResolve
            .fold(
                onSuccess = {
                    Log.d(
                        TAG,
                        "Attempting to resolve $linkToResolve " +
                            "on instance $apiInstance",
                    )
                    lemmyApiClient.resolveObject(it)
                },
                onFailure = {
                    Result.failure(it)
                },
            )
            .fold(
                onSuccess = {
                    val newPostOrCommentRef = if (it.post != null) {
                        Either.Left(PostRef(currentAccount.instance, it.post.post.id))
                    } else if (it.comment != null) {
                        Either.Right(CommentRef(currentAccount.instance, it.comment.comment.id))
                    } else {
                        null
                    }

                    if (newPostOrCommentRef != null) {
                        updatePostOrCommentRef(newPostOrCommentRef)

                        if (!isNativePost) {
                            comments = null
                            pendingComments = null
                            supplementaryComments.clear()
                            newlyPostedCommentId = null
                            additionalLoadedCommentIds.clear()
                            removedCommentIds.clear()
                        }

                        fetchPostData(fetchPostData = true, force = true)
                            ?.join()
                    }
                },
                onFailure = {
                    Log.e(TAG, "Error resolving object.", it)
                },
            )
    }

    private fun updatePendingComments(
        postOrCommentRef: Either<PostRef, CommentRef>,
        resolveCompletedPendingComments: Boolean,
    ) {
        val sortOrder = requireNotNull(commentsSortOrderLiveData.value).toApiSortOrder()

        viewModelScope.launch {
            updatePendingCommentsInternal(
                postOrCommentRef = postOrCommentRef,
                sortOrder = sortOrder,
                resolveCompletedPendingComments = resolveCompletedPendingComments,
            )

            updateData()
        }
    }

    private suspend fun updatePendingCommentsInternal(
        postOrCommentRef: Either<PostRef, CommentRef>,
        sortOrder: CommentSortType,
        resolveCompletedPendingComments: Boolean,
    ) {
        val postRef = postOrCommentRef.leftOrNull() ?: return

        pendingComments = accountActionsManager.getPendingComments(postRef)

        var modified = false
        if (resolveCompletedPendingComments) {
            pendingComments?.forEach { pendingComment ->
                if (pendingComment.complete) {
                    // Looks like commits on the server is async. Refreshing a comment immediately
                    // after we post it may not get us the latest value.
                    delay(1000)

                    val result = commentsFetcher.fetchCommentsWithRetry(
                        Either.Left(pendingComment.postRef.id),
                        sortOrder,
                        maxDepth = null,
                        force = true,
                    )

                    this@PostViewModel.comments = result.getOrNull()

                    val commentsResult = if (pendingComment.parentId == null) {
                        result
                    } else {
                        fetchMoreCommentsInternal(pendingComment.parentId, sortOrder, force = true)
                    }

                    commentsResult.onSuccess {
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
        val postOrCommentRef = postOrCommentRef

        postData.postValue(
            PostData(
                ListView.PostListView(post),
                CommentTreeBuilder(accountManager).buildCommentsTreeListView(
                    post = post,
                    comments = comments,
                    parentComment = true,
                    pendingComments = pendingComments,
                    supplementaryComments = supplementaryComments,
                    removedCommentIds = removedCommentIds,
                    fullyLoadedCommentIds = fullyLoadedCommentIds,
                    targetCommentRef = postOrCommentRef
                        ?.fold(
                            { null },
                            { it },
                        ),
                ),
                newlyPostedCommentId = newlyPostedCommentId,
                selectedCommentId = postOrCommentRef?.getOrNull()?.id,
                isSingleComment = postOrCommentRef?.getOrNull() != null,
            ),
        )
    }

    fun fetchMoreComments(parentId: CommentId?, depth: Int? = null, force: Boolean = false) {
        val sortOrder = requireNotNull(commentsSortOrderLiveData.value).toApiSortOrder()

        viewModelScope.launch {
            if (parentId != null) {
                fetchMoreCommentsInternal(parentId, sortOrder, depth, force)
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
        depth: Int? = null,
        force: Boolean = false,
    ): Result<List<CommentView>> {
        val result = commentsFetcher.fetchCommentsWithRetry(
            Either.Right(parentId),
            sortOrder,
            maxDepth = depth,
            force,
        )

        additionalLoadedCommentIds.add(parentId)

        result.onSuccess { comments ->
            // A comment is likely removed if we are loading a specific comment and it's direct
            // descendant is missing

            val thisComment = comments.find { it.comment.id == parentId }

            if (comments.isEmpty() || thisComment == null) {
                removedCommentIds.add(parentId)
            } else {
                removedCommentIds.remove(parentId)
            }

            val depthIsMoreThanOne = depth == null || depth > 1
            if (thisComment != null && depthIsMoreThanOne) {
                fullyLoadedCommentIds.add(thisComment.comment.id)
            }

//            val commentChildCount = thisComment?.counts?.child_count ?: 0
//            if (thisComment != null && commentChildCount > 0 && comments.size == 1 && depthIsMoreThanOne) {
//                // This comment's child was deleted...
//                val fakeComment = createFakeDeletedComment(thisComment.comment.path)
//                removedCommentIds.add(fakeComment.comment.id)
//                supplementaryComments[fakeComment.comment.id] = fakeComment
//            }

            comments.forEach {
                supplementaryComments[it.comment.id] = it
            }
        }

        return result
    }

    data class PostData(
        val postView: ListView.PostListView,
        val commentTree: List<CommentNodeData>,
        val newlyPostedCommentId: CommentId?,
        val selectedCommentId: CommentId?,
        val isSingleComment: Boolean,
    )

    sealed interface ListView {
        val id: Long

        companion object {
            private const val POST_FLAG = 0x100000000L
            private const val COMMENT_FLAG = 0x200000000L
            private const val PENDING_COMMENT_FLAG = 0x300000000L
            private const val MORE_COMMENTS_FLAG = 0x400000000L
        }

        data class PostListView(
            val post: PostView,
            override val id: Long = post.post.id.toLong() or POST_FLAG,
        ) : ListView

        data class CommentListView(
            val comment: CommentView,
            val pendingCommentView: PendingCommentView? = null,
            var isRemoved: Boolean = false,
            override val id: Long = comment.comment.id.toLong() or COMMENT_FLAG,
        ) : ListView

        data class PendingCommentListView(
            val pendingCommentView: PendingCommentView,
            var author: String?,
            override val id: Long = pendingCommentView.id or PENDING_COMMENT_FLAG,
        ) : ListView

        data class MoreCommentsItem(
            val parentCommentId: CommentId?,
            val depth: Int,
            val moreCount: Int,
            override val id: Long = (parentCommentId?.toLong() ?: 0L) or MORE_COMMENTS_FLAG,
        ) : ListView

        data class MissingCommentItem(
            val commentId: CommentId,
            val parentCommentId: CommentId?,
            override val id: Long = commentId.toLong() or COMMENT_FLAG,
        ) : ListView
    }

    fun setCommentsSortOrder(sortOrder: CommentsSortOrder) {
        commentsSortOrderLiveData.value = sortOrder
    }

    fun deleteComment(postRef: PostRef, commentId: Int) {
        viewModelScope.launch {
            accountActionsManager.deleteComment(postRef, commentId)
        }
    }

    fun toggleCommentNavControls() {
        if (commentNavControlsState.value == null) {
            commentNavControlsState.value = CommentNavControlsState(
                preferences.commentsNavigationFabOffX,
                preferences.commentsNavigationFabOffY,
            )
        } else {
            commentNavControlsState.value = null
        }
    }

    fun setFindInPageQuery(query: String) {
        viewModelScope.launch {
            findInPageQueryFlow.emit(query)
        }
    }

    override fun onCleared() {
        Log.d(TAG, "onCleared()")
        super.onCleared()
    }

    class ObjectResolverFailedException : Exception()
}
