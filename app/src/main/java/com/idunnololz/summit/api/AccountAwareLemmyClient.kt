package com.idunnololz.summit.api

import arrow.core.Either
import com.idunnololz.summit.account.Account
import com.idunnololz.summit.account.AccountManager
import com.idunnololz.summit.api.dto.CommentSortType
import com.idunnololz.summit.api.dto.CommentView
import com.idunnololz.summit.api.dto.CommunityView
import com.idunnololz.summit.api.dto.GetSiteResponse
import com.idunnololz.summit.api.dto.ListingType
import com.idunnololz.summit.api.dto.PostView
import com.idunnololz.summit.api.dto.SearchResponse
import com.idunnololz.summit.api.dto.SearchType
import com.idunnololz.summit.api.dto.SortType
import com.idunnololz.summit.coroutine.CoroutineScopeFactory
import com.idunnololz.summit.util.retry
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@Singleton
class AccountAwareLemmyClient @Inject constructor(
    private val apiClient: LemmyApiClient,
    private val accountManager: AccountManager,
    private val coroutineScopeFactory: CoroutineScopeFactory,
) {

    private val coroutineScope = coroutineScopeFactory.create()
    private var currentAccount: Account? = null

    init {
        accountManager.addOnAccountChangedListener(object : AccountManager.OnAccountChangedListener {
            override suspend fun onAccountChanged(newAccount: Account?) {
                setAccount(newAccount, accountChanged = true)
            }
        })
        coroutineScope.launch {
            accountManager.currentAccount.collect {
                setAccount(it, accountChanged = false)
            }
        }
    }

    suspend fun fetchPosts(
        communityIdOrName: Either<Int, String>? = null,
        sortType: SortType? = null,
        listingType: ListingType? = null,
        page: Int,
        limit: Int? = null,
    ): Result<List<PostView>> {
        val currentAccount = accountForInstance()

        return apiClient.fetchPosts(
            account = currentAccount,
            communityIdOrName = communityIdOrName,
            sortType = sortType
                ?: if (currentAccount == null) {
                    SortType.Active
                } else {
                    SortType.values()[currentAccount.defaultSortType]
                },
            listingType = listingType
                ?: if (currentAccount == null) {
                    ListingType.All
                } else {
                    ListingType.values()[currentAccount.defaultListingType]
                },
            limit = limit,
            page = page
        )
    }

    suspend fun fetchPost(
        id: Either<PostId, CommentId>,
        force: Boolean,
    ): Result<PostView> =
        apiClient.fetchPost(accountForInstance(), id, force)

    suspend fun fetchComments(
        id: Either<PostId, CommentId>,
        sort: CommentSortType,
        force: Boolean,
    ): Result<List<CommentView>> =
        apiClient.fetchComments(accountForInstance(), id, sort, force)

    suspend fun getCommunity(
        idOrName: Either<Int, String>,
    ): Result<CommunityView> =
        apiClient.getCommunity(accountForInstance(), idOrName)

    suspend fun search(
        communityId: Int? = null,
        communityName: String? = null,
        sortType: SortType,
        listingType: ListingType,
        searchType: SearchType,
        page: Int? = null,
        query: String,
        creatorId: Int? = null,
    ): Result<SearchResponse> =
        apiClient.search(
            account = accountForInstance(),
            communityId = communityId,
            communityName = communityName,
            sortType = sortType,
            listingType = listingType,
            searchType = searchType,
            page = page,
            query = query
        )

    suspend fun fetchCommunitiesWithRetry(
        sortType: SortType,
        listingType: ListingType,
        page: Int = 1,
        limit: Int = 50,
    ): Result<List<CommunityView>> = retry {
        apiClient.fetchCommunities(accountForInstance(), sortType, listingType, page, limit)
    }

    suspend fun login(
        instance: String,
        username: String,
        password: String,
    ): Result<String?> =
        apiClient.login(instance, username, password)

    suspend fun fetchSiteWithRetry(
        auth: String? = currentAccount?.jwt,
    ): Result<GetSiteResponse> =
        apiClient.fetchSiteWithRetry(auth)

    fun changeInstance(site: String) =
        apiClient.changeInstance(site)

    fun defaultInstance() {
        val currentAccount = currentAccount
        if (currentAccount == null) {
            apiClient.defaultInstance()
        } else {
            apiClient.changeInstance(currentAccount.instance)
        }
    }

    val instance: String
        get() = apiClient.instance

    /**
     * @return [Account] if the current account matches this instance. Null otherwise.
     */
    private fun accountForInstance(): Account? {
        val instance = instance
        val currentAccount = currentAccount ?: return null

        return if (currentAccount.instance == instance) {
            currentAccount
        } else {
            null
        }
    }

    private fun setAccount(account: Account?, accountChanged: Boolean) {
        if (account == currentAccount) {
            return
        }

        if (accountChanged) {
            // on all account changes, clear the cache
            apiClient.clearCache()
        }

        if (account == null) {
            apiClient.defaultInstance()
        } else {
            apiClient.changeInstance(account.instance)
        }
        currentAccount = account
    }
}
