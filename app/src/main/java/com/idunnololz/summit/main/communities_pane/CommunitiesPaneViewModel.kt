package com.idunnololz.summit.main.communities_pane

import android.content.Context
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.idunnololz.summit.R
import com.idunnololz.summit.account.Account
import com.idunnololz.summit.account.info.AccountInfoManager
import com.idunnololz.summit.account.info.AccountSubscription
import com.idunnololz.summit.databinding.CommunitiesPaneBinding
import com.idunnololz.summit.lemmy.CommunityRef
import com.idunnololz.summit.tabs.TabsManager
import com.idunnololz.summit.user.UserCommunitiesManager
import com.idunnololz.summit.user.UserCommunityItem
import com.idunnololz.summit.util.StatefulData
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CommunitiesPaneViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val communitiesPaneControllerFactory: CommunitiesPaneController.Factory,
    private val accountInfoManager: AccountInfoManager,
    private val userCommunitiesManager: UserCommunitiesManager,
    private val tabsManager: TabsManager,
) : ViewModel() {

    private var subscriptionCommunities: List<AccountSubscription> = listOf()
    private var userCommunities: List<UserCommunityItem> = listOf()
    private var tabsState: Map<TabsManager.Tab, TabsManager.TabState> = mapOf()
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

        viewModelScope.launch {
            tabsManager.tabStateChangedFlow.collect {
                updateTabsState()
            }
        }

        updateTabsState()
    }

    fun createController(
        binding: CommunitiesPaneBinding,
        viewLifecycleOwner: LifecycleOwner,
        onCommunitySelected: OnCommunitySelected,
        onEditMultiCommunity: (UserCommunityItem) -> Unit,
        onAddBookmarkClick: () -> Unit,
    ) =
        communitiesPaneControllerFactory.create(
            this,
            binding,
            viewLifecycleOwner,
            onCommunitySelected,
            onEditMultiCommunity,
            onAddBookmarkClick,
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

    private fun updateTabsState() {
        tabsState = tabsManager.getTabState()

        updateCommunities()
    }

    private fun updateCommunities() {
        communities.postValue(
            CommunityData(
                userCommunities = userCommunities,
                subscriptionCommunities = subscriptionCommunities,
                accountInfoUpdateState = accountInfoUpdateState,
                tabsState = tabsState,
            ),
        )
    }

    class CommunityData(
        val subscriptionCommunities: List<AccountSubscription>,
        val userCommunities: List<UserCommunityItem>,
        val accountInfoUpdateState: StatefulData<Account?>,
        val tabsState: Map<TabsManager.Tab, TabsManager.TabState>,
    )
}
