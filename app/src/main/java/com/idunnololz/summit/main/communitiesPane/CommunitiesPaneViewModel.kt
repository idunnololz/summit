package com.idunnololz.summit.main.communitiesPane

import android.content.Context
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.idunnololz.summit.account.Account
import com.idunnololz.summit.account.info.AccountInfoManager
import com.idunnololz.summit.account.info.AccountSubscription
import com.idunnololz.summit.api.dto.Community
import com.idunnololz.summit.databinding.CommunitiesPaneBinding
import com.idunnololz.summit.lemmy.toCommunityRef
import com.idunnololz.summit.tabs.TabsManager
import com.idunnololz.summit.user.UserCommunitiesManager
import com.idunnololz.summit.user.UserCommunityItem
import com.idunnololz.summit.util.StatefulData
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@HiltViewModel
class CommunitiesPaneViewModel @Inject constructor(
  @ApplicationContext private val context: Context,
  private val communitiesPaneControllerFactory: CommunitiesPaneController.Factory,
  private val accountInfoManager: AccountInfoManager,
  private val userCommunitiesManager: UserCommunitiesManager,
  private val tabsManager: TabsManager,
) : ViewModel() {

  private var subscriptionCommunities: List<AccountSubscription> = listOf()
  private var moderatedCommunities: List<Community> = listOf()
  private var userCommunities: List<UserCommunityItem> = listOf()
  private var tabsState: Map<TabsManager.Tab, TabsManager.TabState> = mapOf()
  private var accountInfoUpdateState: StatefulData<Account?> = StatefulData.NotStarted()

  val communities = MutableLiveData<CommunityData?>(null)

  init {
    viewModelScope.launch(Dispatchers.Default) {
      accountInfoManager.subscribedCommunities.collect {
        subscriptionCommunities = it.sortedBy {
          it.toCommunityRef().getLocalizedFullName(context)
        }

        updateCommunities()
      }
    }
    viewModelScope.launch(Dispatchers.Default) {
      accountInfoManager.moderatedCommunities.collect {
        moderatedCommunities = it.sortedBy {
          it.toCommunityRef().getLocalizedFullName(context)
        }

        updateCommunities()
      }
    }

    viewModelScope.launch(Dispatchers.Default) {
      userCommunitiesManager.userCommunitiesChangedFlow.collect {
        userCommunities = userCommunitiesManager.getAllUserCommunities()

        updateCommunities()
      }
    }

    viewModelScope.launch(Dispatchers.Default) {
      accountInfoManager.accountInfoUpdateState.collect {
        accountInfoUpdateState = it

        updateCommunities()
      }
    }

    viewModelScope.launch(Dispatchers.Default) {
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
    onLongClick: (url: String, text: String?) -> Unit,
  ) = communitiesPaneControllerFactory.create(
    this,
    binding,
    viewLifecycleOwner,
    onCommunitySelected,
    onEditMultiCommunity,
    onAddBookmarkClick,
    onLongClick,
  )

  fun loadCommunities(force: Boolean) {
    userCommunities = userCommunitiesManager.getAllUserCommunities()
    accountInfoManager.refreshAccountInfo(force)
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
        moderatedCommunities = moderatedCommunities,
      ),
    )
  }

  class CommunityData(
    val subscriptionCommunities: List<AccountSubscription>,
    val userCommunities: List<UserCommunityItem>,
    val accountInfoUpdateState: StatefulData<Account?>,
    val tabsState: Map<TabsManager.Tab, TabsManager.TabState>,
    val moderatedCommunities: List<Community>,
  )
}
