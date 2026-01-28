package com.idunnololz.summit.settings.localTrackingEvents

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.idunnololz.summit.lemmy.CommunityRef
import com.idunnololz.summit.localTracking.LocalTracker
import com.idunnololz.summit.localTracking.OnTrackingEventListener
import com.idunnololz.summit.localTracking.TrackingEventsDao
import com.idunnololz.summit.localTracking.getTableSize
import com.idunnololz.summit.util.StatefulLiveData
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class SettingsLocalTrackingEventsViewModel @Inject constructor(
  private val trackingEventsDao: TrackingEventsDao,
  private val localTracker: LocalTracker,
) : ViewModel() {

  data class Model(
    val totalEvents: Int,
    val totalTableSize: Int,
  )

  val data = StatefulLiveData<Model>()

  private val onTrackingEventListener = object : OnTrackingEventListener {
    override fun onDeleteAll() {
      viewModelScope.launch(Dispatchers.Main) {
        load()
      }
    }

    override fun onViewCommunity(communityRef: CommunityRef) {}
  }

  init {
    localTracker.registerOnTrackingEventListener(onTrackingEventListener)
  }

  fun load() {
    data.setIsLoading()

    viewModelScope.launch {
      val model = withContext(Dispatchers.IO) {
        Model(
          totalEvents = trackingEventsDao.getCount(),
          totalTableSize = trackingEventsDao.getTableSize().toInt(),
        )
      }

      data.postValue(model)
    }
  }

  fun deleteAllLocalTrackingData() {
    localTracker.clearLocalTrackingData()
  }

  override fun onCleared() {
    localTracker.unregisterOnTrackingEventListener(onTrackingEventListener)
    super.onCleared()
  }
}