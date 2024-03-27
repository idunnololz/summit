package com.idunnololz.summit.lemmy

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.idunnololz.summit.api.UploadImageResult
import com.idunnololz.summit.preferences.Preferences
import com.idunnololz.summit.util.StatefulLiveData
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class UploadImageViewModel @Inject constructor(
    private val uploadHelper: UploadHelper,
    private val preferences: Preferences,
) : ViewModel() {

    val uploadImageResult = StatefulLiveData<UploadImageResult>()
    val uploadImageForUrlResult = StatefulLiveData<UploadImageResult>()
    val uploadImageForCommunityIcon = StatefulLiveData<UploadImageResult>()
    val uploadImageForCommunityBanner = StatefulLiveData<UploadImageResult>()

    val isUploading: Boolean
        get() = uploadHelper.isUploading

    fun uploadImage(uri: Uri) {
        uploadImageInternal(uri, uploadImageResult)
    }

    fun uploadImageForUrl(uri: Uri) {
        uploadImageInternal(uri, uploadImageForUrlResult)
    }

    fun uploadImageForCommunityIcon(uri: Uri) {
        uploadImageInternal(uri, uploadImageForCommunityIcon)
    }

    fun uploadImageForCommunityBanner(uri: Uri) {
        uploadImageInternal(uri, uploadImageForCommunityBanner)
    }

    private fun uploadImageInternal(
        uri: Uri,
        imageLiveData: StatefulLiveData<UploadImageResult>,
    ) {
        imageLiveData.setIsLoading()

        uploadHelper.upload(
            coroutineScope = viewModelScope,
            uri = uri,
            rotateAccounts = preferences.rotateInstanceOnUploadFail,
            onSuccess = {
                imageLiveData.postValue(it)
            },
            onFailure = {
                imageLiveData.postError(it)
            },
        )
    }
}