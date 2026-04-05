package com.idunnololz.summit.lemmy.post

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class PostTabbedViewModel @Inject constructor(
  private val savedStateHandle: SavedStateHandle,
) : ViewModel() {
  private val currentPageIndexFlow = savedStateHandle.getMutableStateFlow("current_page_index", 0)
  var currentPageIndex: Int
    get() = currentPageIndexFlow.value
    set(value) {
      currentPageIndexFlow.value = value
    }
}
