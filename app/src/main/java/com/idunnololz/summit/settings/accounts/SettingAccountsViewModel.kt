package com.idunnololz.summit.settings.accounts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.idunnololz.summit.account.AccountManager
import com.idunnololz.summit.account.AccountView
import com.idunnololz.summit.account.info.AccountInfoManager
import com.idunnololz.summit.util.StatefulLiveData
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingAccountsViewModel @Inject constructor(
    private val accountManager: AccountManager,
    private val accountInfoManager: AccountInfoManager,
) : ViewModel() {

    val accounts = StatefulLiveData<List<AccountView>>()

    init {
        loadAccounts()
    }

    private fun loadAccounts() {
        accounts.setIsLoading()

        viewModelScope.launch {
            val accountViews = accountManager.getAccounts().map {
                accountInfoManager.getAccountViewForAccount(it)
                    .copy(account = it.copy(current = false))
            }

            accounts.postValue(accountViews)
        }
    }
}
