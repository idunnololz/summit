package com.idunnololz.summit.login

import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.idunnololz.summit.account.Account
import com.idunnololz.summit.api.IncorrectLoginException
import com.idunnololz.summit.api.LemmyApiClient
import com.idunnololz.summit.api.NotAuthenticatedException
import com.idunnololz.summit.util.StatefulLiveData
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.launch

@HiltViewModel
class LoginViewModel @Inject constructor(
  private val lemmyApiClientFactory: LemmyApiClient.Factory,
  private val loginHelper: LoginHelper,
) : ViewModel() {

  companion object {
    private const val TAG = "LoginViewModel"
  }

  private val apiClient = lemmyApiClientFactory.create()

  val accountLiveData = StatefulLiveData<Account>()
  val state = MutableLiveData<State>(State.Login)

  fun login(instance: String, username: String, password: String, twoFactorCode: String?) {
    accountLiveData.setIsLoading()

    val instanceFormatted = instance.trim().lowercase()
    val usernameFormatted = username.trim()

    viewModelScope.launch {
      val result = apiClient.login(
        instanceFormatted,
        usernameFormatted,

        // From jerboa https://github.com/dessalines/jerboa/blob/main/app/src/main/java/com/jerboa/ui/components/login/Login.kt
        password.take(60),
        twoFactorCode,
      )

      if (result.exceptionOrNull()?.message?.contains("totp", ignoreCase = true) ==
        true
      ) {
        // user has 2fa enabled
        state.postValue(State.TwoFactorAuth(instanceFormatted, usernameFormatted, password))
        return@launch
      }

      if (result.isFailure) {
        Log.e(TAG, "", result.exceptionOrNull())

        var error = requireNotNull(result.exceptionOrNull())

        if (error is NotAuthenticatedException) {
          error = IncorrectLoginException()
        }

        accountLiveData.postError(error)
        return@launch
      }

      val jwt = result.getOrNull()
      if (jwt == null) {
        accountLiveData.postError(RuntimeException("200 but no token returned!"))
        return@launch
      }

      loginHelper.loginWithJwt(instanceFormatted, jwt)
        .onSuccess {
          accountLiveData.postValue(it)
        }
        .onFailure {
          accountLiveData.postError(it)
        }
    }
  }

  fun login2fa(twoFactorCode: String) {
    val currentState = state.value
    if (currentState is State.TwoFactorAuth) {
      login(
        currentState.instance,
        currentState.username,
        currentState.password,
        twoFactorCode,
      )
    }
  }

  fun onBackPress() {
    when (state.value) {
      State.Login -> {
        // shouldnt happen
      }
      is State.TwoFactorAuth -> {
        state.value = State.Login
      }
      null -> {}
    }
  }

  sealed interface State {
    data object Login : State

    data class TwoFactorAuth(
      val instance: String,
      val username: String,
      val password: String,
    ) : State
  }
}
