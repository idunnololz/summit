package com.idunnololz.summit.lemmy.person

import android.app.Application
import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import arrow.core.Either
import com.idunnololz.summit.account.AccountView
import com.idunnololz.summit.account.info.AccountInfoManager
import com.idunnololz.summit.api.AccountAwareLemmyClient
import com.idunnololz.summit.api.LemmyApiClient
import com.idunnololz.summit.api.dto.CommentView
import com.idunnololz.summit.api.dto.CommunityModeratorView
import com.idunnololz.summit.api.dto.PersonView
import com.idunnololz.summit.api.dto.PostView
import com.idunnololz.summit.api.dto.SortType
import com.idunnololz.summit.api.utils.instance
import com.idunnololz.summit.lemmy.CommentListEngine
import com.idunnololz.summit.lemmy.CommentRef
import com.idunnololz.summit.lemmy.LocalPostView
import com.idunnololz.summit.lemmy.PersonRef
import com.idunnololz.summit.lemmy.PostRef
import com.idunnololz.summit.lemmy.community.LoadedPostsData
import com.idunnololz.summit.lemmy.community.PostListEngine
import com.idunnololz.summit.lemmy.community.PostLoadError
import com.idunnololz.summit.lemmy.community.SlidingPaneController
import com.idunnololz.summit.lemmy.multicommunity.toFetchedPost
import com.idunnololz.summit.util.StatefulLiveData
import com.idunnololz.summit.util.toErrorMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@HiltViewModel
class PersonTabbedViewModel @Inject constructor(
    private val context: Application,
    private val apiClient: AccountAwareLemmyClient,
    private val apiClientFactory: LemmyApiClient.Factory,
    private val state: SavedStateHandle,
    private val accountInfoManager: AccountInfoManager,
    private val commentListEngineFactory: CommentListEngine.Factory,
    private val postListEngineFactory: PostListEngine.Factory,
) : ViewModel(), SlidingPaneController.PostViewPagerViewModel {

    companion object {
        private const val PAGE_SIZE = 10

        private const val TAG = "PersonTabbedViewModel"
    }

    val personData = StatefulLiveData<PersonDetailsData>()
    val postsState = StatefulLiveData<UpdateInfo>()
    val commentsState = StatefulLiveData<UpdateInfo>()
    val stateLessApiClient = apiClientFactory.create()

    val postListEngine = postListEngineFactory.create(
        infinity = true,
        autoLoadMoreItems = true,
    ).apply {
        setKey("person")
    }
    var commentListEngine = commentListEngineFactory.create()

    var sortType: SortType = SortType.New
        set(value) {
            field = value

            reset()
            fetchPage(pageIndex = 0, isPeronInfoFetch = true, force = true)
        }

    val instance: String
        get() = apiClient.instance

    val currentAccountView = MutableLiveData<AccountView?>()

    override var lastSelectedItem: Either<PostRef, CommentRef>? = null

    private var personRef: PersonRef? = null
    private var fetchingPages = mutableSetOf<Int>()
    private val personIdToPersonName = mutableMapOf<String, String>()

    init {
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
    }

    fun fetchPersonIfNotDone(personRef: PersonRef) {
        if (personData.valueOrNull != null && this.personRef == personRef) return

        this.personRef = personRef
        fetchPage(0, isPeronInfoFetch = true)
    }

    fun fetchPage(pageIndex: Int, isPeronInfoFetch: Boolean = false, force: Boolean = false) {
        if (fetchingPages.contains(pageIndex)) {
            return
        }

        Log.d(TAG, "Fetching page $pageIndex")

        fetchingPages.add(pageIndex)

        val personRef = personRef ?: return

        if (isPeronInfoFetch) {
            personData.setIsLoading()
        }
        postsState.setIsLoading()
        commentsState.setIsLoading()

        viewModelScope.launch {
            if (force) {
                reset()
            }

            val result = when (personRef) {
                is PersonRef.PersonRefByName -> {
                    apiClient.fetchPersonByNameWithRetry(
                        name = personRef.fullName,
                        page = pageIndex.toLemmyPageIndex(),
                        limit = PAGE_SIZE,
                        force = force,
                        sortType = sortType,
                    )
                }
                is PersonRef.PersonRefById -> {
                    val fn = personIdToPersonName[personRef.fullName]

                    if (fn != null) {
                        apiClient.fetchPersonByNameWithRetry(
                            name = fn,
                            page = pageIndex.toLemmyPageIndex(),
                            limit = PAGE_SIZE,
                            force = force,
                            sortType = sortType,
                        )
                    } else {
                        stateLessApiClient.changeInstance(personRef.instance)
                        val r = stateLessApiClient.fetchPerson(
                            personId = personRef.id,
                            force = force,
                            name = null,
                            account = null,
                        )

                        r.fold(
                            {
                                val fullName = PersonRef.PersonRefByName(
                                    it.person_view.person.name,
                                    it.person_view.person.instance,
                                ).fullName

                                personIdToPersonName[personRef.fullName] = fullName

                                apiClient.fetchPersonByNameWithRetry(
                                    name = fullName,
                                    page = pageIndex.toLemmyPageIndex(),
                                    limit = PAGE_SIZE,
                                    force = force,
                                    sortType = sortType,
                                )
                            },
                            {
                                r
                            },
                        )
                    }
                }
            }

            result
                .onSuccess { result ->
                    if (isPeronInfoFetch) {
                        personData.postValue(
                            PersonDetailsData(
                                result.person_view,
                                result.comments,
                                result.posts,
                                result.moderates,
                            ),
                        )
                    }

                    if (postListEngine.hasMore || force) {
                        val posts = result.posts.map {
                            LocalPostView(
                                fetchedPost = it.toFetchedPost(),
                                filterReason = null,
                                isDuplicatePost = false,
                            )
                        }
                        postListEngine.addPage(
                            LoadedPostsData(
                                allPosts = posts,
                                posts = posts,
                                instance = apiClient.instance,
                                pageIndex = pageIndex,
                                dedupingKey = pageIndex.toString(),
                                hasMore = result.posts.size == PAGE_SIZE,
                            ),
                        )
                        postListEngine.createItems()
                    }
                    if (commentListEngine.hasMore || force) {
                        commentListEngine.addComments(
                            comments = result.comments,
                            instance = apiClient.instance,
                            pageIndex = pageIndex,
                            hasMore = result.comments.size == PAGE_SIZE,
                            error = null,
                        )
                    }

                    postsState.postValue(
                        UpdateInfo(
                            isReset = force && pageIndex == 0,
                        ),
                    )
                    commentsState.postValue(
                        UpdateInfo(
                            isReset = force && pageIndex == 0,
                        ),
                    )

                    fetchingPages.remove(pageIndex)
                }
                .onFailure {
                    if (postListEngine.hasMore || force) {
                        postListEngine.addPage(
                            LoadedPostsData(
                                allPosts = listOf(),
                                posts = listOf(),
                                instance = apiClient.instance,
                                pageIndex = pageIndex,
                                dedupingKey = pageIndex.toString(),
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
                    if (commentListEngine.hasMore || force) {
                        commentListEngine.addComments(
                            comments = listOf(),
                            instance = apiClient.instance,
                            pageIndex = pageIndex,
                            hasMore = false,
                            error = it,
                        )
                    }

                    if (isPeronInfoFetch) {
                        personData.postError(it)
                    }
                    postsState.postError(it)
                    commentsState.postError(it)

                    fetchingPages.remove(pageIndex)
                }
        }
    }

    fun fetchNextCommentPage() {
        fetchPage(commentListEngine.nextPage, false)
    }

    fun clearPersonData() {
        personData.setIdle()
    }

    private fun reset() {
        postListEngine.clear()
        commentListEngine.clear()
    }

    data class PersonDetailsData(
        val personView: PersonView,
        val comments: List<CommentView>,
        val posts: List<PostView>,
        val moderates: List<CommunityModeratorView>,
    )

    private fun Int.toLemmyPageIndex() = this + 1 // lemmy pages are 1 indexed

    data class UpdateInfo(
        val isReset: Boolean,
    )
}
