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
import com.idunnololz.summit.preferences.Preferences
import com.idunnololz.summit.preferences.ThemeManager
import com.idunnololz.summit.settings.OnOffSettingItem
import com.idunnololz.summit.settings.RadioGroupSettingItem
import com.idunnololz.summit.settings.SettingsFragment
import com.idunnololz.summit.settings.ui.bindTo
import com.idunnololz.summit.util.BaseFragment
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class SettingThemeFragment : BaseFragment<FragmentSettingThemeBinding>() {

    @Inject
    lateinit var preferences: Preferences
    @Inject
    lateinit var themeManager: ThemeManager

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
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
                R.id.setting_base_theme,
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
                    )
                )
            ).bindTo(
                baseThemeSetting,
                listOf(this.option1, this.option2, this.option3),
                {
                    when(preferences.getBaseTheme()) {
                        BaseTheme.UseSystem -> R.id.setting_option_use_system
                        BaseTheme.Light -> R.id.setting_option_light_theme
                        BaseTheme.Dark -> R.id.setting_option_dark_theme
                    }
                },
                {
                    when(it) {
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
                }
            )


            OnOffSettingItem(
                R.id.use_material_you,
                getString(R.string.material_you),
                getString(R.string.personalized_theming_based_on_your_wallpaper),
                false,
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
                }
            )

            OnOffSettingItem(
                R.id.use_black_theme,
                getString(R.string.black_theme),
                getString(R.string.black_theme_desc),
                false,
            ).bindTo(
                binding.useBlackTheme,
                { preferences.isBlackTheme() },
                {
                    preferences.setUseBlackTheme(it)
                    themeManager.onThemeOverlayChanged()
                }
            )
        }
    }
}