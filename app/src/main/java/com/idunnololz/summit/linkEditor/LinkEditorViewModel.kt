package com.idunnololz.summit.linkEditor

import android.net.Uri
import android.util.Log
import androidx.core.net.toUri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.extractor.TrackOutput
import com.idunnololz.summit.util.StatefulLiveData
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class LinkEditorViewModel @Inject constructor(
  private val savedStateHandle: SavedStateHandle,
) : ViewModel() {

  companion object {
    private const val TAG = "LinkEditorViewModel"
  }

  private var parseJob: Job? = null

  val currentUrl = savedStateHandle.getMutableStateFlow<String>("current_url", "")
  val data = StatefulLiveData<LinkEditorModel>()

  init {
    viewModelScope.launch {
      currentUrl.collect {
        Log.d(TAG, "currentUrl changed to $it")
        parseUrl(it)
      }
    }
  }

  fun parseUrl(url: String) {
    data.setIsLoading()

    parseJob?.cancel()
    parseJob = viewModelScope.launch(Dispatchers.Default) {
      try {
        val uri: Uri = url.toUri()
        val parts = mutableListOf<LinkPart>()

        uri.scheme?.let {
          parts += LinkPart.Scheme(it)
        }
        uri.authority?.let {
          parts += LinkPart.Authority(it)
        }
        uri.path?.let {
          parts += LinkPart.Path(it)
        }
        var id = 0
        uri.queryParameterNames.forEach {
          parts += LinkPart.QueryParam(
            internalKey = id++,
            key = it,
            value = uri.getQueryParameter(it) ?: ""
          )
        }
        uri.fragment?.let {
          parts += LinkPart.Fragment(it)
        }

        data.postValue(
          LinkEditorModel(parts)
        )
      } catch (e: Exception) {
        data.postError(e)
      }
    }
  }

  fun onPartsChanged(part: List<LinkPart>) {
    currentUrl.value = Uri.Builder()
      .apply {
        for (p in part) {
          when (p) {
            is LinkPart.Authority -> authority(p.value)
            is LinkPart.Fragment -> fragment(p.value)
            is LinkPart.Path -> path(p.value)
            is LinkPart.QueryParam -> appendQueryParameter(p.key, p.value)
            is LinkPart.Scheme -> scheme(p.value)
          }
        }
      }
      .toString()
  }

}