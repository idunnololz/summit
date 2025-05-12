package com.idunnololz.summit.settings.misc

import android.os.Bundle
import android.view.View
import androidx.annotation.IdRes
import androidx.appcompat.app.AppCompatDelegate
import androidx.navigation.fragment.findNavController
import com.idunnololz.summit.BuildConfig
import com.idunnololz.summit.R
import com.idunnololz.summit.alert.newAlertDialogLauncher
import com.idunnololz.summit.api.AccountAwareLemmyClient
import com.idunnololz.summit.lemmy.LemmyTextHelper
import com.idunnololz.summit.preferences.GlobalLayoutModes
import com.idunnololz.summit.preferences.GlobalSettings
import com.idunnololz.summit.settings.BaseSettingsFragment
import com.idunnololz.summit.settings.MiscSettings
import com.idunnololz.summit.settings.SettingModelItem
import com.idunnololz.summit.settings.asCustomItem
import com.idunnololz.summit.settings.asOnOffSwitch
import com.idunnololz.summit.settings.asSingleChoiceSelectorItem
import com.idunnololz.summit.settings.dialogs.SettingValueUpdateCallback
import com.idunnololz.summit.settings.locale.LocalePickerBottomSheetFragment
import com.idunnololz.summit.util.AnimationsHelper
import com.idunnololz.summit.util.Utils
import com.idunnololz.summit.util.ext.navigateSafe
import com.idunnololz.summit.util.isPredictiveBackSupported
import com.jakewharton.processphoenix.ProcessPhoenix
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class SettingsMiscFragment :
  BaseSettingsFragment(),
  SettingValueUpdateCallback {

  companion object {
    private const val A_DAY_MS = 1000L * 60L * 60L * 24L
  }

  @Inject
  override lateinit var settings: MiscSettings

  @Inject
  lateinit var accountAwareLemmyClient: AccountAwareLemmyClient

  @Inject
  lateinit var lemmyTextHelper: LemmyTextHelper

  private val restartAppDialogLauncher = newAlertDialogLauncher("restart_app") {
    if (it.isOk) {
      ProcessPhoenix.triggerRebirth(requireContext())
    }
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)

    childFragmentManager.setFragmentResultListener(
      LocalePickerBottomSheetFragment.REQUEST_KEY,
      viewLifecycleOwner,
    ) { _, _ ->
//      updateRendering()
    }
  }

  override fun generateData(): List<SettingModelItem> {
    val context = requireContext()

    return listOf(
      settings.openLinksInExternalBrowser.asOnOffSwitch(
        { preferences.openLinksInExternalApp },
        {
          preferences.openLinksInExternalApp = it
          Utils.openExternalLinksInBrowser = preferences.openLinksInExternalApp
        },
      ),
      settings.autoLinkPhoneNumbers.asOnOffSwitch(
        { preferences.autoLinkPhoneNumbers },
        {
          preferences.autoLinkPhoneNumbers = it

          lemmyTextHelper.resetMarkwon(context)
        },
      ),
      settings.autoLinkIpAddresses.asOnOffSwitch(
        { preferences.autoLinkIpAddresses },
        {
          preferences.autoLinkIpAddresses = it

          lemmyTextHelper.resetMarkwon(context)
        },
      ),
      settings.instanceNameStyle.asSingleChoiceSelectorItem(
        { preferences.displayInstanceStyle },
        { preferences.displayInstanceStyle = it },
      ),
      settings.retainLastPost.asOnOffSwitch(
        { preferences.retainLastPost },
        {
          preferences.retainLastPost = it
        },
      ),
      settings.leftHandMode.asOnOffSwitch(
        { preferences.leftHandMode },
        {
          preferences.leftHandMode = it
        },
      ),
      settings.previewLinks.asSingleChoiceSelectorItem(
        { preferences.previewLinks },
        {
          preferences.previewLinks = it
        },
      ),
      *if (isPredictiveBackSupported()) {
        arrayOf(
          settings.usePredictiveBack.asOnOffSwitch(
            { preferences.usePredictiveBack },
            {
              preferences.usePredictiveBack = it
            },
          ),
        )
      } else {
        arrayOf()
      },
      settings.warnReplyToOldContentThresholdMs.asSingleChoiceSelectorItem(
        { convertThresholdMsToOptionId(preferences.warnReplyToOldContentThresholdMs) },
        {
          val threshold = convertOptionIdToThresholdMs(it)

          if (threshold != null) {
            preferences.warnReplyToOldContent = threshold != 0L
            preferences.warnReplyToOldContentThresholdMs = threshold
          }
          GlobalSettings.refresh(preferences)
        },
      ),
      settings.indicatePostsAndCommentsCreatedByCurrentUser.asOnOffSwitch(
        { preferences.indicatePostsAndCommentsCreatedByCurrentUser },
        {
          preferences.indicatePostsAndCommentsCreatedByCurrentUser = it
        },
      ),
      settings.saveDraftsAutomatically.asOnOffSwitch(
        { preferences.saveDraftsAutomatically },
        {
          preferences.saveDraftsAutomatically = it
        },
      ),
      settings.largeScreenSupport.asOnOffSwitch(
        { preferences.globalLayoutMode == GlobalLayoutModes.Auto },
        {
          if (it) {
            preferences.globalLayoutMode = GlobalLayoutModes.Auto
          } else {
            preferences.globalLayoutMode = GlobalLayoutModes.SmallScreen
          }
          getMainActivity()?.onPreferencesChanged()
        },
      ),
      settings.showEditedDate.asOnOffSwitch(
        { preferences.showEditedDate },
        {
          preferences.showEditedDate = it
        },
      ),
      settings.imagePreviewHideUiByDefault.asOnOffSwitch(
        { preferences.imagePreviewHideUiByDefault },
        {
          preferences.imagePreviewHideUiByDefault = it
        },
      ),
      settings.uploadImagesToImgur.asOnOffSwitch(
        { preferences.uploadImagesToImgur },
        {
          preferences.uploadImagesToImgur = it
        },
      ),
      settings.animationLevel.asSingleChoiceSelectorItem(
        { convertAnimationLevelToOptionId(preferences.animationLevel) },
        {
          preferences.animationLevel = convertOptionIdToAnimationLevel(it)
        },
      ),
      settings.shakeToSendFeedback.asOnOffSwitch(
        { preferences.shakeToSendFeedback },
        { preferences.shakeToSendFeedback = it },
      ),
      settings.showLabelsInNavBar.asOnOffSwitch(
        { preferences.showLabelsInNavBar },
        { preferences.showLabelsInNavBar = it },
      ),
      settings.showNewPersonWarning.asOnOffSwitch(
        { preferences.warnNewPerson },
        { preferences.warnNewPerson = it },
      ),
      settings.communitySelectorShowCommunitySuggestions.asOnOffSwitch(
        { preferences.communitySelectorShowCommunitySuggestions },
        { preferences.communitySelectorShowCommunitySuggestions = it },
      ),
      settings.preferredLocale.asCustomItem(
        {
          val appLocale = AppCompatDelegate.getApplicationLocales()
          val localeText = if (appLocale.size() == 0) {
            context.getString(R.string.use_system_language)
          } else {
            appLocale.get(0)?.displayLanguage
          }
          localeText ?: ""
        },
        {
          LocalePickerBottomSheetFragment.show(childFragmentManager)
        },
      ),
      settings.userAgentChoice.asSingleChoiceSelectorItem(
        { preferences.userAgentChoice },
        { preferences.userAgentChoice = it },
      ),
      *if (BuildConfig.DEBUG) {
        arrayOf(
          settings.rotateInstanceOnUploadFail.asOnOffSwitch(
            { preferences.rotateInstanceOnUploadFail },
            { preferences.rotateInstanceOnUploadFail = it },
          ),
        )
      } else {
        arrayOf()
      },
      SettingModelItem.DividerItem(R.id.divider0),
      settings.perCommunitySettings.asCustomItem {
        val direction = SettingsMiscFragmentDirections
          .actionSettingMiscFragmentToSettingPerCommunityFragment()
        findNavController().navigateSafe(direction)
      },
      SettingModelItem.DividerItem(R.id.divider1),
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
      settings.userAgentChoice.id -> {
        preferences.userAgentChoice = value as Int
      }
    }
  }
}
