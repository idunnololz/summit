package com.idunnololz.summit.settings.view_type

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.idunnololz.summit.lemmy.postListView.PostInListUiConfig
import com.idunnololz.summit.lemmy.postListView.getDefaultPostUiConfig
import com.idunnololz.summit.preferences.Preferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingViewTypeViewModel @Inject constructor(
    private val preferences: Preferences,
) : ViewModel() {

    var currentPostUiConfig: PostInListUiConfig = preferences.getPostInListUiConfig()
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
        currentPostUiConfig = preferences.getPostInListUiConfig()
        onPostUiChanged.value = Unit
    }

    fun resetPostUiConfig() {
        currentPostUiConfig = preferences.getPostsLayout().getDefaultPostUiConfig()
        onPostUiChanged.value = Unit
    }

    private fun applyConfig() {
        preferences.setPostInListUiConfig(currentPostUiConfig)
    }

    override fun onCleared() {
        applyConfig()

        super.onCleared()
    }
}
