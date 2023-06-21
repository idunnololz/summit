package com.idunnololz.summit.main

import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.idunnololz.summit.account.AccountManager
import com.idunnololz.summit.account.AccountView
import com.idunnololz.summit.api.AccountAwareLemmyClient
import com.idunnololz.summit.api.dto.CommunityView
import com.idunnololz.summit.api.dto.ListingType
import com.idunnololz.summit.api.dto.SortType
import com.idunnololz.summit.lemmy.CommunityRef
import com.idunnololz.summit.user.UserCommunitiesManager
import com.idunnololz.summit.util.StatefulLiveData
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.transformWhile
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class MainActivityViewModel @Inject constructor(
    private val apiClient: AccountAwareLemmyClient,
    private val accountManager: AccountManager,
    val communitySelectorControllerFactory: CommunitySelectorController.Factory,
    val userCommunitiesManager: UserCommunitiesManager,
) : ViewModel() {

    companion object {
        private val TAG = "MainActivityViewModel"
    }

    private val readyCount = MutableStateFlow<Int>(0)

    val communities = StatefulLiveData<List<CommunityView>>()
    val currentAccount = MutableLiveData<AccountView?>(null)
    val defaultCommunity = MutableLiveData<CommunityRef>(null)

    val isReady = MutableLiveData<Boolean>(false)

    fun Flow<Int>.completeWhenDone(): Flow<Int> =
        transformWhile { readyCount ->
            emit(readyCount) // always emit progress

            readyCount < 2
        }

    init {
        viewModelScope.launch {
            launch {
                readyCount
                    .completeWhenDone()
                    .collect {}

                Log.d(TAG, "All ready!")
                isReady.postValue(true)
            }

            launch {
                accountManager.currentAccount.collect {
                    val accountView = if (it != null) {
                        accountManager.getAccountViewForAccount(it)
                    } else {
                        null
                    }

                    currentAccount.postValue(accountView)

                    withContext(Dispatchers.Main) {
                        Log.d(TAG, "'accountManager' ready!")
                        readyCount.emit(readyCount.value + 1)
                    }
                }
            }

            launch {
                while (true) {
                    Log.d(TAG, "Waiting for 'userCommunitiesManager'")
                    val mainTab = userCommunitiesManager.waitForTab(
                        UserCommunitiesManager.FIRST_FRAGMENT_TAB_ID)

                    if (mainTab != null) {
                        break
                    }
                }

                withContext(Dispatchers.Main) {
                    Log.d(TAG, "'userCommunitiesManager' ready!")
                    readyCount.emit(readyCount.value + 1)
                }
            }

            launch {
                userCommunitiesManager.defaultCommunity
                    .collect {
                        defaultCommunity.postValue(it)
                    }
            }

            launch {
                accountManager.currentAccount.collect {
                    loadCommunities()
                }
            }
        }
    }

    fun loadCommunities(force: Boolean = false) {
        communities.setIsLoading()
        viewModelScope.launch {
            apiClient.fetchCommunitiesWithRetry(
                sortType = SortType.Active,
                listingType = ListingType.All
            ).onSuccess {
                communities.postValue(it)
            }.onFailure {
                communities.postError(it)
            }
        }
    }
}