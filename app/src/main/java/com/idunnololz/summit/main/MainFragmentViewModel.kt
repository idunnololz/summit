package com.idunnololz.summit.main

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.idunnololz.summit.user.UserCommunitiesManager
import com.idunnololz.summit.user.UserCommunityItem
import com.idunnololz.summit.util.Event
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainFragmentViewModel @Inject constructor(
    private val userCommunitiesManager: UserCommunitiesManager,
) : ViewModel() {

    val userCommunitiesChangedEvents = MutableLiveData<Event<Unit>>()
    val userCommunitiesUpdated = MutableLiveData<Event<UserCommunityItem>>()

    init {
        viewModelScope.launch {
            userCommunitiesManager.userCommunitiesChangedFlow.collect {
                userCommunitiesChangedEvents.postValue(Event(Unit))
            }
        }
        viewModelScope.launch {
            userCommunitiesManager.userCommunitiesUpdatedFlow.collect {
                userCommunitiesUpdated.postValue(Event(it))
            }
        }
    }
}
