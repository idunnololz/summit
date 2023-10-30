package com.idunnololz.summit.settings.misc

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.idunnololz.summit.BuildConfig
import com.idunnololz.summit.databinding.FragmentSettingMiscBinding
import com.idunnololz.summit.hidePosts.HiddenPostsManager
import com.idunnololz.summit.lemmy.LemmyTextHelper
import com.idunnololz.summit.preferences.Preferences
import com.idunnololz.summit.settings.MiscSettings
import com.idunnololz.summit.settings.SettingPath.getPageName
import com.idunnololz.summit.settings.SettingsFragment
import com.idunnololz.summit.settings.dialogs.MultipleChoiceDialogFragment
import com.idunnololz.summit.settings.dialogs.SettingValueUpdateCallback
import com.idunnololz.summit.settings.util.bindTo
import com.idunnololz.summit.util.BaseFragment
import com.idunnololz.summit.util.Utils
import com.idunnololz.summit.util.ext.showAllowingStateLoss
import com.idunnololz.summit.util.isPredictiveBackSupported
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class SettingMiscFragment :
    BaseFragment<FragmentSettingMiscBinding>(),
    SettingValueUpdateCallback {

    @Inject
    lateinit var preferences: Preferences

    @Inject
    lateinit var hiddenPostsManager: HiddenPostsManager

    @Inject
    lateinit var settings: MiscSettings

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        super.onCreateView(inflater, container, savedInstanceState)

        setBinding(FragmentSettingMiscBinding.inflate(inflater, container, false))

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

        updateRendering()
    }

    private fun updateRendering() {
        if (!isBindingAvailable()) {
            return
        }

        val context = requireContext()

        settings.openLinksInExternalBrowser.bindTo(
            b = binding.openLinksInApp,
            { preferences.openLinksInExternalApp },
            {
                preferences.openLinksInExternalApp = it
                Utils.openExternalLinksInBrowser = preferences.openLinksInExternalApp
            },
        )
        settings.autoLinkPhoneNumbers.bindTo(
            binding.autoLinkPhoneNumbers,
            { preferences.autoLinkPhoneNumbers },
            {
                preferences.autoLinkPhoneNumbers = it

                LemmyTextHelper.autoLinkPhoneNumbers = it
                LemmyTextHelper.resetMarkwon(context)
            },
        )
        settings.showUpAndDownVotes.bindTo(
            binding.showUpAndDownVotes,
            { preferences.showUpAndDownVotes },
            {
                preferences.showUpAndDownVotes = it
            },
        )
        settings.instanceNameStyle.bindTo(
            binding.instanceNameStyle,
            { preferences.displayInstanceStyle },
            { setting, currentValue ->
                MultipleChoiceDialogFragment.newInstance(setting, currentValue)
                    .showAllowingStateLoss(childFragmentManager, "aaaaaaa")
            },
        )
        settings.retainLastPost.bindTo(
            binding.restoreLastPost,
            { preferences.retainLastPost },
            {
                preferences.retainLastPost = it
            },
        )
        settings.leftHandMode.bindTo(
            binding.leftHandMode,
            { preferences.leftHandMode },
            {
                preferences.leftHandMode = it
            },
        )
        settings.transparentNotificationBar.bindTo(
            binding.transparentNotificationBar,
            { preferences.transparentNotificationBar },
            {
                preferences.transparentNotificationBar = it
                getMainActivity()?.onPreferencesChanged()
            },
        )
        settings.previewLinks.bindTo(
            binding.previewLinks,
            { preferences.previewLinks },
            { setting, currentValue ->
                MultipleChoiceDialogFragment.newInstance(setting, currentValue)
                    .showAllowingStateLoss(childFragmentManager, "aaaaaaa")
            },
        )
        if (isPredictiveBackSupported()) {
            settings.usePredictiveBack.bindTo(
                binding.usePredictiveBack,
                { preferences.usePredictiveBack },
                {
                    preferences.usePredictiveBack = it
                },
            )
        } else {
            binding.usePredictiveBack.root.visibility = View.GONE
        }
    }

    override fun updateValue(key: Int, value: Any?) {
        when (key) {
            settings.instanceNameStyle.id -> {
                preferences.displayInstanceStyle = value as Int
            }
            settings.previewLinks.id -> {
                preferences.previewLinks = value as Int
            }
        }

        updateRendering()
    }
}
