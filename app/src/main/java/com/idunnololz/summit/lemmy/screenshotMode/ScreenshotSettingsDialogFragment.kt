package com.idunnololz.summit.lemmy.screenshotMode

import android.R.attr.icon
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.PopupMenu
import androidx.fragment.app.viewModels
import com.idunnololz.summit.R
import com.idunnololz.summit.databinding.DialogFragmentScreenshotSettingsBinding
import com.idunnololz.summit.preferences.ScreenshotWatermarkId
import com.idunnololz.summit.util.BaseDialogFragment
import com.idunnololz.summit.util.ext.getColorFromAttribute
import com.idunnololz.summit.util.ext.getDrawableCompat
import com.idunnololz.summit.util.ext.setSizeDynamically
import com.idunnololz.summit.util.ext.tint
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ScreenshotSettingsDialogFragment :
  BaseDialogFragment<DialogFragmentScreenshotSettingsBinding>() {

  private val viewModel: ScreenshotSettingsViewModel by viewModels()

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

    setBinding(
      DialogFragmentScreenshotSettingsBinding.inflate(
        inflater,
        container,
        false,
      ),
    )

    return binding.root
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)

    val context = requireContext()

    with(binding) {
      val preferences = viewModel.preferences
      val color = context.getColorFromAttribute(androidx.appcompat.R.attr.colorControlNormal)

      screenshotWidth.setText(preferences.screenshotWidthDp.toString())
      dateScreenshots.isChecked = preferences.dateScreenshots
      viewModel.screenshotWatermarkId.value = preferences.screenshotWatermark

      viewModel.screenshotWatermarkId.observe(viewLifecycleOwner) {
        watermarkChoice.setImageResource(
          when (it) {
            ScreenshotWatermarkId.PIEFED -> {
              R.drawable.ic_piefed_24
            }
            ScreenshotWatermarkId.LEMMY -> {
              R.drawable.ic_lemmy_24
            }
            ScreenshotWatermarkId.SUMMIT -> {
              R.drawable.ic_logo_mono_24
            }
            ScreenshotWatermarkId.OFF -> {
              0
            }
            else ->
              0
          }
        )
      }

      watermarkChoice.setOnClickListener {
        PopupMenu(
          context,
          watermarkChoice,
          Gravity.NO_GRAVITY,
          0,
          R.style.Theme_App_Widget_Material3_PopupMenu_Overflow,
        )
          .apply {
            menu.add(0, ScreenshotWatermarkId.LEMMY, 0, R.string.lemmy)
              .apply { icon = context.getDrawableCompat(R.drawable.ic_lemmy_24)?.tint(color) }
            menu.add(0, ScreenshotWatermarkId.SUMMIT, 0, R.string.summit)
              .apply { icon = context.getDrawableCompat(R.drawable.ic_logo_mono_24)?.tint(color) }
            menu.add(0, ScreenshotWatermarkId.PIEFED, 0, R.string.piefed)
              .apply { icon = context.getDrawableCompat(R.drawable.ic_piefed_24)?.tint(color) }
            menu.add(0, ScreenshotWatermarkId.OFF, 0, R.string.no_watermark)
              .apply { icon = context.getDrawableCompat(R.drawable.baseline_block_24)?.tint(color) }

            setForceShowIcon(true)

            setOnMenuItemClickListener {
              viewModel.screenshotWatermarkId.value = it.itemId
              true
            }
          }
          .show()
      }

      positiveButton.setOnClickListener {
        var width = binding.screenshotWidth.text?.toString()?.toIntOrNull() ?: 0
        width = width.coerceIn(100, 1000)

        preferences.screenshotWidthDp = width
        preferences.dateScreenshots = dateScreenshots.isChecked

        viewModel.screenshotWatermarkId.value?.let { screenshotWatermark ->
          preferences.screenshotWatermark = screenshotWatermark
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
