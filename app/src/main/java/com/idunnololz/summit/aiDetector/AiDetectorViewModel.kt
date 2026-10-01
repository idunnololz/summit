package com.idunnololz.summit.aiDetector

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.idunnololz.summit.api.openai.ContentProvenanceCheck
import com.idunnololz.summit.api.openai.OpenAiClient
import com.idunnololz.summit.offline.OfflineManager
import com.idunnololz.summit.util.StatefulLiveData
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

@HiltViewModel
class AiDetectorViewModel @Inject constructor(
  private val offlineManager: OfflineManager,
  private val openAiClient: OpenAiClient,
) : ViewModel() {
  val result = StatefulLiveData<ContentProvenanceCheck>()

  private var downloadRegistration: OfflineManager.Registration? = null
  private var checkImageJob: Job? = null

  fun checkImage(url: String, force: Boolean = false) {
    if (!force && !result.isNotStarted) return

    checkImageJob?.cancel()
    downloadRegistration?.let { registration ->
      if (registration.registered) registration.cancel(offlineManager)
    }

    result.setIsLoading()
    downloadRegistration = offlineManager.fetchImage(
      url = url,
      listener = { file ->
        downloadRegistration = null
        checkImageJob = viewModelScope.launch {
          openAiClient.checkImage(imageKey = url, file = file, force = false)
            .onSuccess(result::postValue)
            .onFailure(result::postError)
        }
      },
      errorListener = { error ->
        downloadRegistration = null
        result.postError(error)
      },
      force = force,
    )
  }

  override fun onCleared() {
    downloadRegistration?.let { registration ->
      if (registration.registered) registration.cancel(offlineManager)
    }
  }
}
