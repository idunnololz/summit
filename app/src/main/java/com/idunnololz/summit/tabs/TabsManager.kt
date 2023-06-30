package com.idunnololz.summit.tabs

import androidx.lifecycle.MutableLiveData
import arrow.core.Either
import com.idunnololz.summit.coroutine.CoroutineScopeFactory
import com.idunnololz.summit.lemmy.CommunityRef
import com.idunnololz.summit.user.UserCommunitiesManager
import com.idunnololz.summit.user.UserCommunityItem
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TabsManager @Inject constructor(
    private val coroutineScopeFactory: CoroutineScopeFactory,
    private val userCommunitiesManager: UserCommunitiesManager,
) {

    private val coroutineScope = coroutineScopeFactory.create()

    val currentTab = MutableLiveData<Tab>(userCommunitiesManager.getHomeItem().toTab())

    val previousTabs = linkedSetOf<Tab>()

    init {
        coroutineScope.launch {
            userCommunitiesManager.defaultCommunity.collect {
                val curTab = currentTab.value ?: return@collect

                if (curTab.isHomeTab) {
                    val newHome = userCommunitiesManager.getHomeItem()
                    if (curTab.communityRef != newHome.communityRef) {
                        updateCurrentTab(newHome.toTab())
                    }
                }
            }
        }
    }

    fun updateCurrentTab(tabObj: Either<UserCommunityItem, CommunityRef>) {
        updateCurrentTab(tabObj.fold(
            {
                it.toTab()
            },
            {
                it.toTab()
            }
        ))
    }

    fun updateCurrentTab(tab: Tab) {
        currentTab.value?.let {
            previousTabs.add(it)
        }

        currentTab.postValue(tab)
    }

    fun getHomeTab(): Tab =
        userCommunitiesManager.getHomeItem().toTab()

    sealed interface Tab {
        data class UserCommunityTab(
            val userCommunityItem: UserCommunityItem
        ) : Tab

        data class SubscribedCommunityTab(
            val subscribedCommunity: CommunityRef
        ) : Tab
    }
}

private fun UserCommunityItem.toTab(): TabsManager.Tab =
    TabsManager.Tab.UserCommunityTab(this)

private fun CommunityRef.toTab(): TabsManager.Tab =
    TabsManager.Tab.SubscribedCommunityTab(this)

val TabsManager.Tab.communityRef: CommunityRef
    get() = when (this) {
        is TabsManager.Tab.SubscribedCommunityTab -> this.subscribedCommunity
        is TabsManager.Tab.UserCommunityTab -> this.userCommunityItem.communityRef
    }

val TabsManager.Tab.isHomeTab: Boolean
    get() = when (this) {
        is TabsManager.Tab.SubscribedCommunityTab -> false
        is TabsManager.Tab.UserCommunityTab ->
            this.userCommunityItem.id == UserCommunitiesManager.FIRST_FRAGMENT_TAB_ID
    }


fun TabsManager.Tab.hasTabId(id: Long): Boolean =
    when (this) {
        is TabsManager.Tab.SubscribedCommunityTab -> false
        is TabsManager.Tab.UserCommunityTab ->
            this.userCommunityItem.id == id
    }

fun TabsManager.Tab.isSubscribedCommunity(communityRef: CommunityRef): Boolean =
    when (this) {
        is TabsManager.Tab.SubscribedCommunityTab -> this.communityRef == communityRef
        is TabsManager.Tab.UserCommunityTab -> false
    }