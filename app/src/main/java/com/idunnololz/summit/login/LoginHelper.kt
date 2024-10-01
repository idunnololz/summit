package com.idunnololz.summit.login

import android.util.Log
import com.idunnololz.summit.account.Account
import com.idunnololz.summit.account.AccountManager
import com.idunnololz.summit.account.info.AccountInfoManager
import com.idunnololz.summit.api.LemmyApiClient
import javax.inject.Inject

class LoginHelper @Inject constructor(
    private val lemmyApiClientFactory: LemmyApiClient.Factory,
    private val accountManager: AccountManager,
    private val accountInfoManager: AccountInfoManager,
) {

    private companion object {
        private const val TAG = "LoginHelper"
    }

    private val apiClient = lemmyApiClientFactory.create()

    suspend fun loginWithJwt(
        instance: String,
        jwt: String,
    ): Result<Account> {
        apiClient.changeInstance(instance)

        val siteResult = apiClient.fetchSiteWithRetry(auth = jwt, force = true)

        if (siteResult.isFailure) {
            Log.e(TAG, "", siteResult.exceptionOrNull())
            return Result.failure(siteResult.exceptionOrNull() ?: RuntimeException())
        }

        val site = siteResult.getOrThrow()
        val luv = site.my_user?.local_user_view
            ?: return Result.failure(RuntimeException("Login success but local_user_view is null."))

        val account = Account.from(instance, luv, jwt)

        accountManager.addAccountAndSetCurrent(account)
        accountInfoManager.updateAccountInfoWith(account, site)

        return Result.success(account)
    }
}