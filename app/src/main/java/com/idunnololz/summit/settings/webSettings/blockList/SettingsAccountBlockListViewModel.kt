package com.idunnololz.summit.settings.webSettings.blockList

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.idunnololz.summit.account.AccountManager
import com.idunnololz.summit.account.info.AccountInfoManager
import com.idunnololz.summit.api.AccountAwareLemmyClient
import com.idunnololz.summit.api.dto.CommunityBlockView
import com.idunnololz.summit.api.dto.CommunityId
import com.idunnololz.summit.api.dto.InstanceBlockView
import com.idunnololz.summit.api.dto.InstanceId
import com.idunnololz.summit.api.dto.MyUserInfo
import com.idunnololz.summit.api.dto.PersonBlockView
import com.idunnololz.summit.api.dto.PersonId
import com.idunnololz.summit.util.StatefulData
import com.idunnololz.summit.util.StatefulLiveData
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.launch

@HiltViewModel
class SettingsAccountBlockListViewModel @Inject constructor(
    private val apiClient: AccountAwareLemmyClient,
    private val accountManager: AccountManager,
    private val accountInfoManager: AccountInfoManager,
) : ViewModel() {

    val userBlockList = StatefulLiveData<List<BlockedPersonItem>>()
    val communityBlockList = StatefulLiveData<List<BlockedCommunityItem>>()
    val instanceBlockList = StatefulLiveData<List<BlockedInstanceItem>>()

    val personChanges = mutableMapOf<PersonId, StatefulData<Unit>>()
    val communityChanges = mutableMapOf<CommunityId, StatefulData<Unit>>()
    val instanceChanges = mutableMapOf<InstanceId, StatefulData<Unit>>()

    private var myUserInfo: MyUserInfo? = null

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
                            "Unable to find account that matches the account info fetched.",
                        ).let {
                            userBlockList.postError(it)
                            communityBlockList.postError(it)
                            instanceBlockList.postError(it)
                        }
                    } else {
                        myUserInfo = data.my_user
                        onDataChanged()
                    }
                }
                .onFailure {
                    userBlockList.postError(it)
                    communityBlockList.postError(it)
                    instanceBlockList.postError(it)
                }
        }
    }

    fun unblockPerson(personId: PersonId) {
        personChanges[personId] = StatefulData.Loading()
        onDataChanged()

        viewModelScope.launch {
            apiClient.blockPerson(personId, false)
                .onFailure {
                    personChanges[personId] = StatefulData.Error(it)
                    onDataChanged()
                }
                .onSuccess {
                    personChanges[personId] = StatefulData.Success(Unit)
                    onDataChanged()
                }
        }
    }

    fun unblockCommunity(communityId: CommunityId) {
        communityChanges[communityId] = StatefulData.Loading()
        onDataChanged()

        viewModelScope.launch {
            apiClient.blockCommunity(communityId, false)
                .onFailure {
                    communityChanges[communityId] = StatefulData.Error(it)
                    onDataChanged()
                }
                .onSuccess {
                    communityChanges[communityId] = StatefulData.Success(Unit)
                    onDataChanged()
                }
        }
    }

    fun unblockInstance(instanceId: InstanceId) {
        instanceChanges[instanceId] = StatefulData.Loading()
        onDataChanged()

        viewModelScope.launch {
            apiClient.blockInstance(instanceId, false)
                .onFailure {
                    instanceChanges[instanceId] = StatefulData.Error(it)
                    onDataChanged()
                }
                .onSuccess {
                    instanceChanges[instanceId] = StatefulData.Success(Unit)
                    onDataChanged()
                }
        }
    }

    private fun onDataChanged() {
        val blockedPersonItems = myUserInfo?.person_blocks?.mapNotNull {
            when (personChanges[it.target.id]) {
                is StatefulData.Error -> BlockedPersonItem(it, false)
                is StatefulData.Loading -> BlockedPersonItem(it, true)
                is StatefulData.NotStarted -> BlockedPersonItem(it, false)
                is StatefulData.Success -> null
                null -> BlockedPersonItem(it, false)
            }
        }
        val blockedCommunityItems = myUserInfo?.community_blocks?.mapNotNull {
            when (communityChanges[it.community.id]) {
                is StatefulData.Error -> BlockedCommunityItem(it, false)
                is StatefulData.Loading -> BlockedCommunityItem(it, true)
                is StatefulData.NotStarted -> BlockedCommunityItem(it, false)
                is StatefulData.Success -> null
                null -> BlockedCommunityItem(it, false)
            }
        }
        val blockedInstanceItems = myUserInfo?.instance_blocks?.mapNotNull {
            when (instanceChanges[it.instance.id]) {
                is StatefulData.Error -> BlockedInstanceItem(it, false)
                is StatefulData.Loading -> BlockedInstanceItem(it, true)
                is StatefulData.NotStarted -> BlockedInstanceItem(it, false)
                is StatefulData.Success -> null
                null -> BlockedInstanceItem(it, false)
            }
        }

        userBlockList.postValue(blockedPersonItems ?: listOf())
        communityBlockList.postValue(blockedCommunityItems ?: listOf())
        instanceBlockList.postValue(blockedInstanceItems ?: listOf())
    }

    data class BlockedPersonItem(
        val blockedPerson: PersonBlockView,
        val isRemoving: Boolean,
    )

    data class BlockedCommunityItem(
        val blockedCommunity: CommunityBlockView,
        val isRemoving: Boolean,
    )

    data class BlockedInstanceItem(
        val blockedInstance: InstanceBlockView,
        val isRemoving: Boolean,
    )
}
