package com.idunnololz.summit.settings.webSettings

import android.app.Activity
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.MenuProvider
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView.Adapter
import com.github.drjacky.imagepicker.ImagePicker
import com.google.android.material.snackbar.Snackbar
import com.idunnololz.summit.R
import com.idunnololz.summit.alert.launchAlertDialog
import com.idunnololz.summit.alert.newAlertDialogLauncher
import com.idunnololz.summit.settings.BaseSettingsFragment
import com.idunnololz.summit.settings.ImageValueSettingItem
import com.idunnololz.summit.settings.LemmyWebSettings
import com.idunnololz.summit.settings.SettingItemsAdapter
import com.idunnololz.summit.settings.SettingModelItem
import com.idunnololz.summit.settings.asCustomItem
import com.idunnololz.summit.settings.asCustomItemWithTextEditorDialog
import com.idunnololz.summit.settings.asImageValueItem
import com.idunnololz.summit.settings.asOnOffSwitch
import com.idunnololz.summit.settings.asSingleChoiceSelectorItem
import com.idunnololz.summit.settings.dialogs.SettingValueUpdateCallback
import com.idunnololz.summit.settings.webSettings.changePassword.ChangePasswordDialogFragment
import com.idunnololz.summit.util.AnimationsHelper
import com.idunnololz.summit.util.BottomMenu
import com.idunnololz.summit.util.StatefulData
import com.idunnololz.summit.util.ext.navigateSafe
import com.idunnololz.summit.util.ext.setup
import com.idunnololz.summit.util.toErrorMessage
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class SettingsWebFragment :
  BaseSettingsFragment(),
  SettingValueUpdateCallback {

  private val viewModel: SettingsWebViewModel by viewModels()

  @Inject
  override lateinit var settings: LemmyWebSettings

  @Inject
  lateinit var animationsHelper: AnimationsHelper

  private val saveChangesDialogLauncher = newAlertDialogLauncher("unsaved_changes") {
    if (it.isOk) {
      save()
    } else {
      findNavController().navigateUp()
    }
  }

  private val backPressHandler = object : OnBackPressedCallback(false) {
    override fun handleOnBackPressed() {
      saveChangesDialogLauncher.launchDialog {
        titleResId = R.string.error_unsaved_changes
        messageResId = R.string.error_web_settings_unsaved_changes_desc
        positionButtonResId = R.string.save
        negativeButtonResId = R.string.discard_saves
      }
    }
  }
  private val launcher =
    registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
      if (it.resultCode == Activity.RESULT_OK) {
        val uri = it.data?.data!!

        viewModel.uploadImage(uri)
      }
    }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)

    val context = requireContext()

    requireSummitActivity().apply {
      binding.toolbar.addMenuProvider(
        object : MenuProvider {
          override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
            menuInflater.inflate(R.menu.menu_setting_web, menu)
          }

          override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
            return when (menuItem.itemId) {
              R.id.ca_save -> {
                save()
                true
              }
              else -> false
            }
          }
        },
      )

      onBackPressedDispatcher.addCallback(viewLifecycleOwner, backPressHandler)
    }

    viewModel.accountData.observe(viewLifecycleOwner) {
      when (it) {
        is StatefulData.Error -> {
          binding.loadingView.showDefaultErrorMessageFor(it.error)
          binding.loadingView.setOnRefreshClickListener {
            viewModel.fetchAccountInfo(settings)
          }
        }
        is StatefulData.Loading -> binding.loadingView.showProgressBar()
        is StatefulData.NotStarted -> {}
        is StatefulData.Success -> {
          binding.loadingView.hideAll()
          refresh()
          backPressHandler.isEnabled = viewModel.updatedSettingValues.isNotEmpty()
        }
      }
    }
    viewModel.saveUserSettings.observe(viewLifecycleOwner) {
      when (it) {
        is StatefulData.Error -> {
          binding.loadingView.hideAll()

          launchAlertDialog("no_changes") {
            titleResId = R.string.error_save_failed
            message = it.error.toErrorMessage(context)
            positionButtonResId = android.R.string.ok
          }
        }
        is StatefulData.Loading -> binding.loadingView.showProgressBar()
        is StatefulData.NotStarted -> {}
        is StatefulData.Success -> {
          binding.loadingView.hideAll()
          viewModel.updatedSettingValues.clear()

          Snackbar.make(binding.root, R.string.settings_saved, Snackbar.LENGTH_LONG)
            .show()
        }
      }
    }
    viewModel.uploadImageStatus.observe(viewLifecycleOwner) {
      when (it) {
        is StatefulData.Error -> {
          binding.loadingView.hideAll()
          launchAlertDialog("upload_error") {
            titleResId = R.string.error_upload_failed
            message = it.error.toErrorMessage(context)
            positionButtonResId = android.R.string.ok
          }
        }
        is StatefulData.Loading -> binding.loadingView.showProgressBar()
        is StatefulData.NotStarted -> {}
        is StatefulData.Success -> {
          binding.loadingView.hideAll()
          viewModel.updateSettingValue(settings, it.data.first, it.data.second)
        }
      }
    }

    viewModel.fetchAccountInfo(settings)
  }

  override fun generateData(): List<SettingModelItem> {
    val accountData = viewModel.accountData.valueOrNull ?: return listOf()
    val settingValues = accountData.settingValues

    return listOf(
      settings.instanceSetting.asCustomItem(
        { settingValues.instance },
        {},
      ),
      settings.displayNameSetting.asCustomItemWithTextEditorDialog(
        { settingValues.name },
        childFragmentManager,
      ),
      settings.bioSetting.asCustomItemWithTextEditorDialog(
        { settingValues.bio },
        childFragmentManager,
      ),
      settings.emailSetting.asCustomItemWithTextEditorDialog(
        { settingValues.email },
        childFragmentManager,
      ),
      settings.matrixSetting.asCustomItemWithTextEditorDialog(
        { settingValues.matrixUserId },
        childFragmentManager,
      ),
      settings.avatarSetting.asImageValueItem(
        { settingValues.avatar },
        {
          openImagePicker(accountData, settings.avatarSetting)
        },
      ),
      settings.bannerSetting.asImageValueItem(
        { settingValues.banner },
        {
          openImagePicker(accountData, settings.bannerSetting)
        },
      ),
      settings.defaultSortType.asSingleChoiceSelectorItem(
        { settingValues.defaultSortType },
        { updateValue(settings.defaultSortType.id, it) }
      ),
      settings.showNsfwSetting.asOnOffSwitch(
        { settingValues.showNsfw },
        { updateValue(settings.showNsfwSetting.id, it) }
      ),
      settings.showReadPostsSetting.asOnOffSwitch(
        { settingValues.showReadPosts },
        { updateValue(settings.showReadPostsSetting.id, it) }
      ),
      settings.botAccountSetting.asOnOffSwitch(
        { settingValues.botAccount },
        { updateValue(settings.botAccountSetting.id, it) }
      ),
      settings.sendNotificationsToEmailSetting.asOnOffSwitch(
        { settingValues.sendNotificationsToEmail },
        { updateValue(settings.sendNotificationsToEmailSetting.id, it) }
      ),
      settings.blockSettings.asCustomItem {
        val direction = SettingsWebFragmentDirections
          .actionSettingWebFragmentToSettingsAccountBlockListFragment()
        findNavController().navigateSafe(direction)
      },
      settings.changePassword.asCustomItem {
        ChangePasswordDialogFragment
          .show(childFragmentManager)
      },
    )
  }

  private fun openImagePicker(
    accountData: SettingsWebViewModel.AccountData,
    settingItem: ImageValueSettingItem
  ) {
    val context = requireContext()
    viewModel.imagePickerKey.value = settingItem.id

    val bottomMenu = BottomMenu(context).apply {
      setTitle(settingItem.title)
      addItemWithIcon(
        id = R.id.generate_lemming,
        title = R.string.generate_your_own_lemming,
        icon = R.drawable.ic_lemmy_24,
      )
      addItemWithIcon(
        id = R.id.from_camera,
        title = R.string.take_a_photo,
        icon = R.drawable.baseline_photo_camera_24,
      )
      addItemWithIcon(
        id = R.id.from_gallery,
        title = R.string.choose_from_gallery,
        icon = R.drawable.baseline_image_24,
      )
      addItemWithIcon(R.id.clear, R.string.clear_image, R.drawable.baseline_clear_24)

      setOnMenuItemClickListener {
        when (it.id) {
          R.id.generate_lemming -> {
            viewModel.generateLemming(accountData.account)
          }
          R.id.from_camera -> {
            val intent = ImagePicker.with(requireActivity())
              .apply {
                if (settingItem.isSquare) {
                  cropSquare()
                } else {
                  crop()
                  cropFreeStyle()
                }
              }
              .cameraOnly()
              .maxResultSize(
                1024,
                1024,
                true,
              )
              // Final image resolution will be less than 1080 x 1080
              .createIntent()
            launcher.launch(intent)
          }
          R.id.from_gallery -> {
            val intent = ImagePicker.with(requireActivity())
              .apply {
                if (settingItem.isSquare) {
                  cropSquare()
                } else {
                  crop()
                  cropFreeStyle()
                }
              }
              .galleryOnly()
              .maxResultSize(
                1024,
                1024,
                true,
              )
              // Final image resolution will be less than 1080 x 1080
              .createIntent()
            launcher.launch(intent)
          }
          R.id.clear -> {
            viewModel.updateSettingValue(settings, settingItem.id, "")
          }
        }
      }
    }

    getMainActivity()?.showBottomMenu(bottomMenu)
  }

  private fun save() {
    val updatedSettingValues = viewModel.updatedSettingValues
    if (updatedSettingValues.isEmpty()) {
      launchAlertDialog("no_changes") {
        titleResId = R.string.error_no_settings_changed
        messageResId = R.string.error_no_settings_changed_desc
        positionButtonResId = android.R.string.ok
      }
      return
    }

    viewModel.save(settings, updatedSettingValues)
  }

  override fun updateValue(key: Int, value: Any?) {
    viewModel.updateSettingValue(settings, key, value)
  }
}
