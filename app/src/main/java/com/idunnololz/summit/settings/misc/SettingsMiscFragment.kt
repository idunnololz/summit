package com.idunnololz.summit.settings.misc

import android.graphics.RectF
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.annotation.IdRes
import androidx.navigation.fragment.findNavController
import com.idunnololz.summit.BuildConfig
import com.idunnololz.summit.R
import com.idunnololz.summit.databinding.FragmentSettingsMiscBinding
import com.idunnololz.summit.lemmy.LemmyTextHelper
import com.idunnololz.summit.links.LinkContext
import com.idunnololz.summit.links.onLinkClick
import com.idunnololz.summit.preferences.GlobalLayoutModes
import com.idunnololz.summit.preferences.GlobalSettings
import com.idunnololz.summit.preferences.Preferences
import com.idunnololz.summit.settings.MiscSettings
import com.idunnololz.summit.settings.SettingPath.getPageName
import com.idunnololz.summit.settings.SettingsFragment
import com.idunnololz.summit.settings.dialogs.MultipleChoiceDialogFragment
import com.idunnololz.summit.settings.dialogs.SettingValueUpdateCallback
import com.idunnololz.summit.settings.util.bindTo
import com.idunnololz.summit.util.AnimationsHelper
import com.idunnololz.summit.util.BaseFragment
import com.idunnololz.summit.util.CustomLinkMovementMethod
import com.idunnololz.summit.util.DefaultLinkLongClickListener
import com.idunnololz.summit.util.Utils
import com.idunnololz.summit.util.ext.navigateSafe
import com.idunnololz.summit.util.ext.showAllowingStateLoss
import com.idunnololz.summit.util.insetViewExceptBottomAutomaticallyByMargins
import com.idunnololz.summit.util.insetViewExceptTopAutomaticallyByPadding
import com.idunnololz.summit.util.isPredictiveBackSupported
import com.idunnololz.summit.util.setupForFragment
import com.idunnololz.summit.util.setupToolbar
import com.idunnololz.summit.util.showMoreLinkOptions
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class SettingsMiscFragment :
    BaseFragment<FragmentSettingsMiscBinding>(),
    SettingValueUpdateCallback {

    companion object {
        private const val A_DAY_MS = 1000L * 60L * 60L * 24L
    }

    @Inject
    lateinit var preferences: Preferences

    @Inject
    lateinit var settings: MiscSettings

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        super.onCreateView(inflater, container, savedInstanceState)

        setBinding(FragmentSettingsMiscBinding.inflate(inflater, container, false))

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val context = requireContext()

        requireMainActivity().apply {
            setupForFragment<SettingsFragment>()
            insetViewExceptTopAutomaticallyByPadding(viewLifecycleOwner, binding.scrollView)
            insetViewExceptBottomAutomaticallyByMargins(viewLifecycleOwner, binding.toolbar)

            setupToolbar(binding.toolbar, settings.getPageName(context))
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
        settings.autoLinkIpAddresses.bindTo(
            binding.autoLinkIpAddresses,
            { preferences.autoLinkIpAddresses },
            {
                preferences.autoLinkIpAddresses = it

                LemmyTextHelper.autoLinkIpAddresses = it
                LemmyTextHelper.resetMarkwon(context)
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
        settings.largeScreenSupport.bindTo(
            binding.largeScreenSupport,
            { preferences.globalLayoutMode == GlobalLayoutModes.Auto },
            {
                if (it) {
                    preferences.globalLayoutMode = GlobalLayoutModes.Auto
                } else {
                    preferences.globalLayoutMode = GlobalLayoutModes.SmallScreen
                }
                getMainActivity()?.onPreferencesChanged()
            },
        )
        settings.showEditedDate.bindTo(
            binding.showEditedDate,
            { preferences.showEditedDate },
            {
                preferences.showEditedDate = it
            },
        )
        settings.imagePreviewHideUiByDefault.bindTo(
            binding.imagePreviewHideUiByDefault,
            { preferences.imagePreviewHideUiByDefault },
            {
                preferences.imagePreviewHideUiByDefault = it
            },
        )
        settings.autoPlayVideos.bindTo(
            binding.autoPlayVideos,
            { preferences.autoPlayVideos },
            {
                preferences.autoPlayVideos = it
            },
        )
        settings.uploadImagesToImgur.bindTo(
            binding.uploadImagesToImgur,
            { preferences.uploadImagesToImgur },
            {
                preferences.uploadImagesToImgur = it
            },
        )
        binding.uploadImagesToImgur.desc.movementMethod = CustomLinkMovementMethod().apply {
            onLinkClickListener = object : CustomLinkMovementMethod.OnLinkClickListener {
                override fun onClick(
                    textView: TextView,
                    url: String,
                    text: String,
                    rect: RectF,
                ): Boolean {
                    onLinkClick(url, text, LinkContext.Text)
                    return true
                }
            }
            onLinkLongClickListener = DefaultLinkLongClickListener(requireContext()) { url, text ->
                getMainActivity()?.showMoreLinkOptions(url, text)
            }
        }
        settings.animationLevel.bindTo(
            binding.animationLevel,
            { convertAnimationLevelToOptionId(preferences.animationLevel) },
            { setting, currentValue ->
                MultipleChoiceDialogFragment.newInstance(setting, currentValue)
                    .showAllowingStateLoss(childFragmentManager, "animationLevel")
            },
        )

        if (BuildConfig.DEBUG) {
            settings.rotateInstanceOnUploadFail.bindTo(
                binding.rotateInstanceOnUploadFail,
                { preferences.rotateInstanceOnUploadFail },
                { preferences.rotateInstanceOnUploadFail = it },
            )
        } else {
            binding.rotateInstanceOnUploadFail.root.visibility = View.GONE
        }
        settings.perCommunitySettings.bindTo(
            binding.perCommunitySettings,
        ) {
            val direction = SettingsMiscFragmentDirections
                .actionSettingMiscFragmentToSettingPerCommunityFragment()
            findNavController().navigateSafe(direction)
        }
        settings.shakeToSendFeedback.bindTo(
            binding.shakeToSendFeedback,
            { preferences.shakeToSendFeedback },
            { preferences.shakeToSendFeedback = it },
        )
        settings.showLabelsInNavBar.bindTo(
            binding.showLabelsInNavBar,
            { preferences.showLabelsInNavBar },
            { preferences.showLabelsInNavBar = it },
        )
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

    private fun convertAnimationLevelToOptionId(
        animationLevel: AnimationsHelper.AnimationLevel,
    ): Int {
        return when (animationLevel) {
            AnimationsHelper.AnimationLevel.Critical -> R.id.animation_level_min
            AnimationsHelper.AnimationLevel.Navigation -> R.id.animation_level_low
            AnimationsHelper.AnimationLevel.Polish -> R.id.animation_level_low
            AnimationsHelper.AnimationLevel.Extras -> R.id.animation_level_low
            AnimationsHelper.AnimationLevel.Max -> R.id.animation_level_max
        }
    }

    private fun convertOptionIdToAnimationLevel(@IdRes id: Int) = when (id) {
        R.id.animation_level_min -> AnimationsHelper.AnimationLevel.Critical
        R.id.animation_level_low -> AnimationsHelper.AnimationLevel.Navigation
        R.id.animation_level_max -> AnimationsHelper.AnimationLevel.Max
        else -> AnimationsHelper.AnimationLevel.Max
    }

    private fun convertOptionIdToThresholdMs(@IdRes id: Int) = when (id) {
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
                GlobalSettings.refresh(preferences)
            }
            settings.animationLevel.id -> {
                preferences.animationLevel = convertOptionIdToAnimationLevel(value as Int)
            }
        }

        updateRendering()
    }
}
