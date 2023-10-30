package com.idunnololz.summit.settings.accounts.perAccount

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.idunnololz.summit.account.Account
import com.idunnololz.summit.account.AccountManager
import com.idunnololz.summit.util.StatefulLiveData
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingPerAccountViewModel @Inject constructor(
    private val accountManager: AccountManager,
) : ViewModel() {

    val account = StatefulLiveData<Account>()

    fun loadAccount(accountId: Long) {
        account.setIsLoading()

        viewModelScope.launch {
            val account = if (accountId == 0L) {
                accountManager.currentAccount.value
            } else {
                accountManager.getAccountById(accountId)
            }

            if (account != null) {
                this@SettingPerAccountViewModel.account.setValue(account)
            } else {
                this@SettingPerAccountViewModel.account.setError(NoAccountError())
            }
        }
    }

    class NoAccountError : Exception()
}
