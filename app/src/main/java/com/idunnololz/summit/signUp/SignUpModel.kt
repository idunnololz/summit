package com.idunnololz.summit.signUp

import android.graphics.Bitmap
import android.os.Parcelable
import com.idunnololz.summit.account.Account
import com.idunnololz.summit.api.dto.LocalSite
import com.idunnololz.summit.api.dto.LoginResponse
import kotlinx.parcelize.Parcelize

@Parcelize
data class SignUpModel(
    val currentScene: SignUpScene = SignUpScene.InstanceForm(),
    val signUpFormData: SignUpFormData = SignUpFormData(),
) : Parcelable

@Parcelize
data class SignUpFormData(
    val instance: String = "",
    val username: String = "",
    val email: String = "",
    val password: String = "",
    val questionnaireAnswer: String = "",
    val captchaUuid: String = "",
    val captchaAnswer: String = "",
) : Parcelable

sealed interface SignUpScene : Parcelable {
    val previousScene: SignUpScene?
    val isLoading: Boolean
    val hasNext: Boolean

    @Parcelize
    data class InstanceForm(
        override val hasNext: Boolean = true,
        override val previousScene: SignUpScene? = null,
        override val isLoading: Boolean = false,
        val instance: String? = null,
        val site: SiteModel? = null,
        val instanceError: String? = null,
        val continueClicked: Boolean = false,
    ) : SignUpScene

    @Parcelize
    data class CredentialsForm(
        val instance: String,
        val site: SiteModel,
        val isEmailRequired: Boolean,
        override val hasNext: Boolean,
        override val previousScene: SignUpScene? = null,
        override val isLoading: Boolean = false,
        val usernameError: String? = null,
        val emailError: String? = null,
        val passwordError: String? = null,
    ) : SignUpScene

    @Parcelize
    data class AnswerForm(
        val instance: String,
        val site: SiteModel,
        override val hasNext: Boolean,
        override val previousScene: SignUpScene? = null,
        override val isLoading: Boolean = false,
        val showAnswerEditor: Boolean = false,
        val answer: String = "",
        val answerError: String? = null,
    ) : SignUpScene

    @Parcelize
    data class CaptchaForm(
        val instance: String,
        val site: SiteModel,
        override val hasNext: Boolean,
        override val previousScene: SignUpScene? = null,
        override val isLoading: Boolean = false,
        val captchaUuid: String? = null,
        val captchaImage: Bitmap? = null,
        val captchaWav: String? = null,
        val captchaError: Throwable? = null,
        val captchaAnswer: String = "",
    ) : SignUpScene

    @Parcelize
    data class SubmitApplication(
        val instance: String,
        val site: SiteModel,
        override val hasNext: Boolean,
        override val previousScene: SignUpScene? = null,
        override val isLoading: Boolean = false,
        val error: String? = null,
    ) : SignUpScene

    @Parcelize
    data class NextSteps(
        val instance: String,
        val site: SiteModel,
        override val hasNext: Boolean,
        override val previousScene: SignUpScene? = null,
        override val isLoading: Boolean = false,
        val loginResponse: LoginResponse? = null,
        val done: Boolean = false,
        val account: Account? = null,
        val accountError: String? = null,
        val isAccountLoading: Boolean = false,
    ) : SignUpScene
}

@Parcelize
data class SiteModel(
    val localSite: LocalSite,
    val name: String,
    val description: String?,
    val icon: String?,
) : Parcelable

fun SignUpScene.updateHasNext(hasNext: Boolean): SignUpScene = when (this) {
    is SignUpScene.AnswerForm -> copy(hasNext = hasNext)
    is SignUpScene.CaptchaForm -> copy(hasNext = hasNext)
    is SignUpScene.CredentialsForm -> copy(hasNext = hasNext)
    is SignUpScene.InstanceForm -> copy(hasNext = hasNext)
    is SignUpScene.SubmitApplication -> copy(hasNext = hasNext)
    is SignUpScene.NextSteps -> copy(hasNext = hasNext)
}
