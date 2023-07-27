package com.idunnololz.summit.account

import com.idunnololz.summit.coroutine.CoroutineScopeFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AccountManager @Inject constructor(
    private val accountDao: AccountDao,
    private val coroutineScopeFactory: CoroutineScopeFactory,
) {

    interface OnAccountChangedListener {
        suspend fun onAccountSigningOut(account: Account) {}
        suspend fun onAccountChanged(newAccount: Account?)
    }

    private val coroutineScope = coroutineScopeFactory.create()

    private val onAccountChangeListeners = mutableListOf<OnAccountChangedListener>()

    val currentAccount = MutableStateFlow<Account?>(null)
    val currentAccountOnChange = currentAccount.asSharedFlow().drop(1)

    init {
        runBlocking {
            currentAccount.emit(accountDao.getCurrentAccount())
        }
    }

    fun addAccountAndSetCurrent(account: Account) {
        coroutineScope.launch {
            accountDao.insert(account)
            accountDao.clearAndSetCurrent(account.id)
            doSwitchAccountWork(account)

            currentAccount.emit(account)
        }
    }

    suspend fun getAccounts(): List<Account> = withContext(Dispatchers.IO) {
        val deferred = coroutineScope.async {
            accountDao.getAll()
        }

        deferred.await()
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

    suspend fun setCurrentAccount(account: Account) = withContext(Dispatchers.IO) {
        val deferred = coroutineScope.async {
            accountDao.clearAndSetCurrent(account.id)

            doSwitchAccountWork(account)

            currentAccount.emit(account)
        }

        deferred.await()
    }

    private suspend fun updateCurrentAccount() {
        val currentAccount = accountDao.getCurrentAccount()
        if (this.currentAccount.value != currentAccount) {
            doSwitchAccountWork(currentAccount)
        }
        this.currentAccount.emit(currentAccount)
    }

    suspend fun getAccountById(id: Int): Account? {
        return accountDao.getAccountById(id)
    }

    fun addOnAccountChangedListener(onAccountChangeListener: OnAccountChangedListener) {
        onAccountChangeListeners.add(onAccountChangeListener)
    }

    private suspend fun doSwitchAccountWork(newAccount: Account?) {
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
