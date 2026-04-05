package com.idunnololz.summit.preview

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel

@HiltViewModel(assistedFactory = ImageViewerViewModel.Factory::class)
class ImageViewerViewModel @AssistedInject constructor(
  @Assisted args: ImageViewerActivityArgs,
  savedStateHandle: SavedStateHandle,
) : ViewModel() {

  @AssistedFactory
  interface Factory {
    fun create(args: ImageViewerActivityArgs): ImageViewerViewModel
  }

  var url: String? = null
  private var peekFlow = savedStateHandle.getMutableStateFlow("peek", args.peek)

  var peek
    get() = peekFlow.value
    set(value) {
      peekFlow.value = value
    }
}
