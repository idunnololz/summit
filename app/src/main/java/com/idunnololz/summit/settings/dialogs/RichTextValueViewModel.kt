package com.idunnololz.summit.settings.dialogs

import android.app.Application
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.idunnololz.summit.account.AccountActionsManager
import com.idunnololz.summit.account.AccountManager
import com.idunnololz.summit.api.AccountAwareLemmyClient
import com.idunnololz.summit.api.NotAuthenticatedException
import com.idunnololz.summit.api.UploadImageResult
import com.idunnololz.summit.util.StatefulLiveData
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.launch

@HiltViewModel
class RichTextValueViewModel @Inject constructor(
    private val context: Application,
    private val authedApiClient: AccountAwareLemmyClient,
    private val accountManager: AccountManager,
    private val accountActionsManager: AccountActionsManager,
) : ViewModel() {

    val uploadImageEvent = StatefulLiveData<UploadImageResult>()
    val instance: String
        get() = authedApiClient.instance

    fun uploadImage(uri: Uri) {
        uploadImageEvent.setIsLoading()

        viewModelScope.launch {
            var result = uri.path
            val cut: Int? = result?.lastIndexOf('/')
            if (cut != null && cut != -1) {
                result = result?.substring(cut + 1)
            }

            val account = accountManager.currentAccount.value

            if (account == null) {
                uploadImageEvent.postError(NotAuthenticatedException())
                return@launch
            }
            context.contentResolver
                .openInputStream(uri)
                .use {
                    if (it == null) {
                        return@use Result.failure(RuntimeException("file_not_found"))
                    }
                    return@use authedApiClient.uploadImage(result ?: "image", it)
                }
                .onFailure {
                    uploadImageEvent.postError(it)
                }
                .onSuccess {
                    uploadImageEvent.postValue(it)
                }
        }
    }
}
