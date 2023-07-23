package com.idunnololz.summit.settings.webSettings.blockList.user

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.idunnololz.summit.account.AccountManager
import com.idunnololz.summit.account.info.AccountInfo
import com.idunnololz.summit.account.info.AccountInfoManager
import com.idunnololz.summit.api.AccountAwareLemmyClient
import com.idunnololz.summit.api.dto.CommunityBlockView
import com.idunnololz.summit.api.dto.PersonBlockView
import com.idunnololz.summit.util.StatefulLiveData
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsAccountBlockListViewModel @Inject constructor(
    private val apiClient: AccountAwareLemmyClient,
    private val accountManager: AccountManager,
    private val accountInfoManager: AccountInfoManager,
) : ViewModel() {

    val userBlockList = StatefulLiveData<List<PersonBlockView>>()
    val communityBlockList = StatefulLiveData<List<CommunityBlockView>>()

    init {
        fetchUserBlockList()
    }

    fun fetchUserBlockList() {
        userBlockList.setIsLoading()
        communityBlockList.setIsLoading()

        viewModelScope.launch {
            accountInfoManager.fetchAccountInfo()
                .onSuccess { data ->
                    val account = accountManager.getAccounts().firstOrNull {
                        it.id == data.my_user?.local_user_view?.local_user?.person_id
                    }

                    if (account == null) {
                        RuntimeException(
                            "Unable to find account that matches the account info fetched.").let {

                            userBlockList.postError(it)
                            communityBlockList.postError(it)
                        }
                    } else {
                        userBlockList.postValue(data.my_user?.person_blocks ?: listOf())
                        communityBlockList.postValue(data.my_user?.community_blocks ?: listOf())
                    }
                }
                .onFailure {
                    userBlockList.postError(it)
                    communityBlockList.postError(it)
                }
        }
    }


    data class BlockedPersonItem(
        val blockedPerson: BlockedPersonItem
    )
}