package com.idunnololz.summit.presets

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.idunnololz.summit.api.SummitServerClient
import com.idunnololz.summit.presets.PresetsViewModel.Model
import com.idunnololz.summit.util.StatefulLiveData
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.launch

@HiltViewModel
class AdminViewModel @Inject constructor(
  private val summitServerClient: SummitServerClient,
) : ViewModel() {

  data class Model(
    val presets: List<PresetData>,
  )

  val model = StatefulLiveData<Model>()

  fun loadData(force: Boolean = false) {
    model.setIsLoading()
    viewModelScope.launch {
      summitServerClient.getAllPresets(force)
        .onSuccess {
          model.postValue(Model(it.toPresetData(summitServerClient)))
        }
        .onFailure {
          model.postError(it)
        }
    }
  }
}
