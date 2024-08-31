package com.idunnololz.summit.lemmy.screenshotMode.record

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.idunnololz.summit.util.viewRecorder.RecordScreenshotConfig

class RecordScreenshotViewModel : ViewModel() {

    val recordScreenshotConfig = MutableLiveData(RecordScreenshotConfig())
}
