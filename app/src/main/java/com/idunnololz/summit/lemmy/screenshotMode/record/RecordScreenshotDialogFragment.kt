package com.idunnololz.summit.lemmy.screenshotMode.record

import android.content.DialogInterface
import android.os.Bundle
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.setFragmentResult
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.navArgs
import com.idunnololz.summit.R
import com.idunnololz.summit.alert.AlertDialogFragment
import com.idunnololz.summit.databinding.DialogFragmentRecordScreenshotBinding
import com.idunnololz.summit.util.viewRecorder.RecordScreenshotConfig
import com.idunnololz.summit.util.BaseDialogFragment
import com.idunnololz.summit.util.ext.setSizeDynamically
import com.idunnololz.summit.util.ext.showAllowingStateLoss
import com.idunnololz.summit.util.viewRecorder.RecordingType
import kotlinx.parcelize.Parcelize

class RecordScreenshotDialogFragment :
    BaseDialogFragment<DialogFragmentRecordScreenshotBinding>() {

    companion object {
        const val REQUEST_KEY = "RecordScreenshotDialogFragment_req_key"
        const val REQUEST_KEY_RESULT = "result"

        fun show(config: RecordScreenshotConfig, fragmentManager: FragmentManager) {
            RecordScreenshotDialogFragment()
                .apply {
                    arguments = RecordScreenshotDialogFragmentArgs(
                        config
                    ).toBundle()
                }
                .showAllowingStateLoss(fragmentManager, "RecordScreenshotDialogFragment")
        }
    }

    @Parcelize
    data class Result(
        val config: RecordScreenshotConfig,
        val startRecording: Boolean,
    ) : Parcelable

    private val viewModel: RecordScreenshotViewModel by viewModels()
    private val args by navArgs<RecordScreenshotDialogFragmentArgs>()

    private var startRecording = false
    private var isConfigApplied = false

    override fun onStart() {
        super.onStart()

        setSizeDynamically(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        super.onCreateView(inflater, container, savedInstanceState)

        setBinding(DialogFragmentRecordScreenshotBinding.inflate(inflater, container, false))

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        with(binding) {
            toolbar.setTitle(R.string.save_screenshot_as_recording)
            toolbar.setNavigationIcon(R.drawable.baseline_close_24)
            toolbar.setNavigationOnClickListener {
                dismiss()
            }

            if (savedInstanceState == null && !isConfigApplied) {
                isConfigApplied = true

                viewModel.recordScreenshotConfig.value = args.config

                val config = requireNotNull(viewModel.recordScreenshotConfig.value)

                recordingLengthEditText.setText((config.recordingLengthMs / 1000L).toString())

                recordingTypePicker.check(when (config.recordingType) {
                    RecordingType.Gif -> R.id.gif_button
                    RecordingType.Mp4 -> R.id.mp4_button
                    RecordingType.Webm -> R.id.webm_button
                })
                setRecordingType(config.recordingType, resetFps = false)

                fpsSlider.value = config.maxFps.toFloat()
                qualityFactorSlider.value = config.qualityFactor.toFloat()
                resolutionScalingSlider.value = config.resolutionFactor.toFloat()

                fpsTitle.setOnClickListener {
                    AlertDialogFragment.Builder()
                        .setMessage(R.string.desc_screenshot_recording_fps)
                        .createAndShow(childFragmentManager, "fps_info")
                }
            }

            recordingTypePicker.addOnButtonCheckedListener { group, checkedId, isChecked ->
                if (!isChecked) {
                    return@addOnButtonCheckedListener
                }

                val recordingType = getRecordingType(checkedId)
                setRecordingType(recordingType, resetFps = true)
            }

            recordButton.setOnClickListener {
                startRecording = true
                dismiss()
            }
        }
    }

    private fun setRecordingType(
        recordingType: RecordingType,
        resetFps: Boolean,
    ) {
        with(binding) {
            if (resetFps) {
                when (recordingType) {
                    RecordingType.Gif -> {
                        fpsSlider.value = 10f
                    }

                    RecordingType.Mp4 -> {
                        fpsSlider.value = 32f
                    }

                    RecordingType.Webm -> {
                        fpsSlider.value = 32f
                    }
                }
            }

            when (recordingType) {
                RecordingType.Gif -> {
                    fpsSlider.valueFrom = 1.0f
                    fpsSlider.valueTo = 32.0f
                }
                RecordingType.Mp4 -> {
                    fpsSlider.valueFrom = 1.0f
                    fpsSlider.valueTo = 120.0f
                }
                RecordingType.Webm -> {
                    fpsSlider.valueFrom = 1.0f
                    fpsSlider.valueTo = 120.0f
                }
            }
        }
    }

    private fun getRecordingType(checkedId: Int) =
        when (checkedId) {
            R.id.gif_button -> RecordingType.Gif
            R.id.mp4_button -> RecordingType.Mp4
            R.id.webm_button -> RecordingType.Webm
            else -> RecordingType.Mp4
        }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)

        with(binding) {
            viewModel.recordScreenshotConfig.value = RecordScreenshotConfig(
                recordingLengthMs = recordingLengthEditText.text.toString().toLong() * 1000L,
                recordingType = getRecordingType(recordingTypePicker.checkedButtonId),
                maxFps = fpsSlider.value.toDouble(),
                qualityFactor = qualityFactorSlider.value.toDouble(),
                resolutionFactor = resolutionScalingSlider.value.toDouble(),
            )
        }

        setFragmentResult(
            REQUEST_KEY,
            bundleOf(REQUEST_KEY_RESULT to Result(
                config = requireNotNull(viewModel.recordScreenshotConfig.value),
                startRecording = startRecording,
            ))
        )
    }
}