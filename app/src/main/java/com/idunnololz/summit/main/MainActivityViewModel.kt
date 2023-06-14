package com.idunnololz.summit.main

import androidx.lifecycle.ViewModel
import com.idunnololz.summit.reddit.SubredditLoader
import com.idunnololz.summit.reddit_objects.SubredditItem
import com.idunnololz.summit.scrape.LoaderException
import com.idunnololz.summit.util.StatefulLiveData

class MainActivityViewModel : ViewModel() {

    private val subredditLoader = SubredditLoader()

    val subredditsLiveData = StatefulLiveData<List<SubredditItem>>()

    fun loadSubreddits(force: Boolean = false) {
        subredditLoader.fetchSubreddits(force, {
            subredditsLiveData.postIsLoading()
        }, { subreddits ->
            subredditsLiveData.postValue(subreddits)
        }, {
            subredditsLiveData.postError(LoaderException(it))
        })
    }

    override fun onCleared() {
        super.onCleared()

        subredditLoader.destroy()
    }
}