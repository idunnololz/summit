package com.idunnololz.summit.aiDetector

import android.content.res.ColorStateList
import android.os.Bundle
import android.text.Spannable
import android.text.style.ForegroundColorSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.graphics.ColorUtils
import androidx.core.text.buildSpannedString
import androidx.core.view.isVisible
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.navArgs
import com.idunnololz.summit.R
import com.idunnololz.summit.api.openai.ContentProvenanceApiException
import com.idunnololz.summit.api.openai.ContentProvenanceCheck
import com.idunnololz.summit.api.openai.MissingOpenAiApiKeyException
import com.idunnololz.summit.api.openai.ProvenanceImageTooLargeException
import com.idunnololz.summit.api.openai.UnsupportedProvenanceImageException
import com.idunnololz.summit.databinding.DialogFragmentAiDetectorBinding
import com.idunnololz.summit.lemmy.LemmyTextHelper
import com.idunnololz.summit.lemmy.setMarkdown
import com.idunnololz.summit.util.BaseDialogFragment
import com.idunnololz.summit.util.StatefulData
import com.idunnololz.summit.util.ext.getColorCompat
import com.idunnololz.summit.util.ext.imageTintListCompat
import com.idunnololz.summit.util.ext.showAllowingStateLoss
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class AiDetectorDialogFragment : BaseDialogFragment<DialogFragmentAiDetectorBinding>() {

  companion object {
    fun show(fragmentManager: FragmentManager, url: String) {
      AiDetectorDialogFragment().apply {
        arguments = AiDetectorDialogFragmentArgs(url).toBundle()
      }.showAllowingStateLoss(fragmentManager, "AiDetectorDialogFragment")
    }
  }

  private val args by navArgs<AiDetectorDialogFragmentArgs>()
  private val viewModel: AiDetectorViewModel by viewModels()

  @Inject
  lateinit var lemmyTextHelper: LemmyTextHelper

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?,
  ): View {
    super.onCreateView(inflater, container, savedInstanceState)

    setBinding(DialogFragmentAiDetectorBinding.inflate(inflater, container, false))

    return binding.root
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)

    with(binding) {
      close.setOnClickListener { dismiss() }
      loadingView.setOnRefreshClickListener {
        viewModel.checkImage(args.url, force = true)
      }

      viewModel.result.observe(viewLifecycleOwner) { state ->
        val contentVisible = state is StatefulData.Success

        icon.isVisible = contentVisible
        resultTitle.isVisible = contentVisible
        resultDetails.isVisible = contentVisible
        details.isVisible = contentVisible
        disclaimer.isVisible = contentVisible

        when (state) {
          is StatefulData.NotStarted -> loadingView.hideAll()
          is StatefulData.Loading -> loadingView.showProgressBar()
          is StatefulData.Error -> {
            val message = when (state.error) {
              is MissingOpenAiApiKeyException -> getString(R.string.ai_detector_missing_api_key)
              is UnsupportedProvenanceImageException -> getString(
                R.string.ai_detector_unsupported_format,
                state.error.mimeType ?: getString(R.string.unknown),
              )
              is ProvenanceImageTooLargeException -> getString(R.string.ai_detector_image_too_large)
              is ContentProvenanceApiException ->
                state.error.message
                  ?: getString(R.string.ai_detector_request_failed)
              else -> null
            }
            binding.loadingView.showDefaultErrorMessageFor(state.error, message)
          }
          is StatefulData.Success -> showResult(state.data)
        }
      }

      viewModel.checkImage(args.url)
    }
  }

  private fun showResult(check: ContentProvenanceCheck) {
    if (!isBindingAvailable()) {
      return
    }

    with(binding) {
      val context = root.context

      loadingView.hideAll(makeViewGone = true)

      val color = if (check.hasDetectedSignal) {
        context.getColorCompat(R.color.style_red)
      } else {
        context.getColorCompat(R.color.style_green)
      }

      resultTitle.text = buildSpannedString {
        append(
          if (check.hasDetectedSignal) {
            getString(R.string.ai_detector_detected)
          } else {
            getString(R.string.ai_detector_not_detected)
          },
        )

        setSpan(
          ForegroundColorSpan(color),
          0,
          length,
          Spannable.SPAN_EXCLUSIVE_EXCLUSIVE,
        )
      }
      icon.apply {
        this.imageTintListCompat = ColorStateList.valueOf(color)
        setBackgroundColor(ColorUtils.setAlphaComponent(color, 77))

        if (check.hasDetectedSignal) {
          setImageResource(R.drawable.baseline_close_24)
        } else {
          setImageResource(R.drawable.baseline_check_24)
        }
      }
      resultDetails.text = if (check.hasDetectedSignal) {
        getString(R.string.ai_detector_detected_desc)
      } else {
        getString(R.string.ai_detector_not_detected_desc)
      }
      if (check.hasDetectedSignal) {
        if (check.results.isEmpty()) {
          details.text = getString(R.string.ai_detector_no_applicable_checks)
        } else {
          details.setMarkdown(
            lemmyTextHelper = lemmyTextHelper,
            markdown = buildString {
              for ((index, result) in check.results.withIndex()) {
                val isLast = index == check.results.lastIndex
                val signalName = if (result.type.equals("c2pa", ignoreCase = true)) {
                  getString(R.string.content_credentials)
                } else if (result.type.equals("synthid", ignoreCase = true)) {
                  "SynthID"
                } else {
                  result.type.uppercase()
                }

                append("**")
                if (result.isDetected) {
                  append(getString(R.string.ai_detector_detected_format, signalName))
                } else {
                  append(getString(R.string.ai_detector_not_detected_format, signalName))
                }
                append("**")
                appendLine("  ")
                result.validationState?.let {
                  appendLine("${getString(R.string.validation)}: $it  ")
                }
                result.issuer?.let {
                  appendLine("${getString(R.string.issuer)}: $it  ")
                }
                result.model?.let {
                  appendLine("${getString(R.string.model)}: $it  ")
                }
                result.generatedAt?.let {
                  appendLine("${getString(R.string.generated_at)}: $it  ")
                }

                if (!isLast) {
                  appendLine()
                }
              }
            },
          )
        }
        disclaimer.isVisible = false
      } else {
        details.isVisible = false
        disclaimer.isVisible = true
        disclaimer.setText(R.string.ai_detector_not_detected_disclaimer)
      }
    }
  }
}
