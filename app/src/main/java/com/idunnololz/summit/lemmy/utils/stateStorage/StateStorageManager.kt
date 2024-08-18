package com.idunnololz.summit.lemmy.utils.stateStorage

import com.idunnololz.summit.lemmy.utils.StableAccountId
import com.idunnololz.summit.preferences.PreferenceManager
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StateStorageManager @Inject constructor(
    private val preferenceManager: PreferenceManager,
) {

    private val accountStateStorageByAccount = mutableMapOf<StableAccountId, AccountStateStorage>()

    val globalStateStorage by lazy {
        GlobalStateStorage(preferenceManager.getGlobalStateSharedPreferences())
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
                preferenceManager.getAccountStateSharedPreferences(stableAccountId)
            )
        }

        return requireNotNull(accountStateStorageByAccount[stableAccountId])
    }

}