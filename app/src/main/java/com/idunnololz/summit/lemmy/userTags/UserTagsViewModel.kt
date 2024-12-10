package com.idunnololz.summit.lemmy.userTags

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.idunnololz.summit.util.StatefulLiveData
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class UserTagsViewModel @Inject constructor(
    private val userTagsManager: UserTagsManager,
): ViewModel() {

    data class Model(
        val userTags: List<UserTag>,
    )

    val model = StatefulLiveData<Model>()

    init {
        viewModelScope.launch {
            userTagsManager.onChangedFlow.collect {
                _refresh(force = true)
            }
        }

        viewModelScope.launch {
            _refresh()
        }
    }

    fun refresh(force: Boolean = false) {
        viewModelScope.launch {
            _refresh(force)
        }
    }

    private suspend fun _refresh(force: Boolean = false) {
        if (model.valueOrNull != null && !force) {
            return
        }

        model.postIsLoading()

        withContext(Dispatchers.Default) {
            model.postValue(
                Model(
                    userTagsManager.getAllUserTags()
                        .map {
                            UserTag(
                                it.actorId,
                                it.tag,
                            )
                        }
                )
            )
        }
    }

    fun deleteTag(personName: String?) {
        personName ?: return

        userTagsManager.deleteTag(personName)
    }
}