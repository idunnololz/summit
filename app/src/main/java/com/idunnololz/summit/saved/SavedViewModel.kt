package com.idunnololz.summit.saved

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.idunnololz.summit.api.AccountAwareLemmyClient
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SavedViewModel @Inject constructor(
    private val apiClient: AccountAwareLemmyClient
) : ViewModel() {

    init {
    }

    fun fetch() {
        viewModelScope.launch {
            Log.d("HAHA", "fetchSavedPosts")
            apiClient.fetchSavedPosts(
                null,
                null,
                1,
                null,
                false
            )
                .onSuccess {
                    Log.d("HAHA", "results: ${it.size}")
                    it.forEach {
                        Log.d("HAHA", "results: $it")
                    }
                }
                .onFailure {
                    Log.d("HAHA", "error!")
                }
        }
    }
}