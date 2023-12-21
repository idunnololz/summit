package com.idunnololz.summit.settings.misc

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.IdRes
import androidx.navigation.fragment.findNavController
import com.idunnololz.summit.R
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
import com.idunnololz.summit.util.ext.navigateSafe
import com.idunnololz.summit.util.ext.showAllowingStateLoss
import com.idunnololz.summit.util.isPredictiveBackSupported
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class SettingMiscFragment :
    BaseFragment<FragmentSettingMiscBinding>(),
    SettingValueUpdateCallback {

    companion object {
        private const val A_DAY_MS = 1000L * 60L * 60L * 24L
    }

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
        settings.shareImagesDirectly.bindTo(
            binding.shareImagesDirectly,
            { preferences.shareImagesDirectly },
            {
                preferences.shareImagesDirectly = it
            },
        )
        settings.warnReplyToOldContentThresholdMs.bindTo(
            binding.warnReplyToOldContentThresholdMs,
            { convertThresholdMsToOptionId(preferences.warnReplyToOldContentThresholdMs) },
            { setting, currentValue ->
                MultipleChoiceDialogFragment.newInstance(setting, currentValue)
                    .showAllowingStateLoss(childFragmentManager, "aaaaaaa")
            },
        )
        settings.indicatePostsAndCommentsCreatedByCurrentUser.bindTo(
            binding.indicatePostsAndCommentsCreatedByCurrentUser,
            { preferences.indicatePostsAndCommentsCreatedByCurrentUser },
            {
                preferences.indicatePostsAndCommentsCreatedByCurrentUser = it
            },
        )
        settings.saveDraftsAutomatically.bindTo(
            binding.saveDraftsAutomatically,
            { preferences.saveDraftsAutomatically },
            {
                preferences.saveDraftsAutomatically = it
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
        settings.perCommunitySettings.bindTo(
            binding.perCommunitySettings,
        ) {
            val direction = SettingMiscFragmentDirections
                .actionSettingMiscFragmentToSettingPerCommunityFragment()
            findNavController().navigateSafe(direction)
        }
    }

    private fun convertThresholdMsToOptionId(warnReplyToOldContentThresholdMs: Long): Int {
        if (!preferences.warnReplyToOldContent) {
            return R.id.warn_reply_to_old_dont_warn
        }

        return when {
            warnReplyToOldContentThresholdMs <= A_DAY_MS -> R.id.warn_reply_to_old_1_day
            warnReplyToOldContentThresholdMs <= A_DAY_MS * 2 -> R.id.warn_reply_to_old_2_day
            warnReplyToOldContentThresholdMs <= A_DAY_MS * 3 -> R.id.warn_reply_to_old_3_day
            warnReplyToOldContentThresholdMs <= A_DAY_MS * 4 -> R.id.warn_reply_to_old_4_day
            warnReplyToOldContentThresholdMs <= A_DAY_MS * 5 -> R.id.warn_reply_to_old_5_day
            warnReplyToOldContentThresholdMs <= A_DAY_MS * 7 -> R.id.warn_reply_to_old_week
            warnReplyToOldContentThresholdMs <= A_DAY_MS * 30 -> R.id.warn_reply_to_old_month
            else -> R.id.warn_reply_to_old_year
        }
    }

    private fun convertOptionIdToThresholdMs(@IdRes id: Int) =
        when (id) {
            R.id.warn_reply_to_old_dont_warn -> 0L
            R.id.warn_reply_to_old_1_day -> A_DAY_MS
            R.id.warn_reply_to_old_2_day -> A_DAY_MS * 2
            R.id.warn_reply_to_old_3_day -> A_DAY_MS * 3
            R.id.warn_reply_to_old_4_day -> A_DAY_MS * 4
            R.id.warn_reply_to_old_5_day -> A_DAY_MS * 5
            R.id.warn_reply_to_old_week -> A_DAY_MS * 7
            R.id.warn_reply_to_old_month -> A_DAY_MS * 30
            R.id.warn_reply_to_old_year -> A_DAY_MS * 365
            else -> null
        }

    override fun updateValue(key: Int, value: Any?) {
        when (key) {
            settings.instanceNameStyle.id -> {
                preferences.displayInstanceStyle = value as Int
            }
            settings.previewLinks.id -> {
                preferences.previewLinks = value as Int
            }
            settings.warnReplyToOldContentThresholdMs.id -> {
                val threshold = convertOptionIdToThresholdMs(value as Int)

                if (threshold != null) {
                    preferences.warnReplyToOldContent = threshold != 0L
                    preferences.warnReplyToOldContentThresholdMs = threshold
                }
            }
            settings.navigationRailMode.id -> {
                preferences.navigationRailMode = value as Int
                getMainActivity()?.onPreferencesChanged()
            }
        }

        updateRendering()
    }
}
