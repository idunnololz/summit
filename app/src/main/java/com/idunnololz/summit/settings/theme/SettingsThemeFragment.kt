package com.idunnololz.summit.settings.theme

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.navArgs
import com.google.android.material.color.DynamicColors
import com.idunnololz.summit.R
import com.idunnololz.summit.account.fullName
import com.idunnololz.summit.alert.OldAlertDialogFragment
import com.idunnololz.summit.databinding.FragmentSettingsThemeBinding
import com.idunnololz.summit.lemmy.postAndCommentView.PostAndCommentViewBuilder
import com.idunnololz.summit.preferences.BaseTheme
import com.idunnololz.summit.preferences.ColorSchemes
import com.idunnololz.summit.preferences.GlobalFontColorId
import com.idunnololz.summit.preferences.GlobalFontSizeId
import com.idunnololz.summit.preferences.Preferences
import com.idunnololz.summit.preferences.ThemeManager
import com.idunnololz.summit.settings.BasicSettingItem
import com.idunnololz.summit.settings.PreferencesViewModel
import com.idunnololz.summit.settings.SettingPath.getPageName
import com.idunnololz.summit.settings.SettingsFragment
import com.idunnololz.summit.settings.ThemeSettings
import com.idunnololz.summit.settings.util.bindTo
import com.idunnololz.summit.settings.util.bindToMultiView
import com.idunnololz.summit.util.BaseFragment
import com.idunnololz.summit.util.ext.getColorCompat
import com.idunnololz.summit.util.insetViewExceptBottomAutomaticallyByMargins
import com.idunnololz.summit.util.insetViewExceptTopAutomaticallyByPadding
import com.idunnololz.summit.util.setupForFragment
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class SettingsThemeFragment : BaseFragment<FragmentSettingsThemeBinding>() {

    private val args: SettingsThemeFragmentArgs by navArgs()

    private val preferencesViewModel: PreferencesViewModel by viewModels()

    lateinit var preferences: Preferences

    @Inject
    lateinit var themeManager: ThemeManager

    @Inject
    lateinit var settings: ThemeSettings

    @Inject
    lateinit var postAndCommentViewBuilder: PostAndCommentViewBuilder

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        preferences = preferencesViewModel.getPreferences(args.account)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        super.onCreateView(inflater, container, savedInstanceState)

        setBinding(FragmentSettingsThemeBinding.inflate(inflater, container, false))

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

        val account = args.account
        requireMainActivity().apply {
            setupForFragment<SettingsFragment>(animate = false)

            insetViewExceptTopAutomaticallyByPadding(viewLifecycleOwner, binding.scrollView)
            insetViewExceptBottomAutomaticallyByMargins(viewLifecycleOwner, binding.toolbar)

            setSupportActionBar(binding.toolbar)

            supportActionBar?.setDisplayShowHomeEnabled(true)
            supportActionBar?.setDisplayHomeAsUpEnabled(true)
            supportActionBar?.title = settings.getPageName(context)
            supportActionBar?.subtitle = account?.fullName
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
                        themeManager.onPreferencesChanged()
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
                        themeManager.onPreferencesChanged()
                    } else if (it) {
                        OldAlertDialogFragment.Builder()
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
                    ColorSchemePickerDialogFragment.newInstance(account)
                        .show(childFragmentManager, "asdaa")
                },
            )

            binding.darkThemeSettings.title.text = getString(R.string.dark_theme_settings)
            settings.blackTheme.bindTo(
                binding.useBlackTheme,
                { preferences.isBlackTheme() },
                {
                    preferences.setUseBlackTheme(it)
                    themeManager.onPreferencesChanged()
                },
            )
            settings.lessDarkBackgroundTheme.bindTo(
                binding.lessDarkBackgroundTheme,
                { preferences.useLessDarkBackgroundTheme },
                {
                    preferences.useLessDarkBackgroundTheme = it
                    themeManager.onPreferencesChanged()
                },
            )

            binding.fontStyle.title.text = getString(R.string.font_style)

            settings.font.bindTo(
                binding.font,
                { preferences.globalFont },
                { setting, currentValue ->
                    FontPickerDialogFragment.newInstance(account)
                        .show(childFragmentManager, "FontPickerDialogFragment")
                },
            )

            settings.fontSize.bindTo(
                activity = parentActivity,
                b = binding.fontSize,
                choices = mapOf(
                    GlobalFontSizeId.Small to getString(R.string.small),
                    GlobalFontSizeId.Normal to getString(R.string.normal),
                    GlobalFontSizeId.Large to getString(R.string.large),
                    GlobalFontSizeId.ExtraLarge to getString(R.string.extra_large),
                    GlobalFontSizeId.Xxl to getString(R.string.xxl),
                    GlobalFontSizeId.Xxxl to getString(R.string.xxxl),
                ),
                getCurrentChoice = {
                    preferences.globalFontSize
                },
                onChoiceSelected = {
                    preferences.globalFontSize = it
                    themeManager.onPreferencesChanged()
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
                    themeManager.onPreferencesChanged()
                },
            )

            binding.voteColors.title.text = getString(R.string.vote_colors)
            settings.upvoteColor.bindTo(
                binding.upvoteColor,
                { preferences.upvoteColor },
                {
                    preferences.upvoteColor = it

                    postAndCommentViewBuilder.onPreferencesChanged()
                },
                { context.getColorCompat(R.color.upvoteColor) },
            )
            settings.downvoteColor.bindTo(
                binding.downvoteColor,
                { preferences.downvoteColor },
                {
                    preferences.downvoteColor = it

                    postAndCommentViewBuilder.onPreferencesChanged()
                },
                { context.getColorCompat(R.color.downvoteColor) },
            )
            BasicSettingItem(
                null,
                context.getString(R.string.swap_colors),
                null,
            ).bindTo(
                binding.swapColors,
            ) {
                val upvoteColor = preferences.downvoteColor
                val downvoteColor = preferences.upvoteColor

                preferences.upvoteColor = upvoteColor
                preferences.downvoteColor = downvoteColor

                postAndCommentViewBuilder.onPreferencesChanged()
                setup()
            }

            binding.misc.title.text = getString(R.string.misc)
            settings.transparentNotificationBar.bindTo(
                binding.transparentNotificationBar,
                { preferences.transparentNotificationBar },
                {
                    preferences.transparentNotificationBar = it
                    getMainActivity()?.onPreferencesChanged()
                },
            )
        }
    }
}
