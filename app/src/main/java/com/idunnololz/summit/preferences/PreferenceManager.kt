package com.idunnololz.summit.preferences

import android.content.Context
import android.content.SharedPreferences
import com.idunnololz.summit.account.Account
import com.idunnololz.summit.account.GuestOrUserAccount
import com.idunnololz.summit.coroutine.CoroutineScopeFactory
import com.idunnololz.summit.lemmy.utils.StableAccountId
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.serialization.json.Json

@Singleton
class PreferenceManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val baseSharedPreferences: SharedPreferences,
    val basePreferences: Preferences,
    private val coroutineScopeFactory: CoroutineScopeFactory,
    private val json: Json,
) {

    private var currentAccount: Account? = null
    private var _currentPreferences: Preferences? = basePreferences

    val currentPreferences: Preferences
        get() = _currentPreferences ?: basePreferences

    fun getComposedPreferencesForAccount(guestOrUserAccount: GuestOrUserAccount?): Preferences {
        val account = guestOrUserAccount as? Account

        if (currentAccount == account) {
            return _currentPreferences!!
        }

        val prefs = if (account != null) {
            listOf(
                getSharedPreferencesForAccount(account),
                baseSharedPreferences,
            )
        } else {
            listOf(baseSharedPreferences)
        }

        currentAccount = account
        _currentPreferences = Preferences(
            context = context,
            prefs = ComposedPreferences(prefs),
            coroutineScopeFactory = coroutineScopeFactory,
            json = json,
        )

        return _currentPreferences!!
    }

    fun getOnlyPreferencesForAccount(account: Account): Preferences {
        return Preferences(
            context = context,
            prefs = getSharedPreferencesForAccount(account),
            coroutineScopeFactory = coroutineScopeFactory,
            json = json,
        )
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
