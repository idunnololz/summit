package com.idunnololz.summit.account

import com.idunnololz.summit.api.AccountAwareLemmyClient
import com.idunnololz.summit.api.dto.Community
import com.idunnololz.summit.api.dto.Language
import com.idunnololz.summit.api.dto.MyUserInfo
import com.idunnololz.summit.api.dto.PersonView
import com.idunnololz.summit.api.dto.SiteView
import com.idunnololz.summit.api.dto.Tagline
import com.idunnololz.summit.coroutine.CoroutineScopeFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AccountInfoManager @Inject constructor(
    private val accountManager: AccountManager,
    private val accountAwareLemmyClient: AccountAwareLemmyClient,
    private val coroutineScopeFactory: CoroutineScopeFactory,
) {

    private val coroutineScope = coroutineScopeFactory.create()

    private var currentAccountInfo: AccountInfo? = null

    val subscribedCommunities = MutableStateFlow<List<Community>>(listOf())

    init {
        accountManager.addOnAccountChangedListener(
            onAccountChangeListener = object : AccountManager.OnAccountChangedListener {
                override suspend fun onAccountChanged(newAccount: Account?) {
                    currentAccountInfo = null
                    updateAccountInfo(newAccount)
                }
            }
        )

        coroutineScope.launch {
            accountManager.currentAccount.collect {
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
        if (account == null) {
            currentAccountInfo = null
            return
        }

        accountAwareLemmyClient.fetchSiteWithRetry(force = true, account.jwt)
            .onSuccess { response ->
                currentAccountInfo = AccountInfo(
                    response.site_view,
                    response.admins,
                    response.online,
                    response.version,
                    response.my_user,
                    response.all_languages,
                    response.discussion_languages,
                    response.taglines,
                )

                subscribedCommunities.emit(
                    currentAccountInfo?.myUser?.follows?.map {
                        it.community
                    } ?: listOf()
                )
            }
    }

    private class AccountInfo(
        val siteView: SiteView,
        val admins: List<PersonView>,
        val online: Int,
        val version: String,
        val myUser: MyUserInfo?,
        val allLanguages: List<Language>,
        val discussionLanguages: List<Int>,
        val taglines: List<Tagline>?,
    )
}