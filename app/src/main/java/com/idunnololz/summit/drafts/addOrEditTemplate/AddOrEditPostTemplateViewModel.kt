package com.idunnololz.summit.drafts.addOrEditTemplate

import android.view.View
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.idunnololz.summit.account.AccountManager
import com.idunnololz.summit.account.asAccount
import com.idunnololz.summit.templates.TemplatesManager
import com.idunnololz.summit.templates.db.TemplateData
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AddOrEditPostTemplateViewModel @Inject constructor(
  private val savedStateHandle: SavedStateHandle,
  private val templatesManager: TemplatesManager,
  private val accountManager: AccountManager,
) : ViewModel() {

  val templateId = savedStateHandle.getMutableStateFlow<Long?>("template_id", null)

  fun save(
    name: String,
    title: String,
    body: String,
    isNsfw: Boolean,
  ) {
    viewModelScope.launch {
      val templateData = TemplateData.PostTemplateData(
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
  }

  fun deleteTemplate(entryId: Long) {
    templatesManager.deleteTemplateWithIdAsync(entryId)
  }
}