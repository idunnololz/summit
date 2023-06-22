package com.idunnololz.summit.user

import android.content.Context
import android.util.Log
import com.idunnololz.summit.api.LemmyApiClient
import com.idunnololz.summit.coroutine.CoroutineScopeFactory
import com.idunnololz.summit.lemmy.CommunityRef
import com.idunnololz.summit.lemmy.toInstanceAgnosticCommunityRef
import com.idunnololz.summit.preferences.Preferences
import com.squareup.moshi.Moshi
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
    private val lemmyApiClient: LemmyApiClient,
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
    @OptIn(ExperimentalCoroutinesApi::class)
    private val dbContext = Dispatchers.Default.limitedParallelism(1)

    private var nextAvailableSortId: Long = 1

    val userCommunitiesChangedFlow = MutableSharedFlow<Unit>()
    val defaultCommunity = MutableStateFlow(preferences.getDefaultPage())

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
                    userCommunityItems.maxByOrNull { it.sortOrder }
                ).sortOrder + 1

                isLoading = false
            }
        }
    }

    fun getTab(id: Long): UserCommunityItem? = idToUserCommunity[id]

    suspend fun waitForTab(id: Long): UserCommunityItem? = withContext(dbContext) {
        idToUserCommunity[id]
    }

    fun addUserCommunity(communityRef: CommunityRef, icon: String?) {
        var icon = icon
        var communityRef = communityRef
        if (communityRef is CommunityRef.CommunityRefByObj) {
            icon = communityRef.community.icon
        }
        communityRef = communityRef.toInstanceAgnosticCommunityRef()
        val item = UserCommunityItem(communityRef = communityRef, iconUrl = icon)

        coroutineScope.launch {
            withContext(dbContext) {
                addCommunityOrUpdateInternal(item)
            }
            userCommunitiesChangedFlow.emit(Unit)
        }
    }

    fun removeCommunity(communityRef: CommunityRef) {
        val item = userCommunityItems.firstOrNull { it.communityRef == communityRef } ?: return

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
        val ref = currentCommunityRef.toInstanceAgnosticCommunityRef()
        preferences.setDefaultPage(ref)
        onDefaultPageChanged(ref)
    }

    fun isCommunityBookmarked(communityRef: CommunityRef): Boolean =
        userCommunityItems.any { it.communityRef == communityRef }

    private suspend fun addCommunityOrUpdateInternal(
        communityItem: UserCommunityItem
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

    private suspend fun removeCommunityInternal(
        community: UserCommunityItem
    ) = withContext(dbContext) {
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

    private fun makeHomeTab(): UserCommunityItem =
        UserCommunityItem(
            FIRST_FRAGMENT_TAB_ID,
            FIRST_FRAGMENT_SORT_ID,
            communityRef = preferences.getDefaultPage(),
        )

    private suspend fun onDefaultPageChanged(newValue: CommunityRef) {
        addCommunityOrUpdateInternal(makeHomeTab())

        defaultCommunity.emit(newValue)
        userCommunitiesChangedFlow.emit(Unit)
    }

    fun getHomeItem(): UserCommunityItem =
        makeHomeTab()
}