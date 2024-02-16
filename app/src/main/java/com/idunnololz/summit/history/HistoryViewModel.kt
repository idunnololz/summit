package com.idunnololz.summit.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.idunnololz.summit.api.LemmyApiClient
import com.idunnololz.summit.util.StatefulLiveData
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch
import okhttp3.internal.toImmutableList
import javax.inject.Inject

@OptIn(FlowPreview::class)
@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val apiClient: LemmyApiClient,
    private val historyManager: HistoryManager,
) : ViewModel() {

    private val entries = mutableListOf<LiteHistoryEntry>()
    private val seenIds = mutableSetOf<Long>()
    private var lastTs = 0L
    private var hasMore = true

    private val query = MutableSharedFlow<String>()

    val historyData = StatefulLiveData<HistoryEntryData>()
    val historyQueryData = StatefulLiveData<HistoryQueryResult>()
    val instance: String
        get() = apiClient.instance

    init {
        viewModelScope.launch(Dispatchers.Default) {
            query.debounce(500)
                .collect {
                    queryInternal(it)
                }
        }
    }

    fun loadHistory(force: Boolean = false) {
        viewModelScope.launch(Dispatchers.Default) {
            try {
                if (force) {
                    entries.clear()
                    seenIds.clear()
                    lastTs = 0L
                    hasMore = true
                }

                fetchMore()

                historyData.postValue(
                    HistoryEntryData(
                        entries.toImmutableList(),
                        hasMore,
                    ),
                )
            } catch (e: Exception) {
                historyData.postError(e)
            }
        }
    }

    fun query(query: String) {
        viewModelScope.launch(Dispatchers.Default) {
            this@HistoryViewModel.query.emit(query)
        }
    }

    private fun queryInternal(query: String) {
        viewModelScope.launch(Dispatchers.Default) {
            try {
                val searchEntries = historyManager.query(query)

                historyQueryData.postValue(
                    HistoryQueryResult(
                        query,
                        searchEntries,
                    ),
                )
            } catch (e: Exception) {
                historyData.postError(e)
            }
        }
    }

    private suspend fun fetchMore() {
        val startTs = if (lastTs == 0L) {
            Long.MAX_VALUE
        } else {
            lastTs
        }
        val entries = historyManager.getHistoryFrom(startTs)
        addEntries(entries)

        lastTs = this.entries.lastOrNull()?.ts ?: 0L
        hasMore = entries.size == 1000
    }

    private fun addEntries(entireHistory: List<LiteHistoryEntry>) {
        for (entry in entireHistory) {
            if (seenIds.contains(entry.id)) continue

            entries.add(entry)
        }

        entries.sortedByDescending { it.ts }
    }

    fun clearHistory() {
        viewModelScope.launch(Dispatchers.Default) {
            historyManager.clearHistory()
        }
    }

    fun removeEntry(entryId: Long) {
        historyManager.removeEntry(entryId)
    }

    data class HistoryEntryData(
        val sortedEntries: List<LiteHistoryEntry>,
        val hasMore: Boolean,
    )

    data class HistoryQueryResult(
        val query: String,
        val sortedEntries: List<LiteHistoryEntry>,
    )
}
