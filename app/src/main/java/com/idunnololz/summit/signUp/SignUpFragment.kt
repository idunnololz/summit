package com.idunnololz.summit.signUp

import android.graphics.RectF
import android.media.MediaPlayer
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.ArrayAdapter
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.core.net.toUri
import androidx.core.widget.addTextChangedListener
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.transition.AutoTransition
import androidx.transition.TransitionManager
import androidx.viewbinding.ViewBinding
import coil3.asImage
import coil3.dispose
import coil3.load
import com.idunnololz.summit.R
import com.idunnololz.summit.alert.OldAlertDialogFragment
import com.idunnololz.summit.api.LemmyApiClient.Companion.DEFAULT_LEMMY_INSTANCES
import com.idunnololz.summit.api.LemmyApiClient.Companion.INSTANCE_LEMMY_WORLD
import com.idunnololz.summit.databinding.FragmentSignUpBinding
import com.idunnololz.summit.databinding.SignUpAnswerFormBinding
import com.idunnololz.summit.databinding.SignUpCaptchaFormBinding
import com.idunnololz.summit.databinding.SignUpCredentialsFormBinding
import com.idunnololz.summit.databinding.SignUpInstanceFormBinding
import com.idunnololz.summit.databinding.SignUpNextStepsBinding
import com.idunnololz.summit.databinding.SignUpSubmitApplicationBinding
import com.idunnololz.summit.editTextToolbar.EditTextToolbarSettingsDialogFragment
import com.idunnololz.summit.editTextToolbar.TextFieldToolbarManager
import com.idunnololz.summit.editTextToolbar.TextFormatToolbarViewHolder
import com.idunnololz.summit.lemmy.LemmyTextHelper
import com.idunnololz.summit.lemmy.comment.AddLinkDialogFragment
import com.idunnololz.summit.lemmy.comment.PreviewCommentDialogFragment
import com.idunnololz.summit.lemmy.comment.PreviewCommentDialogFragmentArgs
import com.idunnololz.summit.links.LinkContext
import com.idunnololz.summit.links.onLinkClick
import com.idunnololz.summit.login.LoginFragment
import com.idunnololz.summit.util.BaseFragment
import com.idunnololz.summit.util.CustomLinkMovementMethod
import com.idunnololz.summit.util.DefaultLinkLongClickListener
import com.idunnololz.summit.util.DirectoryHelper
import com.idunnololz.summit.util.Utils
import com.idunnololz.summit.util.ext.getSelectedText
import com.idunnololz.summit.util.ext.requestFocusAndShowKeyboard
import com.idunnololz.summit.util.ext.showAllowingStateLoss
import com.idunnololz.summit.util.insetViewAutomaticallyByPadding
import com.idunnololz.summit.util.setupForFragment
import com.idunnololz.summit.util.shimmer.ShimmerDrawable
import com.idunnololz.summit.util.shimmer.newShimmerDrawable16to9
import com.idunnololz.summit.util.shimmer.newShimmerDrawableSquare
import com.idunnololz.summit.util.showMoreLinkOptions
import com.idunnololz.summit.util.toErrorMessage
import dagger.hilt.android.AndroidEntryPoint
import java.io.File
import javax.inject.Inject
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.runInterruptible

@AndroidEntryPoint
class SignUpFragment :
    BaseFragment<FragmentSignUpBinding>(),
    OldAlertDialogFragment.AlertDialogFragmentListener {

    private val viewModel: SignUpViewModel by viewModels()

    @Inject
    lateinit var textFieldToolbarManager: TextFieldToolbarManager

    @Inject
    lateinit var directoryHelper: DirectoryHelper

    private var currentScene: SignUpScene? = null

    private var textFormatToolbar: TextFormatToolbarViewHolder? = null

    private var currentMediaPlayer: MediaPlayer? = null

    private var currentBinding: ViewBinding? = null

    private var submitApplicationErrorDialog: DialogFragment? = null

    val backPressHandler = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            viewModel.goBack()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        super.onCreateView(inflater, container, savedInstanceState)

        setBinding(FragmentSignUpBinding.inflate(inflater, container, false))

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, backPressHandler)

        val context = requireContext()

        val shimmerDrawable = newShimmerDrawable16to9(context)

        with(binding) {
            requireMainActivity().apply {
                insetViewAutomaticallyByPadding(viewLifecycleOwner, view)
                setupForFragment<LoginFragment>()

                insets.observe(viewLifecycleOwner) {
                    (currentBinding as? SignUpInstanceFormBinding)?.apply {
                        instanceEditText.post {
                            if (!isBindingAvailable()) return@post
                            if (instanceEditText.isPopupShowing) {
                                instanceEditText.dismissDropDown()
                                instanceEditText.showDropDown()
                            }
                        }
                    }
                }
            }

            answerExpandedEditText.apply {
                doOnTextChanged { text, start, before, count ->
                    viewModel.updateAnswer(text?.toString() ?: "")
                }
            }

            textFieldToolbarManager.textFieldToolbarSettings.observe(viewLifecycleOwner) {
                binding.formattingOptionsContainer.removeAllViews()

                textFormatToolbar = textFieldToolbarManager.createTextFormatterToolbar(
                    context,
                    binding.formattingOptionsContainer,
                )

                textFormatToolbar?.setupTextFormatterToolbar(
                    editText = answerExpandedEditText,
                    referenceTextView = null,
                    lifecycleOwner = viewLifecycleOwner,
                    fragmentManager = childFragmentManager,
                    onChooseImageClick = null,
                    onAddLinkClick = {
                        AddLinkDialogFragment.show(
                            answerExpandedEditText.getSelectedText(),
                            childFragmentManager,
                        )
                    },
                    onPreviewClick = {
                        val instance = when (val s = viewModel.signUpModel.value?.currentScene) {
                            is SignUpScene.AnswerForm -> s.instance
                            else -> return@setupTextFormatterToolbar
                        }
                        PreviewCommentDialogFragment()
                            .apply {
                                arguments = PreviewCommentDialogFragmentArgs(
                                    instance,
                                    answerExpandedEditText.text.toString(),
                                ).toBundle()
                            }
                            .showAllowingStateLoss(childFragmentManager, "AA")
                    },
                    onSettingsClick = {
                        EditTextToolbarSettingsDialogFragment.show(childFragmentManager)
                    },
                )
            }

            fun onModelChanged(model: SignUpModel) {
                val isInitialRender = model.currentScene::class != (currentScene ?: Unit)::class
                if (isInitialRender) {
                    val animate = currentScene != null
                    currentScene = model.currentScene
                    currentBinding = layoutScene(model.currentScene, animate = animate)
                }
                renderCurrentScene(model, isInitialRender, shimmerDrawable)
            }

            viewModel.signUpModel.observe(viewLifecycleOwner) {
                onModelChanged(it)
            }
            viewModel.signUpModel.value?.let {
                onModelChanged(it)
            }
        }
    }

    @OptIn(ExperimentalEncodingApi::class)
    private fun renderCurrentScene(
        data: SignUpModel,
        isInitialRender: Boolean,
        shimmerDrawable: ShimmerDrawable,
    ) = with(binding) {
        val context = requireContext()
        val signUpFormData = data.signUpFormData

        if (data.currentScene is SignUpScene.AnswerForm && data.currentScene.showAnswerEditor) {
            answerExpandedContainer.apply {
                if (visibility != View.VISIBLE || alpha != 1f) {
                    visibility = View.VISIBLE
                    alpha = 0f
                    animate()
                        .alpha(1f)
                        .withEndAction {
                            answerExpandedEditText.requestFocusAndShowKeyboard()
                        }
                }
            }
        } else {
            answerExpandedContainer.apply {
                if (visibility == View.VISIBLE) {
                    animate()
                        .alpha(0f)
                        .withEndAction {
                            visibility = View.INVISIBLE
                        }
                }
            }
        }

        backPressHandler.isEnabled = data.currentScene.previousScene != null

        when (val scene = data.currentScene) {
            is SignUpScene.InstanceForm -> with(currentBinding as SignUpInstanceFormBinding) {
                body.text = LemmyTextHelper
                    .getSpannable(context, getString(R.string.sign_up_instance_desc))
                signUp.text = if (scene.hasNext) {
                    getString(R.string.button_continue)
                } else {
                    getString(R.string.submit)
                }
                instanceEditText.apply {
                    if (adapter == null) {
                        setAdapter(
                            ArrayAdapter(
                                context,
                                com.google.android.material.R.layout.m3_auto_complete_simple_item,
                                DEFAULT_LEMMY_INSTANCES,
                            ),
                        )
                    }
                    setOnEditorActionListener { v, actionId, event ->
                        if (actionId == EditorInfo.IME_ACTION_DONE) {
                            signUp.performClick()
                            return@setOnEditorActionListener true
                        }
                        false
                    }

                    if (isInitialRender || text.isNullOrBlank()) {
                        setText(signUpFormData.instance)
                    }
                }

                if (scene.instanceError != null) {
                    instance.error = scene.instanceError
                } else {
                    instance.isErrorEnabled = false
                }

                (instanceEditText.adapter as? ArrayAdapter<*>)?.notifyDataSetChanged()

                signUp.setOnClickListener {
                    instanceEditText.text?.toString()?.let {
                        Utils.hideKeyboard(requireMainActivity())
                        viewModel.submitInstance(it)
                    }
                }
                if (scene.continueClicked) {
                    signUp.isEnabled = false
                    instanceEditText.isEnabled = false
                } else {
                    signUp.isEnabled = true
                    instanceEditText.isEnabled = true
                }

                fun showProgressBar() {
                    progressBar.visibility = View.VISIBLE
                    signUp.textScaleX = 0f
                }

                fun hideProgressBar() {
                    progressBar.visibility = View.GONE
                    signUp.textScaleX = 1f
                }

                if (scene.isLoading) {
                    showProgressBar()
                } else {
                    hideProgressBar()
                }
            }
            is SignUpScene.CredentialsForm -> with(currentBinding as SignUpCredentialsFormBinding) {
                signUp.text = if (scene.hasNext) {
                    getString(R.string.button_continue)
                } else {
                    getString(R.string.submit)
                }

                signUp.isEnabled = true

                if (scene.isEmailRequired) {
                    email.hint = getString(R.string.email)
                } else {
                    email.hint = getString(R.string.email_optional)
                }

                if (scene.usernameError != null) {
                    username.error = scene.usernameError
                } else {
                    username.isErrorEnabled = false
                }
                if (scene.emailError != null) {
                    email.error = scene.emailError
                } else {
                    email.isErrorEnabled = false
                }
                if (scene.passwordError != null) {
                    password.error = scene.passwordError
                } else {
                    password.isErrorEnabled = false
                }

                fun updateCreds() {
                    val username = usernameEditText.text?.toString()
                    val email = emailEditText.text?.toString()
                    val password = passwordEditText.text?.toString()

                    if (username != null && email != null && password != null) {
                        viewModel.updateCredentials(username, email, password)
                    }
                }

                usernameEditText.doOnTextChanged { text, start, before, count ->
                    updateCreds()
                }
                emailEditText.doOnTextChanged { text, start, before, count ->
                    updateCreds()
                }
                passwordEditText.doOnTextChanged { text, start, before, count ->
                    updateCreds()
                }

                if (isInitialRender) {
                    serverIcon.load(scene.site.icon) {
                        placeholder(newShimmerDrawableSquare(context).asImage())
                    }
                    serverName.text = scene.site.name
                    serverDesc.text = scene.site.description

                    usernameEditText.setText(signUpFormData.username)
                    emailEditText.setText(signUpFormData.email)
                    passwordEditText.setText(signUpFormData.password)

                    passwordEditText.setOnEditorActionListener { v, actionId, event ->
                        if (actionId == EditorInfo.IME_ACTION_DONE) {
                            signUp.performClick()
                            return@setOnEditorActionListener true
                        }
                        false
                    }

                    signUp.setOnClickListener {
                        val username = usernameEditText.text?.toString()
                        val email = emailEditText.text?.toString()
                        val password = passwordEditText.text?.toString()

                        if (username != null && email != null && password != null) {
                            Utils.hideKeyboard(requireMainActivity())
                            viewModel.submitCredentials(scene, username, email, password)
                        }
                    }
                }
            }
            is SignUpScene.AnswerForm -> with(currentBinding as SignUpAnswerFormBinding) {
                val localSite = scene.site.localSite
                serverIcon.load(scene.site.icon) {
                    placeholder(newShimmerDrawableSquare(context).asImage())
                }
                serverName.text = scene.site.name
                serverDesc.text = scene.site.description
                signUp.text = if (scene.hasNext) {
                    getString(R.string.button_continue)
                } else {
                    getString(R.string.submit)
                }

                val questionnaire = LemmyTextHelper
                    .getSpannable(context, localSite.application_question ?: "")

                if (isInitialRender) {
                    answerEditText.setText(signUpFormData.questionnaireAnswer)
                    answerExpandedEditText.setText(signUpFormData.questionnaireAnswer)

                    warning.text = LemmyTextHelper
                        .getSpannable(context, getString(R.string.answer_required_to_sign_up_desc))

                    body.text = questionnaire
                    expandedQuestionnaire.text = questionnaire
                }

                if (scene.answerError == null) {
                    answer.isErrorEnabled = false
                } else {
                    answer.error = scene.answerError
                }

                answerEditText.apply {
                    setText(scene.answer)
                    setOnClickListener {
                        viewModel.showAnswerEditor()
                    }
                }
                answer.setOnClickListener {
                    viewModel.showAnswerEditor()
                }
                answerExpandedEditText.apply {
                    setOnEditorActionListener { v, actionId, event ->
                        if (actionId == EditorInfo.IME_ACTION_DONE) {
                            answerDone.performClick()
                            return@setOnEditorActionListener true
                        }
                        false
                    }
                }
                answerDone.setOnClickListener {
                    Utils.hideKeyboard(requireMainActivity())
                    viewModel.showAnswerEditor(show = false)
                }

                signUp.isEnabled = scene.answer.isNotBlank()
                signUp.setOnClickListener {
                    viewModel.submitAnswer(scene.answer)
                }
            }
            is SignUpScene.CaptchaForm -> with(currentBinding as SignUpCaptchaFormBinding) {
                serverIcon.load(scene.site.icon) {
                    placeholder(newShimmerDrawableSquare(context).asImage())
                }
                serverName.text = scene.site.name
                serverDesc.text = scene.site.description
                signUp.text = if (scene.hasNext) {
                    getString(R.string.button_continue)
                } else {
                    getString(R.string.submit)
                }

                if (isInitialRender) {
                    viewModel.fetchCaptcha(scene.instance)
                }

                if (scene.captchaError != null) {
                    captchaImage.dispose()
                    captchaImage.setImageDrawable(null)
                    captchaError.text = when (scene.captchaError) {
                        is CaptchaError -> {
                            when (scene.captchaError) {
                                is CaptchaError.DecodeImageError -> {
                                    getString(R.string.error_unable_to_decode_captcha_image)
                                }

                                is CaptchaError.NoImageError -> {
                                    getString(R.string.error_no_captcha)
                                }
                            }
                        }
                        else ->
                            scene.captchaError.toErrorMessage(context)
                    }
                } else if (scene.captchaImage == null) {
                    captchaImage.load(shimmerDrawable)
                } else if (captchaImage.tag != scene.captchaUuid) {
                    captchaImage.tag = scene.captchaUuid
                    captchaImage.load(scene.captchaImage) {
                        placeholder(shimmerDrawable.asImage())
                    }
                }

                captchaRefresh.setOnClickListener {
                    viewModel.fetchCaptcha(scene.instance)
                }
                if (scene.captchaWav != null) {
                    captchaPlayAudio.isEnabled = true
                    captchaPlayAudio.setOnClickListener {
                        if (currentMediaPlayer?.isPlaying == true) {
                            currentMediaPlayer?.stop()
                            currentMediaPlayer = null
                            return@setOnClickListener
                        }

                        lifecycleScope.launch {
                            directoryHelper.miscDir.mkdirs()

                            val file = File(directoryHelper.miscDir, "captcha_sound.wav")
                            val waveData = Base64.Mime
                                .withPadding(Base64.PaddingOption.ABSENT_OPTIONAL)
                                .decode(scene.captchaWav)

                            runInterruptible {
                                file.outputStream().use {
                                    it.write(waveData)
                                }
                            }

                            currentMediaPlayer = MediaPlayer().apply {
                                setOnCompletionListener {
                                    currentMediaPlayer = null
                                }

                                setDataSource(context, file.toUri())
                                prepare()
                                start()
                            }
                        }
                    }
                } else {
                    captchaPlayAudio.isEnabled = false
                }
                captchaAnswerEditText.apply {
                    setOnEditorActionListener { v, actionId, event ->
                        if (actionId == EditorInfo.IME_ACTION_DONE) {
                            signUp.performClick()
                            return@setOnEditorActionListener true
                        }
                        false
                    }
                }

                signUp.isEnabled = scene.captchaAnswer.isNotBlank()
                signUp.setOnClickListener {
                    if (scene.captchaUuid != null) {
                        Utils.hideKeyboard(requireMainActivity())
                        viewModel.submitCaptchaAnswer(scene.captchaUuid, scene.captchaAnswer)
                    }
                }
            }
            is SignUpScene.SubmitApplication -> with(
                currentBinding as SignUpSubmitApplicationBinding,
            ) {
                serverIconExpanded.load(scene.site.icon) {
                    placeholder(newShimmerDrawableSquare(context).asImage())
                }

                if (isInitialRender) {
                    viewModel.submitApplication(scene.instance, data.signUpFormData)
                }

                if (scene.error != null && submitApplicationErrorDialog?.isVisible != true) {
                    val fm = childFragmentManager
                    submitApplicationErrorDialog = OldAlertDialogFragment.Builder()
                        .setTitle(R.string.error_sign_up)
                        .setMessage(scene.error)
                        .setPositiveButton(android.R.string.ok)
                        .create()
                        .apply {
                            fm.beginTransaction()
                                .add(this, "submit_application_error")
                                .commitAllowingStateLoss()
                        }
                }
            }
            is SignUpScene.NextSteps -> with(currentBinding as SignUpNextStepsBinding) {
                val loginResponse = requireNotNull(scene.loginResponse)

                TransitionManager.beginDelayedTransition(
                    binding.root,
                    AutoTransition().apply {
                        setDuration(220)
                    },
                )

                serverIconExpanded.load(scene.site.icon) {
                    placeholder(newShimmerDrawableSquare(context).asImage())
                }

                if (loginResponse.jwt != null) {
                    viewModel.loginWithJwt(scene.instance, loginResponse.jwt)

                    if (scene.account != null) {
                        errorMessage.visibility = View.GONE
                        progressBar.visibility = View.GONE
                        submittingApplication.text = getString(R.string.sign_up_success)
                    } else if (scene.accountError != null) {
                        errorMessage.visibility = View.VISIBLE
                        progressBar.visibility = View.GONE
                        errorMessage.text = scene.accountError
                    } else if (scene.isAccountLoading) {
                        errorMessage.visibility = View.GONE
                        progressBar.visibility = View.VISIBLE
                    }
                } else if (loginResponse.verify_email_sent) {
                    errorMessage.visibility = View.GONE
                    progressBar.visibility = View.GONE

                    submittingApplication.text = getString(R.string.email_verification_required)

                    finish.visibility = View.VISIBLE
                    finish.setOnClickListener {
                        findNavController().popBackStack()
                    }
                } else if (loginResponse.registration_created) {
                    errorMessage.visibility = View.GONE
                    progressBar.visibility = View.GONE

                    submittingApplication.text = getString(R.string.registration_created)

                    finish.visibility = View.VISIBLE
                    finish.setOnClickListener {
                        findNavController().popBackStack()
                    }
                } else {
                    errorMessage.visibility = View.VISIBLE
                    progressBar.visibility = View.GONE
                    submittingApplication.visibility = View.GONE

                    errorMessage.text = getString(R.string.error_unknown_sign_up_error)

                    finish.visibility = View.VISIBLE
                    finish.setOnClickListener {
                        findNavController().popBackStack()
                    }
                }

                if (scene.done) {
                    findNavController().popBackStack()
                }
            }
        }
    }

    private fun layoutScene(scene: SignUpScene, animate: Boolean): ViewBinding {
        val context = requireContext()

        if (animate) {
            TransitionManager.beginDelayedTransition(
                binding.root,
                AutoTransition().apply {
                    setDuration(220)
                },
            )
        }

        binding.content.removeAllViews()

        val layoutInflater = LayoutInflater.from(context)

        return when (scene) {
            is SignUpScene.InstanceForm ->
                SignUpInstanceFormBinding.inflate(
                    layoutInflater,
                    binding.content,
                    true,
                ).apply {
                    instanceEditText.addTextChangedListener(
                        onTextChanged = { text, _, _, _ ->
                            viewModel.onInstanceTextChanged(text ?: "")
                        },
                    )

                    body.movementMethod = CustomLinkMovementMethod().apply {
                        onLinkLongClickListener = DefaultLinkLongClickListener(context) { url, text ->
                            if (url == "a") {
                                return@DefaultLinkLongClickListener
                            }

                            getMainActivity()?.showMoreLinkOptions(url, text)
                        }
                        onLinkClickListener = object : CustomLinkMovementMethod.OnLinkClickListener {
                            override fun onClick(
                                textView: TextView,
                                url: String,
                                text: String,
                                rect: RectF,
                            ): Boolean {
                                if (url == "a") {
                                    instanceEditText.setText(INSTANCE_LEMMY_WORLD)
                                    return true
                                }

                                onLinkClick(url, text, LinkContext.Text)
                                return true
                            }
                        }
                    }
                }
            is SignUpScene.CredentialsForm ->
                SignUpCredentialsFormBinding.inflate(
                    layoutInflater,
                    binding.content,
                    true,
                )
            is SignUpScene.AnswerForm ->
                SignUpAnswerFormBinding.inflate(
                    layoutInflater,
                    binding.content,
                    true,
                )
            is SignUpScene.CaptchaForm ->
                SignUpCaptchaFormBinding.inflate(
                    layoutInflater,
                    binding.content,
                    true,
                ).apply {
                    captchaAnswerEditText.apply {
                        doOnTextChanged { text, _, _, _ ->
                            viewModel.updateCaptchaAnswer(text?.toString() ?: "")
                        }
                    }
                }
            is SignUpScene.SubmitApplication ->
                SignUpSubmitApplicationBinding.inflate(
                    layoutInflater,
                    binding.content,
                    true,
                )
            is SignUpScene.NextSteps ->
                SignUpNextStepsBinding.inflate(
                    layoutInflater,
                    binding.content,
                    true,
                )
        }
    }

    override fun onPositiveClick(dialog: OldAlertDialogFragment, tag: String?) {
        if (tag == "submit_application_error") {
            viewModel.goBack()
        }
    }

    override fun onNegativeClick(dialog: OldAlertDialogFragment, tag: String?) {
    }
}
