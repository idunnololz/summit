package com.idunnololz.summit.settings.notifications

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.IdRes
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationManagerCompat
import androidx.fragment.app.viewModels
import com.idunnololz.summit.BuildConfig
import com.idunnololz.summit.MainApplication
import com.idunnololz.summit.R
import com.idunnololz.summit.account.fullName
import com.idunnololz.summit.databinding.FragmentSettingNotificationsBinding
import com.idunnololz.summit.databinding.OnOffSettingItemBinding
import com.idunnololz.summit.preferences.Preferences
import com.idunnololz.summit.settings.NotificationSettings
import com.idunnololz.summit.settings.SettingPath.getPageName
import com.idunnololz.summit.settings.SettingsFragment
import com.idunnololz.summit.settings.dialogs.MultipleChoiceDialogFragment
import com.idunnololz.summit.settings.dialogs.SettingValueUpdateCallback
import com.idunnololz.summit.settings.util.bindTo
import com.idunnololz.summit.util.BaseFragment
import com.idunnololz.summit.util.ext.showAllowingStateLoss
import com.idunnololz.summit.util.insetViewExceptBottomAutomaticallyByMargins
import com.idunnololz.summit.util.insetViewExceptTopAutomaticallyByPadding
import com.idunnololz.summit.util.setupForFragment
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class SettingsNotificationsFragment :
    BaseFragment<FragmentSettingNotificationsBinding>(),
    SettingValueUpdateCallback {

    @Inject
    lateinit var preferences: Preferences

    @Inject
    lateinit var settings: NotificationSettings

    private val viewModel: SettingNotificationsViewModel by viewModels()

    private val requestPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission(),
        ) { isGranted: Boolean ->
            setNotificationsEnabled(isGranted)

            updateRendering()
        }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        super.onCreateView(inflater, container, savedInstanceState)

        setBinding(FragmentSettingNotificationsBinding.inflate(inflater, container, false))

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

        settings.isNotificationsEnabled.bindTo(
            b = binding.notifications,
            { preferences.isNotificationsOn },
            {
                val newState = !preferences.isNotificationsOn

                if (newState &&
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                ) {
                    with(NotificationManagerCompat.from(context)) {
                        if (ActivityCompat.checkSelfPermission(
                                context,
                                Manifest.permission.POST_NOTIFICATIONS,
                            ) != PackageManager.PERMISSION_GRANTED
                        ) {
                            requestPermissionLauncher.launch(
                                Manifest.permission.POST_NOTIFICATIONS,
                            )

                            return@with
                        }

                        setNotificationsEnabled(true)
                    }
                } else {
                    setNotificationsEnabled(newState)
                }
            },
        )

        settings.checkInterval.bindTo(
            binding.checkInterval,
            { convertCheckIntervalToOptionId(preferences.notificationsCheckIntervalMs) },
            { setting, currentValue ->
                MultipleChoiceDialogFragment.newInstance(setting, currentValue)
                    .showAllowingStateLoss(childFragmentManager, "aaaaaaa")
            },
        )
//        binding.notifications.root.visibility = View.GONE

        if (BuildConfig.DEBUG) {
            @Suppress("SetTextI18n") // debug only
            binding.forceRunNotificationJob.title.text = "Force run notifications job"
            binding.forceRunNotificationJob.desc.visibility = View.GONE
            binding.forceRunNotificationJob.value.visibility = View.GONE
            binding.forceRunNotificationJob.root.setOnClickListener {
                (activity?.application as? MainApplication)?.runNotificationsUpdate()
            }
        } else {
            binding.forceRunNotificationJob.root.visibility = View.GONE
        }

        viewModel.accounts.observe(viewLifecycleOwner) {
            it ?: return@observe

            binding.accountsContainer.removeAllViews()
            val inflater = LayoutInflater.from(context)

            for (account in it.sortedBy { it.fullName }) {
                val accountRowBinding = OnOffSettingItemBinding
                    .inflate(inflater, binding.accountsContainer, false)

                accountRowBinding.icon.visibility = View.GONE
                accountRowBinding.title.text = account.fullName
                accountRowBinding.switchView.isChecked = viewModel.notificationsManager
                    .isNotificationsEnabledForAccount(account)
                accountRowBinding.desc.visibility = View.GONE

                accountRowBinding.switchView.setOnCheckedChangeListener { buttonView, isChecked ->
                    viewModel.notificationsManager
                        .setNotificationsEnabledForAccount(account, isChecked)
                }

                binding.accountsContainer.addView(accountRowBinding.root)
            }
        }
    }

    private fun setNotificationsEnabled(newState: Boolean) {
        preferences.isNotificationsOn = newState
        viewModel.onNotificationSettingsChanged()
        updateRendering()
    }

    private fun convertCheckIntervalToOptionId(value: Long) = when {
        value >= 1000L * 60L * 60L * 13L -> {
            R.id.unknown
        }
        value >= 1000L * 60L * 60L * 12L -> {
            R.id.refresh_interval_12_hours
        }
        value >= 1000L * 60L * 60L * 4L -> {
            R.id.refresh_interval_4_hours
        }
        value >= 1000L * 60L * 60L * 2L -> {
            R.id.refresh_interval_2_hours
        }
        value >= 1000L * 60L * 60L -> {
            R.id.refresh_interval_60_minutes
        }
        value >= 1000L * 60L * 30L -> {
            R.id.refresh_interval_30_minutes
        }
        else -> {
            R.id.refresh_interval_15_minutes
        }
    }

    private fun convertOptionIdToCheckInterval(@IdRes id: Int) = when (id) {
        R.id.refresh_interval_12_hours -> 1000L * 60L * 60L * 12L
        R.id.refresh_interval_4_hours -> 1000L * 60L * 60L * 4L
        R.id.refresh_interval_2_hours -> 1000L * 60L * 60L * 2L
        R.id.refresh_interval_60_minutes -> 1000L * 60L * 60L
        R.id.refresh_interval_30_minutes -> 1000L * 60L * 30L
        R.id.refresh_interval_15_minutes -> 1000L * 60L * 15L
        else -> null
    }

    override fun updateValue(key: Int, value: Any?) {
        when (key) {
            settings.checkInterval.id -> {
                val checkInterval = convertOptionIdToCheckInterval(value as Int)

                if (checkInterval != null) {
                    preferences.notificationsCheckIntervalMs = checkInterval
                    viewModel.onNotificationCheckIntervalChanged()
                }
            }
        }

        updateRendering()
    }
}
