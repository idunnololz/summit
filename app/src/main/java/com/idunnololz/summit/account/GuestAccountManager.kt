package com.idunnololz.summit.account

import android.content.Context
import com.idunnololz.summit.coroutine.CoroutineScopeFactory
import com.idunnololz.summit.preferences.GuestAccountSettings
import com.idunnololz.summit.preferences.Preferences
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.launch

@Singleton
class GuestAccountManager @Inject constructor(
  @ApplicationContext private val context: Context,
  private val accountManager: AccountManager,
  private val preferences: Preferences,
  private val coroutineScopeFactory: CoroutineScopeFactory,
) {

  private val coroutineScope = coroutineScopeFactory.create()

  fun changeGuestAccountInstance(instance: String) {
    coroutineScope.launch {
      val currentGuestAccount = preferences.guestAccountSettings ?: GuestAccountSettings()

      preferences.guestAccountSettings = currentGuestAccount.copy(instance = instance)

      // Let all observers know the instance changed via an account change event
      accountManager.setCurrentAccount(getGuestAccount())
    }
  }

  fun getGuestAccount(): GuestAccount =
    accountManager.getGuestAccount()
}
