package com.idunnololz.summit.drafts.addOrEditTemplate

import androidx.lifecycle.ViewModel
import com.idunnololz.summit.templates.TemplatesManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class AddOrEditPostTemplateViewModel @Inject constructor(
  private val templatesManager: TemplatesManager,
) : ViewModel() {
}