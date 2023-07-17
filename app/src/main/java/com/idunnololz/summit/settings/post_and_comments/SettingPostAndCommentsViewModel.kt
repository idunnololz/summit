package com.idunnololz.summit.settings.post_and_comments

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.idunnololz.summit.lemmy.postListView.PostAndCommentsUiConfig
import com.idunnololz.summit.lemmy.postListView.getDefaultPostAndCommentsUiConfig
import com.idunnololz.summit.preferences.Preferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingPostAndCommentsViewModel @Inject constructor(
    val preferences: Preferences
) : ViewModel() {

    var currentPostAndCommentUiConfig: PostAndCommentsUiConfig = preferences.getPostAndCommentsUiConfig()
        set(value) {
            field = value

            viewModelScope.launch {
                applyConfig()
            }
        }

    val onPostUiChanged = MutableLiveData<Unit>()

    fun onLayoutChanging() {
        applyConfig()
    }

    fun onLayoutChanged() {
        currentPostAndCommentUiConfig = preferences.getPostAndCommentsUiConfig()
        onPostUiChanged.value = Unit
    }

    fun resetPostUiConfig() {
        currentPostAndCommentUiConfig = currentPostAndCommentUiConfig.copy(
            postUiConfig = getDefaultPostAndCommentsUiConfig().postUiConfig
        )
        onPostUiChanged.value = Unit
    }

    fun resetCommentUiConfig() {
        currentPostAndCommentUiConfig = currentPostAndCommentUiConfig.copy(
            commentUiConfig = getDefaultPostAndCommentsUiConfig().commentUiConfig
        )
        onPostUiChanged.value = Unit
    }

    private fun applyConfig() {
        preferences.setPostAndCommentsUiConfig(currentPostAndCommentUiConfig)
    }

    override fun onCleared() {
        applyConfig()

        super.onCleared()
    }
}