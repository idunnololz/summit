package com.idunnololz.summit.settings.notifications

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.idunnololz.summit.account.Account
import com.idunnololz.summit.account.AccountManager
import com.idunnololz.summit.notifications.NotificationsManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.launch

@HiltViewModel
class SettingsNotificationsViewModel @Inject constructor(
    private val accountManager: AccountManager,
    val notificationsManager: NotificationsManager,
) : ViewModel() {

    val accounts = MutableLiveData<List<Account>>()

    init {
        viewModelScope.launch {
            accountManager.currentAccount.collect {
                accounts.postValue(accountManager.getAccounts())
            }
        }
    }

    fun onNotificationSettingsChanged() {
        notificationsManager.onPreferencesChanged()
    }

    fun onNotificationCheckIntervalChanged() {
        notificationsManager.reenqueue()
    }
}
