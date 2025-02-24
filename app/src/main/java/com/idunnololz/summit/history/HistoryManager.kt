package com.idunnololz.summit.history

import android.content.Context
import android.util.Log
import com.idunnololz.summit.api.AccountAwareLemmyClient
import com.idunnololz.summit.api.dto.PostView
import com.idunnololz.summit.coroutine.CoroutineScopeFactory
import com.idunnololz.summit.lemmy.CommunityRef
import com.idunnololz.summit.lemmy.CommunityViewState
import com.idunnololz.summit.lemmy.toUrl
import com.idunnololz.summit.preferences.Preferences
import com.idunnololz.summit.user.TabCommunityState
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

@Singleton
class HistoryManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val coroutineScopeFactory: CoroutineScopeFactory,
    private val historyDao: HistoryDao,
    private val apiClient: AccountAwareLemmyClient,
    private val preferences: Preferences,
    private val json: Json,
) {

    companion object {
        private const val TAG = "HistoryManager"
    }

    interface OnHistoryChangedListener {
        fun onHistoryChanged()
    }

    private val coroutineScope = coroutineScopeFactory.create()

    @OptIn(ExperimentalCoroutinesApi::class)
    private val dbContext = Dispatchers.IO.limitedParallelism(1)

    private val historyChangedListeners = arrayListOf<OnHistoryChangedListener>()

    init {
        coroutineScope.launch {
            Log.d("dbdb", "historyDao: ${historyDao.count()}")
        }
    }

    suspend fun getEntireHistory(): List<LiteHistoryEntry> = withContext(Dispatchers.IO) {
        historyDao
            .getAllLiteHistoryEntries()
    }

    suspend fun query(query: String): List<LiteHistoryEntry> = withContext(Dispatchers.IO) {
        historyDao.query(query)
    }

    suspend fun getHistoryFrom(ts: Long): List<LiteHistoryEntry> = withContext(Dispatchers.IO) {
        historyDao.getLiteHistoryEntriesFrom(ts)
    }

    fun recordVisit(jsonUrl: String, saveReason: HistorySaveReason, post: PostView?) {
        coroutineScope.launch {
            if (!preferences.trackBrowsingHistory) {
                return@launch
            }

            withContext(dbContext) {
                val ts = System.currentTimeMillis()
                val newEntry = HistoryEntry(
                    id = 0,
                    type = HistoryEntry.TYPE_PAGE_VISIT,
                    reason = saveReason,
                    url = jsonUrl,
                    shortDesc = post?.post?.name ?: "",
                    ts = ts,
                    extras = "",
                )

                recordHistoryEntry(newEntry)
            }
        }
    }

    fun recordCommunityState(
        tabId: Long,
        saveReason: HistorySaveReason,
        state: CommunityViewState,
        shortDesc: String,
    ) {
        coroutineScope.launch {
            if (!preferences.trackBrowsingHistory) {
                return@launch
            }

            withContext(dbContext) {
                if (state.communityState.communityRef is CommunityRef.MultiCommunity) {
                    // Multi-communities are purely client sided so we cannot record history for them.
                    return@withContext
                }

                val ts = System.currentTimeMillis()
                val historyEntry = HistoryEntry(
                    id = 0,
                    type = HistoryEntry.TYPE_COMMUNITY_STATE,
                    reason = saveReason,
                    url = state.toUrl(apiClient.instance),
                    shortDesc = shortDesc,
                    ts = ts,
                    extras = json.encodeToString(
                        TabCommunityState(
                            tabId = tabId,
                            viewState = state,
                        ),
                    ),
                )
                recordHistoryEntry(historyEntry)
            }
        }
    }

    fun removeEntry(id: Long) {
        coroutineScope.launch {
            withContext(dbContext) {
                historyDao
                    .deleteById(id)
            }
            historyChangedListeners.forEach { it.onHistoryChanged() }
        }
    }

    private suspend fun recordHistoryEntry(newEntry: HistoryEntry) {
        withContext(dbContext) {
            historyDao.insertEntryMergeWithPreviousIfSame(json, newEntry)
        }

        withContext(Dispatchers.Default) {
            historyChangedListeners.forEach { it.onHistoryChanged() }
        }
    }

    suspend fun getHistoryEntry(id: Long): HistoryEntry? = withContext(Dispatchers.IO) {
        historyDao
            .getHistoryEntry(id)
    }

    suspend fun clearHistory() {
        withContext(Dispatchers.IO) {
            historyDao
                .deleteAllHistoryEntries()
        }

        historyChangedListeners.forEach { it.onHistoryChanged() }
    }

    fun registerOnHistoryChangedListener(l: OnHistoryChangedListener) {
        historyChangedListeners.add(l)
    }

    fun unregisterOnHistoryChangedListener(l: OnHistoryChangedListener) {
        historyChangedListeners.remove(l)
    }
}
