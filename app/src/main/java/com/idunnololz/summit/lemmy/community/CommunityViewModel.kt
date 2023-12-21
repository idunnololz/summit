package com.idunnololz.summit.lemmy.community

import android.app.Application
import android.os.Parcelable
import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import arrow.core.Either
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
import com.idunnololz.summit.lemmy.PostsRepository
import com.idunnololz.summit.lemmy.RecentCommunityManager
import com.idunnololz.summit.lemmy.toUrl
import com.idunnololz.summit.preferences.PreferenceManager
import com.idunnololz.summit.preferences.Preferences
import com.idunnololz.summit.tabs.TabsManager
import com.idunnololz.summit.user.UserCommunitiesManager
import com.idunnololz.summit.util.DirectoryHelper
import com.idunnololz.summit.util.StatefulLiveData
import com.idunnololz.summit.util.assertMainThread
import com.idunnololz.summit.util.toErrorMessage
import com.squareup.moshi.JsonClass
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.parcelize.Parcelize
import javax.inject.Inject
import kotlin.RuntimeException

@HiltViewModel
class CommunityViewModel @Inject constructor(
    private val context: Application,
    private val postsRepository: PostsRepository,
    private val recentCommunityManager: RecentCommunityManager,
    private val accountManager: AccountManager,
    private val userCommunitiesManager: UserCommunitiesManager,
    private val accountInfoManager: AccountInfoManager,
    private val accountActionsManager: AccountActionsManager,
    private val postReadManager: PostReadManager,
    private val preferenceManager: PreferenceManager,
    private val state: SavedStateHandle,
    private val coroutineScopeFactory: CoroutineScopeFactory,
    private val directoryHelper: DirectoryHelper,
    private val hiddenPostsManager: HiddenPostsManager,
    private val tabsManager: TabsManager,
    private val apiClient: AccountAwareLemmyClient,
    val basePreferences: Preferences,
) : ViewModel(), ViewPagerController.PostViewPagerViewModel {

    companion object {
        private val TAG = CommunityViewModel::class.java.canonicalName
    }

    @Parcelize
    @JsonClass(generateAdapter = true)
    class PageScrollState(
        var isAtBottom: Boolean = false,
        var itemIndex: Int = 0,
        var offset: Int = 0,
    ) : Parcelable

    // Don't use data class so every change triggers the observers
    class PostUpdateInfo(
        val isReadPostUpdate: Boolean = false,
        val scrollToTop: Boolean = false,
    )

    private var pagePositions = arrayListOf<PageScrollState>()

    val currentCommunityRef = MutableLiveData<CommunityRef?>(null)
    val currentPageIndex = MutableLiveData(0)
    private val communityRefChangeObserver = Observer<CommunityRef?> {
        it ?: return@Observer

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

    var preferences: Preferences = preferenceManager.currentPreferences

    private var fetchingPages = mutableSetOf<Int>()
    var postListEngine = PostListEngine(
        coroutineScopeFactory = coroutineScopeFactory,
        directoryHelper = directoryHelper,
        infinity = preferences.infinity,
        autoLoadMoreItems = preferences.autoLoadMorePosts,
        usePageIndicators = preferences.infinityPageIndicator,
    )

    val infinity: Boolean
        get() = postListEngine.infinity

    val sortOrder = postsRepository.sortOrderFlow.asLiveData()

    var lockBottomBar: Boolean = false

    private var hiddenPostObserverJob: Job? = null
    private var fetchPageJob: Job? = null

    init {

        isHideReadEnabled.value?.let {
            postsRepository.hideRead = it
        }
        postsRepository.showNsfwPosts = preferences.showNsfwPosts

        currentCommunityRef.observeForever(communityRefChangeObserver)

        accountManager.addOnAccountChangedListener(
            object : AccountManager.OnAccountChangedListener {
                override suspend fun onAccountChanged(newAccount: Account?) {
                    fetchPageJob?.cancel()
                    fetchingPages.clear()

                    postsRepository.onAccountChanged()

                    onCommunityOrInstanceChange()

                    preferences = preferenceManager.getComposedPreferencesForAccount(newAccount)

                    withContext(Dispatchers.Main) {
                        recheckPreferences()
                    }
                }
            },
        )

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
            accountManager.currentAccountOnChange.collect {
                withContext(Dispatchers.Main) {
                    postListEngine.clearPages()
                    postListEngine.createItems()
                    pagePositions.clear()

                    loadedPostsData.setValue(PostUpdateInfo())
                }
                fetchInitialPage(force = true, clearPagesOnSuccess = true, scrollToTop = true)
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

                postListEngine.setPersistentErrors(postsRepository.persistentErrors)
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

    fun updatePreferences() {
        // check for inconsistency
        if (postListEngine.infinity != preferences.infinity) {
            postListEngine.infinity = preferences.infinity
        }
        if (postListEngine.usePageIndicators != preferences.infinityPageIndicator) {
            postListEngine.usePageIndicators = preferences.infinityPageIndicator
        }
        if (postListEngine.autoLoadMoreItems != preferences.autoLoadMorePosts) {
            postListEngine.autoLoadMoreItems = preferences.autoLoadMorePosts
        }
        lockBottomBar = preferences.lockBottomBar
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

    fun fetchPage(
        pageIndex: Int,
        force: Boolean = false,
        clearPagesOnSuccess: Boolean = false,
        scrollToTop: Boolean = false,
    ) {
        if (!postListEngine.infinity) {
            currentPageIndex.value = pageIndex
        }
        fetchPageInternal(pageIndex, force = force, clearPagesOnSuccess, scrollToTop)
    }

    fun fetchInitialPage(
        force: Boolean = false,
        clearPagesOnSuccess: Boolean = false,
        scrollToTop: Boolean = false,
    ) {
        if (postListEngine.infinity) {
            fetchPage(0, force, clearPagesOnSuccess, scrollToTop)
        } else {
            fetchCurrentPage(force, clearPagesOnSuccess)
        }
    }

    fun fetchCurrentPage(
        force: Boolean = false,
        resetHideRead: Boolean = false,
        clearPages: Boolean = false,
        scrollToTop: Boolean = false,
    ) {
        if (resetHideRead) {
            isHideReadEnabled.value = false
            postsRepository.clearHideRead()
        }

        val pages = if (postListEngine.infinity) {
            if (clearPages) {
                listOf()
            } else {
                postListEngine.getCurrentPages()
            }
        } else {
            listOf(requireNotNull(currentPageIndex.value))
        }

        if (clearPages) {
            postsRepository.reset()
        }

        if (pages.isEmpty()) {
            if (postListEngine.infinity) {
                fetchPage(
                    pageIndex = 0,
                    force = true,
                    clearPagesOnSuccess = clearPages,
                    scrollToTop = scrollToTop,
                )
            } else {
                fetchCurrentPage()
            }
            return
        }
        pages.forEach {
            fetchPageInternal(it, force, scrollToTop = scrollToTop)
        }
    }

    private fun fetchPageInternal(
        pageToFetch: Int,
        force: Boolean,
        clearPagesOnSuccess: Boolean = false,
        scrollToTop: Boolean = false,
    ) {
        if (fetchingPages.contains(pageToFetch)) {
            return
        }

        fetchingPages.add(pageToFetch)

        loadedPostsData.setIsLoading()

        fetchPageJob = viewModelScope.launch {
            val result = postsRepository.getPage(
                pageToFetch,
                force,
            )

            result
                .onSuccess {
                    val pageData =
                        LoadedPostsData(
                            posts = it.posts,
                            instance = it.instance,
                            pageIndex = pageToFetch,
                            hasMore = it.hasMore,
                        )
                    if (clearPagesOnSuccess) {
                        postListEngine.clearPages()
                    }
                    postListEngine.setPersistentErrors(postsRepository.persistentErrors)
                    postListEngine.addPage(pageData)
                    postListEngine.createItems()

                    loadedPostsData.postValue(PostUpdateInfo(scrollToTop = scrollToTop))

                    withContext(Dispatchers.Main) {
                        fetchingPages.remove(pageToFetch)
                    }
                }
                .onFailure {
                    postListEngine.setPersistentErrors(postsRepository.persistentErrors)
                    postListEngine.addPage(
                        LoadedPostsData(
                            posts = listOf(),
                            instance = postsRepository.apiInstance,
                            pageIndex = pageToFetch,
                            hasMore = true,
                            error = PostLoadError(
                                errorCode = 0,
                                errorMessage = it.toErrorMessage(context),
                                isRetryable = true,
                                isLoading = false,
                            ),
                        ),
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

        val currentAccount = accountManager.currentAccount.value
        if (communityRef is CommunityRef.Subscribed && currentAccount == null) {
            return
        }

        val communityRefSafe: CommunityRef = communityRef
        val instance = when (communityRefSafe) {
            is CommunityRef.All -> communityRefSafe.instance
            is CommunityRef.CommunityRefByName -> communityRefSafe.instance
            is CommunityRef.Local -> communityRefSafe.instance
            is CommunityRef.ModeratedCommunities -> communityRefSafe.instance
            is CommunityRef.MultiCommunity -> currentAccount?.instance
            is CommunityRef.Subscribed -> communityRefSafe.instance
        }
        val instanceChange = instance != null && instance != apiInstance

        if (currentCommunityRef.value == communityRefSafe && !instanceChange) {
            return
        }

        FirebaseCrashlytics.getInstance().apply {
            setCustomKey("community", communityRef.toString())
            setCustomKey("view_type", preferences.getPostsLayout().name)
            setCustomKey("prefer_full_size_images", preferences.getPostInListUiConfig().preferFullSizeImages)
        }

        currentCommunityRef.value = communityRefSafe
        postsRepository.setCommunity(communityRef)
        postListEngine.setSecondaryKey(communityRef.getKey())

        postListEngine.tryRestore()

        onCommunityOrInstanceChange()
    }

    private fun onCommunityOrInstanceChange() {
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

                postListEngine.setPersistentErrors(postsRepository.persistentErrors)
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
            pagePositions,
        )
    }

    fun restoreFromState(state: CommunityViewState?) {
        state ?: return
        currentCommunityRef.value = state.communityState.communityRef
        currentPageIndex.value = state.communityState.currentPageIndex
        pagePositions = ArrayList(state.pageScrollStates)
    }

    fun getSharedLinkForCurrentPage(): String? =
        createState()?.toUrl(apiInstance)

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

        fetchCurrentPage(
            force = true,
            resetHideRead = true,
            clearPages = true,
            scrollToTop = true,
        )
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

        reset(resetScrollPosition = true)
    }

    fun onBlockSettingsChanged() {
        viewModelScope.launch {
            postsRepository.resetCacheForCommunity()
            fetchCurrentPage()
        }
    }

    fun updatePost(postId: PostId) {
        viewModelScope.launch {
            apiClient.fetchPostWithRetry(Either.Left(postId), true)
                .onSuccess {
                    postListEngine.updatePost(it)
                    postListEngine.createItems()
                    loadedPostsData.postValue(PostUpdateInfo())
                }
                .onFailure {
                    // do nothing...
                }
        }
    }

    override fun onCleared() {
        super.onCleared()

        currentCommunityRef.removeObserver(communityRefChangeObserver)
    }

    private fun reset(
        resetScrollPosition: Boolean = false,
        resetHideRead: Boolean = false,
        force: Boolean = false,
    ) {
        assertMainThread()

        postListEngine.clearPages()
        postListEngine.setPersistentErrors(postsRepository.persistentErrors)
        postListEngine.addPage(
            LoadedPostsData(
                posts = listOf(),
                instance = postsRepository.apiInstance,
                pageIndex = 0,
                hasMore = false,
            ),
        )
        loadedPostsData.setValue(PostUpdateInfo())
        currentPageIndex.value = 0
        setPagePositionAtTop(0)
        fetchCurrentPage(
            resetHideRead = resetHideRead,
            scrollToTop = resetScrollPosition,
            force = force,
        )
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

        updateStateMaintainingPosition(
            {
                this.hideRead = true
            },
            anchors,
        )
    }

    fun setTag(tag: String?) {
        postListEngine.setKey(tag)
    }

    fun hidePost(id: PostId) {
        hiddenPostsManager.hidePost(id, apiInstance)
    }

    fun recheckPreferences() {
        updatePreferencesState()
    }

    private fun updatePreferencesState() {
        if (postsRepository.showLinkPosts == preferences.showLinkPosts &&
            postsRepository.showImagePosts == preferences.showImagePosts &&
            postsRepository.showVideoPosts == preferences.showVideoPosts &&
            postsRepository.showTextPosts == preferences.showTextPosts &&
            postsRepository.showNsfwPosts == preferences.showNsfwPosts
        ) {
            return
        }

        updateStateMaintainingPosition(
            {
                this.showLinkPosts = preferences.showLinkPosts
                this.showImagePosts = preferences.showImagePosts
                this.showVideoPosts = preferences.showVideoPosts
                this.showTextPosts = preferences.showTextPosts
                this.showNsfwPosts = preferences.showNsfwPosts
            },
            null,
        )
    }

    private fun updateStateMaintainingPosition(
        changeState: PostsRepository.() -> Unit,
        anchors: Set<Int>?,
    ) {
        loadedPostsData.setIsLoading()

        val anchorPosts = if (anchors.isNullOrEmpty()) {
            postListEngine.getPostsCloseBy()
                .mapTo(mutableSetOf<Int>()) { it.post.id }
        } else {
            anchors
        }

        viewModelScope.launch {
            postsRepository
                .updateStateMaintainingPosition(
                    changeState,
                    anchorPosts,
                    postListEngine.biggestPageIndex ?: 0,
                )
                .onSuccess {
                    val position = it.posts.indexOfFirst { anchorPosts.contains(it.post.id) }
                    var scrollToTop = false

                    if (position != -1) {
                        setPagePosition(it.pageIndex, position, 0)
                    } else {
                        setPagePositionAtTop(it.pageIndex)
                        scrollToTop = true
                    }

                    if (infinity) {
                        postListEngine.clearPages()
                        for (index in 0..it.pageIndex) {
                            fetchPageInternal(index, force = false, scrollToTop = scrollToTop)
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

    fun updateTab(tab: TabsManager.Tab, communityRef: CommunityRef?) {
        communityRef ?: return

        tabsManager.updateTabState(tab, communityRef)
    }
}
