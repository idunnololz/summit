package com.idunnololz.summit.settings.postAndCommentsAppearance

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.idunnololz.summit.lemmy.postListView.PostAndCommentsUiConfig
import com.idunnololz.summit.lemmy.postListView.getDefaultPostAndCommentsUiConfig
import com.idunnololz.summit.preferences.Preferences
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.launch

@HiltViewModel
class SettingsPostAndCommentsAppearanceViewModel @Inject constructor(
    val preferences: Preferences,
) : ViewModel() {

    var currentPostAndCommentUiConfig: PostAndCommentsUiConfig = preferences.postAndCommentsUiConfig
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
        currentPostAndCommentUiConfig = preferences.postAndCommentsUiConfig
        onPostUiChanged.value = Unit
    }

    fun resetPostUiConfig() {
        currentPostAndCommentUiConfig = currentPostAndCommentUiConfig.copy(
            postUiConfig = getDefaultPostAndCommentsUiConfig().postUiConfig,
        )
        onPostUiChanged.value = Unit
    }

    fun resetCommentUiConfig() {
        currentPostAndCommentUiConfig = currentPostAndCommentUiConfig.copy(
            commentUiConfig = getDefaultPostAndCommentsUiConfig().commentUiConfig,
        )
        onPostUiChanged.value = Unit
    }

    private fun applyConfig() {
        preferences.postAndCommentsUiConfig = currentPostAndCommentUiConfig
    }

    override fun onCleared() {
        applyConfig()

        super.onCleared()
    }
}
