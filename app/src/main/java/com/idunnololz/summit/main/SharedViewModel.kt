package com.idunnololz.summit.main

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.idunnololz.summit.user.UserCommunitiesManager
import com.idunnololz.summit.user.UserCommunityItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SharedViewModel @Inject constructor(
    private val userCommunitiesManager: UserCommunitiesManager,
) : ViewModel() {
    fun addTab(userCommunityItem: UserCommunityItem) {
        viewModelScope.launch {
            userCommunitiesManager.addUserCommunityItem(userCommunityItem)
        }
    }

    val currentNavController = MutableLiveData<NavController>()
}