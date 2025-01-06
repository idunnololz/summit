package com.idunnololz.summit.lemmy.userTags

import android.content.Context
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.idunnololz.summit.R
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@HiltViewModel
class AddOrEditUserTagViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val userTagsManager: UserTagsManager,
    private val state: SavedStateHandle,
) : ViewModel() {

    data class Model(
        val personName: String,
        val personNameError: String?,
        val tag: String,
        val tagError: String?,
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
        val last = model.value
        model.value = Model(
            personName = personName,
            personNameError = if (validate) {
                if (personName.isBlank()) {
                    context.getString(R.string.error_cannot_be_blank)
                } else {
                    null
                }
            } else {
                last?.personNameError
            },
            tag = tag,
            tagError = if (validate) {
                if (tag.isBlank()) {
                    context.getString(R.string.error_cannot_be_blank)
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
