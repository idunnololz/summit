package com.idunnololz.summit.history

import androidx.lifecycle.ViewModel
import com.idunnololz.summit.util.StatefulLiveData
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable

class HistoryViewModel : ViewModel() {

    val historyEntriesLiveData = StatefulLiveData<List<LiteHistoryEntry>>()

    val disposables = CompositeDisposable()

    fun loadHistory() {
        val d = HistoryManager.instance.getEntireHistory()
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                historyEntriesLiveData.postValue(it)
            }, {
                historyEntriesLiveData.postError(it)
            })
        disposables.add(d)
    }

    override fun onCleared() {
        super.onCleared()

        disposables.clear()
    }
}