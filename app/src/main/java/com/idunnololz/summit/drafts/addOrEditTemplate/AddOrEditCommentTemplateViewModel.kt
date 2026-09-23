package com.idunnololz.summit.drafts.addOrEditTemplate

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.idunnololz.summit.account.AccountManager
import com.idunnololz.summit.api.dto.lemmy.LanguageId
import com.idunnololz.summit.drafts.OriginalCommentData
import com.idunnololz.summit.lemmy.PostRef
import com.idunnololz.summit.templates.TemplatesManager
import com.idunnololz.summit.templates.db.TemplateData
import com.idunnololz.summit.templates.db.TemplateData.CommentTemplateData
import com.idunnololz.summit.templates.db.TemplateData.PostTemplateData
import com.idunnololz.summit.util.StatefulLiveData
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AddOrEditCommentTemplateViewModel @Inject constructor(
  private val savedStateHandle: SavedStateHandle,
  private val templatesManager: TemplatesManager,
  private val accountManager: AccountManager,
) : ViewModel() {

  val templateId = savedStateHandle.getMutableStateFlow<Long?>("template_id", null)
  val templateToEditData = StatefulLiveData<CommentTemplateData>()

  fun loadTemplateIfNeeded(templateToEdit: TemplateToEdit) {
    val entryId = templateToEdit.entryId

    if (templateId.value == entryId) {
      return
    }

    templateId.value = entryId

    if (entryId != null) {
      templateToEditData.setIsLoading()

      viewModelScope.launch {
        val templateData = templatesManager.getTemplateById(entryId)?.data as? CommentTemplateData

        if (templateData != null) {
          templateToEditData.postValue(templateData)
        }
      }
    }
  }

  suspend fun save(
    name: String,
    body: String,
  ) {
    val job = viewModelScope.launch {
      val templateData = CommentTemplateData(
        originalComment = null,
        postRef = null,
        parentCommentId = null,
        name = name,
        title = "",
        content = body,
        accountId = accountManager.currentAccount.value.id,
        accountInstance = accountManager.currentAccount.value.instance,
        languageId = null,
      )

      val templateEntryId = templateId.value
      if (templateEntryId == null) {
        val id = templatesManager.saveTemplate(
          templateData = templateData,
          showToast = false
        )
        templateId.value = id
      } else {
        templatesManager.updateTemplate(
          entryId = templateEntryId,
          templateData = templateData,
          showToast = false
        )
      }
    }

    job.join()
  }

  fun deleteTemplate(entryId: Long) {
    templatesManager.deleteTemplateWithIdAsync(entryId)
  }
}