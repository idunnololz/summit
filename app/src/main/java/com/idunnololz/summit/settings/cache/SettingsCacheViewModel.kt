package com.idunnololz.summit.settings.cache

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.idunnololz.summit.util.DirectoryHelper
import com.idunnololz.summit.util.StatefulLiveData
import com.idunnololz.summit.util.Utils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@HiltViewModel
class SettingsCacheViewModel @Inject constructor(
    private val directoryHelper: DirectoryHelper,
) : ViewModel() {

    data class DataModel(
        val cacheTotalSizeBytes: Long,
        val imagesSizeBytes: Long,
        val videosSizeBytes: Long,
        val cacheMediaSizeBytes: Long,
        val cacheNetworkCacheSizeBytes: Long,
    ) {

        val cacheOtherSizeBytes: Long
            get() = cacheTotalSizeBytes -
                cacheMediaSizeBytes -
                cacheNetworkCacheSizeBytes
    }

    val dataModel = StatefulLiveData<DataModel>()

    fun generateDataModel() {
        dataModel.setIsLoading()

        viewModelScope.launch {
            val fileOrFolderToSize = mutableMapOf<File, Long>()

            val mediaCacheDir = File(directoryHelper.cacheDir, "image_cache")
            val imageDirSize = Utils.getSizeOfFile(directoryHelper.imagesDir)
            val videoDirSize = Utils.getSizeOfFile(directoryHelper.videoCacheDir)

            directoryHelper.cacheDir.listFiles()?.forEach {
                fileOrFolderToSize[it] = Utils.getSizeOfFile(it)
            }

            dataModel.postValue(
                DataModel(
                    cacheTotalSizeBytes =
                        fileOrFolderToSize.values.sum(),
                    imagesSizeBytes = imageDirSize,
                    videosSizeBytes = videoDirSize,
                    cacheMediaSizeBytes =
                        fileOrFolderToSize[mediaCacheDir] ?: 0,
                    cacheNetworkCacheSizeBytes =
                        fileOrFolderToSize[directoryHelper.okHttpCacheDir] ?: 0,
                )
            )
        }
    }

}