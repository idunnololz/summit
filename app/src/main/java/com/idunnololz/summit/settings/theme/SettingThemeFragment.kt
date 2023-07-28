package com.idunnololz.summit.settings.theme

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.color.DynamicColors
import com.idunnololz.summit.R
import com.idunnololz.summit.alert.AlertDialogFragment
import com.idunnololz.summit.databinding.FragmentSettingThemeBinding
import com.idunnololz.summit.preferences.BaseTheme
import com.idunnololz.summit.preferences.GlobalFontColorId
import com.idunnololz.summit.preferences.GlobalFontSizeId
import com.idunnololz.summit.preferences.Preferences
import com.idunnololz.summit.preferences.ThemeManager
import com.idunnololz.summit.settings.OnOffSettingItem
import com.idunnololz.summit.settings.RadioGroupSettingItem
import com.idunnololz.summit.settings.SettingsFragment
import com.idunnololz.summit.settings.ThemeSettings
import com.idunnololz.summit.settings.ui.bindTo
import com.idunnololz.summit.settings.ui.bindToMultiView
import com.idunnololz.summit.util.BaseFragment
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class SettingThemeFragment : BaseFragment<FragmentSettingThemeBinding>() {

    @Inject
    lateinit var preferences: Preferences

    @Inject
    lateinit var themeManager: ThemeManager

    @Inject
    lateinit var themeSettings: ThemeSettings

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        super.onCreateView(inflater, container, savedInstanceState)

        setBinding(FragmentSettingThemeBinding.inflate(inflater, container, false))

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        runOnReady {
            setup()
        }
    }

    private fun setup() {
        if (!isBindingAvailable()) return

        val context = requireContext()
        val parentActivity = requireMainActivity()

        requireMainActivity().apply {
            setupForFragment<SettingsFragment>(animate = false)

            insetViewExceptTopAutomaticallyByPadding(viewLifecycleOwner, binding.scrollView)
            insetViewExceptBottomAutomaticallyByMargins(viewLifecycleOwner, binding.toolbar)

            setSupportActionBar(binding.toolbar)

            supportActionBar?.setDisplayShowHomeEnabled(true)
            supportActionBar?.setDisplayHomeAsUpEnabled(true)
            supportActionBar?.title = context.getString(R.string.theme)
        }

        with(binding) {
            RadioGroupSettingItem(
                0,
                getString(R.string.base_theme),
                null,
                listOf(
                    RadioGroupSettingItem.RadioGroupOption(
                        R.id.setting_option_use_system,
                        getString(R.string.use_system_theme),
                        null,
                        R.drawable.baseline_auto_awesome_24,
                    ),
                    RadioGroupSettingItem.RadioGroupOption(
                        R.id.setting_option_light_theme,
                        getString(R.string.light_theme),
                        null,
                        R.drawable.baseline_light_mode_24,
                    ),
                    RadioGroupSettingItem.RadioGroupOption(
                        R.id.setting_option_dark_theme,
                        getString(R.string.dark_theme),
                        null,
                        R.drawable.baseline_dark_mode_24,
                    ),
                ),
            ).bindToMultiView(
                baseThemeSetting,
                listOf(this.option1, this.option2, this.option3),
                {
                    when (preferences.getBaseTheme()) {
                        BaseTheme.UseSystem -> R.id.setting_option_use_system
                        BaseTheme.Light -> R.id.setting_option_light_theme
                        BaseTheme.Dark -> R.id.setting_option_dark_theme
                    }
                },
                {
                    when (it) {
                        R.id.setting_option_use_system ->
                            preferences.setBaseTheme(BaseTheme.UseSystem)
                        R.id.setting_option_light_theme ->
                            preferences.setBaseTheme(BaseTheme.Light)
                        R.id.setting_option_dark_theme ->
                            preferences.setBaseTheme(BaseTheme.Dark)
                    }

                    binding.root.post {
                        themeManager.applyThemeFromPreferences()
                    }
                },
            )

            OnOffSettingItem(
                null,
                getString(R.string.material_you),
                getString(R.string.personalized_theming_based_on_your_wallpaper),
            ).bindTo(
                binding.useMaterialYou,
                { preferences.isUseMaterialYou() },
                {
                    if (DynamicColors.isDynamicColorAvailable()) {
                        preferences.setUseMaterialYou(it)
                        themeManager.applyThemeFromPreferences()
                    } else if (it) {
                        AlertDialogFragment.Builder()
                            .setMessage(R.string.error_dynamic_color_not_supported)
                            .createAndShow(childFragmentManager, "Asdfff")
                        preferences.setUseMaterialYou(false)
                    } else {
                        preferences.setUseMaterialYou(false)
                    }
                },
            )

            themeSettings.colorScheme.bindTo(
                binding.colorScheme,
                { preferences.colorScheme },
                {
                    ColorSchemePickerDialogFragment()
                        .show(childFragmentManager, "asdaa")
                }
            )

            themeSettings.blackTheme.bindTo(
                binding.useBlackTheme,
                { preferences.isBlackTheme() },
                {
                    preferences.setUseBlackTheme(it)
                    themeManager.onThemeOverlayChanged()
                },
            )

            themeSettings.fontSize.bindTo(
                activity = parentActivity,
                b = binding.fontSize,
                choices = mapOf(
                    GlobalFontSizeId.Small to getString(R.string.small),
                    GlobalFontSizeId.Normal to getString(R.string.normal),
                    GlobalFontSizeId.Large to getString(R.string.large),
                    GlobalFontSizeId.ExtraLarge to getString(R.string.extra_large),
                ),
                getCurrentChoice = {
                    preferences.globalFontSize
                },
                onChoiceSelected = {
                    preferences.globalFontSize = it
                    themeManager.onThemeOverlayChanged()
                },
            )

            themeSettings.fontColor.bindTo(
                activity = parentActivity,
                b = binding.fontColor,
                choices = mapOf(
                    GlobalFontColorId.Calm to getString(R.string.calm),
                    GlobalFontColorId.HighContrast to getString(R.string.high_contrast)
                ),
                getCurrentChoice = {
                    preferences.globalFontColor
                },
                onChoiceSelected = {
                    preferences.globalFontColor = it
                    themeManager.onThemeOverlayChanged()
                },
            )
        }
    }
}
