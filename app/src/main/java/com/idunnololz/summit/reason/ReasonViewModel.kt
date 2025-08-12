package com.idunnololz.summit.reason

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.idunnololz.summit.templates.TemplatesManager
import com.idunnololz.summit.templates.db.TemplateEntry
import com.idunnololz.summit.templates.db.TemplateTypes
import com.idunnololz.summit.util.StatefulLiveData
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@HiltViewModel
class ReasonViewModel @Inject constructor(
  private val templatesManager: TemplatesManager,
) : ViewModel() {

  val templates = StatefulLiveData<List<TemplateEntry>>()

  init {
    loadTemplates()

    viewModelScope.launch {
      templatesManager.onTemplateChanged.collect {
        withContext(Dispatchers.Main) {
          loadTemplates()
        }
      }
    }
  }

  private fun loadTemplates() {
    templates.setIsLoading()

    viewModelScope.launch {
      templatesManager.getTemplatesByType(TemplateTypes.RegistrationApplicationRejection)
        .let {
          templates.postValue(it)
        }
    }
  }
}
