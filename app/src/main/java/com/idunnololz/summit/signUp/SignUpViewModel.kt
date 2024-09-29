package com.idunnololz.summit.signUp

import android.content.Context
import android.graphics.BitmapFactory
import android.util.Log
import android.util.LruCache
import android.util.Patterns
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.idunnololz.summit.BuildConfig
import com.idunnololz.summit.R
import com.idunnololz.summit.api.LemmyApiClient
import com.idunnololz.summit.api.dto.CaptchaResponse
import com.idunnololz.summit.api.dto.GetCaptchaResponse
import com.idunnololz.summit.api.dto.GetSiteResponse
import com.idunnololz.summit.links.LinkFixer
import com.idunnololz.summit.util.PrettyPrintUtils
import com.idunnololz.summit.util.StatefulLiveData
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID
import javax.inject.Inject
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.random.Random

@OptIn(FlowPreview::class)
@HiltViewModel
class SignUpViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val lemmyApiClientFactory: LemmyApiClient.Factory,
    private val linkFixer: LinkFixer,
) : ViewModel() {

    companion object {
        private const val TAG = "SignUpViewModel"
    }

    private val lemmyApiClient = lemmyApiClientFactory.create()
    private var captchaInfo: CaptchaResponse? = null
    private var site: GetSiteResponse? = null
    private val signUpModelState = MutableStateFlow(SignUpModel())
    private var instanceError: InstanceError? = null

    private var siteCache = LruCache<String, GetSiteResponse>(10)

    private val instanceTextState = MutableStateFlow<String>("")

    val signUpModel = MutableLiveData<SignUpModel>(signUpModelState.value)

    val fetchSiteLiveData = StatefulLiveData<Unit>()

    @OptIn(ExperimentalCoroutinesApi::class)
    private val backgroundContext = Dispatchers.Default.limitedParallelism(1)

    init {
        viewModelScope.launch(backgroundContext) {
            signUpModelState.collect {
                signUpModel.postValue(it)
            }
        }
        viewModelScope.launch(backgroundContext) {
            instanceTextState.debounce(500).collectLatest {
                prefetchSite(it)
            }
        }

        if (BuildConfig.DEBUG) {
            signUpModelState.value = signUpModelState.value.copy(
                signUpFormData = SignUpFormData(
                    instance = "lemmy.idunnololz.com",
                    username = "signup_test_${(System.currentTimeMillis() / 1000).toString().drop(3)}",
                    email = "support+signuptest@idunnololz.com",
                    password = "testestest",
                    questionnaireAnswer = "This is a test account!",
                    captchaAnswer = "asdf",
                )
            )
        }
    }

    private fun prefetchSite(instance: String) {
        val isValidUrl = Patterns.WEB_URL.matcher("https://${instance}").matches()
        if (instance.isBlank() || !isValidUrl) {
            return
        }

        viewModelScope.launch(backgroundContext) {
            lemmyApiClient.changeInstance(instance)

            lemmyApiClient.fetchSiteWithRetry(null, true)
                .onSuccess {
                    siteCache.put(instance, it)
                }
        }
    }

    private fun fetchSiteAndGoToNextStep(instance: String) {
        val isValidUrl = Patterns.WEB_URL.matcher("https://${instance}").matches()
        if (instance.isBlank() || !isValidUrl) {
            fetchSiteLiveData.setIdle()
            if (instance.isNotBlank() && !isValidUrl) {
                instanceError = InstanceError.InvalidUrl
            } else {
                instanceError = null
            }
            updateSignUpModel()
            return
        }

        fetchSiteLiveData.setIsLoading()
        updateSignUpModel()

        viewModelScope.launch(backgroundContext) {
            lemmyApiClient.changeInstance(instance)

            val cachedSite = siteCache.get(instance)

            val siteResult =
                if (cachedSite != null) {
                    Result.success(cachedSite)
                } else {
                    withContext(Dispatchers.IO) {
                        lemmyApiClient.fetchSiteWithRetry(null, true)
                    }
                }

            withContext(Dispatchers.Main) {
                siteResult
                    .onFailure {
                        site = null
                        fetchSiteLiveData.setError(it)
                        instanceError = InstanceError.InvalidInstance
                    }
                    .onSuccess {
                        site = it
                        fetchSiteLiveData.setValue(Unit)
                        instanceError = null

                        siteCache.put(instance, it)

                        onSiteLoadedAndConfirmed(instance, it)
                    }

                updateSignUpModel()
            }
        }
    }

    private fun onSiteLoadedAndConfirmed(instance: String, site: GetSiteResponse) {
        val currentScene = signUpModelState.value.currentScene as? SignUpScene.InstanceForm ?: return
        signUpModelState.value = signUpModelState.value.copy(
            currentScene = SignUpScene.CredentialsForm(
                instance = instance,
                site = site,
                previousScene = currentScene.copy(
                    continueClicked = false,
                    isLoading = false,
                    // clear all errors
                    instanceError = null,
                ),
            ),
            signUpFormData = signUpModelState.value.signUpFormData.copy(
                instance = instance,
            ),
        )
    }

    @OptIn(ExperimentalEncodingApi::class)
    private fun updateCaptcha(captcha: Result<GetCaptchaResponse>) {
        val currentScene = signUpModelState.value.currentScene as? SignUpScene.CaptchaForm ?: return

        captcha
            .onSuccess {
                if (it.ok == null) {
                    signUpModelState.value = signUpModelState.value.copy(
                        currentScene = currentScene.copy(
                            captchaError = CaptchaError.NoImageError()
                        )
                    )
                    return
                }

                val bitmap = try {
                    val data = Base64.Mime.decode(it.ok.png)
                    BitmapFactory.decodeByteArray(data, 0, data.size)
                } catch (e: Exception) {
                    Log.e(TAG, "Unable to decode captcha image", e)
                    null
                }

                if (bitmap == null) {
                    signUpModelState.value = signUpModelState.value.copy(
                        currentScene = currentScene.copy(
                            captchaError = CaptchaError.DecodeImageError()
                        )
                    )
                    return
                }

                signUpModelState.value = signUpModelState.value.copy(
                    currentScene = currentScene.copy(
                        captchaUuid = it.ok.uuid,
                        captchaImage = bitmap,
                        captchaWav = it.ok.wav,
                        captchaError = null,
                    )
                )
            }
            .onFailure {
                signUpModelState.value = signUpModelState.value.copy(
                    currentScene = currentScene.copy(
                        captchaUuid = null,
                        captchaImage = null,
                        captchaWav = null,
                        captchaError = it,
                    )
                )
            }
    }

    private fun updateSignUpModel() {
        val currentScene = signUpModelState.value.currentScene as? SignUpScene.InstanceForm
            ?: return

        val continueClicked =
            if (!fetchSiteLiveData.isLoading && instanceError != null) {
                false
            } else {
                currentScene.continueClicked
            }

        signUpModelState.value = signUpModelState.value.copy(
            currentScene = currentScene.copy(
                site = site,
                instance = instanceTextState.value,
                instanceError = instanceError,
                continueClicked = continueClicked,
                isLoading = fetchSiteLiveData.isLoading && continueClicked,
            )
        )
    }

    fun signUp(captchaAnswer: String?) {
        viewModelScope.launch(backgroundContext) {
            val password = UUID.randomUUID().toString()
            val registrationResult = lemmyApiClient.register(
                username = "tester_${Random.nextInt()}",
                password = password,
                passwordVerify = password,
                showNsfw = true,
                email = "support+testing_summit@idunnololz.com",
                captchaUuid = captchaInfo?.uuid,
                captchaAnswer = captchaAnswer,
                honeypot = null,
                answer = "I am testing Lemmy account registration. DO NOT APPROVE.",
            )

            registrationResult.getOrThrow()
        }
    }

    fun submitInstance(instance: String) {
        val currentScene = signUpModelState.value.currentScene as? SignUpScene.InstanceForm
            ?: return
        signUpModelState.value = signUpModelState.value.copy(
            currentScene = currentScene.copy(
                instance = instance,
                continueClicked = true,
            ),
        )
        fetchSiteAndGoToNextStep(instance)
    }

    fun submitCredentials(
        scene: SignUpScene.CredentialsForm,
        username: String,
        email: String,
        password: String,
    ) {
        val localSite = scene.site.site_view.local_site
        val usernameError = if (username.isBlank()) {
            context.getString(R.string.required)
        } else if (username.length < 3) {
            context.getString(R.string.error_username_too_short, "3")
        } else if (username.contains(" ")) {
            context.getString(R.string.error_username_cannot_contain_spaces)
        } else if (username.length > localSite.actor_name_max_length) {
            context.getString(
                R.string.error_username_too_long,
                PrettyPrintUtils.defaultDecimalFormat.format(localSite.actor_name_max_length))
        } else {
            null
        }
        val emailError = if (email.isBlank()) {
            context.getString(R.string.required)
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            context.getString(R.string.error_invalid_email)
        } else {
            null
        }
        val passwordError = if (password.isBlank()) {
            context.getString(R.string.required)
        } else if (password.length < 10) {
            context.getString(R.string.error_password_too_short, "10")
        } else if (password.length > 60) {
            context.getString(R.string.error_password_too_long, "60")
        } else {
            null
        }

        if (usernameError != null || emailError != null || passwordError != null) {
            signUpModelState.value = signUpModelState.value.copy(
                currentScene = scene.copy(
                    usernameError = usernameError,
                    emailError = emailError,
                    passwordError = passwordError,
                )
            )
        } else {
            signUpModelState.value = signUpModelState.value.copy(
                currentScene = SignUpScene.AnswerForm(
                    instance = scene.instance,
                    site = scene.site,
                    username = username,
                    email = email,
                    password = password,
                    previousScene = scene.copy(
                        isLoading = false,
                        // clear all errors
                        usernameError = null,
                        emailError = null,
                        passwordError = null,
                    ),
                ),
                signUpFormData = signUpModelState.value.signUpFormData.copy(
                    username = username,
                    email = email,
                    password = password,
                ),
            )
        }
    }

    fun goBack() {
        val currentScene = signUpModelState.value.currentScene
        if (currentScene is SignUpScene.AnswerForm && currentScene.showAnswerEditor) {
            showAnswerEditor(false)
        } else {
            signUpModelState.value = signUpModelState.value.copy(
                currentScene = currentScene.previousScene
                    ?: currentScene
            )
        }
    }

    fun onInstanceTextChanged(instanceText: CharSequence) {
        instanceTextState.value = instanceText.toString()
    }

    fun showAnswerEditor(show: Boolean = true) {
        val currentScene = signUpModelState.value.currentScene as? SignUpScene.AnswerForm ?: return
        signUpModelState.value = signUpModelState.value.copy(
            currentScene = currentScene.copy(
                showAnswerEditor = show
            )
        )
    }

    fun updateAnswer(text: String) {
        val currentScene = signUpModelState.value.currentScene as? SignUpScene.AnswerForm ?: return
        signUpModelState.value = signUpModelState.value.copy(
            currentScene = currentScene.copy(
                answer = text
            )
        )
    }

    fun submitAnswer(answer: String) {
        val currentScene = signUpModelState.value.currentScene as? SignUpScene.AnswerForm ?: return
        signUpModelState.value = signUpModelState.value.copy(
            currentScene = SignUpScene.CaptchaForm(
                instance = currentScene.instance,
                site = currentScene.site,
                previousScene = currentScene.copy(
                    isLoading = false,
                )
            ),
            signUpFormData = signUpModelState.value.signUpFormData.copy(
                questionnaireAnswer = answer
            ),
        )
    }

    fun fetchCaptcha(instance: String) {
        viewModelScope.launch {
            lemmyApiClient.changeInstance(instance)

            val result = withContext(Dispatchers.IO) {
                lemmyApiClient.getCaptcha()
            }
            withContext(Dispatchers.Main) {
                updateCaptcha(result)
            }
        }
    }

    fun updateCaptchaAnswer(answer: String) {
        val currentScene = signUpModelState.value.currentScene as? SignUpScene.CaptchaForm ?: return
        signUpModelState.value = signUpModelState.value.copy(
            currentScene = currentScene.copy(
                captchaAnswer = answer,
            ),
        )
    }

    fun submitCaptchaAnswer(uuid: String, answer: String) {
        val currentScene = signUpModelState.value.currentScene as? SignUpScene.CaptchaForm ?: return
        signUpModelState.value = signUpModelState.value.copy(
            currentScene = SignUpScene.SubmitApplication(
                instance = currentScene.instance,
                site = currentScene.site,
                previousScene = currentScene.copy(
                    isLoading = false,
                )
            ),
            signUpFormData = signUpModelState.value.signUpFormData.copy(
                captchaUuid = uuid,
                captchaAnswer = answer
            ),
        )
    }
}

sealed interface InstanceError {
    data object InvalidInstance: InstanceError
    data object InvalidUrl: InstanceError
    data class InstanceCorrection(val correctedInstance: String): InstanceError
}

sealed class CaptchaError : Exception() {
    class DecodeImageError : CaptchaError()
    class NoImageError : CaptchaError()
}