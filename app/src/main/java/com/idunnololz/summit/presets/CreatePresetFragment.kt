package com.idunnololz.summit.presets

import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams
import android.widget.FrameLayout
import androidx.activity.OnBackPressedCallback
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.updateLayoutParams
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.transition.AutoTransition
import androidx.transition.TransitionManager
import androidx.viewbinding.ViewBinding
import androidx.window.layout.WindowMetricsCalculator
import coil3.load
import com.idunnololz.summit.R
import com.idunnololz.summit.account.AccountManager
import com.idunnololz.summit.alert.newAlertDialogLauncher
import com.idunnololz.summit.databinding.CreatePresetConfirmBinding
import com.idunnololz.summit.databinding.CreatePresetInitialBinding
import com.idunnololz.summit.databinding.CreatePresetResolveSettingIssuesBinding
import com.idunnololz.summit.databinding.CreatePresetTakePreviewScreenshotBinding
import com.idunnololz.summit.databinding.FragmentCreatePresetBinding
import com.idunnololz.summit.databinding.PresetPreviewScreenshotContainerBinding
import com.idunnololz.summit.error.ErrorDialogFragment
import com.idunnololz.summit.lemmy.LemmyTextHelper
import com.idunnololz.summit.preferences.PreferenceManager
import com.idunnololz.summit.preferences.Preferences
import com.idunnololz.summit.preferences.SharedPreferencesManager
import com.idunnololz.summit.preferences.ThemeManager
import com.idunnololz.summit.util.BaseFragment
import com.idunnololz.summit.util.PiiDetector
import com.idunnololz.summit.util.Utils
import com.idunnololz.summit.util.insetViewAutomaticallyByPadding
import com.idunnololz.summit.util.setupToolbar
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.launch

@AndroidEntryPoint
class CreatePresetFragment : BaseFragment<FragmentCreatePresetBinding>() {

  val viewModel: CreatePresetViewModel by viewModels()

  @Inject
  lateinit var sharedPreferencesManager: SharedPreferencesManager

  @Inject
  lateinit var preferences: Preferences

  @Inject
  lateinit var preferenceManager: PreferenceManager

  @Inject
  lateinit var accountManager: AccountManager

  @Inject
  lateinit var themeManager: ThemeManager

  @Inject
  lateinit var lemmyTextHelper: LemmyTextHelper

  private var currentBinding: ViewBinding? = null
  private var currentState: CreatePresetViewModel.State? = null

  private val cancelLauncher = newAlertDialogLauncher("cancel") {
    if (it.isOk) {
      findNavController().popBackStack()
    }
  }
  private val submittedLauncher = newAlertDialogLauncher("submitted") {
    findNavController().popBackStack()
  }

  private val takePreviewScreenshotLauncher =
    registerForActivityResult(TakePreviewScreenshotContract()) {
      if (it != null) {
        viewModel.updateScreenshots(
          it.phonePreviewSs,
          it.tabletPreviewSs,
        )
      }
    }

  private val onBackPressedHandler = object : OnBackPressedCallback(false) {
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

    setBinding(FragmentCreatePresetBinding.inflate(inflater, container, false))

    return binding.root
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)

    with(binding) {
      requireSummitActivity().apply {
        insetViewAutomaticallyByPadding(viewLifecycleOwner, binding.root)
      }
      requireActivity().onBackPressedDispatcher
        .addCallback(viewLifecycleOwner, onBackPressedHandler)

      setupToolbar(toolbar, getString(R.string.create_preset))
      toolbar.setNavigationOnClickListener {
        cancelLauncher.launchDialog {
          messageResId = R.string.exit_create_preset_flow
        }
      }

      viewLifecycleOwner.lifecycleScope.launch {
        viewModel.state.collect {
          val currentState = currentState

          if (currentState == it) {
            return@collect
          }

          if (it is CreatePresetViewModel.State.Complete) {
            submittedLauncher.launchDialog {
              messageResId = R.string.preset_submitted
            }
            return@collect
          }

          if (currentState == null || currentState::class != it::class) {
            currentBinding = layoutState(it, animate = true)
          }

          bindState(it)

          this@CreatePresetFragment.currentState = it

          if (it is CreatePresetViewModel.State.Initial) {
            onBackPressedHandler.isEnabled = false
          } else {
            onBackPressedHandler.isEnabled = true
          }

//                when (it) {
//                    CreatePresetViewModel.State.Initial -> TODO()
//                    CreatePresetViewModel.State.Complete -> {
//                        submitPresetCompleteLauncher.launchDialog {
//                            messageResId = R.string.preset_submitted
//                            positionButtonResId = android.R.string.ok
//                        }
//                    }
//                    CreatePresetViewModel.State.ConfirmSubmit -> {
//                        confirmSubmitPresetLauncher.launchDialog {
//                            titleResId = R.string.submit_preset_confirm
//                            messageResId = R.string.submit_preset_confirm_desc
//                            positionButtonResId = R.string.submit
//                            negativeButtonResId = R.string.cancel
//                        }
//                    }
//                    is CreatePresetViewModel.State.ResolveSettingIssues -> TODO()
//                }
        }
      }
    }
  }

  override fun onResume() {
    super.onResume()
    sharedPreferencesManager.useTempPreferences = true
    preferences.updateWhichSharedPreference()
    preferenceManager
      .updateCurrentPreferences(accountManager.currentAccount.value, force = true)

    viewModel.refreshDraftPreferences()
  }

  override fun onPause() {
    if (isRemoving) {
      sharedPreferencesManager.useTempPreferences = false
      preferences.updateWhichSharedPreference()
      preferenceManager
        .updateCurrentPreferences(accountManager.currentAccount.value, force = true)
    }

    super.onPause()
  }

  private fun bindState(state: CreatePresetViewModel.State) {
    val context = requireContext()

    when (state) {
      is CreatePresetViewModel.State.Initial -> {}
      is CreatePresetViewModel.State.ConfirmSubmit ->
        with(currentBinding as CreatePresetConfirmBinding) {
          if (state.isSubmitting) {
            loadingView.showProgressBar()

            positiveButton.isEnabled = false
            negativeButton.isEnabled = false
          } else {
            loadingView.hideAll()

            positiveButton.isEnabled = true
            negativeButton.isEnabled = true
          }

          if (state.submitError != null) {
            ErrorDialogFragment.show(
              message = getString(R.string.error_submit_preset),
              error = state.submitError,
              fm = childFragmentManager,
            )
          }
        }
      is CreatePresetViewModel.State.ResolveSettingIssues -> {
        with(currentBinding as CreatePresetResolveSettingIssuesBinding) {
          val piiFoundWarningMd = buildString {
            append(getString(R.string.warn_pii_found_in_preset))
            appendLine()
            appendLine()

            state.settingIssues.forEach {
              if (it.keyError != null) {
                appendLine()
                append("- ")
              }

              when (it.keyError) {
                PiiDetector.ColumnNamePiiIssue.Person ->
                  append(
                    getString(
                      R.string.warn_pii_key_person,
                      "${it.key}, ${it.value}",
                    ),
                  )

                PiiDetector.ColumnNamePiiIssue.Email ->
                  append(
                    getString(
                      R.string.warn_pii_key_email,
                      "${it.key}, ${it.value}",
                    ),
                  )

                PiiDetector.ColumnNamePiiIssue.Dob ->
                  append(
                    getString(
                      R.string.warn_pii_key_dob,
                      "${it.key}, ${it.value}",
                    ),
                  )

                PiiDetector.ColumnNamePiiIssue.Gender ->
                  append(
                    getString(
                      R.string.warn_pii_key_gender,
                      "${it.key}, ${it.value}",
                    ),
                  )

                PiiDetector.ColumnNamePiiIssue.Nationality ->
                  append(
                    getString(
                      R.string.warn_pii_key_nationality,
                      "${it.key}, ${it.value}",
                    ),
                  )

                PiiDetector.ColumnNamePiiIssue.Address ->
                  append(
                    getString(
                      R.string.warn_pii_key_address,
                      "${it.key}, ${it.value}",
                    ),
                  )

                PiiDetector.ColumnNamePiiIssue.UserName ->
                  append(
                    getString(
                      R.string.warn_pii_key_username,
                      "${it.key}, ${it.value}",
                    ),
                  )

                PiiDetector.ColumnNamePiiIssue.Password ->
                  append(
                    getString(
                      R.string.warn_pii_key_password,
                      "${it.key}, ${it.value}",
                    ),
                  )

                PiiDetector.ColumnNamePiiIssue.Ssn ->
                  append(
                    getString(
                      R.string.warn_pii_key_ssn,
                      "${it.key}, ${it.value}",
                    ),
                  )

                PiiDetector.ColumnNamePiiIssue.CreditCard ->
                  append(
                    getString(
                      R.string.warn_pii_key_credit_card,
                      "${it.key}, ${it.value}",
                    ),
                  )

                PiiDetector.ColumnNamePiiIssue.Phone ->
                  append(
                    getString(
                      R.string.warn_pii_key_phone_number,
                      "${it.key}, ${it.value}",
                    ),
                  )

                PiiDetector.ColumnNamePiiIssue.AuthToken ->
                  append(
                    getString(
                      R.string.warn_pii_key_auth_token,
                      "${it.key}, ${it.value}",
                    ),
                  )

                null -> {}
              }

              if (it.valueError != null) {
                appendLine()
                append("- ")
              }

              when (it.valueError) {
                PiiDetector.ValuePiiIssue.DateTime ->
                  append(
                    getString(
                      R.string.warn_pii_value_date_time,
                      "${it.key}, ${it.value}",
                    ),
                  )

                PiiDetector.ValuePiiIssue.Email ->
                  append(
                    getString(
                      R.string.warn_pii_value_email,
                      "${it.key}, ${it.value}",
                    ),
                  )

                PiiDetector.ValuePiiIssue.Ip ->
                  append(
                    getString(
                      R.string.warn_pii_value_ip,
                      "${it.key}, ${it.value}",
                    ),
                  )

                PiiDetector.ValuePiiIssue.Address ->
                  append(
                    getString(
                      R.string.warn_pii_value_address,
                      "${it.key}, ${it.value}",
                    ),
                  )

                PiiDetector.ValuePiiIssue.Ssn ->
                  append(
                    getString(
                      R.string.warn_pii_value_ssn,
                      "${it.key}, ${it.value}",
                    ),
                  )

                PiiDetector.ValuePiiIssue.CreditCard ->
                  append(
                    getString(
                      R.string.warn_pii_value_credit_card,
                      "${it.key}, ${it.value}",
                    ),
                  )

                PiiDetector.ValuePiiIssue.Phone ->
                  append(
                    getString(
                      R.string.warn_pii_value_phone,
                      "${it.key}, ${it.value}",
                    ),
                  )

                PiiDetector.ValuePiiIssue.AuthToken ->
                  append(
                    getString(
                      R.string.warn_pii_value_auth_token,
                      "${it.key}, ${it.value}",
                    ),
                  )

                null -> {}
              }
            }
          }

          this.body.text = lemmyTextHelper.getSpannable(context, piiFoundWarningMd)
        }
      }
      CreatePresetViewModel.State.Complete -> {}
      is CreatePresetViewModel.State.TakePreviewScreenshot ->
        with(currentBinding as CreatePresetTakePreviewScreenshotBinding) {
          val tabletPreviewBinding: PresetPreviewScreenshotContainerBinding =
            root.getTag(R.id.tablet_binding) as PresetPreviewScreenshotContainerBinding
          val phonePreviewBinding: PresetPreviewScreenshotContainerBinding =
            root.getTag(R.id.phone_binding) as PresetPreviewScreenshotContainerBinding

          tabletPreviewBinding.screenshot.load(state.tabletPreviewSs)
          phonePreviewBinding.screenshot.load(state.phonePreviewSs)
        }
    }
  }

  private fun newDevicePreview(
    previewsContainer: ViewGroup,
    dimensionRatio: String,
  ): PresetPreviewScreenshotContainerBinding {
    val context = requireContext()

    return PresetPreviewScreenshotContainerBinding.inflate(
      LayoutInflater.from(context),
      previewsContainer,
      false,
    ).apply {
      screenshot.updateLayoutParams<ConstraintLayout.LayoutParams> {
        this.dimensionRatio = dimensionRatio
      }
    }
  }

  private fun layoutState(state: CreatePresetViewModel.State, animate: Boolean): ViewBinding {
    val context = requireContext()

    if (animate) {
      TransitionManager.beginDelayedTransition(
        binding.root,
        AutoTransition()
          .apply {
            setDuration(220)
            excludeChildren(R.id.previews_container, true)
          },
      )
    }

    binding.content.removeAllViews()

    val layoutInflater = LayoutInflater.from(context)

    return when (state) {
      is CreatePresetViewModel.State.Initial ->
        CreatePresetInitialBinding.inflate(
          layoutInflater,
          binding.content,
          true,
        ).apply {
          choosePreferencesContainer.setOnClickListener {
            PresetChangePreferencesDialogFragment.show(childFragmentManager)
          }
          fun updateCreds() {
            val name = nameEditText.text?.toString()
            val description = descriptionEditText.text?.toString()

            if (name != null && description != null) {
              viewModel.updatePresetForm(name = name, description = description)
            }
          }

          nameEditText.setText(viewModel.presetForm.value.name)
          descriptionEditText.setText(viewModel.presetForm.value.description)

          nameEditText.doOnTextChanged { text, start, before, count ->
            updateCreds()
          }
          descriptionEditText.doOnTextChanged { text, start, before, count ->
            updateCreds()
          }

          positiveButton.setOnClickListener {
            val name = nameEditText.text?.toString()
            val description = descriptionEditText.text?.toString()

            if (name.isNullOrBlank()) {
              nameInput.error = getString(R.string.required)
              return@setOnClickListener
            }
            if (name.length > nameInput.counterMaxLength) {
              return@setOnClickListener
            }
            if ((description?.length ?: 0) > descriptionInput.counterMaxLength) {
              return@setOnClickListener
            }

            nameInput.isErrorEnabled = false

            viewModel.prepareSubmitPreset(
              name = name,
              description = description,
            )
          }
        }
      CreatePresetViewModel.State.Complete -> TODO()
      is CreatePresetViewModel.State.ConfirmSubmit ->
        CreatePresetConfirmBinding.inflate(
          layoutInflater,
          binding.content,
          true,
        ).apply {
          positiveButton.setOnClickListener {
            viewModel.submitPreset()
          }
          negativeButton.setOnClickListener {
            viewModel.goBack()
          }
        }
      is CreatePresetViewModel.State.ResolveSettingIssues ->
        CreatePresetResolveSettingIssuesBinding.inflate(
          layoutInflater,
          binding.content,
          true,
        ).apply {
          positiveButton.setOnClickListener {
            viewModel.resolveSettingIssues()
          }
          negativeButton.setOnClickListener {
            viewModel.goBack()
          }
        }
      is CreatePresetViewModel.State.TakePreviewScreenshot ->
        CreatePresetTakePreviewScreenshotBinding.inflate(
          layoutInflater,
          binding.content,
          true,
        ).apply {
          val screenBounds = WindowMetricsCalculator.getOrCreate()
            .computeCurrentWindowMetrics(requireActivity())
            .bounds

          val tabletPreviewBinding = newDevicePreview(
            previewsContainer = previewsContainer,
            dimensionRatio = "1:1",
          ).apply {
            root.layoutParams = FrameLayout.LayoutParams(
              FrameLayout.LayoutParams.MATCH_PARENT,
              FrameLayout.LayoutParams.WRAP_CONTENT,
            ).apply {
              marginEnd = Utils.convertDpToPixel(64f).toInt()
            }
          }
          val phonePreviewBinding = newDevicePreview(
            previewsContainer = previewsContainer,
            dimensionRatio = "6:13",
          ).apply {
            root.layoutParams = FrameLayout.LayoutParams(
              (screenBounds.width() * 0.45f).toInt(),
              FrameLayout.LayoutParams.WRAP_CONTENT,
            ).apply {
              topMargin = Utils.convertDpToPixel(64f).toInt()

              gravity = Gravity.END or Gravity.BOTTOM
            }
          }

          previewsContainer.addView(tabletPreviewBinding.root)
          previewsContainer.addView(phonePreviewBinding.root)

          root.setTag(R.id.tablet_binding, tabletPreviewBinding)
          root.setTag(R.id.phone_binding, phonePreviewBinding)

          takePictureButton.setOnClickListener {
            takePreviewScreenshotLauncher.launch(Unit)
          }
          previewsContainer.setOnClickListener {
            takePreviewScreenshotLauncher.launch(Unit)
          }

          positiveButton.setOnClickListener {
            viewModel.submitPreviewScreenshots()
          }
          negativeButton.setOnClickListener {
            viewModel.goBack()
          }
        }
    }
  }

  fun onPresetChangePreferencesDismiss() {
    viewModel.refreshDraftPreferences()
  }
}
