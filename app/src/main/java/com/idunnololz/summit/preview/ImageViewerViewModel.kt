package com.idunnololz.summit.preview

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.idunnololz.summit.fileprovider.FileProviderHelper
import com.idunnololz.summit.offline.OfflineManager
import com.idunnololz.summit.util.FileDownloadHelper
import com.idunnololz.summit.util.StatefulLiveData
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okio.buffer
import okio.sink
import okio.source
import java.io.File
import javax.inject.Inject

@HiltViewModel
class ImageViewerViewModel @Inject constructor(
) : ViewModel() {
}
