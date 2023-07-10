package com.idunnololz.summit.settings.web_settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.core.view.MenuProvider
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import com.idunnololz.summit.R
import com.idunnololz.summit.alert.AlertDialogFragment
import com.idunnololz.summit.databinding.FragmentSettingWebBinding
import com.idunnololz.summit.settings.SettingItemsAdapter
import com.idunnololz.summit.settings.SettingsFragment
import com.idunnololz.summit.settings.dialogs.SettingValueUpdateCallback
import com.idunnololz.summit.util.BaseFragment
import com.idunnololz.summit.util.StatefulData
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SettingWebFragment : BaseFragment<FragmentSettingWebBinding>(), SettingValueUpdateCallback,
AlertDialogFragment.AlertDialogFragmentListener {

    private val viewModel: SettingsWebViewModel by viewModels()

    private var adapter: SettingItemsAdapter? = null

    private val backPressHandler = object : OnBackPressedCallback(false) {
        override fun handleOnBackPressed() {
            AlertDialogFragment.Builder()
                .setTitle(R.string.error_unsaved_changes)
                .setMessage(R.string.error_unsaved_changes_desc)
                .setPositiveButton(R.string.save)
                .setNegativeButton(R.string.discard_saves)
                .createAndShow(childFragmentManager, "unsaved_changes")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        super.onCreateView(inflater, container, savedInstanceState)

        setBinding(FragmentSettingWebBinding.inflate(inflater, container, false))

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val context = requireContext()

        requireMainActivity().apply {
            setupForFragment<SettingsFragment>()
            insetViewExceptTopAutomaticallyByPadding(viewLifecycleOwner, binding.recyclerView)
            insetViewExceptBottomAutomaticallyByMargins(viewLifecycleOwner, binding.toolbar)

            setSupportActionBar(binding.toolbar)

            supportActionBar?.setDisplayShowHomeEnabled(true)
            supportActionBar?.setDisplayHomeAsUpEnabled(true)
            supportActionBar?.title = context.getString(R.string.lemmy_web_preferences)

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
                    binding.loadingView.showDefaultErrorMessageFor(it.error)
                    binding.loadingView.setOnRefreshClickListener {
                        save()
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

        viewModel.fetchAccountInfo()

        addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.menu_setting_web, menu)
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                return when (menuItem.itemId) {
                    R.id.save -> {
                        save()
                        true
                    }
                    else -> false
                }
            }
        })
    }

    private fun save() {
        val updatedSettingValues = adapter?.updatedSettingValues
        if (updatedSettingValues.isNullOrEmpty()) {
            AlertDialogFragment.Builder()
                .setTitle(R.string.error_no_settings_changed)
                .setMessage(R.string.error_no_settings_changed_desc)
                .createAndShow(childFragmentManager, "esadfsadf")
            return
        }

        viewModel.save(updatedSettingValues)
    }

    private fun loadWith(data: SettingsWebViewModel.AccountData) {
        val adapter = SettingItemsAdapter(
            onSettingClick = {
                when (it) {
                    else -> false
                }
            },
            childFragmentManager,
        ).apply {
            this.defaultSettingValues = data.defaultValues
            this.data = data.settings

            this.settingsChanged = {
                backPressHandler.isEnabled = this.updatedSettingValues.isNotEmpty()
            }
        }.also {
            adapter = it
        }

        with(binding) {
            recyclerView.layoutManager = LinearLayoutManager(context)
            recyclerView.setHasFixedSize(true)
            recyclerView.adapter = adapter
        }
    }

    override fun updateValue(key: Int, value: Any?) {
        adapter?.updateSettingValue(key, value)
    }

    override fun onPositiveClick(dialog: AlertDialogFragment, tag: String?) {
        if (tag == "unsaved_changes") {
            // save changes
            save()
        }
    }

    override fun onNegativeClick(dialog: AlertDialogFragment, tag: String?) {
        if (tag == "unsaved_changes") {
            // discard changes
            findNavController().navigateUp()
        }
    }
}