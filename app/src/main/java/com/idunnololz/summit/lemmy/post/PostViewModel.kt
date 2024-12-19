package com.idunnololz.summit.lemmy.post

import android.app.Application
import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import arrow.core.Either
import com.idunnololz.summit.R
import com.idunnololz.summit.account.Account
import com.idunnololz.summit.account.AccountActionsManager
import com.idunnololz.summit.account.AccountManager
import com.idunnololz.summit.account.AccountView
import com.idunnololz.summit.account.asAccount
import com.idunnololz.summit.account.info.AccountInfoManager
import com.idunnololz.summit.actions.PendingCommentView
import com.idunnololz.summit.actions.PostReadManager
import com.idunnololz.summit.api.AccountAwareLemmyClient
import com.idunnololz.summit.api.ClientApiException
import com.idunnololz.summit.api.CommentsFetcher
import com.idunnololz.summit.api.LemmyApiClient
import com.idunnololz.summit.api.LemmyApiClient.Companion.DEFAULT_INSTANCE
import com.idunnololz.summit.api.dto.CommentId
import com.idunnololz.summit.api.dto.CommentSortType
import com.idunnololz.summit.api.dto.CommentView
import com.idunnololz.summit.api.dto.PostView
import com.idunnololz.summit.filterLists.ContentFiltersManager
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
import com.idunnololz.summit.util.StatefulData
import com.idunnololz.summit.util.StatefulLiveData
import com.idunnololz.summit.util.dateStringToTs
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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
    private val contentFiltersManager: ContentFiltersManager,
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

    var originalPostOrCommentRef: Either<PostRef, CommentRef>? = null
        set(value) {
            field = value

            state["originalPostRef"] = value?.leftOrNull()
            state["originalCommentRef"] = value?.getOrNull()
        }
    var postOrCommentRef: Either<PostRef, CommentRef>? = null
        set(value) {
            field = value

            state["postRef"] = value?.leftOrNull()
            state["commentRef"] = value?.getOrNull()
        }
    var currentAccountIdOverride: Long? = null
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

    private val commentsFetcher = CommentsFetcher(lemmyApiClient)

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
        state.get<PostRef>("originalPostRef")?.let {
            postOrCommentRef = Either.Left(it)
        }
        state.get<CommentRef>("originalCommentRef")?.let {
            postOrCommentRef = Either.Right(it)
        }

        commentsSortOrderLiveData.observeForever {
            fetchPostData(fetchPostData = false)
        }
        viewModelScope.launch(Dispatchers.Default) {
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
        viewModelScope.launch(Dispatchers.Default) {
            accountManager.currentAccount.collect {
                val account = it as? Account

                if (currentAccountIdOverride == null) {
                    onAccountChanged(account)
                }
            }
        }
        viewModelScope.launch(Dispatchers.Default) {
            findInPageQueryFlow
                .debounce(300)
                .collect {
                    findInPageQuery.postValue(it)
                }
        }
    }

    private suspend fun onAccountChanged(account: Account?) {
        withContext(Dispatchers.Main) {
            preferences = preferenceManager.currentPreferences

            if (account != null) {
                currentAccountView.value = accountInfoManager.getAccountViewForAccount(account)
            } else {
                currentAccountView.value = null
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

    fun forceAccount(accountId: Long) {
        currentAccountIdOverride = accountId

        lemmyApiClient.forceUseAccount(accountId)

        viewModelScope.launch {
            onAccountChanged(accountManager.getAccountById(accountId))
        }
    }

    fun updateOriginalPostOrCommentRef(postOrCommentRef: Either<PostRef, CommentRef>) {
        originalPostOrCommentRef = postOrCommentRef
    }

    fun updatePostOrCommentRef(postOrCommentRef: Either<PostRef, CommentRef>) {
        this.postOrCommentRef = postOrCommentRef

        onPostOrCommentRefChange.postValue(postOrCommentRef)
    }

    fun switchToNativeInstance() {
        val currentInstance = lemmyApiClient.instance
        val nativeInstance = currentAccountView.value?.account?.instance
            ?: return

        switchAccountState.observeForever(
            object : Observer<StatefulData<Unit>> {
                override fun onChanged(value: StatefulData<Unit>) {
                    when (value) {
                        is StatefulData.Error -> {
                            switchAccountState.removeObserver(this)
                            lemmyApiClient.changeInstance(currentInstance)
                        }
                        is StatefulData.Loading -> {}
                        is StatefulData.NotStarted -> {}
                        is StatefulData.Success -> {
                            switchAccountState.removeObserver(this)
                        }
                    }
                }
            },
        )

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

        return viewModelScope.launch(Dispatchers.Default) {
            if (switchToNativeInstance) {
                switchAccountState.postIsLoading(context.getString(R.string.switching_instance))

                val result = translatePostToCurrentInstance()

                result
                    .onSuccess {
                        switchAccountState.postIdle()
                    }
                    .onFailure {
                        switchAccountState.postError(it)
                    }
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

            this@PostViewModel.postView = if (force) {
                postResult.getOrNull()
            } else {
                postResult.getOrNull()
                    ?: this@PostViewModel.postView
            }

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
            val newComments = commentsResult.getOrNull()
            this@PostViewModel.comments = newComments

            invalidateSupplementaryComments(newComments)

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
                // see if we can recover gracefully

                val postView = postView
                if (postView != null && comments != null && !force) {
                    // lets recover!
                    updateData()
                } else {
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
                }
            } else {
                updateData()
            }
        }
    }

    fun switchAccount(account: Account?) {
        val postOrCommentRef = postOrCommentRef ?: return

        val instance = postOrCommentRef.fold(
            { it.instance },
            { it.instance },
        )
        val newInstance = account?.instance ?: DEFAULT_INSTANCE
        val didInstanceChange = instance != newInstance

        if (account?.id == currentAccountView.value?.account?.id) {
            return
        }

        switchAccountState.setIsLoading(context.getString(R.string.switching_instance))

        Log.d(TAG, "Instance changed. Trying to resolve post in new instance.")

        unauthedApiClient.changeInstance(instance)

        viewModelScope.launch(Dispatchers.Default) {
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
                            Either.Left(PostRef(newInstance, it.post.post.id))
                        } else if (it.comment != null) {
                            Either.Right(CommentRef(newInstance, it.comment.comment.id))
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

                            withContext(Dispatchers.Main) {
                                fetchPostData(fetchPostData = true, force = true)
                            }?.join()
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

    private suspend fun translatePostToCurrentInstance(): Result<Unit> {
        val postOrCommentRef = postOrCommentRef ?: return Result.success(Unit)
        val currentAccount = currentAccountView.value?.account ?: return Result.success(Unit)

        val instance = postOrCommentRef.fold(
            { it.instance },
            { it.instance },
        )
        val isNativePost = instance == apiInstance

        if (isNativePost) return Result.success(Unit)

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

        return linkToResolve
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

                        withContext(Dispatchers.Main) {
                            fetchPostData(fetchPostData = true, force = true)
                        }?.join()
                    }

                    Result.success(Unit)
                },
                onFailure = {
                    Log.e(TAG, "Error resolving object.", it)
                    Result.failure(it)
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
            val completedPendingComments = pendingComments?.filter { it.complete } ?: listOf()
            val anyPendingCommentComplete = completedPendingComments.isNotEmpty()

            if (anyPendingCommentComplete) {
                var result: Result<List<CommentView>>? = null

                for (i in 0 until 10) {
                    // Looks like commits on the server is async. Refreshing a comment immediately
                    // after we post it may not get us the latest value.

                    result = commentsFetcher.fetchCommentsWithRetry(
                        Either.Left(postRef.id),
                        sortOrder,
                        maxDepth = null,
                        force = true,
                    )

                    val oldComments = this@PostViewModel.comments
                    val newComments = result.getOrNull()

                    var allCommentsUpdated = true // tracks if all comments are updated on the server

                    if (oldComments != null && newComments != null) {
                        for (completedPendingComment in completedPendingComments) {
                            val commentId = completedPendingComment.commentId
                            if (commentId != null) {
                                val oldComment = oldComments
                                    .firstOrNull { it.comment.id == commentId }
                                    ?: continue
                                val newComment = newComments
                                    .firstOrNull { it.comment.id == commentId }
                                    ?: continue

                                if (oldComment.comment.updated == newComment.comment.updated) {
                                    Log.d(
                                        TAG,
                                        "1 completed pending comment was not " +
                                            "updated on the server.",
                                    )
                                    allCommentsUpdated = false
                                } else {
                                    Log.d(
                                        TAG,
                                        "1 completed pending comment was updated on the " +
                                            "server. New content: '${newComment.comment.content}'",
                                    )
                                }
                            }
                        }
                    }

                    // If a user sends a new comment, we update that comment as a supplementary
                    // comment. If we then update that comment, our new comment data can be
                    // overridden by supplementary comments. So invalidate those...
                    invalidateSupplementaryComments(newComments)

                    if (allCommentsUpdated) {
                        delay(600)
                        break
                    }
                }

                requireNotNull(result)

                this@PostViewModel.comments = result.getOrNull()

                completedPendingComments.forEach { pendingComment ->
                    val commentsResult = if (pendingComment.parentId == null) {
                        result
                    } else {
                        fetchMoreCommentsInternal(pendingComment.parentId, sortOrder, force = true)
                    }

                    commentsResult.onSuccess {
                        modified = true

                        // find the comment that was recently posted by guessing!

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
                                    it.comment.creator_id ==
                                        accountManager.currentAccount.asAccount?.id
                                }
                                ?.comment?.id
                        }

                        accountActionsManager.removePendingComment(pendingComment)
                    }
                }

                withContext(Dispatchers.Main) {
                    fetchPostData(
                        fetchPostData = true,
                        fetchCommentData = false,
                        force = true,
                    )
                }
            }
        }

        if (modified) {
            pendingComments = accountActionsManager.getPendingComments(postRef)
        }
    }

    private fun invalidateSupplementaryComments(newComments: List<CommentView>?) {
        newComments ?: return

        for (comment in newComments) {
            supplementaryComments.remove(comment.comment.id)
        }
    }

    private suspend fun updateData() {
        Log.d(TAG, "updateData()")

        val post = postView ?: return
        val comments = comments
        val pendingComments = pendingComments
        val supplementaryComments = supplementaryComments
        val postOrCommentRef = postOrCommentRef
        val commentRef: CommentRef? = postOrCommentRef?.getOrNull()
        val originalPostOrCommentRef = originalPostOrCommentRef

        val postDataValue = PostData(
            postView = ListView.PostListView(post),
            commentTree = CommentTreeBuilder(
                accountManager,
                contentFiltersManager,
            ).buildCommentsTreeListView(
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
            selectedCommentId = originalPostOrCommentRef?.getOrNull()?.id
                ?: commentRef?.id,
            isSingleComment = commentRef != null,
            isNativePost = isNativePost(),
            accountInstance = currentAccountView.value?.account?.instance,
            isCommentsLoaded = comments != null,
            commentPath = comments?.firstOrNull()?.comment?.path,
        )

        postData.postValue(postDataValue)
    }

    fun isNativePost(): Boolean {
        val postOrCommentRef = postOrCommentRef ?: return true
        val currentAccount = currentAccountView.value ?: return true

        val instance = postOrCommentRef.fold(
            { it.instance },
            { it.instance },
        )
        return instance == currentAccount.account.instance
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
        Log.d(TAG, "fetchMoreCommentsInternal(): parentId = $parentId")
        val result = commentsFetcher.fetchCommentsWithRetry(
            Either.Right(parentId),
            sortOrder,
            maxDepth = depth,
            force,
        )

        additionalLoadedCommentIds.add(parentId)

        result
            .onSuccess { comments ->
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
            .onFailure {
                if (it is ClientApiException && it.errorCode == 404) {
                    // comment has been removed...
                    removedCommentIds.add(parentId)
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
        val isNativePost: Boolean,
        val accountInstance: String?,
        val isCommentsLoaded: Boolean,
        val commentPath: String?,
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

        sealed interface CommentListView : ListView {
            val comment: CommentView
            val pendingCommentView: PendingCommentView?
            var isRemoved: Boolean
        }

        data class VisibleCommentListView(
            override val comment: CommentView,
            override val pendingCommentView: PendingCommentView? = null,
            override var isRemoved: Boolean = false,
            override val id: Long = comment.comment.id.toLong() or COMMENT_FLAG,
        ) : CommentListView

        data class FilteredCommentItem(
            override val comment: CommentView,
            override val pendingCommentView: PendingCommentView? = null,
            override var isRemoved: Boolean = false,
            override val id: Long = comment.comment.id.toLong() or COMMENT_FLAG,
            var show: Boolean = false,
        ) : CommentListView

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

    fun deleteComment(accountId: Long?, postRef: PostRef, commentId: Int) {
        viewModelScope.launch {
            accountActionsManager.deleteComment(postRef, commentId, accountId)
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

    fun updatePostViewIfNeeded(post: PostView?) {
        post ?: return
        postView = post
    }

    fun fetchCommentPath(instance: String, commentPath: String) {
        val commentIds = commentPath.split(".").map { it.toIntOrNull() }
        val topCommentId = if (commentIds.size > 1) {
            commentIds[1]
        } else {
            return
        } ?: return

        this.postOrCommentRef = Either.Right(CommentRef(instance, topCommentId))

        onPostOrCommentRefChange.postValue(postOrCommentRef)
        fetchPostData()
    }

    class ObjectResolverFailedException : Exception()
}
