package com.idunnololz.summit.account.info

import android.content.Context
import android.net.Uri
import com.idunnololz.summit.account.Account
import com.idunnololz.summit.account.AccountImageGenerator
import com.idunnololz.summit.account.AccountManager
import com.idunnololz.summit.account.AccountView
import com.idunnololz.summit.account.asAccount
import com.idunnololz.summit.api.AccountAwareLemmyClient
import com.idunnololz.summit.api.NotAuthenticatedException
import com.idunnololz.summit.api.dto.Community
import com.idunnololz.summit.api.dto.GetSiteResponse
import com.idunnololz.summit.coroutine.CoroutineScopeFactory
import com.idunnololz.summit.lemmy.toCommunityRef
import com.idunnololz.summit.lemmy.toPersonRef
import com.idunnololz.summit.util.StatefulData
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Singleton
class AccountInfoManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val accountManager: AccountManager,
    private val accountAwareLemmyClient: AccountAwareLemmyClient,
    private val coroutineScopeFactory: CoroutineScopeFactory,
    private val accountInfoDao: AccountInfoDao,
    private val accountImageGenerator: AccountImageGenerator,
) {

    private val coroutineScope = coroutineScopeFactory.create()

    private val unreadCountInvalidates = MutableSharedFlow<Unit>()

    val accountDir = File(context.filesDir, "account")

    val subscribedCommunities = MutableStateFlow<List<AccountSubscription>>(listOf())
    val accountInfoUpdateState = MutableStateFlow<StatefulData<Account?>>(StatefulData.NotStarted())
    val unreadCount = MutableStateFlow<UnreadCount>(UnreadCount())
    val currentFullAccount = MutableStateFlow<FullAccount?>(null)
    val currentFullAccountOnChange = currentFullAccount.asSharedFlow().drop(1)

    init {
        accountManager.addOnAccountChangedListener(
            onAccountChangeListener = object : AccountManager.OnAccountChangedListener {
                override suspend fun onAccountSigningOut(account: Account) {
                    accountInfoDao.delete(account.id)

                    accountImageGenerator.getImageForAccount(accountDir, account).let {
                        if (it.exists()) {
                            it.delete()
                        }
                    }

                    unreadCount.emit(UnreadCount())
                }

                override suspend fun onAccountChanged(newAccount: Account?) {
                    // Don't emit null as it will trigger multiple updates
//                    currentFullAccount.emit(null)
                    subscribedCommunities.emit(listOf())
                    accountInfoUpdateState.emit(StatefulData.NotStarted())
                    unreadCount.emit(UnreadCount())
                }
            },
        )

        coroutineScope.launch {
            accountManager.currentAccount.collect {
                accountInfoUpdateState.emit(StatefulData.NotStarted())

                loadAccountInfo(it as? Account)

                refreshAccountInfo(it as? Account)
                updateUnreadCount()
            }
        }

        coroutineScope.launch {
            unreadCountInvalidates.debounce(1000)
                .collect {
                    val account = accountManager.currentAccount.asAccount ?: return@collect
                    updateUnreadCount(account)
                }
        }
    }

    fun refreshAccountInfo() {
        coroutineScope.launch {
            refreshAccountInfo(accountManager.currentAccount.asAccount)
        }
    }

    suspend fun fetchAccountInfo(): Result<GetSiteResponse> =
        refreshAccountInfo(accountManager.currentAccount.asAccount)

    fun updateUnreadCount() {
        coroutineScope.launch {
            unreadCountInvalidates.emit(Unit)
        }
    }

    suspend fun getAccountViewForAccount(account: Account): AccountView {
        val fullAccount = getFullAccount(account)
        return AccountView(
            account = account,
            profileImage = fullAccount?.accountInfo?.miscAccountInfo?.avatar?.let {
                try {
                    Uri.parse(it)
                } catch (e: Exception) {
                    null
                }
            } ?: Uri.fromFile(getImageForAccount(account)),
        )
    }

    private fun getImageForAccount(account: Account): File {
        return accountImageGenerator.getOrGenerateImageForAccount(accountDir, account)
    }

    private suspend fun getFullAccount(account: Account): FullAccount? {
        val fullAccount = currentFullAccount.value
        return if (fullAccount?.accountId == account.id) {
            fullAccount
        } else {
            withContext(Dispatchers.IO) {
                val accountInfo = accountInfoDao.getAccountInfo(account.id)
                    ?: return@withContext null

                FullAccount(
                    account,
                    accountInfo,
                )
            }
        }
    }

    suspend fun updateAccountInfoWith(account: Account, response: GetSiteResponse) {
        val localUserView = response.my_user?.local_user_view
        val accountInfo = AccountInfo(
            accountId = account.id,
            subscriptions = response.my_user
                ?.follows
                ?.map { it.community.toAccountSubscription() }
                ?: listOf(),
            miscAccountInfo = MiscAccountInfo(
                avatar = localUserView?.person?.avatar,
                defaultCommunitySortType = localUserView?.local_user?.default_sort_type,
                showReadPosts = localUserView?.local_user?.show_read_posts,
                modCommunityIds = response.my_user?.moderates?.map { it.community.id },
                isAdmin = response.admins.firstOrNull { it.person.id == account.id } != null,
                blockedPersons = response.my_user?.person_blocks?.map {
                    BlockedPerson(
                        personId = it.target.id,
                        personRef = it.target.toPersonRef(),
                    )
                },
                blockedCommunities = response.my_user?.community_blocks?.map {
                    BlockedCommunity(
                        it.community.id,
                        it.community.toCommunityRef(),
                    )
                },
                blockedInstances = response.my_user?.instance_blocks?.map {
                    BlockedInstance(
                        it.instance.id,
                        it.instance.domain,
                    )
                },
            ),
        )
        val fullAccount = FullAccount(
            account,
            accountInfo,
        )
        currentFullAccount.emit(fullAccount)

//        fullAccount.accountInfo
//            .miscAccountInfo
//            ?.modCommunityIds
//            ?.let {
//                CommunityRef.MultiCommunity(
//                    context.getString(R.string.moderated_communities),
//                    "mod",
//
//                    )
//            }

        accountInfoDao.insert(accountInfo)

        subscribedCommunities.emit(accountInfo.subscriptions ?: listOf())
    }

    private suspend fun updateUnreadCount(account: Account) {
        val j1 = coroutineScope.launch {
            accountAwareLemmyClient.fetchUnreadCountWithRetry(force = true, account)
                .onSuccess {
                    unreadCount.emit(
                        unreadCount.value.copy(
                            lastUpdateTimeMs = System.currentTimeMillis(),
                            account = account,
                            totalUnreadCount = it.mentions + it.private_messages + it.replies,
                        ),
                    )
                }
        }
        val j2 = coroutineScope.launch {
            accountAwareLemmyClient.fetchUnresolvedReportsCountWithRetry(force = true, account)
                .onSuccess {
                    unreadCount.emit(
                        unreadCount.value.copy(
                            lastUpdateTimeMs = System.currentTimeMillis(),
                            account = account,
                            totalUnresolvedReportsCount =
                            it.comment_reports +
                                it.post_reports +
                                (it.private_message_reports ?: 0),
                        ),
                    )
                }
        }

        j1.join()
        j2.join()
    }

    private suspend fun refreshAccountInfo(account: Account?): Result<GetSiteResponse> {
        accountInfoUpdateState.emit(StatefulData.Loading())

        if (account == null) {
            currentFullAccount.emit(null)
            accountInfoUpdateState.emit(StatefulData.Success(null))
            return Result.failure(NotAuthenticatedException())
        }

        val result = withContext(Dispatchers.IO) {
            accountAwareLemmyClient.fetchSiteWithRetry(force = true, account.jwt)
        }

        result
            .onSuccess { response ->
                updateAccountInfoWith(account, response)
            }

        accountInfoUpdateState.emit(StatefulData.Success(account))

        return result
    }

    private suspend fun loadAccountInfo(account: Account?) {
        account ?: return

        val accountInfo = accountInfoDao.getAccountInfo(account.id)
            ?: return

        currentFullAccount.emit(
            FullAccount(
                account,
                accountInfo,
            ),
        )

        subscribedCommunities.emit(accountInfo.subscriptions ?: listOf())
    }

    data class UnreadCount(
        val lastUpdateTimeMs: Long = 0,
        val account: Account? = null,
        val totalUnreadCount: Int = 0,
        val totalUnresolvedReportsCount: Int = 0,
    )

//    private class AccountInfo(
//        val siteView: SiteView,
//        val admins: List<PersonView>,
//        val online: Int,
//        val version: String,
//        val myUser: MyUserInfo?,
//        val allLanguages: List<Language>,
//        val discussionLanguages: List<Int>,
//        val taglines: List<Tagline>?,
//    )
}

private fun Community.toAccountSubscription() = AccountSubscription(
    this.id,
    this.name,
    this.title,
    this.removed,
    this.published,
    this.updated,
    this.deleted,
    this.nsfw,
    this.actor_id,
    this.local,
    this.icon,
    this.banner,
    this.hidden,
    this.posting_restricted_to_mods,
    this.instance_id,
)
