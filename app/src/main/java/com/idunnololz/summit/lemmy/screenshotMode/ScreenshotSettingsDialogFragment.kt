package com.idunnololz.summit.lemmy.screenshotMode

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.idunnololz.summit.databinding.DialogFragmentScreenshotSettingsBinding
import com.idunnololz.summit.preferences.Preferences
import com.idunnololz.summit.preferences.ScreenshotWatermarkId
import com.idunnololz.summit.util.BaseDialogFragment
import com.idunnololz.summit.util.ext.setSizeDynamically
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class ScreenshotSettingsDialogFragment :
    BaseDialogFragment<DialogFragmentScreenshotSettingsBinding>() {

    @Inject
    lateinit var preferences: Preferences

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

        setBinding(DialogFragmentScreenshotSettingsBinding.inflate(inflater, container, false))

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        with(binding) {
            screenshotWidth.setText(preferences.screenshotWidthDp.toString())
            dateScreenshots.isChecked = preferences.dateScreenshots
            watermarkScreenshots.isChecked =
                preferences.screenshotWatermark == ScreenshotWatermarkId.LEMMY

            positiveButton.setOnClickListener {
                var width = binding.screenshotWidth.text?.toString()?.toIntOrNull() ?: 0
                width = width.coerceIn(100, 1000)

                preferences.screenshotWidthDp = width
                preferences.dateScreenshots = dateScreenshots.isChecked
                if (watermarkScreenshots.isChecked) {
                    preferences.screenshotWatermark = ScreenshotWatermarkId.LEMMY
                } else {
                    preferences.screenshotWatermark = ScreenshotWatermarkId.OFF
                }

                (parentFragment as? ScreenshotModeDialogFragment)?.generateScreenshot()

                dismiss()
            }
            negativeButton.setOnClickListener {
                dismiss()
            }
        }
    }
}
