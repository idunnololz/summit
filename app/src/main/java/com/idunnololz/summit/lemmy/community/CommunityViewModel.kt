package com.idunnololz.summit.lemmy.community

import android.os.Parcelable
import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.idunnololz.summit.account.Account
import com.idunnololz.summit.account.AccountActionsManager
import com.idunnololz.summit.account.AccountManager
import com.idunnololz.summit.account.AccountView
import com.idunnololz.summit.account.info.AccountInfoManager
import com.idunnololz.summit.actions.PostReadManager
import com.idunnololz.summit.api.AccountAwareLemmyClient
import com.idunnololz.summit.api.dto.PostId
import com.idunnololz.summit.api.dto.PostView
import com.idunnololz.summit.coroutine.CoroutineScopeFactory
import com.idunnololz.summit.hidePosts.HiddenPostsManager
import com.idunnololz.summit.lemmy.CommunityRef
import com.idunnololz.summit.lemmy.CommunitySortOrder
import com.idunnololz.summit.lemmy.CommunityState
import com.idunnololz.summit.lemmy.CommunityViewState
import com.idunnololz.summit.lemmy.PostRef
import com.idunnololz.summit.lemmy.RecentCommunityManager
import com.idunnololz.summit.lemmy.PostsRepository
import com.idunnololz.summit.lemmy.toUrl
import com.idunnololz.summit.offline.OfflineManager
import com.idunnololz.summit.preferences.Preferences
import com.idunnololz.summit.user.UserCommunitiesManager
import com.idunnololz.summit.util.StatefulLiveData
import com.idunnololz.summit.util.assertMainThread
import com.squareup.moshi.JsonClass
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.parcelize.Parcelize
import java.lang.RuntimeException
import javax.inject.Inject

@HiltViewModel
class CommunityViewModel @Inject constructor(
    private val postsRepository: PostsRepository,
    private val recentCommunityManager: RecentCommunityManager,
    private val accountManager: AccountManager,
    private val userCommunitiesManager: UserCommunitiesManager,
    private val accountInfoManager: AccountInfoManager,
    private val accountActionsManager: AccountActionsManager,
    private val apiClient: AccountAwareLemmyClient,
    private val postReadManager: PostReadManager,
    private val preferences: Preferences,
    private val state: SavedStateHandle,
    private val coroutineScopeFactory: CoroutineScopeFactory,
    private val offlineManager: OfflineManager,
    private val hiddenPostsManager: HiddenPostsManager,
) : ViewModel(), ViewPagerController.PostViewPagerViewModel {

    companion object {
        private val TAG = CommunityViewModel::class.java.canonicalName
    }

    @Parcelize
    @JsonClass(generateAdapter = true)
    class PageScrollState(
        var isAtBottom: Boolean = false,
        var itemIndex: Int = 0,
        var offset: Int = 0
    ) : Parcelable

    // Dont use data class so every change triggers the observers
    class PostUpdateInfo(
        val isReadPostUpdate: Boolean = false,
    )

    private var pagePositions = arrayListOf<PageScrollState>()

    val currentCommunityRef = MutableLiveData<CommunityRef>(CommunityRef.All())
    val currentPageIndex = MutableLiveData(0)
    private val communityRefChangeObserver = Observer<CommunityRef> {
        recentCommunityManager.addRecentCommunity(it)
    }
    val loadedPostsData = StatefulLiveData<PostUpdateInfo>()

    val communityInstance: String
        get() = postsRepository.communityInstance
    val apiInstance: String
        get() = postsRepository.apiInstance

    override var lastSelectedPost: PostRef? = null

    override val viewPagerAdapter = ViewPagerController.ViewPagerAdapter()

    val defaultCommunity = MutableLiveData<CommunityRef>(null)
    val currentAccount = MutableLiveData<AccountView?>(null)

    private var isHideReadEnabled = state.getLiveData<Boolean>("_isHideReadEnabled", false)

    private var fetchingPages = mutableSetOf<Int>()
    var postListEngine = PostListEngine(preferences.infinity, coroutineScopeFactory, offlineManager)

    val infinity: Boolean
        get() = postListEngine.infinity

    private var hiddenPostObserverJob: Job? = null

    init {
        isHideReadEnabled.value?.let {
            postsRepository.hideRead = it
        }

        currentCommunityRef.observeForever(communityRefChangeObserver)

        accountManager.addOnAccountChangedListener(object : AccountManager.OnAccountChangedListener {
            override suspend fun onAccountChanged(newAccount: Account?) {
                postsRepository.reset()
            }
        })

        viewModelScope.launch {
            accountInfoManager.currentFullAccountOnChange
                .collect {
                    withContext(Dispatchers.Main) {
                        reset()
                    }
                }
        }

        viewModelScope.launch {
            userCommunitiesManager.defaultCommunity
                .collect {
                    defaultCommunity.postValue(it)
                }
        }

        viewModelScope.launch {
            accountManager.currentAccount.collect {
                val accountView = if (it != null) {
                    accountInfoManager.getAccountViewForAccount(it)
                } else {
                    null
                }

                currentAccount.postValue(accountView)
            }
        }

        viewModelScope.launch {
            accountInfoManager.currentFullAccount.collect {
                if (it != null) {
                    val accountView = accountInfoManager.getAccountViewForAccount(it.account)

                    currentAccount.postValue(accountView)
                }
            }
        }

        viewModelScope.launch {
            accountInfoManager.currentFullAccountOnChange.collect {
                withContext(Dispatchers.Main) {
                    postListEngine.clearPages()
                    postListEngine.createItems()
                    pagePositions.clear()

                    loadedPostsData.setValue(PostUpdateInfo())
                }
                fetchInitialPage()
            }
        }

        viewModelScope.launch {
            postReadManager.postReadChanged.collect {
                val pagesCopy = withContext(Dispatchers.Main) {
                    ArrayList(postListEngine.pages)
                }

                val updatedPages = withContext(Dispatchers.Default) {
                    pagesCopy.map {
                        it.copy(
                            posts = postsRepository.update(it.posts),
                            isReadPostUpdate = false,
                        )
                    }
                }

                updatedPages.forEach {
                    postListEngine.addPage(it)
                }
                postListEngine.createItems()

                withContext(Dispatchers.Main) {
                    loadedPostsData.setValue(PostUpdateInfo(isReadPostUpdate = true))
                }
            }
        }
    }

    fun updateInfinity() {
        // check for inconsistency
        if (postListEngine.infinity != preferences.infinity) {
            postListEngine.infinity = preferences.infinity
        }
    }

    fun fetchPrevPage(force: Boolean = false) {
        val pageIndex = requireNotNull(currentPageIndex.value)
        if (pageIndex == 0) {
            return
        }

        if (!postListEngine.infinity) {
            currentPageIndex.value = pageIndex - 1
        }
        fetchPageInternal(pageIndex - 1, force)
    }

    fun fetchNextPage(force: Boolean = false, clearPagePosition: Boolean) {
        val pageIndex = requireNotNull(currentPageIndex.value)

        if (clearPagePosition) {
            pagePositions = ArrayList(pagePositions.take(pageIndex + 1))
        }

        if (!postListEngine.infinity) {
            currentPageIndex.value = pageIndex + 1
        }

        fetchPageInternal(pageIndex + 1, force)
    }

    fun fetchPage(pageIndex: Int, force: Boolean = false) {
        if (!postListEngine.infinity) {
            currentPageIndex.value = pageIndex
        }
        fetchPageInternal(pageIndex, force = force)
    }

    fun fetchInitialPage() {
        if (postListEngine.infinity) {
            fetchPage(0)
        } else {
            fetchCurrentPage()
        }
    }

    fun fetchCurrentPage(force: Boolean = false, resetHideRead: Boolean = false) {
        if (resetHideRead) {
            isHideReadEnabled.value = false
            postsRepository.clearHideRead()
        }

        val pages = if (postListEngine.infinity) {
            postListEngine.getCurrentPages()
        } else {
            listOf(requireNotNull(currentPageIndex.value))
        }
        if (pages.isEmpty()) {
            if (postListEngine.infinity) {
                fetchPage(0, force = true)
            } else {
                fetchCurrentPage()
            }
            return
        }
        pages.forEach {
            fetchPageInternal(it, force)
        }
    }

    private fun fetchPageInternal(pageToFetch: Int, force: Boolean) {
        if (fetchingPages.contains(pageToFetch)) {
            return
        }

        fetchingPages.add(pageToFetch)

        loadedPostsData.setIsLoading()

        viewModelScope.launch {
            val result = postsRepository.getPage(
                pageToFetch, force)

            result
                .onSuccess {
                    val pageData =
                        LoadedPostsData(
                            posts = it.posts,
                            instance = it.instance,
                            pageIndex = pageToFetch,
                            hasMore = it.hasMore,
                        )
                    postListEngine.addPage(pageData)
                    postListEngine.createItems()

                    loadedPostsData.postValue(PostUpdateInfo())

                    withContext(Dispatchers.Main) {
                        fetchingPages.remove(pageToFetch)
                    }
                }
                .onFailure {
                    postListEngine.addPage(
                        LoadedPostsData(
                            posts = listOf(),
                            instance = postsRepository.apiInstance,
                            pageIndex = pageToFetch,
                            hasMore = true,
                            error = PostLoadError(0),
                        )
                    )
                    postListEngine.createItems()
                    loadedPostsData.postError(it)

                    withContext(Dispatchers.Main) {
                        fetchingPages.remove(pageToFetch)
                    }
                }
        }
    }

    fun changeCommunity(communityRef: CommunityRef?) {
        if (communityRef == null) {
            return
        }

        if (communityRef is CommunityRef.Subscribed && accountManager.currentAccount.value == null) {
            return
        }

        val communityRefSafe: CommunityRef = communityRef

        FirebaseCrashlytics.getInstance().apply {
            setCustomKey("community", communityRef.toString())
            setCustomKey("view_type", preferences.getPostsLayout().name)
            setCustomKey("prefer_full_size_images", preferences.getPostInListUiConfig().preferFullSizeImages)
        }

        currentCommunityRef.value = communityRefSafe
        postsRepository.setCommunity(communityRef)
        postListEngine.setSecondaryKey(communityRef.getKey())

        postListEngine.tryRestore()

        hiddenPostObserverJob?.cancel()
        hiddenPostObserverJob = viewModelScope.launch {
            Log.d(TAG, "Hidden posts changed. Refreshing!")
            hiddenPostsManager.getOnHiddenPostsChangeFlow(apiInstance).collect {
                val hiddenPosts = hiddenPostsManager.getHiddenPostEntries(apiInstance)
                postsRepository.onHiddenPostsChange()

                val pagesCopy = withContext(Dispatchers.Main) {
                    ArrayList(postListEngine.pages)
                }

                val updatedPages = withContext(Dispatchers.Default) {
                    pagesCopy.map {
                        it.copy(
                            posts = it.posts.filter { !hiddenPosts.contains(it.post.id) },
                            isReadPostUpdate = false,
                        )
                    }
                }

                updatedPages.forEach {
                    postListEngine.addPage(it)
                }
                postListEngine.createItems()

                withContext(Dispatchers.Main) {
                    loadedPostsData.setValue(PostUpdateInfo())
                }
            }
        }
    }

    fun createState(): CommunityViewState? {
        return CommunityViewState(
            CommunityState(
                communityRef = currentCommunityRef.value ?: return null,
                pages = listOf(),
                currentPageIndex = if (infinity) {
                    postListEngine.biggestPageIndex
                } else {
                    currentPageIndex.value
                } ?: return null,
            ),
            pagePositions
        )
    }

    fun restoreFromState(state: CommunityViewState?) {
        state ?: return
        currentCommunityRef.value = state.communityState.communityRef
        currentPageIndex.value = state.communityState.currentPageIndex
        pagePositions = ArrayList(state.pageScrollStates)
    }

    fun getSharedLinkForCurrentPage(): String? =
        createState()?.toUrl()

    fun setPagePositionAtTop(pageIndex: Int) {
        getPagePosition(pageIndex).apply {
            isAtBottom = false
            itemIndex = 0
            offset = 0
        }
    }

    fun setPagePositionAtBottom(pageIndex: Int) {
        getPagePosition(pageIndex).isAtBottom = true
    }

    fun setPagePosition(pageIndex: Int, itemIndex: Int = 0, offset: Int) {
        getPagePosition(pageIndex).apply {
            isAtBottom = false
            this.itemIndex = itemIndex
            this.offset = offset
        }
    }

    fun getPagePosition(pageIndex: Int): PageScrollState {
        if (pageIndex < 0) throw RuntimeException("getPagePosition(): Index was -1!")
        if (pageIndex >= pagePositions.size) {
            for (i in 0 until pageIndex + 1 - pagePositions.size) {
                pagePositions.add(PageScrollState())
            }
        }
        return pagePositions[pageIndex]
    }

    fun setSortOrder(newSortOrder: CommunitySortOrder) {
        postsRepository.sortOrder = newSortOrder

        reset()
    }

    fun getCurrentSortOrder(): CommunitySortOrder = postsRepository.sortOrder

    fun setDefaultPage(currentCommunityRef: CommunityRef) {
        viewModelScope.launch {
            userCommunitiesManager.setDefaultPage(currentCommunityRef)
        }
    }

    fun resetToAccountInstance() {
        val account = accountManager.currentAccount.value ?: return
        changeCommunity(CommunityRef.All(account.instance))

        reset()
    }

    fun onBlockSettingsChanged() {
        viewModelScope.launch {
            postsRepository.resetCacheForCommunity()
            fetchCurrentPage()
        }
    }

    override fun onCleared() {
        super.onCleared()

        currentCommunityRef.removeObserver(communityRefChangeObserver)
    }

    private fun reset() {
        assertMainThread()

        postListEngine.clearPages()
        postListEngine.addPage(LoadedPostsData(
            posts = listOf(),
            instance = postsRepository.apiInstance,
            pageIndex = 0,
            hasMore = false
        ))
        loadedPostsData.setValue(PostUpdateInfo())
        currentPageIndex.value = 0
        setPagePositionAtTop(0)
        fetchCurrentPage()
    }

    fun onPostRead(postView: PostView, delayMs: Long = 0) {
        if (postView.read) {
            return
        }

        viewModelScope.launch {
            if (delayMs > 0) {
                delay(delayMs)
            }
            accountActionsManager.markPostAsRead(apiInstance, postView.post.id, read = true)
        }
    }

    fun onHideRead(anchors: Set<Int>) {
        isHideReadEnabled.value = true
        loadedPostsData.setIsLoading()

        val anchorPosts = if (anchors.isEmpty()) {
            postListEngine.getPostsCloseBy()
                .mapTo(mutableSetOf<Int>()) { it.post.id }
        } else {
            anchors
        } ?: setOf()

        viewModelScope.launch {
            postsRepository.hideReadPosts(anchorPosts, postListEngine.biggestPageIndex ?: 0)
                .onSuccess {
                    val position = it.posts.indexOfFirst { anchorPosts.contains(it.post.id) }

                    if (position != -1) {
                        setPagePosition(it.pageIndex, position, 0)
                    } else {
                        setPagePositionAtTop(it.pageIndex)
                    }

                    if (infinity) {
                        postListEngine.clearPages()
                        for (index in 0..it.pageIndex) {
                            fetchPageInternal(index, force = false)
                        }
                    } else {
                        fetchPageInternal(it.pageIndex, force = false)
                    }
                }
                .onFailure {
                    loadedPostsData.postError(it)
                }
        }
    }

    fun setTag(tag: String?) {
        postListEngine.setKey(tag)
    }

    fun hidePost(id: PostId) {
        hiddenPostsManager.hidePost(id, apiInstance)
    }
}
