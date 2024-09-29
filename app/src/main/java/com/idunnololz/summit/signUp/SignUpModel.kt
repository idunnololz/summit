package com.idunnololz.summit.signUp

import android.graphics.Bitmap
import com.idunnololz.summit.api.dto.GetSiteResponse

data class SignUpModel(
    val currentScene: SignUpScene = SignUpScene.InstanceForm(),
    val signUpFormData: SignUpFormData = SignUpFormData(),
)

data class SignUpFormData(
    val instance: String = "",
    val username: String = "",
    val email: String = "",
    val password: String = "",
    val questionnaireAnswer: String = "",
    val captchaUuid: String = "",
    val captchaAnswer: String = "",
)

sealed interface SignUpScene {
    val previousScene: SignUpScene?
    val isLoading: Boolean

    data class InstanceForm(
        override val previousScene: SignUpScene? = null,
        override val isLoading: Boolean = false,
        val instance: String? = null,
        val site: GetSiteResponse? = null,
        val instanceError: InstanceError? = null,
        val continueClicked: Boolean = false,
    ): SignUpScene

    data class CredentialsForm(
        val instance: String,
        val site: GetSiteResponse,
        override val previousScene: SignUpScene? = null,
        override val isLoading: Boolean = false,
        val captchaImage: ByteArray? = null,
        val captchaUuid: String? = null,
        val usernameError: String? = null,
        val emailError: String? = null,
        val passwordError: String? = null,
    ): SignUpScene

    data class AnswerForm(
        val instance: String,
        val site: GetSiteResponse,
        val username: String,
        val email: String,
        val password: String,
        override val previousScene: SignUpScene? = null,
        override val isLoading: Boolean = false,
        val showAnswerEditor: Boolean = false,
        val answer: String = "",
    ): SignUpScene

    data class CaptchaForm(
        val instance: String,
        val site: GetSiteResponse,
        override val previousScene: SignUpScene? = null,
        override val isLoading: Boolean = false,
        val captchaUuid: String? = null,
        val captchaImage: Bitmap? = null,
        val captchaWav: String? = null,
        val captchaError: Throwable? = null,
        val captchaAnswer: String = "",
    ): SignUpScene

    data class SubmitApplication(
        val instance: String,
        val site: GetSiteResponse,
        override val previousScene: SignUpScene? = null,
        override val isLoading: Boolean = false,
    ): SignUpScene
}