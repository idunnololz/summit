package com.idunnololz.summit.saved

import android.app.Application
import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import arrow.core.Either
import com.idunnololz.summit.account.AccountManager
import com.idunnololz.summit.account.AccountView
import com.idunnololz.summit.account.info.AccountInfoManager
import com.idunnololz.summit.actions.SavedManager
import com.idunnololz.summit.api.AccountAwareLemmyClient
import com.idunnololz.summit.api.dto.CommentView
import com.idunnololz.summit.api.dto.PostView
import com.idunnololz.summit.coroutine.CoroutineScopeFactory
import com.idunnololz.summit.lemmy.CommentListEngine
import com.idunnololz.summit.lemmy.CommentRef
import com.idunnololz.summit.lemmy.LocalPostView
import com.idunnololz.summit.lemmy.PostRef
import com.idunnololz.summit.lemmy.community.LoadedPostsData
import com.idunnololz.summit.lemmy.community.PostListEngine
import com.idunnololz.summit.lemmy.community.PostLoadError
import com.idunnololz.summit.lemmy.community.SlidingPaneController
import com.idunnololz.summit.lemmy.utils.actions.SaveCommentResult
import com.idunnololz.summit.lemmy.utils.actions.SavePostResult
import com.idunnololz.summit.util.DirectoryHelper
import com.idunnololz.summit.util.StatefulLiveData
import com.idunnololz.summit.util.toErrorMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class SavedViewModel @Inject constructor(
    private val context: Application,
    private val apiClient: AccountAwareLemmyClient,
    private val accountManager: AccountManager,
    private val accountInfoManager: AccountInfoManager,
    private val state: SavedStateHandle,
    private val coroutineScopeFactory: CoroutineScopeFactory,
    private val directoryHelper: DirectoryHelper,
    private val savedManager: SavedManager,
    private val commentListEngineFactory: CommentListEngine.Factory,
) : ViewModel(), SlidingPaneController.PostViewPagerViewModel {

    companion object {
        private const val TAG = "SavedViewModel"

        private const val PAGE_SIZE = 20
    }

    val currentAccountView = MutableLiveData<AccountView?>()

    val postsState = StatefulLiveData<Unit>()
    val commentsState = StatefulLiveData<Unit>()

    val postListEngine = PostListEngine(
        infinity = true,
        autoLoadMoreItems = true,
        coroutineScopeFactory = coroutineScopeFactory,
        directoryHelper = directoryHelper,
    )
    var commentListEngine = commentListEngineFactory.create()

    val instance: String
        get() = apiClient.instance

    private var fetchingPostPages = mutableSetOf<Int>()
    private var fetchingCommentPages = mutableSetOf<Int>()

    override var lastSelectedItem: Either<PostRef, CommentRef>? = null
        set(value) {
            field = value

            lastSelectedItemLiveData.postValue(value)
        }

    val lastSelectedItemLiveData = MutableLiveData<Either<PostRef, CommentRef>?>()

    init {
        fetchPostPage(0, false)
        fetchCommentPage(0, false)

        viewModelScope.launch {
            accountManager.currentAccountOnChange.collect {
                delay(10) // just in case it takes a second for the api client to update...

                fetchingPostPages.clear()
                fetchingCommentPages.clear()

                postListEngine.clearPages()
                commentListEngine.clear()

                fetchPostPage(0, false)
                fetchCommentPage(0, false)
            }
        }

        viewModelScope.launch {
            accountInfoManager.currentFullAccount.collect {
                withContext(Dispatchers.Main) {
                    if (it != null) {
                        currentAccountView.value = accountInfoManager.getAccountViewForAccount(it.account)
                    } else {
                        currentAccountView.value = null
                    }
                }
            }
        }

        viewModelScope.launch {
            savedManager.onPostSaveChange.collect {
                if (it == SavedManager.SavedState.Changed) {
                    Log.d(TAG, "onPostSaveChange() - changed")

                    savedManager.resetPostSaveState()

                    fetchingPostPages.clear()

                    postListEngine.clearPages()

                    fetchPostPage(0, true)
                }
            }
        }

        viewModelScope.launch {
            savedManager.onCommentSaveChange.collect {
                if (it == SavedManager.SavedState.Changed) {
                    Log.d(TAG, "onCommentSaveChange() - changed")

                    savedManager.resetCommentSaveState()

                    fetchingCommentPages.clear()

                    commentListEngine.clear()

                    fetchCommentPage(0, true)
                }
            }
        }
    }

    fun fetchPostPage(pageIndex: Int, force: Boolean) {
        if (fetchingPostPages.contains(pageIndex)) {
            return
        }

        Log.d(TAG, "Fetching page $pageIndex")

        fetchingPostPages.add(pageIndex)

        viewModelScope.launch(Dispatchers.Default) {
            apiClient.fetchSavedPostsWithRetry(pageIndex.toLemmyPageIndex(), PAGE_SIZE, force)
                .onSuccess {
                    if (postListEngine.hasMore || force) {
                        val posts = it.map {
                            LocalPostView(it, null)
                        }
                        postListEngine.addPage(
                            LoadedPostsData(
                                allPosts = posts,
                                posts = posts,
                                instance = apiClient.instance,
                                pageIndex = pageIndex,
                                hasMore = it.size == PAGE_SIZE,
                            ),
                        )
                        postListEngine.createItems()
                    }

                    postsState.postValue(Unit)

                    fetchingPostPages.remove(pageIndex)
                }
                .onFailure {
                    if (postListEngine.hasMore || force) {
                        postListEngine.addPage(
                            LoadedPostsData(
                                allPosts = listOf(),
                                posts = listOf(),
                                instance = apiClient.instance,
                                pageIndex = pageIndex,
                                hasMore = false,
                                error = PostLoadError(
                                    errorCode = 0,
                                    errorMessage = it.toErrorMessage(context),
                                    isRetryable = true,
                                    isLoading = false,
                                ),
                            ),
                        )
                        postListEngine.createItems()
                    }

                    postsState.postError(it)

                    fetchingPostPages.remove(pageIndex)
                }
        }
    }

    fun fetchCommentPage(pageIndex: Int, force: Boolean = false) {
        if (fetchingCommentPages.contains(pageIndex)) {
            return
        }

        Log.d(TAG, "Fetching page $pageIndex")

        fetchingCommentPages.add(pageIndex)

        viewModelScope.launch(Dispatchers.Default) {
            apiClient.fetchSavedCommentsWithRetry(pageIndex.toLemmyPageIndex(), PAGE_SIZE, force)
                .onSuccess {
                    if (commentListEngine.hasMore || force) {
                        commentListEngine.addComments(
                            comments = it,
                            instance = apiClient.instance,
                            pageIndex = pageIndex,
                            hasMore = it.size == PAGE_SIZE,
                            error = null,
                        )
                    }

                    commentsState.postValue(Unit)

                    withContext(Dispatchers.Main) {
                        fetchingCommentPages.remove(pageIndex)
                    }
                }
                .onFailure {
                    if (commentListEngine.hasMore || force) {
                        commentListEngine.addComments(
                            comments = listOf(),
                            instance = apiClient.instance,
                            pageIndex = pageIndex,
                            hasMore = false,
                            error = it,
                        )
                    }

                    commentsState.postError(it)

                    withContext(Dispatchers.Main) {
                        fetchingCommentPages.remove(pageIndex)
                    }
                }
        }
    }

    private fun Int.toLemmyPageIndex() =
        this + 1 // lemmy pages are 1 indexed

    fun onSavePostChanged(savePostResult: SavePostResult) {
        if (!savePostResult.save) {
            postListEngine.removePost(savePostResult.postId)
            postListEngine.createItems()
            postsState.postValue(Unit)
        }
    }

    fun onSaveCommentChanged(saveCommentResult: SaveCommentResult) {
        if (!saveCommentResult.save) {
            commentListEngine.removeComment(saveCommentResult.commentId)
            commentsState.postValue(Unit)
        }
    }
}
