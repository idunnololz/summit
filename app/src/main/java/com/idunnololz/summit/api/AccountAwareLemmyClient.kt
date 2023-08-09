package com.idunnololz.summit.api

import android.util.Log
import arrow.core.Either
import com.idunnololz.summit.account.Account
import com.idunnololz.summit.account.AccountManager
import com.idunnololz.summit.api.dto.CommentId
import com.idunnololz.summit.api.dto.CommentReplyId
import com.idunnololz.summit.api.dto.CommentSortType
import com.idunnololz.summit.api.dto.CommentView
import com.idunnololz.summit.api.dto.CommunityId
import com.idunnololz.summit.api.dto.CommunityView
import com.idunnololz.summit.api.dto.GetCommunityResponse
import com.idunnololz.summit.api.dto.GetPersonDetailsResponse
import com.idunnololz.summit.api.dto.GetRepliesResponse
import com.idunnololz.summit.api.dto.GetSiteResponse
import com.idunnololz.summit.api.dto.GetUnreadCountResponse
import com.idunnololz.summit.api.dto.ListingType
import com.idunnololz.summit.api.dto.PersonId
import com.idunnololz.summit.api.dto.PersonMentionId
import com.idunnololz.summit.api.dto.PersonMentionView
import com.idunnololz.summit.api.dto.PostId
import com.idunnololz.summit.api.dto.PostView
import com.idunnololz.summit.api.dto.PrivateMessageId
import com.idunnololz.summit.api.dto.PrivateMessageView
import com.idunnololz.summit.api.dto.SearchResponse
import com.idunnololz.summit.api.dto.SearchType
import com.idunnololz.summit.api.dto.SortType
import com.idunnololz.summit.coroutine.CoroutineScopeFactory
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

    class Factory @Inject constructor(
        val apiClient: LemmyApiClient,
        private val accountManager: AccountManager,
        private val coroutineScopeFactory: CoroutineScopeFactory,
    ) {
        fun create() = AccountAwareLemmyClient(apiClient, accountManager, coroutineScopeFactory)
    }

    companion object {
        private const val TAG = "AccountAwareLemmyClient"
    }

    private val coroutineScope = coroutineScopeFactory.create()
    private var currentAccount: Account? = null

    init {
        accountManager.addOnAccountChangedListener(
            object : AccountManager.OnAccountChangedListener {
                override suspend fun onAccountChanged(newAccount: Account?) {
                    setAccount(newAccount, accountChanged = true)
                }
            },
        )
        coroutineScope.launch {
            accountManager.currentAccount.collect {
                setAccount(it, accountChanged = false)
            }
        }
    }

    suspend fun fetchSavedPostsWithRetry(
        page: Int,
        limit: Int? = null,
        force: Boolean,
        account: Account? = accountForInstance(),
    ): Result<List<PostView>> =
        if (account != null) {
            retry {
                apiClient.fetchSavedPosts(
                    account = account,
                    limit = limit,
                    page = page,
                    force = force,
                ).autoSignOut(account)
            }
        } else {
            createAccountErrorResult()
        }

    suspend fun fetchSavedCommentsWithRetry(
        page: Int,
        limit: Int? = null,
        force: Boolean,
        account: Account? = accountForInstance(),
    ): Result<List<CommentView>> =
        if (account != null) {
            retry {
                apiClient.fetchSavedComments(
                    account = account,
                    limit = limit,
                    page = page,
                    force = force,
                ).autoSignOut(account)
            }
        } else {
            createAccountErrorResult()
        }

    suspend fun fetchPosts(
        communityIdOrName: Either<Int, String>? = null,
        sortType: SortType? = null,
        listingType: ListingType? = null,
        page: Int,
        limit: Int? = null,
        force: Boolean,
        account: Account? = accountForInstance(),
    ): Result<List<PostView>> {
        return apiClient.fetchPosts(
            account = account,
            communityIdOrName = communityIdOrName,
            sortType = sortType
                ?: if (account == null) {
                    SortType.Active
                } else {
                    SortType.values()[account.defaultSortType]
                },
            listingType = listingType
                ?: if (account == null) {
                    ListingType.All
                } else {
                    ListingType.values()[account.defaultListingType]
                },
            limit = limit,
            page = page,
            force = force,
        ).autoSignOut(account)
    }

    suspend fun fetchPostWithRetry(
        id: Either<PostId, CommentId>,
        force: Boolean,
    ): Result<PostView> = retry {
        apiClient.fetchPost(accountForInstance(), id, force)
    }

    suspend fun markPostAsRead(
        postId: PostId,
        read: Boolean,
        account: Account? = accountForInstance(),
    ): Result<PostView> {
        return if (account != null) {
            apiClient.markPostAsRead(postId, read, account)
                .autoSignOut(account)
        } else {
            createAccountErrorResult()
        }
    }

    suspend fun fetchCommentsWithRetry(
        id: Either<PostId, CommentId>,
        sort: CommentSortType,
        force: Boolean,
        maxDepth: Int? = null,
        account: Account? = accountForInstance(),
    ): Result<List<CommentView>> = retry {
        apiClient.fetchComments(account, id, sort, maxDepth, force)
            .autoSignOut(account)
    }

    suspend fun fetchCommunityWithRetry(
        idOrName: Either<Int, String>,
        force: Boolean,
        account: Account? = accountForInstance(),
    ): Result<GetCommunityResponse> = retry {
        apiClient.getCommunity(account, idOrName, force)
            .autoSignOut(account)
    }

    suspend fun fetchPersonByIdWithRetry(
        personId: PersonId,
        account: Account? = accountForInstance(),
        force: Boolean,
    ): Result<GetPersonDetailsResponse> = retry {
        apiClient.fetchPerson(personId = personId, name = null, account = account, force = force)
            .autoSignOut(account)
    }

    suspend fun fetchPersonByNameWithRetry(
        name: String,
        page: Int,
        limit: Int,
        account: Account? = accountForInstance(),
        force: Boolean,
    ): Result<GetPersonDetailsResponse> = retry {
        apiClient
            .fetchPerson(
                personId = null,
                name = name,
                account = account,
                page = page,
                limit = limit,
                force = force,
            )
            .autoSignOut(account)
    }

    suspend fun search(
        communityId: Int? = null,
        communityName: String? = null,
        sortType: SortType,
        listingType: ListingType,
        searchType: SearchType,
        page: Int? = null,
        query: String,
        limit: Int? = null,
        creatorId: Int? = null,
        account: Account? = accountForInstance(),
        force: Boolean = false,
    ): Result<SearchResponse> =
        apiClient.search(
            account = account,
            communityId = communityId,
            communityName = communityName,
            sortType = sortType,
            listingType = listingType,
            searchType = searchType,
            page = page,
            limit = limit,
            query = query,
            force = force,
        )
            .autoSignOut(account)

    suspend fun fetchCommunitiesWithRetry(
        sortType: SortType,
        listingType: ListingType,
        page: Int = 1,
        limit: Int = 50,
        account: Account? = accountForInstance(),
    ): Result<List<CommunityView>> = retry {
        apiClient.fetchCommunities(account, sortType, listingType, page, limit)
            .autoSignOut(account)
    }

    suspend fun followCommunityWithRetry(
        communityId: Int,
        subscribe: Boolean,
        account: Account? = accountForInstance(),
    ): Result<CommunityView> {
        return if (account != null) {
            apiClient.followCommunityWithRetry(communityId, subscribe, account)
                .autoSignOut(account)
        } else {
            createAccountErrorResult()
        }
    }

    suspend fun login(
        instance: String,
        username: String,
        password: String,
        twoFactorCode: String?,
    ): Result<String?> =
        apiClient.login(instance, username, password, twoFactorCode)

    suspend fun fetchSiteWithRetry(
        force: Boolean,
        auth: String? = currentAccount?.jwt,
    ): Result<GetSiteResponse> =
        apiClient.fetchSiteWithRetry(auth, force)

    suspend fun fetchUnreadCountWithRetry(
        force: Boolean,
        account: Account? = currentAccount,
    ): Result<GetUnreadCountResponse> =
        retry {
            if (account == null) {
                createAccountErrorResult()
            } else {
                apiClient.fetchUnreadCount(force, account)
                    .autoSignOut(account)
            }
        }

    suspend fun deletePost(
        id: PostId,
        account: Account? = accountForInstance(),
    ): Result<PostView> {
        return if (account == null) {
            createAccountErrorResult()
        } else {
            apiClient.deletePost(account, id)
                .autoSignOut(account)
        }
    }

    suspend fun createPost(
        name: String,
        body: String?,
        url: String?,
        isNsfw: Boolean,
        account: Account? = accountForInstance(),
        communityId: CommunityId,
    ): Result<PostView> =
        if (account == null) {
            createAccountErrorResult()
        } else {
            apiClient.createPost(name, body, url, isNsfw, account, communityId)
                .autoSignOut(account)
        }

    suspend fun editPost(
        postId: PostId,
        name: String,
        body: String?,
        url: String?,
        isNsfw: Boolean,
        account: Account? = accountForInstance(),
    ): Result<PostView> =
        if (account == null) {
            createAccountErrorResult()
        } else {
            apiClient.editPost(postId, name, body, url, isNsfw, account)
                .autoSignOut(account)
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
                .autoSignOut(account)
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
                .autoSignOut(account)
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
                .autoSignOut(account)
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
                .autoSignOut(account)
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
                .autoSignOut(account)
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
                .autoSignOut(account)
        }

    suspend fun markReplyAsRead(
        id: CommentReplyId,
        read: Boolean,
        account: Account? = accountForInstance(),
    ): Result<CommentView> =
        if (account == null) {
            createAccountErrorResult()
        } else {
            apiClient.markReplyAsRead(id, read, account)
                .autoSignOut(account)
        }

    suspend fun markMentionAsRead(
        id: PersonMentionId,
        read: Boolean,
        account: Account? = accountForInstance(),
    ): Result<PersonMentionView> =
        if (account == null) {
            createAccountErrorResult()
        } else {
            apiClient.markMentionAsRead(id, read, account)
                .autoSignOut(account)
        }

    suspend fun markPrivateMessageAsRead(
        id: PrivateMessageId,
        read: Boolean,
        account: Account? = accountForInstance(),
    ): Result<PrivateMessageView> =
        if (account == null) {
            createAccountErrorResult()
        } else {
            apiClient.markPrivateMessageAsRead(id, read, account)
                .autoSignOut(account)
        }

    suspend fun markAllAsRead(
        account: Account? = accountForInstance(),
    ): Result<GetRepliesResponse> =
        if (account == null) {
            createAccountErrorResult()
        } else {
            apiClient.markAllAsRead(account)
                .autoSignOut(account)
        }

    suspend fun createPrivateMessage(
        content: String,
        recipient: PersonId,
        account: Account? = accountForInstance(),
    ): Result<PrivateMessageView> =
        if (account == null) {
            createAccountErrorResult()
        } else {
            apiClient.createPrivateMessage(content, recipient, account)
                .autoSignOut(account)
        }

    suspend fun savePost(
        postId: PostId,
        save: Boolean,
        account: Account? = accountForInstance(),
    ): Result<PostView> =
        if (account == null) {
            createAccountErrorResult()
        } else {
            apiClient.savePost(postId, save, account)
                .autoSignOut(account)
        }

    suspend fun saveComment(
        commentId: CommentId,
        save: Boolean,
        account: Account? = accountForInstance(),
    ): Result<CommentView> =
        if (account == null) {
            createAccountErrorResult()
        } else {
            apiClient.saveComment(commentId, save, account)
                .autoSignOut(account)
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
                AccountInstanceMismatchException(currentAccount.instance, instance),
            )
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

    private fun <T> Result<T>.autoSignOut(account: Account?): Result<T> {
        if (account == null) {
            return this
        }

        if (this.isSuccess) {
            return this
        }

        val error = requireNotNull(this.exceptionOrNull())
        if (error is NotAuthenticatedException) {
            if (account.instance == apiClient.instance) {
                Log.d(TAG, "Account token expired. Signing user '${account.name}' out.")
                // sign this account out
                coroutineScope.launch {
                    accountManager.signOut(account)
                }
            }
        }

        return this
    }
}
