package com.idunnololz.summit.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.idunnololz.summit.util.StatefulLiveData
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val historyManager: HistoryManager,
) : ViewModel() {

    val historyEntriesLiveData = StatefulLiveData<List<LiteHistoryEntry>>()

    fun loadHistory() {
        viewModelScope.launch {
            try {
                val entireHistory = historyManager.getEntireHistory()

                historyEntriesLiveData.postValue(entireHistory)
            } catch (e: Exception) {
                historyEntriesLiveData.postError(e)
            }
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            historyManager.clearHistory()
        }
    }
}