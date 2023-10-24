package com.idunnololz.summit.settings.hiddenPosts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.idunnololz.summit.hidePosts.HiddenPostsManager
import com.idunnololz.summit.util.StatefulLiveData
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HiddenPostsViewModel @Inject constructor(
    private val hiddenPostsManager: HiddenPostsManager,
) : ViewModel() {

    val hiddenPosts = StatefulLiveData<List<HiddenPostsManager.HiddenPost>>()

    init {
        loadHiddenPosts()
    }

    fun loadHiddenPosts() {
        hiddenPosts.setIsLoading()

        viewModelScope.launch {
            val posts = hiddenPostsManager.getAllHiddenPostEntries().sortedByDescending { it.ts }

            hiddenPosts.postValue(posts)
        }
    }

    fun removeHiddenPost(entryId: Long) {
        viewModelScope.launch {
            hiddenPostsManager.removeEntry(entryId)

            loadHiddenPosts()
        }
    }
}