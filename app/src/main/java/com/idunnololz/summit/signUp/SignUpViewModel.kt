package com.idunnololz.summit.signUp

import android.content.Context
import android.graphics.BitmapFactory
import android.util.Log
import android.util.LruCache
import android.util.Patterns
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.idunnololz.summit.BuildConfig
import com.idunnololz.summit.R
import com.idunnololz.summit.api.ClientApiException
import com.idunnololz.summit.api.LemmyApiClient
import com.idunnololz.summit.api.dto.CaptchaResponse
import com.idunnololz.summit.api.dto.GetCaptchaResponse
import com.idunnololz.summit.api.dto.GetSiteResponse
import com.idunnololz.summit.api.dto.RegistrationMode
import com.idunnololz.summit.login.LoginHelper
import com.idunnololz.summit.util.PrettyPrintUtils
import com.idunnololz.summit.util.StatefulLiveData
import com.idunnololz.summit.util.toErrorMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.UUID
import javax.inject.Inject
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.random.Random
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(FlowPreview::class)
@HiltViewModel
class SignUpViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val lemmyApiClientFactory: LemmyApiClient.Factory,
    private val loginHelper: LoginHelper,
    private val savedStateHandle: SavedStateHandle,
) : ViewModel() {

    companion object {
        private const val TAG = "SignUpViewModel"
    }

    private val lemmyApiClient = lemmyApiClientFactory.create()
    private var captchaInfo: CaptchaResponse? = null
    private var site: GetSiteResponse? = null
    private val signUpModelState = MutableStateFlow(SignUpModel())
    private var instanceError: String? = null

    private var siteCache = LruCache<String, GetSiteResponse>(10)

    private val instanceTextState = MutableStateFlow<String>("")

    val signUpModel = MutableLiveData<SignUpModel>(signUpModelState.value)

    val fetchSiteLiveData = StatefulLiveData<Unit>()

    @OptIn(ExperimentalCoroutinesApi::class)
    private val backgroundContext = Dispatchers.Default.limitedParallelism(1)

    init {
        viewModelScope.launch(backgroundContext) {
            signUpModelState.collect {
                savedStateHandle["sign_up_model_state"] = it
                signUpModel.postValue(it)
            }
        }
        viewModelScope.launch(backgroundContext) {
            instanceTextState.debounce(500).collectLatest {
                prefetchSite(it)
            }
        }

        if (BuildConfig.DEBUG) {
//            signUpModelState.value = signUpModelState.value.copy(
//                signUpFormData = SignUpFormData(
//                    instance = "lemmy.idunnololz.com",
//                    username = "signup_test_${(System.currentTimeMillis() / 1000).toString().drop(3)}",
//                    email = "",//"support+signuptest@idunnololz.com",
//                    password = "testestest",
//                    questionnaireAnswer = "This is a test account!",
//                    captchaAnswer = "asdf",
//                )
//            )
        }

        savedStateHandle.get<SignUpModel>("sign_up_model_state")?.let {
            signUpModelState.value = it
        }
    }

    private fun prefetchSite(instance: String) {
        val isValidUrl = Patterns.WEB_URL.matcher("https://$instance").matches()
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
        val isValidUrl = Patterns.WEB_URL.matcher("https://$instance").matches()
        if (instance.isBlank() || !isValidUrl) {
            fetchSiteLiveData.setIdle()
            if (instance.isNotBlank() && !isValidUrl) {
                instanceError = context.getString(R.string.error_invalid_instance_format)
            } else {
                instanceError = context.getString(R.string.required)
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
                        instanceError = context.getString(R.string.error_unable_to_resolve_instance)
                    }
                    .onSuccess {
                        site = it
                        fetchSiteLiveData.setValue(Unit)
                        instanceError = null

                        siteCache.put(instance, it)

                        if (it.site_view.local_site.registration_mode == RegistrationMode.Closed) {
                            instanceError = context.getString(R.string.error_instance_closed_registrations)
                        } else {
                            onSiteLoadedAndConfirmed(instance, it.toSiteModel())
                        }
                    }

                updateSignUpModel()
            }
        }
    }

    private fun onSiteLoadedAndConfirmed(instance: String, site: SiteModel) {
        val currentScene = signUpModelState.value.currentScene as? SignUpScene.InstanceForm
            ?: return
        signUpModelState.value = signUpModelState.value.copy(
            currentScene = requireNotNull(currentScene.nextScene(instance, site)),
            signUpFormData = signUpModelState.value.signUpFormData.copy(
                instance = instance,
            ),
        )
    }

    @OptIn(ExperimentalEncodingApi::class)
    private fun updateCaptcha(captcha: Result<GetCaptchaResponse>) {
        val currentScene = signUpModelState.value.currentScene as? SignUpScene.CaptchaForm
            ?: return

        captcha
            .onSuccess {
                if (it.ok == null) {
                    signUpModelState.value = signUpModelState.value.copy(
                        currentScene = currentScene.copy(
                            captchaError = CaptchaError.NoImageError(),
                        ),
                    )
                    return
                }

                val bitmap = try {
                    val data = Base64.Mime
                        .withPadding(Base64.PaddingOption.ABSENT_OPTIONAL)
                        .decode(it.ok.png)
                    BitmapFactory.decodeByteArray(data, 0, data.size)
                } catch (e: Exception) {
                    Log.e(TAG, "Unable to decode captcha image", e)
                    null
                }

                if (bitmap == null) {
                    signUpModelState.value = signUpModelState.value.copy(
                        currentScene = currentScene.copy(
                            captchaError = CaptchaError.DecodeImageError(),
                        ),
                    )
                    return
                }

                signUpModelState.value = signUpModelState.value.copy(
                    currentScene = currentScene.copy(
                        captchaUuid = it.ok.uuid,
                        captchaImage = bitmap,
                        captchaWav = it.ok.wav,
                        captchaError = null,
                    ),
                )
            }
            .onFailure {
                signUpModelState.value = signUpModelState.value.copy(
                    currentScene = currentScene.copy(
                        captchaUuid = null,
                        captchaImage = null,
                        captchaWav = null,
                        captchaError = it,
                    ),
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
                site = site?.toSiteModel(),
                instance = instanceTextState.value,
                instanceError = instanceError,
                continueClicked = continueClicked,
                isLoading = fetchSiteLiveData.isLoading && continueClicked,
            ),
        )
    }

    private fun SignUpScene.nextScene(instance: String, site: SiteModel): SignUpScene? {
        fun SignUpScene.nextSceneWithoutHasNextOrSkipping() = when (this) {
            is SignUpScene.InstanceForm ->
                SignUpScene.CredentialsForm(
                    instance = instance,
                    site = site,
                    isEmailRequired = site.localSite.require_email_verification,
                    hasNext = false,
                    previousScene = this.reset(),
                )
            is SignUpScene.CredentialsForm ->
                SignUpScene.AnswerForm(
                    instance = instance,
                    site = site,
                    hasNext = false,
                    previousScene = this.reset(),
                )
            is SignUpScene.AnswerForm ->
                SignUpScene.CaptchaForm(
                    instance = instance,
                    site = site,
                    hasNext = false,
                    previousScene = this.reset(),
                )
            is SignUpScene.CaptchaForm ->
                SignUpScene.SubmitApplication(
                    instance = instance,
                    site = site,
                    hasNext = false,
                    previousScene = this.reset(),
                )
            is SignUpScene.SubmitApplication ->
                SignUpScene.NextSteps(
                    instance = instance,
                    site = site,
                    loginResponse = null,
                    hasNext = false,
                    previousScene = this.reset(),
                )
            is SignUpScene.NextSteps ->
                null
        }

        fun SignUpScene.nextSceneWithoutHasNext(): SignUpScene? {
            var nextScene = this.nextSceneWithoutHasNextOrSkipping()

            while (true) {
                when (nextScene) {
                    is SignUpScene.AnswerForm ->
                        if (site.localSite.registration_mode == RegistrationMode.Open) {
                            nextScene = nextScene.nextSceneWithoutHasNextOrSkipping()
                        } else {
                            break
                        }
                    is SignUpScene.CaptchaForm ->
                        if (!site.localSite.captcha_enabled) {
                            nextScene = nextScene.nextSceneWithoutHasNextOrSkipping()
                        } else {
                            break
                        }
                    is SignUpScene.CredentialsForm -> break
                    is SignUpScene.InstanceForm -> break
                    is SignUpScene.SubmitApplication -> break
                    is SignUpScene.NextSteps -> break
                    null -> break
                }
            }

            return nextScene
        }

        val nextScene = nextSceneWithoutHasNext()
        val nextNextScene = nextScene?.nextSceneWithoutHasNext()
        return nextScene?.updateHasNext(
            hasNext = nextNextScene != null && nextNextScene !is SignUpScene.SubmitApplication,
        )
    }

    fun SignUpScene.reset() = when (this) {
        is SignUpScene.InstanceForm ->
            copy(
                continueClicked = false,
                isLoading = false,
                // clear all errors
                instanceError = null,
            )
        is SignUpScene.CredentialsForm ->
            copy(
                isLoading = false,
                // clear all errors
                usernameError = null,
                emailError = null,
                passwordError = null,
            )
        is SignUpScene.AnswerForm ->
            copy(
                isLoading = false,
                showAnswerEditor = false,
                // clear all errors
                answerError = null,
            )
        is SignUpScene.CaptchaForm ->
            copy(
                isLoading = false,

                // clear captcha
                captchaUuid = null,
                captchaImage = null,
                captchaWav = null,
                captchaError = null,
                captchaAnswer = "",
            )
        is SignUpScene.SubmitApplication ->
            copy(
                isLoading = false,
                // clear errors
                error = null,
            )
        is SignUpScene.NextSteps ->
            copy(
                isLoading = false,
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

    fun updateCredentials(username: String, email: String, password: String) {
        signUpModelState.value = signUpModelState.value.copy(
            signUpFormData = signUpModelState.value.signUpFormData.copy(
                username = username,
                email = email,
                password = password,
            ),
        )
    }

    fun submitCredentials(
        currentScene: SignUpScene.CredentialsForm,
        username: String,
        email: String,
        password: String,
    ) {
        val localSite = currentScene.site.localSite
        val usernameError = if (username.isBlank()) {
            context.getString(R.string.required)
        } else if (username.length < 3) {
            context.getString(R.string.error_username_too_short, "3")
        } else if (username.contains(" ")) {
            context.getString(R.string.error_username_cannot_contain_spaces)
        } else if (username.length > localSite.actor_name_max_length) {
            context.getString(
                R.string.error_username_too_long,
                PrettyPrintUtils.defaultDecimalFormat.format(localSite.actor_name_max_length),
            )
        } else {
            null
        }
        val emailError = if (email.isBlank()) {
            if (currentScene.isEmailRequired) {
                context.getString(R.string.required)
            } else {
                null
            }
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
                currentScene = currentScene.copy(
                    usernameError = usernameError,
                    emailError = emailError,
                    passwordError = passwordError,
                ),
            )
        } else {
            signUpModelState.value = signUpModelState.value.copy(
                currentScene = requireNotNull(
                    currentScene.nextScene(currentScene.instance, currentScene.site),
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
                    ?: currentScene,
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
                showAnswerEditor = show,
            ),
        )
    }

    fun updateAnswer(text: String) {
        val currentScene = signUpModelState.value.currentScene as? SignUpScene.AnswerForm ?: return
        signUpModelState.value = signUpModelState.value.copy(
            currentScene = currentScene.copy(
                answer = text,
            ),
            signUpFormData = signUpModelState.value.signUpFormData.copy(
                questionnaireAnswer = text,
            ),
        )
    }

    fun submitAnswer(answer: String) {
        val currentScene = signUpModelState.value.currentScene as? SignUpScene.AnswerForm ?: return

        if (answer.isBlank()) {
            signUpModelState.value = signUpModelState.value.copy(
                currentScene = currentScene.copy(
                    answerError = context.getString(R.string.required),
                ),
            )
        } else {
            signUpModelState.value = signUpModelState.value.copy(
                currentScene = requireNotNull(
                    currentScene.nextScene(currentScene.instance, currentScene.site),
                ),
                signUpFormData = signUpModelState.value.signUpFormData.copy(
                    questionnaireAnswer = answer,
                ),
            )
        }
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
            currentScene = requireNotNull(
                currentScene.nextScene(currentScene.instance, currentScene.site),
            ),
            signUpFormData = signUpModelState.value.signUpFormData.copy(
                captchaUuid = uuid,
                captchaAnswer = answer,
            ),
        )
    }

    fun submitApplication(instance: String, signUpFormData: SignUpFormData) {
        val currentScene = signUpModelState.value.currentScene as? SignUpScene.SubmitApplication
            ?: return

        viewModelScope.launch {
            lemmyApiClient.changeInstance(instance)

            val startTime = System.currentTimeMillis()

            val result = withContext(Dispatchers.IO) {
                lemmyApiClient
                    .register(
                        username = signUpFormData.username,
                        password = signUpFormData.password,
                        passwordVerify = signUpFormData.password,
                        showNsfw = true,
                        email = signUpFormData.email.ifBlank {
                            null
                        },
                        captchaUuid = signUpFormData.captchaUuid.ifBlank {
                            null
                        },
                        captchaAnswer = signUpFormData.captchaAnswer.ifBlank {
                            null
                        },
                        honeypot = null,
                        answer = signUpFormData.questionnaireAnswer.ifBlank {
                            null
                        },
                    )
            }

            val elapsedTime = System.currentTimeMillis() - startTime

            if (elapsedTime < 2000) {
                delay(2000 - elapsedTime)
            }

            result
                .onSuccess {
                    signUpModelState.value = signUpModelState.value.copy(
                        currentScene = SignUpScene.NextSteps(
                            instance = currentScene.instance,
                            site = currentScene.site,
                            loginResponse = it,
                            hasNext = false,
                        ),
                    )
                }
                .onFailure {
                    signUpModelState.value = signUpModelState.value.copy(
                        currentScene = currentScene.copy(
                            error = if (it is ClientApiException) {
                                it.errorMessage
                                    ?: it.toErrorMessage(context)
                            } else {
                                it.toErrorMessage(context)
                            },
                        ),
                    )
                }
        }
    }

    fun loginWithJwt(instance: String, jwt: String) {
        val currentScene = signUpModelState.value.currentScene as? SignUpScene.NextSteps
            ?: return

        if (currentScene.account != null || currentScene.isAccountLoading) {
            return
        }

        signUpModelState.value = signUpModelState.value.copy(
            currentScene = currentScene.copy(
                isAccountLoading = true,
            ),
        )

        viewModelScope.launch(backgroundContext) {
            loginHelper.loginWithJwt(instance, jwt)
                .onSuccess {
                    signUpModelState.value = signUpModelState.value.copy(
                        currentScene = currentScene.copy(
                            account = it,
                        ),
                    )

                    delay(1500)

                    signUpModelState.value = signUpModelState.value.copy(
                        currentScene = currentScene.copy(
                            done = true,
                        ),
                    )
                }
                .onFailure {
                    signUpModelState.value = signUpModelState.value.copy(
                        currentScene = currentScene.copy(
                            accountError = it.toErrorMessage(context),
                        ),
                    )
                }
        }
    }
}

private fun GetSiteResponse.toSiteModel(): SiteModel = SiteModel(
    localSite = site_view.local_site,
    name = site_view.site.name,
    description = site_view.site.description,
    icon = site_view.site.icon,
)

sealed interface InstanceError {
    data object InvalidInstance : InstanceError
    data object InvalidUrl : InstanceError
    data object RegistrationClosed : InstanceError
    data object BlankInstance : InstanceError
    data class InstanceCorrection(val correctedInstance: String) : InstanceError
}

sealed class CaptchaError : Exception() {
    class DecodeImageError : CaptchaError()
    class NoImageError : CaptchaError()
}
