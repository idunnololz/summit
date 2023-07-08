package com.idunnololz.summit.account.info

import android.content.Context
import android.net.Uri
import com.idunnololz.summit.account.Account
import com.idunnololz.summit.account.AccountImageGenerator
import com.idunnololz.summit.account.AccountManager
import com.idunnololz.summit.account.AccountView
import com.idunnololz.summit.api.AccountAwareLemmyClient
import com.idunnololz.summit.api.dto.Community
import com.idunnololz.summit.coroutine.CoroutineScopeFactory
import com.idunnololz.summit.util.StatefulData
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

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

    private var currentAccountInfo: AccountInfo? = null

    private val unreadCountInvalidates = MutableSharedFlow<Unit>()

    val accountDir = File(context.filesDir, "account")

    val subscribedCommunities = MutableStateFlow<List<AccountSubscription>>(listOf())
    val accountInfoUpdateState = MutableStateFlow<StatefulData<Unit>>(StatefulData.NotStarted())
    val unreadCount = MutableStateFlow<UnreadCount>(UnreadCount(0))

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

                    unreadCount.emit(UnreadCount(0))
                }

                override suspend fun onAccountChanged(newAccount: Account?) {
                    currentAccountInfo = null
                    subscribedCommunities.emit(listOf())
                    accountInfoUpdateState.emit(StatefulData.NotStarted())
                    unreadCount.emit(UnreadCount(0))
                }
            }
        )

        coroutineScope.launch {
            accountManager.currentAccount.collect {
                accountInfoUpdateState.emit(StatefulData.NotStarted())

                loadAccountInfo(it)

                updateAccountInfo(it)
                updateUnreadCount()
            }
        }

        coroutineScope.launch {
            unreadCountInvalidates.debounce(1000)
                .collect {
                    val account = accountManager.currentAccount.value ?: return@collect
                    updateUnreadCount(account)
                }
        }
    }

    fun fetchAccountInfo() {
        coroutineScope.launch {
            updateAccountInfo(accountManager.currentAccount.value)
        }
    }

    fun updateUnreadCount() {
        coroutineScope.launch {
            unreadCountInvalidates.emit(Unit)
        }
    }

    fun getCurrentAccountView(): AccountView? =
        accountManager.currentAccount.value?.let {
            currentAccountInfo?.toAccountView(it)
        }

    suspend fun getAccountViewForAccount(account: Account): AccountView {
        val accountInfo =
            if (currentAccountInfo?.accountId == account.id) {
                currentAccountInfo
            } else {
                withContext(Dispatchers.IO) {
                    accountInfoDao.getAccountInfo(account.id)
                }
            }

        return AccountView(
            account = account,
            profileImage = accountInfo?.miscAccountInfo?.avatar?.let {
                try {
                    Uri.parse(it)
                } catch (e: Exception) {
                    null
                }
            } ?: Uri.fromFile(getImageForAccount(account))
        )
    }

    fun getImageForAccount(account: Account): File {
        return accountImageGenerator.getOrGenerateImageForAccount(accountDir, account)
    }

    private suspend fun updateUnreadCount(account: Account) {
        accountAwareLemmyClient.fetchUnreadCountWithRetry(force = true, account)
            .onSuccess {
                unreadCount.emit(UnreadCount(it.mentions + it.private_messages + it.replies))
            }
    }

    private suspend fun updateAccountInfo(account: Account?) {
        accountInfoUpdateState.emit(StatefulData.Loading())

        if (account == null) {
            currentAccountInfo = null
            accountInfoUpdateState.emit(StatefulData.Success(Unit))
            return
        }

        accountAwareLemmyClient.fetchSiteWithRetry(force = true, account.jwt)
            .onSuccess { response ->
                val accountInfo = AccountInfo(
                    accountId = account.id,
                    subscriptions = response.my_user
                        ?.follows
                        ?.map { it.community.toAccountSubscription() }
                        ?: listOf(),
                    miscAccountInfo = MiscAccountInfo(
                        response.my_user?.local_user_view?.person?.avatar
                    )
                )
                currentAccountInfo = accountInfo

                accountInfoDao.insert(accountInfo)

                subscribedCommunities.emit(accountInfo.subscriptions ?: listOf())
            }

        accountInfoUpdateState.emit(StatefulData.Success(Unit))
    }

    private suspend fun loadAccountInfo(account: Account?) {
        account ?: return

        val accountInfo = accountInfoDao.getAccountInfo(account.id)

        currentAccountInfo = accountInfo

        subscribedCommunities.emit(accountInfo?.subscriptions ?: listOf())
    }

    private fun AccountInfo.toAccountView(account: Account): AccountView? {
        if (this.accountId != account.id) {
            return null
        }

        return AccountView(
            account = account,
            profileImage = this.miscAccountInfo?.avatar?.let {
                try {
                    Uri.parse(it)
                } catch (e: Exception) {
                    null
                }
            } ?: Uri.fromFile(getImageForAccount(account))
        )
    }

    data class UnreadCount(
        val totalUnreadCount: Int
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

private fun Community.toAccountSubscription() =
    AccountSubscription(
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
