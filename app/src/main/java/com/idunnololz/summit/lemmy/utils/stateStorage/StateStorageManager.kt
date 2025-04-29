package com.idunnololz.summit.lemmy.utils.stateStorage

import com.idunnololz.summit.lemmy.utils.StableAccountId
import com.idunnololz.summit.preferences.SharedPreferencesManager
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StateStorageManager @Inject constructor(
    private val sharedPreferencesManager: SharedPreferencesManager,
) {

    private val accountStateStorageByAccount = mutableMapOf<StableAccountId, AccountStateStorage>()

    val globalStateStorage by lazy {
        GlobalStateStorage(sharedPreferencesManager.getGlobalStateSharedPreferences())
    }

    fun getAccountStateStorage(accountId: Long, accountInstance: String): AccountStateStorage {
        val stableAccountId = StableAccountId(accountId, accountInstance)

        accountStateStorageByAccount[stableAccountId]?.let {
            return it
        }

        synchronized(this) {
            accountStateStorageByAccount[stableAccountId]?.let {
                return it
            }

            accountStateStorageByAccount[stableAccountId] = AccountStateStorage(
                accountId,
                accountInstance,
                sharedPreferencesManager.getAccountStateSharedPreferences(stableAccountId),
            )
        }

        return requireNotNull(accountStateStorageByAccount[stableAccountId])
    }
}
