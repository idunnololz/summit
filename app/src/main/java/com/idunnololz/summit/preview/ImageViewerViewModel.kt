package com.idunnololz.summit.preview

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.idunnololz.summit.util.FileDownloadHelper
import com.idunnololz.summit.util.StatefulLiveData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class ImageViewerViewModel : ViewModel() {

    val downloadResult = StatefulLiveData<Result<FileDownloadHelper.DownloadResult>>()

    fun downloadFile(
        context: Context,
        destFileName: String,
        url: String?,
        cacheFile: File? = null,
        mimeType: String? = null,
    ) {
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                FileDownloadHelper
                    .downloadFile(
                        context,
                        destFileName,
                        url,
                        cacheFile,
                        mimeType = mimeType,
                    )
            }

            downloadResult.postValue(result)
        }
    }
}
