package com.idunnololz.summit.lemmy.community

import android.os.Parcelable
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.idunnololz.summit.account.Account
import com.idunnololz.summit.account.AccountActionsManager
import com.idunnololz.summit.account.AccountManager
import com.idunnololz.summit.account.AccountView
import com.idunnololz.summit.account.info.AccountInfoManager
import com.idunnololz.summit.actions.PostReadManager
import com.idunnololz.summit.api.AccountAwareLemmyClient
import com.idunnololz.summit.api.dto.PostId
import com.idunnololz.summit.api.dto.PostView
import com.idunnololz.summit.lemmy.CommunityRef
import com.idunnololz.summit.lemmy.CommunitySortOrder
import com.idunnololz.summit.lemmy.CommunityState
import com.idunnololz.summit.lemmy.CommunityViewState
import com.idunnololz.summit.lemmy.PostRef
import com.idunnololz.summit.lemmy.RecentCommunityManager
import com.idunnololz.summit.lemmy.PostsRepository
import com.idunnololz.summit.lemmy.toUrl
import com.idunnololz.summit.user.UserCommunitiesManager
import com.idunnololz.summit.util.StatefulLiveData
import com.idunnololz.summit.util.assertMainThread
import com.squareup.moshi.JsonClass
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
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
) : ViewModel() {

    companion object {
        private val TAG = CommunityViewModel::class.java.canonicalName
    }

    data class LoadedPostsData(
        val posts: List<PostView>,
        val instance: String,
        val pageIndex: Int,
        val hasMore: Boolean,
        val isReadPostUpdate: Boolean = true,
    )

    @Parcelize
    @JsonClass(generateAdapter = true)
    class PageScrollState(
        var isAtBottom: Boolean = false,
        var itemIndex: Int = 0,
        var offset: Int = 0
    ) : Parcelable

    private var pagePositions = arrayListOf<PageScrollState>()

    val currentCommunityRef = MutableLiveData<CommunityRef>(CommunityRef.All())
    val currentPageIndex = MutableLiveData(0)
    private val communityRefChangeObserver = Observer<CommunityRef> {
        recentCommunityManager.addRecentCommunity(it)
    }
    val loadedPostsData = StatefulLiveData<LoadedPostsData>()

    val instance: String
        get() = postsRepository.instance

    var lastSelectedPost: PostRef? = null

    val viewPagerAdapter = ViewPagerController.ViewPagerAdapter()

    val defaultCommunity = MutableLiveData<CommunityRef>(null)
    val currentAccount = MutableLiveData<AccountView?>(null)

    var isHideReadEnabled = false

    init {
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

                    isHideReadEnabled = !(it.accountInfo.miscAccountInfo?.showReadPosts ?: true)

                    currentAccount.postValue(accountView)
                }
            }
        }

        viewModelScope.launch {
            postReadManager.postReadChanged.collect {
                val postData = loadedPostsData.valueOrNull ?: return@collect

                val updatedPostData = withContext(Dispatchers.Default) {
                    postData.copy(
                        posts = postsRepository.update(postData.posts),
                        isReadPostUpdate = false,
                    )
                }

                withContext(Dispatchers.Main) {
                    loadedPostsData.setValue(updatedPostData)
                }
            }
        }
    }

    fun fetchPrevPage(force: Boolean = false) {
        val pageIndex = requireNotNull(currentPageIndex.value)
        if (pageIndex == 0) {
            return
        }

        currentPageIndex.value = pageIndex - 1
        fetchCurrentPageInternal(force)
    }

    fun fetchNextPage(force: Boolean = false, clearPagePosition: Boolean) {
        val pageIndex = requireNotNull(currentPageIndex.value)

        if (clearPagePosition) {
            pagePositions = ArrayList(pagePositions.take(pageIndex + 1))
        }

        currentPageIndex.value = pageIndex + 1

        fetchCurrentPageInternal(force)
    }

    fun fetchPage(pageIndex: Int) {
        currentPageIndex.value = pageIndex
        fetchCurrentPageInternal(force = false)
    }

    fun fetchCurrentPage(force: Boolean = false) {
        fetchCurrentPageInternal(force)
    }

    private fun fetchCurrentPageInternal(force: Boolean) {
        loadedPostsData.setIsLoading()

        val currentPage = requireNotNull(currentPageIndex.value)
        viewModelScope.launch {
            val result = postsRepository.getPage(
                currentPage, force)

            result
                .onSuccess {
                    loadedPostsData.postValue(
                        LoadedPostsData(
                            posts = it.posts,
                            instance = it.instance,
                            pageIndex = currentPage,
                            hasMore = it.hasMore,
                        )
                    )
                }
                .onFailure {
                    loadedPostsData.postError(it)
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

        currentCommunityRef.value = communityRefSafe
        postsRepository.setCommunity(communityRef)
    }

    fun createState(): CommunityViewState? {
        return CommunityViewState(
            CommunityState(
                communityRef = currentCommunityRef.value ?: return null,
                pages = listOf(),
                currentPageIndex = currentPageIndex.value ?: return null,
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

        loadedPostsData.setValue(LoadedPostsData(
            posts = listOf(),
            instance = postsRepository.instance,
            pageIndex = 0,
            hasMore = false
        ))
        currentPageIndex.value = 0
        setPagePositionAtTop(0)
        fetchCurrentPage()
    }

    fun onPostRead(postView: PostView) {
        if (postView.read) {
            return
        }

        viewModelScope.launch {
            accountActionsManager.markPostAsRead(instance, postView.post.id, read = true)
        }
    }

    fun onHideRead() {
        val loadedPostData = loadedPostsData.valueOrNull
        loadedPostsData.setIsLoading()

        viewModelScope.launch {
            val anchorPosts = mutableListOf<PostId>()
            loadedPostData?.posts
                ?.filter { !it.read }
                ?.mapTo(anchorPosts) { it.post.id }

            postsRepository.hideReadPosts(anchorPosts, currentPageIndex.value ?: 0)
                .onSuccess {
                    fetchPage(it.pageIndex)
//                    setPagePositionAtTop(it.pageIndex)
                }
                .onFailure {
                    loadedPostsData.postError(it)
                }
        }
    }
}
