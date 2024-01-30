package com.idunnololz.summit.accountUi

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.idunnololz.summit.account.Account
import com.idunnololz.summit.account.AccountManager
import com.idunnololz.summit.account.AccountView
import com.idunnololz.summit.account.GuestOrUserAccount
import com.idunnololz.summit.account.info.AccountInfoManager
import com.idunnololz.summit.util.StatefulLiveData
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class AccountsViewModel @Inject constructor(
    private val accountManager: AccountManager,
    private val accountInfoManager: AccountInfoManager,
) : ViewModel() {

    val accounts = StatefulLiveData<List<AccountView>>()
    val signOut = StatefulLiveData<Unit>()

    init {
        refreshAccounts()
    }

    fun signOut(account: Account) {
        signOut.setIsLoading()

        viewModelScope.launch {
            accountManager.signOut(account)
            signOut.postValue(Unit)

            withContext(Dispatchers.Main) {
                refreshAccounts()
            }
        }
    }

    fun switchAccount(account: GuestOrUserAccount) {
        viewModelScope.launch {
            accountManager.setCurrentAccount(account)

            refreshAccounts()
        }
    }

    fun refreshAccounts() {
        accounts.setIsLoading()

        viewModelScope.launch {
            val accountViews = accountManager.getAccounts().map {
                accountInfoManager.getAccountViewForAccount(it)
            }

            accounts.postValue(accountViews)
        }
    }
}
