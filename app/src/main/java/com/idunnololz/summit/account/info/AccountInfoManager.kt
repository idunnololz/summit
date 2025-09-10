package com.idunnololz.summit.account.info

import android.net.Uri
import android.util.Log
import com.idunnololz.summit.account.Account
import com.idunnololz.summit.account.AccountImageGenerator
import com.idunnololz.summit.account.AccountManager
import com.idunnololz.summit.account.AccountView
import com.idunnololz.summit.account.GuestOrUserAccount
import com.idunnololz.summit.account.asAccount
import com.idunnololz.summit.api.AccountAwareLemmyClient
import com.idunnololz.summit.api.NotAuthenticatedException
import com.idunnololz.summit.api.dto.lemmy.Community
import com.idunnololz.summit.api.dto.lemmy.GetSiteResponse
import com.idunnololz.summit.coroutine.CoroutineScopeFactory
import com.idunnololz.summit.util.StatefulData
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Singleton
class AccountInfoManager @Inject constructor(
  private val accountManager: AccountManager,
  private val accountAwareLemmyClient: AccountAwareLemmyClient,
  private val coroutineScopeFactory: CoroutineScopeFactory,
  private val accountInfoDao: AccountInfoDao,
  private val accountImageGenerator: AccountImageGenerator,
) {

  companion object {
    private const val TAG = "AccountInfoManager"
  }

  private val coroutineScope = coroutineScopeFactory.create()

  private val unreadCountInvalidates = MutableSharedFlow<Unit>()

  val subscribedCommunities = MutableStateFlow<List<AccountSubscription>>(listOf())
  val moderatedCommunities = MutableStateFlow<List<Community>>(listOf())
  val accountInfoUpdateState = MutableStateFlow<StatefulData<Account?>>(StatefulData.NotStarted())
  val unreadCount = MutableStateFlow<UnreadCount>(UnreadCount())
  val currentFullAccount = MutableStateFlow<FullAccount?>(null)
  val currentFullAccountOnChange = currentFullAccount.asSharedFlow().drop(1)
  var fetchAccountInfoJob: Deferred<Result<GetSiteResponse>>? = null

  fun init() {
    accountManager.addOnAccountChangedListener(
      onAccountChangeListener = object : AccountManager.OnAccountChangedListener {
        override suspend fun onAccountSigningOut(account: Account) {
          accountInfoDao.delete(account.id)

          accountImageGenerator.getImageForAccount(account).let {
            if (it.exists()) {
              it.delete()
            }
          }

          unreadCount.emit(UnreadCount())
        }

        override suspend fun onAccountChanged(newAccount: GuestOrUserAccount?) {
          Log.d(TAG, "onAccountChanged()")
          // Don't emit null as it will trigger multiple updates
//                    currentFullAccount.emit(null)
          subscribedCommunities.emit(listOf())
          moderatedCommunities.emit(listOf())
          accountInfoUpdateState.emit(StatefulData.NotStarted())
          unreadCount.emit(UnreadCount())
        }
      },
    )

    coroutineScope.launch {
      accountManager.currentAccount.collect {
        Log.d(TAG, "currentAccount changed!")

        accountInfoUpdateState.emit(StatefulData.NotStarted())

        loadAccountInfo(it as? Account)

        refreshAccountInfo(it as? Account, force = true)
        updateUnreadCount()
      }
    }

    coroutineScope.launch {
      @Suppress("OPT_IN_USAGE")
      unreadCountInvalidates.debounce(1000)
        .collect {
          val account = currentFullAccount.value ?: return@collect
          updateUnreadCount(account)
        }
    }
  }

  fun refreshAccountInfo(force: Boolean) {
    coroutineScope.launch {
      refreshAccountInfo(accountManager.currentAccount.asAccount, force)
    }
  }

  suspend fun fetchAccountInfo(force: Boolean): Result<GetSiteResponse> =
    refreshAccountInfo(accountManager.currentAccount.asAccount, force)

  fun updateUnreadCount() {
    coroutineScope.launch(Dispatchers.Default) {
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

  private fun getImageForAccount(account: Account): File =
    accountImageGenerator.getOrGenerateImageForAccount(account)

  suspend fun getFullAccount(account: Account): FullAccount? {
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
    val fullAccount = response.toFullAccount(account)
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

    accountInfoDao.insert(fullAccount.accountInfo)

    subscribedCommunities.emit(fullAccount.accountInfo.subscriptions ?: listOf())
    moderatedCommunities.emit(fullAccount.accountInfo.miscAccountInfo?.modCommunities ?: listOf())
  }

  private suspend fun updateUnreadCount(fullAccount: FullAccount) {
    val account = fullAccount.account
    val j1 = coroutineScope.launch {
      accountAwareLemmyClient.fetchUnreadCountWithRetry(force = true, account)
        .onSuccess {
          unreadCount.emit(
            unreadCount.value.copy(
              lastUpdateTimeMs = System.currentTimeMillis(),
              account = account,
              totalUnreadCount = it.mentions + it.private_messages + it.replies,
              mentions = it.mentions,
              privateMessages = it.private_messages,
              replies = it.replies,
            ),
          )
        }
    }
    val j2 = coroutineScope.launch {
      if (fullAccount.isMod()) {
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
    }
    val j3 = coroutineScope.launch {
      if (fullAccount.isAdmin()) {
        accountAwareLemmyClient.getUnreadRegistrationApplicationsCount(force = true, account)
          .onSuccess {
            unreadCount.emit(
              unreadCount.value.copy(
                lastUpdateTimeMs = System.currentTimeMillis(),
                account = account,
                totalUnreadApplicationsCount = it.registration_applications,
              ),
            )
          }
      }
    }

    j1.join()
    j2.join()
    j3.join()
  }

  private suspend fun refreshAccountInfo(
    account: Account?,
    force: Boolean,
  ): Result<GetSiteResponse> {
    fetchAccountInfoJob?.let {
      if (!force) {
        return it.await()
      }
    }

    fetchAccountInfoJob?.cancel()
    val fetchAccountInfoJob = coroutineScope.async {
      delay(100)

      accountInfoUpdateState.emit(StatefulData.Loading())

      if (account == null) {
        currentFullAccount.emit(null)
        accountInfoUpdateState.emit(StatefulData.Success(null))
        return@async Result.failure(NotAuthenticatedException())
      }

      val result = withContext(Dispatchers.IO) {
        accountAwareLemmyClient.fetchSiteWithRetry(force = true, account.jwt)
      }

      result
        .onSuccess { response ->
          updateAccountInfoWith(account, response)
        }

      accountInfoUpdateState.emit(StatefulData.Success(account))

      return@async result
    }.also {
      this.fetchAccountInfoJob = it
    }

    return fetchAccountInfoJob.await()
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
    moderatedCommunities.emit(accountInfo.miscAccountInfo?.modCommunities ?: listOf())
  }

  data class UnreadCount(
    val lastUpdateTimeMs: Long = 0,
    val account: Account? = null,
    val totalUnreadCount: Int = 0,
    val mentions: Int = 0,
    val privateMessages: Int = 0,
    val replies: Int = 0,
    val totalUnresolvedReportsCount: Int = 0,
    val totalUnreadApplicationsCount: Int = 0,
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
