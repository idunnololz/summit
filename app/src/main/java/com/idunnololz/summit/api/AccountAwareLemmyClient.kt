package com.idunnololz.summit.api

import arrow.core.Either
import com.idunnololz.summit.account.Account
import com.idunnololz.summit.account.AccountManager
import com.idunnololz.summit.api.dto.CommentId
import com.idunnololz.summit.api.dto.CommentReplyView
import com.idunnololz.summit.api.dto.CommentSortType
import com.idunnololz.summit.api.dto.CommentView
import com.idunnololz.summit.api.dto.CommunityId
import com.idunnololz.summit.api.dto.CommunityView
import com.idunnololz.summit.api.dto.GetPersonDetailsResponse
import com.idunnololz.summit.api.dto.GetPersonMentions
import com.idunnololz.summit.api.dto.GetPrivateMessages
import com.idunnololz.summit.api.dto.GetReplies
import com.idunnololz.summit.api.dto.GetSiteResponse
import com.idunnololz.summit.api.dto.ListingType
import com.idunnololz.summit.api.dto.PersonId
import com.idunnololz.summit.api.dto.PersonMentionView
import com.idunnololz.summit.api.dto.PostId
import com.idunnololz.summit.api.dto.PostView
import com.idunnololz.summit.api.dto.PrivateMessageView
import com.idunnololz.summit.api.dto.SearchResponse
import com.idunnololz.summit.api.dto.SearchType
import com.idunnololz.summit.api.dto.SortType
import com.idunnololz.summit.coroutine.CoroutineScopeFactory
import com.idunnololz.summit.util.Utils.serializeToMap
import com.idunnololz.summit.util.retry
import kotlinx.coroutines.launch
import java.io.InputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AccountAwareLemmyClient @Inject constructor(
    val apiClient: LemmyApiClient,
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
        force: Boolean,
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
            page = page,
            force = force,
        )
    }

    suspend fun fetchPostWithRetry(
        id: Either<PostId, CommentId>,
        force: Boolean,
    ): Result<PostView> = retry {
        apiClient.fetchPost(accountForInstance(), id, force)
    }

    suspend fun fetchCommentsWithRetry(
        id: Either<PostId, CommentId>,
        sort: CommentSortType,
        force: Boolean,
    ): Result<List<CommentView>> = retry {
        apiClient.fetchComments(accountForInstance(), id, sort, force)
    }

    suspend fun fetchCommunityWithRetry(
        idOrName: Either<Int, String>,
        force: Boolean,
    ): Result<CommunityView> = retry {
        apiClient.getCommunity(accountForInstance(), idOrName, force)
    }

    suspend fun fetchPersonByIdWithRetry(
        personId: PersonId,
        account: Account? = accountForInstance(),
    ): Result<GetPersonDetailsResponse> = retry {
        apiClient.fetchPerson(personId = personId, name = null, account = account)
    }

    suspend fun fetchPersonByNameWithRetry(
        name: String,
        account: Account? = accountForInstance(),
    ): Result<GetPersonDetailsResponse> = retry {
        apiClient.fetchPerson(personId = null, name = name, account = account)
    }

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

    suspend fun followCommunityWithRetry(
        communityId: Int,
        subscribe: Boolean,
        account: Account? = accountForInstance(),
    ): Result<CommunityView> {
        return if (account != null) {
            apiClient.followCommunityWithRetry(communityId, subscribe, account)
        } else {
            createAccountErrorResult()
        }
    }

    suspend fun login(
        instance: String,
        username: String,
        password: String,
    ): Result<String?> =
        apiClient.login(instance, username, password)

    suspend fun fetchSiteWithRetry(
        force: Boolean,
        auth: String? = currentAccount?.jwt,
    ): Result<GetSiteResponse> =
        apiClient.fetchSiteWithRetry(auth, force)

    suspend fun deletePost(
        id: PostId,
        account: Account? = accountForInstance(),
    ): Result<PostView> {
        return if (account == null) {
            createAccountErrorResult()
        } else {
            apiClient.deletePost(account, id)
        }
    }

    suspend fun uploadImage(
        fileName: String,
        imageIs: InputStream,
        account: Account? = accountForInstance(),
    ): Result<UploadImageResult> =
        if (account == null) {
            createAccountErrorResult()
        } else {
            apiClient.uploadImage(account, fileName, imageIs)
        }

    suspend fun blockCommunity(
        communityId: CommunityId,
        block: Boolean,
        account: Account? = accountForInstance(),
    ) =
        if (account == null) {
            createAccountErrorResult()
        } else {
            apiClient.blockCommunity(communityId, block, account)
        }

    suspend fun blockPerson(
        personId: PersonId,
        block: Boolean,
        account: Account? = accountForInstance(),
    ) =
        if (account == null) {
            createAccountErrorResult()
        } else {
            apiClient.blockPerson(personId, block, account)
        }

    suspend fun fetchReplies(
        sort: CommentSortType? /* "Hot" | "Top" | "New" | "Old" */ = null,
        page: Int? = 0,
        limit: Int? = 20,
        unreadOnly: Boolean? = null,
        account: Account? = accountForInstance(),
        force: Boolean,
    ) =
        if (account == null) {
            createAccountErrorResult()
        } else {
            apiClient.fetchReplies(sort, page, limit, unreadOnly, account, force)
        }

    suspend fun fetchMentions(
        sort: CommentSortType? /* "Hot" | "Top" | "New" | "Old" */ = null,
        page: Int? = 0,
        limit: Int? = 20,
        unreadOnly: Boolean? = null,
        account: Account? = accountForInstance(),
        force: Boolean,
    ) =
        if (account == null) {
            createAccountErrorResult()
        } else {
            apiClient.fetchMentions(sort, page, limit, unreadOnly, account, force)
        }

    suspend fun fetchPrivateMessages(
        unreadOnly: Boolean? = null,
        page: Int? = 0,
        limit: Int? = 20,
        account: Account? = accountForInstance(),
        force: Boolean,
    ) =
        if (account == null) {
            createAccountErrorResult()
        } else {
            apiClient.fetchPrivateMessages(unreadOnly, page, limit, account, force)
        }


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

    private fun <T> createAccountErrorResult(): Result<T> {
        val currentAccount = currentAccount
        if (currentAccount != null) {
            return Result.failure(
                AccountInstanceMismatchException(currentAccount.instance, instance))
        }

        return Result.failure(NotAuthenticatedException())
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
