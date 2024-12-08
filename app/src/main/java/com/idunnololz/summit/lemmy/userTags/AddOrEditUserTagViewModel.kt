package com.idunnololz.summit.lemmy.userTags

import android.content.Context
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.idunnololz.summit.R
import com.idunnololz.summit.util.ext.getColorFromAttribute
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

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
    )

    var personName: String = ""
    var tag: String = ""
    var fillColor: Int = 0
    var strokeColor: Int = 0

    val isSubmitted = state.getLiveData<Boolean>("is_submitted")

    val model = MutableLiveData<Model>()

    fun generateModel() {
        model.value = Model(
            personName = personName,
            personNameError = if (personName.isBlank()) {
                context.getString(R.string.error_cannot_be_blank)
            } else {
                null
            },
            tag = tag,
            tagError = if (tag.isBlank()) {
                context.getString(R.string.error_cannot_be_blank)
            } else {
                null
            },
            fillColor = fillColor,
            strokeColor = strokeColor,
            isSubmitted = isSubmitted.value ?: false,
        )
    }

    fun addTag() {
        generateModel()

        val model = model.value ?: return

        if (model.tagError != null || model.personNameError != null) {
            return
        }

        userTagsManager.addOrUpdateTag(model.personName, tag, fillColor, strokeColor)
        isSubmitted.value = true
        generateModel()
    }

}