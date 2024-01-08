package com.idunnololz.summit.settings.navigation

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.doOnPreDraw
import androidx.fragment.app.viewModels
import com.idunnololz.summit.R
import com.idunnololz.summit.databinding.FragmentSettingNavigationBinding
import com.idunnololz.summit.databinding.SettingTextValueBinding
import com.idunnololz.summit.preferences.Preferences
import com.idunnololz.summit.settings.NavigationSettings
import com.idunnololz.summit.settings.RadioGroupSettingItem
import com.idunnololz.summit.settings.SettingPath.getPageName
import com.idunnololz.summit.settings.SettingsFragment
import com.idunnololz.summit.settings.dialogs.MultipleChoiceDialogFragment
import com.idunnololz.summit.settings.dialogs.SettingValueUpdateCallback
import com.idunnololz.summit.settings.util.bindTo
import com.idunnololz.summit.settings.util.isEnabled
import com.idunnololz.summit.util.BaseFragment
import com.idunnololz.summit.util.ext.showAllowingStateLoss
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class SettingNavigationFragment :
    BaseFragment<FragmentSettingNavigationBinding>(),
    SettingValueUpdateCallback {

    @Inject
    lateinit var preferences: Preferences

    @Inject
    lateinit var settings: NavigationSettings

    private val viewModel: SettingNavigationViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        super.onCreateView(inflater, container, savedInstanceState)

        setBinding(FragmentSettingNavigationBinding.inflate(inflater, container, false))

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val context = requireContext()

        requireMainActivity().apply {
            setupForFragment<SettingsFragment>()
            insetViewExceptTopAutomaticallyByPadding(viewLifecycleOwner, binding.scrollView)
            insetViewExceptBottomAutomaticallyByMargins(viewLifecycleOwner, binding.toolbar)

            setSupportActionBar(binding.toolbar)

            supportActionBar?.setDisplayShowHomeEnabled(true)
            supportActionBar?.setDisplayHomeAsUpEnabled(true)
            supportActionBar?.title = settings.getPageName(context)
        }

        viewModel.loadNavBarOptions()

        viewModel.navBarOptions.observe(viewLifecycleOwner) {
            updateRendering()
        }

        updateRendering()
    }

    private fun updateRendering() {
        if (!isBindingAvailable()) {
            return
        }

        val context = requireContext()

        settings.useBottomNavBar.bindTo(
            b = binding.useBottomNavBar,
            { preferences.useBottomNavBar },
            {
                preferences.useBottomNavBar = it

                updateRendering()
            },
        )

        settings.useCustomNavBar.bindTo(
            b = binding.useCustomNavBar,
            { preferences.useCustomNavBar },
            {
                preferences.useCustomNavBar = it

                updateRendering()
            },
        )
        settings.navigationRailMode.bindTo(
            binding.navigationRailMode,
            { preferences.navigationRailMode },
            { setting, currentValue ->
                MultipleChoiceDialogFragment.newInstance(setting, currentValue)
                    .showAllowingStateLoss(childFragmentManager, "aaaaaaa")
            },
        )

        binding.navBarOptions.title.setText(R.string.navigation_options)

        val enableNavBarOptions = preferences.useCustomNavBar
        val navBarOptions = viewModel.navBarOptions.value
        fun setupNavBarOption(
            index: Int,
            setting: RadioGroupSettingItem,
            binding: SettingTextValueBinding,
        ) {
            binding.isEnabled = enableNavBarOptions

            setting.bindTo(
                binding,
                { navBarOptions?.getOrNull(index) ?: NavBarDestinations.None },
                { s, currentValue ->
                    MultipleChoiceDialogFragment.newInstance(s, currentValue)
                        .showAllowingStateLoss(childFragmentManager, "navBarDest$index")
                },
            )
        }
        setupNavBarOption(0, settings.navBarDest1, binding.navBarOption1)
        setupNavBarOption(1, settings.navBarDest2, binding.navBarOption2)
        setupNavBarOption(2, settings.navBarDest3, binding.navBarOption3)
        setupNavBarOption(3, settings.navBarDest4, binding.navBarOption4)
        setupNavBarOption(4, settings.navBarDest5, binding.navBarOption5)
    }

    override fun onPause() {
        super.onPause()

        viewModel.applyChanges()

        getMainActivity()?.onPreferencesChanged()
    }

    override fun updateValue(key: Int, value: Any?) {
        when (key) {
            settings.navBarDest1.id -> {
                viewModel.updateDestination(0, value as NavBarDestId)
            }
            settings.navBarDest2.id -> {
                viewModel.updateDestination(1, value as NavBarDestId)
            }
            settings.navBarDest3.id -> {
                viewModel.updateDestination(2, value as NavBarDestId)
            }
            settings.navBarDest4.id -> {
                viewModel.updateDestination(3, value as NavBarDestId)
            }
            settings.navBarDest5.id -> {
                viewModel.updateDestination(4, value as NavBarDestId)
            }
            settings.navigationRailMode.id -> {
                preferences.navigationRailMode = value as Int
                getMainActivity()?.onPreferencesChanged()

                binding.root.doOnPreDraw {
                    getMainActivity()?.navBarController?.hideNavBar(animate = false)
                }
            }
        }

        updateRendering()
    }
}
