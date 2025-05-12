package com.idunnololz.summit.settings.notifications

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.idunnololz.summit.account.Account
import com.idunnololz.summit.account.AccountManager
import com.idunnololz.summit.account.fullName
import com.idunnololz.summit.notifications.NotificationsManager
import com.idunnololz.summit.settings.OnOffSettingItem
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.launch

@HiltViewModel
class SettingsNotificationsViewModel @Inject constructor(
  private val accountManager: AccountManager,
  val notificationsManager: NotificationsManager,
) : ViewModel() {

  data class AccountSettingItem(
    val account: Account,
    val settingItem: OnOffSettingItem,
  )

  val accountToSetting = LinkedHashMap<String, AccountSettingItem>()
  val settings = MutableLiveData<List<AccountSettingItem>>()

  init {
    viewModelScope.launch {
      accountManager.currentAccount.collect {
        accountManager.getAccounts().forEach {
          if (accountToSetting[it.fullName] == null) {
            accountToSetting[it.fullName] =
              AccountSettingItem(
                it,
                OnOffSettingItem(
                  null,
                  it.fullName,
                  null,
                ),
              )
          }
        }

        settings.postValue(accountToSetting.values.toList().sortedBy { it.account.fullName })
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
