package com.idunnololz.summit.settings.postQuickActions

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.idunnololz.summit.preferences.PostQuickActionId
import com.idunnololz.summit.preferences.PostQuickActionsSettings
import com.idunnololz.summit.preferences.Preferences
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.launch

@HiltViewModel
class PostQuickActionsViewModel @Inject constructor(
    val preferences: Preferences,
) : ViewModel() {

    val settingsChangedLiveData = MutableLiveData<Unit>()

    fun updatePostQuickActions(quickActions: List<PostQuickActionId>) {
        viewModelScope.launch {
            preferences.postQuickActions = PostQuickActionsSettings(
                quickActions,
            )
        }
    }

    fun resetSettings() {
        viewModelScope.launch {
            preferences.postQuickActions = null

            settingsChangedLiveData.postValue(Unit)
        }
    }
}
