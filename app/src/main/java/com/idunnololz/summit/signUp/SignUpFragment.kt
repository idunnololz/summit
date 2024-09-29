package com.idunnololz.summit.signUp

import android.content.Context
import android.graphics.RectF
import android.media.MediaPlayer
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.ArrayAdapter
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.BOTTOM
import androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.PARENT_ID
import androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.TOP
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.net.toUri
import androidx.core.view.children
import androidx.core.widget.addTextChangedListener
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.transition.TransitionManager
import coil.dispose
import coil.load
import com.google.android.material.textfield.TextInputLayout
import com.idunnololz.summit.R
import com.idunnololz.summit.api.LemmyApiClient.Companion.DEFAULT_LEMMY_INSTANCES
import com.idunnololz.summit.api.LemmyApiClient.Companion.INSTANCE_LEMMY_WORLD
import com.idunnololz.summit.databinding.FragmentSignUpBinding
import com.idunnololz.summit.databinding.SignUpInstanceFormBinding
import com.idunnololz.summit.drafts.DraftTypes
import com.idunnololz.summit.drafts.DraftsDialogFragment
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
import com.idunnololz.summit.util.makeTransition
import com.idunnololz.summit.util.setupForFragment
import com.idunnololz.summit.util.shimmer.ShimmerDrawable
import com.idunnololz.summit.util.shimmer.newShimmerDrawable16to9
import com.idunnololz.summit.util.shimmer.newShimmerDrawableSquare
import com.idunnololz.summit.util.showMoreLinkOptions
import com.idunnololz.summit.util.toErrorMessage
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import kotlinx.coroutines.runInterruptible
import java.io.File
import javax.inject.Inject
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi


@AndroidEntryPoint
class SignUpFragment : BaseFragment<FragmentSignUpBinding>() {

    private val viewModel: SignUpViewModel by viewModels()

    @Inject
    lateinit var textFieldToolbarManager: TextFieldToolbarManager

    @Inject
    lateinit var directoryHelper: DirectoryHelper

    private var currentScene: SignUpScene? = null

    private var textFormatToolbar: TextFormatToolbarViewHolder? = null

    private var currentMediaPlayer: MediaPlayer? = null

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

//        viewModel.fetchSite()
//
//        viewModel.signUpModel.observe(viewLifecycleOwner) {
//            when (it) {
//                is StatefulData.Error -> TODO()
//                is StatefulData.Loading -> {} //FIXME
//                is StatefulData.NotStarted -> TODO()
//                is StatefulData.Success -> {
//                    render(it.data)
//                }
//            }
//        }

        val shimmerDrawable = newShimmerDrawable16to9(context)

        with(binding) {

            serverIcon.transitionName = "server_icon"
            serverIconExpanded.transitionName = "server_icon"

            requireMainActivity().apply {
                insetViewAutomaticallyByPadding(viewLifecycleOwner, view)
                setupForFragment<LoginFragment>()

                insets.observe(viewLifecycleOwner) {
                    instanceEditText.post {
                        if (!isBindingAvailable()) return@post
                        if (instanceEditText.isPopupShowing) {
                            instanceEditText.dismissDropDown()
                            instanceEditText.showDropDown()
                        }
                    }
                }
            }

            instanceEditText.addTextChangedListener(
                onTextChanged = { text, _, _, _ ->
                    viewModel.onInstanceTextChanged(text ?: "")
                },
            )
            answerExpandedEditText.apply {
                doOnTextChanged { text, start, before, count ->
                    viewModel.updateAnswer(text?.toString() ?: "")
                }
            }
            captchaAnswerEditText.apply {
                doOnTextChanged { text, _, _, _ ->
                    viewModel.updateCaptchaAnswer(text?.toString() ?: "")
                }
            }

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


            textFieldToolbarManager.textFieldToolbarSettings.observe(viewLifecycleOwner) {
                binding.formattingOptionsContainer.removeAllViews()

                textFormatToolbar = textFieldToolbarManager.createTextFormatterToolbar(
                    context,
                    binding.formattingOptionsContainer,
                )

                textFormatToolbar?.setupTextFormatterToolbar(
                    editText = answerExpandedEditText,
                    referenceTextView = null,
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
                    onDraftsClick = {
                        DraftsDialogFragment.show(childFragmentManager, DraftTypes.Comment)
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
                    layoutScene(model.currentScene, animate = animate)
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

        if (data.currentScene.isLoading) {
            showProgressBar()
        } else {
            hideProgressBar()
        }

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
            is SignUpScene.InstanceForm -> {
                body.text = LemmyTextHelper
                    .getSpannable(context, getString(R.string.sign_up_instance_desc))
                signUp.text = getString(R.string.button_continue)
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

                when (scene.instanceError) {
                    is InstanceError.InstanceCorrection ->
                        instance.error = getString(R.string.error_unable_to_resolve_instance)
                    InstanceError.InvalidInstance ->
                        instance.error = getString(R.string.error_unable_to_resolve_instance)
                    InstanceError.InvalidUrl ->
                        instance.error = getString(R.string.error_invalid_instance_format)
                    null -> instance.isErrorEnabled = false
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
            }
            is SignUpScene.CredentialsForm -> {
                val siteView = scene.site.site_view
                serverIcon.load(siteView.site.icon) {
                    placeholder(newShimmerDrawableSquare(context))
                }
                serverName.text = siteView.site.name
                serverDesc.text = siteView.site.description

                signUp.isEnabled = true
                instanceEditText.isEnabled = true

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

                if (isInitialRender) {
                    usernameEditText.setText(signUpFormData.username)
                    emailEditText.setText(signUpFormData.email)
                    passwordEditText.setText(signUpFormData.password)
                }

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
            is SignUpScene.AnswerForm -> {
                val siteView = scene.site.site_view
                val localSite = siteView.local_site
                serverIcon.load(siteView.site.icon) {
                    placeholder(newShimmerDrawableSquare(context))
                }
                serverName.text = siteView.site.name
                serverDesc.text = siteView.site.description

                val questionnaire = LemmyTextHelper
                    .getSpannable(context, localSite.application_question ?: "")

                if (isInitialRender) {
                    answerEditText.setText(signUpFormData.questionnaireAnswer)
                    answerExpandedEditText.setText(signUpFormData.questionnaireAnswer)
                }

                warning.text = LemmyTextHelper
                    .getSpannable(context, getString(R.string.answer_required_to_sign_up_desc))
                body.text = questionnaire
                answerEditText.apply {
                    setText(scene.answer)
                    setOnClickListener {
                        viewModel.showAnswerEditor()
                    }
                }
                answer.setOnClickListener {
                    viewModel.showAnswerEditor()
                }
                expandedQuestionnaire.text = questionnaire
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
                    viewModel.showAnswerEditor(show = false)
                    Utils.hideKeyboard(requireMainActivity())
                }

                signUp.isEnabled = scene.answer.isNotBlank()
                signUp.setOnClickListener {
                    viewModel.submitAnswer(scene.answer)
                }
            }
            is SignUpScene.CaptchaForm -> {
                val siteView = scene.site.site_view
                serverIcon.load(siteView.site.icon) {
                    placeholder(newShimmerDrawableSquare(context))
                }
                serverName.text = siteView.site.name
                serverDesc.text = siteView.site.description

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
                        placeholder(shimmerDrawable)
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
                            val waveData = Base64.Mime.decode(scene.captchaWav)

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
                        viewModel.submitCaptchaAnswer(scene.captchaUuid, scene.captchaAnswer)
                    }
                }
            }
            is SignUpScene.SubmitApplication -> {
                val siteView = scene.site.site_view
                serverIcon.load(siteView.site.icon) {
                    placeholder(newShimmerDrawableSquare(context))
                }
                serverIconExpanded.load(siteView.site.icon) {
                    placeholder(newShimmerDrawableSquare(context))
                }
                serverName.text = siteView.site.name
                serverDesc.text = siteView.site.description
            }
        }
    }

    private fun layoutScene(scene: SignUpScene, animate: Boolean) = with(binding) {
        val context = requireContext()

        if (animate) {
            TransitionManager.beginDelayedTransition(content, makeTransition())
        }

        val sceneElements = when (scene) {
            is SignUpScene.InstanceForm -> {
                SignUpInstanceFormBinding.inflate(LayoutInflater.from(context), content, true)
                listOf(
                    title.toView(),
                    16.toPadding(),
                    body.toView(),
                    24.toPadding(),
                    instance.toView(),
                    24.toPadding(),
                    signUp.toView(),
                )
            }
            is SignUpScene.CredentialsForm -> {
                listOf(
                    titleLarge.toView(),
                    16.toPadding(),
                    cardView.toView(),
                    16.toPadding(),
                    username.toView(),
                    8.toPadding(),
                    email.toView(),
                    8.toPadding(),
                    password.toView(),
                    24.toPadding(),
                    signUp.toView(),
                )
            }
            is SignUpScene.AnswerForm -> {
                listOf(
                    titleLarge.toView(),
                    16.toPadding(),
                    cardView.toView(),
                    16.toPadding(),
                    warning.toView(),
                    16.toPadding(),
                    body.toView(),
                    16.toPadding(),
                    answer.toView(),
                    24.toPadding(),
                    signUp.toView(),
                )
            }
            is SignUpScene.CaptchaForm -> {
                listOf(
                    titleLarge.toView(),
                    16.toPadding(),
                    cardView.toView(),
                    16.toPadding(),
                    captchaImageContainer.toView(),
                    captchaControls.toView(),
                    16.toPadding(),
                    captchaAnswerInputLayout.toView(),
                    24.toPadding(),
                    signUp.toView(),
                )
            }
            is SignUpScene.SubmitApplication -> {
                listOf(
                    serverIconExpanded.toView(),
                )
            }
        }
        val usedViewIds = sceneElements.mapTo(mutableSetOf<Int>()) {
            (it as? SceneElement.ViewElement)?.id ?: 0
        }

        content.children.forEach {
            if (!usedViewIds.contains(it.id) && it.id != R.id.progress_bar) {
                it.visibility = View.GONE
            } else {
                it.visibility = View.VISIBLE
            }
        }

        val constraintSet = ConstraintSet()
        constraintSet.clone(content)

        var pendingViewId = 0
        var pendingPadding = 0
        var firstEditText: View? = null

        for (element in sceneElements) {
            when (element) {
                is SceneElement.Padding -> {
                    pendingPadding = Utils.convertDpToPixel(element.paddingDp).toInt()
                }
                is SceneElement.ViewElement -> {
                    if (pendingViewId == 0) {
                        constraintSet.connect(PARENT_ID, TOP, element.id, TOP, pendingPadding)
                    } else {
                        constraintSet.connect(pendingViewId, BOTTOM, element.id, TOP, pendingPadding)
                    }

                    pendingViewId = element.id
                    pendingPadding = 0

                    if (element.view is TextInputLayout) {
                        firstEditText = element.view.editText
                    }
                }
            }
        }
        constraintSet.connect(pendingViewId, BOTTOM, PARENT_ID, BOTTOM)
        constraintSet.applyTo(content)

        if (firstEditText != null) {
            val inputMethodManager = context.getSystemService(Context.INPUT_METHOD_SERVICE)
                as InputMethodManager
            inputMethodManager.restartInput(firstEditText)
        }
    }

//    private fun render(data: SignUpModel) = with(binding) {
//        val context = requireContext()
//
//        if (data.captchaImage != null) {
//            captchaImage.load(data.captchaImage) {
//                placeholder(newShimmerDrawable16to9(context))
//            }
//        }
//    }

    sealed interface SceneElement {
        data class ViewElement(val id: Int, val view: View): SceneElement
        data class Padding(val paddingDp: Float): SceneElement
    }

    private fun showProgressBar() {
        with(binding) {
            progressBar.visibility = View.VISIBLE
            signUp.textScaleX = 0f
        }
    }

    private fun hideProgressBar() {
        with(binding) {
            progressBar.visibility = View.GONE
            signUp.textScaleX = 1f
        }
    }

    private fun View.toView() = SceneElement.ViewElement(this.id, this)

    private fun Int.toPadding() = SceneElement.Padding(this.toFloat())
}