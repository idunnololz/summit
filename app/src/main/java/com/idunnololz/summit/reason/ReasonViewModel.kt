package com.idunnololz.summit.reason

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.idunnololz.summit.templates.db.TemplateEntry
import com.idunnololz.summit.templates.db.TemplateTypes
import com.idunnololz.summit.templates.db.TemplatesDao
import com.idunnololz.summit.util.StatefulLiveData
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ReasonViewModel @Inject constructor(
  private val templatesDao: TemplatesDao,
) : ViewModel() {

  val templates = StatefulLiveData<List<TemplateEntry>>()

  init {
    loadTemplates()
  }

  private fun loadTemplates() {
    templates.setIsLoading()

    viewModelScope.launch {
      templatesDao.getTemplatesByType(TemplateTypes.RegistrationApplicationRejection)
        .let {
          templates.postValue(it)
        }
    }
  }
}