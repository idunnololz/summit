package com.idunnololz.summit.preferences

import android.content.Context
import android.content.SharedPreferences
import com.idunnololz.summit.account.Account
import com.idunnololz.summit.account.AccountManager
import com.idunnololz.summit.coroutine.CoroutineScopeFactory
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PreferenceManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val baseSharedPreferences: SharedPreferences,
    private val basePreferences: Preferences,
) {

    private var currentAccount: Account? = null
    private var _currentPreferences: Preferences? = null

    val currentPreferences: Preferences
        get() = _currentPreferences ?: basePreferences

    fun getComposedPreferencesForAccount(account: Account?): Preferences {
        if (currentAccount == account) {
            return _currentPreferences!!
        }

        val prefs = if (account != null) {
            listOf(
                getSharedPreferencesForAccount(account),
                baseSharedPreferences
            )
        } else {
            listOf(baseSharedPreferences)
        }

        currentAccount = account
        _currentPreferences = Preferences(
            context = context,
            prefs = ComposedPreferences(prefs)
        )

        return _currentPreferences!!
    }

    fun getOnlyPreferencesForAccount(account: Account): Preferences {
        return Preferences(context, getSharedPreferencesForAccount(account))
    }

    fun getSharedPreferencesForAccount(account: Account): SharedPreferences {
        val key = "account@${account.instance}@${account.id}"
        return context.getSharedPreferences(key, Context.MODE_PRIVATE)
    }

}