package com.idunnololz.summit.lemmy.userTags

import android.util.Log
import androidx.annotation.StringRes
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.idunnololz.summit.R
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@HiltViewModel
class AddOrEditUserTagViewModel @Inject constructor(
    private val userTagsManager: UserTagsManager,
    private val state: SavedStateHandle,
) : ViewModel() {

    companion object {
        private const val TAG = "AddOrEditUserTagViewModel"
    }

    data class Model(
        val personName: String,
        @StringRes val personNameError: Int?,
        val tag: String,
        @StringRes val tagError: Int?,
        val fillColor: Int,
        val strokeColor: Int,
        val isSubmitted: Boolean,
        val showDeleteButton: Boolean,
    )

    var personName: String = ""
        set(value) {
            if (field == value) {
                return
            }

            field = value

            generateModel()
            loadUserTag(field)
        }
    var tag: String = ""
        set(value) {
            field = value

            generateModel()
        }
    var fillColor: Int = 0
        set(value) {
            field = value

            generateModel()
        }
    var strokeColor: Int = 0
        set(value) {
            field = value

            generateModel()
        }
    var showDeleteButton: Boolean = false
        set(value) {
            field = value

            generateModel()
        }

    val isSubmitted = state.getLiveData<Boolean>("is_submitted")

    val model = MutableLiveData<Model>()

    fun loadUserTag(personName: String) {
        viewModelScope.launch {
            val userTag = userTagsManager.getUserTag(personName) ?: return@launch

            withContext(Dispatchers.Main) {
                this@AddOrEditUserTagViewModel.personName = personName
                tag = userTag.tagName
                fillColor = userTag.fillColor
                strokeColor = userTag.borderColor
                showDeleteButton = true
            }
        }
    }

    fun generateModel(validate: Boolean = false) {
        Log.d(TAG, "generateModel()")
        val last = model.value
        val newModel = Model(
            personName = personName,
            personNameError = if (validate) {
                if (personName.isBlank()) {
                    R.string.error_cannot_be_blank
                } else {
                    null
                }
            } else {
                last?.personNameError
            },
            tag = tag,
            tagError = if (validate) {
                if (tag.isBlank()) {
                    R.string.error_cannot_be_blank
                } else {
                    null
                }
            } else {
                last?.tagError
            },
            fillColor = fillColor,
            strokeColor = strokeColor,
            isSubmitted = isSubmitted.value ?: false,
            showDeleteButton = showDeleteButton,
        )

        if (last == newModel) {
            return
        }

        model.value = newModel
    }

    fun addTag() {
        generateModel(validate = true)

        val model = model.value ?: return

        if (model.tagError != null || model.personNameError != null) {
            return
        }

        userTagsManager.addOrUpdateTag(model.personName, tag, fillColor, strokeColor)
        isSubmitted.value = true
        generateModel()
    }

    fun deleteUserTag() {
        userTagsManager.deleteTag(personName)
    }
}
