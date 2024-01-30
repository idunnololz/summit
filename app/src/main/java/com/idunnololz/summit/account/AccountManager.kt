package com.idunnololz.summit.account

import android.util.Log
import androidx.lifecycle.asLiveData
import androidx.lifecycle.map
import com.idunnololz.summit.coroutine.CoroutineScopeFactory
import com.idunnololz.summit.preferences.PreferenceManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AccountManager @Inject constructor(
    private val accountDao: AccountDao,
    private val coroutineScopeFactory: CoroutineScopeFactory,
    private val preferenceManager: PreferenceManager,
) {

    interface OnAccountChangedListener {
        suspend fun onAccountSigningOut(account: Account) {}
        suspend fun onAccountChanged(newAccount: Account?)
    }

    private val coroutineScope = coroutineScopeFactory.create()

    private val onAccountChangeListeners = mutableListOf<OnAccountChangedListener>()

    private val _currentAccount = MutableStateFlow<GuestOrUserAccount?>(null)

    val currentAccount: StateFlow<GuestOrUserAccount?> = _currentAccount
    val currentAccountOnChange = _currentAccount.asSharedFlow().drop(1)

    val mutex = Mutex()

    init {
        runBlocking {
            val curAccount = accountDao.getCurrentAccount()?.fix()
            preferenceManager.getComposedPreferencesForAccount(curAccount)
            _currentAccount.emit(curAccount)
        }
        coroutineScope.launch {
            Log.d("dbdb", "accountDao: ${accountDao.count()}")
        }
    }

    fun addAccountAndSetCurrent(account: Account) {
        coroutineScope.launch {
            accountDao.insert(account)
            accountDao.clearAndSetCurrent(account.id)
            doSwitchAccountWork(account)

            _currentAccount.emit(account)
        }
    }

    suspend fun getAccounts(): List<Account> = withContext(Dispatchers.IO) {
        val deferred = coroutineScope.async {
            accountDao.getAll()
        }

        deferred.await()
            .map { it.fix() }
    }

    suspend fun signOut(account: Account) = withContext(Dispatchers.IO) {
        val deferred = coroutineScope.async {
            doSignOutWork(account)

            accountDao.delete(account)
            if (accountDao.getCurrentAccount() == null) {
                val firstAccount = accountDao.getFirstAccount()

                if (firstAccount != null) {
                    accountDao.clearAndSetCurrent(firstAccount.id)
                }
            }

            updateCurrentAccount()
        }

        deferred.await()
    }

    suspend fun setCurrentAccount(guestOrUserAccount: GuestOrUserAccount?) = withContext(Dispatchers.IO) {
        val deferred = coroutineScope.async {
            val account = guestOrUserAccount as? Account
            accountDao.clearAndSetCurrent(account?.id)

            doSwitchAccountWork(account)

            _currentAccount.emit(guestOrUserAccount)
        }

        deferred.await()
    }

    private suspend fun updateCurrentAccount() {
        val currentAccount = accountDao.getCurrentAccount()
        if (this._currentAccount.value != currentAccount) {
            doSwitchAccountWork(currentAccount)
        }
        this._currentAccount.emit(currentAccount)
    }

    suspend fun getAccountById(id: Long): Account? {
        return accountDao.getAccountById(id)
    }

    fun addOnAccountChangedListener(onAccountChangeListener: OnAccountChangedListener) {
        onAccountChangeListeners.add(onAccountChangeListener)
    }

    private suspend fun doSwitchAccountWork(newAccount: Account?) {
        // Do pre-switch work here...

        preferenceManager.getComposedPreferencesForAccount(newAccount)

        val listeners = withContext(Dispatchers.Main) {
            onAccountChangeListeners.toList()
        }
        listeners.forEach {
            it.onAccountChanged(newAccount)
        }
    }

    private suspend fun doSignOutWork(account: Account) {
        val listeners = withContext(Dispatchers.Main) {
            onAccountChangeListeners.toList()
        }
        listeners.forEach {
            it.onAccountSigningOut(account)
        }
    }
}

val StateFlow<GuestOrUserAccount?>.asAccount
    get() = value as? Account

fun StateFlow<GuestOrUserAccount?>.asAccountLiveData() =
    this.asLiveData().map { it as? Account }

private fun Account.fix() =
    copy(instance = instance.trim())
