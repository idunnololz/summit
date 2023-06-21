package com.idunnololz.summit.history

import android.content.Context
import com.idunnololz.summit.api.dto.PostView
import com.idunnololz.summit.coroutine.CoroutineScopeFactory
import com.idunnololz.summit.db.MainDatabase
import com.idunnololz.summit.lemmy.CommunityViewState
import com.idunnololz.summit.lemmy.toUrl
import com.idunnololz.summit.user.TabCommunityState
import com.idunnololz.summit.util.Utils
import com.idunnololz.summit.util.moshi
import dagger.hilt.android.qualifiers.ApplicationContext
import io.reactivex.Scheduler
import io.reactivex.Single
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.Executors
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HistoryManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val coroutineScopeFactory: CoroutineScopeFactory,
) {

    interface OnHistoryChangedListener {
        fun onHistoryChanged()
    }

    private val coroutineScope = coroutineScopeFactory.create()
    @OptIn(ExperimentalCoroutinesApi::class)
    private val dbContext = Dispatchers.Default.limitedParallelism(1)

    private val historyChangedListeners = arrayListOf<OnHistoryChangedListener>()

    private val mainDatabase by lazy {
        MainDatabase.getInstance(context)
    }

    suspend fun getEntireHistory(): List<LiteHistoryEntry> = withContext(Dispatchers.IO) {
        mainDatabase
            .historyDao()
            .getAllLiteHistoryEntries()
    }

    fun recordVisit(jsonUrl: String, saveReason: HistorySaveReason, post: PostView?) {
        coroutineScope.launch {
            withContext(dbContext) {
                val ts = System.currentTimeMillis()
                val newEntry = HistoryEntry(
                    id = 0,
                    type = HistoryEntry.TYPE_PAGE_VISIT,
                    reason = saveReason,
                    url = jsonUrl,
                    shortDesc = post?.post?.name ?: "",
                    ts = ts,
                    extras = ""
                )

                recordHistoryEntry(newEntry)
            }
        }
    }

    fun recordSubredditState(
        tabId: Long,
        saveReason: HistorySaveReason,
        state: CommunityViewState,
        shortDesc: String
    ) {
        coroutineScope.launch {
            withContext(dbContext) {
                val ts = System.currentTimeMillis()
                val historyEntry = HistoryEntry(
                    id = 0,
                    type = HistoryEntry.TYPE_SUBREDDIT_STATE,
                    reason = saveReason,
                    url = state.toUrl(),
                    shortDesc = shortDesc,
                    ts = ts,
                    extras = Utils.gson.toJson(TabCommunityState(tabId = tabId, viewState = state))
                )
                recordHistoryEntry(historyEntry)
            }
        }
    }

    fun removeEntry(id: Long) {
        coroutineScope.launch {
            withContext(dbContext) {
                mainDatabase
                    .historyDao()
                    .deleteById(id)
            }
            historyChangedListeners.forEach { it.onHistoryChanged() }
        }
    }

    private suspend fun recordHistoryEntry(newEntry: HistoryEntry) {
        val lastEntry = withContext(Dispatchers.IO) {
            mainDatabase
                .historyDao()
                .getLastHistoryEntryWithType(newEntry.type)
        }


        val entryToInsert = try {
            if (lastEntry?.type != newEntry.type) {
                newEntry
            } else {
                when (newEntry.type) {
                    HistoryEntry.TYPE_PAGE_VISIT ->
                        if (lastEntry.url == newEntry.url) {
                            // just update the last entry
                            lastEntry.copy(
                                ts = newEntry.ts,
                                shortDesc = newEntry.shortDesc
                            )
                        } else {
                            newEntry
                        }

                    HistoryEntry.TYPE_SUBREDDIT_STATE -> {
                        // Url for subreddit state is actually the tab id...
                        val adapter = moshi.adapter(TabCommunityState::class.java)
                        val oldState = adapter.fromJson(lastEntry.extras)
                        val newState = adapter.fromJson(newEntry.extras)
                        if (oldState?.tabId == newState?.tabId &&
                            oldState?.viewState?.communityState?.currentPageIndex ==
                            newState?.viewState?.communityState?.currentPageIndex
                        ) {
                            // just update the last entry
                            lastEntry.copy(
                                ts = newEntry.ts,
                                shortDesc = newEntry.shortDesc,
                                extras = newEntry.extras
                            )
                        } else {
                            newEntry
                        }
                    }

                    else -> newEntry
                }
            }
        } catch (e: Exception) {
            newEntry
        }

        mainDatabase.historyDao()
            .insertHistoryEntry(entryToInsert)

        historyChangedListeners.forEach { it.onHistoryChanged() }
    }

    suspend fun getHistoryEntry(id: Long): HistoryEntry? = withContext(Dispatchers.IO) {
        mainDatabase
            .historyDao()
            .getHistoryEntry(id)
    }

    suspend fun clearHistory() {
        withContext(Dispatchers.IO) {
            mainDatabase
                .historyDao()
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