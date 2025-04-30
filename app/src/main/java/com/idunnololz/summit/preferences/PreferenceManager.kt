package com.idunnololz.summit.preferences

import android.content.Context
import com.idunnololz.summit.account.Account
import com.idunnololz.summit.account.GuestOrUserAccount
import com.idunnololz.summit.coroutine.CoroutineScopeFactory
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.serialization.json.Json

@Singleton
class PreferenceManager @Inject constructor(
  @ApplicationContext private val context: Context,
  private val basePreferences: Preferences,
  private val coroutineScopeFactory: CoroutineScopeFactory,
  private val json: Json,
  private val sharedPreferencesManager: SharedPreferencesManager,
) {

  private var currentAccount: Account? = null
  private var _currentPreferences: Preferences? = basePreferences

  val currentPreferences: Preferences
    get() = _currentPreferences ?: basePreferences

  fun updateCurrentPreferences(
    guestOrUserAccount: GuestOrUserAccount?,
    force: Boolean = false,
  ): Preferences {
    val account = guestOrUserAccount as? Account

    if (currentAccount == account && !force) {
      return _currentPreferences!!
    }

    val prefs = if (account != null) {
      listOf(
        sharedPreferencesManager.getSharedPreferencesForAccount(account),
        sharedPreferencesManager.currentSharedPreferences,
      )
    } else {
      listOf(sharedPreferencesManager.currentSharedPreferences)
    }

    currentAccount = account
    _currentPreferences = Preferences(
      context = context,
      sharedPreferencesManager = sharedPreferencesManager,
      sharedPreferences = ComposedPreferences(
        preferences = prefs,
        base = sharedPreferencesManager.currentSharedPreferences,
      ),
      coroutineScopeFactory = coroutineScopeFactory,
      json = json,
    )

    return _currentPreferences!!
  }

  fun getOnlyPreferencesForAccount(account: Account): Preferences {
    return Preferences(
      context = context,
      sharedPreferencesManager = sharedPreferencesManager,
      sharedPreferences = sharedPreferencesManager.getSharedPreferencesForAccount(account),
      coroutineScopeFactory = coroutineScopeFactory,
      json = json,
    )
  }
}
