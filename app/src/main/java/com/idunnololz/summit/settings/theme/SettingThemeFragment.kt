package com.idunnololz.summit.settings.theme

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.color.DynamicColors
import com.idunnololz.summit.R
import com.idunnololz.summit.alert.AlertDialogFragment
import com.idunnololz.summit.databinding.FragmentSettingThemeBinding
import com.idunnololz.summit.lemmy.postAndCommentView.PostAndCommentViewBuilder
import com.idunnololz.summit.preferences.BaseTheme
import com.idunnololz.summit.preferences.ColorSchemes
import com.idunnololz.summit.preferences.GlobalFontColorId
import com.idunnololz.summit.preferences.GlobalFontSizeId
import com.idunnololz.summit.preferences.Preferences
import com.idunnololz.summit.preferences.ThemeManager
import com.idunnololz.summit.settings.SettingPath.getPageName
import com.idunnololz.summit.settings.SettingsFragment
import com.idunnololz.summit.settings.ThemeSettings
import com.idunnololz.summit.settings.dialogs.MultipleChoiceDialogFragment
import com.idunnololz.summit.settings.util.bindTo
import com.idunnololz.summit.settings.util.bindToMultiView
import com.idunnololz.summit.util.BaseFragment
import com.idunnololz.summit.util.ext.getColorCompat
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject


@AndroidEntryPoint
class SettingThemeFragment : BaseFragment<FragmentSettingThemeBinding>() {

    @Inject
    lateinit var preferences: Preferences

    @Inject
    lateinit var themeManager: ThemeManager

    @Inject
    lateinit var settings: ThemeSettings

    @Inject
    lateinit var postAndCommentViewBuilder: PostAndCommentViewBuilder

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
            supportActionBar?.title = settings.getPageName(context)
        }

        with(binding) {
            settings.baseTheme.bindToMultiView(
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

            binding.themeConfiguration.title.text = getString(R.string.theme_config)
            settings.materialYou.bindTo(
                binding.useMaterialYou,
                { preferences.isUseMaterialYou() },
                {
                    if (DynamicColors.isDynamicColorAvailable()) {
                        preferences.setUseMaterialYou(it)
                        preferences.colorScheme = ColorSchemes.Default
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

            settings.colorScheme.bindTo(
                binding.colorScheme,
                { preferences.colorScheme },
                { setting, currentValue ->
                    MultipleChoiceDialogFragment.newInstance(setting, currentValue)
                        .show(childFragmentManager, "asdaa")
                },
            )

            binding.darkThemeSettings.title.text = getString(R.string.dark_theme_settings)
            settings.blackTheme.bindTo(
                binding.useBlackTheme,
                { preferences.isBlackTheme() },
                {
                    preferences.setUseBlackTheme(it)
                    themeManager.onThemeOverlayChanged()
                },
            )

            binding.fontStyle.title.text = getString(R.string.font_style)

            settings.font.bindTo(
                binding.font,
                { preferences.globalFont },
                { setting, currentValue ->
                    MultipleChoiceDialogFragment.newInstance(setting, currentValue)
                        .show(childFragmentManager, "FontPickerDialogFragment")
                },
            )

//            settings.font.bindTo(
//                activity = parentActivity,
//                b = binding.fontSize,
//                choices = mapOf(
//                    com.idunnololz.summit.preferences.GlobalFontSizeId.Small to getString(com.idunnololz.summit.R.string.small),
//                    com.idunnololz.summit.preferences.GlobalFontSizeId.Normal to getString(com.idunnololz.summit.R.string.normal),
//                    com.idunnololz.summit.preferences.GlobalFontSizeId.Large to getString(com.idunnololz.summit.R.string.large),
//                    com.idunnololz.summit.preferences.GlobalFontSizeId.ExtraLarge to getString(com.idunnololz.summit.R.string.extra_large),
//                ),
//                getCurrentChoice = {
//                    preferences.globalFontSize
//                },
//                onChoiceSelected = {
//                    preferences.globalFontSize = it
//                    themeManager.onThemeOverlayChanged()
//                },
//            )

            settings.fontSize.bindTo(
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

            settings.fontColor.bindTo(
                activity = parentActivity,
                b = binding.fontColor,
                choices = mapOf(
                    GlobalFontColorId.Calm to getString(R.string.calm),
                    GlobalFontColorId.HighContrast to getString(R.string.high_contrast),
                ),
                getCurrentChoice = {
                    preferences.globalFontColor
                },
                onChoiceSelected = {
                    preferences.globalFontColor = it
                    themeManager.onThemeOverlayChanged()
                },
            )

            binding.misc.title.text = getString(R.string.misc)
            settings.upvoteColor.bindTo(
                binding.upvoteColor,
                { preferences.upvoteColor },
                {
                    preferences.upvoteColor = it

                    postAndCommentViewBuilder.onPreferencesChanged()
                },
                { context.getColorCompat(R.color.upvoteColor) }
            )
            settings.downvoteColor.bindTo(
                binding.downvoteColor,
                { preferences.downvoteColor },
                {
                    preferences.downvoteColor = it

                    postAndCommentViewBuilder.onPreferencesChanged()
                },
                { context.getColorCompat(R.color.downvoteColor) }
            )
        }
    }
}
