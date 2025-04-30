package com.idunnololz.summit.preferences

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.idunnololz.summit.account.Account
import com.idunnololz.summit.lemmy.utils.StableAccountId
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SharedPreferencesManager @Inject constructor(
  @ApplicationContext private val context: Context,
) {
  companion object {
    private const val DEFAULT_PREF = "pref"
    private const val TEMP_PREF = "temp_preferences"
  }

  val defaultSharedPreferences = context.getSharedPreferences(DEFAULT_PREF, Context.MODE_PRIVATE)
  val tempPreferences = context.getSharedPreferences(TEMP_PREF, Context.MODE_PRIVATE)
  var useTempPreferences = false
    set(value) {
      if (field == value) {
        return
      }

      if (!value) {
        tempPreferences.edit {
          clear()
        }
      }

      field = value
    }

  val currentSharedPreferences
    get() =
      if (useTempPreferences) {
        tempPreferences
      } else {
        defaultSharedPreferences
      }

  fun getSharedPreferencesForAccount(account: Account): SharedPreferences {
    val key = "account@${account.instance}@${account.id}"
    return context.getSharedPreferences(key, Context.MODE_PRIVATE)
  }

  fun getAccountIdSharedPreferences(): SharedPreferences {
    return context.getSharedPreferences("account_ids@", Context.MODE_PRIVATE)
  }

  fun getNotificationsSharedPreferences(): SharedPreferences {
    return context.getSharedPreferences("notifications@", Context.MODE_PRIVATE)
  }

  fun getGlobalStateSharedPreferences(): SharedPreferences {
    return context.getSharedPreferences("global_state@", Context.MODE_PRIVATE)
  }

  fun getAccountStateSharedPreferences(stableAccountId: StableAccountId): SharedPreferences {
    return context.getSharedPreferences("state_$stableAccountId@", Context.MODE_PRIVATE)
  }
}
