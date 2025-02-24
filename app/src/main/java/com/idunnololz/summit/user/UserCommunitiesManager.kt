package com.idunnololz.summit.user

import android.util.Log
import com.idunnololz.summit.coroutine.CoroutineScopeFactory
import com.idunnololz.summit.lemmy.CommunityRef
import com.idunnololz.summit.preferences.Preferences
import com.idunnololz.summit.tabs.isHomeTab
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.collections.HashMap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Singleton
class UserCommunitiesManager @Inject constructor(
    private val preferences: Preferences,
    private val coroutineScopeFactory: CoroutineScopeFactory,
    private val userCommunitiesDao: UserCommunitiesDao,
) {
    companion object {
        private const val TAG = "UserCommunitiesManager"

        const val FIRST_FRAGMENT_TAB_ID: Long = 1L
        const val FIRST_FRAGMENT_SORT_ID: Long = 1L
    }

    var isLoading: Boolean = true
        private set

    private val idToUserCommunity = HashMap<Long, UserCommunityItem>()
    private val userCommunityItems = arrayListOf<UserCommunityItem>()

    private val coroutineScope = coroutineScopeFactory.create()

    private val dbContext = Dispatchers.Default.limitedParallelism(1)

    private var nextAvailableSortId: Long = 1

    val userCommunitiesChangedFlow = MutableSharedFlow<Unit>()
    val userCommunitiesUpdatedFlow = MutableSharedFlow<UserCommunityItem>()
    val defaultCommunity = MutableStateFlow(preferences.defaultPage)

    init {
        coroutineScope.launch {
            val communities = userCommunitiesDao.getAllCommunities()
            val userCommunities = listOf(makeHomeTab()) + communities.mapNotNull { it.toItem() }

            withContext(dbContext) {
                addCommunityOrUpdateInternal(makeHomeTab())

                resetTabsDataInternal()

                userCommunities.forEach {
                    addCommunityOrUpdateInternal(it)
                }

                nextAvailableSortId = checkNotNull(
                    userCommunityItems.maxByOrNull { it.sortOrder },
                ).sortOrder + 1

                isLoading = false

                Log.d("dbdb", "userCommunitiesDao: ${userCommunitiesDao.count()}")
            }
        }
    }

    fun getTab(id: Long): UserCommunityItem? = idToUserCommunity[id]

    suspend fun waitForTab(id: Long): UserCommunityItem? = withContext(dbContext) {
        idToUserCommunity[id]
    }

    fun addUserCommunity(communityRef: CommunityRef, icon: String?, dbId: Long = 0L) {
        val item = UserCommunityItem(communityRef = communityRef, iconUrl = icon, id = dbId)

        coroutineScope.launch {
            withContext(dbContext) {
                addCommunityOrUpdateInternal(item)
            }
            userCommunitiesChangedFlow.emit(Unit)
            if (item.id != 0L) {
                userCommunitiesUpdatedFlow.emit(item)
            }
        }
    }

    fun removeCommunity(communityRef: CommunityRef) {
        val item = userCommunityItems.firstOrNull {
            it.communityRef == communityRef && !it.isHomeTab
        } ?: return

        coroutineScope.launch {
            removeCommunityInternal(item)
        }
    }

    suspend fun addUserCommunityItem(communityItem: UserCommunityItem) = withContext(dbContext) {
        addCommunityOrUpdateInternal(communityItem)
    }

    suspend fun deleteUserCommunity(id: Long) = withContext(dbContext) {
        val tab = getTab(id) ?: return@withContext

        removeCommunityInternal(tab)
    }

    fun getAllUserCommunities(): List<UserCommunityItem> = userCommunityItems

    suspend fun setDefaultPage(currentCommunityRef: CommunityRef) {
        preferences.defaultPage = currentCommunityRef
        onDefaultPageChanged(currentCommunityRef)
    }

    fun isCommunityBookmarked(communityRef: CommunityRef): Boolean =
        userCommunityItems.any { it.communityRef == communityRef && !it.isHomeTab }

    private suspend fun addCommunityOrUpdateInternal(
        communityItem: UserCommunityItem,
    ): UserCommunityItem = withContext(dbContext) {
        var newCommunityItem = communityItem

        if (newCommunityItem.sortOrder == 0L) {
            newCommunityItem = newCommunityItem.copy(sortOrder = nextAvailableSortId)
            nextAvailableSortId++
        }

        val newId = userCommunitiesDao.insertCommunity(newCommunityItem.toEntry())

        Log.d(TAG, "newId: $newId")

        if (newCommunityItem.id == 0L) {
            newCommunityItem = newCommunityItem.copy(id = newId)
        }

        idToUserCommunity[newCommunityItem.id] = newCommunityItem

        val oldTabIndex = userCommunityItems.indexOfFirst { it.id == newCommunityItem.id }
        if (oldTabIndex == -1) {
            userCommunityItems.add(newCommunityItem)
        } else {
            userCommunityItems[oldTabIndex] = newCommunityItem
        }

        newCommunityItem
    }

    private suspend fun removeCommunityInternal(community: UserCommunityItem) =
        withContext(dbContext) {
            if (community.id == FIRST_FRAGMENT_TAB_ID) {
                return@withContext
            }

            idToUserCommunity.remove(community.id)
            userCommunityItems.remove(community)

            userCommunitiesDao.delete(community.id)

            userCommunitiesChangedFlow.emit(Unit)
        }

    private fun resetTabsDataInternal() {
        idToUserCommunity.clear()
        userCommunityItems.clear()
    }

    private fun makeHomeTab(): UserCommunityItem = UserCommunityItem(
        id = FIRST_FRAGMENT_TAB_ID,
        sortOrder = FIRST_FRAGMENT_SORT_ID,
        communityRef = preferences.defaultPage,
    )

    private suspend fun onDefaultPageChanged(newValue: CommunityRef) {
        addCommunityOrUpdateInternal(makeHomeTab())

        defaultCommunity.emit(newValue)
        userCommunitiesChangedFlow.emit(Unit)
    }

    fun getHomeItem(): UserCommunityItem = makeHomeTab()
}
