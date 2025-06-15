package com.idunnololz.summit.lemmy.modlogs.filter

import androidx.lifecycle.ViewModel
import com.idunnololz.summit.lemmy.modlogs.ModLogsFilterConfig
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class ModLogsFilterViewModel @Inject constructor() : ViewModel() {

  var filter = ModLogsFilterConfig()
}
