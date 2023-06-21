package com.idunnololz.summit.user

import android.content.Context
import androidx.lifecycle.MutableLiveData
import com.idunnololz.summit.coroutine.CoroutineScopeFactory
import com.idunnololz.summit.lemmy.CommunityRef
import com.idunnololz.summit.preferences.Preferences
import com.squareup.moshi.Moshi
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.collections.HashMap

@Singleton
class UserCommunitiesManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val preferences: Preferences,
    private val moshi: Moshi,
    private val coroutineScopeFactory: CoroutineScopeFactory,
    private val userCommunitiesDao: UserCommunitiesDao,
) {
    companion object {
        const val FIRST_FRAGMENT_TAB_ID: Long = 1L
        const val FIRST_FRAGMENT_SORT_ID: Long = 1L
    }

    var isLoading: Boolean = true
        private set

    private val idToUserCommunity = HashMap<Long, UserCommunityItem>()
    private val allTabs = arrayListOf<UserCommunityItem>()

    private val coroutineScope = coroutineScopeFactory.create()
    @OptIn(ExperimentalCoroutinesApi::class)
    private val dbContext = Dispatchers.Default.limitedParallelism(1)

    private var nextAvailableSortId: Long = 1

    val userCommunitiesChangedLiveData = MutableLiveData(Unit)
    val currentTabId = MutableLiveData(FIRST_FRAGMENT_TAB_ID)
    val defaultCommunity = MutableStateFlow(preferences.getDefaultPage())

    init {
        coroutineScope.launch {
            val communities = userCommunitiesDao.getAllCommunities()
            val userCommunities = listOf(makeHomeTab()) + communities.mapNotNull { it.toItem() }

            withContext(dbContext) {
                addCommunityOrUpdateInternal(makeHomeTab())

                clearTabsInternal()

                userCommunities.forEach {
                    addCommunityOrUpdateInternal(it)
                }

                nextAvailableSortId = checkNotNull(allTabs.maxByOrNull { it.sortOrder }).sortOrder + 1

                isLoading = false
                userCommunitiesChangedLiveData.postValue(Unit)
            }
        }
    }

    fun getTab(id: Long): UserCommunityItem? = idToUserCommunity[id]

    suspend fun waitForTab(id: Long): UserCommunityItem? = withContext(dbContext) {
        idToUserCommunity[id]
    }

    suspend fun addUserCommunity(communityRef: CommunityRef) = withContext(dbContext) {
        val item = UserCommunityItem(communityRef = communityRef)

        addCommunityOrUpdateInternal(item)
    }

    suspend fun addUserCommunityItem(communityItem: UserCommunityItem) = withContext(dbContext) {
        addCommunityOrUpdateInternal(communityItem)
    }

    suspend fun deleteUserCommunity(id: Long) = withContext(dbContext) {
        val tab = getTab(id) ?: return@withContext

        removeCommunityInternal(tab)
    }

    fun getAllUserCommunities(): List<UserCommunityItem> = allTabs

    suspend fun setDefaultPage(currentCommunityRef: CommunityRef) {
        preferences.setDefaultPage(currentCommunityRef)
        onDefaultPageChanged(currentCommunityRef)
    }

    private suspend fun addCommunityOrUpdateInternal(
        communityItem: UserCommunityItem
    ): UserCommunityItem = withContext(dbContext) {
        var newCommunityItem = communityItem

        if (newCommunityItem.sortOrder == 0L) {
            newCommunityItem = newCommunityItem.copy(sortOrder = nextAvailableSortId)
            nextAvailableSortId++
        }

        val newId = userCommunitiesDao.insertCommunity(communityItem.toEntry())

        if (newCommunityItem.id == 0L) {
            newCommunityItem = newCommunityItem.copy(id = newId)
        }

        idToUserCommunity[communityItem.id] = communityItem

        val oldTabIndex = allTabs.indexOfFirst { it.id == communityItem.id }
        if (oldTabIndex == -1) {
            allTabs.add(communityItem)
        } else {
            allTabs[oldTabIndex] = communityItem
        }

        newCommunityItem
    }

    private suspend fun removeCommunityInternal(community: UserCommunityItem) = withContext(dbContext) {
        idToUserCommunity.remove(community.id)
        allTabs.remove(community)

        if (currentTabId.value == community.id) {
            currentTabId.value = FIRST_FRAGMENT_TAB_ID
        }

        userCommunitiesDao.delete(community.id)
    }

    private fun clearTabsInternal() {
        idToUserCommunity.clear()
        allTabs.clear()
    }

    private fun makeHomeTab(): UserCommunityItem =
        UserCommunityItem(
            FIRST_FRAGMENT_TAB_ID,
            FIRST_FRAGMENT_SORT_ID,
            communityRef = preferences.getDefaultPage(),
        )

    private suspend fun onDefaultPageChanged(newValue: CommunityRef) {
        addCommunityOrUpdateInternal(makeHomeTab())

        defaultCommunity.emit(newValue)
    }
}