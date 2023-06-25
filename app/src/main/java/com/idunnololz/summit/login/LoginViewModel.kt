package com.idunnololz.summit.login

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.idunnololz.summit.account.Account
import com.idunnololz.summit.account.AccountManager
import com.idunnololz.summit.api.AccountAwareLemmyClient
import com.idunnololz.summit.api.LemmyApiClient
import com.idunnololz.summit.api.dto.ListingType
import com.idunnololz.summit.api.dto.SortType
import com.idunnololz.summit.util.StatefulLiveData
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val apiClient: AccountAwareLemmyClient,
    private val accountManager: AccountManager,
) : ViewModel() {

    companion object {
        private const val TAG = "LoginViewModel"
    }

    val accountLiveData = StatefulLiveData<Account>()

    fun login(instance: String, username: String, password: String) {
        accountLiveData.setIsLoading()

        viewModelScope.launch {
            val result = apiClient.login(
                instance,
                username,
                password
            )

            if (result.isFailure) {
                Log.e(TAG, "", result.exceptionOrNull())
                accountLiveData.postError(requireNotNull(result.exceptionOrNull()))
                return@launch
            }

            val jwt = result.getOrNull()
            if (jwt == null) {
                accountLiveData.postError(RuntimeException("200 but no token returned!"))
                return@launch
            }

            val siteResult = apiClient.fetchSiteWithRetry(force = true, jwt)

            if (siteResult.isFailure) {
                Log.e(TAG, "", siteResult.exceptionOrNull())
                accountLiveData.postError(siteResult.exceptionOrNull() ?: RuntimeException())
                return@launch
            }

            val site = siteResult.getOrThrow()
            val luv = site.my_user?.local_user_view

            if (luv == null) {
                accountLiveData.postError(RuntimeException("Login success but local_user_view is null."))
                return@launch
            }

            val account = Account(
                id = luv.person.id,
                name = luv.person.name,
                current = true,
                instance = instance,
                jwt = jwt,
                defaultListingType = luv.local_user.default_listing_type?.ordinal
                    ?: ListingType.All.ordinal,
                defaultSortType = luv.local_user.default_sort_type?.ordinal
                    ?: SortType.Active.ordinal,
            )

            accountManager.addAccountAndSetCurrent(account)

            accountLiveData.postValue(account)

        }
    }
}