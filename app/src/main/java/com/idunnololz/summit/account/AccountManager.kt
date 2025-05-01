package com.idunnololz.summit.account

import android.content.SharedPreferences
import android.util.Log
import androidx.lifecycle.asLiveData
import androidx.lifecycle.map
import com.idunnololz.summit.coroutine.CoroutineScopeFactory
import com.idunnololz.summit.preferences.AccountIdsSharedPreference
import com.idunnololz.summit.preferences.PreferenceManager
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.withContext
import java.util.Locale

@Singleton
class AccountManager @Inject constructor(
  private val accountDao: AccountDao,
  private val coroutineScopeFactory: CoroutineScopeFactory,
  private val preferenceManager: PreferenceManager,
  @AccountIdsSharedPreference private val accountIdsSharedPreference: SharedPreferences,

) {

  companion object {
    private const val KEY_ACCOUNT_ID_COUNTER = "#counter"
    private const val ACCOUNT_ID_MAX_VALUE = 10000
  }

  interface OnAccountChangedListener {
    suspend fun onAccountSigningOut(account: Account) {}
    suspend fun onAccountChanged(newAccount: Account?)
  }

  private val coroutineScope = coroutineScopeFactory.create()

  private val onAccountChangeListeners = mutableListOf<OnAccountChangedListener>()

  private val _currentAccount = MutableStateFlow<GuestOrUserAccount?>(null)
  private val _numAccounts = MutableStateFlow<Int>(0)

  private val accountByIdCache = mutableMapOf<Long, Account?>()

  val currentAccount: StateFlow<GuestOrUserAccount?> = _currentAccount
  val currentAccountOnChange = _currentAccount.asSharedFlow().drop(1)

  val numAccounts: StateFlow<Int> = _numAccounts

  val mutex = Mutex()

  init {
    runBlocking {
      val curAccount = accountDao.getCurrentAccount()?.fix()
      preferenceManager.updateCurrentPreferences(curAccount)
      _currentAccount.emit(curAccount)
      updateNumAccounts()
    }
    coroutineScope.launch {
      Log.d("dbdb", "accountDao: ${_numAccounts.value}")
    }
  }

  fun addAccountAndSetCurrent(account: Account) {
    val fixedAccount = account.fix()
    coroutineScope.launch {
      accountDao.insert(fixedAccount)
      accountDao.clearAndSetCurrent(fixedAccount.id)
      doSwitchAccountWork(fixedAccount)

      _currentAccount.emit(fixedAccount)
      updateNumAccounts()
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
      accountIdsSharedPreference.edit()
        .remove(account.fullName)
        .apply()

      updateCurrentAccount()
      updateNumAccounts()
    }

    deferred.await()
  }

  suspend fun setCurrentAccount(guestOrUserAccount: GuestOrUserAccount?) = withContext(
    Dispatchers.IO,
  ) {
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
    return accountDao.getAccountById(id)?.fix()
  }

  suspend fun getAccountByIdOrDefault(accountId: Long?): Account? = if (accountId == null) {
    currentAccount.asAccount
  } else {
    getAccountById(accountId)
  }

  fun getAccountByIdBlocking(id: Long): Account? {
    val cachedAccount = accountByIdCache[id]
    if (cachedAccount != null) {
      return cachedAccount
    }

    return runBlocking {
      val account = getAccountById(id)
      accountByIdCache[id] = account

      account
    }
  }

  fun addOnAccountChangedListener(onAccountChangeListener: OnAccountChangedListener) {
    onAccountChangeListeners.add(onAccountChangeListener)
  }

  fun removeOnAccountChangedListener(onAccountChangeListener: OnAccountChangedListener) {
    onAccountChangeListeners.add(onAccountChangeListener)
  }

  fun getLocalAccountId(account: Account): Int {
    val accountKey = account.fullName
    if (accountIdsSharedPreference.contains(accountKey)) {
      return accountIdsSharedPreference.getInt(accountKey, 0)
    } else {
      val nextAccountId = accountIdsSharedPreference.getInt(KEY_ACCOUNT_ID_COUNTER, 0)
      val newNextAccountId = if (nextAccountId + 1 == ACCOUNT_ID_MAX_VALUE) {
        0
      } else {
        nextAccountId + 1
      }

      accountIdsSharedPreference.edit()
        .putInt(KEY_ACCOUNT_ID_COUNTER, newNextAccountId)
        .putInt(accountKey, nextAccountId)
        .apply()

      return nextAccountId
    }
  }

  private suspend fun doSwitchAccountWork(newAccount: Account?) {
    // Do pre-switch work here...

    preferenceManager.updateCurrentPreferences(newAccount)

    val listeners = withContext(Dispatchers.Main) {
      onAccountChangeListeners.toList()
    }
    listeners.forEach {
      it.onAccountChanged(newAccount)
    }
  }

  private suspend fun doSignOutWork(account: Account) {
    val listeners: List<OnAccountChangedListener>

    withContext(Dispatchers.Main) {
      accountByIdCache.remove(account.id)

      listeners = onAccountChangeListeners.toList()
    }
    listeners.forEach {
      it.onAccountSigningOut(account)
    }
  }

  private suspend fun updateNumAccounts() {
    _numAccounts.value = accountDao.count()
  }
}

val StateFlow<GuestOrUserAccount?>.asAccount
  get() = value as? Account

fun StateFlow<GuestOrUserAccount?>.asAccountLiveData() = this.asLiveData().map { it as? Account }

private fun Account.fix() = copy(instance = instance.trim().lowercase(Locale.US))
