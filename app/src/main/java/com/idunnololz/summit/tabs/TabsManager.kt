package com.idunnololz.summit.tabs

import android.os.Parcelable
import androidx.lifecycle.MutableLiveData
import arrow.core.Either
import com.idunnololz.summit.coroutine.CoroutineScopeFactory
import com.idunnololz.summit.lemmy.CommunityRef
import com.idunnololz.summit.user.UserCommunitiesManager
import com.idunnololz.summit.user.UserCommunityItem
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
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

    val tabStateChangedFlow = MutableSharedFlow<Unit>()

    private val tabState = mutableMapOf<Tab, TabState>()

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

    fun getTab(tabObj: Either<UserCommunityItem, CommunityRef>) =
        tabObj.fold(
            {
                it.toTab()
            },
            {
                it.toTab()
            },
        )

    fun updateCurrentTab(tabObj: Either<UserCommunityItem, CommunityRef>) {
        updateCurrentTab(getTab(tabObj))
    }

    fun updateCurrentTab(tab: Tab) {
        currentTab.value?.let {
            previousTabs.add(it)
        }

        currentTab.postValue(tab)
    }

    fun updateCurrentTabNow(tab: Tab) {
        currentTab.value?.let {
            previousTabs.add(it)
        }

        currentTab.value = tab
    }

    fun getHomeTab(): Tab =
        userCommunitiesManager.getHomeItem().toTab()

    fun getTabState(): Map<Tab, TabState> =
        tabState

    fun updateTabState(tab: Tab, communityRef: CommunityRef) {
        val newTabState = (tabState[tab] ?: makeDefaultState(tab)).copy(
            currentCommunity = communityRef,
        )
        tabState[tab] = newTabState

        coroutineScope.launch {
            tabStateChangedFlow.emit(Unit)
        }
    }

    private fun makeDefaultState(tab: Tab): TabState =
        TabState(tab.communityRef)

    sealed interface Tab : Parcelable {

        @Parcelize
        data class UserCommunityTab(
            val userCommunityItem: UserCommunityItem,
        ) : Tab

        @Parcelize
        data class SubscribedCommunityTab(
            val subscribedCommunity: CommunityRef,
        ) : Tab
    }

    data class TabState(
        val currentCommunity: CommunityRef,
    )
}

fun UserCommunityItem.toTab(): TabsManager.Tab =
    TabsManager.Tab.UserCommunityTab(this)

fun CommunityRef.toTab(): TabsManager.Tab =
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
            this.userCommunityItem.isHomeTab
    }

val UserCommunityItem.isHomeTab: Boolean
    get() =
        this.id == UserCommunitiesManager.FIRST_FRAGMENT_TAB_ID

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
