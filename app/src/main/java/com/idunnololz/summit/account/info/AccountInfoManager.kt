package com.idunnololz.summit.account.info

import com.idunnololz.summit.account.Account
import com.idunnololz.summit.account.AccountManager
import com.idunnololz.summit.api.AccountAwareLemmyClient
import com.idunnololz.summit.api.dto.Community
import com.idunnololz.summit.coroutine.CoroutineScopeFactory
import com.idunnololz.summit.util.StatefulData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AccountInfoManager @Inject constructor(
    private val accountManager: AccountManager,
    private val accountAwareLemmyClient: AccountAwareLemmyClient,
    private val coroutineScopeFactory: CoroutineScopeFactory,
    private val accountInfoDao: AccountInfoDao,
) {

    private val coroutineScope = coroutineScopeFactory.create()

    private var currentAccountInfo: AccountInfo? = null

    val subscribedCommunities = MutableStateFlow<List<AccountSubscription>>(listOf())
    val accountInfoUpdateState = MutableStateFlow<StatefulData<Unit>>(StatefulData.NotStarted())

    init {
        accountManager.addOnAccountChangedListener(
            onAccountChangeListener = object : AccountManager.OnAccountChangedListener {
                override suspend fun onAccountSigningOut(account: Account) {
                    accountInfoDao.delete(account.id)
                }

                override suspend fun onAccountChanged(newAccount: Account?) {
                    currentAccountInfo = null
                    subscribedCommunities.emit(listOf())
                    accountInfoUpdateState.emit(StatefulData.NotStarted())
                }
            }
        )

        coroutineScope.launch {
            accountManager.currentAccount.collect {
                accountInfoUpdateState.emit(StatefulData.NotStarted())

                loadSubscriptions(it)

                updateAccountInfo(it)
            }
        }
    }

    fun fetchAccountInfo() {
        coroutineScope.launch {
            updateAccountInfo(accountManager.currentAccount.value)
        }
    }

    private suspend fun updateAccountInfo(account: Account?) {
        accountInfoUpdateState.emit(StatefulData.Loading())

        if (account == null) {
            currentAccountInfo = null
            return
        }

        accountAwareLemmyClient.fetchSiteWithRetry(force = true, account.jwt)
            .onSuccess { response ->
                val accountInfo = AccountInfo(
                    accountId = account.id,
                    subscriptions = response.my_user
                        ?.follows
                        ?.map { it.community.toAccountSubscription() }
                        ?: listOf()
                )
                currentAccountInfo = accountInfo

                accountInfoDao.insert(accountInfo)

                subscribedCommunities.emit(accountInfo.subscriptions ?: listOf())
            }

        accountInfoUpdateState.emit(StatefulData.Success(Unit))
    }

    private suspend fun loadSubscriptions(account: Account?) {
        account ?: return

        val subscriptions = accountInfoDao.getAccountInfo(account.id)

        subscribedCommunities.emit(subscriptions?.subscriptions ?: listOf())
    }

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
