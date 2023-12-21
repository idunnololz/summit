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
    @ApplicationContext private val context: Context,
    private val offlineManager: OfflineManager,
    private val fileDownloadHelper: FileDownloadHelper,
) : ViewModel() {

    val downloadResult = StatefulLiveData<Result<FileDownloadHelper.DownloadResult>>()
    val downloadAndShareFile = StatefulLiveData<Uri>()

    fun downloadFile(
        context: Context,
        destFileName: String,
        url: String?,
        cacheFile: File? = null,
        mimeType: String? = null,
    ) {
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                fileDownloadHelper
                    .downloadFile(
                        c = context,
                        destFileName = destFileName,
                        url = url,
                        cacheFile = cacheFile,
                        mimeType = mimeType,
                    )
            }

            downloadResult.postValue(result)
        }
    }

    fun downloadAndShareImage(url: String) {
        downloadAndShareFile.setIsLoading()

        offlineManager.fetchImage(
            url = url,
            listener = { file ->
                val fileUri = FileProviderHelper(context)
                    .openTempFile("img_${file.name}") { os ->
                        os.sink().buffer().use {
                            it.writeAll(file.source())
                        }
                    }
                downloadAndShareFile.postValue(fileUri)
            },
        )
    }
}
