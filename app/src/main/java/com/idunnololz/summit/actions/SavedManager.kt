package com.idunnololz.summit.actions

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow

@Singleton
class SavedManager @Inject constructor() {
  var changeId = 0

  enum class SavedState {
    NoChange,
    Changed,
  }

  val onPostSaveChange = MutableStateFlow(SavedState.NoChange)
  val onCommentSaveChange = MutableStateFlow(SavedState.NoChange)

  fun onPostSaveChanged() {
    changeId++
    onPostSaveChange.value = SavedState.Changed
  }

  fun onCommentSaveChanged() {
    changeId++
    onCommentSaveChange.value = SavedState.Changed
  }

  fun resetPostSaveState() {
    onPostSaveChange.value = SavedState.NoChange
  }

  fun resetCommentSaveState() {
    onCommentSaveChange.value = SavedState.NoChange
  }
}
