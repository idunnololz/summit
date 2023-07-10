package com.idunnololz.summit.main.communities_pane

import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.idunnololz.summit.account.Account
import com.idunnololz.summit.account.info.AccountInfo
import com.idunnololz.summit.account.info.AccountInfoManager
import com.idunnololz.summit.account.info.AccountSubscription
import com.idunnololz.summit.databinding.CommunitiesPaneBinding
import com.idunnololz.summit.user.UserCommunitiesManager
import com.idunnololz.summit.user.UserCommunityItem
import com.idunnololz.summit.util.StatefulData
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CommunitiesPaneViewModel @Inject constructor(
    private val communitiesPaneControllerFactory: CommunitiesPaneController.Factory,
    private val accountInfoManager: AccountInfoManager,
    private val userCommunitiesManager: UserCommunitiesManager,
) : ViewModel() {

    private var subscriptionCommunities: List<AccountSubscription> = listOf()
    private var userCommunities: List<UserCommunityItem> = listOf()
    private var accountInfoUpdateState: StatefulData<Account?> = StatefulData.NotStarted()

    val communities = MutableLiveData<CommunityData?>(null)

    init {
        viewModelScope.launch {
            accountInfoManager.subscribedCommunities.collect {
                subscriptionCommunities = it

                updateCommunities()
            }
        }

        viewModelScope.launch {
            userCommunitiesManager.userCommunitiesChangedFlow.collect {
                userCommunities = userCommunitiesManager.getAllUserCommunities()

                updateCommunities()
            }
        }

        viewModelScope.launch {
            accountInfoManager.accountInfoUpdateState.collect {
                accountInfoUpdateState = it

                updateCommunities()
            }
        }
    }

    fun createController(
        binding: CommunitiesPaneBinding,
        viewLifecycleOwner: LifecycleOwner,
        onCommunitySelected: OnCommunitySelected,
    ) =
        communitiesPaneControllerFactory.create(
            this,
            binding,
            viewLifecycleOwner,
            onCommunitySelected,
        )

    fun loadCommunities() {
        userCommunities = userCommunitiesManager.getAllUserCommunities()
        accountInfoManager.refreshAccountInfo()
        updateCommunities()
    }

    fun deleteUserCommunity(id: Long) {
        viewModelScope.launch {
            userCommunitiesManager.deleteUserCommunity(id)
        }
    }

    private fun updateCommunities() {
        communities.postValue(CommunityData(
            userCommunities = userCommunities,
            subscriptionCommunities = subscriptionCommunities,
            accountInfoUpdateState = accountInfoUpdateState,
        ))
    }

    class CommunityData(
        val subscriptionCommunities: List<AccountSubscription>,
        val userCommunities: List<UserCommunityItem>,
        val accountInfoUpdateState: StatefulData<Account?>,
    )
}