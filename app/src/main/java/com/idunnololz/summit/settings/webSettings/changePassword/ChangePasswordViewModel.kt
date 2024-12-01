package com.idunnololz.summit.settings.webSettings.changePassword

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.idunnololz.summit.api.AccountAwareLemmyClient
import com.idunnololz.summit.util.StatefulLiveData
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.launch

@HiltViewModel
class ChangePasswordViewModel @Inject constructor(
    private val lemmyClient: AccountAwareLemmyClient,
) : ViewModel() {

    val changePasswordState = StatefulLiveData<ChangePasswordResult>()

    fun changePassword(currentPassword: String, newPassword: String, newPasswordAgain: String) {
        changePasswordState.setIsLoading()

        viewModelScope.launch {
            lemmyClient
                .changePassword(
                    newPassword = newPassword,
                    newPasswordVerify = newPasswordAgain,
                    oldPassword = currentPassword,
                )
                .onSuccess {
                    changePasswordState.postValue(ChangePasswordResult)
                }
                .onFailure {
                    changePasswordState.postError(it)
                }
        }
    }

    data object ChangePasswordResult
}
