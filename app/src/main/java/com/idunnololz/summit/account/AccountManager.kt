package com.idunnololz.summit.account

import android.content.Context
import com.idunnololz.summit.coroutine.CoroutineScopeFactory
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AccountManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val accountDao: AccountDao,
    private val coroutineScopeFactory: CoroutineScopeFactory,
    private val accountImageGenerator: AccountImageGenerator,
) {

    interface OnAccountChangedListener {
        suspend fun onAccountChanged(newAccount: Account?)
    }

    val accountDir = File(context.filesDir, "account")

    private val coroutineScope = coroutineScopeFactory.create()

    private val onAccountChangeListeners = mutableListOf<OnAccountChangedListener>()

    val currentAccount = MutableStateFlow<Account?>(null)

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

    fun getAccountViewForAccount(account: Account) =
        AccountView(
            account = account,
            profileImage = getImageForAccount(account)
        )

    suspend fun getAccounts(): List<Account> = withContext(Dispatchers.IO) {
        val deferred = coroutineScope.async {
            accountDao.getAll()
        }

        deferred.await()
    }

    suspend fun signOut(account: Account) = withContext(Dispatchers.IO) {
        val deferred = coroutineScope.async {
            accountDao.delete(account)
            if (accountDao.getCurrentAccount() == null) {
                val firstAccount = accountDao.getFirstAccount()

                if (firstAccount != null) {
                    accountDao.clearAndSetCurrent(firstAccount.id)
                }
            }
            accountImageGenerator.getImageForAccount(accountDir, account).let {
                if (it.exists()) {
                    it.delete()
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

    fun getImageForAccount(account: Account): File {
        return accountImageGenerator.getOrGenerateImageForAccount(accountDir, account)
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
        onAccountChangeListeners.forEach {
            it.onAccountChanged(newAccount)
        }
    }
}