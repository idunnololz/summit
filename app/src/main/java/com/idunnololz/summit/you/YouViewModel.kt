package com.idunnololz.summit.you

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.idunnololz.summit.account.AccountManager
import com.idunnololz.summit.account.AccountView
import com.idunnololz.summit.account.asAccount
import com.idunnololz.summit.account.info.AccountInfoManager
import com.idunnololz.summit.api.AccountAwareLemmyClient
import com.idunnololz.summit.api.NotAuthenticatedException
import com.idunnololz.summit.util.StatefulLiveData
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@HiltViewModel
class YouViewModel @Inject constructor(
    private val apiClient: AccountAwareLemmyClient,
    private val accountManager: AccountManager,
    private val accountInfoManager: AccountInfoManager,
) : ViewModel() {

    val currentAccount
        get() = accountManager.currentAccount.asAccount
    val currentAccountView = MutableLiveData<AccountView?>()
    val model = StatefulLiveData<YouModel>()

    init {
        viewModelScope.launch {
            accountInfoManager.currentFullAccount.collect {
                withContext(Dispatchers.Main) {
                    if (it != null) {
                        currentAccountView.value = accountInfoManager
                            .getAccountViewForAccount(it.account)
                    } else {
                        currentAccountView.value = null
                    }
                }
            }
        }
        viewModelScope.launch {
            accountInfoManager.currentFullAccountOnChange.collect {
                loadModel(force = true)
            }
        }
    }

    fun loadModel(force: Boolean) {
        if (model.isLoaded && !force) {
            return
        }

        model.setIsLoading()

        viewModelScope.launch {
            val account = accountInfoManager.currentFullAccount.value

            val personResult = if (account != null) {
                apiClient.fetchPersonByIdWithRetry(personId = account.accountId, force = force)
            } else {
                Result.failure(NotAuthenticatedException())
            }

            model.postValue(
                YouModel(
                    name = personResult.getOrNull()?.person_view?.person?.display_name ?: account?.account?.name,
                    account?.account,
                    account?.accountInfo,
                    personResult,
                ),
            )
        }
    }
}
