package com.idunnololz.summit.preview

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.idunnololz.summit.util.FileDownloadHelper
import com.idunnololz.summit.util.StatefulLiveData
import com.idunnololz.summit.video.VideoDownloadManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class VideoViewerViewModel @Inject constructor(
    private val videoDownloadManager: VideoDownloadManager,
    private val fileDownloadHelper: FileDownloadHelper,
) : ViewModel() {

    var initialPositionHandled: Boolean = false

    val downloadVideoResult = StatefulLiveData<FileDownloadHelper.DownloadResult>()

    fun downloadVideo(
        context: Context,
        url: String,
    ) {
        downloadVideoResult.setIsLoading()

        viewModelScope.launch {
            videoDownloadManager.downloadVideo(url)
                .onSuccess { file ->
                    fileDownloadHelper
                        .downloadFile(
                            c = context,
                            destFileName = file.name,
                            url = url,
                            cacheFile = file,
                        )
                        .onSuccess {
                            downloadVideoResult.postValue(it)
                        }
                        .onFailure {
                            downloadVideoResult.postError(it)
                        }
                }
                .onFailure {
                    downloadVideoResult.postError(it)
                }
        }
    }
}
