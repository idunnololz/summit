package com.idunnololz.summit.drafts.templates

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.idunnololz.summit.templates.TemplatesManager
import com.idunnololz.summit.templates.db.TemplateData
import com.idunnololz.summit.templates.db.TemplateTypes
import com.idunnololz.summit.util.StatefulLiveData
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PostAndCommentTemplatesViewModel @Inject constructor(
  private val templatesManager: TemplatesManager,
) : ViewModel() {

  sealed interface Item {

    data object HeaderItem : Item

    data object FooterItem : Item

    data object EmptyItem : Item

    data class PostTemplateItem(
      val entryId: Long,
      val postTemplateData: TemplateData.PostTemplateData,
      val description: String,
    ): Item
    data class CommentTemplateItem(
      val entryId: Long,
      val commentTemplateData: TemplateData.CommentTemplateData,
      val description: String,
    ): Item
  }

  data class Model(
    val items: List<Item>
  )

  val model = StatefulLiveData<Model>()

  init {
    load(force = false)

    viewModelScope.launch {
      templatesManager.onTemplateChanged.collect {
        load(force = true)
      }
    }
  }

  fun load(force: Boolean) {
    viewModelScope.launch {
      model.setIsLoading()

      val modelItems = mutableListOf<Item>()
      val postTemplates = templatesManager.getTemplatesByType(TemplateTypes.Post)
      val commentTemplates = templatesManager.getTemplatesByType(TemplateTypes.Comment)

      modelItems += Item.HeaderItem

      (postTemplates + commentTemplates)
        .sortedByDescending { it.updatedTs }
        .mapNotNullTo(modelItems) {
          when (it.data) {
            is TemplateData.CommentTemplateData ->
              Item.CommentTemplateItem(
                entryId = it.id,
                commentTemplateData = it.data,
                description = it.data.content,
              )
            is TemplateData.PostTemplateData ->
              Item.PostTemplateItem(
                entryId = it.id,
                postTemplateData = it.data,
                description = buildString {
                  appendLine(it.data.title)
                  appendLine()
                  appendLine(it.data.content)
                }.trim(),
              )
            is TemplateData.RegistrationApplicationRejectionTemplateData -> null
            null -> null
          }
        }

      if (postTemplates.isEmpty() && commentTemplates.isEmpty()) {
        modelItems += Item.EmptyItem
      } else {
        modelItems += Item.FooterItem
      }

      model.postValue(
        Model(
          items = modelItems
        )
      )
    }
  }
}