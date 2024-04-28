package com.idunnololz.summit.settings.perAccount

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.idunnololz.summit.account.Account
import com.idunnololz.summit.account.AccountManager
import com.idunnololz.summit.account.asAccount
import com.idunnololz.summit.preferences.PreferenceManager
import com.idunnololz.summit.preferences.Preferences
import com.idunnololz.summit.util.StatefulLiveData
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.launch

@HiltViewModel
class SettingsPerAccountViewModel @Inject constructor(
    private val accountManager: AccountManager,
    private val preferenceManager: PreferenceManager,
) : ViewModel() {

    val preferenceData = StatefulLiveData<PreferenceData>()

    fun loadAccount(accountId: Long) {
        preferenceData.setIsLoading()

        viewModelScope.launch {
            val account = if (accountId == 0L) {
                accountManager.currentAccount.asAccount
            } else {
                accountManager.getAccountById(accountId)
            }

            if (account != null) {
                preferenceData.setValue(
                    PreferenceData(
                        account,
                        preferenceManager.getOnlyPreferencesForAccount(account),
                    ),
                )
            } else {
                this@SettingsPerAccountViewModel.preferenceData.setError(NoAccountError())
            }
        }
    }

    class NoAccountError : Exception()

    class PreferenceData(
        val account: Account,
        val preferences: Preferences,
    )
}
