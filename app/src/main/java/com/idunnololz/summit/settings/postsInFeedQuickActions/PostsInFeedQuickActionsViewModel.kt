package com.idunnololz.summit.settings.postsInFeedQuickActions

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.idunnololz.summit.preferences.PostQuickActionId
import com.idunnololz.summit.preferences.PostQuickActionsSettings
import com.idunnololz.summit.preferences.PostsInFeedQuickActionsSettings
import com.idunnololz.summit.preferences.Preferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import javax.inject.Inject
import kotlinx.coroutines.launch

@HiltViewModel
class PostsInFeedQuickActionsViewModel @Inject constructor(
    val preferences: Preferences,
) : ViewModel() {

    var postsInFeedQuickActions = MutableStateFlow(preferences.postsInFeedQuickActions
        ?: PostsInFeedQuickActionsSettings())

    init {
        viewModelScope.launch {
            postsInFeedQuickActions.collect {
                preferences.postsInFeedQuickActions = it
            }
        }
    }

    fun updatePostsInFeedQuickActions(quickActions: List<PostQuickActionId>) {
        viewModelScope.launch {
            postsInFeedQuickActions.value =
                postsInFeedQuickActions.value.copy(actions = quickActions)
        }
    }

    fun setEnabled(enabled: Boolean) {
        viewModelScope.launch {
            postsInFeedQuickActions.value =
                postsInFeedQuickActions.value.copy(enabled = enabled)
        }
    }

    fun resetSettings() {
        viewModelScope.launch {
            postsInFeedQuickActions.value = PostsInFeedQuickActionsSettings()
        }
    }
}
