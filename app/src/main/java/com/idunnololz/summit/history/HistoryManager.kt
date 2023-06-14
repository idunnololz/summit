package com.idunnololz.summit.history

import android.content.Context
import com.idunnololz.summit.db.MainDatabase
import com.idunnololz.summit.reddit.SubredditViewState
import com.idunnololz.summit.reddit_objects.ListingItem
import com.idunnololz.summit.tabs.TabSubredditState
import com.idunnololz.summit.util.Utils
import io.reactivex.Scheduler
import io.reactivex.Single
import io.reactivex.schedulers.Schedulers
import java.util.concurrent.Executors

class HistoryManager(
    private val context: Context
) {
    companion object {
        const val FIRST_FRAGMENT_TAB_ID: Long = 0

        lateinit var instance: HistoryManager

        fun initialize(context: Context) {
            instance = HistoryManager(context)
        }
    }

    interface OnHistoryChangedListener {
        fun onHistoryChanged()
    }

    private val scheduler: Scheduler = Schedulers.from(Executors.newSingleThreadExecutor())

    private val historyChangedListeners = arrayListOf<OnHistoryChangedListener>()

    private val mainDatabase by lazy {
        MainDatabase.getInstance(context)
    }

    fun getEntireHistory(): Single<List<LiteHistoryEntry>> =
        mainDatabase
            .historyDao()
            .getAllLiteHistoryEntries()
            .subscribeOn(scheduler)

    fun recordVisit(jsonUrl: String, saveReason: HistorySaveReason, originalPost: ListingItem?) {
        val ts = System.currentTimeMillis()
        val newEntry = HistoryEntry(
            id = 0,
            type = HistoryEntry.TYPE_PAGE_VISIT,
            reason = saveReason,
            url = jsonUrl,
            shortDesc = originalPost?.title ?: "",
            ts = ts,
            extras = ""
        )

        recordHistoryEntry(newEntry)
    }

    fun recordSubredditState(
        tabId: String,
        saveReason: HistorySaveReason,
        state: SubredditViewState,
        shortDesc: String
    ) {
        val ts = System.currentTimeMillis()
        val historyEntry = HistoryEntry(
            id = 0,
            type = HistoryEntry.TYPE_SUBREDDIT_STATE,
            reason = saveReason,
            url = state.subredditState.currentUrl,
            shortDesc = shortDesc,
            ts = ts,
            extras = Utils.gson.toJson(TabSubredditState(tabId = tabId, viewState = state))
        )
        recordHistoryEntry(historyEntry)
    }

    fun removeEntry(id: Long) {
        mainDatabase
            .historyDao()
            .deleteById(id)
            .doAfterTerminate {
                historyChangedListeners.forEach { it.onHistoryChanged() }
            }
            .subscribeOn(scheduler)
            .subscribe()
    }

    private fun recordHistoryEntry(newEntry: HistoryEntry) {
        val gson = Utils.gson

        mainDatabase
            .historyDao()
            .getLastHistoryEntryWithType(newEntry.type)
            .map a@{ lastEntry ->
                if (lastEntry.type != newEntry.type) {
                    return@a newEntry
                }

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
                        val oldState =
                            gson.fromJson(lastEntry.extras, TabSubredditState::class.java)
                        val newState = gson.fromJson(newEntry.extras, TabSubredditState::class.java)
                        if (oldState.tabId == newState.tabId &&
                            oldState.viewState.subredditState.currentPageIndex == newState.viewState.subredditState.currentPageIndex
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
            .onErrorReturn { newEntry }
            .flatMap { entryToInsert ->
                mainDatabase.historyDao()
                    .insertHistoryEntry(entryToInsert)
            }
            .doAfterTerminate {
                historyChangedListeners.forEach { it.onHistoryChanged() }
            }
            .subscribeOn(scheduler)
            .subscribe()
    }

    fun getHistoryEntry(id: Long): Single<HistoryEntry> =
        mainDatabase
            .historyDao()
            .getHistoryEntry(id)
            .subscribeOn(scheduler)

    fun clearHistory() {
        mainDatabase
            .historyDao()
            .deleteAllHistoryEntries()
            .doAfterTerminate {
                historyChangedListeners.forEach { it.onHistoryChanged() }
            }
            .subscribeOn(scheduler)
            .subscribe()
    }

    fun registerOnHistoryChangedListener(l: OnHistoryChangedListener) {
        historyChangedListeners.add(l)
    }

    fun unregisterOnHistoryChangedListener(l: OnHistoryChangedListener) {
        historyChangedListeners.remove(l)
    }
}