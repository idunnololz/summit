package com.idunnololz.summit.drafts.addOrEditTemplate

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.idunnololz.summit.account.AccountManager
import com.idunnololz.summit.templates.TemplatesManager
import com.idunnololz.summit.templates.db.TemplateData.PostTemplateData
import com.idunnololz.summit.util.StatefulLiveData
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.launch

@HiltViewModel
class AddOrEditPostTemplateViewModel @Inject constructor(
  private val savedStateHandle: SavedStateHandle,
  private val templatesManager: TemplatesManager,
  private val accountManager: AccountManager,
) : ViewModel() {

  val templateId = savedStateHandle.getMutableStateFlow<Long?>("template_id", null)
  val templateToEditData = StatefulLiveData<PostTemplateData>()

  fun loadTemplateIfNeeded(templateToEdit: TemplateToEdit) {
    val entryId = templateToEdit.entryId

    if (templateId.value == entryId) {
      return
    }

    templateId.value = entryId

    if (entryId != null) {
      templateToEditData.setIsLoading()

      viewModelScope.launch {
        val templateData = templatesManager.getTemplateById(entryId)?.data as? PostTemplateData

        if (templateData != null) {
          templateToEditData.postValue(templateData)
        }
      }
    }
  }

  suspend fun save(name: String, title: String, body: String, isNsfw: Boolean) {
    val job = viewModelScope.launch {
      val templateData = PostTemplateData(
        name = name,
        url = null,
        isNsfw = isNsfw,
        title = title,
        content = body,
        accountId = accountManager.currentAccount.value.id,
        accountInstance = accountManager.currentAccount.value.instance,
        thumbnailUrl = null,
        altText = null,
        languageId = null,
      )

      val templateEntryId = templateId.value
      if (templateEntryId == null) {
        val id = templatesManager.saveTemplate(
          templateData = templateData,
          showToast = false,
        )
        templateId.value = id
      } else {
        templatesManager.updateTemplate(
          entryId = templateEntryId,
          templateData = templateData,
          showToast = false,
        )
      }
    }

    job.join()
  }

  fun deleteTemplate(entryId: Long) {
    templatesManager.deleteTemplateWithIdAsync(entryId)
  }
}
