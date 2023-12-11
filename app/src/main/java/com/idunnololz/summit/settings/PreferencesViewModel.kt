package com.idunnololz.summit.settings

import androidx.lifecycle.ViewModel
import com.idunnololz.summit.account.Account
import com.idunnololz.summit.preferences.PreferenceManager
import com.idunnololz.summit.preferences.Preferences
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class PreferencesViewModel @Inject constructor(
    private val preferenceManager: PreferenceManager,
    private val preferences: Preferences,
) : ViewModel() {

    fun getPreferences(account: Account?) =
        if (account == null) {
            preferences
        } else {
            preferenceManager.getOnlyPreferencesForAccount(account)
        }
}