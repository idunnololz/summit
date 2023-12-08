package com.idunnololz.summit.preferences

import android.content.Context
import android.content.SharedPreferences
import com.idunnololz.summit.account.Account
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PreferenceManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val basePreferences: SharedPreferences,
) {

    private var currentAccount: Account? = null
    private var currentPreferences: Preferences? = null

    fun getPreferencesForAccount(account: Account?): Preferences {
        if (currentAccount == account) {
            return currentPreferences!!
        }

        currentAccount = account
        currentPreferences = Preferences(context, ComposedPreferences(listOf(basePreferences)))

        return currentPreferences!!
    }
}