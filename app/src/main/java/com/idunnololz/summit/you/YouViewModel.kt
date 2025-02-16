package com.idunnololz.summit.you

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.idunnololz.summit.account.AccountManager
import com.idunnololz.summit.account.AccountView
import com.idunnololz.summit.account.asAccount
import com.idunnololz.summit.account.info.AccountInfoManager
import com.idunnololz.summit.actions.PendingActionsManager
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
    private val pendingActionsManager: PendingActionsManager,
) : ViewModel() {

    val currentAccount
        get() = accountManager.currentAccount.asAccount
    val currentAccountView = MutableLiveData<AccountView?>()
    val model = StatefulLiveData<YouModel>()
    val newActionErrorsCount = pendingActionsManager.numNewFailedActionsFlow.asLiveData()

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

            if (account?.account?.name != null && !force) {
                withContext(Dispatchers.Main) {
                    model.setValue(
                        YouModel(
                            name = account.account.name,
                            account = account.account,
                            accountInfo = account.accountInfo,
                            personResult = null,
                            isLoading = true,
                        ),
                    )
                }
            }

            val personResult = if (account != null) {
                apiClient.fetchPersonByIdWithRetry(personId = account.accountId, force = force)
            } else {
                Result.failure(NotAuthenticatedException())
            }

            withContext(Dispatchers.Main) {
                model.setValue(
                    YouModel(
                        name = personResult.getOrNull()?.person_view?.person?.display_name
                            ?: account?.account?.name,
                        account = account?.account,
                        accountInfo = account?.accountInfo,
                        personResult = personResult,
                        isLoading = false,
                    ),
                )
            }
        }
    }
}
