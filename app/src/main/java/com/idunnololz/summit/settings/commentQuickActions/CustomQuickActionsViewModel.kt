package com.idunnololz.summit.settings.commentQuickActions

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.idunnololz.summit.preferences.CommentQuickActionId
import com.idunnololz.summit.preferences.CommentQuickActionsSettings
import com.idunnololz.summit.preferences.Preferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CustomQuickActionsViewModel @Inject constructor(
    val preferences: Preferences,
) : ViewModel() {

    val settingsChangedLiveData = MutableLiveData<Unit>()

    fun updateCommentQuickActions(quickActions: List<CommentQuickActionId>) {
        viewModelScope.launch {
            preferences.commentQuickActions = CommentQuickActionsSettings(
                quickActions,
            )
        }
    }

    fun resetSettings() {
        viewModelScope.launch {
            preferences.commentQuickActions = null

            settingsChangedLiveData.postValue(Unit)
        }
    }
}
