package com.idunnololz.summit.uploads

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.idunnololz.summit.api.AccountAwareLemmyClient
import com.idunnololz.summit.api.dto.lemmy.LocalImage
import com.idunnololz.summit.util.StatefulLiveData
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class UploadsViewModel @Inject constructor(
  @ApplicationContext private val context: Context,
  private val apiClient: AccountAwareLemmyClient,
  private val uploadsEngine: UploadsEngine,
) : ViewModel() {

  val itemsFlow
    get() = uploadsEngine.itemsFlow

  val deleteResult = StatefulLiveData<Unit>()

  fun fetchUploads(force: Boolean = false) {
    viewModelScope.launch {
      if (force) {
        uploadsEngine.reset()
      }
      uploadsEngine.fetchPage(0, force)
    }
  }

  fun loadMoreIfNeeded(pageIndex: Int) {
    viewModelScope.launch {
      uploadsEngine.fetchPage(pageIndex, force = false)
    }
  }

  fun deleteImage(deleteToken: String, filename: String) {
    deleteResult.setIsLoading()

    viewModelScope.launch {
      apiClient.deleteMediaItem(deleteToken, filename)
        .onSuccess {
          deleteResult.postValue(Unit)
          uploadsEngine.removeItem(filename)
        }
        .onFailure {
          deleteResult.postError(it)
        }

    }
  }
}
