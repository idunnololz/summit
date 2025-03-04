package com.idunnololz.summit.settings.webSettings

import android.app.Activity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
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
import com.idunnololz.summit.alert.OldAlertDialogFragment
import com.idunnololz.summit.alert.launchAlertDialog
import com.idunnololz.summit.alert.newAlertDialogLauncher
import com.idunnololz.summit.databinding.FragmentSettingsWebBinding
import com.idunnololz.summit.settings.LemmyWebSettings
import com.idunnololz.summit.settings.SettingItemsAdapter
import com.idunnololz.summit.settings.SettingPath.getPageName
import com.idunnololz.summit.settings.SettingsFragment
import com.idunnololz.summit.settings.dialogs.SettingValueUpdateCallback
import com.idunnololz.summit.settings.webSettings.changePassword.ChangePasswordDialogFragment
import com.idunnololz.summit.util.AnimationsHelper
import com.idunnololz.summit.util.BaseFragment
import com.idunnololz.summit.util.BottomMenu
import com.idunnololz.summit.util.StatefulData
import com.idunnololz.summit.util.ext.navigateSafe
import com.idunnololz.summit.util.ext.setup
import com.idunnololz.summit.util.insetViewExceptBottomAutomaticallyByMargins
import com.idunnololz.summit.util.insetViewExceptTopAutomaticallyByMargins
import com.idunnololz.summit.util.setupForFragment
import com.idunnololz.summit.util.setupToolbar
import com.idunnololz.summit.util.toErrorMessage
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class SettingsWebFragment :
    BaseFragment<FragmentSettingsWebBinding>(),
    SettingValueUpdateCallback {

    private val viewModel: SettingsWebViewModel by viewModels()

    private var adapter: SettingItemsAdapter? = null

    @Inject
    lateinit var lemmyWebSettings: LemmyWebSettings

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

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        super.onCreateView(inflater, container, savedInstanceState)

        setBinding(FragmentSettingsWebBinding.inflate(inflater, container, false))

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val context = requireContext()

        requireMainActivity().apply {
            setupForFragment<SettingsFragment>()
            insetViewExceptTopAutomaticallyByMargins(viewLifecycleOwner, binding.recyclerView)
            insetViewExceptBottomAutomaticallyByMargins(viewLifecycleOwner, binding.toolbar)

            setupToolbar(binding.toolbar, lemmyWebSettings.getPageName(context))
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
                }
            )

            onBackPressedDispatcher.addCallback(viewLifecycleOwner, backPressHandler)
        }

        viewModel.accountData.observe(viewLifecycleOwner) {
            when (it) {
                is StatefulData.Error -> {
                    binding.loadingView.showDefaultErrorMessageFor(it.error)
                    binding.loadingView.setOnRefreshClickListener {
                        viewModel.fetchAccountInfo()
                    }
                }
                is StatefulData.Loading -> binding.loadingView.showProgressBar()
                is StatefulData.NotStarted -> {}
                is StatefulData.Success -> {
                    binding.loadingView.hideAll()
                    loadWith(it.data)
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
                    adapter?.changesCommitted()

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
                    adapter?.updateSettingValue(it.data.first, it.data.second)
                }
            }
        }

        viewModel.fetchAccountInfo()
    }

    private fun save() {
        val updatedSettingValues = adapter?.updatedSettingValues
        if (updatedSettingValues.isNullOrEmpty()) {
            launchAlertDialog("no_changes") {
                titleResId = R.string.error_no_settings_changed
                messageResId = R.string.error_no_settings_changed_desc
                positionButtonResId = android.R.string.ok
            }
            return
        }

        viewModel.save(updatedSettingValues)
    }

    private fun loadWith(data: SettingsWebViewModel.AccountData) {
        val context = requireContext()
        val adapter = SettingItemsAdapter(
            context = context,
            onSettingClick = {
                when (it.id) {
                    lemmyWebSettings.blockSettings.id -> {
                        val direction = SettingsWebFragmentDirections
                            .actionSettingWebFragmentToSettingsAccountBlockListFragment()
                        findNavController().navigateSafe(direction)
                        true
                    }
                    lemmyWebSettings.changePassword.id -> {
                        ChangePasswordDialogFragment
                            .show(childFragmentManager)
                        true
                    }
                    else -> false
                }
            },
            fragmentManager = childFragmentManager,
            onImagePickerClick = { settingItem ->
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
                                viewModel.generateLemming(data.account)
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
                                adapter?.updateSettingValue(settingItem.id, "")
                            }
                        }
                    }
                }

                getMainActivity()?.showBottomMenu(bottomMenu)
            },
        ).apply {
            this.stateRestorationPolicy = Adapter.StateRestorationPolicy.PREVENT_WHEN_EMPTY
            this.defaultSettingValues = data.defaultValues
            this.setData(data.settings)

            this.settingsChanged = {
                backPressHandler.isEnabled = this.updatedSettingValues.isNotEmpty()
            }
        }.also {
            adapter = it
        }

        with(binding) {
            recyclerView.setup(animationsHelper)
            recyclerView.layoutManager = LinearLayoutManager(context)
            recyclerView.setHasFixedSize(true)
            recyclerView.adapter = adapter
        }
    }

    override fun updateValue(key: Int, value: Any?) {
        adapter?.updateSettingValue(key, value)
    }
}
