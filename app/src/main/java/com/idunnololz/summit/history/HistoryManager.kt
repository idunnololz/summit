package com.idunnololz.summit.history

import android.content.Context
import com.idunnololz.summit.api.dto.PostView
import com.idunnololz.summit.db.MainDatabase
import com.idunnololz.summit.lemmy.CommunityViewState
import com.idunnololz.summit.lemmy.toUrl
import com.idunnololz.summit.user.TabCommunityState
import com.idunnololz.summit.util.Utils
import com.idunnololz.summit.util.moshi
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

    fun recordVisit(jsonUrl: String, saveReason: HistorySaveReason, post: PostView?) {
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

    fun recordSubredditState(
        tabId: Long,
        saveReason: HistorySaveReason,
        state: CommunityViewState,
        shortDesc: String
    ) {
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